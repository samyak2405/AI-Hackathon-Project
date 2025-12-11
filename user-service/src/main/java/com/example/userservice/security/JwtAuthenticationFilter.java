package com.example.userservice.security;

import com.example.userservice.user.User;
import com.example.userservice.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        String jwt = null;

        // 1. Prefer Authorization header if present
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            log.debug("JWT filter: using token from Authorization header for {} {}", method, path);
        }

        // 2. Fallback to access_token cookie (used by browser clients)
        if (jwt == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("access_token".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        log.debug("JWT filter: using token from access_token cookie for {} {}", method, path);
                        break;
                    }
                }
            }
        }

        if (jwt == null) {
            log.debug("JWT filter: no JWT found for {} {}, continuing without authentication", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        String username;
        try {
            username = jwtService.extractUsername(jwt);
            log.debug("JWT filter: extracted username '{}' from token for {} {}", username, method, path);
        } catch (Exception e) {
            log.warn("JWT filter: failed to parse token for {} {}: {}", method, path, e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent() && jwtService.isTokenValid(jwt, userOptional.get())) {
                User user = userOptional.get();
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("JWT filter: authentication set for user '{}' on {} {}", username, method, path);
            } else {
                log.warn("JWT filter: token invalid or user not found for username '{}' on {} {}", username, method, path);
            }
        }

        filterChain.doFilter(request, response);
    }
}


