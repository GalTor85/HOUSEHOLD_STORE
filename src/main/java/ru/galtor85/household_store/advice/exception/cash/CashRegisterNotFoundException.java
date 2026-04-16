package ru.galtor85.household_store.advice.exception.cash;

import lombok.Getter;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Exception thrown when a cash register is not found.
 */
@Getter
public class CashRegisterNotFoundException extends RuntimeException {

    private static final String MESSAGE_WITH_ID = "Cash register with ID %d not found";
    private static final String MESSAGE_WITH_NUMBER = "Cash register with number %s not found";
    private static final String MESSAGE_DEFAULT = "Cash register not found";

    private final Long cashRegisterId;
    private final String registerNumber;

    /**
     * Constructor for lookup by ID.
     *
     * @param cashRegisterId cash register ID
     */
    public CashRegisterNotFoundException(Long cashRegisterId) {
        super();
        this.cashRegisterId = cashRegisterId;
        this.registerNumber = null;
    }

    /**
     * Constructor for lookup by number.
     *
     * @param registerNumber cash register number
     */
    public CashRegisterNotFoundException(String registerNumber) {
        super();
        this.registerNumber = registerNumber;
        this.cashRegisterId = null;
    }

    /**
     * Constructor with custom message.
     *
     * @param message custom message
     * @param cashRegisterId cash register ID
     */
    public CashRegisterNotFoundException(String message, Long cashRegisterId) {
        super(message);
        this.cashRegisterId = cashRegisterId;
        this.registerNumber = null;
    }

    /**
     * Returns localized message using MessageService.
     *
     * @param messageService message service for localization
     * @return localized error message
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

    @Override
    public String getMessage() {
        if (cashRegisterId != null) {
            return String.format(MESSAGE_WITH_ID, cashRegisterId);
        }
        if (registerNumber != null) {
            return String.format(MESSAGE_WITH_NUMBER, registerNumber);
        }
        return MESSAGE_DEFAULT;
    }
}