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
import ru.galtor85.household_store.controller.resolve.UserIdentifierResolver;
import ru.galtor85.household_store.dto.AuthResponse;
import ru.galtor85.household_store.dto.LoginForm;
import ru.galtor85.household_store.dto.UserCreateRequest;
import ru.galtor85.household_store.dto.UserResponse;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserMapper;
import ru.galtor85.household_store.mapper.UserToEntity;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.JwtTokenProvider;
import ru.galtor85.household_store.security.SecurityUser;

import java.util.Locale;

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

    @Transactional
    public AuthResponse register(UserCreateRequest request, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("auth.log.register.attempt", request.getEmail()));

        // Создаем User из запроса
        User user = userToEntity.build(request, "Registration");

        // Регистрируем пользователя (User + SecurityUser)
        User registeredUser = userService.register(user, request.getPassword(), null, locale);

        // Получаем SecurityUser по ID из репозитория
        SecurityUser securityUser = securityUserRepository.findById(registeredUser.getId())
                .orElseThrow(() -> {
                    log.error(messageService.get("auth.log.security.user.not.found", registeredUser.getId()));
                    return new SecurityUserNotFoundException(registeredUser.getId());
                });

        // Получаем User для билдера ответа
        User userForResponse = userSearchService.getUserById(registeredUser.getId(), locale);

        log.info(messageService.get("auth.log.user.registered", registeredUser.getEmail()));

        return buildAuthResponse(securityUser, userForResponse);
    }

    public AuthResponse login(LoginForm request, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        String identify = userIdentifierResolver.resolve(request, locale);
        log.debug(messageService.get("auth.log.login.attempt", identify));

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identify, request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

            // Получаем User по userId из securityUser
            User user = userSearchService.getUserById(securityUser.getUserId(), locale);

            // Проверяем активен ли пользователь
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

    public void logout() {
        log.info(messageService.get("auth.log.logout.success"));
        SecurityContextHolder.clearContext();
    }

    public UserResponse validateToken(String token, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        String tokenInfo = token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "no token";
        log.debug(messageService.get("auth.log.token.validate.attempt", tokenInfo));

        if (token == null || !token.startsWith("Bearer ")) {
            log.warn(messageService.get("auth.log.token.invalid.format"));
            throw new InvalidTokenFormatException();
        }

        String jwtToken = token.substring(7);

        if (!jwtTokenProvider.validateToken(jwtToken)) {
            log.warn(messageService.get("auth.log.token.invalid.expired"));
            throw new TokenExpiredException();
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(jwtToken);
        log.debug(messageService.get("auth.log.token.userid.extracted", userId));

        // Получаем SecurityUser по ID
        SecurityUser securityUser = securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("auth.log.security.user.not.found", userId));
                    return new SecurityUserNotFoundException(userId);
                });

        // Проверяем активен ли пользователь через SecurityUser
        if (!securityUser.isEnabled()) {
            log.warn(messageService.get("auth.log.token.user.inactive", userId));
            throw new AccountDeactivatedException(userId);
        }

        // Получаем User через userSearchService по userId
        User user = userSearchService.getUserById(userId, locale);

        log.info(messageService.get("auth.log.token.valid", userId));

        return userMapper.build(user);
    }

    public AuthResponse refreshToken(String refreshToken, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("auth.log.refresh.attempt"));

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn(messageService.get("auth.log.refresh.token.missing"));
            throw new RefreshTokenMissingException();
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn(messageService.get("auth.log.refresh.token.invalid"));
            throw new TokenExpiredException();
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        log.debug(messageService.get("auth.log.refresh.userid.extracted", userId));

        // Получаем SecurityUser
        SecurityUser securityUser = securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("auth.log.security.user.not.found", userId));
                    return new SecurityUserNotFoundException(userId);
                });

        if (!securityUser.isEnabled()) {
            log.warn(messageService.get("auth.log.refresh.user.inactive", userId));
            throw new AccountDeactivatedException(userId);
        }

        // Получаем User через userSearchService
        User user = userSearchService.getUserById(userId, locale);

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