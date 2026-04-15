package ru.galtor85.household_store.processor.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.builder.price.PriceCalculationResultBuilder;
import ru.galtor85.household_store.calculator.BasePriceCalculator;
import ru.galtor85.household_store.dto.request.price.PriceCalculationRequest;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;
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

        BigDecimal originalTotal = basePriceCalculator.calculateOriginalTotal(request.getItems());
        BigDecimal currentTotal = originalTotal;
        List<PriceCalculationResult.AppliedDiscount> appliedDiscounts = new ArrayList<>();

        UserTypeDiscountProcessor.UserTypeDiscountResult userTypeResult =
                userTypeDiscountProcessor.applyUserTypeDiscount(
                        currentTotal, request.getUserId(), appliedDiscounts);
        currentTotal = userTypeResult.totalAfterDiscount();
        UserType userType = userTypeResult.userType();

        currentTotal = priceRuleProcessor.applyPriceRules(
                currentTotal, userType, request.getItems(), appliedDiscounts);

        if (request.getPromoCode() != null && !request.getPromoCode().isEmpty()) {
            PromoCodeProcessor.PromoCodeResult promoResult = promoCodeProcessor.applyPromoCode(
                    currentTotal, request.getPromoCode(), request.getUserId(),
                    userType, appliedDiscounts);

            if (promoResult.applied()) {
                currentTotal = promoResult.totalAfterDiscount();
            }
        }

        PriceCalculationResult result = resultBuilder.build(
                originalTotal, currentTotal, appliedDiscounts);

        log.info(logMsg.get("price.calculation.processor.complete",
                originalTotal, result.getFinalTotal(), appliedDiscounts.size()));

        return result;
    }

    /**
     * Calculates price for a list of items.
     *
     * @param items  cart items
     * @param userId user ID
     * @return PriceCalculationResult
     */
    public PriceCalculationResult calculatePriceForItems(List<CartItemDto> items, Long userId) {
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .userId(userId)
                .items(items)
                .applyUserTypeDiscounts(true)
                .applyPriceRules(true)
                .applyPromoCode(true)
                .build();

        return calculatePrice(request);
    }

    /**
     * Calculates price with promo code.
     *
     * @param items     cart items
     * @param userId    user ID
     * @param promoCode promo code
     * @return PriceCalculationResult
     */
    public PriceCalculationResult calculatePriceWithPromoCode(List<CartItemDto> items,
                                                              Long userId,
                                                              String promoCode) {
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .userId(userId)
                .items(items)
                .promoCode(promoCode)
                .applyUserTypeDiscounts(true)
                .applyPriceRules(true)
                .applyPromoCode(true)
                .build();

        return calculatePrice(request);
    }

    /**
     * Calculates base price without discounts.
     *
     * @param items cart items
     * @return base total
     */
    public BigDecimal calculateBasePrice(List<CartItemDto> items) {
        return basePriceCalculator.calculateOriginalTotal(items);
    }
}