package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTypeDiscountProcessor {

    private final UserTypeAssignmentRepository userTypeAssignmentRepository;
    private final MessageService messageService;
    private final FinancialConfig financialConfig;

    public UserTypeDiscountResult applyUserTypeDiscount(BigDecimal currentTotal, Long userId,
                                                        List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {
        UserType userType = getUserType(userId);

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

            log.debug(messageService.get("discount.user.type.applied",
                    userType, discountPercent, discountAmount));

            return new UserTypeDiscountResult(
                    currentTotal.subtract(discountAmount),
                    userType,
                    discountPercent
            );
        }

        return new UserTypeDiscountResult(currentTotal, userType, 0.0);
    }

    private UserType getUserType(Long userId) {
        return userTypeAssignmentRepository.findActiveByUserId(userId)
                .map(UserTypeAssignment::getUserType)
                .orElse(UserType.RETAIL);
    }

    private double getUserTypeDiscountPercent(UserType userType) {
        return switch (userType) {
            case WHOLESALE -> financialConfig.getDiscounts().getWholesale() != null ?
                    financialConfig.getDiscounts().getWholesale() : 5.0;
            case VIP -> financialConfig.getDiscounts().getVip() != null ?
                    financialConfig.getDiscounts().getVip() : 10.0;
            case PARTNER -> financialConfig.getDiscounts().getPartner() != null ?
                    financialConfig.getDiscounts().getPartner() : 7.0;
            case EMPLOYEE -> financialConfig.getDiscounts().getEmployee() != null ?
                    financialConfig.getDiscounts().getEmployee() : 15.0;
            default -> 0.0;
        };
    }

    @lombok.Value
    public static class UserTypeDiscountResult {
        BigDecimal totalAfterDiscount;
        UserType userType;
        double appliedPercent;
    }
}