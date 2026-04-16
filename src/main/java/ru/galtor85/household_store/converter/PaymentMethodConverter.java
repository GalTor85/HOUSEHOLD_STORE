package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.dto.request.payment.CreatePaymentMethodWithTypesRequest;
import ru.galtor85.household_store.dto.response.payment.PaymentMethodForUserDto;
import ru.galtor85.household_store.dto.response.payment.PaymentMethodWithUserTypesDto;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentProvider;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Converter for PaymentMethod entity to/from DTO.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMethodConverter {

    private static final String DEFAULT_MASKED_IDENTIFIER = "***";
    private static final String CREDIT_CARD_MASK_PREFIX = "**** **** **** ";
    private static final String BANK_ACCOUNT_MASK_PREFIX = "****";
    private static final String WALLET_MASK_SUFFIX = "***";
    private static final String EMAIL_MASK_PREFIX = "***@";
    private static final String AT_SIGN = "@";
    private static final int DEFAULT_SORT_ORDER = 0;
    private static final int MASK_VISIBLE_DIGITS = 4;
    private static final int MIN_LENGTH_FOR_MASK = 4;
    private static final int EMAIL_LOCAL_PART_MIN_LENGTH = 3;

    private final MessageService messageService;
    private final FinancialConfig financialConfig;

    /**
     * Converts PaymentMethod to DTO with user types.
     *
     * @param paymentMethod the entity
     * @param userTypes available user types
     * @param sortOrder sort order for display
     * @return PaymentMethodWithUserTypesDto
     */
    public PaymentMethodWithUserTypesDto toDtoWithUserTypes(PaymentMethod paymentMethod,
                                                            Set<UserType> userTypes,
                                                            Integer sortOrder) {
        if (paymentMethod == null) {
            return null;
        }

        String localizedStatus = paymentMethod.isActive()
                ? messageService.get("payment.method.active")
                : messageService.get("payment.method.inactive");

        return PaymentMethodWithUserTypesDto.builder()
                .id(paymentMethod.getId())
                .name(paymentMethod.getName())
                .methodType(paymentMethod.getMethodType())
                .maskedIdentifier(paymentMethod.getMaskedIdentifier())
                .provider(paymentMethod.getProvider())
                .active(paymentMethod.isActive())
                .localizedStatus(localizedStatus)
                .currency(paymentMethod.getCurrency())
                .processingFee(paymentMethod.getProcessingFee())
                .availableForUserTypes(userTypes)
                .sortOrder(sortOrder != null ? sortOrder : DEFAULT_SORT_ORDER)
                .createdBy(paymentMethod.getCreatedBy())
                .createdAt(paymentMethod.getCreatedAt())
                .updatedAt(paymentMethod.getUpdatedAt())
                .build();
    }

    /**
     * Converts PaymentMethod to DTO for users (safe view).
     *
     * @param paymentMethod the entity
     * @return PaymentMethodForUserDto
     */
    public PaymentMethodForUserDto toUserDto(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }

        return PaymentMethodForUserDto.builder()
                .id(paymentMethod.getId())
                .name(paymentMethod.getName())
                .methodType(paymentMethod.getMethodType())
                .maskedIdentifier(paymentMethod.getMaskedIdentifier())
                .provider(paymentMethod.getProvider())
                .currency(paymentMethod.getCurrency())
                .build();
    }

    /**
     * Converts CreatePaymentMethodWithTypesRequest to PaymentMethod entity.
     *
     * @param request creation request
     * @param createdBy ID of user creating
     * @return PaymentMethod entity
     */
    public PaymentMethod toEntityWithTypes(CreatePaymentMethodWithTypesRequest request, Long createdBy) {
        if (request == null) {
            return null;
        }

        PaymentMethod paymentMethod = new PaymentMethod() {
            @Override
            public String getMaskedIdentifier() {
                return buildMaskedIdentifier(request);
            }

            @Override
            public PaymentProvider getProvider() {
                return determineProvider(request);
            }
        };

        paymentMethod.setName(request.getName());
        paymentMethod.setMethodType(request.getMethodType());
        paymentMethod.setActive(request.getActive() != null ? request.getActive() : true);
        paymentMethod.setDefault(false);
        paymentMethod.setCurrency(getEffectiveCurrency(request));
        paymentMethod.setProcessingFee(getEffectiveFee(request));
        paymentMethod.setCreatedBy(createdBy);
        paymentMethod.setCreatedAt(LocalDateTime.now());
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        return paymentMethod;
    }

    private String buildMaskedIdentifier(CreatePaymentMethodWithTypesRequest request) {
        if (request.getMaskedIdentifier() != null && !request.getMaskedIdentifier().isEmpty()) {
            return request.getMaskedIdentifier();
        }
        if (request.isCreditCard() && request.getCardNumber() != null) {
            return maskCreditCard(request.getCardNumber());
        }
        if (request.isBankAccount() && request.getBankAccountNumber() != null) {
            return maskBankAccount(request.getBankAccountNumber());
        }
        if (request.isElectronic() && request.getWalletId() != null) {
            return maskWallet(request.getWalletId());
        }
        return DEFAULT_MASKED_IDENTIFIER;
    }

    private String maskCreditCard(String cardNumber) {
        String cleaned = cardNumber.replaceAll("\\s", "");
        if (cleaned.length() >= MIN_LENGTH_FOR_MASK) {
            return CREDIT_CARD_MASK_PREFIX + cleaned.substring(cleaned.length() - MASK_VISIBLE_DIGITS);
        }
        return DEFAULT_MASKED_IDENTIFIER;
    }

    private String maskBankAccount(String account) {
        String cleaned = account.replaceAll("\\s", "");
        if (cleaned.length() >= MIN_LENGTH_FOR_MASK) {
            return BANK_ACCOUNT_MASK_PREFIX + cleaned.substring(cleaned.length() - MASK_VISIBLE_DIGITS);
        }
        return DEFAULT_MASKED_IDENTIFIER;
    }

    private String maskWallet(String wallet) {
        if (wallet.contains(AT_SIGN)) {
            String[] parts = wallet.split(AT_SIGN);
            if (parts[0].length() > EMAIL_LOCAL_PART_MIN_LENGTH) {
                return parts[0].substring(0, EMAIL_LOCAL_PART_MIN_LENGTH) + EMAIL_MASK_PREFIX + parts[1];
            }
        }
        return wallet.length() > MIN_LENGTH_FOR_MASK
                ? WALLET_MASK_SUFFIX + wallet.substring(wallet.length() - MASK_VISIBLE_DIGITS)
                : DEFAULT_MASKED_IDENTIFIER;
    }

    private PaymentProvider determineProvider(CreatePaymentMethodWithTypesRequest request) {
        if (request.getProvider() != null) {
            return request.getProvider();
        }
        if (request.isCreditCard()) {
            return PaymentProvider.VISA_MASTERCARD;
        }
        if (request.isBankAccount()) {
            return PaymentProvider.SBERBANK;
        }
        if (request.isElectronic() || request.isMobilePayment()) {
            return PaymentProvider.YOOMONEY;
        }
        if (request.isCrypto()) {
            return PaymentProvider.CRYPTO;
        }
        return PaymentProvider.SBERBANK;
    }

    private String getEffectiveCurrency(CreatePaymentMethodWithTypesRequest request) {
        String currency = request.getCurrency();
        if (currency == null || currency.isBlank()) {
            return financialConfig.getDefaultCurrency();
        }
        return currency;
    }

    private BigDecimal getEffectiveFee(CreatePaymentMethodWithTypesRequest request) {
        BigDecimal fee = request.getProcessingFee();
        return fee != null ? fee : BigDecimal.ZERO;
    }
}