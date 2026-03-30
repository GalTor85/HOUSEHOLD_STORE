package ru.galtor85.household_store.advice.exception.invoice;

import lombok.Getter;

@Getter
public class InvoiceAlreadyPaidException extends RuntimeException {

    private final Long invoiceId;
    private final String invoiceNumber;

    public InvoiceAlreadyPaidException(Long invoiceId, String invoiceNumber) {
        super();
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
    }

    public InvoiceAlreadyPaidException(Long invoiceId) {
        this(invoiceId, null);
    }

    public InvoiceAlreadyPaidException(String invoiceNumber) {
        this(null, invoiceNumber);
    }
}