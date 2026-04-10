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
import ru.galtor85.household_store.dto.request.order.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.request.order.ReceiveAndStockRequest;
import ru.galtor85.household_store.dto.request.order.ReverseReceiptRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierCreateRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierProductRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierUpdateRequest;
import ru.galtor85.household_store.dto.response.order.PurchaseOrderDto;
import ru.galtor85.household_store.dto.response.supplier.SupplierDto;
import ru.galtor85.household_store.dto.response.supplier.SupplierProductDto;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.manager.ManagerPurchaseService;

import java.util.HashMap;
import java.util.Map;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_MANAGER_PURCHASES;

/**
 * REST controller for manager purchase operations.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>Purchase order management (create, receive, cancel)</li>
 *   <li>Receiving goods with or without warehouse cell assignment</li>
 *   <li>Returning received goods to suppliers (reverse receipt)</li>
 *   <li>Supplier management (create, update, list)</li>
 *   <li>Supplier product catalog management</li>
 * </ul>
 *
 * <p>All endpoints require ADMIN or MANAGER role for access.</p>
 */
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping(CONTROL_MANAGER_PURCHASES)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Manager Purchase Operations", description = "Endpoints for managing purchase orders and suppliers")
public class ManagerPurchaseController extends BaseController {

    private final ManagerPurchaseService managerPurchaseService;
    private final MessageService messageService;

    // =========================================================================
    // PURCHASE ORDER MANAGEMENT
    // =========================================================================

