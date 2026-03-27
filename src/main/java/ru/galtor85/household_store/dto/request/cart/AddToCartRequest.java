package ru.galtor85.household_store.dto.request.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for adding an item to shopping cart", title = "Add to Cart Request")
public class AddToCartRequest {

    @NotNull(message = "Product ID cannot be empty")
    @Schema(description = "Unique identifier of the product to add",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull(message = "Quantity cannot be empty")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Number of items to add",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}