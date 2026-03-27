package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.advice.exception.auth.CustomAuthenticationException;
import ru.galtor85.household_store.dto.response.order.RollbackApprovalDto;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.dto.request.order.RollbackRequest;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.manager.ManagerSalesOrderService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.rollback.RollbackService;
import ru.galtor85.household_store.service.user.UserSearchService;

import java.math.BigDecimal;

@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/api/v1/manager/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Manager Sales Operations", description = "Endpoints for managing customer orders")
public class ManagerSalesOrderController {

    private final ManagerSalesOrderService managerSalesOrderService;
    private final UserSearchService userSearchService;
    private final MessageService messageService;
    private final RollbackService rollbackService;

    private SecurityUser getCurrentSecurityUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomAuthenticationException(
                    messageService.get("manager.error.not.authenticated"));
        }
        return (SecurityUser) auth.getPrincipal();
    }

    private User getCurrentManager() {
        SecurityUser securityUser = getCurrentSecurityUser();
        return userSearchService.getUserById(securityUser.getUserId());
    }

    // ========== CUSTOMER ORDER MANAGEMENT ==========

    @GetMapping
    @Operation(summary = "Get customer orders")
    public ResponseEntity<ApiResponse<Page<SalesOrderDto>>> getCustomerOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<SalesOrderDto> orders = managerSalesOrderService.getCustomerOrders(
                 customerId, status, startDate, endDate, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.orders.fetched"),
                orders));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get customer order by ID")
    public ResponseEntity<ApiResponse<SalesOrderDto>> getCustomerOrder(
            @PathVariable Long orderId) {

        SalesOrderDto order = managerSalesOrderService.getSalesOrderById(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.fetched"),
                order));
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<SalesOrderDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @Parameter(
                    description = "New status for the order",
                    example = "PROCESSING"
            )
            @RequestParam
            @Schema(allowableValues = {"PENDING", "PAID", "PROCESSING", "DELIVERED",
                    "COMPLETED", "CANCELLED", "SHIPPED", "REFUNDED", "RETURNED"})
            String status,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false) String reason) {

        User manager = getCurrentManager();
        SalesOrderDto order = managerSalesOrderService.updateOrderStatus(
                orderId, status, trackingNumber, reason, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.status.updated"),
                order));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel customer order")
    public ResponseEntity<ApiResponse<SalesOrderDto>> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam String reason) {

        User manager = getCurrentManager();
        SalesOrderDto order = managerSalesOrderService.cancelOrder(orderId, reason, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.cancelled"),
                order));
    }

    @PutMapping("/{orderId}/items/{itemId}/price")
    @Operation(summary = "Update order item price")
    public ResponseEntity<ApiResponse<SalesOrderDto>> updateOrderItemPrice(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam BigDecimal newPrice,
            @RequestParam String reason) {

        User manager = getCurrentManager();
        SalesOrderDto order = managerSalesOrderService.updateOrderItemPrice(
                orderId, itemId, newPrice, reason, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.price.updated"),
                order));
    }

    @PostMapping("/{orderId}/rollback-request")
    @Operation(summary = "Request order status rollback")
    public ResponseEntity<ApiResponse<RollbackApprovalDto>> requestRollback(
            @PathVariable Long orderId,
            @Valid @RequestBody RollbackRequest request) {

        User manager = getCurrentManager();
        request.setOrderId(orderId);
        RollbackApprovalDto approval = rollbackService.requestRollback(
                request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.rollback.requested"),
                approval));
    }

    @PostMapping("/{orderId}/notes")
    @Operation(summary = "Add note to order")
    public ResponseEntity<ApiResponse<SalesOrderDto>> addOrderNote(
            @PathVariable Long orderId,
            @RequestParam String note) {

        User manager = getCurrentManager();
        SalesOrderDto order = managerSalesOrderService.addOrderNote(orderId, note, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.note.added"),
                order));
    }
}