package ru.galtor85.household_store.service.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment result DTO
 *
 * <p>Contains the result of a payment processing operation,
 * including success status, transaction identifiers, and fee information.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    // =========================================================================
    // STATUS
    // =========================================================================

    /**
     * Indicates whether the payment was successful
     */
    private boolean success;

    /**
     * Error message if payment failed
     */
    private String errorMessage;

    // =========================================================================
    // TRANSACTION IDENTIFIERS
    // =========================================================================

    /**
     * Transaction ID from the payment provider
     */
    private String transactionId;

    /**
     * URL for completing the payment (for redirect-based payments)
     */
    private String paymentUrl;

    // =========================================================================
    // FEE INFORMATION
    // =========================================================================

    /**
     * Fee charged by the payment provider
     */
    private BigDecimal fee;

    /**
     * Net amount after fees (amount - fee)
     */
    private BigDecimal netAmount;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Creates a successful payment result
     *
     * @param transactionId provider transaction ID
     * @return successful payment result
     */
    public static PaymentResult success(String transactionId) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .build();
    }

    /**
     * Creates a successful payment result with payment URL
     *
     * @param transactionId provider transaction ID
     * @param paymentUrl URL for payment completion
     * @return successful payment result
     */
    public static PaymentResult success(String transactionId, String paymentUrl) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .paymentUrl(paymentUrl)
                .build();
    }

    /**
     * Creates a successful payment result with fees
     *
     * @param transactionId provider transaction ID
     * @param fee fee amount
     * @param netAmount net amount after fees
     * @return successful payment result
     */
    public static PaymentResult success(String transactionId, BigDecimal fee, BigDecimal netAmount) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .fee(fee)
                .netAmount(netAmount)
                .build();
    }

    /**
     * Creates a failed payment result
     *
     * @param errorMessage error description
     * @return failed payment result
     */
    public static PaymentResult failure(String errorMessage) {
        return PaymentResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Creates a failed payment result with transaction ID
     *
     * @param transactionId provider transaction ID
     * @param errorMessage error description
     * @return failed payment result
     */
    public static PaymentResult failure(String transactionId, String errorMessage) {
        return PaymentResult.builder()
                .success(false)
                .transactionId(transactionId)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Checks if the result has a payment URL
     *
     * @return true if payment URL is present
     */
    public boolean hasPaymentUrl() {
        return paymentUrl != null && !paymentUrl.isEmpty();
    }

    /**
     * Checks if the result has fee information
     *
     * @return true if fee information is present
     */
    public boolean hasFeeInfo() {
        return fee != null || netAmount != null;
    }

    /**
     * Gets formatted fee amount
     *
     * @return formatted fee string
     */
    public String getFormattedFee() {
        return fee != null ? String.format("%.2f", fee) : "0.00";
    }

    /**
     * Gets formatted net amount
     *
     * @return formatted net amount string
     */
    public String getFormattedNetAmount() {
        return netAmount != null ? String.format("%.2f", netAmount) : "0.00";
    }
}