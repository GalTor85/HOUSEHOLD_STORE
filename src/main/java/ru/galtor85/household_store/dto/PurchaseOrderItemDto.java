package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Purchase order item DTO", title = "Purchase Order Item")
public class PurchaseOrderItemDto {

    @NotNull(message = "{purchase.validation.product.id.empty}")
    @Positive(message = "{purchase.validation.product.id.positive}")
    @Schema(description = "Product ID", example = "1", required = true)
    private Long productId;

    @NotNull(message = "{purchase.validation.quantity.empty}")
    @Positive(message = "{purchase.validation.quantity.positive}")
    @Schema(description = "Quantity to purchase", example = "100", required = true)
    private Integer quantity;

    @Schema(description = "Supplier SKU (if different from product SKU)", example = "SUP-IPHONE-128")
    private String supplierSku;

    @Schema(description = "Custom purchase price (overrides supplier's default price)", example = "850.00")
    private BigDecimal customPrice;

    @Schema(description = "Notes for this specific item")
    private String notes;
}