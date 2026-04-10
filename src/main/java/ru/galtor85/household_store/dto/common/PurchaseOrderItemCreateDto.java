package ru.galtor85.household_store.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * DTO for purchase order item creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Purchase order item create request")
public class PurchaseOrderItemCreateDto {

    @NotNull(message = "{purchase.validation.product.id.empty}")
    @Positive(message = "{purchase.validation.product.id.positive}")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull(message = "{purchase.validation.quantity.empty}")
    @Positive(message = "{purchase.validation.quantity.positive}")
    @Schema(description = "Quantity", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @DecimalMin(value = MIN_PRICE_STR, message = "{purchase.validation.custom.price.min}")
    @Schema(description = "Custom price (overrides supplier price)", example = "850.00")
    private BigDecimal customPrice;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasProductId() {
        return productId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasQuantity() {
        return quantity != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCustomPrice() {
        return customPrice != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidQuantity() {
        return quantity != null && quantity > 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidCustomPrice() {
        return customPrice != null && customPrice.compareTo(BigDecimal.ZERO) > 0;
    }
}