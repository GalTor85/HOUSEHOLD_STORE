package ru.galtor85.household_store.validator.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cash.CashRegisterClosedException;
import ru.galtor85.household_store.advice.exception.cash.InsufficientCashException;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Validator for cash transaction operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashTransactionValidator {

    private final CashTransactionRepository cashTransactionRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    private static final int REFUND_DAYS_LIMIT = 30;

    /**
     * Validates transaction exists.
     *
     * @param transactionId transaction ID
     * @return cash transaction entity
     * @throws IllegalArgumentException if not found
     */
    public CashTransaction validateTransactionExists(Long transactionId) {
        return cashTransactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("cash.transaction.validation.not.found", transactionId));
                    return new IllegalArgumentException(
                            messageService.get("cash.transaction.validation.not.found", transactionId)
                    );
                });
    }

    /**
     * Validates transaction can be cancelled.
     *
     * @param transaction cash transaction entity
     * @throws IllegalStateException if cannot be cancelled
     */
    public void validateTransactionCancellable(CashTransaction transaction) {
        if (transaction.getTransactionType() == TransactionType.REFUND) {
            log.error(logMsg.get("cash.transaction.validation.cannot.cancel.refund",
                    transaction.getId()));
            throw new IllegalStateException(
                    messageService.get("cash.transaction.validation.cannot.cancel.refund",
                            transaction.getId())
            );
        }

        boolean hasRefund = cashTransactionRepository.existsByOriginalTransactionId(transaction.getId());
        if (hasRefund) {
            log.error(logMsg.get("cash.transaction.validation.already.cancelled",
                    transaction.getId()));
            throw new IllegalStateException(
                    messageService.get("cash.transaction.validation.already.cancelled",
                            transaction.getId())
            );
        }
    }

    /**
     * Validates transaction request.
     *
     * @param request transaction request
     * @throws IllegalArgumentException if invalid
     */
    public void validateRequest(CashTransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error(logMsg.get("cash.transaction.validation.amount.invalid", request.getAmount()));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.amount.invalid", request.getAmount())
            );
        }
        if (request.getTransactionType() == null) {
            log.error(logMsg.get("cash.transaction.validation.type.empty"));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.type.empty")
            );
        }
        if (request.getCashRegisterId() == null) {
            log.error(logMsg.get("cash.transaction.validation.cash.register.empty"));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.cash.register.empty")
            );
        }
    }

    /**
     * Validates cash register is active.
     *
     * @param cashRegister cash register entity
     * @throws CashRegisterClosedException if closed
     */
    public void validateCashRegisterActive(CashRegister cashRegister) {
        if (cashRegister == null) {
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.cash.register.null")
            );
        }
        if (!cashRegister.getIsActive()) {
            log.error(logMsg.get("cash.transaction.validation.cash.register.inactive",
                    cashRegister.getId()));
            throw new CashRegisterClosedException(cashRegister.getId());
        }
    }

    /**
     * Validates sufficient balance for expense transaction.
     *
     * @param cashRegister cash register entity
     * @param amount transaction amount
     * @param currentBalance current balance
     * @throws InsufficientCashException if insufficient funds
     */
    public void validateSufficientBalance(CashRegister cashRegister, BigDecimal amount, BigDecimal currentBalance) {
        if (amount == null || currentBalance == null) {
            return;
        }
        if (currentBalance.compareTo(amount) < 0) {
            log.error(logMsg.get("cash.register.insufficient.balance.with.name",
                    cashRegister.getName(), currentBalance, amount));
            throw new InsufficientCashException(
                    cashRegister.getId(), cashRegister.getName(), currentBalance, amount
            );
        }
    }

    /**
     * Validates invoice exists and is payable.
     *
     * @param invoice invoice entity
     * @param amount payment amount
     * @throws IllegalArgumentException if invoice invalid
     * @throws IllegalStateException if invoice not payable
     */
    public void validateInvoiceForPayment(Invoice invoice, BigDecimal amount) {
        if (invoice == null) {
            log.error(logMsg.get("cash.transaction.validation.invoice.not.found"));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.invoice.not.found")
            );
        }
        if (invoice.isNotPayable()) {
            log.error(logMsg.get("invoice.not.payable", invoice.getId(), invoice.getStatus()));
            throw new IllegalStateException(
                    messageService.get("invoice.not.payable", invoice.getId(), invoice.getStatus())
            );
        }
        BigDecimal remaining = invoice.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            log.error(logMsg.get("cash.transaction.validation.amount.exceeds", amount, remaining));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.amount.exceeds", amount, remaining)
            );
        }
    }

    /**
     * Validates that a transaction can be refunded.
     *
     * @param transaction the transaction to refund
     * @param refundAmount the amount to refund
     * @throws IllegalStateException if refund is not allowed
     */
    public void validateTransactionRefundable(CashTransaction transaction, BigDecimal refundAmount) {
        // 1. Cannot refund a REFUND transaction
        if (transaction.getTransactionType() == TransactionType.REFUND) {
            log.error(logMsg.get("cash.transaction.validation.cannot.refund.refund",
                    transaction.getId()));
            throw new IllegalStateException(
                    messageService.get("cash.transaction.validation.cannot.refund.refund",
                            transaction.getId())
            );
        }

        // 2. Cannot refund already refunded transaction
        boolean hasRefund = cashTransactionRepository.existsByOriginalTransactionId(transaction.getId());
        if (hasRefund) {
            log.error(logMsg.get("cash.transaction.validation.already.refunded",
                    transaction.getId()));
            throw new IllegalStateException(
                    messageService.get("cash.transaction.validation.always.refunded",
                            transaction.getId())
            );
        }

        // 3. Check refund deadline (30 days)
        LocalDateTime deadline = transaction.getCreatedAt().plusDays(REFUND_DAYS_LIMIT);
        if (LocalDateTime.now().isAfter(deadline)) {
            log.error(logMsg.get("cash.transaction.validation.refund.deadline.expired",
                    transaction.getId(), REFUND_DAYS_LIMIT));
            throw new IllegalStateException(
                    messageService.get("cash.transaction.validation.refund.deadline.expired",
                            transaction.getId(), REFUND_DAYS_LIMIT)
            );
        }

        // 4. Refund amount cannot exceed transaction amount
        if (refundAmount != null && refundAmount.compareTo(transaction.getAmount()) > 0) {
            log.error(logMsg.get("cash.transaction.validation.refund.amount.exceeds",
                    refundAmount, transaction.getAmount()));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.refund.amount.exceeds",
                            refundAmount, transaction.getAmount())
            );
        }
    }

    /**
     * Validates that a transaction exists and is not a refund.
     *
     * @param transactionId transaction ID
     * @return CashTransaction entity
     */
    public CashTransaction validateRefundableTransactionExists(Long transactionId) {
        CashTransaction transaction = validateTransactionExists(transactionId);
        validateTransactionRefundable(transaction, null);
        return transaction;
    }

    /**
     * Checks if a refund already exists for a transaction.
     *
     * @param transactionId original transaction ID
     * @return true if refund exists
     */
    public boolean hasRefund(Long transactionId) {
        return cashTransactionRepository.existsByOriginalTransactionId(transactionId);
    }

    // CashTransactionValidator.java
    /**
     * Validates that invoice can be refunded.
     *
     * @param invoice the invoice entity
     * @param refundAmount the amount to refund
     * @throws IllegalArgumentException if invoice is not refundable
     */
    public void validateInvoiceForRefund(Invoice invoice, BigDecimal refundAmount) {
        if (invoice == null) {
            log.error(logMsg.get("cash.transaction.validation.invoice.not.found"));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.invoice.not.found")
            );
        }

        // For refund: invoice must be PAID or PARTIALLY_PAID
        if (invoice.getStatus() != InvoiceStatus.PAID &&
                invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID) {
            log.error(logMsg.get("invoice.not.refundable", invoice.getId(), invoice.getStatus()));
            throw new IllegalStateException(
                    messageService.get("invoice.not.refundable", invoice.getId(), invoice.getStatus())
            );
        }

        // Refund amount cannot exceed total paid amount
        BigDecimal totalPaid = invoice.getTotalPaidAmount();
        if (refundAmount != null && refundAmount.compareTo(totalPaid) > 0) {
            log.error(logMsg.get("cash.transaction.validation.refund.amount.exceeds.paid",
                    refundAmount, totalPaid));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.refund.amount.exceeds.paid",
                            refundAmount, totalPaid)
            );
        }
    }
}
