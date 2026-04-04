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
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.auth.CustomAuthenticationException;
import ru.galtor85.household_store.dto.response.product.ProductDto;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.dto.response.product.ProductStockDistributionDto;
import ru.galtor85.household_store.dto.response.product.ProductStockDto;
import ru.galtor85.household_store.dto.request.product.ProductCreateRequest;
import ru.galtor85.household_store.dto.request.product.ProductUpdateRequest;
import ru.galtor85.household_store.dto.request.stock.StockWriteOffRequest;
import ru.galtor85.household_store.dto.request.warehouse.StorageCellCreateRequest;
import ru.galtor85.household_store.dto.request.warehouse.WarehouseCreateRequest;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.dto.response.warehouse.StorageCellDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseStockSummaryDto;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.warehouse.CellType;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.mapper.warehouse.WarehouseMapper;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.manager.ManagerProductService;
import ru.galtor85.household_store.service.manager.ManagerPurchaseService;
import ru.galtor85.household_store.service.stock.StockService;
import ru.galtor85.household_store.service.user.UserSearchService;
import ru.galtor85.household_store.service.warehouse.WarehouseService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.galtor85.household_store.config.ApiConstants.API_BASE;

/**
 * REST controller for manager operations on products, inventory, warehouse, and stock.
 *
 * <p>This controller provides comprehensive management endpoints for:</p>
 * <ul>
 *   <li>Product CRUD operations and media management</li>
 *   <li>Inventory adjustments (stock, pricing, activation)</li>
 *   <li>Warehouse and storage cell management</li>
 *   <li>Stock queries, movements, and distribution analysis</li>
 *   <li>Stock write-offs for damaged or lost goods</li>
 * </ul>
 *
 * <p>All endpoints require ADMIN or MANAGER role for access.</p>
 */
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping(API_BASE+"/manager")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Manager Operations", description = "Endpoints for products, inventory, warehouse and stock management")
public class ManagerInventoryController {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final ManagerProductService managerProductService;
    private final UserSearchService userSearchService;
    private final MessageService messageService;
    private final WarehouseService warehouseService;
    private final StockService stockService;
    private final WarehouseMapper warehouseMapper;
    private final ManagerPurchaseService managerPurchaseService;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

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
    // PRODUCT MANAGEMENT
    // =========================================================================

