package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.dto.response.system.HealthStatus;
import ru.galtor85.household_store.dto.response.system.HomeInfo;
import ru.galtor85.household_store.dto.response.system.SystemInfo;
import ru.galtor85.household_store.service.system.SystemService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Home", description = "API for getting system information")
public class HomeRestController {

    private final SystemService systemService;
    private final MessageService messageService;

    @GetMapping("/")
    @Operation(summary = "Home page API",
            description = "Returns system information and available endpoints")
    public ResponseEntity<ApiResponse<HomeInfo>> getHome() {

        HomeInfo homeInfo = HomeInfo.builder()
                .applicationName(messageService.get("home-rest-controller.app.name"))
                .version(messageService.get("home-rest-controller.app.version"))
                .status(messageService.get("home-rest-controller.app.status.running"))
                .currentTime(LocalDateTime.now())
                .uptime(systemService.getUptime())
                .endpoints(getAvailableEndpoints())
                .build();

        String welcomeMessage = messageService.get("home-rest-controller.home.welcome");
        ApiResponse<HomeInfo> response = ApiResponse.success(welcomeMessage, homeInfo);

        log.info(messageService.get("home-rest-controller.log.home.requested"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "System health check",
            description = "Checks the availability of the service and its components")
    public ResponseEntity<ApiResponse<HealthStatus>> healthCheck() {

        HealthStatus healthStatus = HealthStatus.builder()
                .status(messageService.get("home-rest-controller.health.status.up"))
                .timestamp(LocalDateTime.now())
                .database(systemService.checkDatabase())
                .diskSpace(systemService.checkDiskSpace())
                .build();

        String healthMessage = messageService.get("home-rest-controller.health.message.normal");
        ApiResponse<HealthStatus> response = ApiResponse.success(healthMessage, healthStatus);

        log.info(messageService.get("home-rest-controller.log.health.requested"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    @Operation(summary = "System information",
            description = "Returns detailed information about the system")
    public ResponseEntity<ApiResponse<SystemInfo>> getSystemInfo() {

        SystemInfo systemInfo = SystemInfo.builder()
                .javaVersion(System.getProperty("java.version"))
                .springVersion(systemService.getSpringVersion())
                .environment(systemService.getEnvironment())
                .serverInfo(systemService.getServerInfo())
                .uptime(systemService.getUptime())
                .build();

        String infoMessage = messageService.get("home-rest-controller.info.message");
        ApiResponse<SystemInfo> response = ApiResponse.success(infoMessage, systemInfo);

        log.info(messageService.get("home-rest-controller.log.info.requested"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ping")
    @Operation(summary = "System ping",
            description = "Simple availability check of the service")
    public ResponseEntity<ApiResponse<String>> ping() {

        log.info(messageService.get("home-rest-controller.log.ping.requested"));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("home-rest-controller.ping.response"),
                "pong"
        ));
    }

    private String[] getAvailableEndpoints() {
        return new String[] {
                "GET  /api/v1/              - " + messageService.get("home-rest-controller.endpoint.home"),
                "GET  /api/v1/health        - " + messageService.get("home-rest-controller.endpoint.health"),
                "GET  /api/v1/info          - " + messageService.get("home-rest-controller.endpoint.info"),
                "GET  /api/v1/ping          - " + messageService.get("home-rest-controller.endpoint.ping"),
                "POST /api/v1/auth/login    - " + messageService.get("home-rest-controller.endpoint.login"),
                "POST /api/v1/auth/register - " + messageService.get("home-rest-controller.endpoint.register"),
                "GET  /api/v1/admin/users   - " + messageService.get("home-rest-controller.endpoint.admin.users"),
                "GET  /api/v1/products      - " + messageService.get("home-rest-controller.endpoint.products"),
                "GET  /api/v1/orders        - " + messageService.get("home-rest-controller.endpoint.orders")
        };
    }
}