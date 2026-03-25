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
@Schema(description = "Sales order item DTO")
public class SalesOrderItemDto {

    @Schema(description = "Item ID", example = "1")
    private Long id;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String productName;

    @Schema(description = "Product SKU", example = "IPHONE-13-PRO-128")
    private String productSku;

    @Schema(description = "Quantity", example = "2")
    private Integer quantity;

    @Schema(description = "Price per unit", example = "999.99")
    private BigDecimal price;

    @Schema(description = "Total price", example = "1999.98")
    private BigDecimal totalPrice;

    @Schema(description = "Discount amount", example = "100.00")
    private BigDecimal discountAmount;

    @Schema(description = "Discounted price", example = "899.99")
    private BigDecimal discountedPrice;

    @Schema(description = "Notes", example = "Gift wrapping")
    private String notes;
}