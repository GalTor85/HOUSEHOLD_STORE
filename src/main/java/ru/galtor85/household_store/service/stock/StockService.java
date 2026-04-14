package ru.galtor85.household_store.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.stock.InsufficientStockException;
import ru.galtor85.household_store.advice.exception.stock.ProductStockNotFoundException;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.dto.request.stock.StockTransferRequest;
import ru.galtor85.household_store.dto.response.product.ProductStockDistributionDto;
import ru.galtor85.household_store.dto.response.product.ProductStockDto;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.dto.response.stock.StockTransferResponseDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseStockSummaryDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.processor.product.ProductStockProcessor;
import ru.galtor85.household_store.processor.stock.StockMovementProcessor;
import ru.galtor85.household_store.processor.stock.StockTransferProcessor;
import ru.galtor85.household_store.processor.warehouse.WarehouseStockProcessor;
import ru.galtor85.household_store.processor.warehouse.WarehouseSummaryProcessor;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.util.stock.StockDtoEnricher;
import ru.galtor85.household_store.validator.stock.StockTransferValidator;
import ru.galtor85.household_store.validator.stock.StockValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing product stock and inventory operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductStockRepository stockRepository;
    private final StockValidator validator;
    private final WarehouseStockProcessor warehouseStockProcessor;
    private final ProductStockProcessor productStockProcessor;
    private final WarehouseSummaryProcessor summaryProcessor;
    private final StockMovementProcessor movementProcessor;
    private final StockDtoEnricher dtoEnricher;
    private final BusinessConfig businessConfig;
    private final StockTransferProcessor stockTransferProcessor;
    private final StockTransferValidator stockTransferValidator;
    private final LogMessageService logMsg;


    // =========================================================================
    // STOCK VIEW BY WAREHOUSE
    // =========================================================================

    /**
     * Retrieves paginated stock information for a specific warehouse
     *
     * @param warehouseId warehouse identifier
     * @param page        page number (0-indexed)
     * @param size        page size
     * @param sortBy      field to sort by
     * @param sortDir     sort direction (asc/desc)
     * @return page of product stock DTOs
     */
    @Transactional(readOnly = true)
    public Page<ProductStockDto> getStockByWarehouse(Long warehouseId,
                                                     Integer page, Integer size,
                                                     String sortBy, String sortDir) {
        int effectivePage = page != null ? page : businessConfig.getPagination().getDefaultPage();
        int effectiveSize = size != null ? size : businessConfig.getPagination().getDefaultSize();
        validator.validateWarehouseExists(warehouseId);
        return warehouseStockProcessor.getStockByWarehouse(warehouseId, effectivePage, effectiveSize, sortBy, sortDir);
    }

    // =========================================================================
    // STOCK VIEW BY PRODUCT (ALL WAREHOUSES)
    // =========================================================================

    /**
     * Retrieves stock information for a product across all warehouses
     *
     * @param productId product identifier
     * @return list of product stock DTOs per warehouse
     */
    @Transactional(readOnly = true)
    public List<ProductStockDto> getProductStockAcrossAllWarehouses(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return productStockProcessor.getProductStockAcrossAllWarehouses(product);
    }

    /**
     * Gets total stock quantity for a product across all warehouses
     *
     * @param productId product identifier
     * @return total quantity in stock
     */
    @Transactional(readOnly = true)
    public Integer getTotalStockForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return productStockProcessor.getTotalStockForProduct(product);
    }

    /**
     * Gets stock distribution for a product across all warehouses
     *
     * @param productId product identifier
     * @return distribution DTO with total and per-warehouse breakdown
     */
    @Transactional(readOnly = true)
    public ProductStockDistributionDto getProductStockDistribution(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return productStockProcessor.getProductStockDistribution(product);
    }

    // =========================================================================
    // STOCK VIEW BY WAREHOUSE AND PRODUCT
    // =========================================================================

    /**
     * Retrieves stock information for a specific product at a specific warehouse
     *
     * @param productId   product identifier
     * @param warehouseId warehouse identifier
     * @return product stock DTO
     */
    @Transactional(readOnly = true)
    public ProductStockDto getProductStockAtWarehouse(Long productId, Long warehouseId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException(warehouseId));

        return dtoEnricher.enrichStockDto(
                productStockProcessor.getProductStockAtWarehouse(product, warehouse)
        );
    }

    // =========================================================================
    // WAREHOUSE SUMMARIES
    // =========================================================================

    /**
     * Gets stock summaries for all warehouses
     *
     * @return list of warehouse stock summary DTOs
     */
    @Transactional(readOnly = true)
    public List<WarehouseStockSummaryDto> getAllWarehousesSummary() {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        return warehouses.stream()
                .map(warehouse -> getWarehouseSummary(warehouse.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Gets stock summary for a specific warehouse
     *
     * @param warehouseId warehouse identifier
     * @return warehouse stock summary DTO
     */
    @Transactional(readOnly = true)
    public WarehouseStockSummaryDto getWarehouseSummary(Long warehouseId) {
        validator.validateWarehouseExists(warehouseId);
        var summary = warehouseStockProcessor.getWarehouseSummary(warehouseId);
        return summaryProcessor.buildSummary(summary.getWarehouse(), summary.getStocks());
    }

    // =========================================================================
    // SEARCH AND FILTER
    // =========================================================================

    /**
     * Gets products with low stock in a warehouse
     *
     * @param warehouseId warehouse identifier
     * @return list of product stock DTOs with low stock
     */
    @Transactional(readOnly = true)
    public List<ProductStockDto> getLowStockItems(Long warehouseId) {
        validator.validateWarehouseExists(warehouseId);
        int threshold = businessConfig.getStock().getLowStockThreshold();

        return warehouseStockProcessor.getLowStockItems(warehouseId).stream()
                .filter(item -> item.getQuantity() < threshold)
                .map(dtoEnricher::enrichStockDto)
                .collect(Collectors.toList());
    }

    /**
     * Searches for stock in a warehouse by product name or SKU
     *
     * @param warehouseId warehouse identifier
     * @param searchTerm  search term (product name or SKU)
     * @param page        page number
     * @param size        page size
     * @return page of product stock DTOs matching the search
     */
    @Transactional(readOnly = true)
    public Page<ProductStockDto> searchStockOnWarehouse(Long warehouseId, String searchTerm,
                                                        int page, int size) {
        validator.validateWarehouseExists(warehouseId);
        return warehouseStockProcessor.searchStockOnWarehouse(warehouseId, searchTerm, page, size)
                .map(dtoEnricher::enrichStockDto);
    }

    /**
     * Finds all warehouses where a product is stored
     *
     * @param productId product identifier
     * @return list of warehouses
     */
    @Transactional(readOnly = true)
    public List<Warehouse> findWarehousesWithProduct(Long productId) {
        validator.validateProductExists(productId);
        List<ProductStock> stocks = stockRepository.findByProductId(productId);
        List<Long> warehouseIds = stocks.stream()
                .map(ProductStock::getWarehouseId)
                .collect(Collectors.toList());
        return warehouseRepository.findAllById(warehouseIds);
    }

    // =========================================================================
    // STOCK MOVEMENTS
    // =========================================================================

    /**
     * Gets stock movements for a product with pagination
     *
     * @param productId product identifier
     * @param page      page number
     * @param size      page size
     * @return page of stock movement DTOs
     */
    @Transactional(readOnly = true)
    public Page<StockMovementDto> getProductMovements(Long productId, int page, int size) {
        validator.validateProductExists(productId);
        return movementProcessor.getProductMovements(productId, page, size);
    }

    /**
     * Gets stock movements for a warehouse with pagination
     *
     * @param warehouseId warehouse identifier
     * @param page        page number
     * @param size        page size
     * @return page of stock movement DTOs
     */
    @Transactional(readOnly = true)
    public Page<StockMovementDto> getWarehouseMovements(Long warehouseId, int page, int size) {
        validator.validateWarehouseExists(warehouseId);
        return movementProcessor.getWarehouseMovements(warehouseId, page, size);
    }

    /**
     * Gets stock movements by reference (e.g., order ID, purchase ID)
     *
     * @param refType reference type (ORDER, PURCHASE, WRITEOFF, INVENTORY)
     * @param refId   reference identifier
     * @return list of stock movement DTOs
     */
    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByReference(String refType, Long refId) {
        return movementProcessor.getMovementsByReference(refType, refId);
    }

    /**
     * Gets stock movements by batch/lot number
     *
     * @param batchNumber batch/lot number
     * @return list of stock movement DTOs
     */
    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByBatch(String batchNumber) {
        return movementProcessor.getMovementsByBatch(batchNumber);
    }

    /**
     * Gets all batch numbers for a product
     *
     * @param productId product identifier
     * @return list of batch numbers
     */
    @Transactional(readOnly = true)
    public List<String> getProductBatches(Long productId) {
        validator.validateProductExists(productId);
        return movementProcessor.getProductBatches(productId);
    }

    /**
     * Transfers stock between warehouses or cells.
     *
     * @param request     transfer request
     * @param performedBy ID of user performing transfer
     * @return transfer response
     */
    @Transactional
    public StockTransferResponseDto transferStock(StockTransferRequest request, Long performedBy) {
        log.info(logMsg.get("stock.transfer.service.start",
                request.getProductId(), request.getQuantity()));

        stockTransferValidator.validateTransferRequest(request);

        return stockTransferProcessor.transferStock(request, performedBy);
    }

    // =========================================================================
    // STOCK UPDATE OPERATIONS
    // =========================================================================

    /**
     * Updates product stock quantity (increase or decrease)
     * Creates a new stock record if it doesn't exist and operation is increase
     *
     * @param product     product entity
     * @param quantity    quantity to change (positive number)
     * @param warehouseId warehouse identifier
     * @param increase    true = increase stock, false = decrease stock
     * @throws ProductStockNotFoundException if stock record not found and operation is decrease
     * @throws InsufficientStockException    if decrease would result in negative stock
     */
    public void updateProductStock(Product product, int quantity, Long warehouseId, boolean increase) {
        ProductStock stock = stockRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouseId)
                .orElse(null);

        // Create new stock record only when increasing
        if (stock == null && increase) {
            stock = ProductStock.builder()
                    .productId(product.getId())
                    .warehouseId(warehouseId)
                    .quantity(quantity)
                    .reservedQuantity(0)
                    .availableQuantity(quantity)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        // If stock not found and operation is decrease - throw exception
        if (stock == null) {
            throw new ProductStockNotFoundException(product.getId(), warehouseId);
        }

        int delta = increase ? quantity : -quantity;
        int newQuantity = stock.getQuantity() + delta;

        // Prevent negative stock
        if (newQuantity < 0) {
            throw new InsufficientStockException(product.getName(), stock.getQuantity());
        }

        stock.setQuantity(newQuantity);
        stock.setAvailableQuantity(newQuantity -
                (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0));
        stock.setUpdatedAt(LocalDateTime.now());
        stockRepository.save(stock);
    }
}