package ru.galtor85.household_store.processor.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.converter.WarehouseStockDetailConverter;
import ru.galtor85.household_store.dto.response.stock.WarehouseStockDetailDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.util.ArrayList;
import java.util.List;

/**
 * Processor for calculating product stock availability.
 *
 * <p>Handles stock calculations including total available stock across all warehouses,
 * customer-visible stock (visible warehouses only), and warehouse-level stock details.</p>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockAvailabilityProcessor {

    private final ProductStockRepository productStockRepository;
    private final LogMessageService logMsg;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockDetailConverter warehouseStockDetailConverter;

    // =========================================================================
    // INDEX CONSTANTS FOR QUERY RESULTS
    // =========================================================================

    private static final int IDX_WAREHOUSE_ID = 0;
    private static final int IDX_QUANTITY = 1;
    private static final int IDX_RESERVED_QUANTITY = 2;
    private static final int IDX_AVAILABLE_QUANTITY = 3;
    private static final int IDX_VISIBLE_FOR_SALE = 4;

    // =========================================================================
    // STOCK CALCULATION METHODS
    // =========================================================================

    /**
     * Calculates available stock quantity for a product across all warehouses.
     * Formula: Available = SUM(quantity) - SUM(reserved_quantity)
     *
     * @param product the product entity
     * @return available quantity (0 if no stock)
     */
    public Integer calculateAvailableStock(Product product) {
        log.debug(logMsg.get("stock.processor.calculate.start", product.getId()));

        Integer available = productStockRepository.getAvailableStockForProduct(product.getId());
        available = available != null ? available : 0;

        log.debug(logMsg.get("stock.processor.calculate.complete", product.getId(), available));

        return available;
    }

    /**
     * Calculates available stock for customer view.
     * Only includes warehouses marked as visible for sale.
     *
     * @param product the product entity
     * @return available quantity from visible warehouses (0 if no stock)
     */
    public Integer calculateAvailableStockForCustomer(Product product) {
        log.debug(logMsg.get("stock.processor.calculate.customer.start", product.getId()));

        Integer available = productStockRepository.getAvailableStockForCustomer(product.getId());
        available = available != null ? available : 0;

        log.debug(logMsg.get("stock.processor.calculate.customer.complete", product.getId(), available));

        return available;
    }

    // =========================================================================
    // WAREHOUSE DETAILS METHODS
    // =========================================================================

    /**
     * Gets stock details by warehouse for a product.
     *
     * @param productId product identifier
     * @param includeInvisible whether to include warehouses marked as invisible for sale
     * @return list of warehouse stock detail DTOs
     */
    public List<WarehouseStockDetailDto> getStockDetailsByWarehouse(Long productId, boolean includeInvisible) {
        log.debug(logMsg.get("stock.processor.warehouse.details.start", productId));

        List<Object[]> results = productStockRepository.getStockByWarehouseWithVisibility(productId);
        List<WarehouseStockDetailDto> details = new ArrayList<>();

        for (Object[] row : results) {
            Long warehouseId = (Long) row[IDX_WAREHOUSE_ID];
            Integer quantity = (Integer) row[IDX_QUANTITY];
            Integer reservedQuantity = (Integer) row[IDX_RESERVED_QUANTITY];
            Integer availableQuantity = (Integer) row[IDX_AVAILABLE_QUANTITY];
            Boolean isVisibleForSale = (Boolean) row[IDX_VISIBLE_FOR_SALE];

            // Skip invisible warehouses if not requested
            if (!includeInvisible && !isVisibleForSale) {
                continue;
            }

            warehouseRepository.findById(warehouseId).ifPresent(warehouse ->
                    details.add(warehouseStockDetailConverter.toDto(
                            warehouse, quantity, reservedQuantity, availableQuantity))
            );
        }

        log.debug(logMsg.get("stock.processor.warehouse.details.complete", productId, details.size()));

        return details;
    }
}