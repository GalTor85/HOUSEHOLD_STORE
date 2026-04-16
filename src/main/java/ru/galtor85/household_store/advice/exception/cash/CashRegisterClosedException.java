package ru.galtor85.household_store.advice.exception.cash;

import lombok.Getter;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Exception thrown when attempting to operate on a closed cash register.
 */
@Getter
public class CashRegisterClosedException extends RuntimeException {

    private static final String MESSAGE_WITH_NAME = "Cash register '%s' is closed";
    private static final String MESSAGE_WITH_ID = "Cash register with ID %d is closed";

    private final Long cashRegisterId;
    private final String registerName;

    /**
     * Constructor with cash register ID.
     *
     * @param cashRegisterId cash register ID
     */
    public CashRegisterClosedException(Long cashRegisterId) {
        super();
        this.cashRegisterId = cashRegisterId;
        this.registerName = null;
    }

    /**
     * Constructor with cash register ID and name.
     *
     * @param cashRegisterId cash register ID
     * @param registerName cash register name
     */
    public CashRegisterClosedException(Long cashRegisterId, String registerName) {
        super();
        this.cashRegisterId = cashRegisterId;
        this.registerName = registerName;
    }

    /**
     * Constructor with custom message.
     *
     * @param message custom message
     * @param cashRegisterId cash register ID
     */
    public CashRegisterClosedException(String message, Long cashRegisterId) {
        super(message);
        this.cashRegisterId = cashRegisterId;
        this.registerName = null;
    }

    /**
     * Returns localized message using MessageService.
     *
     * @param messageService message service for localization
     * @return localized error message
     */
    public String getLocalizedMessage(MessageService messageService) {
        if (registerName != null) {
            return messageService.get("cash.register.closed.with.name", registerName);
        }
        return messageService.get("cash.register.closed", cashRegisterId);
    }

    @Override
    public String getMessage() {
        if (registerName != null) {
            return String.format(MESSAGE_WITH_NAME, registerName);
        }
        return String.format(MESSAGE_WITH_ID, cashRegisterId);
    }
}