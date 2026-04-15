package ru.galtor85.household_store.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentMethodType;
import ru.galtor85.household_store.entity.payment.PaymentMethodUserType;
import ru.galtor85.household_store.entity.payment.PaymentProvider;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.repository.payment.PaymentMethodRepository;
import ru.galtor85.household_store.repository.payment.PaymentMethodUserTypeRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Initializes default payment methods on application startup.
 *
 * <p>This component runs after application context initialization and creates
 * default payment methods only if the database is empty. It ensures that basic
 * payment methods are available for customers.</p>
 *
 * <p>Initialization can be disabled by setting
 * {@code app.payment-methods.initialize=false}.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.payment-methods.initialize", havingValue = "true", matchIfMissing = true)
public class PaymentMethodInitializer {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    private static final BigDecimal DEFAULT_PROCESSING_FEE = BigDecimal.ZERO;
    private static final String DEFAULT_MASKED_IDENTIFIER = "***";

    private static final String CASH_METHOD_NAME = "Cash";
    private static final String CREDIT_CARD_METHOD_NAME = "Credit Card";
    private static final String BANK_TRANSFER_METHOD_NAME = "Bank Transfer";

    private static final int CASH_SORT_ORDER = 1;
    private static final int CREDIT_CARD_SORT_ORDER = 2;
    private static final int BANK_TRANSFER_SORT_ORDER = 3;
    private static final int SORT_ORDER_INCREMENT = 10;

    // =========================================================================
    // FIELDS
    // =========================================================================

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodUserTypeRepository paymentMethodUserTypeRepository;
    private final LogMessageService logMsg;
    private final FinancialConfig financialConfig;

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    /**
     * Initializes payment methods after application is ready.
     * Skips initialization if payment methods already exist.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initPaymentMethods() {
        if (paymentMethodRepository.count() > 0) {
            log.debug(logMsg.get("payment.method.init.skip"));
            return;
        }

        log.info(logMsg.get("payment.method.init.start"));

        createCashPaymentMethod();
        createCreditCardPaymentMethod();
        createBankTransferPaymentMethod();

        log.info(logMsg.get("payment.method.init.complete"));
    }

    // =========================================================================
    // PAYMENT METHOD CREATION
    // =========================================================================

    /**
     * Creates cash payment method.
     */
    private void createCashPaymentMethod() {
        PaymentMethod cash = createPaymentMethod(
                CASH_METHOD_NAME,
                PaymentMethodType.CASH,
                PaymentProvider.CASH_REGISTER,
                true
        );

        assignToAllUserTypes(cash.getId(), CASH_SORT_ORDER);
        log.info(logMsg.get("payment.method.created", CASH_METHOD_NAME));
    }

    /**
     * Creates credit card payment method.
     */
    private void createCreditCardPaymentMethod() {
        PaymentMethod card = createPaymentMethod(
                CREDIT_CARD_METHOD_NAME,
                PaymentMethodType.CREDIT_CARD,
                PaymentProvider.VISA_MASTERCARD,
                false
        );

        assignToAllUserTypes(card.getId(), CREDIT_CARD_SORT_ORDER);
        log.info(logMsg.get("payment.method.created", CREDIT_CARD_METHOD_NAME));
    }

    /**
     * Creates bank transfer payment method.
     */
    private void createBankTransferPaymentMethod() {
        PaymentMethod bankTransfer = createPaymentMethod(
                BANK_TRANSFER_METHOD_NAME,
                PaymentMethodType.BANK_ACCOUNT,
                PaymentProvider.SBERBANK,
                false
        );

        assignToAllUserTypes(bankTransfer.getId(), BANK_TRANSFER_SORT_ORDER);
        log.info(logMsg.get("payment.method.created", BANK_TRANSFER_METHOD_NAME));
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Gets default currency from FinancialConfig.
     *
     * @return default currency code
     */
    private String getDefaultCurrency() {
        return financialConfig.getDefaultCurrency();
    }

    /**
     * Creates and saves a payment method.
     *
     * @param name display name
     * @param type payment method type
     * @param provider payment provider
     * @param isDefault whether this is default method
     * @return saved payment method
     */
    private PaymentMethod createPaymentMethod(String name, PaymentMethodType type,
                                              PaymentProvider provider, boolean isDefault) {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setName(name);
        paymentMethod.setMethodType(type);
        paymentMethod.setProvider(provider);
        paymentMethod.setActive(true);
        paymentMethod.setDefault(isDefault);
        paymentMethod.setCurrency(getDefaultCurrency());
        paymentMethod.setProcessingFee(DEFAULT_PROCESSING_FEE);
        paymentMethod.setMaskedIdentifier(DEFAULT_MASKED_IDENTIFIER);

        return paymentMethodRepository.save(paymentMethod);
    }

    /**
     * Assigns payment method to all user types.
     *
     * @param paymentMethodId payment method ID
     * @param sortOrder display sort order
     */
    private void assignToAllUserTypes(Long paymentMethodId, int sortOrder) {
        UserType[] allUserTypes = UserType.values();
        int order = sortOrder;

        for (UserType userType : allUserTypes) {
            assignToUserType(paymentMethodId, userType, order);
            order += SORT_ORDER_INCREMENT;
        }
    }

    /**
     * Assigns payment method to a specific user type.
     *
     * @param paymentMethodId payment method ID
     * @param userType user type
     * @param sortOrder display sort order
     */
    private void assignToUserType(Long paymentMethodId, UserType userType, int sortOrder) {
        PaymentMethodUserType assignment = PaymentMethodUserType.builder()
                .paymentMethodId(paymentMethodId)
                .userType(userType)
                .active(true)
                .sortOrder(sortOrder)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentMethodUserTypeRepository.save(assignment);
    }
}