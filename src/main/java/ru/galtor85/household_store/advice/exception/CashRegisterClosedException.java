package ru.galtor85.household_store.advice.exception;

import lombok.Getter;
import ru.galtor85.household_store.service.MessageService;

@Getter
public class CashRegisterClosedException extends RuntimeException {

    private final Long cashRegisterId;
    private final String registerName;

    /**
     * Конструктор
     */
    public CashRegisterClosedException(Long cashRegisterId) {
        super();
        this.cashRegisterId = cashRegisterId;
        this.registerName = null;
    }

    /**
     * Конструктор с именем кассы
     */
    public CashRegisterClosedException(Long cashRegisterId, String registerName) {
        super();
        this.cashRegisterId = cashRegisterId;
        this.registerName = registerName;
    }

    /**
     * Конструктор с кастомным сообщением
     */
    public CashRegisterClosedException(String message, Long cashRegisterId) {
        super(message);
        this.cashRegisterId = cashRegisterId;
        this.registerName = null;
    }

    /**
     * Получает локализованное сообщение
     */
    public String getLocalizedMessage(MessageService messageService) {
        if (registerName != null) {
            return messageService.get("cash.register.closed.with.name", registerName);
        }
        return messageService.get("cash.register.closed", cashRegisterId);
    }

    /**
     * Получает локализованное сообщение с параметрами
     */
    public String getLocalizedMessage(MessageService messageService, Object... args) {
        if (registerName != null) {
            return messageService.get("cash.register.closed.with.name", args);
        }
        return messageService.get("cash.register.closed", args);
    }

    @Override
    public String getMessage() {
        if (registerName != null) {
            return "Cash register '" + registerName + "' is closed";
        }
        return "Cash register with ID " + cashRegisterId + " is closed";
    }
}