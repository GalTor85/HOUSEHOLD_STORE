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
     * Checks if the result has fee information
     *
     * @return true if fee information is present
     */
    public boolean hasFeeInfo() {
        return fee != null || netAmount != null;
    }

}