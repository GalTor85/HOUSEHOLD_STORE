package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.calculator.StockCalculator;
import ru.galtor85.household_store.dto.TopProductDto;
import ru.galtor85.household_store.dto.WarehouseStockSummaryDto;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductStock;
import ru.galtor85.household_store.entity.Warehouse;
import ru.galtor85.household_store.repository.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseSummaryProcessor {

    private final ProductRepository productRepository;
    private final StockCalculator stockCalculator;

    public WarehouseStockSummaryDto buildSummary(Warehouse warehouse, List<ProductStock> stocks) {
        int totalProducts = stocks.size();
        int totalItems = stockCalculator.sumQuantity(stocks);
        double totalValue = stockCalculator.calculateTotalValue(stocks);
        int lowStockCount = stockCalculator.countLowStock(stocks);
        int outOfStockCount = stockCalculator.countOutOfStock(stocks);

        List<TopProductDto> topProducts = stocks.stream()
                .map(this::createTopProductDto)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        return WarehouseStockSummaryDto.builder()
                .warehouseId(warehouse.getId())
                .warehouseName(warehouse.getName())
                .totalProducts(totalProducts)
                .totalItems(totalItems)
                .totalValue(totalValue)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .utilizationPercentage(stockCalculator.calculateUtilization(warehouse, stocks))
                .topProducts(topProducts)
                .build();
    }

    private TopProductDto createTopProductDto(ProductStock stock) {
        Product product = productRepository.findById(stock.getProductId()).orElse(null);
        double value = product != null && product.getPrice() != null ?
                product.getPrice().doubleValue() * stock.getQuantity() : 0;

        return new TopProductDto(
                stock.getProductId(),
                product != null ? product.getName() : "Unknown",
                stock.getQuantity(),
                value
        );
    }
}