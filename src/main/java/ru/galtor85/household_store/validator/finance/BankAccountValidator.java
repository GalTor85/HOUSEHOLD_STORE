package ru.galtor85.household_store.validator.finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.finance.BankAccountNotFoundException;
import ru.galtor85.household_store.advice.exception.finance.InsufficientFundsException;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.entity.finance.BankAccount;
import ru.galtor85.household_store.repository.finance.BankAccountRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

/**
 * Validator for bank account operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankAccountValidator {

    private final BankAccountRepository bankAccountRepository;
    private final MessageService messageService;
    private final BusinessConfig businessConfig;

    /**
     * Validates that bank account exists
     *
     * @param accountId account ID
     * @return bank account entity
     * @throws BankAccountNotFoundException if account not found
     */
    public BankAccount validateExists(Long accountId) {
        return bankAccountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.error(messageService.get("bank.account.not.found", accountId));
                    return new BankAccountNotFoundException(accountId);
                });
    }

    /**
     * Validates that bank account is active
     *
     * @param account bank account entity
     * @throws IllegalStateException if account is not active
     */
    public void validateActive(BankAccount account) {
        if (!account.isActive()) {
            log.error(messageService.get("bank.account.inactive", account.getId()));
            throw new IllegalStateException(
                    messageService.get("bank.account.inactive", account.getId())
            );
        }
    }

    /**
     * Validates that account number is unique
     *
     * @param accountNumber account number
     * @throws IllegalArgumentException if account number already exists
     */
    public void validateAccountNumberUnique(String accountNumber) {
        if (bankAccountRepository.existsByAccountNumber(accountNumber)) {
            log.error(messageService.get("bank.account.number.exists", accountNumber));
            throw new IllegalArgumentException(
                    messageService.get("bank.account.number.exists", accountNumber)
            );
        }
    }

    /**
     * Validates initial balance
     *
     * @param initialBalance initial balance
     * @throws IllegalArgumentException if balance is negative
     */
    public void validateInitialBalance(BigDecimal initialBalance) {
        if (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error(messageService.get("bank.account.initial.balance.negative", initialBalance));
            throw new IllegalArgumentException(
                    messageService.get("bank.account.initial.balance.negative", initialBalance)
            );
        }
    }

    /**
     * Validates sufficient funds for withdrawal
     *
     * @param account bank account
     * @param amount  withdrawal amount
     * @throws InsufficientFundsException if insufficient funds
     */
    public void validateSufficientFunds(BankAccount account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            log.error(messageService.get("bank.account.insufficient.funds",
                    account.getId(), account.getBalance(), amount));
            throw new InsufficientFundsException(
                    account.getId(), account.getBalance(), amount
            );
        }
    }

    public void validateNameLength(String name) {
        if (name == null) return;

        int maxLength = businessConfig.getBankAccount().getMaxNameLength();
        if (name.length() > maxLength) {
            throw new IllegalArgumentException(
                    messageService.get("bank.account.validation.name.max.length", maxLength)
            );
        }
    }

    public void validateBankNameLength(String bankName) {
        if (bankName == null) return;

        int maxLength = businessConfig.getBankAccount().getMaxBankNameLength();
        if (bankName.length() > maxLength) {
            throw new IllegalArgumentException(
                    messageService.get("bank.account.validation.bank.name.max.length", maxLength)
            );
        }
    }

}