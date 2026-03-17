package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.CustomAuthenticationException;
import ru.galtor85.household_store.advice.exception.WarehouseNotFoundException;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Manager Operations", description = "Endpoints for store managers to manage products, purchases, inventory, and orders")
public class ManagerController {

    private final ManagerProductService managerProductService;
    private final ManagerPurchaseService managerPurchaseService;
    private final ManagerOrderService managerOrderService;
    private final UserSearchService userSearchService;
    private final MessageService messageService;
    private final WarehouseService warehouseService;

    private SecurityUser getCurrentSecurityUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomAuthenticationException(
                    messageService.get("manager.error.not.authenticated"));
        }
        return (SecurityUser) auth.getPrincipal();
    }

    private User getCurrentManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser securityUser = getCurrentSecurityUser();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException(messageService.get("manager.error.not.authenticated"));
        }        return userSearchService.getUserById(securityUser.getUserId());
    }

    // ========== PRODUCT MANAGEMENT ==========

    @PostMapping("/products")
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        ProductDto product = managerProductService.createProduct(request, manager.getId(), locale);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.product.created"),
                        product));
    }

    @PutMapping("/products/{productId}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        ProductDto product = managerProductService.updateProduct(productId, request, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.updated"),
                product));
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(
            @PathVariable Long productId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        ProductDto product = managerProductService.getProductById(productId, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.fetched"),
                product));
    }

    @GetMapping("/products")
    @Operation(summary = "Get paginated list of products")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {


        Page<ProductDto> products = managerProductService.getProducts(
                name, category, brand, active, page, size, sortBy, sortDir, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.products.fetched"),
                products));
    }

    @PatchMapping("/products/{productId}/stock")
    @Operation(summary = "Adjust product stock")
    public ResponseEntity<ApiResponse<ProductDto>> adjustStock(
            @PathVariable Long productId,
            @RequestParam int quantity,
            @RequestParam(required = false) String reason,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        ProductDto product = managerProductService.adjustStock(productId, quantity, reason, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.stock.adjusted"),
                product));
    }

    @PatchMapping("/products/{productId}/price")
    @Operation(summary = "Update product price")
    public ResponseEntity<ApiResponse<ProductDto>> updatePrice(
            @PathVariable Long productId,
            @RequestParam BigDecimal newPrice,
            @RequestParam(required = false) String reason,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        ProductDto product = managerProductService.updatePrice(productId, newPrice, reason, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.price.updated"),
                product));
    }

    @PatchMapping("/products/{productId}/toggle")
    @Operation(summary = "Toggle product active status")
    public ResponseEntity<ApiResponse<ProductDto>> toggleProductActive(
            @PathVariable Long productId,
            @RequestParam boolean active,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        ProductDto product = managerProductService.toggleProductActive(productId, active, locale);

        String messageKey = active ? "manager.product.activated" : "manager.product.deactivated";
        return ResponseEntity.ok(ApiResponse.success(
                messageService.get(messageKey),
                product));
    }

    @PostMapping("/products/{productId}/media")
    @Operation(summary = "Upload media files for a product")
    public ResponseEntity<ApiResponse<List<ProductMediaDto>>> uploadProductMedia(
            @PathVariable
            @Parameter(description = "Product ID", example = "1", required = true)
            Long productId,

            @RequestParam("files")
            @Parameter(description = "Media files to upload")
            MultipartFile[] files,

            @RequestParam(required = false)
            @Parameter(description = "Media metadata as JSON array")
            String metadata,

            @RequestHeader(name = "Accept-Language", required = false)
            Locale locale) {

        User manager = getCurrentManager();

        // Добавим логирование для отладки
        log.info("=== ЗАГРУЗКА ФАЙЛОВ ===");
        log.info("Product ID: {}", productId);
        log.info("Manager ID: {}", manager.getId());
        log.info("Locale: {}", locale);

        if (files != null) {
            log.info("Количество файлов: {}", files.length);
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                log.info("Файл {}: {}", i, file.getOriginalFilename());
                log.info("  - Размер: {} байт", file.getSize());
                log.info("  - Content-Type: {}", file.getContentType());
                log.info("  - Пустой: {}", file.isEmpty());
            }
        } else {
            log.info("files = null");
        }

        log.info("Metadata: {}", metadata);

        // КОНВЕРТИРУЕМ МАССИВ В СПИСОК
        List<MultipartFile> fileList = Arrays.asList(files);

        List<ProductMediaDto> media = managerProductService.uploadMedia(
                productId, fileList, metadata, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.media.uploaded"),
                media));
    }

    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete a media file")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable Long mediaId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        managerProductService.deleteMedia(mediaId, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.media.deleted"),
                null));
    }

    @PutMapping("/media/{mediaId}/main")
    @Operation(summary = "Set media as main image")
    public ResponseEntity<ApiResponse<Void>> setMainMedia(
            @PathVariable Long mediaId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        managerProductService.setMainMedia(mediaId, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.media.main.set"),
                null));
    }

    // ========== PURCHASE MANAGEMENT ==========

    @PostMapping("/purchases")
    @Operation(summary = "Create purchase order from supplier")
    public ResponseEntity<ApiResponse<OrderDto>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderCreateRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        OrderDto purchaseOrder = managerPurchaseService.createPurchaseOrder(request, manager.getId(), locale);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.purchase.created"),
                        purchaseOrder));
    }

    @GetMapping("/purchases")
    @Operation(summary = "Get purchase orders")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getPurchaseOrders(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        Page<OrderDto> orders = managerPurchaseService.getPurchaseOrders(
                supplierId, status, startDate, endDate, page, size, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchases.fetched"),
                orders));
    }

    @GetMapping("/purchases/{orderId}")
    @Operation(summary = "Get purchase order by ID")
    public ResponseEntity<ApiResponse<OrderDto>> getPurchaseOrder(
            @PathVariable Long orderId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        OrderDto order = managerPurchaseService.getPurchaseOrderById(orderId, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.fetched"),
                order));
    }

    @PutMapping("/purchases/{orderId}/receive")
    @Operation(summary = "Receive purchase order")
    public ResponseEntity<ApiResponse<OrderDto>> receivePurchaseOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) ReceiveOrderRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        OrderDto order = managerPurchaseService.receivePurchaseOrder(orderId, request, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.received"),
                order));
    }

    // ========== INVENTORY MANAGEMENT ==========

    @PostMapping("/inventory/write-off")
    @Operation(summary = "Write off damaged/lost stock")
    public ResponseEntity<ApiResponse<Void>> writeOffStock(
            @Valid @RequestBody StockWriteOffRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        managerPurchaseService.writeOffStock(request, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.stock.writeoff.completed"),
                null));
    }

    @GetMapping("/inventory/low-stock")
    @Operation(summary = "Get low stock products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        List<ProductDto> products = managerProductService.getLowStockProducts(threshold, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.inventory.low.stock"),
                products));
    }

    // ========== ORDER MANAGEMENT ==========

    @GetMapping("/orders")
    @Operation(summary = "Get customer orders")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getCustomerOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        Page<OrderDto> orders = managerOrderService.getCustomerOrders(
                status, customerId, startDate, endDate, page, size, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.orders.fetched"),
                orders));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get customer order by ID")
    public ResponseEntity<ApiResponse<OrderDto>> getCustomerOrder(
            @PathVariable Long orderId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        OrderDto order = managerOrderService.getCustomerOrderById(orderId, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.fetched"),
                order));
    }


    @PutMapping("/orders/{orderId}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false) String reason,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        OrderDto order = managerOrderService.updateOrderStatus(
                orderId, status, trackingNumber, reason, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.status.updated"),
                order));
    }

    @PutMapping("/orders/{orderId}/items/{itemId}/price")
    @Operation(summary = "Update order item price")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderItemPrice(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam BigDecimal newPrice,
            @RequestParam String reason,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        OrderDto order = managerOrderService.updateOrderItemPrice(
                orderId, itemId, newPrice, reason, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.price.updated"),
                order));
    }

    @PutMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Cancel customer order")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam String reason,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        OrderDto order = managerOrderService.cancelOrder(orderId, reason, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.order.cancelled"),
                order));
    }

    // ========== SUPPLIER MANAGEMENT ==========

    @PostMapping("/suppliers")
    @Operation(summary = "Create new supplier")
    public ResponseEntity<ApiResponse<SupplierDto>> createSupplier(
            @Valid @RequestBody SupplierCreateRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        SupplierDto supplier = managerPurchaseService.createSupplier(request, manager.getId(), locale);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.supplier.created"),
                        supplier));
    }

    @PutMapping("/suppliers/{supplierId}")
    @Operation(summary = "Update supplier information")
    public ResponseEntity<ApiResponse<SupplierDto>> updateSupplier(
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierUpdateRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        SupplierDto supplier = managerPurchaseService.updateSupplier(supplierId, request, locale);

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
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        Page<SupplierDto> suppliers = managerPurchaseService.getSuppliers(name, status, page, size, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.suppliers.fetched"),
                suppliers));
    }

    @PostMapping("/suppliers/{supplierId}/products/{productId}")
    @Operation(summary = "Add product to supplier catalog")
    public ResponseEntity<ApiResponse<SupplierProductDto>> addProductToSupplier(
            @PathVariable Long supplierId,
            @PathVariable Long productId,
            @Valid @RequestBody SupplierProductRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        SupplierProductDto supplierProduct = managerPurchaseService.addProductToSupplier(
                supplierId, productId, request, manager.getId(), locale);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.supplier.product.added"),
                        supplierProduct));
    }

    // ========== WAREHOUSE MANAGEMENT ==========

    @PostMapping("/warehouses")
    @Operation(summary = "Create a new warehouse")
    public ResponseEntity<ApiResponse<WarehouseDto>> createWarehouse(
            @Valid @RequestBody WarehouseCreateRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        WarehouseDto warehouse = warehouseService.createWarehouse(
                request, manager.getId(), locale);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.warehouse.created"),
                        warehouse));
    }

    @GetMapping("/warehouses")
    @Operation(summary = "Get list of warehouses")
    public ResponseEntity<ApiResponse<Page<WarehouseDto>>> getWarehouses(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        Page<WarehouseDto> warehouses = warehouseService.getWarehouses(search, page, size, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.warehouses.fetched"),
                warehouses));
    }

    @GetMapping("/warehouses/{warehouseId}")
    @Operation(summary = "Get warehouse by ID")
    public ResponseEntity<ApiResponse<WarehouseDto>> getWarehouse(
            @PathVariable Long warehouseId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        // Можно добавить метод в сервис для получения одного склада
        Page<WarehouseDto> warehouses = warehouseService.getWarehouses(null, 0, 1, locale);
        WarehouseDto warehouse = warehouses.getContent().stream()
                .filter(w -> w.getId().equals(warehouseId))
                .findFirst()
                .orElseThrow(() -> new WarehouseNotFoundException(warehouseId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.warehouse.fetched"),
                warehouse));
    }

    // ========== STORAGE CELL MANAGEMENT ==========

    @PostMapping("/warehouses/{warehouseId}/cells")
    @Operation(summary = "Add a storage cell to warehouse")
    public ResponseEntity<ApiResponse<StorageCellDto>> addStorageCell(
            @PathVariable Long warehouseId,
            @Valid @RequestBody StorageCellCreateRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        StorageCellDto cell = warehouseService.addCell(
                warehouseId, request, manager.getId(), locale);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.cell.created"),
                        cell));
    }

    @GetMapping("/warehouses/{warehouseId}/cells")
    @Operation(summary = "Get all cells in warehouse")
    public ResponseEntity<ApiResponse<List<StorageCellDto>>> getWarehouseCells(
            @PathVariable Long warehouseId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {


        List<StorageCellDto> cells = warehouseService.getWarehouseCells(warehouseId, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cells.fetched"),
                cells));
    }

    @GetMapping("/warehouses/{warehouseId}/cells/available")
    @Operation(summary = "Get available cells by type")
    public ResponseEntity<ApiResponse<List<StorageCellDto>>> getAvailableCells(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) String cellType,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        ru.galtor85.household_store.entity.CellType type = cellType != null ?
                ru.galtor85.household_store.entity.CellType.valueOf(cellType.toUpperCase()) : null;

        List<StorageCellDto> cells = warehouseService.getAvailableCells(warehouseId, type, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cells.available.fetched"),
                cells));
    }

    @GetMapping("/cells/{cellId}")
    @Operation(summary = "Get cell by ID")
    public ResponseEntity<ApiResponse<StorageCellDto>> getCell(
            @PathVariable Long cellId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        // Можно добавить метод в сервис
        StorageCellDto cell = warehouseService.getCellById(cellId, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cell.fetched"),
                cell));
    }

    @PutMapping("/cells/{cellId}/assign")
    @Operation(summary = "Assign product to cell")
    public ResponseEntity<ApiResponse<StorageCellDto>> assignProductToCell(
            @PathVariable Long cellId,
            @RequestParam Long productId,
            @RequestParam int quantity,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        StorageCellDto cell = warehouseService.assignProductToCell(
                cellId, productId, quantity, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cell.product.assigned"),
                cell));
    }

    @PutMapping("/cells/{cellId}/clear")
    @Operation(summary = "Clear cell (remove product)")
    public ResponseEntity<ApiResponse<StorageCellDto>> clearCell(
            @PathVariable Long cellId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        StorageCellDto cell = warehouseService.clearCell(cellId, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cell.cleared"),
                cell));
    }

    // ========== ADVANCED RECEIVING ==========

    @PutMapping("/purchases/{orderId}/receive-with-stock")
    @Operation(summary = "Receive purchase order and place items in warehouse cells")
    public ResponseEntity<ApiResponse<OrderDto>> receivePurchaseOrderWithStock(
            @PathVariable Long orderId,
            @Valid @RequestBody ReceiveAndStockRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        User manager = getCurrentManager();
        OrderDto order = managerPurchaseService.receivePurchaseOrderWithStock(
                orderId, request, manager.getId(), locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.purchase.received.with.stock"),
                order));
    }

    // ========== STOCK MOVEMENTS ==========

    @GetMapping("/movements/product/{productId}")
    @Operation(summary = "Get stock movements for product")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getProductMovements(
            @PathVariable Long productId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        // Добавить метод в сервис
        List<StockMovementDto> movements = warehouseService.getProductMovements(productId, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.movements.fetched"),
                movements));
    }

    @GetMapping("/movements/cell/{cellId}")
    @Operation(summary = "Get stock movements for cell")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getCellMovements(
            @PathVariable Long cellId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        List<StockMovementDto> movements = warehouseService.getCellMovements(cellId, locale);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.movements.fetched"),
                movements));
    }
}