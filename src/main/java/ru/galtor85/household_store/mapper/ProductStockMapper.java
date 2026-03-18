package ru.galtor85.household_store.mapper;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.ProductStockDto;
import ru.galtor85.household_store.entity.ProductStock;

@Component
public class ProductStockMapper {

    public ProductStockDto toDto(ProductStock stock) {
        if (stock == null) return null;

        return ProductStockDto.builder()
                .id(stock.getId())
                .productId(stock.getProductId())
                .warehouseId(stock.getWarehouseId())
                .quantity(stock.getQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(stock.getQuantity() - (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0))
                .minStockLevel(stock.getMinStockLevel())
                .maxStockLevel(stock.getMaxStockLevel())
                .reorderPoint(stock.getReorderPoint())
                .locationInWarehouse(stock.getLocationInWarehouse())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }
}