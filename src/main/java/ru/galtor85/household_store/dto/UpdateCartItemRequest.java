package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update cart item request DTO", title = "Update Cart Item Request")
public class UpdateCartItemRequest {

    @NotNull(message = "{cart.validation.quantity.empty}")
    @Min(value = 0, message = "{cart.validation.quantity.min}")
    @Schema(description = "New quantity (0 to remove)", example = "3")
    private Integer quantity;
}