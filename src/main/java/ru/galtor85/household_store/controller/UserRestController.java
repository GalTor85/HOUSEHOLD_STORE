package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.ApiResponse;
import ru.galtor85.household_store.dto.EditUserRequest;
import ru.galtor85.household_store.dto.UserResponse;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.security.JwtTokenProvider;
import ru.galtor85.household_store.service.UserSearchService;
import ru.galtor85.household_store.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "API для управления пользователями")
public class UserRestController {
    private final UserSearchService userSearchService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/edit")
    public ResponseEntity<ApiResponse<UserResponse>> editUser(
            @Valid @RequestBody EditUserRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 1. Проверяем наличие заголовка
            if (authHeader == null || authHeader.isEmpty()) {
                log.warn("Missing Authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Отсутствует заголовок авторизации"));
            }

            // 2. Извлекаем токен (убираем "Bearer ")
            String token;
            if (authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else {
                token = authHeader; // если вдруг пришел просто токен
            }

            log.info("Processing edit request with token");

            // 3. Валидируем токен
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Невалидный или просроченный токен"));
            }

            // 4. Получаем идентификатор из токена
            String identify = jwtTokenProvider.getIdentifierFromToken(token);
            log.info("Identifier from token: {}", identify);

            // 5. Ищем пользователя
            User user = userSearchService.searchUsersByEmailOrMobileNumber(identify)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + identify));

            // 6. Обновляем данные пользователя
            User updatedUser = userService.edit(user, request);

            log.info("User data updated successfully: {}", updatedUser.getId());

            // 7. Возвращаем успешный ответ
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Данные пользователя изменены",
                            UserResponse.fromEntity(updatedUser)
                    )
            );

        } catch (Exception e) {
            log.error("Error editing user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка при изменении данных пользователя: " + e.getMessage()));
        }
    }
}