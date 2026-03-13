package ru.galtor85.household_store.dto;

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
public class PriceCalculationResult {
    private BigDecimal originalTotal;
    private BigDecimal finalTotal;
    private BigDecimal totalDiscount;

    @Builder.Default
    private List<AppliedDiscount> appliedDiscounts = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliedDiscount {
        private String name;
        private String description;
        private BigDecimal discountAmount;
        private String type; // RULE, PROMO_CODE, USER_TYPE
    }
}