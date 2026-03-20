package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for warehouse stock information", title = "Warehouse Stock")
public class WarehouseStockDto {

    @Schema(description = "Warehouse ID", example = "1", required = true)
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String warehouseName;

    @Schema(description = "Total quantity in stock", example = "150")
    private Integer quantity;

    @Schema(description = "Reserved quantity (for pending orders)", example = "15")
    private Integer reservedQuantity;

    @Schema(description = "Available quantity (quantity - reserved)", example = "135")
    private Integer availableQuantity;

    @Schema(description = "Percentage of total stock in this warehouse", example = "45.5")
    private Double percentage;

    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String productName;

    @Schema(description = "Product category", example = "Electronics")
    private String category;

    @Schema(description = "Product SKU (Stock Keeping Unit)", example = "IPHONE-13-PRO-128")
    private String sku;

    @Schema(description = "Product brand", example = "Apple")
    private String brand;

    @Schema(description = "Product price", example = "999.99")
    private BigDecimal price;
}