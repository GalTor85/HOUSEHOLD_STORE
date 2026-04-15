package ru.galtor85.household_store.processor.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.product.ProductStockDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.util.stock.StockDtoEnricher;

import java.util.List;
import java.util.Set;

import static ru.galtor85.household_store.constants.PaginationConstants.DESC_SORT_DIRECTION;

/**
 * Processor for warehouse stock operations.
 *
 * <p>Handles retrieval and processing of stock information for warehouses,
 * including paginated stock lists, low stock detection, stock search,
 * and warehouse summaries.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseStockProcessor {

    /**
     * Fields from Product entity that are allowed for sorting.
     * Using FieldNameConstants ensures type-safety during refactoring.
     */
    private static final Set<String> PRODUCT_SORT_FIELDS = Set.of(
            Product.Fields.name,
            Product.Fields.category,
            Product.Fields.sku,
            Product.Fields.brand,
            Product.Fields.price
    );

    private final ProductStockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockDtoEnricher dtoEnricher;
    private final LogMessageService logMsg;

    /**
     * Retrieves paginated stock information for a specific warehouse.
     *
     * <p>Supports sorting by both ProductStock fields and Product fields.
     * When sorting by Product fields (productName, category, sku, brand, price),
     * uses a specialized query with JOIN to the products table.</p>
     *
     * @param warehouseId the warehouse identifier
     * @param page the page number (0-indexed)
     * @param size the page size
     * @param sortBy the field to sort by
     * @param sortDir the sort direction (asc/desc)
     * @return page of enriched ProductStockDto objects
     */
    @Transactional(readOnly = true)
    public Page<ProductStockDto> getStockByWarehouse(Long warehouseId,
                                                     int page,
                                                     int size,
                                                     String sortBy,
                                                     String sortDir) {
        Page<ProductStock> stocks = fetchStockByWarehouse(warehouseId, page, size, sortBy, sortDir);

        log.debug(logMsg.get("stock.by.warehouse.fetched",
                stocks.getTotalElements(), warehouseId));

        return stocks.map(dtoEnricher::enrichStockDto);
    }

    /**
     * Retrieves items with low stock in a specific warehouse.
     *
     * <p>Low stock is defined as quantity below the minimum stock level
     * configured for each product.</p>
     *
     * @param warehouseId the warehouse identifier
     * @return list of ProductStock entities with low stock
     */
    @Transactional(readOnly = true)
    public List<ProductStock> getLowStockItems(Long warehouseId) {
        List<ProductStock> lowStockItems = stockRepository.findLowStockItems(warehouseId);

        log.debug(logMsg.get("stock.low.items.fetched",
                lowStockItems.size(), warehouseId));

        return lowStockItems;
    }

    /**
     * Searches for stock in a warehouse by product name or SKU.
     *
     * <p>Performs a case-insensitive partial match search against
     * product names and SKUs.</p>
     *
     * @param warehouseId the warehouse identifier
     * @param searchTerm the search term (product name or SKU)
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return page of ProductStock entities matching the search
     */
    @Transactional(readOnly = true)
    public Page<ProductStock> searchStockOnWarehouse(Long warehouseId,
                                                     String searchTerm,
                                                     int page,
                                                     int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.searchOnWarehouse(warehouseId, searchTerm, pageable);
    }

    /**
     * Retrieves a summary of stock information for a warehouse.
     *
     * <p>Returns both the warehouse entity and all its stock records
     * for further processing and aggregation.</p>
     *
     * @param warehouseId the warehouse identifier
     * @return WarehouseStockSummary containing warehouse and its stock records
     */
    @Transactional(readOnly = true)
    public WarehouseStockSummary getWarehouseSummary(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
        List<ProductStock> stocks = stockRepository.findByWarehouseId(warehouseId);

        return new WarehouseStockSummary(warehouse, stocks);
    }

    /**
     * Checks if the sort field is from the Product entity (requires JOIN).
     *
     * @param sortBy the field to check
     * @return true if sorting by a Product field
     */
    private boolean isProductField(String sortBy) {
        return PRODUCT_SORT_FIELDS.contains(sortBy);
    }

    /**
     * Fetches stock from repository with appropriate sorting strategy.
     *
     * @param warehouseId the warehouse identifier
     * @param page the page number
     * @param size the page size
     * @param sortBy the field to sort by
     * @param sortDir the sort direction
     * @return page of ProductStock entities
     */
    private Page<ProductStock> fetchStockByWarehouse(Long warehouseId,
                                                     int page,
                                                     int size,
                                                     String sortBy,
                                                     String sortDir) {
        if (isProductField(sortBy)) {
            Pageable pageable = PageRequest.of(page, size);
            return stockRepository.findByWarehouseIdWithSort(warehouseId, sortBy, sortDir, pageable);
        }

        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);
        return stockRepository.findByWarehouseId(warehouseId, pageable);
    }

    /**
     * Builds a Sort object based on the provided parameters.
     *
     * @param sortBy the field to sort by
     * @param sortDir the sort direction
     * @return configured Sort object
     */
    private Sort buildSort(String sortBy, String sortDir) {
        return sortDir.equalsIgnoreCase(DESC_SORT_DIRECTION)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
    }

    /**
     * Summary of warehouse stock information.
     *
     * @param warehouse the warehouse entity
     * @param stocks list of stock records for the warehouse
     */
    public record WarehouseStockSummary(Warehouse warehouse, List<ProductStock> stocks) {}
}