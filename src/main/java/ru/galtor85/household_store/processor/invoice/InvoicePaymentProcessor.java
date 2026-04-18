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
import ru.galtor85.household_store.entity.order.OrderType;
import ru.galtor85.household_store.processor.cash.CashTransactionProcessor;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.service.cash.CashRegisterService;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.cash.CashRegisterValidator;
import ru.galtor85.household_store.validator.finance.InvoiceValidator;

import java.math.BigDecimal;

/**
 * Processor for invoice payment operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentProcessor {

    private final InvoiceValidator invoiceValidator;
    private final CashRegisterValidator cashRegisterValidator;
    private final CashTransactionProcessor cashTransactionProcessor;
    private final InvoiceRepository invoiceRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final CashRegisterService cashRegisterService;

    /**
     * Processes a payment for an invoice.
     *
     * @param invoiceId      the invoice ID
     * @param amount         the payment amount
     * @param cashRegisterId the cash register ID
     * @param cashierId      the cashier ID
     * @param isPartial      whether this is a partial payment
     * @return InvoicePaymentResult containing invoice, transaction, and status
     */
    @Transactional
    public InvoicePaymentResult processPayment(Long invoiceId, BigDecimal amount,
                                               Long cashRegisterId, Long cashierId,
                                               boolean isPartial) {

        log.info(logMsg.get("invoice.payment.processor.start", invoiceId, amount, cashRegisterId),
                invoiceId, amount, cashRegisterId);

        // 1. Validate invoice
        Invoice invoice = invoiceValidator.validateInvoiceForPayment(invoiceId);

        // 2. Validate cash register
        CashRegister cashRegister = cashRegisterValidator.validateExists(cashRegisterId);
        cashRegisterValidator.validateActive(cashRegister);

        // 3. Determine transaction type based on invoice type
        TransactionType transactionType = determineTransactionType(invoice);

        // 4. Validate amount
        if (isPartial) {
            invoiceValidator.validatePartialPaymentAmount(invoice, amount);
        } else {
            invoiceValidator.validatePaymentAmount(invoice, amount);
        }
        invoiceValidator.validateNoOverpayment(invoice, amount);

        // 5. For expense, check sufficient cash in register
        if (transactionType == TransactionType.EXPENSE) {
            BigDecimal currentBalance = cashRegisterService.getCurrentBalance(cashRegisterId);
            if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientCashException(currentBalance, amount);
            }
        }

        // 6. Create cash transaction request
        CashTransactionRequest request = CashTransactionRequest.builder()
                .cashRegisterId(cashRegisterId)
                .transactionType(transactionType)
                .amount(amount)
                .currency(invoice.getCurrency())
                .invoiceId(invoiceId)
                .description(buildDescription(invoice, amount, isPartial, transactionType))
                .build();

        // 7. Create cash transaction
        CashTransaction transaction = cashTransactionProcessor.createTransaction(
                request, cashRegister, invoice, cashierId);

        invoice.getCashTransactions().add(transaction);

        // 8. Update invoice status
        updateInvoiceStatus(invoice);

        // 9. Save invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        log.info(logMsg.get("invoice.payment.processor.complete",
                invoiceId, transaction.getId(), savedInvoice.getStatus()));

        return InvoicePaymentResult.builder()
                .invoice(savedInvoice)
                .transaction(transaction)
                .status(savedInvoice.getStatus())
                .build();
    }

    /**
     * Updates invoice status based on total paid amount.
     *
     * @param invoice the invoice to update
     */
    private void updateInvoiceStatus(Invoice invoice) {
        BigDecimal totalPaid = invoice.getTotalPaidAmount();

        if (totalPaid.compareTo(invoice.getAmount()) >= 0) {
            invoice.markAsPaid();
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
    }

    /**
     * Determines the transaction type based on invoice type.
     *
     * @param invoice the invoice
     * @return TransactionType (EXPENSE for purchase, INCOME for sales)
     */
    private TransactionType determineTransactionType(Invoice invoice) {
        if (invoice.getPurchaseOrderId() != null) {
            return TransactionType.EXPENSE;
        } else if (invoice.getSalesOrderId() != null) {
            return TransactionType.INCOME;
        }
        throw new IllegalArgumentException(
                messageService.get("invoice.no.order.associated", invoice.getId()));
    }

    /**
     * Builds a description for the cash transaction.
     *
     * @param invoice         the invoice
     * @param amount          the payment amount
     * @param isPartial       whether this is a partial payment
     * @param transactionType the transaction type
     * @return transaction description
     */
    private String buildDescription(Invoice invoice, BigDecimal amount,
                                    boolean isPartial, TransactionType transactionType) {
        OrderType orderType = invoice.getPurchaseOrderId() != null
                ? OrderType.PURCHASE
                : OrderType.SALES;
        String paymentType = orderType.getPaymentType(transactionType);

        if (isPartial) {
            return messageService.get("invoice.partial." + orderType.getCode() + "." + paymentType,
                    invoice.getInvoiceNumber(), amount);
        }
        return messageService.get("invoice.full." + orderType.getCode() + "." + paymentType,
                invoice.getInvoiceNumber());
    }

    /**
     * Result of invoice payment processing.
     */
    @Value
    @Builder
    public static class InvoicePaymentResult {
        Invoice invoice;
        CashTransaction transaction;
        InvoiceStatus status;
    }
}