    /**
     * Creates a new product.
     *
     * @param request product creation request
     * @return created product DTO
     */
    @PostMapping("/products")
    @Operation(summary = "Create a new product",
            description = "Creates a new product with SKU, name, price, and other details")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {

        User manager = getCurrentManager();
        ProductDto product = managerProductService.createProduct(request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.product.created"),
                        product));
    }

    /**
     * Updates an existing product.
     *
     * @param productId product ID
     * @param request   product update request
     * @return updated product DTO
     */
    @PutMapping("/products/{productId}")
    @Operation(summary = "Update an existing product",
            description = "Updates product details. Only provided fields are updated.")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {

        ProductDto product = managerProductService.updateProduct(productId, request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.updated"),
                product));
    }

    /**
     * Retrieves a product by its ID.
     *
     * @param productId product ID
     * @return product DTO
     */
    @GetMapping("/products/{productId}")
    @Operation(summary = "Get product by ID",
            description = "Retrieves detailed product information including media and variants")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        ProductDto product = managerProductService.getProductById(productId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.fetched"),
                product));
    }

    /**
     * Retrieves a paginated list of products with optional filters.
     *
     * @param name     product name filter (partial match)
     * @param category category filter
     * @param brand    brand filter
     * @param active   active status filter
     * @param page     page number
     * @param size     page size
     * @param sortBy   sort field
     * @param sortDir  sort direction
     * @return page of product DTOs
     */
    @GetMapping("/products")
    @Operation(summary = "Get paginated list of products")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> getProducts(
            @Parameter(description = "Product name filter", example = "iPhone")
            @RequestParam(required = false) String name,
            @Parameter(description = "Category filter", example = "Electronics")
            @RequestParam(required = false) String category,
            @Parameter(description = "Brand filter", example = "Apple")
            @RequestParam(required = false) String brand,
            @Parameter(description = "Active status filter", example = "true")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<ProductDto> products = managerProductService.getProducts(
                name, category, brand, active, page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.products.fetched"),
                products));
    }

    /**
     * Adjusts product stock quantity.
     *
     * @param productId product ID
     * @param quantity  amount to adjust (positive to increase, negative to decrease)
     * @param reason    adjustment reason
     * @return updated product DTO
     */
    @PatchMapping("/products/{productId}/stock")
    @Operation(summary = "Adjust product stock",
            description = "Increases or decreases product stock quantity")
    public ResponseEntity<ApiResponse<ProductDto>> adjustStock(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Parameter(description = "Quantity to adjust (positive to increase, negative to decrease)", example = "10", required = true)
            @RequestParam int quantity,
            @Parameter(description = "Adjustment reason", example = "Inventory count")
            @RequestParam(required = false) String reason) {

        ProductDto product = managerProductService.adjustStock(productId, quantity, reason);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.stock.adjusted"),
                product));
    }

    /**
     * Updates product price.
     *
     * @param productId product ID
     * @param newPrice  new price
     * @param reason    price change reason
     * @return updated product DTO
     */
    @PatchMapping("/products/{productId}/price")
    @Operation(summary = "Update product price",
            description = "Updates the selling price of a product")
    public ResponseEntity<ApiResponse<ProductDto>> updatePrice(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Parameter(description = "New price", example = "999.99", required = true)
            @RequestParam BigDecimal newPrice,
            @Parameter(description = "Price change reason", example = "Seasonal discount")
            @RequestParam(required = false) String reason) {

        ProductDto product = managerProductService.updatePrice(productId, newPrice, reason);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.price.updated"),
                product));
    }

    /**
     * Toggles product active status.
     *
     * @param productId product ID
     * @param active    new active status
     * @return updated product DTO
     */
    @PatchMapping("/products/{productId}/toggle")
    @Operation(summary = "Toggle product active status",
            description = "Activates or deactivates a product")
    public ResponseEntity<ApiResponse<ProductDto>> toggleProductActive(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Parameter(description = "Active status", example = "true", required = true)
            @RequestParam boolean active) {

        ProductDto product = managerProductService.toggleProductActive(productId, active);

        String messageKey = active ? "manager.product.activated" : "manager.product.deactivated";
        return ResponseEntity.ok(ApiResponse.success(
                messageService.get(messageKey),
                product));
    }

    /**
     * Uploads media files for a product.
     *
     * @param productId product ID
     * @param files     media files to upload
     * @param metadata  JSON metadata for files
     * @return list of uploaded media DTOs
     */
    @PostMapping("/products/{productId}/media")
    @Operation(summary = "Upload media files for a product",
            description = "Uploads images, videos, or documents for a product")
    public ResponseEntity<ApiResponse<List<ProductMediaDto>>> uploadProductMedia(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Parameter(description = "Media files to upload", required = true)
            @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "JSON metadata for files")
            @RequestParam(required = false) String metadata) {

        User manager = getCurrentManager();
        List<MultipartFile> fileList = Arrays.asList(files);
        List<ProductMediaDto> media = managerProductService.uploadMedia(
                productId, fileList, metadata, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.media.uploaded"),
                media));
    }

    /**
     * Deletes a media file.
     *
     * @param mediaId media ID
     * @return success response
     */
    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete a media file",
            description = "Permanently deletes a media file from the system")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @Parameter(description = "Media ID", example = "1", required = true)
            @PathVariable Long mediaId) {

        User manager = getCurrentManager();
        managerProductService.deleteMedia(mediaId, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.media.deleted"),
                null));
    }

    /**
     * Sets a media file as the main product image.
     *
     * @param mediaId media ID
     * @return success response
     */
    @PutMapping("/media/{mediaId}/main")
    @Operation(summary = "Set media as main image",
            description = "Sets the specified media as the main product image")
    public ResponseEntity<ApiResponse<Void>> setMainMedia(
            @Parameter(description = "Media ID", example = "1", required = true)
            @PathVariable Long mediaId) {

        User manager = getCurrentManager();
        managerProductService.setMainMedia(mediaId, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.media.main.set"),
                null));
    }

    /**
     * Retrieves products with low stock levels.
     *
     * @param threshold stock threshold
     * @return list of products with low stock
     */
    @GetMapping("/inventory/low-stock")
    @Operation(summary = "Get low stock products",
            description = "Retrieves products with quantity below the specified threshold")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getLowStockProducts(
            @Parameter(description = "Stock threshold", example = "10")
            @RequestParam(defaultValue = "10") int threshold) {

        List<ProductDto> products = managerProductService.getLowStockProducts(threshold);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.inventory.low.stock"),
                products));
    }

    /**
     * Writes off damaged or lost stock.
     *
     * @param request write-off request with items and reason
     * @return success response
     */
    @PostMapping("/inventory/write-off")
    @Operation(summary = "Write off damaged/lost stock",
            description = "Removes damaged or lost items from inventory")
    public ResponseEntity<ApiResponse<Void>> writeOffStock(
            @Valid @RequestBody StockWriteOffRequest request) {

        User manager = getCurrentManager();
        managerPurchaseService.writeOffStock(request, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.stock.writeoff.completed"),
                null));
    }

    // =========================================================================
    // WAREHOUSE MANAGEMENT
    // =========================================================================

    /**
     * Creates a new warehouse.
     *
     * @param request warehouse creation request
     * @return created warehouse DTO
     */
    @PostMapping("/warehouses")
    @Operation(summary = "Create a new warehouse",
            description = "Creates a new warehouse with specified capacity and address")
    public ResponseEntity<ApiResponse<WarehouseDto>> createWarehouse(
            @Valid @RequestBody WarehouseCreateRequest request) {

        User manager = getCurrentManager();
        WarehouseDto warehouse = warehouseService.createWarehouse(
                request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.warehouse.created"),
                        warehouse));
    }

    /**
     * Retrieves a paginated list of warehouses.
     *
     * @param search search term (name, code, or barcode)
     * @param page   page number
     * @param size   page size
     * @return page of warehouse DTOs
     */
    @GetMapping("/warehouses")
    @Operation(summary = "Get list of warehouses",
            description = "Retrieves a paginated list of warehouses with optional search")
    public ResponseEntity<ApiResponse<Page<WarehouseDto>>> getWarehouses(
            @Parameter(description = "Search term (name, code, or barcode)", example = "Main")
            @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Page<WarehouseDto> warehouses = warehouseService.getWarehouses(search, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.warehouses.fetched"),
                warehouses));
    }

    /**
     * Retrieves a warehouse by its ID.
     *
     * @param warehouseId warehouse ID
     * @return warehouse DTO
     */
    @GetMapping("/warehouses/{warehouseId}")
    @Operation(summary = "Get warehouse by ID",
            description = "Retrieves detailed information about a warehouse")
    public ResponseEntity<ApiResponse<WarehouseDto>> getWarehouse(
            @Parameter(description = "Warehouse ID", example = "1", required = true)
            @PathVariable Long warehouseId) {

        WarehouseDto warehouse = warehouseService.getWarehouseById(warehouseId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.warehouse.fetched"),
                warehouse));
    }

    // =========================================================================
    // STORAGE CELL MANAGEMENT
    // =========================================================================

    /**
     * Adds a storage cell to a warehouse.
     *
     * @param warehouseId warehouse ID
     * @param request     cell creation request
     * @return created cell DTO
     */
    @PostMapping("/warehouses/{warehouseId}/cells")
    @Operation(summary = "Add a storage cell to warehouse",
            description = "Creates a new storage cell in the specified warehouse")
    public ResponseEntity<ApiResponse<StorageCellDto>> addStorageCell(
            @Parameter(description = "Warehouse ID", example = "1", required = true)
            @PathVariable Long warehouseId,
            @Valid @RequestBody StorageCellCreateRequest request) {

        User manager = getCurrentManager();
        StorageCellDto cell = warehouseService.addCell(
                warehouseId, request, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("manager.cell.created"),
                        cell));
    }

    /**
     * Retrieves all cells in a warehouse.
     *
     * @param warehouseId warehouse ID
     * @return list of cell DTOs
     */
    @GetMapping("/warehouses/{warehouseId}/cells")
    @Operation(summary = "Get all cells in warehouse",
            description = "Retrieves all storage cells in the specified warehouse")
    public ResponseEntity<ApiResponse<List<StorageCellDto>>> getWarehouseCells(
            @Parameter(description = "Warehouse ID", example = "1", required = true)
            @PathVariable Long warehouseId) {

        List<StorageCellDto> cells = warehouseService.getWarehouseCells(warehouseId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cells.fetched"),
                cells));
    }

    /**
     * Retrieves available cells by type.
     *
     * @param warehouseId warehouse ID
     * @param cellType    cell type filter (optional)
     * @return list of available cell DTOs
     */
    @GetMapping("/warehouses/{warehouseId}/cells/available")
    @Operation(summary = "Get available cells by type",
            description = "Retrieves available (unoccupied) storage cells, optionally filtered by type")
    public ResponseEntity<ApiResponse<List<StorageCellDto>>> getAvailableCells(
            @Parameter(description = "Warehouse ID", example = "1", required = true)
            @PathVariable Long warehouseId,
            @Parameter(description = "Cell type filter", example = "STANDARD",
                    schema = @Schema(allowableValues = {"STANDARD", "PALLET", "FRIDGE", "FREEZER", "DANGEROUS", "BULK", "LIQUID", "OVERSIZE"}))
            @RequestParam(required = false) String cellType) {

        CellType type = cellType != null ?
                CellType.valueOf(cellType.toUpperCase()) : null;

        List<StorageCellDto> cells = warehouseService.getAvailableCells(warehouseId, type);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cells.available.fetched"),
                cells));
    }

    /**
     * Retrieves a cell by its ID.
     *
     * @param cellId cell ID
     * @return cell DTO
     */
    @GetMapping("/cells/{cellId}")
    @Operation(summary = "Get cell by ID",
            description = "Retrieves detailed information about a storage cell")
    public ResponseEntity<ApiResponse<StorageCellDto>> getCell(
            @Parameter(description = "Cell ID", example = "1", required = true)
            @PathVariable Long cellId) {

        StorageCellDto cell = warehouseService.getCellById(cellId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cell.fetched"),
                cell));
    }

    /**
     * Assigns a product to a storage cell.
     *
     * @param cellId    cell ID
     * @param productId product ID
     * @param quantity  quantity to store
     * @return updated cell DTO
     */
    @PutMapping("/cells/{cellId}/assign")
    @Operation(summary = "Assign product to cell",
            description = "Assigns a product to a storage cell with specified quantity")
    public ResponseEntity<ApiResponse<StorageCellDto>> assignProductToCell(
            @Parameter(description = "Cell ID", example = "1", required = true)
            @PathVariable Long cellId,
            @Parameter(description = "Product ID", example = "1", required = true)
            @RequestParam Long productId,
            @Parameter(description = "Quantity", example = "10", required = true)
            @RequestParam int quantity) {

        User manager = getCurrentManager();
        StorageCellDto cell = warehouseService.assignProductToCell(
                cellId, productId, quantity, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cell.product.assigned"),
                cell));
    }

    /**
     * Clears a storage cell (removes product).
     *
     * @param cellId cell ID
     * @return updated cell DTO
     */
    @PutMapping("/cells/{cellId}/clear")
    @Operation(summary = "Clear cell (remove product)",
            description = "Removes the product from a storage cell")
    public ResponseEntity<ApiResponse<StorageCellDto>> clearCell(
            @Parameter(description = "Cell ID", example = "1", required = true)
            @PathVariable Long cellId) {

        User manager = getCurrentManager();
        StorageCellDto cell = warehouseService.clearCell(cellId, manager.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.cell.cleared"),
                cell));
    }

    // =========================================================================
    // STOCK MOVEMENTS
    // =========================================================================

    /**
     * Gets stock movements for a product.
     *
     * @param productId product ID
     * @return list of stock movement DTOs
     */
    @GetMapping("/movements/product/{productId}")
    @Operation(summary = "Get stock movements for product",
            description = "Retrieves all stock movements (receipts, shipments, transfers) for a product")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getProductMovements(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        List<StockMovementDto> movements = warehouseService.getProductMovements(productId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.movements.fetched"),
                movements));
    }

    /**
     * Gets stock movements for a cell.
     *
     * @param cellId cell ID
     * @return list of stock movement DTOs
     */
    @GetMapping("/movements/cell/{cellId}")
    @Operation(summary = "Get stock movements for cell",
            description = "Retrieves all stock movements involving a specific storage cell")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getCellMovements(
            @Parameter(description = "Cell ID", example = "1", required = true)
            @PathVariable Long cellId) {

        List<StockMovementDto> movements = warehouseService.getCellMovements(cellId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.movements.fetched"),
                movements));
    }

    // =========================================================================
    // STOCK QUERIES
    // =========================================================================

    /**
     * Gets stock by warehouse with pagination.
     *
     * @param warehouseId warehouse ID
     * @param page        page number
     * @param size        page size
     * @param sortBy      sort field
     * @param sortDir     sort direction
     * @return page of product stock DTOs
     */
    @GetMapping("/warehouses/{warehouseId}/stock")
    @Operation(summary = "Get stock by warehouse",
            description = "Retrieves paginated stock information for a warehouse")
    public ResponseEntity<ApiResponse<Page<ProductStockDto>>> getStockByWarehouse(
            @Parameter(description = "Warehouse ID", example = "1", required = true)
            @PathVariable Long warehouseId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "productName")
            @RequestParam(defaultValue = "productName") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<ProductStockDto> stocks = stockService.getStockByWarehouse(
                warehouseId, page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.warehouse.stock.fetched"),
                stocks));
    }

    /**
     * Gets product stock across all warehouses.
     *
     * @param productId product ID
     * @return list of product stock DTOs
     */
    @GetMapping("/products/{productId}/stock")
    @Operation(summary = "Get product stock across all warehouses",
            description = "Retrieves stock information for a product across all warehouses")
    public ResponseEntity<ApiResponse<List<ProductStockDto>>> getProductStock(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        List<ProductStockDto> stocks = stockService.getProductStockAcrossAllWarehouses(productId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.stock.fetched"),
                stocks));
    }

    /**
     * Gets product stock at a specific warehouse.
     *
     * @param warehouseId warehouse ID
     * @param productId   product ID
     * @return product stock DTO
     */
    @GetMapping("/warehouses/{warehouseId}/products/{productId}/stock")
    @Operation(summary = "Get product stock at specific warehouse",
            description = "Retrieves stock information for a product at a specific warehouse")
    public ResponseEntity<ApiResponse<ProductStockDto>> getProductStockAtWarehouse(
            @Parameter(description = "Warehouse ID", example = "1", required = true)
            @PathVariable Long warehouseId,
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        ProductStockDto stock = stockService.getProductStockAtWarehouse(productId, warehouseId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.warehouse.stock.fetched"),
                stock));
    }

    /**
     * Gets stock summary for all warehouses.
     *
     * @return list of warehouse stock summary DTOs
     */
    @GetMapping("/warehouses/stock/summary")
    @Operation(summary = "Get stock summary for all warehouses",
            description = "Retrieves stock summaries for all warehouses with totals and top products")
    public ResponseEntity<ApiResponse<List<WarehouseStockSummaryDto>>> getAllWarehousesSummary() {

        List<WarehouseStockSummaryDto> summaries = stockService.getAllWarehousesSummary();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.all.warehouses.summary"),
                summaries));
    }

    /**
     * Gets low stock items in a warehouse.
     *
     * @param warehouseId warehouse ID
     * @return list of low stock product DTOs
     */
    @GetMapping("/warehouses/{warehouseId}/stock/low")
    @Operation(summary = "Get low stock items in warehouse",
            description = "Retrieves products with stock below minimum level in a warehouse")
    public ResponseEntity<ApiResponse<List<ProductStockDto>>> getLowStockItems(
            @Parameter(description = "Warehouse ID", example = "1", required = true)
            @PathVariable Long warehouseId) {

        List<ProductStockDto> lowStock = stockService.getLowStockItems(warehouseId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.warehouse.low.stock"),
                lowStock));
    }

    /**
     * Searches stock in a warehouse.
     *
     * @param warehouseId warehouse ID
     * @param query       search term (product name or SKU)
     * @param page        page number
     * @param size        page size
     * @return page of product stock DTOs
     */
    @GetMapping("/warehouses/{warehouseId}/stock/search")
    @Operation(summary = "Search stock in warehouse",
            description = "Searches for stock items by product name or SKU")
    public ResponseEntity<ApiResponse<Page<ProductStockDto>>> searchStock(
            @Parameter(description = "Warehouse ID", example = "1", required = true)
            @PathVariable Long warehouseId,
            @Parameter(description = "Search term (product name or SKU)", example = "iPhone", required = true)
            @RequestParam String query,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Page<ProductStockDto> stocks = stockService.searchStockOnWarehouse(
                warehouseId, query, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.warehouse.stock.search"),
                stocks));
    }

    /**
     * Gets product stock distribution across warehouses.
     *
     * @param productId product ID
     * @return stock distribution DTO
     */
    @GetMapping("/products/{productId}/stock/distribution")
    @Operation(summary = "Get product stock distribution across warehouses",
            description = "Retrieves how a product's stock is distributed across all warehouses")
    public ResponseEntity<ApiResponse<ProductStockDistributionDto>> getProductStockDistribution(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        ProductStockDistributionDto distribution = stockService.getProductStockDistribution(productId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.stock.distribution"),
                distribution));
    }

    /**
     * Gets total stock for a product across all warehouses.
     *
     * @param productId product ID
     * @return total quantity
     */
    @GetMapping("/products/{productId}/stock/total")
    @Operation(summary = "Get total stock for product across all warehouses",
            description = "Retrieves the total quantity of a product across all warehouses")
    public ResponseEntity<ApiResponse<Integer>> getTotalProductStock(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        Integer total = stockService.getTotalStockForProduct(productId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.stock.total"),
                total));
    }

    /**
     * Finds warehouses where a product is stored.
     *
     * @param productId product ID
     * @return list of warehouse DTOs
     */
    @GetMapping("/products/{productId}/warehouses")
    @Operation(summary = "Find warehouses where product is stored",
            description = "Retrieves all warehouses that have stock of the specified product")
    public ResponseEntity<ApiResponse<List<WarehouseDto>>> findWarehousesWithProduct(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        List<Warehouse> warehouses = stockService.findWarehousesWithProduct(productId);

        List<WarehouseDto> warehouseDtos = warehouses.stream()
                .map(warehouseMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("manager.product.warehouses.found"),
                warehouseDtos));
    }

    /**
     * Gets paginated stock movements for a product.
     *
     * @param productId product ID
     * @param page      page number
     * @param size      page size
     * @return page of stock movement DTOs
     */
    @GetMapping("/stock/movements/product/{productId}")
    @Operation(summary = "Get stock movements for a product",
            description = "Retrieves paginated stock movements for a product")
    public ResponseEntity<ApiResponse<Page<StockMovementDto>>> getProductMovements(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Page<StockMovementDto> movements = stockService.getProductMovements(productId, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("stock.movements.fetched"),
                movements));
    }

    /**
     * Gets paginated stock movements for a warehouse.
     *
     * @param warehouseId warehouse ID
     * @param page        page number
     * @param size        page size
     * @return page of stock movement DTOs
     */
    @GetMapping("/stock/movements/warehouse/{warehouseId}")
    @Operation(summary = "Get stock movements for a warehouse",
            description = "Retrieves paginated stock movements for a warehouse")
    public ResponseEntity<ApiResponse<Page<StockMovementDto>>> getWarehouseMovements(
            @Parameter(description = "Warehouse ID", example = "1", required = true)
            @PathVariable Long warehouseId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Page<StockMovementDto> movements = stockService.getWarehouseMovements(warehouseId, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("stock.movements.fetched"),
                movements));
    }

    /**
     * Gets movements by reference (order, purchase, etc.).
     *
     * @param refType reference type
     * @param refId   reference ID
     * @return list of stock movement DTOs
     */
    @GetMapping("/stock/movements/reference")
    @Operation(summary = "Get movements by reference",
            description = "Retrieves stock movements by reference type and ID (e.g., order, purchase)")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getMovementsByReference(
            @Parameter(description = "Reference type", example = "ORDER", required = true)
            @RequestParam String refType,
            @Parameter(description = "Reference ID", example = "1", required = true)
            @RequestParam Long refId) {

        List<StockMovementDto> movements = stockService.getMovementsByReference(refType, refId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("stock.movements.fetched"),
                movements));
    }

    /**
     * Gets movements by batch number.
     *
     * @param batchNumber batch number
     * @return list of stock movement DTOs
     */
    @GetMapping("/stock/movements/batch/{batchNumber}")
    @Operation(summary = "Get movements by batch number",
            description = "Retrieves all stock movements for a specific batch/lot number")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getMovementsByBatch(
            @Parameter(description = "Batch number", example = "BATCH-20240331-ABC123", required = true)
            @PathVariable String batchNumber) {

        List<StockMovementDto> movements = stockService.getMovementsByBatch(batchNumber);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("stock.movements.batch.fetched"),
                movements));
    }

    /**
     * Gets all batch numbers for a product.
     *
     * @param productId product ID
     * @return list of batch numbers
     */
    @GetMapping("/products/{productId}/batches")
    @Operation(summary = "Get all batch numbers for a product",
            description = "Retrieves all distinct batch/lot numbers for a product")
    public ResponseEntity<ApiResponse<List<String>>> getProductBatches(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        List<String> batches = stockService.getProductBatches(productId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("stock.product.batches.fetched"),
                batches));
    }
}