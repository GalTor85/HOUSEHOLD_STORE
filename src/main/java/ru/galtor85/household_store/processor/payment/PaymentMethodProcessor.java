package ru.galtor85.household_store.processor.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.repository.payment.PaymentMethodRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.time.LocalDateTime;

/**
 * Processor for payment method operations.
 * Handles transactional database operations for payment methods.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMethodProcessor {

    private final PaymentMethodRepository paymentMethodRepository;
    private final LogMessageService logMsg;

    // =========================================================================
    // CREATE OPERATIONS
    // =========================================================================

    /**
     * Creates a new payment method.
     *
     * @param paymentMethod the payment method entity
     * @return created PaymentMethod entity
     */
    @Transactional
    public PaymentMethod createPaymentMethod(PaymentMethod paymentMethod) {
        log.info(logMsg.get("payment.processor.create.method.start",
                paymentMethod.getName()));

        paymentMethod.setCreatedAt(LocalDateTime.now());
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(logMsg.get("payment.processor.create.method.success",
                saved.getId(), saved.getName()));

        return saved;
    }

    // =========================================================================
    // UPDATE OPERATIONS
    // =========================================================================

    /**
     * Updates an existing payment method.
     *
     * @param paymentMethod the payment method entity with updated values
     * @return updated PaymentMethod entity
     */
    @Transactional
    public PaymentMethod updatePaymentMethod(PaymentMethod paymentMethod) {
        log.info(logMsg.get("payment.processor.update.method.start",
                paymentMethod.getId()));

        paymentMethod.setUpdatedAt(LocalDateTime.now());
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(logMsg.get("payment.processor.update.method.success",
                saved.getId()));

        return saved;
    }

    // =========================================================================
    // ACTIVATION OPERATIONS
    // =========================================================================

    /**
     * Activates a payment method.
     *
     * @param paymentMethod the payment method entity
     * @return activated PaymentMethod entity
     */
    @Transactional
    public PaymentMethod activatePaymentMethod(PaymentMethod paymentMethod) {
        log.info(logMsg.get("payment.processor.activate.method.start",
                paymentMethod.getId()));

        paymentMethod.setActive(true);
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(logMsg.get("payment.processor.activate.method.success",
                saved.getId()));

        return saved;
    }

    /**
     * Deactivates a payment method.
     *
     * @param paymentMethod the payment method entity
     * @return deactivated PaymentMethod entity
     */
    @Transactional
    public PaymentMethod deactivatePaymentMethod(PaymentMethod paymentMethod) {
        log.info(logMsg.get("payment.processor.deactivate.method.start",
                paymentMethod.getId()));

        paymentMethod.setActive(false);
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(logMsg.get("payment.processor.deactivate.method.success",
                saved.getId()));

        return saved;
    }
}