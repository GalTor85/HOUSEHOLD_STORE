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
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashTransactionValidator {

    private final CashTransactionRepository cashTransactionRepository;
    private final MessageService messageService;

    /**
     * Проверяет существование операции
     */
    public CashTransaction validateTransactionExists(Long transactionId) {
        return cashTransactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.error(messageService.get("cash.transaction.validation.not.found", transactionId));
                    return new IllegalArgumentException(
                            messageService.get("cash.transaction.validation.not.found", transactionId)
                    );
                });
    }

    /**
     * Проверяет, можно ли отменить операцию
     */
    public void validateTransactionCancellable(CashTransaction transaction) {
        if (transaction.getTransactionType() == TransactionType.REFUND) {
            log.error(messageService.get("cash.transaction.validation.cannot.cancel.refund",
                    transaction.getId()));
            throw new IllegalStateException(
                    messageService.get("cash.transaction.validation.cannot.cancel.refund",
                            transaction.getId())
            );
        }

        // Проверяем, не была ли операция уже отменена
        boolean hasRefund = cashTransactionRepository.existsByOriginalTransactionId(transaction.getId());
        if (hasRefund) {
            log.error(messageService.get("cash.transaction.validation.already.cancelled",
                    transaction.getId()));
            throw new IllegalStateException(
                    messageService.get("cash.transaction.validation.already.cancelled",
                            transaction.getId())
            );
        }
    }

    /**
     * Проверяет запрос на создание операции
     */
    public void validateRequest(CashTransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error(messageService.get("cash.transaction.validation.amount.invalid", request.getAmount()));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.amount.invalid", request.getAmount())
            );
        }

        if (request.getTransactionType() == null) {
            log.error(messageService.get("cash.transaction.validation.type.empty"));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.type.empty")
            );
        }

        if (request.getCashRegisterId() == null) {
            log.error(messageService.get("cash.transaction.validation.cash.register.empty"));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.cash.register.empty")
            );
        }
    }

    /**
     * Проверяет, активна ли касса
     */
    public void validateCashRegisterActive(CashRegister cashRegister) {
        if (cashRegister == null) {
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.cash.register.null")
            );
        }
        if (!cashRegister.getIsActive()) {
            log.error(messageService.get("cash.transaction.validation.cash.register.inactive",
                    cashRegister.getId()));
            throw new CashRegisterClosedException(cashRegister.getId());
        }
    }

    /**
     * Проверяет, что операция возможна при текущем балансе кассы
     */
    public void validateSufficientBalance(CashRegister cashRegister, BigDecimal amount, BigDecimal currentBalance) {
        if (amount == null || currentBalance == null) {
            return;
        }

        if (currentBalance.compareTo(amount) < 0) {
            log.error(messageService.get("cash.register.insufficient.balance.with.name",
                    cashRegister.getName(), currentBalance, amount));
            throw new InsufficientCashException(
                    cashRegister.getId(), cashRegister.getName(), currentBalance, amount
            );
        }
    }

    /**
     * Проверяет, что счет существует и может быть оплачен
     */
    public void validateInvoiceForPayment(Invoice invoice, BigDecimal amount) {
        if (invoice == null) {
            log.error(messageService.get("cash.transaction.validation.invoice.not.found"));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.invoice.not.found")
            );
        }

        if (!invoice.isPayable()) {
            log.error(messageService.get("invoice.not.payable", invoice.getId(), invoice.getStatus()));
            throw new IllegalStateException(
                    messageService.get("invoice.not.payable", invoice.getId(), invoice.getStatus())
            );
        }

        BigDecimal remaining = invoice.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            log.error(messageService.get("cash.transaction.validation.amount.exceeds", amount, remaining));
            throw new IllegalArgumentException(
                    messageService.get("cash.transaction.validation.amount.exceeds", amount, remaining)
            );
        }
    }
}