package com.example.userservice.auth;

import com.example.userservice.auth.dto.AuthResponse;
import com.example.userservice.auth.dto.LoginRequest;
import com.example.userservice.auth.dto.RegisterRequest;
import com.example.userservice.auth.dto.RegisterResponse;
import com.example.userservice.exception.AuthenticationFailedException;
import com.example.userservice.security.JwtService;
import com.example.userservice.user.User;
import com.example.userservice.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Attempting to register new user with username='{}', email='{}'", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username '{}' is already taken", request.getUsername());
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email '{}' is already in use", request.getEmail());
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);

        log.info("Successfully registered user with id={}, username='{}', role={}", user.getId(), user.getUsername(), user.getRole());

        return new RegisterResponse(
                "Registration successful. You can login now.",
                user.getUsername(),
                user.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for username='{}'", request.getUsername());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException | AuthenticationServiceException ex) {
            log.warn("Login failed for username='{}': bad credentials or auth service error", request.getUsername());
            throw new AuthenticationFailedException("Invalid username or password");
        } catch (AuthenticationException ex) {
            log.error("Login failed for username='{}' due to unexpected authentication error", request.getUsername(), ex);
            throw new AuthenticationFailedException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: user '{}' not found after successful authentication", request.getUsername());
                    return new AuthenticationFailedException("Invalid username or password");
                });

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Login successful for username='{}' with role={}", user.getUsername(), user.getRole());

        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }
}


