package ru.galtor85.household_store.dto.response.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for warehouse stock details for manager view.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Warehouse stock detail DTO for manager")
public class WarehouseStockDetailDto {

    @Schema(description = "Warehouse ID", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String warehouseName;

    @Builder.Default
    @Schema(description = "Is visible for sale", example = "true")
    private Boolean isVisibleForSale = true;

    @Builder.Default
    @Schema(description = "Quantity in stock", example = "50")
    private Integer quantity = 0;

    @Builder.Default
    @Schema(description = "Reserved quantity", example = "5")
    private Integer reservedQuantity = 0;

    @Builder.Default
    @Schema(description = "Available quantity", example = "45")
    private Integer availableQuantity = 0;

    @Schema(description = "Localized status", example = "В наличии")
    private String localizedStatus;
}