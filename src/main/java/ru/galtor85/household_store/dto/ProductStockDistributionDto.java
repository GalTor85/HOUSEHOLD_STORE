package ru.galtor85.household_store.dto;
import java.util.List;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ProductStockDistributionDto {
    private Long productId;
    private String productName;
    private String productSku;
    private Integer totalQuantity;
    private Integer totalReserved;
    private Integer totalAvailable;
    private List<WarehouseStockDto> warehouses;
}