package ru.galtor85.household_store.dto.response.order;

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
@Schema(description = "Purchase salesOrder item DTO")
public class PurchaseOrderItemDto {

    @Schema(description = "Item ID", example = "1")
    private Long id;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String productName;

    @Schema(description = "Product SKU", example = "IPHONE-13-PRO-128")
    private String productSku;

    @Schema(description = "Quantity", example = "100")
    private Integer quantity;

    @Schema(description = "Price per unit", example = "850.00")
    private BigDecimal price;

    @Schema(description = "Supplier price", example = "800.00")
    private BigDecimal supplierPrice;

    @Schema(description = "Supplier SKU", example = "SUP-IPHONE-128")
    private String supplierSku;

    @Schema(description = "Received quantity", example = "0")
    private Integer receivedQuantity;

    @Schema(description = "Remaining quantity", example = "100")
    private Integer remainingQuantity;

    @Schema(description = "Total price", example = "85000.00")
    private BigDecimal totalPrice;
}