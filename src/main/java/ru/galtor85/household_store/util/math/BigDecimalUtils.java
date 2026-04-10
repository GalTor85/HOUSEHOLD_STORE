package ru.galtor85.household_store.util.math;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.PaymentConfig;

import java.math.BigDecimal;

/**
 * Utility class for BigDecimal operations.
 */
@Component
@RequiredArgsConstructor
public class BigDecimalUtils {

    private final PaymentConfig paymentConfig;

    public boolean isZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isNullOrZero(BigDecimal value) {
        return value == null || isZero(value);
    }

    public boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isNotPositive(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }

    public BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public BigDecimal calculatePercentage(BigDecimal amount, BigDecimal percent) {
        if (isNullOrZero(percent)) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(percent)
                .divide(BigDecimal.valueOf(paymentConfig.getProcessing().getPercentBase()),
                        paymentConfig.getProcessing().getFeeScale(),
                        paymentConfig.getProcessing().getRoundingMode());
    }

    public BigDecimal applyPercentageDiscount(BigDecimal amount, BigDecimal percent) {
        if (isNullOrZero(percent)) {
            return amount;
        }
        BigDecimal discount = calculatePercentage(amount, percent);
        return amount.subtract(discount);
    }

    public BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        return zeroIfNull(a).add(zeroIfNull(b));
    }

    public BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        return zeroIfNull(a).subtract(zeroIfNull(b));
    }

    public BigDecimal safeMultiply(BigDecimal a, BigDecimal b) {
        return zeroIfNull(a).multiply(zeroIfNull(b));
    }
}