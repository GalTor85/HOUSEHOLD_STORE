package ru.galtor85.household_store.validator.finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cash.InvoiceNotFoundException;
import ru.galtor85.household_store.advice.exception.invoice.InvoiceAlreadyCancelledException;
import ru.galtor85.household_store.advice.exception.invoice.InvoiceAlreadyPaidException;
import ru.galtor85.household_store.advice.exception.invoice.InvoiceAlreadyRefundedException;
import ru.galtor85.household_store.dto.request.finance.InvoiceCreateRequest;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Validator for invoice operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceValidator {

    private final InvoiceRepository invoiceRepository;
    private final MessageService messageService;
    private final CashTransactionRepository cashTransactionRepository;
    private final LogMessageService logMsg;

    /**
     * Validates invoice exists by ID.
     *
     * @param invoiceId invoice ID
     * @return invoice entity
     * @throws InvoiceNotFoundException if not found
     */
    public Invoice validateInvoiceExists(Long invoiceId) {
        return invoiceRepository.findByIdWithTransactions(invoiceId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("invoice.not.found", invoiceId));
                    return new InvoiceNotFoundException(invoiceId);
                });
    }

    /**
     * Validates invoice number is unique.
     *
     * @param invoiceNumber invoice number
     * @throws IllegalArgumentException if already exists
     */
    public void validateInvoiceNumberUnique(String invoiceNumber) {
        if (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            log.error(logMsg.get("invoice.number.exists", invoiceNumber));
            throw new IllegalArgumentException(
                    messageService.get("invoice.number.exists", invoiceNumber)
            );
        }
    }

    /**
     * Validates invoice creation request.
     *
     * @param request creation request
     * @throws IllegalArgumentException if invalid
     */
    public void validateCreateRequest(InvoiceCreateRequest request) {
        if (!request.hasOrder()) {
            log.error(logMsg.get("invoice.validation.order.required"));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.order.required")
            );
        }
        validateAmount(request.getAmount());
        validatePaymentMethod(request.getPaymentMethod());
        validateDueDate(request.getDueDate());
    }

    /**
     * Validates invoice amount is positive.
     *
     * @param amount invoice amount
     * @throws IllegalArgumentException if invalid
     */
    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(logMsg.get("invoice.validation.amount.invalid", amount));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.amount.invalid", amount)
            );
        }
    }

    /**
     * Validates payment method is specified.
     *
     * @param paymentMethod payment method
     * @throws IllegalArgumentException if null
     */
    public void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            log.error(logMsg.get("invoice.validation.payment.method.empty"));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.payment.method.empty")
            );
        }
    }

    /**
     * Validates due date is not in the past.
     *
     * @param dueDate due date
     * @throws IllegalArgumentException if in the past
     */
    public void validateDueDate(LocalDateTime dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            log.error(logMsg.get("invoice.validation.due.date.past", dueDate));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.due.date.past", dueDate)
            );
        }
    }

    /**
     * Validates invoice is payable.
     *
     * @param invoice invoice entity
     * @throws InvoiceAlreadyPaidException if already paid
     * @throws InvoiceAlreadyCancelledException if cancelled
     * @throws InvoiceAlreadyRefundedException if refunded
     * @throws IllegalStateException if not payable
     */
    public void validateInvoicePayable(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvoiceAlreadyPaidException(invoice.getId(), invoice.getInvoiceNumber());
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvoiceAlreadyCancelledException(invoice.getId(), invoice.getInvoiceNumber());
        }
        if (invoice.getStatus() == InvoiceStatus.REFUNDED) {
            throw new InvoiceAlreadyRefundedException(invoice.getId(), invoice.getInvoiceNumber());
        }
        if (!invoice.isPayable()) {
            log.error(logMsg.get("invoice.not.payable", invoice.getId(), invoice.getStatus()));
            throw new IllegalStateException(
                    messageService.get("invoice.not.payable",
                            invoice.getId(), invoice.getStatus().getLocalizedName(messageService))
            );
        }
    }

    /**
     * Validates partial payment amount.
     *
     * @param invoice invoice entity
     * @param paidAmount payment amount
     * @throws IllegalArgumentException if invalid or exceeds remaining
     */
    public void validatePartialPaymentAmount(Invoice invoice, BigDecimal paidAmount) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(logMsg.get("invoice.validation.partial.amount.invalid", paidAmount));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.partial.amount.invalid", paidAmount)
            );
        }
        BigDecimal totalPaid = invoice.getTotalPaidAmount();
        BigDecimal remaining = invoice.getAmount().subtract(totalPaid);
        if (paidAmount.compareTo(remaining) > 0) {
            log.error(logMsg.get("invoice.partial.amount.exceeds", paidAmount, remaining));
            throw new IllegalArgumentException(
                    messageService.get("invoice.partial.amount.exceeds", paidAmount, remaining)
            );
        }
    }

    /**
     * Validates invoice exists and is payable.
     *
     * @param invoiceId invoice ID
     * @return invoice entity
     * @throws InvoiceNotFoundException if not found
     */
    public Invoice validateInvoiceForPayment(Long invoiceId) {
        Invoice invoice = invoiceRepository.findByIdWithTransactions(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        validateInvoicePayable(invoice);
        return invoice;
    }

    /**
     * Validates payment amount does not exceed remaining.
     *
     * @param invoice invoice entity
     * @param amount payment amount
     * @throws IllegalArgumentException if invalid or exceeds remaining
     */
    public void validatePaymentAmount(Invoice invoice, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(logMsg.get("invoice.validation.payment.amount.invalid", amount));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.payment.amount.invalid", amount)
            );
        }
        BigDecimal remaining = invoice.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            log.error(logMsg.get("invoice.validation.payment.amount.exceeds", amount, remaining));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.payment.amount.exceeds", amount, remaining)
            );
        }
    }

    /**
     * Validates no overpayment.
     *
     * @param invoice invoice entity
     * @param amount payment amount
     * @throws IllegalArgumentException if overpayment
     */
    public void validateNoOverpayment(Invoice invoice, BigDecimal amount) {
        BigDecimal remaining = invoice.getAmount().subtract(
                cashTransactionRepository.findByInvoiceId(invoice.getId()).stream()
                        .filter(t -> t.getTransactionType() == ru.galtor85.household_store.entity.finance.TransactionType.INCOME)
                        .map(ru.galtor85.household_store.entity.finance.CashTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        if (amount.compareTo(remaining) > 0) {
            log.error(logMsg.get("invoice.validation.overpayment", amount, remaining));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.overpayment", amount, remaining)
            );
        }
    }

    /**
     * Validates invoice can be cancelled.
     *
     * @param invoice invoice entity
     * @throws IllegalStateException if not cancellable
     */
    public void validateInvoiceCancellable(Invoice invoice) {
        if (!invoice.isCancellable()) {
            log.error(logMsg.get("invoice.not.cancellable", invoice.getId(), invoice.getStatus()));
            throw new IllegalStateException(
                    messageService.get("invoice.not.cancellable",
                            invoice.getId(), invoice.getStatus().getLocalizedName(messageService))
            );
        }
    }
}