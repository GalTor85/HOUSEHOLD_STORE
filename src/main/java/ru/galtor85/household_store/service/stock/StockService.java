package ru.galtor85.household_store.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.dto.response.product.ProductStockDistributionDto;
import ru.galtor85.household_store.dto.response.product.ProductStockDto;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseStockSummaryDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.processor.product.ProductStockProcessor;
import ru.galtor85.household_store.processor.stock.StockMovementProcessor;
import ru.galtor85.household_store.processor.warehouse.WarehouseStockProcessor;
import ru.galtor85.household_store.processor.warehouse.WarehouseSummaryProcessor;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.util.stock.StockDtoEnricher;
import ru.galtor85.household_store.validator.stock.StockValidator;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductStockRepository stockRepository;

    // Валидаторы
    private final StockValidator validator;

    // Процессоры
    private final WarehouseStockProcessor warehouseStockProcessor;
    private final ProductStockProcessor productStockProcessor;
    private final WarehouseSummaryProcessor summaryProcessor;
    private final StockMovementProcessor movementProcessor;

    // Утилиты
    private final StockDtoEnricher dtoEnricher;

    // ========== ПРОСМОТР ОСТАТКОВ ПО СКЛАДУ ==========

    @Transactional(readOnly = true)
    public Page<ProductStockDto> getStockByWarehouse(Long warehouseId,
                                                     int page, int size,
                                                     String sortBy, String sortDir) {
        validator.validateWarehouseExists(warehouseId);
        return warehouseStockProcessor.getStockByWarehouse(warehouseId, page, size, sortBy, sortDir);
    }

    // ========== ПРОСМОТР ПО ПРОДУКТУ (ВСЕ СКЛАДЫ) ==========

    @Transactional(readOnly = true)
    public List<ProductStockDto> getProductStockAcrossAllWarehouses(Long productId) {
        validator.validateProductExists(productId);
        Product product = productRepository.findById(productId).get();
        return productStockProcessor.getProductStockAcrossAllWarehouses(product);
    }

    @Transactional(readOnly = true)
    public Integer getTotalStockForProduct(Long productId) {
        validator.validateProductExists(productId);
        Product product = productRepository.findById(productId).get();
        return productStockProcessor.getTotalStockForProduct(product);
    }

    @Transactional(readOnly = true)
    public ProductStockDistributionDto getProductStockDistribution(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return productStockProcessor.getProductStockDistribution(product);
    }

    // ========== ПРОСМОТР ПО КОНКРЕТНОМУ СКЛАДУ И ПРОДУКТУ ==========

    @Transactional(readOnly = true)
    public ProductStockDto getProductStockAtWarehouse(Long productId, Long warehouseId) {
        validator.validateProductExists(productId);
        validator.validateWarehouseExists(warehouseId);

        Product product = productRepository.findById(productId).get();
        Warehouse warehouse = warehouseRepository.findById(warehouseId).get();

        return dtoEnricher.enrichStockDto(
                productStockProcessor.getProductStockAtWarehouse(product, warehouse)
        );
    }

    // ========== ПРОСМОТР ПО ВСЕМ СКЛАДАМ ==========

    @Transactional(readOnly = true)
    public List<WarehouseStockSummaryDto> getAllWarehousesSummary() {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        return warehouses.stream()
                .map(warehouse -> getWarehouseSummary(warehouse.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WarehouseStockSummaryDto getWarehouseSummary(Long warehouseId) {
        validator.validateWarehouseExists(warehouseId);
        var summary = warehouseStockProcessor.getWarehouseSummary(warehouseId);
        return summaryProcessor.buildSummary(summary.getWarehouse(), summary.getStocks());
    }

    // ========== ПОИСК И ФИЛЬТРАЦИЯ ==========

    @Transactional(readOnly = true)
    public List<ProductStockDto> getLowStockItems(Long warehouseId) {
        validator.validateWarehouseExists(warehouseId);
        return warehouseStockProcessor.getLowStockItems(warehouseId).stream()
                .map(dtoEnricher::enrichStockDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductStockDto> searchStockOnWarehouse(Long warehouseId, String searchTerm,
                                                        int page, int size) {
        validator.validateWarehouseExists(warehouseId);
        return warehouseStockProcessor.searchStockOnWarehouse(warehouseId, searchTerm, page, size)
                .map(dtoEnricher::enrichStockDto);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> findWarehousesWithProduct(Long productId) {
        validator.validateProductExists(productId);
        List<ProductStock> stocks = stockRepository.findByProductId(productId);
        List<Long> warehouseIds = stocks.stream()
                .map(ProductStock::getWarehouseId)
                .collect(Collectors.toList());
        return warehouseRepository.findAllById(warehouseIds);
    }

    // ========== ДВИЖЕНИЯ ТОВАРОВ ==========

    @Transactional(readOnly = true)
    public Page<StockMovementDto> getProductMovements(Long productId, int page, int size) {
        validator.validateProductExists(productId);
        return movementProcessor.getProductMovements(productId, page, size);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> getWarehouseMovements(Long warehouseId, int page, int size) {
        validator.validateWarehouseExists(warehouseId);
        return movementProcessor.getWarehouseMovements(warehouseId, page, size);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByReference(String refType, Long refId) {
        return movementProcessor.getMovementsByReference(refType, refId);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByBatch(String batchNumber) {
        return movementProcessor.getMovementsByBatch(batchNumber);
    }

    @Transactional(readOnly = true)
    public List<String> getProductBatches(Long productId) {
        validator.validateProductExists(productId);
        return movementProcessor.getProductBatches(productId);
    }

    @Transactional(readOnly = true)
    public StockMovementDto getLatestBatchMovement(Long productId, String batchNumber) {
        validator.validateProductExists(productId);
        return movementProcessor.getLatestBatchMovement(productId, batchNumber);
    }
}