package ru.galtor85.household_store.dto.response.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for product availability with warehouse details for manager view.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product availability with warehouse details for manager")
public class ProductAvailabilityWithWarehousesDto {

    @NotNull
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull
    @Schema(description = "Product name", example = "iPhone 13 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productName;

    @Schema(description = "Product SKU", example = "IPHONE-13-PRO-128")
    private String productSku;

    @Builder.Default
    @Schema(description = "Total available across all warehouses", example = "45")
    private Integer totalAvailable = 0;

    @Builder.Default
    @Schema(description = "Stock details by warehouse")
    private List<WarehouseStockDetailDto> warehouses = new ArrayList<>();
}