package ru.galtor85.household_store.dto.request.price;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;

import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_ADDRESS_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_PROMO_CODE_LENGTH;

/**
 * Request DTO for price calculation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Price calculation request DTO", title = "Price Calculation Request")
public class PriceCalculationRequest {

    @NotNull(message = "{price.validation.user.id.empty}")
    @Schema(description = "User ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotEmpty(message = "{price.validation.items.empty}")
    @Valid
    @Schema(description = "List of items in cart", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CartItemDto> items;

    @Size(max = MAX_PROMO_CODE_LENGTH, message = "{price.validation.promo.code.max}")
    @Schema(description = "Promo code", example = "WELCOME10")
    private String promoCode;

    @Size(max = MAX_ADDRESS_LENGTH, message = "{price.validation.shipping.address.max}")
    @Schema(description = "Shipping address for delivery cost calculation", example = "123 Main St, Moscow")
    private String shippingAddress;

    @Schema(description = "Apply user type discounts", example = "true", defaultValue = "true")
    @Builder.Default
    private boolean applyUserTypeDiscounts = true;

    @Schema(description = "Apply promo code", example = "true", defaultValue = "true")
    @Builder.Default
    private boolean applyPromoCode = true;

    @Schema(description = "Apply price rules", example = "true", defaultValue = "true")
    @Builder.Default
    private boolean applyPriceRules = true;
}