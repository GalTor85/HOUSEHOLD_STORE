package ru.galtor85.household_store.processor.invoice;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cash.InsufficientCashException;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.processor.cash.CashTransactionProcessor;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.cash.CashRegisterValidator;
import ru.galtor85.household_store.validator.finance.InvoiceValidator;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoicePaymentProcessor {

    private final InvoiceValidator invoiceValidator;
    private final CashRegisterValidator cashRegisterValidator;
    private final CashTransactionProcessor cashTransactionProcessor;
    private final InvoiceRepository invoiceRepository;
    private final MessageService messageService;

    @Transactional
    public InvoicePaymentResult processPayment(Long invoiceId, BigDecimal amount,
                                               Long cashRegisterId, Long cashierId,
                                               boolean isPartial) {

        log.info(messageService.get("invoice.payment.processor.start", invoiceId, amount, cashRegisterId),
                invoiceId, amount, cashRegisterId);

        // 1. Валидация счёта
        Invoice invoice = invoiceValidator.validateInvoiceForPayment(invoiceId);

        // 2. Валидация кассы
        CashRegister cashRegister = cashRegisterValidator.validateExists(cashRegisterId);
        cashRegisterValidator.validateActive(cashRegister);

        // 3. Определяем тип транзакции в зависимости от типа счёта
        TransactionType transactionType = determineTransactionType(invoice);

        // 4. Валидация суммы
        if (isPartial) {
            invoiceValidator.validatePartialPaymentAmount(invoice, amount);
        } else {
            invoiceValidator.validatePaymentAmount(invoice, amount);
        }
        invoiceValidator.validateNoOverpayment(invoice, amount);

        // 5. Для расхода (EXPENSE) проверяем, что в кассе достаточно денег
        if (transactionType == TransactionType.EXPENSE) {
            BigDecimal currentBalance = cashRegister.getCurrentBalance();
            if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientCashException(currentBalance, amount);
            }
        }

        // 6. СОЗДАЁМ ЗАПРОС ДЛЯ ТРАНЗАКЦИИ
        CashTransactionRequest request = CashTransactionRequest.builder()
                .cashRegisterId(cashRegisterId)
                .transactionType(transactionType)  // ← INCOME или EXPENSE
                .amount(amount)
                .invoiceId(invoiceId)
                .description(buildDescription(invoice, amount, isPartial, transactionType))
                .build();


        // 7. Создание кассовой операции
        CashTransaction transaction = cashTransactionProcessor.createTransaction(
                request, cashRegister, invoice, cashierId);

        invoice.getCashTransactions().add(transaction);

        // 8. Обновление статуса счёта
        updateInvoiceStatus(invoice, amount);

        // 9. Сохранение
        Invoice savedInvoice = invoiceRepository.save(invoice);

        log.info(messageService.get("invoice.payment.processor.complete",
                invoiceId, transaction.getId(), savedInvoice.getStatus()));

        return InvoicePaymentResult.builder()
                .invoice(savedInvoice)
                .transaction(transaction)
                .status(savedInvoice.getStatus())
                .build();
    }

    private void updateInvoiceStatus(Invoice invoice, BigDecimal amount) {
        BigDecimal totalPaid = invoice.getTotalPaidAmount();

        if (totalPaid.compareTo(invoice.getAmount()) >= 0) {
            invoice.markAsPaid();
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
    }

    private TransactionType determineTransactionType(Invoice invoice) {
        if (invoice.getPurchaseOrderId() != null) {
            // Счёт на закупку — мы платим поставщику (расход)
            return TransactionType.EXPENSE;
        } else if (invoice.getSalesOrderId() != null) {
            // Счёт на продажу — нам платит клиент (приход)
            return TransactionType.INCOME;
        }
        throw new IllegalArgumentException(
                messageService.get("invoice.no.order.associated", invoice.getId()));
    }

    private String getTransactionDescription(Invoice invoice, BigDecimal amount, boolean isPartial) {
        if (isPartial) {
            return messageService.get("invoice.partial.payment.description",
                    invoice.getInvoiceNumber(), amount);
        }
        return messageService.get("invoice.payment.description",
                invoice.getInvoiceNumber());
    }

    private String buildDescription(Invoice invoice, BigDecimal amount,
                                    boolean isPartial, TransactionType type) {
        String orderType = invoice.getPurchaseOrderId() != null ? "purchase" : "sales";
        String paymentType = type == TransactionType.EXPENSE ? "payment" : "receipt";

        if (isPartial) {
            return messageService.get("invoice.partial." + orderType + "." + paymentType,
                    invoice.getInvoiceNumber(), amount);
        }
        return messageService.get("invoice.full." + orderType + "." + paymentType,
                invoice.getInvoiceNumber());
    }

    @Value
    @Builder
    public static class InvoicePaymentResult {
        Invoice invoice;
        CashTransaction transaction;
        InvoiceStatus status;
    }
}