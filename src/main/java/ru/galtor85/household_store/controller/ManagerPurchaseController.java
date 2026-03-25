package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import ru.galtor85.household_store.advice.exception.CustomAuthenticationException;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.*;

import java.util.List;

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

    // ========== PURCHASE ORDER MANAGEMENT ==========

    @PostMapping
    @Operation(summary = "Create purchase order from supplier")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderCreateRequest request) {

        User manager = getCurrentManager();
        PurchaseOrderDto purchaseOrder = managerPurchaseService.createPurchaseOrder(request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.purchase.created"),
                        purchaseOrder));
    }

    @GetMapping
    @Operation(summary = "Get purchase orders")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDto>>> getPurchaseOrders(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
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
            @PathVariable Long orderId) {

        PurchaseOrderDto order = managerPurchaseService.getPurchaseOrderById(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.fetched"),
                order));
    }

    @PutMapping("/{orderId}/receive")
    @Operation(summary = "Receive purchase order")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> receivePurchaseOrder(
            @PathVariable Long orderId,
            @RequestBody ReceiveAndStockRequest request) {

        User manager = getCurrentManager();
        PurchaseOrderDto order = managerPurchaseService.receivePurchaseOrder(orderId, request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.received"),
                order));
    }

    @PutMapping("/{orderId}/receive-with-stock")
    @Operation(summary = "Receive purchase order and place items in warehouse cells")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> receivePurchaseOrderWithStock(
            @PathVariable Long orderId,
            @Valid @RequestBody ReceiveAndStockRequest request) {

        User manager = getCurrentManager();
        PurchaseOrderDto order = managerPurchaseService.receivePurchaseOrderWithStock(
                orderId, request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.received.with.stock"),
                order));
    }

    // ========== SUPPLIER MANAGEMENT ==========

    @PostMapping("/suppliers")
    @Operation(summary = "Create new supplier")
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
    @Operation(summary = "Update supplier information")
    public ResponseEntity<ApiResponse<SupplierDto>> updateSupplier(
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierUpdateRequest request) {

        SupplierDto supplier = managerPurchaseService.updateSupplier(supplierId, request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.supplier.updated"),
                supplier));
    }

    @GetMapping("/suppliers")
    @Operation(summary = "Get list of suppliers")
    public ResponseEntity<ApiResponse<Page<SupplierDto>>> getSuppliers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<SupplierDto> suppliers = managerPurchaseService.getSuppliers(name, status, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.suppliers.fetched"),
                suppliers));
    }

    @PostMapping("/suppliers/{supplierId}/products/{productId}")
    @Operation(summary = "Add product to supplier catalog")
    public ResponseEntity<ApiResponse<SupplierProductDto>> addProductToSupplier(
            @PathVariable Long supplierId,
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