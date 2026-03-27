package ru.galtor85.household_store.calculator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseStockDto;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockCalculator {

    private final MessageService messageService;

    public int sumQuantity(List<ProductStock> stocks) {
        return stocks.stream().mapToInt(ProductStock::getQuantity).sum();
    }

    public int sumReserved(List<ProductStock> stocks) {
        return stocks.stream()
                .mapToInt(s -> s.getReservedQuantity() != null ? s.getReservedQuantity() : 0)
                .sum();
    }

    public double calculateTotalValue(List<ProductStock> stocks) {
        return 0.0; // Будет переопределено в другом месте
    }

    public int countLowStock(List<ProductStock> stocks) {
        return (int) stocks.stream()
                .filter(s -> s.getQuantity() < (s.getMinStockLevel() != null ? s.getMinStockLevel() : 0))
                .count();
    }

    public int countOutOfStock(List<ProductStock> stocks) {
        return (int) stocks.stream()
                .filter(s -> s.getQuantity() == 0)
                .count();
    }

    public double calculateUtilization(Warehouse warehouse, List<ProductStock> stocks) {
        if (warehouse.getTotalCapacity() == null || warehouse.getTotalCapacity() == 0) {
            return 0.0;
        }
        double totalItems = stocks.stream().mapToInt(ProductStock::getQuantity).sum();
        return (totalItems / warehouse.getTotalCapacity()) * 100;
    }

    public WarehouseStockDto createWarehouseStockDto(ProductStock stock, Warehouse warehouse, int totalQuantity) {
        int available = stock.getQuantity() - (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0);
        double percentage = totalQuantity > 0 ? (stock.getQuantity() * 100.0 / totalQuantity) : 0;

        return WarehouseStockDto.builder()
                .warehouseId(stock.getWarehouseId())
                .warehouseName(warehouse != null ? warehouse.getName() :
                        messageService.get("stock.warehouse.unknown"))
                .quantity(stock.getQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(available)
                .percentage(percentage)
                .build();
    }
}