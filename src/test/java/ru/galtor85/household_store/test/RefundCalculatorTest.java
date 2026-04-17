package ru.galtor85.household_store.test;

import org.junit.jupiter.api.Test;
import ru.galtor85.household_store.util.calculator.RefundCalculator;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class RefundCalculatorTest {

    @Test
    void testCalculateProportionalRefund() {
        BigDecimal paymentAmount = BigDecimal.valueOf(1000);
        BigDecimal invoiceAmount = BigDecimal.valueOf(2000);
        BigDecimal totalRefund = BigDecimal.valueOf(500);

        BigDecimal result = RefundCalculator.calculateProportionalRefund(
                paymentAmount, invoiceAmount, totalRefund);

        assertEquals(BigDecimal.valueOf(250), result);
    }

    @Test
    void testCalculateProportionalRefundWithZeroInvoice() {
        BigDecimal result = RefundCalculator.calculateProportionalRefund(
                BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(500));

        assertEquals(BigDecimal.ZERO, result);
    }
}