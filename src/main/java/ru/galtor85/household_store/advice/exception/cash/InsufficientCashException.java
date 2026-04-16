package ru.galtor85.household_store.advice.exception.cash;

import lombok.Getter;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

/**
 * Exception thrown when cash register has insufficient funds.
 */
@Getter
public class InsufficientCashException extends RuntimeException {

    private static final String MESSAGE_WITH_NAME = "Insufficient cash in register '%s'. Available: %s, requested: %s";
    private static final String MESSAGE_WITH_ID = "Insufficient cash in register %d. Available: %s, requested: %s";
    private static final String MESSAGE_DEFAULT = "Insufficient cash. Available: %s, requested: %s";

    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;
    private final Long cashRegisterId;
    private final String cashRegisterName;

    /**
     * Constructor with balance and requested amount.
     *
     * @param currentBalance current balance
     * @param requestedAmount requested amount
     */
    public InsufficientCashException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super();
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.cashRegisterId = null;
        this.cashRegisterName = null;
    }

    /**
     * Constructor with cash register ID.
     *
     * @param cashRegisterId cash register ID
     * @param currentBalance current balance
     * @param requestedAmount requested amount
     */
    public InsufficientCashException(Long cashRegisterId, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super();
        this.cashRegisterId = cashRegisterId;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.cashRegisterName = null;
    }

    /**
     * Constructor with cash register name.
     *
     * @param cashRegisterId cash register ID
     * @param cashRegisterName cash register name
     * @param currentBalance current balance
     * @param requestedAmount requested amount
     */
    public InsufficientCashException(Long cashRegisterId, String cashRegisterName,
                                     BigDecimal currentBalance, BigDecimal requestedAmount) {
        super();
        this.cashRegisterId = cashRegisterId;
        this.cashRegisterName = cashRegisterName;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    /**
     * Constructor with custom message.
     *
     * @param message custom message
     * @param currentBalance current balance
     * @param requestedAmount requested amount
     */
    public InsufficientCashException(String message, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(message);
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.cashRegisterId = null;
        this.cashRegisterName = null;
    }

    /**
     * Returns localized message using MessageService.
     *
     * @param messageService message service for localization
     * @return localized error message
     */
    public String getLocalizedMessage(MessageService messageService) {
        if (cashRegisterName != null) {
            return messageService.get("cash.register.insufficient.balance.with.name",
                    cashRegisterName, currentBalance, requestedAmount);
        }
        if (cashRegisterId != null) {
            return messageService.get("cash.register.insufficient.balance.with.id",
                    cashRegisterId, currentBalance, requestedAmount);
        }
        return messageService.get("cash.register.insufficient.balance", currentBalance, requestedAmount);
    }

    @Override
    public String getMessage() {
        if (cashRegisterName != null) {
            return String.format(MESSAGE_WITH_NAME, cashRegisterName, currentBalance, requestedAmount);
        }
        if (cashRegisterId != null) {
            return String.format(MESSAGE_WITH_ID, cashRegisterId, currentBalance, requestedAmount);
        }
        return String.format(MESSAGE_DEFAULT, currentBalance, requestedAmount);
    }
}