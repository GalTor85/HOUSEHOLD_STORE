package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Processor for applying user type based discounts.
 *
 * <p>Calculates and applies discounts based on the user's assigned type
 * (RETAIL, WHOLESALE, VIP, PARTNER, EMPLOYEE). Discount percentages are
 * configured in {@link FinancialConfig}.</p>
 *
 * <p>Discount application rules:
 * <ul>
 *   <li>RETAIL users receive no discount</li>
 *   <li>WHOLESALE users receive configured wholesale discount</li>
 *   <li>VIP users receive configured VIP discount</li>
 *   <li>PARTNER users receive configured partner discount</li>
 *   <li>EMPLOYEE users receive configured employee discount</li>
 * </ul>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTypeDiscountProcessor {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final String DISCOUNT_TYPE_USER_TYPE = "USER_TYPE";
    private static final double NO_DISCOUNT = 0.0;

    private final UserTypeAssignmentRepository userTypeAssignmentRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final FinancialConfig financialConfig;

    /**
     * Applies a user type discount to the current total.
     *
     * <p>Retrieves the user's current type and applies the corresponding
     * discount percentage. If the user has no assigned type, defaults to
     * RETAIL (no discount).</p>
     *
     * @param currentTotal the current order total before discount
     * @param userId the ID of the user making the purchase
     * @param appliedDiscounts list to track all applied discounts
     * @return result containing new total, user type, and applied percentage
     */
    public UserTypeDiscountResult applyUserTypeDiscount(BigDecimal currentTotal,
                                                        Long userId,
                                                        List<PriceCalculationResult.AppliedDiscount> appliedDiscounts) {

        // Add null check
        if (currentTotal == null || currentTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug(logMsg.get("discount.user.type.skipped",
                    currentTotal == null ? "null" : currentTotal.toString()));
            return new UserTypeDiscountResult(
                    currentTotal != null ? currentTotal : BigDecimal.ZERO,
                    null,
                    NO_DISCOUNT
            );
        }

        UserType userType = getUserType(userId);
        double discountPercent = getUserTypeDiscountPercent(userType);

        if (discountPercent <= NO_DISCOUNT) {
            log.debug(logMsg.get("discount.user.type.none", userType));
            return new UserTypeDiscountResult(currentTotal, userType, NO_DISCOUNT);
        }

        BigDecimal discountAmount = calculateDiscountAmount(currentTotal, discountPercent);

        addDiscountToTracking(appliedDiscounts, userType, discountPercent, discountAmount);

        BigDecimal totalAfterDiscount = currentTotal.subtract(discountAmount);

        log.debug(logMsg.get("discount.user.type.applied",
                userType, discountPercent, discountAmount));

        return new UserTypeDiscountResult(totalAfterDiscount, userType, discountPercent);
    }

    /**
     * Retrieves the current user type for a user.
     * Defaults to RETAIL if no active assignment exists.
     *
     * @param userId the user ID
     * @return the user's current type
     */
    private UserType getUserType(Long userId) {
        return userTypeAssignmentRepository.findActiveByUserId(userId)
                .map(UserTypeAssignment::getUserType)
                .orElse(UserType.RETAIL);
    }

    /**
     * Gets the discount percentage for a given user type.
     * All values are retrieved from {@link FinancialConfig}.
     *
     * @param userType the user type
     * @return discount percentage (0-100)
     */
    private double getUserTypeDiscountPercent(UserType userType) {
        Double discount = switch (userType) {
            case WHOLESALE -> financialConfig.getDiscounts().getWholesale();
            case VIP -> financialConfig.getDiscounts().getVip();
            case PARTNER -> financialConfig.getDiscounts().getPartner();
            case EMPLOYEE -> financialConfig.getDiscounts().getEmployee();
            default -> null;
        };

        return discount != null ? discount : NO_DISCOUNT;
    }

    /**
     * Calculates the discount amount based on percentage.
     *
     * @param total the current total
     * @param discountPercent the discount percentage
     * @return calculated discount amount
     */
    private BigDecimal calculateDiscountAmount(BigDecimal total, double discountPercent) {
        int scale = financialConfig.getDefaultDecimalPlaces();
        return total
                .multiply(BigDecimal.valueOf(discountPercent))
                .divide(ONE_HUNDRED, scale, RoundingMode.HALF_UP);
    }

    /**
     * Adds the applied discount to the tracking list.
     *
     * @param appliedDiscounts the list of applied discounts
     * @param userType the user type that triggered the discount
     * @param discountPercent the discount percentage
     * @param discountAmount the calculated discount amount
     */
    private void addDiscountToTracking(List<PriceCalculationResult.AppliedDiscount> appliedDiscounts,
                                       UserType userType,
                                       double discountPercent,
                                       BigDecimal discountAmount) {
        appliedDiscounts.add(PriceCalculationResult.AppliedDiscount.builder()
                .name(messageService.get("discount.user.type", userType.name()))
                .description(messageService.get("discount.user.type.description", discountPercent, userType.name()))
                .discountAmount(discountAmount)
                .type(DISCOUNT_TYPE_USER_TYPE)
                .build());
    }

    /**
     * Result of applying a user type discount.
     *
     * @param totalAfterDiscount the order total after discount applied
     * @param userType the user type used for discount calculation
     * @param appliedPercent the discount percentage that was applied
     */
    public record UserTypeDiscountResult(
            BigDecimal totalAfterDiscount,
            UserType userType,
            double appliedPercent
    ) {}
}