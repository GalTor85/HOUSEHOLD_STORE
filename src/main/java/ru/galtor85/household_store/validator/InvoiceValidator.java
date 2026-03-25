package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.InvoiceNotFoundException;
import ru.galtor85.household_store.dto.InvoiceCreateRequest;
import ru.galtor85.household_store.entity.Invoice;
import ru.galtor85.household_store.entity.InvoiceStatus;
import ru.galtor85.household_store.entity.PaymentMethod;
import ru.galtor85.household_store.repository.InvoiceRepository;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceValidator {

    private final InvoiceRepository invoiceRepository;
    private final MessageService messageService;

    // =========================================================================
    // ВАЛИДАЦИЯ СУЩЕСТВОВАНИЯ
    // =========================================================================

    /**
     * Проверяет существование счета
     */
    public Invoice validateInvoiceExists(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    log.error(messageService.get("invoice.not.found", invoiceId));
                    return new InvoiceNotFoundException(invoiceId);
                });
    }

    /**
     * Проверяет существование счета по номеру
     */
    public Invoice validateInvoiceExists(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> {
                    log.error(messageService.get("invoice.not.found.by.number", invoiceNumber));
                    return new InvoiceNotFoundException(invoiceNumber);
                });
    }

    /**
     * Проверяет уникальность номера счета
     */
    public void validateInvoiceNumberUnique(String invoiceNumber) {
        if (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            log.error(messageService.get("invoice.number.exists", invoiceNumber));
            throw new IllegalArgumentException(
                    messageService.get("invoice.number.exists", invoiceNumber)
            );
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ СОЗДАНИЯ СЧЕТА
    // =========================================================================

    /**
     * Проверяет запрос на создание счета
     */
    public void validateCreateRequest(InvoiceCreateRequest request) {
        // Проверяем, что указан один из заказов
        if (!request.hasOrder()) {
            log.error(messageService.get("invoice.validation.order.required"));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.order.required")
            );
        }

        // Проверяем сумму
        validateAmount(request.getAmount());

        // Проверяем способ оплаты
        validatePaymentMethod(request.getPaymentMethod());

        // Проверяем дату
        validateDueDate(request.getDueDate());
    }

    /**
     * Проверяет сумму счета
     */
    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(messageService.get("invoice.validation.amount.invalid", amount));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.amount.invalid", amount)
            );
        }
    }

    /**
     * Проверяет способ оплаты
     */
    public void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            log.error(messageService.get("invoice.validation.payment.method.empty"));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.payment.method.empty")
            );
        }
    }

    /**
     * Проверяет дату оплаты
     */
    public void validateDueDate(LocalDateTime dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            log.error(messageService.get("invoice.validation.due.date.past", dueDate));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.due.date.past", dueDate)
            );
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ОПЛАТЫ
    // =========================================================================

    /**
     * Проверяет, можно ли оплатить счет
     */
    public void validateInvoicePayable(Invoice invoice) {
        if (!invoice.isPayable()) {
            log.error(messageService.get("invoice.not.payable",
                    invoice.getId(), invoice.getStatus()));
            throw new IllegalStateException(
                    messageService.get("invoice.not.payable",
                            invoice.getId(), invoice.getStatus().getLocalizedName(messageService))
            );
        }
    }

    /**
     * Проверяет сумму частичной оплаты
     */
    public void validatePartialPaymentAmount(Invoice invoice, BigDecimal paidAmount) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(messageService.get("invoice.validation.partial.amount.invalid", paidAmount));
            throw new IllegalArgumentException(
                    messageService.get("invoice.validation.partial.amount.invalid", paidAmount)
            );
        }

        BigDecimal remaining = invoice.getAmount();
        if (invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
            // TODO: вычислить уже оплаченную сумму
            remaining = invoice.getAmount();
        }

        if (paidAmount.compareTo(remaining) > 0) {
            log.error(messageService.get("invoice.partial.amount.exceeds",
                    paidAmount, remaining));
            throw new IllegalArgumentException(
                    messageService.get("invoice.partial.amount.exceeds",
                            paidAmount, remaining)
            );
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ОТМЕНЫ
    // =========================================================================

    /**
     * Проверяет, можно ли отменить счет
     */
    public void validateInvoiceCancellable(Invoice invoice) {
        if (!invoice.isCancellable()) {
            log.error(messageService.get("invoice.not.cancellable",
                    invoice.getId(), invoice.getStatus()));
            throw new IllegalStateException(
                    messageService.get("invoice.not.cancellable",
                            invoice.getId(), invoice.getStatus().getLocalizedName(messageService))
            );
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ДЛЯ ЗАКАЗОВ
    // =========================================================================

    /**
     * Проверяет, что у заказа на закупку нет неоплаченных счетов
     */
    public void validateNoUnpaidInvoicesForPurchaseOrder(Long purchaseOrderId) {
        if (invoiceRepository.hasUnpaidInvoicesForPurchaseOrder(purchaseOrderId)) {
            log.error(messageService.get("invoice.validation.unpaid.purchase.order",
                    purchaseOrderId));
            throw new IllegalStateException(
                    messageService.get("invoice.validation.unpaid.purchase.order",
                            purchaseOrderId)
            );
        }
    }

    /**
     * Проверяет, что у заказа на продажу нет неоплаченных счетов
     */
    public void validateNoUnpaidInvoicesForSalesOrder(Long salesOrderId) {
        if (invoiceRepository.hasUnpaidInvoicesForSalesOrder(salesOrderId)) {
            log.error(messageService.get("invoice.validation.unpaid.sales.order",
                    salesOrderId));
            throw new IllegalStateException(
                    messageService.get("invoice.validation.unpaid.sales.order",
                            salesOrderId)
            );
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ДЛЯ КАССОВЫХ ОПЕРАЦИЙ
    // =========================================================================

    /**
     * Проверяет, что счет существует и не оплачен
     */
    public void validateInvoiceForCashTransaction(Long invoiceId) {
        Invoice invoice = validateInvoiceExists(invoiceId);
        validateInvoicePayable(invoice);
    }

    /**
     * Проверяет, что сумма операции не превышает остаток по счету
     */
    public void validateCashTransactionAmount(Invoice invoice, BigDecimal amount) {
        BigDecimal remaining = invoice.getAmount();
        if (invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
            // TODO: вычислить уже оплаченную сумму
            remaining = invoice.getAmount();
        }

        if (amount.compareTo(remaining) > 0) {
            log.error(messageService.get("invoice.cash.transaction.amount.exceeds",
                    amount, remaining));
            throw new IllegalArgumentException(
                    messageService.get("invoice.cash.transaction.amount.exceeds",
                            amount, remaining)
            );
        }
    }
}