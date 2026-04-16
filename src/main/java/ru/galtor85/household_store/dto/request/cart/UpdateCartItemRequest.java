package ru.galtor85.household_store.dto.request.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.MIN_QUANTITY;

/**
 * Request DTO for updating an item in shopping cart.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update cart item request")
public class UpdateCartItemRequest {

    @NotNull(message = "{cart.validation.quantity.empty}")
    @Min(value = MIN_QUANTITY, message = "{cart.validation.quantity.min}")
    @Schema(description = "New quantity (0 to remove)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}