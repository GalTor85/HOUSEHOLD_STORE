package ru.galtor85.household_store.builder.price;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class PriceCalculationResultBuilder {

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

    public PriceCalculationResult buildWithDefaultDiscounts(BigDecimal originalTotal) {
        return build(originalTotal, originalTotal, new ArrayList<>());
    }
}