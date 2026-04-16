package ru.galtor85.household_store.dto.response.finance;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Price calculation result DTO", title = "Price Calculation Result")
public class PriceCalculationResult {

    @Schema(description = "Original total before discounts", example = "1000.00")
    private BigDecimal originalTotal;

    @Schema(description = "Final total after all discounts", example = "850.00")
    private BigDecimal finalTotal;

    @Schema(description = "Total discount amount", example = "150.00")
    private BigDecimal totalDiscount;

    @Builder.Default
    @Schema(description = "List of applied discounts")
    private List<AppliedDiscount> appliedDiscounts = new ArrayList<>();

    @Schema(description = "Applied promo code ID", example = "5")
    private Long appliedPromoCodeId;

    @Schema(description = "Applied promo code", example = "SUMMER20")
    private String appliedPromoCode;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Applied discount details", title = "Applied Discount")
    public static class AppliedDiscount {

        @Schema(description = "Discount name", example = "VIP Customer Discount")
        private String name;

        @Schema(description = "Discount description", example = "10% off for VIP customers")
        private String description;

        @Schema(description = "Discount amount", example = "100.00")
        private BigDecimal discountAmount;

        @Schema(description = "Discount type", example = "USER_TYPE",
                allowableValues = {"RULE", "PROMO_CODE", "USER_TYPE"})
        private String type;
    }
}