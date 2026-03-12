package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.controller.resolve.UserIdentifierResolver;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserMapper;
import ru.galtor85.household_store.mapper.UserToEntity;
import ru.galtor85.household_store.security.JwtTokenProvider;
import ru.galtor85.household_store.service.UserSearchService;
import ru.galtor85.household_store.service.UserService;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API for registration, login, and session management")
public class AuthRestController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserSearchService userSearchService;
    private final UserToEntity userToEntity;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final UserIdentifierResolver userIdentifierResolver;

    // ========== РЕГИСТРАЦИЯ ==========
    @PostMapping("/register")
    @Operation(summary = "Registration of a new user",
            description = "Creates a new user with the default role USER")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody UserCreateRequest request) {

        log.debug(messageService.get("auth-rest-controller.log.register.attempt", request.getEmail()));

        try {
            User user = userToEntity.build(request, "Registration");

            User registeredUser = userService.register(user);
            log.info(messageService.get("auth-rest-controller.log.user.registered", registeredUser.getEmail()));

            AuthResponse authResponse = buildAuthResponse(registeredUser, jwtTokenProvider);

            String successMessage = messageService.get("auth-rest-controller.auth.register.success");
            log.debug(messageService.get("auth-rest-controller.log.register.completed", registeredUser.getEmail()));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(successMessage, authResponse));

        } catch (UserAlreadyExistsException e) {
            log.warn(messageService.get("auth-rest-controller.log.register.email.exists", request.getEmail()));
            throw e;
        } catch (ValidationRequestException e) {
            log.warn(messageService.get("auth-rest-controller.log.register.validation.failed", e.getMessage()));
            throw e;
        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.registration.failed", request.getEmail(), e.getMessage()));
            throw new UserRegistrationException(
                    messageService.get("auth-rest-controller.auth.register.error", e.getMessage()));
        }
    }

    // ========== ВХОД ==========
    @PostMapping("/login")
    @Operation(summary = "Login to the system",
            description = "Authenticate user and get JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginForm request) {

        String identify = userIdentifierResolver.resolve(request);
        log.debug(messageService.get("auth-rest-controller.log.login.attempt", identify));

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identify, request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userSearchService.searchUsersByEmailOrMobileNumber(identify)
                    .orElseThrow(() -> {
                        String errorMessage = messageService.get(
                                "auth-rest-controller.error.user.not.found.identify", identify);
                        log.error(errorMessage);
                        return new UserNotFoundException(errorMessage);
                    });

            if (!user.isActive()) {
                log.warn(messageService.get("auth-rest-controller.log.login.user.inactive", identify));
                throw new UserNotActiveException(
                        messageService.get("auth-rest-controller.auth.login.user.inactive"));
            }

            AuthResponse authResponse = buildAuthResponse(user, jwtTokenProvider);

            String successMessage = messageService.get("auth-rest-controller.auth.login.success");
            log.info(messageService.get("auth-rest-controller.log.login.success", identify, user.getId()));

            return ResponseEntity.ok(ApiResponse.success(successMessage, authResponse));

        } catch (UserNotFoundException e) {
            log.warn(messageService.get("auth-rest-controller.log.login.user.not.found", identify));
            throw new UserLoginException(
                    messageService.get("auth-rest-controller.auth.login.invalid.credentials"));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.warn(messageService.get("auth-rest-controller.log.login.bad.credentials", identify));
            throw new UserLoginException(
                    messageService.get("auth-rest-controller.auth.login.invalid.credentials"));
        } catch (UserLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.logins.failed", identify, e.getMessage()));
            throw new UserAuthenticationError(
                    messageService.get("auth-rest-controller.auth.login.error", e.getMessage()));
        }
    }

    // ========== ВЫХОД ==========
    @PostMapping("/logout")
    @Operation(summary = "Logout from the system",
            description = "Invalidates the current session/token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String token) {

        String tokenInfo = token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "no token";
        log.debug(messageService.get("auth-rest-controller.log.logout.attempt", tokenInfo));

        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                log.debug(messageService.get("auth-rest-controller.log.logout.token.received"));
                // Здесь можно добавить логику инвалидации токена
            }

            SecurityContextHolder.clearContext();

            String successMessage = messageService.get("auth-rest-controller.auth.logout.success");
            log.info(messageService.get("auth-rest-controller.log.logout.success"));

            return ResponseEntity.ok(
                    ApiResponse.success(successMessage, null)
            );

        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.logout.failed", e.getMessage()));
            throw new UserAuthenticationError(
                    messageService.get("auth-rest-controller.auth.logout.error", e.getMessage()));
        }
    }

    // ========== ПРОВЕРКА ТОКЕНА ==========
    @GetMapping("/validate")
    @Operation(summary = "Token validation",
            description = "Validates JWT token and retrieves user information")
    public ResponseEntity<ApiResponse<UserResponse>> validateToken(
            @RequestHeader("Authorization") String token) {

        String tokenInfo = token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "no token";
        log.debug(messageService.get("auth-rest-controller.log.token.validate.attempt", tokenInfo));

        try {
            if (token == null || !token.startsWith("Bearer ")) {
                String errorMessage = messageService.get("auth-rest-controller.auth.token.invalid.format");
                log.warn(messageService.get("auth-rest-controller.log.token.invalid.format"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(errorMessage));
            }

            String jwtToken = token.substring(7);

            if (!jwtTokenProvider.validateToken(jwtToken)) {
                String errorMessage = messageService.get("auth-rest-controller.auth.token.invalid.expired");
                log.warn(messageService.get("auth-rest-controller.log.token.invalid.expired"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(errorMessage));
            }

            String identify = jwtTokenProvider.getUsernameFromToken(jwtToken);
            log.debug(messageService.get("auth-rest-controller.log.token.identify.extracted", identify));

            User user = userSearchService.searchUsersByEmailOrMobileNumber(identify)
                    .orElseThrow(() -> {
                        String errorMessage = messageService.get(
                                "auth-rest-controller.error.user.not.found.identify", identify);
                        log.error(errorMessage);
                        return new UserNotFoundException(errorMessage);
                    });

            if (!user.isActive()) {
                log.warn(messageService.get("auth-rest-controller.log.token.user.inactive", identify));
                throw new TokenValidationError(
                        messageService.get("auth-rest-controller.auth.token.user.inactive"));
            }

            String successMessage = messageService.get("auth-rest-controller.auth.token.valid");
            log.info(messageService.get("auth-rest-controller.log.token.valid", identify));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            successMessage,
                            userMapper.build(user)
                    ));

        } catch (UserNotFoundException e) {
            throw new TokenValidationError(
                    messageService.get("auth-rest-controller.auth.token.user.not.found"));
        } catch (TokenValidationError e) {
            throw e;
        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.token.validation.error", e.getMessage()));
            throw new TokenValidationError(
                    messageService.get("auth-rest-controller.auth.token.validation.error"));
        }
    }

    // ========== ОБНОВЛЕНИЕ ТОКЕНА ==========
    @PostMapping("/refresh")
    @Operation(summary = "Token refresh",
            description = "Obtains a new token using a refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request) {

        log.debug(messageService.get("auth-rest-controller.log.refresh.attempt"));

        try {
            if (request.getRefreshToken() == null || request.getRefreshToken().isEmpty()) {
                String errorMessage = messageService.get("auth-rest-controller.auth.refresh.token.missing");
                log.warn(messageService.get("auth-rest-controller.log.refresh.token.missing"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(errorMessage));
            }

            if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
                String errorMessage = messageService.get("auth-rest-controller.auth.refresh.token.invalid");
                log.warn(messageService.get("auth-rest-controller.log.refresh.token.invalid"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(errorMessage));
            }

            String identify = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());
            log.debug(messageService.get("auth-rest-controller.log.refresh.identify.extracted", identify));

            User user = userSearchService.searchUsersByEmailOrMobileNumber(identify)
                    .orElseThrow(() -> {
                        String errorMessage = messageService.get(
                                "auth-rest-controller.error.user.not.found.identify", identify);
                        log.error(errorMessage);
                        return new UserNotFoundException(errorMessage);
                    });

            if (!user.isActive()) {
                log.warn(messageService.get("auth-rest-controller.log.refresh.user.inactive", identify));
                throw new TokenValidationError(
                        messageService.get("auth-rest-controller.auth.refresh.user.inactive"));
            }

            String newAccessToken = jwtTokenProvider.createToken(identify, user.getRole());
            String newRefreshToken = jwtTokenProvider.createRefreshToken(identify);

            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getValidity())
                    .user(userMapper.build(user))
                    .build();

            String successMessage = messageService.get("auth-rest-controller.auth.refresh.success");
            log.info(messageService.get("auth-rest-controller.log.refresh.success", identify));

            return ResponseEntity.ok(
                    ApiResponse.success(successMessage, authResponse));

        } catch (UserNotFoundException e) {
            throw new TokenValidationError(
                    messageService.get("auth-rest-controller.auth.refresh.user.not.found"));
        } catch (TokenValidationError e) {
            throw e;
        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.refresh.error", e.getMessage()));
            String errorMessage = messageService.get("auth-rest-controller.auth.refresh.error");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(errorMessage));
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЙ МЕТОД ==========
    private AuthResponse authenticateAndGetToken(String email, String mobileNumber, String password) {
        String identify = email != null && !email.isEmpty() ? email : mobileNumber;
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identify, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userSearchService.searchUsersByEmailOrMobileNumber(identify)
                .orElseThrow(() -> new UserNotFoundException(
                        messageService.get("auth-rest-controller.error.user.not.found.identify", identify)));

        return buildAuthResponse(user, jwtTokenProvider);
    }

    public AuthResponse buildAuthResponse(User user, JwtTokenProvider jwtTokenProvider) {
        String identify = user.getEmail() != null ? user.getEmail() : user.getMobileNumber();
        String accessToken = jwtTokenProvider.createToken(identify, user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(identify);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getValidity())
                .user(userMapper.build(user))
                .build();
    }
}