package ru.galtor85.household_store.processor.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cash.InsufficientCashException;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.service.cash.CashRegisterService;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.cash.CashBalanceCalculator;

import java.math.BigDecimal;

/**
 * Processor for cash transaction operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashTransactionProcessor {

    private final MessageService messageService;
    private final CashTransactionRepository cashTransactionRepository;
    private final CashRegisterService cashRegisterService;
    private final LogMessageService logMsg;
    private final CashBalanceCalculator balanceCalculator;

    /**
     * Creates a new cash transaction.
     *
     * @param request      the transaction request
     * @param cashRegister the cash register
     * @param invoice      the associated invoice (optional)
     * @param cashierId    ID of the cashier performing the transaction
     * @return created CashTransaction entity
     */
    @Transactional
    public CashTransaction createTransaction(CashTransactionRequest request,
                                             CashRegister cashRegister,
                                             Invoice invoice,
                                             Long cashierId) {
        log.info(logMsg.get("cash.transaction.processor.create.start",
                request.getTransactionType().getLocalizedName(messageService),
                request.getAmount()));

        // Get current balance BEFORE transaction
        BigDecimal currentBalance = cashRegisterService.getCurrentBalance(cashRegister.getId());

        // Calculate balance after
        BigDecimal balanceAfter = balanceCalculator.getBalanceAfter(currentBalance, request.getTransactionType(),
                request.getAmount(), invoice);

        CashTransaction transaction = CashTransaction.builder()
                .cashRegister(cashRegister)
                .invoice(invoice)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .currency(request.getNormalizedCurrency())
                .paymentMethod(request.getPaymentMethod())
                .cashierId(cashierId)
                .description(request.getDescription())
                .notes(request.getNotes())
                .balanceBefore(currentBalance)
                .balanceAfter(balanceAfter)
                .build();

        CashTransaction saved = cashTransactionRepository.save(transaction);
        if (invoice != null) {
            invoice.getCashTransactions().add(saved);
        }

        log.info(logMsg.get("cash.transaction.processor.created",
                saved.getId(), saved.getAmount()));

        return saved;
    }

    /**
     * Creates a refund transaction for an existing transaction.
     *
     * @param original  the original transaction to refund
     * @param reason    the reason for refund
     * @param cashierId ID of the cashier performing the refund
     * @return created refund CashTransaction entity
     */
    @Transactional
    public CashTransaction createRefundTransaction(CashTransaction original, String reason, Long cashierId) {
        log.info(logMsg.get("cash.transaction.processor.refund.start",
                original.getId(), reason));

        // Get current balance BEFORE refund
        BigDecimal currentBalance = cashRegisterService.getCurrentBalance(original.getCashRegister().getId());

        // Calculate balance after based on order type
        // For SALES order: refund TO customer - DECREASES balance
        // For PURCHASE order: refund FROM supplier - INCREASES balance
        BigDecimal balanceAfter;
        boolean isSalesReturn = original.getInvoice() != null && original.getInvoice().getSalesOrderId() != null;

        if (isSalesReturn) {
            // Customer refund - money goes OUT of cash register
            balanceAfter = currentBalance.subtract(original.getAmount());
            log.debug(logMsg.get("cash.transaction.processor.refund.sales.balance.decrease",
                    original.getAmount()));

            // Check sufficient funds for customer refund
            if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientCashException(
                        messageService.get("cash.register.insufficient.balance.for.refund",
                                currentBalance, original.getAmount()),
                        currentBalance, original.getAmount()
                );
            }
        } else {
            // Supplier refund - money comes INTO cash register
            balanceAfter = currentBalance.add(original.getAmount());
            log.debug(logMsg.get("cash.transaction.processor.refund.purchase.balance.increase",
                    original.getAmount()));
        }

        CashTransaction refund = CashTransaction.builder()
                .cashRegister(original.getCashRegister())
                .invoice(original.getInvoice())
                .transactionType(TransactionType.REFUND)
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .paymentMethod(original.getPaymentMethod())
                .cashierId(cashierId)
                .description(messageService.get("cash.transaction.refund.description", reason))
                .notes(buildRefundNotes(original, reason))
                .originalTransactionId(original.getId())
                .balanceBefore(currentBalance)
                .balanceAfter(balanceAfter)
                .build();

        CashTransaction saved = cashTransactionRepository.save(refund);

        log.info(logMsg.get("cash.transaction.processor.refund.created",
                saved.getId(), original.getId()));

        return saved;
    }

    /**
     * Creates a partial refund transaction for an existing transaction.
     *
     * @param original     the original transaction to refund
     * @param refundAmount the amount to refund
     * @param reason       the reason for refund
     * @param cashierId    ID of the cashier performing the refund
     * @return created partial refund CashTransaction entity
     */
    @Transactional
    public CashTransaction createPartialRefundTransaction(CashTransaction original,
                                                          BigDecimal refundAmount,
                                                          String reason,
                                                          Long cashierId) {
        log.info(logMsg.get("cash.transaction.processor.partial.refund.start",
                original.getId(), refundAmount, reason));

        // Get current balance BEFORE refund
        BigDecimal currentBalance = cashRegisterService.getCurrentBalance(original.getCashRegister().getId());

        // Calculate balance after based on order type
        BigDecimal balanceAfter;
        if (original.getInvoice() != null && original.getInvoice().getSalesOrderId() != null) {
            // SALES order: refund TO customer - money goes OUT of cash register
            balanceAfter = currentBalance.subtract(refundAmount);
            log.debug("Sales order partial refund: balance will decrease by {}", refundAmount);
        } else {
            // PURCHASE order: refund FROM supplier - money comes INTO cash register
            balanceAfter = currentBalance.add(refundAmount);
            log.debug("Purchase order partial refund: balance will increase by {}", refundAmount);
        }

        CashTransaction refund = CashTransaction.builder()
                .cashRegister(original.getCashRegister())
                .invoice(original.getInvoice())
                .transactionType(TransactionType.REFUND)
                .amount(refundAmount)
                .currency(original.getCurrency())
                .paymentMethod(original.getPaymentMethod())
                .cashierId(cashierId)
                .description("Partial refund: " + reason)
                .notes(original.getNotes() != null ?
                        original.getNotes() + "\nPartial refund: " + reason : "Partial refund: " + reason)
                .originalTransactionId(original.getId())
                .balanceBefore(currentBalance)
                .balanceAfter(balanceAfter)
                .build();

        CashTransaction saved = cashTransactionRepository.save(refund);

        log.info(logMsg.get("cash.transaction.processor.partial.refund.created",
                saved.getId(), original.getId(), refundAmount));

        return saved;
    }

    private String buildRefundNotes(CashTransaction original, String reason) {
        if (original.getNotes() != null) {
            return original.getNotes() + "\n" +
                    messageService.get("cash.transaction.refund.note", reason);
        }
        return messageService.get("cash.transaction.refund.note", reason);
    }
}