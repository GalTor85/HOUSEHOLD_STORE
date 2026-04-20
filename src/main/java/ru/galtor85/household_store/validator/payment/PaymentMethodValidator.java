package ru.galtor85.household_store.validator.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.dto.request.payment.CreatePaymentMethodWithTypesRequest;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentMethodType;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.repository.payment.PaymentMethodRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for payment method operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMethodValidator {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_BANK_NAME_LENGTH = 200;
    private static final int CURRENCY_CODE_LENGTH = 3;
    private static final BigDecimal MAX_PROCESSING_FEE = BigDecimal.valueOf(100);

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("RUB", "USD", "EUR", "KZT");
    private static final Set<UserType> ALL_USER_TYPES = Set.of(UserType.values());

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{16}$");
    private static final Pattern EXPIRY_DATE_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/([0-9]{2})$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3,4}$");
    private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile("^\\d{20}$");
    private static final Pattern BIC_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final PaymentMethodRepository paymentMethodRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final FinancialConfig financialConfig;

    /**
     * Validates that payment method exists.
     *
     * @param methodId the payment method ID
     * @return PaymentMethod entity
     * @throws IllegalArgumentException if not found
     */
    public PaymentMethod validatePaymentMethodExists(Long methodId) {
        return paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("payment.method.not.found", methodId));
                    return new IllegalArgumentException(
                            messageService.get("payment.method.not.found", methodId));
                });
    }

    /**
     * Validates create payment method with types request.
     *
     * @param request the creation request
     * @throws IllegalArgumentException if validation fails
     */
    public void validateCreateWithTypesRequest(CreatePaymentMethodWithTypesRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.request.null"));
        }
        validateName(request.getName());
        validateMethodType(request.getMethodType());
        validateCurrency(request.getCurrency());
        validateProcessingFee(request.getProcessingFee());
        validateUserTypes(request.getAvailableForUserTypes());
        validateTypeSpecificFieldsForTypesRequest(request);
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(messageService.get("payment.validation.name.required"));
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.name.max.length", MAX_NAME_LENGTH));
        }
    }

    private void validateMethodType(PaymentMethodType methodType) {
        if (methodType == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.method.type.required"));
        }
    }

    private void validateCurrency(String currency) {
        String effectiveCurrency = currency;
        if (effectiveCurrency == null || effectiveCurrency.isBlank()) {
            effectiveCurrency = financialConfig.getDefaultCurrency();
        }
        if (effectiveCurrency.length() != CURRENCY_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.currency.invalid"));
        }
        if (!SUPPORTED_CURRENCIES.contains(effectiveCurrency.toUpperCase())) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.currency.not.supported", effectiveCurrency));
        }
    }

    private void validateProcessingFee(BigDecimal fee) {
        if (fee == null) {
            return;
        }
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(messageService.get("payment.validation.processing.fee.min"));
        }
        if (fee.compareTo(MAX_PROCESSING_FEE) > 0) {
            throw new IllegalArgumentException(messageService.get("payment.validation.processing.fee.max", MAX_PROCESSING_FEE));
        }
    }

    private void validateUserTypes(Set<UserType> userTypes) {
        if (userTypes == null || userTypes.isEmpty()) {
            throw new IllegalArgumentException(messageService.get("payment.validation.user.type.required"));
        }
        for (UserType userType : userTypes) {
            if (!ALL_USER_TYPES.contains(userType)) {
                throw new IllegalArgumentException(
                        messageService.get("payment.validation.user.type.invalid", userType));
            }
        }
    }

    private void validateTypeSpecificFieldsForTypesRequest(CreatePaymentMethodWithTypesRequest request) {
        if (request.getMethodType() == null) {
            return;
        }
        switch (request.getMethodType()) {
            case CREDIT_CARD -> validateCreditCardFields(request);
            case BANK_ACCOUNT -> validateBankAccountFields(request);
            case ELECTRONIC -> validateElectronicFields(request);
            case CRYPTO -> validateCryptoFields(request);
            case MOBILE_PAYMENT -> validateMobilePaymentFields(request);
            case CASH, INSTALLMENT -> { /* No additional validation needed */ }
        }
    }

    private void validateCreditCardFields(CreatePaymentMethodWithTypesRequest request) {
        if (request.getCardNumber() == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.card.number.required"));
        }
        if (!CARD_NUMBER_PATTERN.matcher(request.getCardNumber().replaceAll("\\s", "")).matches()) {
            throw new IllegalArgumentException(messageService.get("payment.validation.card.number.invalid"));
        }
        if (request.getExpiryDate() == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.card.expiry.required"));
        }
        if (!EXPIRY_DATE_PATTERN.matcher(request.getExpiryDate()).matches()) {
            throw new IllegalArgumentException(messageService.get("payment.validation.card.expiry.invalid"));
        }
        if (request.getCvv() == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.card.cvv.required"));
        }
        if (!CVV_PATTERN.matcher(request.getCvv()).matches()) {
            throw new IllegalArgumentException(messageService.get("payment.validation.card.cvv.invalid"));
        }
    }

    private void validateBankAccountFields(CreatePaymentMethodWithTypesRequest request) {
        if (request.getBankAccountNumber() == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.bank.account.number.required"));
        }
        if (!BANK_ACCOUNT_PATTERN.matcher(request.getBankAccountNumber().replaceAll("\\s", "")).matches()) {
            throw new IllegalArgumentException(messageService.get("payment.validation.bank.account.number.invalid"));
        }
        if (request.getBankBic() == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.bank.bic.required"));
        }
        if (!BIC_PATTERN.matcher(request.getBankBic()).matches()) {
            throw new IllegalArgumentException(messageService.get("payment.validation.bank.bic.invalid"));
        }
        if (request.getBankName() == null || request.getBankName().trim().isEmpty()) {
            throw new IllegalArgumentException(messageService.get("payment.validation.bank.name.required"));
        }
        if (request.getBankName().length() > MAX_BANK_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.bank.name.max", MAX_BANK_NAME_LENGTH));
        }
    }

    private void validateElectronicFields(CreatePaymentMethodWithTypesRequest request) {
        if (request.getWalletId() == null && request.getWalletPhone() == null && request.getWalletEmail() == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.wallet.identifier.required"));
        }
        if (request.getWalletEmail() != null && !EMAIL_PATTERN.matcher(request.getWalletEmail()).matches()) {
            throw new IllegalArgumentException(messageService.get("payment.validation.wallet.email.invalid"));
        }
    }

    private void validateCryptoFields(CreatePaymentMethodWithTypesRequest request) {
        if (request.getWalletId() == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.crypto.wallet.required"));
        }
    }

    private void validateMobilePaymentFields(CreatePaymentMethodWithTypesRequest request) {
        if (request.getWalletPhone() == null && request.getWalletId() == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.mobile.payment.required"));
        }
    }
}