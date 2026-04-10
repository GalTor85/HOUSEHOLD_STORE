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

import static ru.galtor85.household_store.constants.EndpointConstants.*;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_ENDPOINT_LENGTH;

/**
 * REST controller for home and system information endpoints.
 *
 * <p>This controller provides public endpoints for:</p>
 * <ul>
 *   <li>System information and API documentation</li>
 *   <li>Health checks for monitoring</li>
 *   <li>System details (versions, environment, uptime)</li>
 *   <li>Simple ping endpoint for connectivity testing</li>
 * </ul>
 *
 * <p>All endpoints are public and do not require authentication.</p>
 */
@Slf4j
@RestController
@RequestMapping(CONTROL_HOME)
@RequiredArgsConstructor
@Tag(name = "Home", description = "API for getting system information")
public class HomeRestController {

    private final SystemService systemService;
    private final MessageService messageService;

    /**
     * Retrieves home page information including system status and available endpoints.
     *
     * @return home information DTO with application details and endpoint list
     */
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

    /**
     * Performs a health check on the system and its components.
     *
     * @return health status DTO with database and disk space status
     */
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

    /**
     * Retrieves detailed system information.
     *
     * @return system info DTO with Java version, Spring version, environment, and server info
     */
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

    /**
     * Simple ping endpoint for connectivity testing.
     *
     * @returns "pong" response to indicate service is alive
     */
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

    /**
     * Builds an array of available API endpoints for documentation purposes.
     *
     * @return array of endpoint descriptions
     */
    private String[] getAvailableEndpoints() {
        int maxLength = MAX_ENDPOINT_LENGTH;
        return new String[] {
                formatEndpoint("GET", HOME, maxLength, "home-rest-controller.endpoint.home"),
                formatEndpoint("GET", HEALTH, maxLength, "home-rest-controller.endpoint.health"),
                formatEndpoint("GET", INFO, maxLength, "home-rest-controller.endpoint.info"),
                formatEndpoint("GET", PING, maxLength, "home-rest-controller.endpoint.ping"),
                formatEndpoint("POST", AUTH_LOGIN, maxLength, "home-rest-controller.endpoint.login"),
                formatEndpoint("POST", AUTH_REGISTER, maxLength, "home-rest-controller.endpoint.register"),
                formatEndpoint("GET", ADMIN_USERS, maxLength, "home-rest-controller.endpoint.admin.users"),
                formatEndpoint("GET", MANAGER_PRODUCTS, maxLength, "home-rest-controller.endpoint.products"),
                formatEndpoint("GET", MANAGER_ORDERS, maxLength, "home-rest-controller.endpoint.orders"),
                formatEndpoint("GET", USER_ME, maxLength, "home-rest-controller.endpoint.user.me"),
                formatEndpoint("GET", USER_CART, maxLength, "home-rest-controller.endpoint.user.cart"),
                formatEndpoint("POST", USER_ORDER_FROM_CART, maxLength, "home-rest-controller.endpoint.user.order.from.cart"),
                formatEndpoint("GET", FINANCE_INVOICES, maxLength, "home-rest-controller.endpoint.finance.invoices"),
                formatEndpoint("GET", CURRENCIES, maxLength, "home-rest-controller.endpoint.currencies"),
                formatEndpoint("GET", BANK_ACCOUNTS, maxLength, "home-rest-controller.endpoint.bank.accounts"),
                formatEndpoint("GET", MANAGER_PURCHASE_ROOT, maxLength, "home-rest-controller.endpoint.manager.purchases"),
                formatEndpoint("POST", MANAGER_PAYMENT_SUPPLIER_BANK, maxLength, "home-rest-controller.endpoint.manager.payment.supplier.bank"),
                formatEndpoint("POST", MANAGER_PAYMENT_CUSTOMER_CASH, maxLength, "home-rest-controller.endpoint.manager.payment.customer.cash"),
                formatEndpoint("GET", MANAGER_STOCK_SUMMARY, maxLength, "home-rest-controller.endpoint.manager.stock.summary"),
                formatEndpoint("GET", MANAGER_WAREHOUSES, maxLength, "home-rest-controller.endpoint.manager.warehouses"),
                formatEndpoint("GET", MEDIA_PRODUCT, maxLength, "home-rest-controller.endpoint.media.product")
        };
    }

    private String formatEndpoint(String method, String path, int maxLength, String messageKey) {
        int padding = maxLength - path.length() + 2;
        String spaces = " ".repeat(Math.max(1, padding));
        return method + "  " + path + spaces + "- " + messageService.get(messageKey);
    }
}