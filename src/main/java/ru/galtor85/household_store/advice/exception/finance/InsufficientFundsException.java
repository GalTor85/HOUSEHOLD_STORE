package ru.galtor85.household_store.advice.exception.finance;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exception thrown when there are insufficient funds in a bank account
 */
@Getter
public class InsufficientFundsException extends RuntimeException {

    private final Long accountId;
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(Long accountId, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super();
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public InsufficientFundsException(String message) {
        super(message);
        this.accountId = null;
        this.currentBalance = null;
        this.requestedAmount = null;
    }
}