    /**
     * Creates a new purchase order from a supplier.
     *
     * @param request purchase order creation request with items and supplier
     * @return created purchase order DTO
     */
    @PostMapping
    @Operation(summary = "Create purchase order from supplier",
            description = "Creates a new purchase order with items from the specified supplier")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderCreateRequest request) {

        User manager = getCurrentUser();
        PurchaseOrderDto purchaseOrder = managerPurchaseService.createPurchaseOrder(request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.purchase.created"),
                        purchaseOrder));
    }

    /**
     * Cancels a purchase order.
     *
     * @param orderId  purchase order ID
     * @param reason   cancellation reason
     * @param comments additional comments (optional)
     * @return cancelled purchase order DTO
     */
    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel purchase order",
            description = "Cancels a purchase order that is not yet delivered or completed")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> cancelPurchaseOrder(
            @Parameter(description = "Purchase order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "Cancellation reason", example = "Supplier cannot fulfill the order", required = true)
            @RequestParam String reason,
            @Parameter(description = "Additional comments", example = "Product discontinued")
            @RequestParam(required = false) String comments) {

        User manager = getCurrentUser();

        // Combine reason and comments if provided
        String fullReason = reason;
        if (comments != null && !comments.isEmpty()) {
            fullReason = reason + ". " + comments;
        }

        PurchaseOrderDto cancelledOrder = managerPurchaseService.cancelPurchaseOrder(
                orderId, fullReason, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.cancelled"),
                cancelledOrder));
    }

    /**
     * Reverses a purchase order receipt (returns goods to supplier).
     *
     * @param request reversal request with order ID, reason, and optional items
     * @return updated purchase order DTO
     */
    @PostMapping("/reverse-receipt")
    @Operation(summary = "Reverse purchase order receipt",
            description = "Returns received goods to supplier. Can reverse all items or specific items.")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> reverseReceipt(
            @Valid @RequestBody ReverseReceiptRequest request) {

        User manager = getCurrentUser();

        PurchaseOrderDto updatedOrder = managerPurchaseService.reverseReceipt(request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("purchase.receiving.reverse.success"),
                updatedOrder));
    }

    /**
     * Retrieves a paginated list of purchase orders with optional filters.
     *
     * @param supplierId filter by supplier ID
     * @param status     filter by order status
     * @param startDate  filter by start date
     * @param endDate    filter by end date
     * @param page       page number
     * @param size       page size
     * @return page of purchase order DTOs
     */
    @GetMapping
    @Operation(summary = "Get purchase orders",
            description = "Retrieves a paginated list of purchase orders with optional filters")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDto>>> getPurchaseOrders(
            @Parameter(description = "Supplier ID filter", example = "1")
            @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Order status filter", schema = @Schema(allowableValues =
                    {"PENDING", "PAID", "PROCESSING", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED", "PARTIALLY_RECEIVED"}))
            @RequestParam(required = false) String status,
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

        Page<PurchaseOrderDto> orders = managerPurchaseService.getPurchaseOrders(
                supplierId, status, startDate, endDate, effectivePage, effectiveSize);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchases.fetched"),
                orders));
    }

    /**
     * Retrieves a purchase order by its ID.
     *
     * @param orderId purchase order ID
     * @return purchase order DTO
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get purchase order by ID",
            description = "Retrieves detailed information about a specific purchase order")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getPurchaseOrder(
            @Parameter(description = "Purchase order ID", example = "1", required = true)
            @PathVariable Long orderId) {

        PurchaseOrderDto order = managerPurchaseService.getPurchaseOrderById(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.fetched"),
                order));
    }

    /**
     * Receives a purchase order (basic receipt without cell placement).
     *
     * @param orderId purchase order ID
     * @param request receiving request with warehouse and items
     * @return updated purchase order DTO
     */
    @PutMapping("/{orderId}/receive")
    @Operation(summary = "Receive purchase order",
            description = "Marks purchase order as received and updates inventory")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> receivePurchaseOrder(
            @Parameter(description = "Purchase order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Valid @RequestBody ReceiveAndStockRequest request) {

        User manager = getCurrentUser();
        PurchaseOrderDto order = managerPurchaseService.receivePurchaseOrder(orderId, request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.received"),
                order));
    }

    /**
     * Receives a purchase order with automatic placement into warehouse cells.
     *
     * @param orderId purchase order ID
     * @param request receiving request with cell assignments
     * @return updated purchase order DTO
     */
    @PutMapping("/{orderId}/receive-with-stock")
    @Operation(summary = "Receive purchase order and place items in warehouse cells",
            description = "Receives purchase order and automatically assigns items to warehouse cells")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> receivePurchaseOrderWithStock(
            @Parameter(description = "Purchase order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Valid @RequestBody ReceiveAndStockRequest request) {

        User manager = getCurrentUser();
        PurchaseOrderDto order = managerPurchaseService.receivePurchaseOrderWithStock(
                orderId, request, manager.getId());

        if (order.getFailedItems() != null && !order.getFailedItems().isEmpty()) {
            Map<String, Object> details = new HashMap<>();
            details.put("failedItems", order.getFailedItems());
            details.put("errorMessages", order.getErrorMessages());
            details.put("order", order);

            return ResponseEntity.status(HttpStatus.MULTI_STATUS)
                    .body(ApiResponse.error(
                            messageService.get("manager.purchase.received.with.stock.partial"),
                            details));
        }

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.received.with.stock"),
                order));
    }

    // =========================================================================
    // SUPPLIER MANAGEMENT
    // =========================================================================

    /**
     * Creates a new supplier.
     *
     * @param request supplier creation request with company details
     * @return created supplier DTO
     */
    @PostMapping("/suppliers")
    @Operation(summary = "Create new supplier",
            description = "Adds a new supplier to the system")
    public ResponseEntity<ApiResponse<SupplierDto>> createSupplier(
            @Valid @RequestBody SupplierCreateRequest request) {

        User manager = getCurrentUser();
        SupplierDto supplier = managerPurchaseService.createSupplier(request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.supplier.created"),
                        supplier));
    }

    /**
     * Updates an existing supplier.
     *
     * @param supplierId supplier ID
     * @param request    supplier update request
     * @return updated supplier DTO
     */
    @PutMapping("/suppliers/{supplierId}")
    @Operation(summary = "Update supplier information",
            description = "Updates an existing supplier's details")
    public ResponseEntity<ApiResponse<SupplierDto>> updateSupplier(
            @Parameter(description = "Supplier ID", example = "1", required = true)
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierUpdateRequest request) {

        SupplierDto supplier = managerPurchaseService.updateSupplier(supplierId, request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.supplier.updated"),
                supplier));
    }

    /**
     * Retrieves a paginated list of suppliers with optional filters.
     *
     * @param name   filter by supplier name
     * @param status filter by supplier status
     * @param page   page number
     * @param size   page size
     * @return page of supplier DTOs
     */
    @GetMapping("/suppliers")
    @Operation(summary = "Get list of suppliers",
            description = "Retrieves a paginated list of suppliers with optional filters")
    public ResponseEntity<ApiResponse<Page<SupplierDto>>> getSuppliers(
            @Parameter(description = "Supplier name filter", example = "TechnoPost")
            @RequestParam(required = false) String name,
            @Parameter(description = "Supplier status filter", schema = @Schema(allowableValues = {"PENDING", "ACTIVE", "INACTIVE", "BLOCKED", "VERIFIED"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(required = false) Integer size) {

        int effectivePage = getPage(page);
        int effectiveSize = getSize(size);

        Page<SupplierDto> suppliers = managerPurchaseService.getSuppliers(name, status, effectivePage, effectiveSize);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.suppliers.fetched"),
                suppliers));
    }

    /**
     * Adds a product to a supplier's catalog.
     *
     * @param supplierId supplier ID
     * @param productId  product ID
     * @param request    supplier product request with price and SKU
     * @return created supplier product DTO
     */
    @PostMapping("/suppliers/{supplierId}/products/{productId}")
    @Operation(summary = "Add product to supplier catalog",
            description = "Associates a product with a supplier, setting supplier-specific pricing and SKU")
    public ResponseEntity<ApiResponse<SupplierProductDto>> addProductToSupplier(
            @Parameter(description = "Supplier ID", example = "1", required = true)
            @PathVariable Long supplierId,
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody SupplierProductRequest request) {

        User manager = getCurrentUser();
        SupplierProductDto supplierProduct = managerPurchaseService.addProductToSupplier(
                supplierId, productId, request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.supplier.product.added"),
                        supplierProduct));
    }
}