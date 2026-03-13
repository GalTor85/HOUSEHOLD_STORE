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
@Schema(description = "Add to cart request DTO", title = "Add to Cart Request")
public class AddToCartRequest {

    @NotNull(message = "{cart.validation.product.id.empty}")
    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @NotNull(message = "{cart.validation.quantity.empty}")
    @Min(value = 1, message = "{cart.validation.quantity.min}")
    @Schema(description = "Quantity", example = "2")
    private Integer quantity;
}