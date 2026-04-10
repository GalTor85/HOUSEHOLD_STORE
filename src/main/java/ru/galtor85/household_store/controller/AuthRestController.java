package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.request.auth.LoginFormRequest;
import ru.galtor85.household_store.dto.request.auth.RefreshTokenRequest;
import ru.galtor85.household_store.dto.request.auth.UserCreateRequest;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.dto.response.auth.AuthResponse;
import ru.galtor85.household_store.dto.response.user.UserResponse;
import ru.galtor85.household_store.service.auth.AuthService;
import ru.galtor85.household_store.service.i18n.MessageService;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_AUTH;

/**
 * REST controller for authentication operations.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>User registration (creates a new user account)</li>
 *   <li>User login (authenticates and returns JWT tokens)</li>
 *   <li>Token validation (checks if a token is valid)</li>
 *   <li>Token refresh (obtains new tokens using refresh token)</li>
 *   <li>User logout (invalidates the current session)</li>
 * </ul>
 *
 * <p>All endpoints are public except logout and validate which require authentication.</p>
 */
@Slf4j
@RestController
@RequestMapping(CONTROL_AUTH)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API for registration, login, and session management")
public class AuthRestController {

    private final AuthService authService;
    private final MessageService messageService;

    /**
     * Registers a new user in the system.
     *
     * <p>Creates a new user account with the default USER role.
     * The password must meet complexity requirements (minimum 6 characters,
     * containing at least one digit, one uppercase letter, one lowercase letter,
     * and one special character).</p>
     *
     * @param request user registration details (email or mobile, password, personal info)
     * @return authentication response with access and refresh tokens
     */
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

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * <p>Users can authenticate using either email address or mobile number.
     * The credentials are validated against the database, and if successful,
     * the user receives an access token and a refresh token.</p>
     *
     * @param request login credentials (email OR mobile, password)
     * @return authentication response with access token, refresh token, and user info
     */
    @PostMapping("/login")
    @Operation(summary = "Login to the system",
            description = "Authenticate user and get JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginFormRequest request) {

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("auth.success.login"),
                authResponse));
    }

    /**
     * Logs out the currently authenticated user.
     *
     * <p>This endpoint invalidates the current JWT token by adding it to a blacklist.
     * The token will no longer be accepted for authenticated requests.</p>
     *
     * @return success response indicating logout was performed
     */
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

    /**
     * Validates the current JWT token and returns user information.
     *
     * <p>This endpoint can be used to check if a token is still valid
     * and to retrieve the associated user details without performing
     * a full authentication flow.</p>
     *
     * @return user information for the authenticated token
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/validate")
    @Operation(summary = "Token validation",
            description = "Validates JWT token and retrieves user information")
    public ResponseEntity<ApiResponse<UserResponse>> validateToken() {

        UserResponse userResponse = authService.validateToken();

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("auth.success.token.valid"),
                        userResponse
                ));
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     *
     * <p>When the access token expires, clients can use this endpoint
     * to obtain a new access token by providing the refresh token.
     * The refresh token itself must still be valid.</p>
     *
     * @param request contains the refresh token
     * @return new authentication response with fresh access token
     */
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