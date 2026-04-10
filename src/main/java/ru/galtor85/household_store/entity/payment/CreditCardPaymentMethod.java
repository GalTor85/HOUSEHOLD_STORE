package ru.galtor85.household_store.entity.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Credit card payment method
 */
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "credit_cards", schema = "household_schema")
public class CreditCardPaymentMethod extends PaymentMethod {

    @Column(name = "card_number", nullable = false)
    private String cardNumber;

    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Column(name = "cvv", nullable = false)
    private String cvv;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "issuer_bank")
    private String issuerBank;

    @Column(name = "tokenized")
    @Builder.Default
    private boolean tokenized = false;

    @Column(name = "payment_token")
    private String paymentToken;

    @Override
    public boolean validate() {
        // Validate card number, expiry date, etc.
        return cardNumber != null && !cardNumber.isEmpty() &&
                expiryMonth != null && expiryMonth >= 1 && expiryMonth <= 12 &&
                expiryYear != null && expiryYear >= java.time.Year.now().getValue();
    }

    @Override
    public String getMaskedIdentifier() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    @Override
    public PaymentProvider getProvider() {
        // Determine provider based on card brand
        if ("MIR".equalsIgnoreCase(cardBrand)) {
            return PaymentProvider.MIR;
        }
        return PaymentProvider.VISA_MASTERCARD;
    }

    public boolean isExpired() {
        int currentYear = java.time.Year.now().getValue();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        return expiryYear < currentYear || (expiryYear == currentYear && expiryMonth < currentMonth);
    }
}