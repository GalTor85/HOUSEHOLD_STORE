package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Price calculation request DTO", title = "Price Calculation Request")
public class PriceCalculationRequest {

    @NotNull(message = "{price.validation.user.id.empty}")
    @Schema(description = "User ID", example = "1", required = true)
    private Long userId;

    @NotEmpty(message = "{price.validation.items.empty}")
    @Valid
    @Schema(description = "List of items in cart", required = true)
    private List<CartItemDto> items;

    @Schema(description = "Promo code", example = "WELCOME10")
    private String promoCode;

    @Schema(description = "User type ID (overrides user's current type)", example = "1")
    private Long userTypeId;

    @Schema(description = "Shipping address for delivery cost calculation", example = "123 Main St, Moscow")
    private String shippingAddress;

    @Schema(description = "Currency code", example = "RUB", defaultValue = "RUB")
    private String currency;

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