package ru.galtor85.household_store.advice.exception.invoice;

import lombok.Getter;

@Getter
public class InvoiceAlreadyCancelledException extends RuntimeException {

    private final Long invoiceId;
    private final String invoiceNumber;

    public InvoiceAlreadyCancelledException(Long invoiceId, String invoiceNumber) {
        super();
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
    }

    public InvoiceAlreadyCancelledException(Long invoiceId) {
        this(invoiceId, null);
    }

    public InvoiceAlreadyCancelledException(String invoiceNumber) {
        this(null, invoiceNumber);
    }
}