package ru.galtor85.household_store.dto.response.cart;

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
@Schema(description = "Cart item DTO", title = "Cart Item")
public class CartItemDto {

    @Schema(description = "Cart item ID", example = "1")
    private Long id;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String productName;

    @Schema(description = "SKU", example = "IPHONE-13-PRO-128")
    private String sku;

    @Schema(description = "Category", example = "Electronics")
    private String category;

    @Schema(description = "Quantity", example = "2")
    private Integer quantity;

    @Schema(description = "Price per unit", example = "999.99")
    private BigDecimal price;

    @Schema(description = "Total price", example = "1999.98")
    private BigDecimal totalPrice;
}