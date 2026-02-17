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
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.security.JwtTokenProvider;
import ru.galtor85.household_store.service.UserSearchService;
import ru.galtor85.household_store.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "API для регистрации, входа и управления сессиями")
public class AuthRestController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserSearchService userSearchService;

    // ========== РЕГИСТРАЦИЯ ==========
    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя",
            description = "Создает нового пользователя с ролью USER по умолчанию")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterForm request) {

        try {
            // Преобразуем запрос в сущность
            User user = User.builder()
                    .email(request.getEmail())
                    .password(request.getPassword()) // Service захеширует
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .surname(request.getSurname())
                    .birthDate(request.getBirthDate())
                    .role(Role.USER)
                    .creator(request.getFirstName()+" "+request.getLastName()+" email:"+request.getEmail())
                    .active(true)
                    .build();

            User registeredUser = userService.register(user);
            log.info("User registered: {}", registeredUser.getEmail());

            // Автоматически логиним пользователя после регистрации
            AuthResponse authResponse = authenticateAndGetToken(
                    request.getEmail(),
                    request.getMobileNumber(),
                    request.getPassword()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Регистрация успешна",
                            authResponse
                    ));

        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== ВХОД ==========
    @PostMapping("/login")
    @Operation(summary = "Вход в систему",
            description = "Аутентификация пользователя и получение JWT токена")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginForm request) {

        try {
            AuthResponse authResponse = authenticateAndGetToken(
                    request.getEmail(),
                    request.getMobileNumber(),
                    request.getPassword()
            );

            log.info("User logged in: {}", request.getEmail());

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Вход выполнен успешно",
                            authResponse
                    ));

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Неверный email или пароль"));
        }
    }

    // ========== ВЫХОД ==========
    @PostMapping("/logout")
    @Operation(summary = "Выход из системы",
            description = "Инвалидация текущей сессии/токена")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String token) {

        try {
            // В реальном приложении добавляем токен в blacklist
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                // Здесь можно добавить логику инвалидации токена
                log.info("User logged out, token invalidated");
            }

            SecurityContextHolder.clearContext();

            return ResponseEntity.ok(
                    ApiResponse.success("Выход выполнен успешно", null)
            );

        } catch (Exception e) {
            return ResponseEntity.ok(
                    ApiResponse.error("Ошибка при выходе: " + e.getMessage())
            );
        }
    }

    // ========== ПРОВЕРКА ТОКЕНА ==========
    @GetMapping("/validate")
    @Operation(summary = "Проверка валидности токена",
            description = "Валидация JWT токена и получение информации о пользователе")
    public ResponseEntity<ApiResponse<UserResponse>> validateToken(
            @RequestHeader("Authorization") String token) {

        try {
            if (!token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Неверный формат токена"));
            }

            String jwtToken = token.substring(7);

            if (!jwtTokenProvider.validateToken(jwtToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Токен невалиден или истек"));
            }

            String email = jwtTokenProvider.getUsernameFromToken(jwtToken);
            User user = userSearchService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Токен валиден",
                            UserResponse.fromEntity(user)
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Ошибка валидации токена"));
        }
    }

    // ========== ОБНОВЛЕНИЕ ТОКЕНА ==========
    @PostMapping("/refresh")
    @Operation(summary = "Обновление токена",
            description = "Получение нового токена по refresh токену")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request) {

        try {
            // Проверяем refresh токен
            if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Невалидный refresh токен"));
            }

            String email = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());
            User user = userSearchService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Генерируем новую пару токенов
            String newAccessToken = jwtTokenProvider.createToken(user.getEmail(), user.getRole());
            String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getValidity())
                    .user(UserResponse.fromEntity(user))
                    .build();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Токен обновлен",
                            authResponse
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Ошибка обновления токена"));
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЙ МЕТОД ==========
    private AuthResponse authenticateAndGetToken(String email,String mobileNumber, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userSearchService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String accessToken = jwtTokenProvider.createToken(email, user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getValidity())
                .user(UserResponse.fromEntity(user))
                .build();
    }
}