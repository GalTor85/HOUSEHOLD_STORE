package ru.galtor85.household_store.validator.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cash.CashRegisterClosedException;
import ru.galtor85.household_store.advice.exception.cash.CashRegisterNotFoundException;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.repository.cash.CashRegisterRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

/**
 * Validator for cash register operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashRegisterValidator {

    private final CashRegisterRepository cashRegisterRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Validates cash register exists.
     *
     * @param cashRegisterId cash register ID
     * @return cash register entity
     * @throws CashRegisterNotFoundException if not found
     */
    public CashRegister validateExists(Long cashRegisterId) {
        return cashRegisterRepository.findById(cashRegisterId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("cash.register.not.found.id", cashRegisterId));
                    return new CashRegisterNotFoundException(cashRegisterId);
                });
    }

    /**
     * Validates cash register is active.
     *
     * @param cashRegister cash register entity
     * @throws CashRegisterClosedException if closed
     */
    public void validateActive(CashRegister cashRegister) {
        if (!cashRegister.getIsActive()) {
            log.error(logMsg.get("cash.register.closed", cashRegister.getId()));
            throw new CashRegisterClosedException(cashRegister.getId());
        }
    }

    /**
     * Validates cash register is closed.
     *
     * @param cashRegister cash register entity
     * @throws IllegalStateException if open
     */
    public void validateClosed(CashRegister cashRegister) {
        if (cashRegister.getIsActive()) {
            log.error(logMsg.get("cash.register.is.open", cashRegister.getId()));
            throw new IllegalStateException(
                    messageService.get("cash.register.is.open", cashRegister.getId())
            );
        }
    }

    /**
     * Validates register number is unique.
     *
     * @param registerNumber register number
     * @throws IllegalArgumentException if already exists
     */
    public void validateRegisterNumberUnique(String registerNumber) {
        if (cashRegisterRepository.findByRegisterNumber(registerNumber).isPresent()) {
            log.error(logMsg.get("cash.register.number.exists", registerNumber));
            throw new IllegalArgumentException(
                    messageService.get("cash.register.number.exists", registerNumber)
            );
        }
    }

    /**
     * Validates cashier is the same who opened the register.
     *
     * @param cashRegister cash register entity
     * @param cashierId cashier ID
     * @throws IllegalStateException if wrong cashier
     */
    public void validateCashier(CashRegister cashRegister, Long cashierId) {
        if (cashRegister.getCashierId() != null && !cashRegister.getCashierId().equals(cashierId)) {
            log.error(logMsg.get("cash.register.wrong.cashier",
                    cashRegister.getId(), cashierId));
            throw new IllegalStateException(
                    messageService.get("cash.register.wrong.cashier",
                            cashRegister.getId(), cashierId)
            );
        }
    }

    /**
     * Validates opening balance is not negative.
     *
     * @param openingBalance opening balance
     * @throws IllegalArgumentException if negative
     */
    public void validateOpeningBalance(BigDecimal openingBalance) {
        if (openingBalance != null && openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error(logMsg.get("cash.register.opening.balance.negative", openingBalance));
            throw new IllegalArgumentException(
                    messageService.get("cash.register.opening.balance.negative", openingBalance)
            );
        }
    }

    /**
     * Validates closing balance is valid.
     *
     * @param closingBalance closing balance
     * @throws IllegalArgumentException if null or negative
     */
    public void validateClosingBalance(BigDecimal closingBalance) {
        if (closingBalance == null) {
            log.error(logMsg.get("cash.register.closing.balance.required"));
            throw new IllegalArgumentException(
                    messageService.get("cash.register.closing.balance.required"));
        }
        if (closingBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error(logMsg.get("cash.register.closing.balance.negative", closingBalance));
            throw new IllegalArgumentException(
                    messageService.get("cash.register.closing.balance.negative", closingBalance)
            );
        }
    }

    /**
     * Validates discrepancy reason is provided when balances differ.
     *
     * @param actualBalance actual closing balance
     * @param calculatedBalance calculated balance
     * @param discrepancyReason reason for discrepancy
     * @throws IllegalArgumentException if reason required but not provided
     */
    public void validateDiscrepancyReason(BigDecimal actualBalance,
                                          BigDecimal calculatedBalance,
                                          String discrepancyReason) {
        if (actualBalance == null || calculatedBalance == null) {
            return;
        }
        BigDecimal discrepancy = actualBalance.subtract(calculatedBalance);
        if (discrepancy.compareTo(BigDecimal.ZERO) != 0) {
            if (discrepancyReason == null || discrepancyReason.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        messageService.get("cash.register.discrepancy.reason.required"));
            }
        }
    }
}