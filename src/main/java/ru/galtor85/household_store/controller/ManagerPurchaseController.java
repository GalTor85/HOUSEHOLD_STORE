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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.advice.exception.auth.CustomAuthenticationException;
import ru.galtor85.household_store.dto.request.order.ReverseReceiptRequest;
import ru.galtor85.household_store.dto.response.order.PurchaseOrderDto;
import ru.galtor85.household_store.dto.request.order.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.request.order.ReceiveAndStockRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierCreateRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierProductRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierUpdateRequest;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.dto.response.supplier.SupplierDto;
import ru.galtor85.household_store.dto.response.supplier.SupplierProductDto;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.manager.ManagerPurchaseService;
import ru.galtor85.household_store.service.user.UserSearchService;

@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/api/v1/manager/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Manager Purchase Operations", description = "Endpoints for managing purchase orders and suppliers")
public class ManagerPurchaseController {

    private final ManagerPurchaseService managerPurchaseService;
    private final UserSearchService userSearchService;
    private final MessageService messageService;

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

    // =========================================================================
    // PURCHASE ORDER MANAGEMENT
    // =========================================================================

    @PostMapping
    @Operation(summary = "Create purchase order from supplier",
            description = "Creates a new purchase order with items from the specified supplier")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderCreateRequest request) {

        User manager = getCurrentManager();
        PurchaseOrderDto purchaseOrder = managerPurchaseService.createPurchaseOrder(request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.purchase.created"),
                        purchaseOrder));
    }

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

        User manager = getCurrentManager();

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

    @PostMapping("/reverse-receipt")
    @Operation(summary = "Reverse purchase order receipt",
            description = "Returns received goods to supplier. Can reverse all items or specific items.")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> reverseReceipt(
            @Valid @RequestBody ReverseReceiptRequest request) {

        User manager = getCurrentManager();

        PurchaseOrderDto updatedOrder = managerPurchaseService.reverseReceipt(request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("purchase.receiving.reverse.success"),
                updatedOrder));
    }

    @GetMapping
    @Operation(summary = "Get purchase orders",
            description = "Retrieves a paginated list of purchase orders with optional filters")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDto>>> getPurchaseOrders(
            @Parameter(description = "Supplier ID filter", example = "1")
            @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Order status filter", schema = @Schema(allowableValues =
                    {"PENDING", "PAID", "PROCESSING", "SHIPPED",
                            "DELIVERED", "COMPLETED", "CANCELLED", "PARTIALLY_RECEIVED"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "Start date (ISO format)", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "End date (ISO format)", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) String endDate,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Page<PurchaseOrderDto> orders = managerPurchaseService.getPurchaseOrders(
                supplierId, status, startDate, endDate, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchases.fetched"),
                orders));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get purchase order by ID")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getPurchaseOrder(
            @Parameter(description = "Purchase order ID", example = "1", required = true)
            @PathVariable Long orderId) {

        PurchaseOrderDto order = managerPurchaseService.getPurchaseOrderById(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.fetched"),
                order));
    }

    @PutMapping("/{orderId}/receive")
    @Operation(summary = "Receive purchase order",
            description = "Marks purchase order as received and updates inventory")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> receivePurchaseOrder(
            @Parameter(description = "Purchase order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Valid @RequestBody ReceiveAndStockRequest request) {

        User manager = getCurrentManager();
        PurchaseOrderDto order = managerPurchaseService.receivePurchaseOrder(orderId, request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.received"),
                order));
    }

    @PutMapping("/{orderId}/receive-with-stock")
    @Operation(summary = "Receive purchase order and place items in warehouse cells",
            description = "Receives purchase order and automatically assigns items to warehouse cells")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> receivePurchaseOrderWithStock(
            @Parameter(description = "Purchase order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Valid @RequestBody ReceiveAndStockRequest request) {

        User manager = getCurrentManager();
        PurchaseOrderDto order = managerPurchaseService.receivePurchaseOrderWithStock(
                orderId, request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.received.with.stock"),
                order));
    }

    // =========================================================================
    // SUPPLIER MANAGEMENT
    // =========================================================================

    @PostMapping("/suppliers")
    @Operation(summary = "Create new supplier",
            description = "Adds a new supplier to the system")
    public ResponseEntity<ApiResponse<SupplierDto>> createSupplier(
            @Valid @RequestBody SupplierCreateRequest request) {

        User manager = getCurrentManager();
        SupplierDto supplier = managerPurchaseService.createSupplier(request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.supplier.created"),
                        supplier));
    }

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

    @GetMapping("/suppliers")
    @Operation(summary = "Get list of suppliers",
            description = "Retrieves a paginated list of suppliers with optional filters")
    public ResponseEntity<ApiResponse<Page<SupplierDto>>> getSuppliers(
            @Parameter(description = "Supplier name filter", example = "TechnoPost")
            @RequestParam(required = false) String name,
            @Parameter(description = "Supplier status filter", schema = @Schema(allowableValues = {"PENDING", "ACTIVE", "INACTIVE", "BLOCKED", "VERIFIED"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Page<SupplierDto> suppliers = managerPurchaseService.getSuppliers(name, status, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.suppliers.fetched"),
                suppliers));
    }

    @PostMapping("/suppliers/{supplierId}/products/{productId}")
    @Operation(summary = "Add product to supplier catalog",
            description = "Associates a product with a supplier, setting supplier-specific pricing and SKU")
    public ResponseEntity<ApiResponse<SupplierProductDto>> addProductToSupplier(
            @Parameter(description = "Supplier ID", example = "1", required = true)
            @PathVariable Long supplierId,
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody SupplierProductRequest request) {

        User manager = getCurrentManager();
        SupplierProductDto supplierProduct = managerPurchaseService.addProductToSupplier(
                supplierId, productId, request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.supplier.product.added"),
                        supplierProduct));
    }
}