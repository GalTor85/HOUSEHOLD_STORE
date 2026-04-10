package ru.galtor85.household_store.dto.request.cart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.MIN_POSITIVE_QUANTITY;

/**
 * Request DTO for adding an item to shopping cart.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for adding an item to shopping cart", title = "Add to Cart Request")
public class AddToCartRequest {

    @NotNull(message = "{cart.validation.product.id.empty}")
    @Schema(description = "Unique identifier of the product to add",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull(message = "{cart.validation.quantity.empty}")
    @Min(value = MIN_POSITIVE_QUANTITY, message = "{cart.validation.quantity.min}")
    @Schema(description = "Number of items to add",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

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
    public boolean isValidQuantity() {
        return quantity != null && quantity >= MIN_POSITIVE_QUANTITY;
    }
}