package ru.galtor85.household_store.processor.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.calculator.StockCalculator;
import ru.galtor85.household_store.dto.response.product.ProductStockDistributionDto;
import ru.galtor85.household_store.dto.response.product.ProductStockDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseStockDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.stock.StockDtoEnricher;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStockProcessor {

    private final ProductStockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockCalculator stockCalculator;
    private final StockDtoEnricher dtoEnricher;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public List<ProductStockDto> getProductStockAcrossAllWarehouses(Product product) {
        List<ProductStock> stocks = stockRepository.findByProductId(product.getId());

        log.debug(messageService.get("stock.by.product.all.warehouses",
                stocks.size(), product.getId()));

        return stocks.stream()
                .map(dtoEnricher::enrichStockDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Integer getTotalStockForProduct(Product product) {
        Integer total = stockRepository.getTotalStockForProduct(product.getId());
        total = total != null ? total : 0;

        log.debug(messageService.get("stock.product.total", product.getId(), total));

        return total;
    }

    @Transactional(readOnly = true)
    public ProductStockDistributionDto getProductStockDistribution(Product product) {
        List<ProductStock> stocks = stockRepository.findByProductId(product.getId());

        int totalQuantity = stockCalculator.sumQuantity(stocks);
        int totalReserved = stockCalculator.sumReserved(stocks);
        int totalAvailable = totalQuantity - totalReserved;

        List<WarehouseStockDto> warehouseStocks = stocks.stream()
                .map(stock -> {
                    Warehouse warehouse = warehouseRepository.findById(stock.getWarehouseId()).orElse(null);
                    return stockCalculator.createWarehouseStockDto(stock, warehouse, totalQuantity);
                })
                .collect(Collectors.toList());

        return ProductStockDistributionDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .totalQuantity(totalQuantity)
                .totalReserved(totalReserved)
                .totalAvailable(totalAvailable)
                .warehouses(warehouseStocks)
                .build();
    }

    @Transactional(readOnly = true)
    public ProductStock getProductStockAtWarehouse(Product product, Warehouse warehouse) {
        return stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElse(ProductStock.builder()
                        .productId(product.getId())
                        .warehouseId(warehouse.getId())
                        .quantity(0)
                        .reservedQuantity(0)
                        .availableQuantity(0)
                        .build());
    }
}