package ru.galtor85.household_store.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product stock DTO", title = "Product Stock")
public class ProductStockDto {

    @Schema(description = "Stock ID", example = "1")
    private Long id;

    @Schema(description = "Product ID", example = "123")
    private Long productId;

    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String productName;

    @Schema(description = "Product SKU", example = "IPHONE-13-PRO-128")
    private String productSku;

    @Schema(description = "Warehouse ID", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String warehouseName;

    @Schema(description = "Quantity in stock", example = "50")
    private Integer quantity;

    @Schema(description = "Reserved quantity", example = "5")
    private Integer reservedQuantity;

    @Schema(description = "Available quantity", example = "45")
    private Integer availableQuantity;

    @Schema(description = "Minimum stock level", example = "10")
    private Integer minStockLevel;

    @Schema(description = "Maximum stock level", example = "100")
    private Integer maxStockLevel;

    @Schema(description = "Reorder point", example = "20")
    private Integer reorderPoint;

    @Schema(description = "Location in warehouse", example = "A-12-03")
    private String locationInWarehouse;

    @Schema(description = "Is low stock", example = "false")
    private Boolean isLowStock;

    @Schema(description = "Stock value", example = "49999.50")
    private Double stockValue;

    @Schema(description = "Last updated", example = "2024-03-18T15:30:00")
    private LocalDateTime updatedAt;
}