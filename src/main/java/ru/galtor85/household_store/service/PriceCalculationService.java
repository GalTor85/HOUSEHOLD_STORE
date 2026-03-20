package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.calculator.BasePriceCalculator;
import ru.galtor85.household_store.dto.PriceCalculationRequest;
import ru.galtor85.household_store.dto.PriceCalculationResult;
import ru.galtor85.household_store.entity.UserType;
import ru.galtor85.household_store.processor.PriceRuleProcessor;
import ru.galtor85.household_store.processor.PromoCodeProcessor;
import ru.galtor85.household_store.processor.UserTypeDiscountProcessor;
import ru.galtor85.household_store.builder.PriceCalculationResultBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCalculationService {

    private final BasePriceCalculator basePriceCalculator;
    private final UserTypeDiscountProcessor userTypeDiscountProcessor;
    private final PriceRuleProcessor priceRuleProcessor;
    private final PromoCodeProcessor promoCodeProcessor;
    private final PriceCalculationResultBuilder resultBuilder;
    private final MessageService messageService;

    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
        log.debug(messageService.get("price.calculation.start"));

        // 1. Расчет оригинальной суммы
        BigDecimal originalTotal = basePriceCalculator.calculateOriginalTotal(request.getItems());
        BigDecimal currentTotal = originalTotal;
        var appliedDiscounts = new ArrayList<PriceCalculationResult.AppliedDiscount>();

        // 2. Применяем скидки по типу пользователя
        var userTypeResult = userTypeDiscountProcessor.applyUserTypeDiscount(
                currentTotal, request.getUserId(), appliedDiscounts);
        currentTotal = userTypeResult.getTotalAfterDiscount();
        UserType userType = userTypeResult.getUserType();

        // 3. Применяем правила ценообразования
        currentTotal = priceRuleProcessor.applyPriceRules(
                currentTotal, userType, request.getItems(), appliedDiscounts);

        // 4. Применяем промокод (если есть)
        if (request.getPromoCode() != null && !request.getPromoCode().isEmpty()) {
            var promoResult = promoCodeProcessor.applyPromoCode(
                    currentTotal, request.getPromoCode(), request.getUserId(),
                    userType, request.getItems(), appliedDiscounts);

            if (promoResult.isApplied()) {
                currentTotal = promoResult.getTotalAfterDiscount();
            }
        }

        // 5. Строим результат
        PriceCalculationResult result = resultBuilder.build(
                originalTotal, currentTotal, appliedDiscounts);

        log.info(messageService.get("price.calculation.complete",
                originalTotal, result.getFinalTotal(),
                appliedDiscounts.size()));

        return result;
    }
}