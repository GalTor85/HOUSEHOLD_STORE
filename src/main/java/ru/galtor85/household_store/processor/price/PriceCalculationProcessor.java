package ru.galtor85.household_store.processor.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.builder.price.PriceCalculationResultBuilder;
import ru.galtor85.household_store.calculator.BasePriceCalculator;
import ru.galtor85.household_store.dto.request.price.PriceCalculationRequest;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.processor.user.UserTypeDiscountProcessor;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Processor for price calculation with discounts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCalculationProcessor {

    private final BasePriceCalculator basePriceCalculator;
    private final UserTypeDiscountProcessor userTypeDiscountProcessor;
    private final PriceRuleProcessor priceRuleProcessor;
    private final PromoCodeProcessor promoCodeProcessor;
    private final PriceCalculationResultBuilder resultBuilder;
    private final LogMessageService logMsg;

    /**
     * Calculates final price with all applicable discounts.
     *
     * @param request price calculation request
     * @return PriceCalculationResult with original and final totals
     */
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
        log.debug(logMsg.get("price.calculation.processor.start"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.warn(logMsg.get("price.calculation.processor.no.items"));
            return resultBuilder.build(BigDecimal.ZERO, BigDecimal.ZERO, new ArrayList<>());
        }

        BigDecimal originalTotal = basePriceCalculator.calculateOriginalTotal(request.getItems());
        BigDecimal currentTotal = originalTotal;
        List<PriceCalculationResult.AppliedDiscount> appliedDiscounts = new ArrayList<>();

        UserTypeDiscountProcessor.UserTypeDiscountResult userTypeResult =
                userTypeDiscountProcessor.applyUserTypeDiscount(
                        currentTotal, request.getUserId(), appliedDiscounts);
        currentTotal = userTypeResult.totalAfterDiscount();

        UserType userType = userTypeResult.userType();
        if (userType == null) {
            userType = UserType.RETAIL; // default
        }

        currentTotal = priceRuleProcessor.applyPriceRules(
                currentTotal, userType, request.getItems(), appliedDiscounts);

        Long appliedPromoCodeId = null;
        String appliedPromoCode = null;

        if (request.getPromoCode() != null && !request.getPromoCode().isEmpty()) {
            PromoCodeProcessor.PromoCodeResult promoResult = promoCodeProcessor.applyPromoCode(
                    currentTotal, request.getPromoCode(), request.getUserId(),
                    userType, appliedDiscounts);

            if (promoResult.applied()) {
                currentTotal = promoResult.totalAfterDiscount();
                appliedPromoCodeId = promoResult.promoCode().getId();
                appliedPromoCode = promoResult.promoCode().getCode();
            }
        }

        PriceCalculationResult result = resultBuilder.build(
                originalTotal, currentTotal, appliedDiscounts);

        result.setAppliedPromoCodeId(appliedPromoCodeId);
        result.setAppliedPromoCode(appliedPromoCode);

        log.info(logMsg.get("price.calculation.processor.complete",
                originalTotal, result.getFinalTotal(), appliedDiscounts.size()));

        return result;
    }

}