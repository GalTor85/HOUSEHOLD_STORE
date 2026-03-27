package ru.galtor85.household_store.advice.exception.cash;

import lombok.Getter;
import ru.galtor85.household_store.service.i18n.MessageService;

@Getter
public class CashRegisterNotFoundException extends RuntimeException {

    private final Long cashRegisterId;
    private final String registerNumber;

    /**
     * Конструктор для поиска по ID
     */
    public CashRegisterNotFoundException(Long cashRegisterId) {
        super();
        this.cashRegisterId = cashRegisterId;
        this.registerNumber = null;
    }

    /**
     * Конструктор для поиска по номеру
     */
    public CashRegisterNotFoundException(String registerNumber) {
        super();
        this.registerNumber = registerNumber;
        this.cashRegisterId = null;
    }

    /**
     * Конструктор с кастомным сообщением
     */
    public CashRegisterNotFoundException(String message, Long cashRegisterId) {
        super(message);
        this.cashRegisterId = cashRegisterId;
        this.registerNumber = null;
    }

    /**
     * Получает локализованное сообщение
     */
    public String getLocalizedMessage(MessageService messageService) {
        if (cashRegisterId != null) {
            return messageService.get("cash.register.not.found.id", cashRegisterId);
        }
        if (registerNumber != null) {
            return messageService.get("cash.register.not.found.number", registerNumber);
        }
        return messageService.get("cash.register.not.found");
    }

    /**
     * Получает локализованное сообщение с параметрами
     */
    public String getLocalizedMessage(MessageService messageService, Object... args) {
        if (cashRegisterId != null) {
            return messageService.get("cash.register.not.found.id", args);
        }
        if (registerNumber != null) {
            return messageService.get("cash.register.not.found.number", args);
        }
        return messageService.get("cash.register.not.found");
    }

    @Override
    public String getMessage() {
        if (cashRegisterId != null) {
            return "Cash register with ID " + cashRegisterId + " not found";
        }
        if (registerNumber != null) {
            return "Cash register with number " + registerNumber + " not found";
        }
        return "Cash register not found";
    }
}