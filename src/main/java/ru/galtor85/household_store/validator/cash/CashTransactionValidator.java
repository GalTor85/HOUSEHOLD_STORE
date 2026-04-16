package ru.galtor85.household_store.validator.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cash.CashRegisterClosedException;
import ru.galtor85.household_store.advice.exception.cash.InsufficientCashException;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.entity.finance.CashTransaction;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.TransactionType;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

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
}