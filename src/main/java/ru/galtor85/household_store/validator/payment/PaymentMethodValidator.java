package ru.galtor85.household_store.validator.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.dto.request.payment.CreatePaymentMethodRequest;
import ru.galtor85.household_store.dto.request.payment.CreatePaymentMethodWithTypesRequest;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentMethodType;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.repository.payment.PaymentMethodRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for payment method operations.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMethodValidator {

    private final PaymentMethodRepository paymentMethodRepository;
    private final MessageService messageService;
    private final FinancialConfig financialConfig;

    // Validation constants
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_BANK_NAME_LENGTH = 200;
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("RUB", "USD", "EUR", "KZT");
    private static final Set<UserType> ALL_USER_TYPES = Set.of(UserType.values());

    // Regex patterns
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{16}$");
    private static final Pattern EXPIRY_DATE_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/([0-9]{2})$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3,4}$");
    private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile("^\\d{20}$");
    private static final Pattern BIC_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    // =========================================================================
    // REQUEST VALIDATION
    // =========================================================================

    /**
     * Validates create payment method request.
     *
     * @param request the creation request
     * @throws IllegalArgumentException if validation fails
     */
    public void validateCreateRequest(CreatePaymentMethodRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.request.null"));
        }

        validateName(request.getName());
        validateMethodType(request.getMethodType());
        validateCurrency(request.getCurrency());
        validateProcessingFee(request.getProcessingFee());
        validateUserTypes(request.getAvailableForUserTypes());
    }

    /**
     * Validates update payment method request.
     *
     * @param request the update request
     * @throws IllegalArgumentException if validation fails
     */
    public void validateUpdateRequest(CreatePaymentMethodRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.request.null"));
        }

        if (request.getName() != null) {
            validateName(request.getName());
        }
        if (request.getCurrency() != null) {
            validateCurrency(request.getCurrency());
        }
        if (request.getProcessingFee() != null) {
            validateProcessingFee(request.getProcessingFee());
        }
    }

    // =========================================================================
    // FIELD VALIDATIONS
    // =========================================================================

    /**
     * Validates payment method name.
     *
     * @param name the payment method name
     * @throws IllegalArgumentException if name is invalid
     */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.name.required"));
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.name.max.length", MAX_NAME_LENGTH));
        }
    }

    /**
     * Validates payment method type.
     *
     * @param methodType the payment method type
     * @throws IllegalArgumentException if type is invalid
     */
    private void validateMethodType(PaymentMethodType methodType) {
        if (methodType == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.method.type.required"));
        }
    }

    /**
     * Validates currency.
     *
     * @param currency the currency code
     * @throws IllegalArgumentException if currency is invalid
     */
    private void validateCurrency(String currency) {
        String effectiveCurrency = currency;
        if (effectiveCurrency == null || effectiveCurrency.isBlank()) {
            effectiveCurrency = financialConfig.getDefaultCurrency();
        }

        if (effectiveCurrency.length() != 3) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.currency.invalid", effectiveCurrency));
        }

        if (!SUPPORTED_CURRENCIES.contains(effectiveCurrency.toUpperCase())) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.currency.not.supported", effectiveCurrency));
        }
    }

    /**
     * Validates processing fee.
     *
     * @param fee the processing fee
     * @throws IllegalArgumentException if fee is invalid
     */
    private void validateProcessingFee(BigDecimal fee) {
        if (fee == null) {
            return;
        }
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.processing.fee.min"));
        }
        if (fee.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.processing.fee.max"));
        }
    }

    /**
     * Validates user types.
     *
     * @param userTypes the set of user types
     * @throws IllegalArgumentException if user types are invalid
     */
    private void validateUserTypes(Set<UserType> userTypes) {
        if (userTypes == null || userTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.user.type.required"));
        }

        for (UserType userType : userTypes) {
            if (!ALL_USER_TYPES.contains(userType)) {
                throw new IllegalArgumentException(
                        messageService.get("payment.validation.user.type.invalid", userType));
            }
        }
    }


    /**
     * Validates bank account specific fields.
     *
     * @param request the creation request
     */
    private void validateBankAccountFields(CreatePaymentMethodRequest request) {
        if (request.getBankAccountNumber() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.bank.account.number.required"));
        }
        if (!BANK_ACCOUNT_PATTERN.matcher(request.getBankAccountNumber().replaceAll("\\s", "")).matches()) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.bank.account.number.invalid"));
        }

        if (request.getBankBic() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.bank.bic.required"));
        }
        if (!BIC_PATTERN.matcher(request.getBankBic()).matches()) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.bank.bic.invalid"));
        }

        if (request.getBankName() == null || request.getBankName().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.bank.name.required"));
        }
        if (request.getBankName().length() > MAX_BANK_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.bank.name.max", MAX_BANK_NAME_LENGTH));
        }
    }


    // =========================================================================
    // ENTITY VALIDATION
    // =========================================================================

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
                    log.error(messageService.get("payment.method.not.found", methodId));
                    return new IllegalArgumentException(
                            messageService.get("payment.method.not.found", methodId));
                });
    }

    /**
     * Validates that payment method is active.
     *
     * @param paymentMethod the payment method
     * @throws IllegalStateException if payment method is inactive
     */
    public void validatePaymentMethodActive(PaymentMethod paymentMethod) {
        if (!paymentMethod.isActive()) {
            throw new IllegalStateException(
                    messageService.get("payment.method.inactive", paymentMethod.getId()));
        }
    }

    /**
     * Validates that payment method can be deleted.
     *
     * @param paymentMethod the payment method
     * @throws IllegalStateException if payment method cannot be deleted
     */
    public void validatePaymentMethodDeletable(PaymentMethod paymentMethod) {
        // TODO: Check if payment method has any pending transactions
        // if (hasPendingTransactions(paymentMethod.getId())) {
        //     throw new IllegalStateException(
        //             messageService.get("payment.method.has.pending.transactions"));
        // }
    }

    /**
     * Validates that payment method is available for user type.
     *
     * @param paymentMethod the payment method
     * @param userType      the user type
     * @throws IllegalArgumentException if not available
     */
    public void validatePaymentMethodAvailableForUserType(PaymentMethod paymentMethod, UserType userType) {
        if (!paymentMethod.isActive()) {
            throw new IllegalStateException(
                    messageService.get("payment.method.inactive", paymentMethod.getId()));
        }
        // This validation should be done with PaymentMethodUserTypeRepository
        // Will be handled in service layer
    }

    /**
     * Validates create payment method with types request.
     *
     * @param request the creation request
     * @throws IllegalArgumentException if validation fails
     */
    public void validateCreateWithTypesRequest(CreatePaymentMethodWithTypesRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.request.null"));
        }

        validateName(request.getName());
        validateMethodType(request.getMethodType());
        validateCurrency(request.getCurrency());
        validateProcessingFee(request.getProcessingFee());
        validateUserTypes(request.getAvailableForUserTypes());
        validateTypeSpecificFieldsForTypesRequest(request);
    }

    /**
     * Validates type-specific fields for CreatePaymentMethodWithTypesRequest.
     *
     * @param request the creation request
     */
    private void validateTypeSpecificFieldsForTypesRequest(CreatePaymentMethodWithTypesRequest request) {
        if (request.getMethodType() == null) {
            return;
        }

        switch (request.getMethodType()) {
            case CREDIT_CARD:
                if (request.getCardNumber() == null) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.card.number.required"));
                }
                if (!CARD_NUMBER_PATTERN.matcher(request.getCardNumber().replaceAll("\\s", "")).matches()) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.card.number.invalid"));
                }
                if (request.getExpiryDate() == null) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.card.expiry.required"));
                }
                if (!EXPIRY_DATE_PATTERN.matcher(request.getExpiryDate()).matches()) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.card.expiry.invalid"));
                }
                if (request.getCvv() == null) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.card.cvv.required"));
                }
                if (!CVV_PATTERN.matcher(request.getCvv()).matches()) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.card.cvv.invalid"));
                }
                break;

            case BANK_ACCOUNT:
                if (request.getBankAccountNumber() == null) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.bank.account.number.required"));
                }
                if (!BANK_ACCOUNT_PATTERN.matcher(request.getBankAccountNumber().replaceAll("\\s", "")).matches()) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.bank.account.number.invalid"));
                }
                if (request.getBankBic() == null) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.bank.bic.required"));
                }
                if (!BIC_PATTERN.matcher(request.getBankBic()).matches()) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.bank.bic.invalid"));
                }
                if (request.getBankName() == null || request.getBankName().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.bank.name.required"));
                }
                if (request.getBankName().length() > MAX_BANK_NAME_LENGTH) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.bank.name.max", MAX_BANK_NAME_LENGTH));
                }
                break;

            case ELECTRONIC:
                if (request.getWalletId() == null && request.getWalletPhone() == null && request.getWalletEmail() == null) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.wallet.identifier.required"));
                }
                if (request.getWalletEmail() != null && !EMAIL_PATTERN.matcher(request.getWalletEmail()).matches()) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.wallet.email.invalid"));
                }
                break;

            case CRYPTO:
                if (request.getWalletId() == null) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.crypto.wallet.required"));
                }
                break;

            case MOBILE_PAYMENT:
                if (request.getWalletPhone() == null && request.getWalletId() == null) {
                    throw new IllegalArgumentException(
                            messageService.get("payment.validation.mobile.payment.required"));
                }
                break;

            case CASH:
            case INSTALLMENT:
                // No additional validation needed
                break;
        }
    }
}