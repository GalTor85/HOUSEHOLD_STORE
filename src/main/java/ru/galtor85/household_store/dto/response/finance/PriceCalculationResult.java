package ru.galtor85.household_store.dto.response.finance;

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

        @Schema(description = "Discount code (for promo codes)", example = "WELCOME10")
        private String code;

        @Schema(description = "Discount percentage (if applicable)", example = "10")
        private Integer percentage;
    }

    // Вспомогательный метод для добавления скидки
    public void addDiscount(String name, String description, BigDecimal amount, String type) {
        if (this.appliedDiscounts == null) {
            this.appliedDiscounts = new ArrayList<>();
        }
        this.appliedDiscounts.add(AppliedDiscount.builder()
                .name(name)
                .description(description)
                .discountAmount(amount)
                .type(type)
                .build());
    }

    // Вспомогательный метод для расчета итога
    public void calculateFinalTotal() {
        if (originalTotal == null) {
            this.finalTotal = BigDecimal.ZERO;
            this.totalDiscount = BigDecimal.ZERO;
            return;
        }

        this.totalDiscount = appliedDiscounts.stream()
                .map(AppliedDiscount::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.finalTotal = originalTotal.subtract(totalDiscount);
        if (this.finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            this.finalTotal = BigDecimal.ZERO;
        }
    }
}