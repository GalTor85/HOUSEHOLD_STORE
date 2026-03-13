package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.dto.CartItemDto;
import ru.galtor85.household_store.dto.PriceCalculationRequest;
import ru.galtor85.household_store.dto.PriceCalculationResult;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCalculationService {

    private final PriceRuleRepository priceRuleRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final ProductPriceRuleRepository productPriceRuleRepository;
    private final UserTypeAssignmentRepository userTypeAssignmentRepository;
    private final MessageService messageService;

    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
        PriceCalculationResult result = PriceCalculationResult.builder()
                .originalTotal(calculateOriginalTotal(request.getItems()))
                .build();

        BigDecimal currentTotal = result.getOriginalTotal();
        List<PriceCalculationResult.AppliedDiscount> appliedDiscounts = new ArrayList<>();

        // 1. Применяем скидки по типу пользователя
        UserType currentUserType = getUserType(request.getUserId());
        currentTotal = applyUserTypeDiscounts(currentTotal, currentUserType, request.getItems(), appliedDiscounts);

        // 2. Применяем правила ценообразования
        currentTotal = applyPriceRules(currentTotal, currentUserType, request.getItems(), appliedDiscounts);

        // 3. Применяем промокод (если есть)
        if (request.getPromoCode() != null && !request.getPromoCode().isEmpty()) {
            currentTotal = applyPromoCode(currentTotal, request.getPromoCode(),
                    request.getUserId(), request.getItems(), appliedDiscounts);
        }

        result.setFinalTotal(currentTotal);
        result.setTotalDiscount(result.getOriginalTotal().subtract(currentTotal));
        result.setAppliedDiscounts(appliedDiscounts);

        return result;
    }

    private BigDecimal calculateOriginalTotal(List<CartItemDto> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private UserType getUserType(Long userId) {
        return userTypeAssignmentRepository.findActiveByUserId(userId)
                .map(UserTypeAssignment::getUserType)
                .orElse(UserType.RETAIL); // По умолчанию розничный
    }

    private BigDecimal applyUserTypeDiscounts(BigDecimal currentTotal, UserType userType,
                                              List<CartItemDto> items,
                                              List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {
        // Базовая скидка по типу пользователя
        double discountPercent = getUserTypeDiscountPercent(userType);
        if (discountPercent > 0) {
            BigDecimal discountAmount = currentTotal
                    .multiply(BigDecimal.valueOf(discountPercent))
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

            appliedDiscounts.add(PriceCalculationResult.AppliedDiscount.builder()
                    .name(messageService.get("discount.user.type", userType.name()))
                    .description(messageService.get("discount.user.type.description", discountPercent))
                    .discountAmount(discountAmount)
                    .type("USER_TYPE")
                    .build());

            return currentTotal.subtract(discountAmount);
        }
        return currentTotal;
    }

    private double getUserTypeDiscountPercent(UserType userType) {
        // Здесь можно получать из БД или конфига
        switch (userType) {
            case WHOLESALE: return 5.0;
            case VIP: return 10.0;
            case PARTNER: return 7.0;
            case EMPLOYEE: return 15.0;
            default: return 0.0;
        }
    }

    private BigDecimal applyPriceRules(BigDecimal currentTotal, UserType userType,
                                       List<CartItemDto> items,
                                       List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {
        List<PriceRule> activeRules = priceRuleRepository.findActiveRulesForUserType(
                userType, LocalDateTime.now());

        // Сортируем по приоритету
        activeRules.sort(Comparator.comparing(PriceRule::getPriority));

        BigDecimal result = currentTotal;
        for (PriceRule rule : activeRules) {
            BigDecimal beforeRule = result;
            result = applySingleRule(result, rule, items);

            if (result.compareTo(beforeRule) < 0) {
                appliedDiscounts.add(PriceCalculationResult.AppliedDiscount.builder()
                        .name(rule.getName())
                        .description(rule.getDescription())
                        .discountAmount(beforeRule.subtract(result))
                        .type("RULE")
                        .build());
            }
        }
        return result;
    }

    private BigDecimal applySingleRule(BigDecimal currentTotal, PriceRule rule, List<CartItemDto> items) {
        switch (rule.getDiscountType()) {
            case PERCENTAGE:
                return currentTotal.multiply(BigDecimal.ONE.subtract(
                        rule.getDiscountValue().divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)));
            case FIXED_AMOUNT:
                return currentTotal.subtract(rule.getDiscountValue()).max(BigDecimal.ZERO);
            case BUY_X_GET_Y:
                return applyBuyXGetY(currentTotal, rule, items);
            default:
                return currentTotal;
        }
    }

    private BigDecimal applyBuyXGetY(BigDecimal currentTotal, PriceRule rule, List<CartItemDto> items) {
        // Логика "купи X получи Y"
        // rule.getDiscountValue() может содержать пары X:Y
        return currentTotal;
    }

    private BigDecimal applyPromoCode(BigDecimal currentTotal, String promoCode,
                                      Long userId, List<CartItemDto> items,
                                      List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {
        Optional<PromoCode> promoOpt = promoCodeRepository.findByCodeAndActiveTrue(promoCode);

        if (promoOpt.isEmpty()) {
            log.warn(messageService.get("promo.code.not.found", promoCode));
            return currentTotal;
        }

        PromoCode promo = promoOpt.get();

        if (!promo.isValid()) {
            log.warn(messageService.get("promo.code.invalid", promoCode));
            return currentTotal;
        }

        // Проверяем тип пользователя
        UserType userType = getUserType(userId);
        if (!promo.getApplicableUserTypes().isEmpty() &&
                !promo.getApplicableUserTypes().contains(userType)) {
            log.warn(messageService.get("promo.code.not.applicable", promoCode));
            return currentTotal;
        }

        // Проверяем минимальную сумму
        if (promo.getMinOrderAmount() != null &&
                currentTotal.compareTo(promo.getMinOrderAmount()) < 0) {
            log.warn(messageService.get("promo.code.min.order", promoCode));
            return currentTotal;
        }

        // Проверяем лимит на пользователя
        if (promo.getPerUserLimit() != null) {
            long userUsage = promoCodeUsageRepository.countByPromoCodeIdAndUserId(promo.getId(), userId);
            if (userUsage >= promo.getPerUserLimit()) {
                log.warn(messageService.get("promo.code.user.limit", promoCode));
                return currentTotal;
            }
        }

        BigDecimal discount = BigDecimal.ZERO;
        switch (promo.getDiscountType()) {
            case PERCENTAGE:
                discount = currentTotal.multiply(promo.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                break;
            case FIXED_AMOUNT:
                discount = promo.getDiscountValue();
                break;
        }

        appliedDiscounts.add(PriceCalculationResult.AppliedDiscount.builder()
                .name(messageService.get("promo.code.discount", promoCode))
                .description(promo.getDescription())
                .discountAmount(discount)
                .type("PROMO_CODE")
                .build());

        return currentTotal.subtract(discount);
    }
}