package ru.galtor85.household_store.processor.invoice;

import ru.galtor85.household_store.entity.finance.Invoice;

/**
 * Interface for invoice creation strategies.
 *
 * @param <T> order type (PurchaseOrder or SalesOrder)
 */
public interface InvoiceCreator<T> {

    /**
     * Creates invoice for order.
     *
     * @param order order entity
     * @param createdBy user ID creating invoice
     * @return created invoice
     */
    Invoice createInvoice(T order, Long createdBy);

    /**
     * Checks if invoice should be created automatically.
     *
     * @param order order entity
     * @return true if you should create
     */
    boolean shouldCreateInvoice(T order);
}