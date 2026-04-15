package ru.galtor85.household_store.service.payment;

import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentTransactionStatus;

import java.math.BigDecimal;

/**
 * Payment gateway interface for different providers
 */
public interface PaymentGateway {

    /**
     * Process payment
     *
     * @param paymentMethod payment method (card, wallet, etc.)
     * @param amount payment amount
     * @param currency payment currency
     * @param description payment description
     * @return payment result
     */
    PaymentResult processPayment(PaymentMethod paymentMethod, BigDecimal amount,
                                 String currency, String description);

    /**
     * Refund payment
     *
     * @param paymentMethod payment method
     * @param transactionId provider transaction ID
     * @param amount refund amount
     * @param reason refund reason
     * @return refund result
     */
    PaymentResult refundPayment(PaymentMethod paymentMethod, String transactionId,
                                BigDecimal amount, String reason);

    /**
     * Check payment status
     *
     * @param transactionId provider transaction ID
     * @return payment status
     */
    PaymentTransactionStatus checkStatus(String transactionId);
}