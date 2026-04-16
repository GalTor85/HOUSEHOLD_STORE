package ru.galtor85.household_store.advice.exception.cash;

import lombok.Getter;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Exception thrown when an invoice is not found.
 */
@Getter
public class InvoiceNotFoundException extends RuntimeException {

    private static final String MESSAGE_WITH_ID = "Invoice with ID %d not found";
    private static final String MESSAGE_WITH_NUMBER = "Invoice with number %s not found";
    private static final String MESSAGE_DEFAULT = "Invoice not found";

    private final Long invoiceId;
    private final String invoiceNumber;

    /**
     * Constructor for lookup by ID.
     *
     * @param invoiceId invoice ID
     */
    public InvoiceNotFoundException(Long invoiceId) {
        super();
        this.invoiceId = invoiceId;
        this.invoiceNumber = null;
    }

    /**
     * Constructor for lookup by number.
     *
     * @param invoiceNumber invoice number
     */
    public InvoiceNotFoundException(String invoiceNumber) {
        super();
        this.invoiceNumber = invoiceNumber;
        this.invoiceId = null;
    }

    /**
     * Constructor with custom message.
     *
     * @param message custom message
     * @param invoiceId invoice ID
     */
    public InvoiceNotFoundException(String message, Long invoiceId) {
        super(message);
        this.invoiceId = invoiceId;
        this.invoiceNumber = null;
    }

    /**
     * Constructor with custom message and cause.
     *
     * @param message custom message
     * @param cause the cause
     * @param invoiceId invoice ID
     */
    public InvoiceNotFoundException(String message, Throwable cause, Long invoiceId) {
        super(message, cause);
        this.invoiceId = invoiceId;
        this.invoiceNumber = null;
    }

    /**
     * Returns localized message using MessageService.
     *
     * @param messageService message service for localization
     * @return localized error message
     */
    public String getLocalizedMessage(MessageService messageService) {
        if (invoiceId != null) {
            return messageService.get("invoice.not.found", invoiceId);
        }
        if (invoiceNumber != null) {
            return messageService.get("invoice.not.found.by.number", invoiceNumber);
        }
        return messageService.get("invoice.not.found.unknown");
    }

    @Override
    public String getMessage() {
        if (invoiceId != null) {
            return String.format(MESSAGE_WITH_ID, invoiceId);
        }
        if (invoiceNumber != null) {
            return String.format(MESSAGE_WITH_NUMBER, invoiceNumber);
        }
        return MESSAGE_DEFAULT;
    }
}