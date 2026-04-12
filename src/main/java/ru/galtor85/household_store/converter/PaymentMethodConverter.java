package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.dto.request.payment.CreatePaymentMethodRequest;
import ru.galtor85.household_store.dto.request.payment.CreatePaymentMethodWithTypesRequest;
import ru.galtor85.household_store.dto.response.payment.PaymentMethodForUserDto;
import ru.galtor85.household_store.dto.response.payment.PaymentMethodWithUserTypesDto;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentProvider;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converter for PaymentMethod entity to/from DTO.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMethodConverter {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    private static final String DEFAULT_MASKED_IDENTIFIER = "***";
    private static final String CREDIT_CARD_MASK_PREFIX = "**** **** **** ";
    private static final String BANK_ACCOUNT_MASK_PREFIX = "****";
    private static final String WALLET_MASK_SUFFIX = "***";
    private static final String EMAIL_MASK_PREFIX = "***@";
    private static final String NOT_AVAILABLE = "N/A";
    private static final int DEFAULT_SORT_ORDER = 0;
    private static final int MASK_VISIBLE_DIGITS = 4;
    private static final int MIN_LENGTH_FOR_MASK = 4;

    private static final String CREDIT_CARD_ICON = "💳 ";
    private static final String BANK_ACCOUNT_ICON = "🏦 ";
    private static final String ELECTRONIC_ICON = "📱 ";
    private static final String CASH_ICON = "💵 ";

    // =========================================================================
    // FIELDS
    // =========================================================================

    private final MessageService messageService;
    private final FinancialConfig financialConfig;

    // =========================================================================
    // ENTITY CREATION
    // =========================================================================

    /**
     * Converts CreatePaymentMethodRequest to PaymentMethod entity.
     *
     * @param request the creation request
     * @param createdBy ID of the user creating the payment method
     * @return PaymentMethod entity
     */
    public PaymentMethod toEntity(CreatePaymentMethodRequest request, Long createdBy) {
        if (request == null) {
            return null;
        }

        PaymentMethod paymentMethod = new PaymentMethod() {
            @Override
            public boolean validate() {
                return true;
            }

            @Override
            public String getMaskedIdentifier() {
                return DEFAULT_MASKED_IDENTIFIER;
            }

            @Override
            public PaymentProvider getProvider() {
                if (request.getProvider() != null) {
                    return request.getProvider();
                }
                if (request.isCreditCard()) {
                    return PaymentProvider.VISA_MASTERCARD;
                }
                if (request.isBankAccount()) {
                    return PaymentProvider.SBERBANK;
                }
                if (request.isElectronic()) {
                    return PaymentProvider.YOOMONEY;
                }
                return PaymentProvider.SBERBANK;
            }
        };

        paymentMethod.setName(request.getName());
        paymentMethod.setMethodType(request.getMethodType());
        paymentMethod.setActive(request.getActive() != null ? request.getActive() : true);

        String currency = request.getCurrency();
        if (currency == null || currency.isBlank()) {
            currency = financialConfig.getDefaultCurrency();
        }
        paymentMethod.setCurrency(currency);

        BigDecimal fee = request.getProcessingFee();
        if (fee == null) {
            fee = BigDecimal.ZERO;
        }
        paymentMethod.setProcessingFee(fee);

        paymentMethod.setCreatedBy(createdBy);
        paymentMethod.setCreatedAt(LocalDateTime.now());
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        return paymentMethod;
    }

    // =========================================================================
    // ENTITY UPDATE
    // =========================================================================

    /**
     * Updates existing PaymentMethod entity from request.
     *
     * @param paymentMethod the existing entity
     * @param request the update request
     */
    public void updateEntity(PaymentMethod paymentMethod, CreatePaymentMethodRequest request) {
        if (paymentMethod == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            paymentMethod.setName(request.getName());
        }
        if (request.getMethodType() != null) {
            paymentMethod.setMethodType(request.getMethodType());
        }
        if (request.getActive() != null) {
            paymentMethod.setActive(request.getActive());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            paymentMethod.setCurrency(request.getCurrency());
        }
        if (request.getProcessingFee() != null) {
            paymentMethod.setProcessingFee(request.getProcessingFee());
        }

        paymentMethod.setUpdatedAt(LocalDateTime.now());
    }

    // =========================================================================
    // DTO CONVERSION (MANAGER VIEW)
    // =========================================================================

    /**
     * Converts PaymentMethod to DTO with user types.
     *
     * @param paymentMethod the entity
     * @param userTypes available user types for this payment method
     * @param sortOrder sort order for display
     * @return PaymentMethodWithUserTypesDto
     */
    public PaymentMethodWithUserTypesDto toDtoWithUserTypes(PaymentMethod paymentMethod,
                                                            Set<UserType> userTypes,
                                                            Integer sortOrder) {
        if (paymentMethod == null) {
            return null;
        }

        String localizedStatus = paymentMethod.isActive() ?
                messageService.get("payment.method.active") :
                messageService.get("payment.method.inactive");

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
     * Converts PaymentMethod to simple DTO (manager view).
     *
     * @param paymentMethod the entity
     * @return simple PaymentMethodWithUserTypesDto
     */
    public PaymentMethodWithUserTypesDto toSimpleDto(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }

        String localizedStatus = paymentMethod.isActive() ?
                messageService.get("payment.method.active") :
                messageService.get("payment.method.inactive");

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
                .createdBy(paymentMethod.getCreatedBy())
                .createdAt(paymentMethod.getCreatedAt())
                .updatedAt(paymentMethod.getUpdatedAt())
                .build();
    }

    // =========================================================================
    // DTO CONVERSION (USER VIEW)
    // =========================================================================

    /**
     * Converts PaymentMethod to DTO for users (safe view without sensitive data).
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
     * Converts PaymentMethod to DTO for users with sort order.
     *
     * @param paymentMethod the entity
     * @param sortOrder sort order for display
     * @return PaymentMethodForUserDto with sort order
     */
    public PaymentMethodForUserDto toUserDtoWithSortOrder(PaymentMethod paymentMethod, Integer sortOrder) {
        PaymentMethodForUserDto dto = toUserDto(paymentMethod);
        if (dto != null) {
            dto.setSortOrder(sortOrder != null ? sortOrder : DEFAULT_SORT_ORDER);
        }
        return dto;
    }

    // =========================================================================
    // LIST CONVERSIONS
    // =========================================================================

    /**
     * Converts list of PaymentMethod entities to list of DTOs (manager view).
     *
     * @param paymentMethods list of entities
     * @return list of DTOs
     */
    public List<PaymentMethodWithUserTypesDto> toDtoList(List<PaymentMethod> paymentMethods) {
        if (paymentMethods == null) {
            return null;
        }
        return paymentMethods.stream()
                .map(this::toSimpleDto)
                .collect(Collectors.toList());
    }

    /**
     * Converts list of PaymentMethod entities to list of user DTOs.
     *
     * @param paymentMethods list of entities
     * @return list of user DTOs
     */
    public List<PaymentMethodForUserDto> toUserDtoList(List<PaymentMethod> paymentMethods) {
        if (paymentMethods == null) {
            return null;
        }
        return paymentMethods.stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Creates a copy of PaymentMethod entity.
     *
     * @param source source entity
     * @return copy of the entity
     */
    public PaymentMethod copy(PaymentMethod source) {
        if (source == null) {
            return null;
        }

        PaymentMethod copy = new PaymentMethod() {
            @Override
            public boolean validate() { return source.validate(); }
            @Override
            public String getMaskedIdentifier() { return source.getMaskedIdentifier(); }
            @Override
            public PaymentProvider getProvider() { return source.getProvider(); }
        };

        copy.setName(source.getName());
        copy.setMethodType(source.getMethodType());
        copy.setActive(source.isActive());
        copy.setDefault(source.isDefault());
        copy.setCurrency(source.getCurrency());
        copy.setProcessingFee(source.getProcessingFee());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());

        return copy;
    }

    /**
     * Validates that the payment method is active and available for use.
     *
     * @param paymentMethod the payment method
     * @return true if active
     */
    public boolean isAvailable(PaymentMethod paymentMethod) {
        return paymentMethod != null && paymentMethod.isActive();
    }

    /**
     * Gets formatted masked identifier for display.
     *
     * @param paymentMethod the payment method
     * @return formatted masked identifier
     */
    public String getFormattedIdentifier(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return NOT_AVAILABLE;
        }
        String masked = paymentMethod.getMaskedIdentifier();

        return switch (paymentMethod.getMethodType()) {
            case CREDIT_CARD -> CREDIT_CARD_ICON + masked;
            case BANK_ACCOUNT -> BANK_ACCOUNT_ICON + masked;
            case ELECTRONIC -> ELECTRONIC_ICON + masked;
            case CASH -> CASH_ICON + messageService.get("payment.method.cash");
            default -> masked;
        };
    }

    /**
     * Converts CreatePaymentMethodWithTypesRequest to PaymentMethod entity.
     *
     * @param request the creation request with user types
     * @param createdBy ID of the user creating the payment method
     * @return PaymentMethod entity
     */
    public PaymentMethod toEntityWithTypes(CreatePaymentMethodWithTypesRequest request, Long createdBy) {
        if (request == null) {
            return null;
        }

        PaymentMethod paymentMethod = new PaymentMethod() {
            @Override
            public boolean validate() {
                return true;
            }

            @Override
            public String getMaskedIdentifier() {
                if (request.getMaskedIdentifier() != null && !request.getMaskedIdentifier().isEmpty()) {
                    return request.getMaskedIdentifier();
                }
                if (request.isCreditCard() && request.getCardNumber() != null) {
                    String cardNumber = request.getCardNumber().replaceAll("\\s", "");
                    if (cardNumber.length() >= MIN_LENGTH_FOR_MASK) {
                        return CREDIT_CARD_MASK_PREFIX + cardNumber.substring(cardNumber.length() - MASK_VISIBLE_DIGITS);
                    }
                }
                if (request.isBankAccount() && request.getBankAccountNumber() != null) {
                    String account = request.getBankAccountNumber().replaceAll("\\s", "");
                    if (account.length() >= MIN_LENGTH_FOR_MASK) {
                        return BANK_ACCOUNT_MASK_PREFIX + account.substring(account.length() - MASK_VISIBLE_DIGITS);
                    }
                }
                if (request.isElectronic() && request.getWalletId() != null) {
                    String wallet = request.getWalletId();
                    if (wallet.contains("@")) {
                        String[] parts = wallet.split("@");
                        if (parts[0].length() > 3) {
                            return parts[0].substring(0, 3) + EMAIL_MASK_PREFIX + parts[1];
                        }
                    }
                    return wallet.length() > MIN_LENGTH_FOR_MASK ?
                            WALLET_MASK_SUFFIX + wallet.substring(wallet.length() - MASK_VISIBLE_DIGITS) :
                            DEFAULT_MASKED_IDENTIFIER;
                }
                return DEFAULT_MASKED_IDENTIFIER;
            }

            @Override
            public PaymentProvider getProvider() {
                if (request.getProvider() != null) {
                    return request.getProvider();
                }
                if (request.isCreditCard()) {
                    return PaymentProvider.VISA_MASTERCARD;
                }
                if (request.isBankAccount()) {
                    return PaymentProvider.SBERBANK;
                }
                if (request.isElectronic()) {
                    return PaymentProvider.YOOMONEY;
                }
                if (request.isCrypto()) {
                    return PaymentProvider.CRYPTO;
                }
                if (request.isMobilePayment()) {
                    return PaymentProvider.YOOMONEY;
                }
                return PaymentProvider.SBERBANK;
            }
        };

        paymentMethod.setName(request.getName());
        paymentMethod.setMethodType(request.getMethodType());
        paymentMethod.setActive(request.getActive() != null ? request.getActive() : true);
        paymentMethod.setDefault(false);

        String currency = request.getCurrency();
        if (currency == null || currency.isBlank()) {
            currency = financialConfig.getDefaultCurrency();
        }
        paymentMethod.setCurrency(currency);

        BigDecimal fee = request.getProcessingFee();
        if (fee == null) {
            fee = BigDecimal.ZERO;
        }
        paymentMethod.setProcessingFee(fee);

        paymentMethod.setCreatedBy(createdBy);
        paymentMethod.setCreatedAt(LocalDateTime.now());
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        return paymentMethod;
    }

    /**
     * Updates existing PaymentMethod entity from CreatePaymentMethodWithTypesRequest.
     *
     * @param paymentMethod the existing entity
     * @param request the update request
     */
    public void updateEntityWithTypes(PaymentMethod paymentMethod, CreatePaymentMethodWithTypesRequest request) {
        if (paymentMethod == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            paymentMethod.setName(request.getName());
        }
        if (request.getMethodType() != null) {
            paymentMethod.setMethodType(request.getMethodType());
        }
        if (request.getActive() != null) {
            paymentMethod.setActive(request.getActive());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            paymentMethod.setCurrency(request.getCurrency());
        }
        if (request.getProcessingFee() != null) {
            paymentMethod.setProcessingFee(request.getProcessingFee());
        }

        paymentMethod.setUpdatedAt(LocalDateTime.now());
    }
}