package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.galtor85.household_store.dto.ApiResponse;
import ru.galtor85.household_store.dto.HealthStatus;
import ru.galtor85.household_store.dto.HomeInfo;
import ru.galtor85.household_store.dto.SystemInfo;
import ru.galtor85.household_store.service.SystemService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Главная", description = "API для получения информации о системе")
public class HomeRestController {

    private final SystemService systemService;

    @GetMapping("/")
    @Operation(summary = "Главная страница API",
            description = "Возвращает информацию о системе и доступных эндпоинтах")
    public ResponseEntity<ApiResponse<HomeInfo>> getHome() {
        HomeInfo homeInfo = HomeInfo.builder()
                .applicationName("Household Store")
                .version("1.0.0")
                .status("RUNNING")
                .currentTime(LocalDateTime.now())
                .uptime(systemService.getUptime())
                .endpoints(getAvailableEndpoints())
                .build();

        ApiResponse<HomeInfo> response = ApiResponse.success(
                "Добро пожаловать в Household Store API",
                homeInfo
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Проверка здоровья системы",
            description = "Проверяет доступность сервиса и его компонентов")
    public ResponseEntity<ApiResponse<HealthStatus>> healthCheck() {
        HealthStatus healthStatus = HealthStatus.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .database(systemService.checkDatabase())
                .diskSpace(systemService.checkDiskSpace())
                .build();

        ApiResponse<HealthStatus> response = ApiResponse.success(
                "Система работает нормально",
                healthStatus
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    @Operation(summary = "Информация о системе",
            description = "Возвращает детальную информацию о системе")
    public ResponseEntity<ApiResponse<SystemInfo>> getSystemInfo() {
        SystemInfo systemInfo = SystemInfo.builder()
                .javaVersion(System.getProperty("java.version"))
                .springVersion(systemService.getSpringVersion())
                .environment(systemService.getEnvironment())
                .serverInfo(systemService.getServerInfo())
                .build();

        ApiResponse<SystemInfo> response = ApiResponse.success(
                "Информация о системе",
                systemInfo
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/ping")
    @Operation(summary = "Пинг системы",
            description = "Простая проверка доступности сервиса")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    // Вспомогательный метод для получения списка эндпоинтов
    private String[] getAvailableEndpoints() {
        return new String[] {
                "GET  /api/v1/              - Главная страница API",
                "GET  /api/v1/health        - Проверка здоровья системы",
                "GET  /api/v1/info          - Информация о системе",
                "GET  /api/v1/ping          - Пинг системы",
                "POST /api/v1/auth/login    - Аутентификация",
                "GET  /api/v1/admin/users   - Управление пользователями (требует ADMIN)",
                "GET  /api/v1/products      - Список товаров",
                "GET  /api/v1/orders        - Управление заказами"
        };
    }
}