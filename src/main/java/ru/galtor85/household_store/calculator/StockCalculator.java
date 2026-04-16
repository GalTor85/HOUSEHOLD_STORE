package ru.galtor85.household_store.calculator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseStockDto;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;

/**
 * Calculator for stock metrics and statistics.
 */
@Component
@RequiredArgsConstructor
public class StockCalculator {

    private static final int DEFAULT_MIN_STOCK_LEVEL = 0;
    private static final int DEFAULT_RESERVED_QUANTITY = 0;
    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double ZERO_VALUE = 0.0;

    private final MessageService messageService;

    /**
     * Sums total quantity across all stock records.
     *
     * @param stocks list of stock records
     * @return total quantity
     */
    public int sumQuantity(List<ProductStock> stocks) {
        return stocks.stream().mapToInt(ProductStock::getQuantity).sum();
    }

    /**
     * Sums total reserved quantity across all stock records.
     *
     * @param stocks list of stock records
     * @return total reserved quantity
     */
    public int sumReserved(List<ProductStock> stocks) {
        return stocks.stream()
                .mapToInt(s -> s.getReservedQuantity() != null ? s.getReservedQuantity() : DEFAULT_RESERVED_QUANTITY)
                .sum();
    }

    /**
     * Calculates total stock value.
     *
     * @return total value (to be implemented)
     */
    public double calculateTotalValue() {
        return ZERO_VALUE;
    }

    /**
     * Counts items with quantity below minimum stock level.
     *
     * @param stocks list of stock records
     * @return count of low stock items
     */
    public int countLowStock(List<ProductStock> stocks) {
        return (int) stocks.stream()
                .filter(s -> s.getQuantity() < (s.getMinStockLevel() != null ? s.getMinStockLevel() : DEFAULT_MIN_STOCK_LEVEL))
                .count();
    }

    /**
     * Counts items with zero quantity.
     *
     * @param stocks list of stock records
     * @return count of out of stock items
     */
    public int countOutOfStock(List<ProductStock> stocks) {
        return (int) stocks.stream()
                .filter(s -> s.getQuantity() == 0)
                .count();
    }

    /**
     * Calculates warehouse utilization percentage.
     *
     * @param warehouse warehouse entity
     * @param stocks list of stock records
     * @return utilization percentage (0-100)
     */
    public double calculateUtilization(Warehouse warehouse, List<ProductStock> stocks) {
        if (warehouse.getTotalCapacity() == null || warehouse.getTotalCapacity() == 0) {
            return ZERO_VALUE;
        }
        double totalItems = stocks.stream().mapToInt(ProductStock::getQuantity).sum();
        return (totalItems / warehouse.getTotalCapacity()) * PERCENTAGE_MULTIPLIER;
    }

    /**
     * Creates warehouse stock DTO from stock record.
     *
     * @param stock stock record
     * @param warehouse warehouse entity
     * @param totalQuantity total quantity for percentage calculation
     * @return warehouse stock DTO
     */
    public WarehouseStockDto createWarehouseStockDto(ProductStock stock, Warehouse warehouse, int totalQuantity) {
        int reserved = stock.getReservedQuantity() != null ? stock.getReservedQuantity() : DEFAULT_RESERVED_QUANTITY;
        int available = stock.getQuantity() - reserved;
        double percentage = totalQuantity > 0 ? (stock.getQuantity() * PERCENTAGE_MULTIPLIER / totalQuantity) : ZERO_VALUE;

        return WarehouseStockDto.builder()
                .warehouseId(stock.getWarehouseId())
                .warehouseName(warehouse != null ? warehouse.getName() : messageService.get("stock.warehouse.unknown"))
                .quantity(stock.getQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(available)
                .percentage(percentage)
                .build();
    }
}