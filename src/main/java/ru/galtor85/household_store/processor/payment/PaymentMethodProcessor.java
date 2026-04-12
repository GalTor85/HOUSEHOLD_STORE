package ru.galtor85.household_store.processor.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.repository.payment.PaymentMethodRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

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
    private final MessageService messageService;

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
        log.info(messageService.get("payment.processor.create.method.start",
                paymentMethod.getName()));

        paymentMethod.setCreatedAt(LocalDateTime.now());
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(messageService.get("payment.processor.create.method.success",
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
        log.info(messageService.get("payment.processor.update.method.start",
                paymentMethod.getId()));

        paymentMethod.setUpdatedAt(LocalDateTime.now());
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(messageService.get("payment.processor.update.method.success",
                saved.getId()));

        return saved;
    }

    /**
     * Updates payment method name.
     *
     * @param paymentMethod the payment method entity
     * @param newName the new name
     * @return updated PaymentMethod entity
     */
    @Transactional
    public PaymentMethod updatePaymentMethodName(PaymentMethod paymentMethod, String newName) {
        log.info(messageService.get("payment.processor.update.method.name.start",
                paymentMethod.getId(), newName));

        paymentMethod.setName(newName);
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(messageService.get("payment.processor.update.method.name.success",
                saved.getId()));

        return saved;
    }

    /**
     * Updates payment method active status.
     *
     * @param paymentMethod the payment method entity
     * @param active the new active status
     * @return updated PaymentMethod entity
     */
    @Transactional
    public PaymentMethod updatePaymentMethodActiveStatus(PaymentMethod paymentMethod, boolean active) {
        log.info(messageService.get("payment.processor.update.method.status.start",
                paymentMethod.getId(), active));

        paymentMethod.setActive(active);
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(messageService.get("payment.processor.update.method.status.success",
                saved.getId(), active));

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
        log.info(messageService.get("payment.processor.activate.method.start",
                paymentMethod.getId()));

        paymentMethod.setActive(true);
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(messageService.get("payment.processor.activate.method.success",
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
        log.info(messageService.get("payment.processor.deactivate.method.start",
                paymentMethod.getId()));

        paymentMethod.setActive(false);
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info(messageService.get("payment.processor.deactivate.method.success",
                saved.getId()));

        return saved;
    }

    // =========================================================================
    // DELETE OPERATIONS
    // =========================================================================

    /**
     * Deletes a payment method.
     *
     * @param paymentMethod the payment method entity
     */
    @Transactional
    public void deletePaymentMethod(PaymentMethod paymentMethod) {
        log.info(messageService.get("payment.processor.delete.method.start",
                paymentMethod.getId()));

        paymentMethodRepository.delete(paymentMethod);

        log.info(messageService.get("payment.processor.delete.method.success",
                paymentMethod.getId()));
    }

    /**
     * Deletes a payment method by ID.
     *
     * @param methodId the payment method ID
     */
    @Transactional
    public void deletePaymentMethodById(Long methodId) {
        log.info(messageService.get("payment.processor.delete.method.start", methodId));

        paymentMethodRepository.deleteById(methodId);

        log.info(messageService.get("payment.processor.delete.method.success", methodId));
    }

    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================

    /**
     * Finds payment method by ID.
     *
     * @param methodId the payment method ID
     * @return PaymentMethod entity
     */
    @Transactional(readOnly = true)
    public PaymentMethod findPaymentMethodById(Long methodId) {
        log.debug(messageService.get("payment.processor.find.method.start", methodId));
        return paymentMethodRepository.findById(methodId).orElse(null);
    }

    /**
     * Checks if payment method exists.
     *
     * @param methodId the payment method ID
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean paymentMethodExists(Long methodId) {
        return paymentMethodRepository.existsById(methodId);
    }

    /**
     * Counts total payment methods.
     *
     * @return total count
     */
    @Transactional(readOnly = true)
    public long countPaymentMethods() {
        return paymentMethodRepository.count();
    }
}