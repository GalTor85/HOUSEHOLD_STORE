package ru.galtor85.household_store.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.ProductStockDto;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductStock;
import ru.galtor85.household_store.entity.Warehouse;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.WarehouseRepository;
import ru.galtor85.household_store.service.MessageService;

@Component
@RequiredArgsConstructor
public class StockDtoEnricher {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final MessageService messageService;

    public ProductStockDto enrichStockDto(ProductStock stock) {
        Product product = productRepository.findById(stock.getProductId()).orElse(null);
        Warehouse warehouse = warehouseRepository.findById(stock.getWarehouseId()).orElse(null);

        int reserved = stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0;
        int available = stock.getQuantity() - reserved;
        boolean isLowStock = stock.getMinStockLevel() != null && stock.getQuantity() < stock.getMinStockLevel();

        double stockValue = 0.0;
        if (product != null && product.getPrice() != null) {
            stockValue = product.getPrice().doubleValue() * stock.getQuantity();
        }

        return ProductStockDto.builder()
                .id(stock.getId())
                .productId(stock.getProductId())
                .productName(product != null ? product.getName() :
                        messageService.get("stock.product.unknown"))
                .productSku(product != null ? product.getSku() :
                        messageService.get("stock.product.unknown.sku"))
                .warehouseId(stock.getWarehouseId())
                .warehouseName(warehouse != null ? warehouse.getName() :
                        messageService.get("stock.warehouse.unknown"))
                .quantity(stock.getQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(available)
                .minStockLevel(stock.getMinStockLevel())
                .maxStockLevel(stock.getMaxStockLevel())
                .reorderPoint(stock.getReorderPoint())
                .locationInWarehouse(stock.getLocationInWarehouse())
                .isLowStock(isLowStock)
                .stockValue(stockValue)
                .updatedAt(stock.getUpdatedAt())
                .build();
    }
}