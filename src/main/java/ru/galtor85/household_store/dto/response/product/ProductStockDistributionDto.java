package ru.galtor85.household_store.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseStockDto;

import java.util.List;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO representing distribution of product stock across warehouses")
public class ProductStockDistributionDto {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String productName;

    @Schema(description = "Product SKU", example = "IPHONE-13-PRO-128")
    private String productSku;

    @Schema(description = "Total quantity in stock across all warehouses", example = "100")
    private Integer totalQuantity;

    @Schema(description = "Total reserved quantity (for pending orders)", example = "5")
    private Integer totalReserved;

    @Schema(description = "Total available quantity (totalQuantity - totalReserved)", example = "95")
    private Integer totalAvailable;

    @Schema(description = "List of stock details per warehouse")
    private List<WarehouseStockDto> warehouses;
}