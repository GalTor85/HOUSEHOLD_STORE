package ru.galtor85.household_store.dto.response.product;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseStockDto;

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