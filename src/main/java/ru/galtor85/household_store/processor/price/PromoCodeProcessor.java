package ru.galtor85.household_store.processor.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.entity.promotion.PromoCode;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.repository.promotion.PromoCodeRepository;
import ru.galtor85.household_store.repository.promotion.PromoCodeUsageRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromoCodeProcessor {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final MessageService messageService;

    public PromoCodeResult applyPromoCode(BigDecimal currentTotal, String promoCode,
                                          Long userId, UserType userType,
                                          List<CartItemDto> items,
                                          List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {

        Optional<PromoCode> promoOpt = promoCodeRepository.findByCodeAndActiveTrue(promoCode);

        if (promoOpt.isEmpty()) {
            log.warn(messageService.get("promo.code.not.found", promoCode));
            return new PromoCodeResult(currentTotal, false, "NOT_FOUND");
        }

        PromoCode promo = promoOpt.get();

        // Валидация промокода
        PromoCodeValidationResult validation = validatePromoCode(promo, userId, userType, currentTotal);

        if (!validation.isValid()) {
            return new PromoCodeResult(currentTotal, false, validation.getReason());
        }

        // Расчет скидки
        BigDecimal discount = calculatePromoDiscount(currentTotal, promo);

        // Сохраняем использование
        // savePromoCodeUsage(promo, userId);

        appliedDiscounts.add(PriceCalculationResult.AppliedDiscount.builder()
                .name(messageService.get("promo.code.discount", promoCode))
                .description(promo.getDescription())
                .discountAmount(discount)
                .type("PROMO_CODE")
                .build());

        log.info(messageService.get("promo.code.applied",
                promoCode, discount, userId));

        return new PromoCodeResult(
                currentTotal.subtract(discount),
                true,
                "APPLIED"
        );
    }

    private PromoCodeValidationResult validatePromoCode(PromoCode promo, Long userId,
                                                        UserType userType, BigDecimal currentTotal) {

        if (!promo.isValid()) {
            log.warn(messageService.get("promo.code.invalid", promo.getCode()));
            return new PromoCodeValidationResult(false, "INVALID");
        }

        // Проверяем тип пользователя
        if (!promo.getApplicableUserTypes().isEmpty() &&
                !promo.getApplicableUserTypes().contains(userType)) {
            log.warn(messageService.get("promo.code.not.applicable", promo.getCode()));
            return new PromoCodeValidationResult(false, "NOT_APPLICABLE");
        }

        // Проверяем минимальную сумму
        if (promo.getMinOrderAmount() != null &&
                currentTotal.compareTo(promo.getMinOrderAmount()) < 0) {
            log.warn(messageService.get("promo.code.min.order", promo.getCode()));
            return new PromoCodeValidationResult(false, "MIN_ORDER_NOT_MET");
        }

        // Проверяем лимит на пользователя
        if (promo.getPerUserLimit() != null) {
            long userUsage = promoCodeUsageRepository.countByPromoCodeIdAndUserId(promo.getId(), userId);
            if (userUsage >= promo.getPerUserLimit()) {
                log.warn(messageService.get("promo.code.user.limit", promo.getCode()));
                return new PromoCodeValidationResult(false, "USER_LIMIT_REACHED");
            }
        }

        // Проверяем общий лимит
        if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            log.warn(messageService.get("promo.code.global.limit", promo.getCode()));
            return new PromoCodeValidationResult(false, "GLOBAL_LIMIT_REACHED");
        }

        return new PromoCodeValidationResult(true, "VALID");
    }

    private BigDecimal calculatePromoDiscount(BigDecimal currentTotal, PromoCode promo) {
        return switch (promo.getDiscountType()) {
            case PERCENTAGE -> {
                BigDecimal discount = currentTotal.multiply(promo.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                log.debug(messageService.get("promo.discount.percentage.applied",
                        promo.getCode(), discount));
                yield discount;
            }

            case FIXED_AMOUNT -> {
                BigDecimal discount = promo.getDiscountValue().min(currentTotal);
                log.debug(messageService.get("promo.discount.fixed.applied",
                        promo.getCode(), discount));
                yield discount;
            }

            case BUY_X_GET_Y -> {
                BigDecimal discount = calculateBuyXGetYDiscount(currentTotal, promo);
                log.debug(messageService.get("promo.discount.buyxgety.applied",
                        promo.getCode(), discount));
                yield discount;
            }

            case FREE_SHIPPING -> {
                log.debug(messageService.get("promo.discount.free.shipping",
                        promo.getCode()));
                yield BigDecimal.ZERO; // Бесплатная доставка не влияет на сумму товаров
            }

            case BUNDLE -> {
                BigDecimal discount = calculateBundleDiscount(currentTotal, promo);
                log.debug(messageService.get("promo.discount.bundle.applied",
                        promo.getCode(), discount));
                yield discount;
            }
        };
    }

    /**
     * Расчет скидки по акции "Купи X получи Y"
     */
    private BigDecimal calculateBuyXGetYDiscount(BigDecimal currentTotal, PromoCode promo) {
        // Формат: "2:1" - купи 2 получи 1 бесплатно
        String rule = promo.getDiscountValue().toString();
        try {
            String[] parts = rule.split(":");
            if (parts.length == 2) {
                int buyQuantity = Integer.parseInt(parts[0]);
                int freeQuantity = Integer.parseInt(parts[1]);

                // TODO: Реализовать логику расчета на основе товаров в корзине
                // Для простоты пока возвращаем фиксированную скидку
                BigDecimal discount = currentTotal.multiply(BigDecimal.valueOf(0.1)); // 10%

                log.debug("Buy X Get Y discount calculated: buy {}, free {}", buyQuantity, freeQuantity);
                return discount;
            }
        } catch (Exception e) {
            log.error("Invalid BUY_X_GET_Y rule format: {}", rule, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Расчет скидки для комплекта товаров
     */
    private BigDecimal calculateBundleDiscount(BigDecimal currentTotal, PromoCode promo) {
        // Формат: "3:20" - купи 3 товара, получи скидку 20%
        String rule = promo.getDiscountValue().toString();
        try {
            String[] parts = rule.split(":");
            if (parts.length == 2) {
                int minItems = Integer.parseInt(parts[0]);
                double discountPercent = Double.parseDouble(parts[1]);

                // TODO: Проверить, что в корзине есть минимум minItems товаров
                // TODO: Проверить, что товары подходят под условия комплекта

                BigDecimal discount = currentTotal
                        .multiply(BigDecimal.valueOf(discountPercent))
                        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

                log.debug("Bundle discount calculated: min items {}, discount {}%", minItems, discountPercent);
                return discount;
            }
        } catch (Exception e) {
            log.error("Invalid BUNDLE rule format: {}", rule, e);
        }
        return BigDecimal.ZERO;
    }

    @lombok.Value
    public static class PromoCodeResult {
        BigDecimal totalAfterDiscount;
        boolean applied;
        String status;
    }

    @lombok.Value
    private static class PromoCodeValidationResult {
        boolean valid;
        String reason;
    }
}