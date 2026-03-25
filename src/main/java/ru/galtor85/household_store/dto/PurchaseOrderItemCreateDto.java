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
@Schema(description = "Purchase salesOrder item create request")
public class PurchaseOrderItemCreateDto {

    @NotNull(message = "{purchase.validation.product.id.empty}")
    @Positive(message = "{purchase.validation.product.id.positive}")
    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @NotNull(message = "{purchase.validation.quantity.empty}")
    @Positive(message = "{purchase.validation.quantity.positive}")
    @Schema(description = "Quantity", example = "100")
    private Integer quantity;

    @Schema(description = "Custom price (overrides supplier price)", example = "850.00")
    private BigDecimal customPrice;
}