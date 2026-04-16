package ru.galtor85.household_store.builder.price;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for price calculation results.
 */
@Component
public class PriceCalculationResultBuilder {

    /**
     * Builds price calculation result with applied discounts.
     *
     * @param originalTotal original total before discounts
     * @param finalTotal final total after discounts
     * @param appliedDiscounts list of applied discounts
     * @return price calculation result
     */
    public PriceCalculationResult build(BigDecimal originalTotal,
                                        BigDecimal finalTotal,
                                        List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {
        return PriceCalculationResult.builder()
                .originalTotal(originalTotal)
                .finalTotal(finalTotal)
                .totalDiscount(originalTotal.subtract(finalTotal))
                .appliedDiscounts(appliedDiscounts != null ? appliedDiscounts : new ArrayList<>())
                .build();
    }
}