package ru.galtor85.household_store.processor.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.entity.promotion.PromoCode;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.repository.promotion.PromoCodeRepository;
import ru.galtor85.household_store.repository.promotion.PromoCodeUsageRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Processor for applying promo codes to orders.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromoCodeProcessor {

    private static final String STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String STATUS_APPLIED = "APPLIED";
    private static final String STATUS_INVALID = "INVALID";
    private static final String STATUS_NOT_APPLICABLE = "NOT_APPLICABLE";
    private static final String STATUS_MIN_ORDER_NOT_MET = "MIN_ORDER_NOT_MET";
    private static final String STATUS_USER_LIMIT_REACHED = "USER_LIMIT_REACHED";
    private static final String STATUS_GLOBAL_LIMIT_REACHED = "GLOBAL_LIMIT_REACHED";
    private static final String STATUS_VALID = "VALID";

    private static final String DISCOUNT_TYPE_PROMO_CODE = "PROMO_CODE";
    private static final BigDecimal TEN_PERCENT = BigDecimal.valueOf(0.1);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private static final int RULE_PARTS_COUNT = 2;
    private static final int BUY_INDEX = 0;
    private static final int FREE_OR_DISCOUNT_INDEX = 1;

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Applies a promo code to the current total.
     *
     * @param currentTotal     current order total
     * @param promoCode        promo code string
     * @param userId           user ID
     * @param userType         user type
     * @param appliedDiscounts list to add applied discount to
     * @return PromoCodeResult with new total and status
     */
    public PromoCodeResult applyPromoCode(BigDecimal currentTotal, String promoCode,
                                          Long userId, UserType userType,
                                          List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {

        Optional<PromoCode> promoOpt = promoCodeRepository.findByCodeAndActiveTrue(promoCode);

        if (promoOpt.isEmpty()) {
            log.warn(logMsg.get("promo.code.not.found", promoCode));
            return new PromoCodeResult(currentTotal, false, STATUS_NOT_FOUND);
        }

        PromoCode promo = promoOpt.get();
        PromoCodeValidationResult validation = validatePromoCode(promo, userId, userType, currentTotal);

        if (!validation.valid()) {
            return new PromoCodeResult(currentTotal, false, validation.reason());
        }

        BigDecimal discount = calculatePromoDiscount(currentTotal, promo);

        appliedDiscounts.add(PriceCalculationResult.AppliedDiscount.builder()
                .name(messageService.get("promo.code.discount", promoCode))
                .description(promo.getDescription())
                .discountAmount(discount)
                .type(DISCOUNT_TYPE_PROMO_CODE)
                .build());

        log.info(logMsg.get("promo.code.applied", promoCode, discount, userId));

        return new PromoCodeResult(
                currentTotal.subtract(discount),
                true,
                STATUS_APPLIED
        );
    }

    private PromoCodeValidationResult validatePromoCode(PromoCode promo, Long userId,
                                                        UserType userType, BigDecimal currentTotal) {

        if (!promo.isValid()) {
            log.warn(logMsg.get("promo.code.invalid", promo.getCode()));
            return new PromoCodeValidationResult(false, STATUS_INVALID);
        }

        if (!promo.getApplicableUserTypes().isEmpty() &&
                !promo.getApplicableUserTypes().contains(userType)) {
            log.warn(logMsg.get("promo.code.not.applicable", promo.getCode()));
            return new PromoCodeValidationResult(false, STATUS_NOT_APPLICABLE);
        }

        if (promo.getMinOrderAmount() != null &&
                currentTotal.compareTo(promo.getMinOrderAmount()) < 0) {
            log.warn(logMsg.get("promo.code.min.order", promo.getCode()));
            return new PromoCodeValidationResult(false, STATUS_MIN_ORDER_NOT_MET);
        }

        if (promo.getPerUserLimit() != null) {
            long userUsage = promoCodeUsageRepository.countByPromoCodeIdAndUserId(promo.getId(), userId);
            if (userUsage >= promo.getPerUserLimit()) {
                log.warn(logMsg.get("promo.code.user.limit", promo.getCode()));
                return new PromoCodeValidationResult(false, STATUS_USER_LIMIT_REACHED);
            }
        }

        if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            log.warn(logMsg.get("promo.code.global.limit", promo.getCode()));
            return new PromoCodeValidationResult(false, STATUS_GLOBAL_LIMIT_REACHED);
        }

        return new PromoCodeValidationResult(true, STATUS_VALID);
    }

    private BigDecimal calculatePromoDiscount(BigDecimal currentTotal, PromoCode promo) {
        return switch (promo.getDiscountType()) {
            case PERCENTAGE -> {
                BigDecimal discount = currentTotal.multiply(promo.getDiscountValue())
                        .divide(ONE_HUNDRED, RoundingMode.HALF_UP);
                log.debug(logMsg.get("promo.discount.percentage.applied", promo.getCode(), discount));
                yield discount;
            }
            case FIXED_AMOUNT -> {
                BigDecimal discount = promo.getDiscountValue().min(currentTotal);
                log.debug(logMsg.get("promo.discount.fixed.applied", promo.getCode(), discount));
                yield discount;
            }
            case BUY_X_GET_Y -> {
                BigDecimal discount = calculateBuyXGetYDiscount(currentTotal, promo);
                log.debug(logMsg.get("promo.discount.buyxgety.applied", promo.getCode(), discount));
                yield discount;
            }
            case FREE_SHIPPING -> {
                log.debug(logMsg.get("promo.discount.free.shipping", promo.getCode()));
                yield BigDecimal.ZERO;
            }
            case BUNDLE -> {
                BigDecimal discount = calculateBundleDiscount(currentTotal, promo);
                log.debug(logMsg.get("promo.discount.bundle.applied", promo.getCode(), discount));
                yield discount;
            }
        };
    }

    private BigDecimal calculateBuyXGetYDiscount(BigDecimal currentTotal, PromoCode promo) {
        String rule = promo.getDiscountValue().toString();
        try {
            String[] parts = rule.split(":");
            if (parts.length == RULE_PARTS_COUNT) {
                int buyQuantity = Integer.parseInt(parts[BUY_INDEX]);
                int freeQuantity = Integer.parseInt(parts[FREE_OR_DISCOUNT_INDEX]);
                BigDecimal discount = currentTotal.multiply(TEN_PERCENT);
                log.debug(logMsg.get("promo.discount.buyxgety.calculated", buyQuantity, freeQuantity));
                return discount;
            }
        } catch (Exception e) {
            log.error(logMsg.get("promo.discount.buyxgety.invalid.format", rule), e);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateBundleDiscount(BigDecimal currentTotal, PromoCode promo) {
        String rule = promo.getDiscountValue().toString();
        try {
            String[] parts = rule.split(":");
            if (parts.length == RULE_PARTS_COUNT) {
                int minItems = Integer.parseInt(parts[BUY_INDEX]);
                double discountPercent = Double.parseDouble(parts[FREE_OR_DISCOUNT_INDEX]);
                BigDecimal discount = currentTotal
                        .multiply(BigDecimal.valueOf(discountPercent))
                        .divide(ONE_HUNDRED, RoundingMode.HALF_UP);
                log.debug(logMsg.get("promo.discount.bundle.calculated", minItems, discountPercent));
                return discount;
            }
        } catch (Exception e) {
            log.error(logMsg.get("promo.discount.bundle.invalid.format", rule), e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Result of promo code application.
     */
    public record PromoCodeResult(BigDecimal totalAfterDiscount, boolean applied, String status) {
    }

    private record PromoCodeValidationResult(boolean valid, String reason) {
    }
}