package ru.galtor85.household_store.advice.exception;

import lombok.Getter;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;

@Getter
public class InsufficientCashException extends RuntimeException {

    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;
    private final Long cashRegisterId;
    private final String cashRegisterName;

    /**
     * Конструктор
     */
    public InsufficientCashException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super();
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.cashRegisterId = null;
        this.cashRegisterName = null;
    }

    /**
     * Конструктор с ID кассы
     */
    public InsufficientCashException(Long cashRegisterId, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super();
        this.cashRegisterId = cashRegisterId;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.cashRegisterName = null;
    }

    /**
     * Конструктор с именем кассы
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
     * Конструктор с кастомным сообщением
     */
    public InsufficientCashException(String message, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(message);
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.cashRegisterId = null;
        this.cashRegisterName = null;
    }

    /**
     * Получает локализованное сообщение
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
        return messageService.get("cash.register.insufficient.balance",
                currentBalance, requestedAmount);
    }

    /**
     * Получает локализованное сообщение с параметрами
     */
    public String getLocalizedMessage(MessageService messageService, Object... args) {
        if (cashRegisterName != null) {
            return messageService.get("cash.register.insufficient.balance.with.name", args);
        }
        if (cashRegisterId != null) {
            return messageService.get("cash.register.insufficient.balance.with.id", args);
        }
        return messageService.get("cash.register.insufficient.balance", args);
    }

    @Override
    public String getMessage() {
        if (cashRegisterName != null) {
            return "Insufficient cash in register '" + cashRegisterName +
                    "'. Available: " + currentBalance + ", requested: " + requestedAmount;
        }
        if (cashRegisterId != null) {
            return "Insufficient cash in register " + cashRegisterId +
                    ". Available: " + currentBalance + ", requested: " + requestedAmount;
        }
        return "Insufficient cash. Available: " + currentBalance + ", requested: " + requestedAmount;
    }
}