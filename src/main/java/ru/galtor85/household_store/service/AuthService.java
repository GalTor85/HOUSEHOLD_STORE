package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.BlacklistedToken;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserMapper;
import ru.galtor85.household_store.mapper.UserToEntity;
import ru.galtor85.household_store.repository.BlacklistedTokenRepository;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.resolver.UserIdentifierResolver;
import ru.galtor85.household_store.security.JwtTokenProvider;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.validator.AuthenticationValidator;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserSearchService userSearchService;
    private final UserToEntity userToEntity;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final UserIdentifierResolver userIdentifierResolver;
    private final SecurityUserRepository securityUserRepository;
    private final AuthenticationValidator authenticationValidator;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Transactional
    public AuthResponse register(UserCreateRequest request) {
        log.debug(messageService.get("auth.log.register.attempt", request.getEmail()));

        User user = userToEntity.build(request, messageService.get("self-registration"));
        User registeredUser = userService.register(user, request.getPassword());

        SecurityUser securityUser = securityUserRepository.findById(registeredUser.getId())
                .orElseThrow(() -> {
                    log.error(messageService.get("auth.log.security.user.not.found", registeredUser.getId()));
                    return new SecurityUserNotFoundException(registeredUser.getId());
                });

        User userForResponse = userSearchService.getUserById(registeredUser.getId());

        log.info(messageService.get("auth.log.user.registered", registeredUser.getEmail()));

        return buildAuthResponse(securityUser, userForResponse);
    }

    public AuthResponse login(LoginForm request) {
        String identify = userIdentifierResolver.resolve(request);
        log.debug(messageService.get("auth.log.login.attempt", identify));

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identify, request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            User user = userSearchService.getUserById(securityUser.getUserId());

            if (!securityUser.isEnabled()) {
                log.warn(messageService.get("auth.log.login.deactivated", identify));
                throw new AccountDeactivatedException(securityUser.getUserId());
            }

            log.info(messageService.get("auth.log.login.success",
                    user.getAuthenticationId(), securityUser.getId()));

            return buildAuthResponse(securityUser, user);

        } catch (BadCredentialsException e) {
            log.warn(messageService.get("auth.log.login.bad.credentials", identify));
            throw new InvalidCredentialsException(identify);
        }
    }

    /**
     * ✅ Logout - добавляет текущий токен в черный список
     */
    @Transactional
    public AuthResponse logout() {
        // ✅ Получаем токен из ThreadLocal
        String currentToken = JwtTokenHolder.getToken();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            User user = userSearchService.getUserById(securityUser.getUserId());

            if (currentToken != null && !currentToken.isEmpty()) {
                // ✅ Добавляем токен в черный список
                addTokenToBlacklist(currentToken, securityUser.getUserId());
                log.info(messageService.get("auth.log.token.blacklisted",
                        currentToken.substring(0, Math.min(20, currentToken.length())) + "..."));
            } else {
                log.warn("No token found in ThreadLocal for logout");
            }

            // Очищаем текущий контекст
            SecurityContextHolder.clearContext();

            // ✅ Генерируем новые токены для следующей сессии
            AuthResponse newTokens = buildAuthResponse(securityUser, user);

            log.info(messageService.get("auth.log.logout.success", user.getEmail()));

            return newTokens;
        }

        log.info(messageService.get("auth.log.logout.success"));
        SecurityContextHolder.clearContext();

        return AuthResponse.builder()
                .tokenType("Bearer")
                .build();
    }

    /**
     * Добавляет токен в черный список
     */
    private void addTokenToBlacklist(String token, Long userId) {
        try {
            LocalDateTime expiresAt = jwtTokenProvider.getExpirationDateFromToken(token);

            // Проверяем, не добавлен ли уже токен
            if (blacklistedTokenRepository.existsByToken(token)) {
                log.debug("Token already in blacklist");
                return;
            }

            BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                    .token(token)
                    .userId(userId)
                    .expiresAt(expiresAt)
                    .blacklistedAt(LocalDateTime.now())
                    .build();

            blacklistedTokenRepository.save(blacklistedToken);

            log.debug("Token added to blacklist, expires at: {}", expiresAt);

            // Очищаем просроченные токены
            int deleted = blacklistedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            if (deleted > 0) {
                log.debug("Deleted {} expired tokens from blacklist", deleted);
            }

        } catch (Exception e) {
            log.error("Failed to add token to blacklist: {}", e.getMessage(), e);
        }
    }

    public UserResponse validateToken() {
        Authentication authentication = authenticationValidator.validateAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        log.debug(messageService.get("auth.log.token.valid", securityUser.getUsername()));

        User user = userSearchService.getUserById(securityUser.getUserId());

        return userMapper.build(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        log.debug(messageService.get("auth.log.refresh.attempt"));

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn(messageService.get("auth.log.refresh.token.missing"));
            throw new RefreshTokenMissingException();
        }

        // ✅ Проверяем, не в черном ли списке refresh token
        if (blacklistedTokenRepository.existsByToken(refreshToken)) {
            log.warn("Refresh token is blacklisted");
            throw new TokenExpiredException();
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn(messageService.get("auth.log.refresh.token.invalid"));
            throw new TokenExpiredException();
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        log.debug(messageService.get("auth.log.refresh.userid.extracted", userId));

        SecurityUser securityUser = securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("auth.log.security.user.not.found", userId));
                    return new SecurityUserNotFoundException(userId);
                });

        if (!securityUser.isEnabled()) {
            log.warn(messageService.get("auth.log.refresh.user.inactive", userId));
            throw new AccountDeactivatedException(userId);
        }

        User user = userSearchService.getUserById(userId);

        log.info(messageService.get("auth.log.refresh.success", userId));

        return buildAuthResponse(securityUser, user);
    }

    private AuthResponse buildAuthResponse(SecurityUser securityUser, User user) {
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