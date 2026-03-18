package ru.galtor85.household_store.dto;

import java.math.BigDecimal;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class WarehouseStockDto {
    private Long warehouseId;
    private String warehouseName;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Double percentage;
    private String productName;
    private String category;
    private String sku;
    private String brand;
    private BigDecimal price;
}