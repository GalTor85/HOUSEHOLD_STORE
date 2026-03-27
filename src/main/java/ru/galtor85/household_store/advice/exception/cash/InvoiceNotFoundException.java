package ru.galtor85.household_store.advice.exception.cash;

import lombok.Getter;
import ru.galtor85.household_store.service.i18n.MessageService;

@Getter
public class InvoiceNotFoundException extends RuntimeException {

    private final Long invoiceId;
    private final String invoiceNumber;

    /**
     * Конструктор для поиска по ID
     */
    public InvoiceNotFoundException(Long invoiceId) {
        super();
        this.invoiceId = invoiceId;
        this.invoiceNumber = null;
    }

    /**
     * Конструктор для поиска по номеру
     */
    public InvoiceNotFoundException(String invoiceNumber) {
        super();
        this.invoiceNumber = invoiceNumber;
        this.invoiceId = null;
    }

    /**
     * Конструктор с кастомным сообщением
     */
    public InvoiceNotFoundException(String message, Long invoiceId) {
        super(message);
        this.invoiceId = invoiceId;
        this.invoiceNumber = null;
    }

    /**
     * Конструктор с кастомным сообщением и причиной
     */
    public InvoiceNotFoundException(String message, Throwable cause, Long invoiceId) {
        super(message, cause);
        this.invoiceId = invoiceId;
        this.invoiceNumber = null;
    }

    /**
     * Получает локализованное сообщение
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

    /**
     * Получает локализованное сообщение с параметрами
     */
    public String getLocalizedMessage(MessageService messageService, Object... args) {
        if (invoiceId != null) {
            return messageService.get("invoice.not.found", args);
        }
        if (invoiceNumber != null) {
            return messageService.get("invoice.not.found.by.number", args);
        }
        return messageService.get("invoice.not.found.unknown");
    }

    @Override
    public String getMessage() {
        if (invoiceId != null) {
            return "Invoice with ID " + invoiceId + " not found";
        }
        if (invoiceNumber != null) {
            return "Invoice with number " + invoiceNumber + " not found";
        }
        return "Invoice not found";
    }
}