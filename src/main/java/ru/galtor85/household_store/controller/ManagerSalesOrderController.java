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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.dto.response.order.RollbackApprovalDto;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.dto.request.order.RollbackRequest;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.service.order.SalesOrderService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.rollback.RollbackService;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_MANAGER_ORDERS;

/**
 * REST controller for manager sales order operations.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>Viewing customer orders with filtering and pagination</li>
 *   <li>Creating customer orders on behalf of customers</li>
 *   <li>Updating order status (processing, shipping, delivery, cancellation)</li>
 *   <li>Managing order items (price updates)</li>
 *   <li>Requesting order status rollbacks (requires admin approval)</li>
 *   <li>Adding internal notes to orders</li>
 * </ul>
 *
 * <p>All endpoints require ADMIN or MANAGER role for access.</p>
 */
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping(CONTROL_MANAGER_ORDERS)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Manager Sales Operations", description = "Endpoints for managing customer orders")
public class ManagerSalesOrderController extends BaseController {

    private final SalesOrderService salesOrderService;
    private final MessageService messageService;
    private final RollbackService rollbackService;

    // =========================================================================
    // CUSTOMER ORDER MANAGEMENT
    // =========================================================================

    /**
     * Retrieves a paginated list of customer orders with optional filters.
     *
     * @param status     filter by order status
     * @param customerId filter by customer ID
     * @param startDate  filter by start date
     * @param endDate    filter by end date
     * @param page       page number (0-indexed)
     * @param size       page size
     * @return page of sales order DTOs
     */
    @GetMapping
    @Operation(summary = "Get customer orders",
            description = "Retrieves a paginated list of customer orders with optional filters")
    public ResponseEntity<ApiResponse<Page<SalesOrderDto>>> getCustomerOrders(
            @Parameter(description = "Order status filter", schema = @Schema(allowableValues = {"PENDING", "PAID",
                    "PROCESSING", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED", "REFUNDED", "RETURNED"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "Customer ID filter", example = "1")
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "Start date (ISO format)", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "End date (ISO format)", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) String endDate,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(required = false) Integer size) {

        int effectivePage = getPage(page);
        int effectiveSize = getSize(size);

        Page<SalesOrderDto> orders = salesOrderService.getCustomerOrders(
                customerId, status, startDate, endDate, effectivePage, effectiveSize);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.orders.fetched"),
                orders));
    }

    /**
     * Retrieves a customer order by its ID.
     *
     * @param orderId order ID
     * @return sales order DTO
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get customer order by ID",
            description = "Retrieves detailed information about a specific customer order")
    public ResponseEntity<ApiResponse<SalesOrderDto>> getCustomerOrder(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long orderId) {

        SalesOrderDto order = salesOrderService.getSalesOrderById(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.fetched"),
                order));
    }

    /**
     * Creates a new customer order (manager creates on behalf of customer).
     *
     * @param request order creation request with items and customer info
     * @return created sales order DTO
     */
    @PostMapping
    @Operation(summary = "Create customer order (for managers)",
            description = "Allows manager to create a customer order with discounts, shipping and tax")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<SalesOrderDto>> createCustomerOrder(
            @Valid @RequestBody SalesOrderCreateRequest request) {

        User manager = getCurrentUser();
        log.info(messageService.get("manager.order.create.start", manager.getEmail(), request.getUserId()));

        SalesOrderDto order = salesOrderService.createSalesOrder(request, manager.getId());

        String message = request.hasDiscount() ?
                messageService.get("manager.order.created.with.discount") :
                messageService.get("manager.order.created");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, order));
    }

    /**
     * Updates the status of a customer order.
     *
     * @param orderId        order ID
     * @param status         new status
     * @param trackingNumber tracking number (required for SHIPPED status)
     * @param reason         reason (required for CANCELLED or REFUNDED status)
     * @return updated sales order DTO
     */
    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status",
            description = "Updates the status of an existing customer order")
    public ResponseEntity<ApiResponse<SalesOrderDto>> updateOrderStatus(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Parameter(
                    description = "New status for the order",
                    example = "PROCESSING",
                    required = true,
                    schema = @Schema(allowableValues = {"PENDING", "PAID", "PROCESSING", "SHIPPED", "DELIVERED",
                            "COMPLETED", "CANCELLED", "REFUNDED", "RETURNED"})
            )
            @RequestParam String status,
            @Parameter(description = "Tracking number (for SHIPPED status)", example = "RU123456789")
            @RequestParam(required = false) String trackingNumber,
            @Parameter(description = "Reason (for CANCELLED or REFUNDED status)", example = "Customer request")
            @RequestParam(required = false) String reason) {

        User manager = getCurrentUser();
        SalesOrderDto order = salesOrderService.updateOrderStatus(
                orderId, status, trackingNumber, reason, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.status.updated"),
                order));
    }

    /**
     * Cancels a customer order.
     *
     * @param orderId order ID
     * @param reason  cancellation reason
     * @return cancelled sales order DTO
     */
    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel customer order",
            description = "Cancels an existing customer order")
    public ResponseEntity<ApiResponse<SalesOrderDto>> cancelOrder(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "Cancellation reason", example = "Out of stock", required = true)
            @RequestParam String reason) {

        User manager = getCurrentUser();
        SalesOrderDto order = salesOrderService.cancelOrder(orderId, reason, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.cancelled"),
                order));
    }

    /**
     * Updates the price of an item in a customer order.
     *
     * @param orderId  order ID
     * @param itemId   order item ID
     * @param newPrice new price for the item
     * @param reason   reason for price change
     * @return updated sales order DTO
     */
    @PutMapping("/{orderId}/items/{itemId}/price")
    @Operation(summary = "Update order item price",
            description = "Updates the price of a specific item in an order")
    public ResponseEntity<ApiResponse<SalesOrderDto>> updateOrderItemPrice(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "Order item ID", example = "1", required = true)
            @PathVariable Long itemId,
            @Parameter(description = "New price", example = "999.99", required = true)
            @RequestParam BigDecimal newPrice,
            @Parameter(description = "Reason for price change", example = "Manager discount", required = true)
            @RequestParam String reason) {

        User manager = getCurrentUser();
        SalesOrderDto order = salesOrderService.updateOrderItemPrice(
                orderId, itemId, newPrice, reason, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.price.updated"),
                order));
    }

    /**
     * Requests a rollback of the order status (requires admin approval).
     *
     * @param orderId order ID
     * @param request rollback request with reason and comments
     * @return rollback approval DTO
     */
    @PostMapping("/{orderId}/rollback-request")
    @Operation(summary = "Request order status rollback",
            description = "Requests a rollback of the order status (requires admin approval)")
    public ResponseEntity<ApiResponse<RollbackApprovalDto>> requestRollback(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Valid @RequestBody RollbackRequest request) {

        User manager = getCurrentUser();
        request.setOrderId(orderId);
        RollbackApprovalDto approval = rollbackService.requestRollback(
                request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.rollback.requested"),
                approval));
    }

    /**
     * Adds an internal note to a customer order.
     *
     * @param orderId order ID
     * @param note    note text
     * @return updated sales order DTO
     */
    @PostMapping("/{orderId}/notes")
    @Operation(summary = "Add note to order",
            description = "Adds a manager note to the customer order")
    public ResponseEntity<ApiResponse<SalesOrderDto>> addOrderNote(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "Note text", example = "Customer requested gift wrapping", required = true)
            @RequestParam String note) {

        User manager = getCurrentUser();
        SalesOrderDto order = salesOrderService.addOrderNote(orderId, note, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.note.added"),
                order));
    }
}