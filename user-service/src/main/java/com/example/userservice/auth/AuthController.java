package com.example.userservice.auth;

import com.example.userservice.auth.dto.AuthResponse;
import com.example.userservice.auth.dto.LoginRequest;
import com.example.userservice.auth.dto.RegisterRequest;
import com.example.userservice.auth.dto.RegisterResponse;
import com.example.userservice.auth.dto.PromptRequest;
import com.example.userservice.chat.ChatService;
import com.example.userservice.chat.MultiAgentService;
import com.example.userservice.user.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ChatService chatService;
    private final MultiAgentService multiAgentService;

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);

        Cookie accessCookie = new Cookie("access_token", authResponse.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        // 1 hour by default
        accessCookie.setMaxAge(60 * 60);

        Cookie refreshCookie = new Cookie("refresh_token", authResponse.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/auth");
        // 7 days
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return authResponse;
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        // Clear access token cookie
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);

        // Clear refresh token cookie
        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/auth");
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal User user) {
        return new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    @PostMapping(value = "/prompt", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PRODUCT_MANAGER', 'BUSINESS_MANAGER', 'DEVELOPER')")
    public ResponseEntity<String> prompt(@AuthenticationPrincipal User user,
                                         @RequestBody PromptRequest request) {
        String prompt = request.getPrompt();
        String chatId = request.getChatId();
        // Route to the appropriate agent (DevAgent/OpenAI or DataAgent/Perplexity)
        String htmlResponse = multiAgentService.getResponseForPrompt(
                user,
                prompt,
                chatId,
                request.getLimit()
        );

        // Persist user prompt & AI HTML response so history survives refresh
        chatService.saveUserAndAiMessages(user, chatId, prompt, htmlResponse);

            return ResponseEntity
                    .ok()
                .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                .body(htmlResponse);
    }

    // ==== ROLE-BASED TEST ENDPOINTS USING JWT ====

    /**
     * Accessible only to users with role CUSTOMER
     */
    @GetMapping("/test/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerOnly(@AuthenticationPrincipal User user) {
        return "Hello CUSTOMER " + user.getUsername();
    }

    /**
     * Accessible only to users with role PRODUCT_MANAGER
     */
    @GetMapping("/test/product-manager")
    @PreAuthorize("hasRole('PRODUCT_MANAGER')")
    public String productManagerOnly(@AuthenticationPrincipal User user) {
        return "Hello PRODUCT_MANAGER " + user.getUsername();
    }

    /**
     * Accessible only to users with role BUSINESS_MANAGER
     */
    @GetMapping("/test/business-manager")
    @PreAuthorize("hasRole('BUSINESS_MANAGER')")
    public String businessManagerOnly(@AuthenticationPrincipal User user) {
        return "Hello BUSINESS_MANAGER " + user.getUsername();
    }

    /**
     * Accessible only to users with role DEVELOPER
     */
    @GetMapping("/test/developer")
    @PreAuthorize("hasRole('DEVELOPER')")
    public String developerOnly(@AuthenticationPrincipal User user) {
        return "Hello DEVELOPER " + user.getUsername();
    }

    public record UserMeResponse(Long id, String username, String email, String role) {
    }

}


