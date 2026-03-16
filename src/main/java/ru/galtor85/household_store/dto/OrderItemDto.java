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
@Schema(description = "Order item DTO", title = "Order Item")
public class OrderItemDto {

    @Schema(description = "Order item ID", example = "1")
    private Long id;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product name at time of order", example = "iPhone 13 Pro")
    private String productName;

    @Schema(description = "Product SKU at time of order", example = "IPHONE-13-PRO-128")
    private String productSku;

    @Schema(description = "Quantity ordered", example = "2")
    private Integer quantity;

    @Schema(description = "Price per unit at time of order", example = "999.99")
    private BigDecimal price;

    @Schema(description = "Total price for this item (price * quantity)", example = "1999.98")
    private BigDecimal totalPrice;

    // ДОПОЛНИТЕЛЬНЫЕ ПОЛЯ (опционально)
    @Schema(description = "Discount amount for this item", example = "100.00")
    private BigDecimal discountAmount;

    @Schema(description = "Discounted price", example = "899.99")
    private BigDecimal discountedPrice;

    @Schema(description = "Supplier ID (for purchase orders)", example = "1")
    private Long supplierId;

    @Schema(description = "Supplier SKU", example = "SUP-IPHONE-128")
    private String supplierSku;

    @Schema(description = "Notes for this item")
    private String notes;
}