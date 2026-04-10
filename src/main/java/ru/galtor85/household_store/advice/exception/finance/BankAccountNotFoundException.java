package ru.galtor85.household_store.advice.exception.finance;

import lombok.Getter;

/**
 * Exception thrown when a bank account is not found
 */
@Getter
public class BankAccountNotFoundException extends RuntimeException {

    private final Long accountId;
    private final String accountNumber;

    public BankAccountNotFoundException(Long accountId) {
        super();
        this.accountId = accountId;
        this.accountNumber = null;
    }

    public BankAccountNotFoundException(String accountNumber) {
        super();
        this.accountNumber = accountNumber;
        this.accountId = null;
    }

    public BankAccountNotFoundException(String message, Long accountId) {
        super(message);
        this.accountId = accountId;
        this.accountNumber = null;
    }
}