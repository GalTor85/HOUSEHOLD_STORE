package ru.galtor85.household_store.validator.finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.finance.BankAccountNotFoundException;
import ru.galtor85.household_store.advice.exception.finance.InsufficientFundsException;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.entity.finance.BankAccount;
import ru.galtor85.household_store.repository.finance.BankAccountRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

/**
 * Validator for bank account operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankAccountValidator {

    private final BankAccountRepository bankAccountRepository;
    private final MessageService messageService;
    private final BusinessConfig businessConfig;
    private final LogMessageService logMsg;

    /**
     * Validates bank account exists.
     *
     * @param accountId account ID
     * @return bank account entity
     * @throws BankAccountNotFoundException if not found
     */
    public BankAccount validateExists(Long accountId) {
        return bankAccountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("bank.account.not.found.id", accountId));
                    return new BankAccountNotFoundException(accountId);
                });
    }

    /**
     * Validates bank account is active.
     *
     * @param account bank account entity
     * @throws IllegalStateException if inactive
     */
    public void validateActive(BankAccount account) {
        if (!account.isActive()) {
            log.error(logMsg.get("bank.account.inactive", account.getId()));
            throw new IllegalStateException(
                    messageService.get("bank.account.inactive", account.getId())
            );
        }
    }

    /**
     * Validates account number is unique.
     *
     * @param accountNumber account number
     * @throws IllegalArgumentException if already exists
     */
    public void validateAccountNumberUnique(String accountNumber) {
        if (bankAccountRepository.existsByAccountNumber(accountNumber)) {
            log.error(logMsg.get("bank.account.number.exists", accountNumber));
            throw new IllegalArgumentException(
                    messageService.get("bank.account.number.exists", accountNumber)
            );
        }
    }

    /**
     * Validates initial balance is not negative.
     *
     * @param initialBalance initial balance
     * @throws IllegalArgumentException if negative
     */
    public void validateInitialBalance(BigDecimal initialBalance) {
        if (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error(logMsg.get("bank.account.initial.balance.negative", initialBalance));
            throw new IllegalArgumentException(
                    messageService.get("bank.account.initial.balance.negative", initialBalance)
            );
        }
    }

    /**
     * Validates sufficient funds for withdrawal.
     *
     * @param account bank account
     * @param amount withdrawal amount
     * @throws InsufficientFundsException if insufficient funds
     */
    public void validateSufficientFunds(BankAccount account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            log.error(logMsg.get("bank.account.insufficient.funds",
                    account.getId(), account.getBalance(), amount));
            throw new InsufficientFundsException(
                    account.getId(), account.getBalance(), amount
            );
        }
    }

    /**
     * Validates account name length.
     *
     * @param name account name
     * @throws IllegalArgumentException if exceeds max length
     */
    public void validateNameLength(String name) {
        if (name == null) {
            return;
        }
        int maxLength = businessConfig.getBankAccount().getMaxNameLength();
        if (name.length() > maxLength) {
            throw new IllegalArgumentException(
                    messageService.get("bank.account.validation.name.max.length", maxLength)
            );
        }
    }

    /**
     * Validates bank name length.
     *
     * @param bankName bank name
     * @throws IllegalArgumentException if exceeds max length
     */
    public void validateBankNameLength(String bankName) {
        if (bankName == null) {
            return;
        }
        int maxLength = businessConfig.getBankAccount().getMaxBankNameLength();
        if (bankName.length() > maxLength) {
            throw new IllegalArgumentException(
                    messageService.get("bank.account.validation.bank.name.max.length", maxLength)
            );
        }
    }
}