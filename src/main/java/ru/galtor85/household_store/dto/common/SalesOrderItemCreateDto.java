package ru.galtor85.household_store.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.MIN_PRICE_STR;

/**
 * DTO for sales order item creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sales order item create request")
public class SalesOrderItemCreateDto {

    @NotNull(message = "{sales.validation.product.id.empty}")
    @Positive(message = "{sales.validation.product.id.positive}")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull(message = "{sales.validation.quantity.empty}")
    @Positive(message = "{sales.validation.quantity.positive}")
    @Schema(description = "Quantity", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @DecimalMin(value = MIN_PRICE_STR, message = "{sales.validation.custom.price.min}")
    @Schema(description = "Custom price (overrides product price)", example = "999.00")
    private BigDecimal customPrice;
}