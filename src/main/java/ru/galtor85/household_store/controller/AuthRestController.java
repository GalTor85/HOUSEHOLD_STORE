package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.service.AuthService;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API for registration, login, and session management")
public class AuthRestController {

    private final AuthService authService;
    private final MessageService messageService;

    @PostMapping("/register")
    @Operation(summary = "Registration of a new user",
            description = "Creates a new user with the default role USER")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody UserCreateRequest request) {

        AuthResponse authResponse = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("auth.success.registered"),
                        authResponse));
    }

    @PostMapping("/login")
    @Operation(summary = "Login to the system",
            description = "Authenticate user and get JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginForm request) {

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("auth.success.login"),
                authResponse));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    @Operation(summary = "Logout from the system",
            description = "Invalidates the current session/token")
    public ResponseEntity<ApiResponse<Void>> logout() {

        authService.logout();

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("auth.success.logout"),
                        null
                )
        );
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/validate")
    @Operation(summary = "Token validation",
            description = "Validates JWT token and retrieves user information")
    public ResponseEntity<ApiResponse<UserResponse>> validateToken() {
       // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserResponse userResponse = authService.validateToken();

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("auth.success.token.valid"),
                        userResponse
                ));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Token refresh",
            description = "Obtains a new token using a refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request) {

        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("auth.success.refresh"),
                        authResponse));
    }
}