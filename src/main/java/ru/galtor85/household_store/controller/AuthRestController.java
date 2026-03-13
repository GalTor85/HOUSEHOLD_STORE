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
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.JwtTokenProvider;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.service.UserSearchService;
import ru.galtor85.household_store.service.UserService;

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
    private final SecurityUserRepository securityUserRepository;

    @PostMapping("/register")
    @Operation(summary = "Registration of a new user",
            description = "Creates a new user with the default role USER")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody UserCreateRequest request) {

        log.debug(messageService.get("auth-rest-controller.log.register.attempt", request.getEmail()));

        try {
            // Создаем User из запроса
            User user = userToEntity.build(request, "Registration");

            // Регистрируем пользователя (User + SecurityUser)
            User registeredUser = userService.register(user, request.getPassword(), null, null);

            // ИСПРАВЛЕНО: получаем SecurityUser по ID из репозитория
            SecurityUser securityUser = securityUserRepository.findById(registeredUser.getId())
                    .orElseThrow(() -> new RuntimeException("Security user not found after registration"));

            // ИСПРАВЛЕНО: получаем User для билдера ответа
            User userForResponse = userSearchService.getUserById(registeredUser.getId());

            AuthResponse authResponse = buildAuthResponse(securityUser, userForResponse, jwtTokenProvider);

            log.info(messageService.get("auth-rest-controller.log.user.registered", registeredUser.getEmail()));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            messageService.get("auth-rest-controller.auth.register.success"),
                            authResponse));

        } catch (UserAlreadyExistsException e) {
            log.warn(messageService.get("auth-rest-controller.log.register.email.exists", request.getEmail()));
            throw e;
        } catch (ValidationRequestException e) {
            log.warn(messageService.get("auth-rest-controller.log.register.validation.failed", e.getMessage()));
            throw e;
        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.registration.failed",
                    request.getEmail(), e.getMessage()));
            throw new UserRegistrationException(
                    messageService.get("auth-rest-controller.auth.register.error", e.getMessage()));
        }
    }

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

            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

            // ИСПРАВЛЕНО: получаем User по userId из securityUser
            User user = userSearchService.getUserById(securityUser.getUserId());

            // ИСПРАВЛЕНО: передаем и securityUser и user
            AuthResponse authResponse = buildAuthResponse(securityUser, user, jwtTokenProvider);

            log.info(messageService.get("auth-rest-controller.log.login.success",
                    user.getAuthenticationId(), securityUser.getId()));

            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("auth-rest-controller.auth.login.success"),
                    authResponse));

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.warn(messageService.get("auth-rest-controller.log.login.bad.credentials", identify));
            throw new UserAuthenticationError(
                    messageService.get("auth-rest-controller.auth.login.invalid.credentials"));
        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.logins.failed", identify, e.getMessage()));
            throw new UserAuthenticationError(
                    messageService.get("auth-rest-controller.auth.login.error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout from the system",
            description = "Invalidates the current session/token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String token) {

        String tokenInfo = token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "no token";
        log.debug(messageService.get("auth-rest-controller.log.logout.attempt", tokenInfo));

        try {
            if (token != null && token.startsWith("Bearer ")) {
                log.debug(messageService.get("auth-rest-controller.log.logout.token.received"));
            }

            SecurityContextHolder.clearContext();
            log.info(messageService.get("auth-rest-controller.log.logout.success"));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            messageService.get("auth-rest-controller.auth.logout.success"),
                            null
                    )
            );

        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.logout.failed", e.getMessage()));
            throw new UserAuthenticationError(
                    messageService.get("auth-rest-controller.auth.logout.error", e.getMessage()));
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "Token validation",
            description = "Validates JWT token and retrieves user information")
    public ResponseEntity<ApiResponse<UserResponse>> validateToken(
            @RequestHeader("Authorization") String token) {

        String tokenInfo = token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "no token";
        log.debug(messageService.get("auth-rest-controller.log.token.validate.attempt", tokenInfo));

        try {
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn(messageService.get("auth-rest-controller.log.token.invalid.format"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(
                                messageService.get("auth-rest-controller.auth.token.invalid.format")));
            }

            String jwtToken = token.substring(7);

            if (!jwtTokenProvider.validateToken(jwtToken)) {
                log.warn(messageService.get("auth-rest-controller.log.token.invalid.expired"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(
                                messageService.get("auth-rest-controller.auth.token.invalid.expired")));
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(jwtToken);
            log.debug(messageService.get("auth-rest-controller.log.token.userid.extracted", userId));

            // Получаем SecurityUser по ID
            SecurityUser securityUser = securityUserRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("Security user not found for ID: {}", userId);
                        return new TokenValidationError(
                                messageService.get("auth-rest-controller.error.security.user.not.found"));
                    });

            // Проверяем активен ли пользователь через SecurityUser
            if (!securityUser.isEnabled()) {
                log.warn(messageService.get("auth-rest-controller.log.token.user.inactive", userId));
                throw new TokenValidationError(
                        messageService.get("auth-rest-controller.auth.token.user.inactive"));
            }

            // ИСПРАВЛЕНО: получаем User через userSearchService по userId
            User user = userSearchService.getUserById(userId);

            log.info(messageService.get("auth-rest-controller.log.token.valid", userId));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            messageService.get("auth-rest-controller.auth.token.valid"),
                            userMapper.build(user)
                    ));

        } catch (TokenValidationError e) {
            throw e;
        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.token.validation.error", e.getMessage()));
            throw new TokenValidationError(
                    messageService.get("auth-rest-controller.auth.token.validation.error"));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Token refresh",
            description = "Obtains a new token using a refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request) {

        log.debug(messageService.get("auth-rest-controller.log.refresh.attempt"));

        try {
            if (request.getRefreshToken() == null || request.getRefreshToken().isEmpty()) {
                log.warn(messageService.get("auth-rest-controller.log.refresh.token.missing"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(
                                messageService.get("auth-rest-controller.auth.refresh.token.missing")));
            }

            if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
                log.warn(messageService.get("auth-rest-controller.log.refresh.token.invalid"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(
                                messageService.get("auth-rest-controller.auth.refresh.token.invalid")));
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(request.getRefreshToken());
            log.debug(messageService.get("auth-rest-controller.log.refresh.userid.extracted", userId));

            // Получаем SecurityUser
            SecurityUser securityUser = securityUserRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("Security user not found for ID: {}", userId);
                        return new TokenValidationError(
                                messageService.get("auth-rest-controller.error.security.user.not.found"));
                    });

            if (!securityUser.isEnabled()) {
                log.warn(messageService.get("auth-rest-controller.log.refresh.user.inactive", userId));
                throw new TokenValidationError(
                        messageService.get("auth-rest-controller.auth.refresh.user.inactive"));
            }

            // ИСПРАВЛЕНО: получаем User через userSearchService
            User user = userSearchService.getUserById(userId);

            AuthResponse authResponse = buildAuthResponse(securityUser, user, jwtTokenProvider);

            log.info(messageService.get("auth-rest-controller.log.refresh.success", userId));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            messageService.get("auth-rest-controller.auth.refresh.success"),
                            authResponse));

        } catch (TokenValidationError e) {
            throw e;
        } catch (Exception e) {
            log.error(messageService.get("auth-rest-controller.log.refresh.error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(
                            messageService.get("auth-rest-controller.auth.refresh.error")));
        }
    }

    // ИСПРАВЛЕНО: метод buildAuthResponse теперь принимает User отдельно
    private AuthResponse buildAuthResponse(SecurityUser securityUser, User user, JwtTokenProvider jwtTokenProvider) {
        String accessToken = jwtTokenProvider.createToken(securityUser, user);
        String refreshToken = jwtTokenProvider.createRefreshToken(securityUser, user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getValidity())
                .user(userMapper.build(user))
                .build();
    }
}