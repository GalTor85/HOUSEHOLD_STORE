package ru.galtor85.household_store.processor.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.galtor85.household_store.constants.TechnicalConstants.DATE_FORMAT_PATTERN;

/**
 * Invoice creator for purchase orders.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderInvoiceCreator implements InvoiceCreator<PurchaseOrder> {

    private final LogMessageService logMsg;
    private final MessageService messageService;
    private final FinancialConfig financialConfig;

    @Value("${app.invoice.purchase.due-days:30}")
    private int purchaseDueDays;

    /**
     * Creates an invoice for a purchase order.
     *
     * @param order     the purchase order
     * @param createdBy ID of the user creating the invoice
     * @return created Invoice entity
     */
    @Override
    public Invoice createInvoice(PurchaseOrder order, Long createdBy) {
        log.info(logMsg.get("invoice.creator.purchase.start",
                order.getOrderNumber(), order.getTotalAmount()));

        LocalDateTime issueDate = LocalDateTime.now();
        LocalDateTime dueDate = calculateDueDate(order);

        return Invoice.builder()
                .purchaseOrderId(order.getId())
                .amount(order.getTotalAmount())
                .currency(financialConfig.getDefaultCurrency())
                .status(InvoiceStatus.PENDING)
                .paymentMethod(determinePaymentMethod(order))
                .issueDate(issueDate)
                .dueDate(dueDate)
                .description(getDescription(order))
                .notes(getNotes(order))
                .createdBy(createdBy)
                .build();
    }

    /**
     * Determines the payment method for a purchase order.
     *
     * @param order the purchase order
     * @return PaymentMethod (always BANK_TRANSFER for purchases)
     */
    @Override
    public PaymentMethod determinePaymentMethod(PurchaseOrder order) {
        return PaymentMethod.BANK_TRANSFER;
    }

    /**
     * Calculates the due date for a purchase order invoice.
     *
     * @param order the purchase order
     * @return due date (configured days from now)
     */
    @Override
    public LocalDateTime calculateDueDate(PurchaseOrder order) {
        return LocalDateTime.now().plusDays(purchaseDueDays);
    }

    /**
     * Builds the invoice description.
     *
     * @param order the purchase order
     * @return localized description
     */
    @Override
    public String getDescription(PurchaseOrder order) {
        return messageService.get("invoice.description.purchase", order.getOrderNumber());
    }

    /**
     * Builds the invoice notes.
     *
     * @param order the purchase order
     * @return localized notes with dates
     */
    @Override
    public String getNotes(PurchaseOrder order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
        return messageService.get("invoice.notes.purchase",
                order.getOrderNumber(),
                LocalDateTime.now().format(formatter),
                calculateDueDate(order).format(formatter));
    }

    /**
     * Determines if an invoice should be created for a purchase order.
     *
     * @param order the purchase order
     * @return true (always create for purchases)
     */
    @Override
    public boolean shouldCreateInvoice(PurchaseOrder order) {
        return true;
    }
}