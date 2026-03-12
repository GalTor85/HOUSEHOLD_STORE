package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserMapper;
import ru.galtor85.household_store.security.JwtTokenProvider;
import ru.galtor85.household_store.service.UserSearchService;
import ru.galtor85.household_store.service.UserService;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API for Users")
public class UserRestController {

    private final UserSearchService userSearchService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final MessageService messageService;

    /**
     * Вспомогательный метод для извлечения пользователя из токена
     */
    private User getUserFromToken(String authHeader) {
        // 1. Проверяем наличие заголовка
        if (authHeader == null || authHeader.isEmpty()) {
            log.warn(messageService.get("user-rest-controller.log.auth.header.missing"));
            throw new UnauthorizedException(messageService.get("user-rest-controller.error.auth.header.missing"));
        }

        // 2. Извлекаем токен (убираем "Bearer ")
        String token;
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            token = authHeader;
        }

        // 3. Валидируем токен
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn(messageService.get("user-rest-controller.log.auth.token.invalid"));
            throw new UnauthorizedException(messageService.get("user-rest-controller.error.auth.token.invalid"));
        }

        // 4. Получаем идентификатор из токена
        String identify = jwtTokenProvider.getIdentifierFromToken(token);
        log.debug(messageService.get("user-rest-controller.log.auth.token.identify", identify));

        // 5. Ищем пользователя
        return userSearchService.searchUsersByEmailOrMobileNumber(identify)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.get("user-rest-controller.error.user.not.found", identify)));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile",
            description = "Updates the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> editUser(
            @Valid @RequestBody UserEditRequest request,
            @RequestHeader("Authorization") String authHeader) {

        log.info(messageService.get("user-rest-controller.log.profile.update.start"));

        try {
            User user = getUserFromToken(authHeader);
            User updatedUser = userService.edit(user, request);

            log.info(messageService.get("user-rest-controller.log.profile.update.success", updatedUser.getId()));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            messageService.get("user-rest-controller.msg.profile.update.success"),
                            userMapper.build(updatedUser)
                    )
            );

        } catch (UnauthorizedException e) {
            log.warn(messageService.get("user-rest-controller.log.profile.update.unauthorized", e.getMessage()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (ResourceNotFoundException e) {
            log.warn(messageService.get("user-rest-controller.log.profile.update.notfound", e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (IllegalArgumentException | ValidationRequestException e) {
            log.warn(messageService.get("user-rest-controller.log.profile.update.validation", e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error(messageService.get("user-rest-controller.log.profile.update.error", e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(messageService.get("error.unexpected")));
        }
    }

    @PutMapping("/password")
    @Operation(summary = "Update user password",
            description = "Updates the password of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(
            @Valid @RequestBody UserUpdatePasswordRequest request,
            @RequestHeader("Authorization") String authHeader) {

        log.info(messageService.get("user-rest-controller.log.password.update.start"));

        try {
            User user = getUserFromToken(authHeader);
            User updatedUser = userService.passwordUpdate(user, request);

            log.info(messageService.get("user-rest-controller.log.password.update.success", updatedUser.getId()));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            messageService.get("user-rest-controller.msg.password.update.success"),
                            userMapper.build(updatedUser)
                    )
            );

        } catch (UnauthorizedException e) {
            log.warn(messageService.get("user-rest-controller.log.password.update.unauthorized", e.getMessage()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (ResourceNotFoundException e) {
            log.warn(messageService.get("user-rest-controller.log.password.update.notfound", e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (IllegalArgumentException | ValidationRequestException e) {
            log.warn(messageService.get("user-rest-controller.log.password.update.validation", e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error(messageService.get("user-rest-controller.log.password.update.error", e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(messageService.get("error.unexpected")));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user information",
            description = "Retrieves the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {

        log.info(messageService.get("user-rest-controller.log.profile.fetch.start"));

        try {
            User user = getUserFromToken(authHeader);

            log.info(messageService.get("user-rest-controller.log.profile.fetch.success", user.getId()));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            messageService.get("user-rest-controller.msg.profile.fetch.success"),
                            userMapper.build(user)
                    )
            );

        } catch (UnauthorizedException e) {
            log.warn(messageService.get("user-rest-controller.log.profile.fetch.unauthorized", e.getMessage()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (ResourceNotFoundException e) {
            log.warn(messageService.get("user-rest-controller.log.profile.fetch.notfound", e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error(messageService.get("user-rest-controller.log.profile.fetch.error", e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(messageService.get("error.unexpected")));
        }
    }
}