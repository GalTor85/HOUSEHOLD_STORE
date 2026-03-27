package ru.galtor85.household_store.processor.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.builder.price.PriceCalculationResultBuilder;
import ru.galtor85.household_store.calculator.BasePriceCalculator;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;
import ru.galtor85.household_store.dto.request.price.PriceCalculationRequest;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.processor.user.UserTypeDiscountProcessor;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCalculationProcessor {

    private final BasePriceCalculator basePriceCalculator;
    private final UserTypeDiscountProcessor userTypeDiscountProcessor;
    private final PriceRuleProcessor priceRuleProcessor;
    private final PromoCodeProcessor promoCodeProcessor;
    private final PriceCalculationResultBuilder resultBuilder;
    private final MessageService messageService;

    /**
     * Рассчитывает итоговую цену с учетом всех скидок
     */
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {

        log.debug(messageService.get("price.calculation.processor.start"));

        // 1. Расчет оригинальной суммы
        BigDecimal originalTotal = basePriceCalculator.calculateOriginalTotal(request.getItems());
        BigDecimal currentTotal = originalTotal;
        List<PriceCalculationResult.AppliedDiscount> appliedDiscounts = new ArrayList<>();

        // 2. Применяем скидки по типу пользователя
        UserTypeDiscountProcessor.UserTypeDiscountResult userTypeResult =
                userTypeDiscountProcessor.applyUserTypeDiscount(
                        currentTotal, request.getUserId(), appliedDiscounts);
        currentTotal = userTypeResult.getTotalAfterDiscount();
        UserType userType = userTypeResult.getUserType();

        // 3. Применяем правила ценообразования
        currentTotal = priceRuleProcessor.applyPriceRules(
                currentTotal, userType, request.getItems(), appliedDiscounts);

        // 4. Применяем промокод (если есть)
        if (request.getPromoCode() != null && !request.getPromoCode().isEmpty()) {
            PromoCodeProcessor.PromoCodeResult promoResult = promoCodeProcessor.applyPromoCode(
                    currentTotal, request.getPromoCode(), request.getUserId(),
                    userType, request.getItems(), appliedDiscounts);

            if (promoResult.isApplied()) {
                currentTotal = promoResult.getTotalAfterDiscount();
            }
        }

        // 5. Строим результат
        PriceCalculationResult result = resultBuilder.build(
                originalTotal, currentTotal, appliedDiscounts);

        log.info(messageService.get("price.calculation.processor.complete",
                originalTotal, result.getFinalTotal(), appliedDiscounts.size()));

        return result;
    }

    /**
     * Рассчитывает цену для списка товаров (упрощенная версия)
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
     * Рассчитывает цену с промокодом
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
     * Рассчитывает только базовую цену без скидок
     */
    public BigDecimal calculateBasePrice(List<CartItemDto> items) {
        return basePriceCalculator.calculateOriginalTotal(items);
    }
}