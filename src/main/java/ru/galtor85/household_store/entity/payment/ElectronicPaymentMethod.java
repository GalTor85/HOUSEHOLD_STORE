package ru.galtor85.household_store.entity.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "electronic_payments", schema = "household_schema")
@EqualsAndHashCode(callSuper = true)
public class ElectronicPaymentMethod extends PaymentMethod {

    @Column(name = "wallet_id", nullable = false)
    private String walletId;

    @Column(name = "wallet_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ElectronicWalletType walletType;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "is_verified")
    @Builder.Default
    private boolean verified = false;

    @Override
    public boolean validate() {
        return walletId != null && !walletId.isEmpty() && walletType != null;
    }

    @Override
    public String getMaskedIdentifier() {
        if (walletId == null || walletId.length() <= 4) {
            return "***";
        }
        return "***" + walletId.substring(walletId.length() - 4);
    }

    @Override
    public PaymentProvider getProvider() {
        if (walletType == null) {
            return PaymentProvider.STRIPE;
        }

        return switch (walletType) {
            case YOOMONEY -> PaymentProvider.YOOMONEY;
            case QIWI -> PaymentProvider.QIWI;
            case WEBMONEY -> PaymentProvider.WEBMONEY;
            case PAYPAL -> PaymentProvider.PAYPAL;
            case STRIPE -> PaymentProvider.STRIPE;
            case CRYPTO -> PaymentProvider.CRYPTO;
        };
    }
}