package ru.galtor85.household_store.processor.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.galtor85.household_store.constants.TechnicalConstants.DATE_FORMAT_PATTERN;

/**
 * Invoice creator for sales orders.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderInvoiceCreator implements InvoiceCreator<SalesOrder> {

    private final LogMessageService logMsg;
    private final MessageService messageService;
    private final FinancialConfig financialConfig;

    @Value("${app.invoice.sales.retail.due-days:7}")
    private int retailDueDays;

    @Value("${app.invoice.sales.wholesale.due-days:30}")
    private int wholesaleDueDays;

    /**
     * Creates an invoice for a sales order.
     *
     * @param order     the sales order
     * @param createdBy ID of the user creating the invoice
     * @return created Invoice entity
     */
    @Override
    public Invoice createInvoice(SalesOrder order, Long createdBy) {
        log.info(logMsg.get("invoice.creator.sales.start",
                order.getOrderNumber(), order.getTotalAmount()));

        LocalDateTime issueDate = LocalDateTime.now();
        LocalDateTime dueDate = calculateDueDate(order);

        return Invoice.builder()
                .salesOrderId(order.getId())
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
     * Determines the payment method for a sales order.
     *
     * @param order the sales order
     * @return PaymentMethod (BANK_TRANSFER for wholesale, CARD for retail)
     */
    @Override
    public PaymentMethod determinePaymentMethod(SalesOrder order) {
        if (order.isWholesale()) {
            return PaymentMethod.BANK_TRANSFER;
        }
        return PaymentMethod.CARD;
    }

    /**
     * Calculates the due date for a sales order invoice.
     *
     * @param order the sales order
     * @return due date (30 days for wholesale, 7 days for retail)
     */
    @Override
    public LocalDateTime calculateDueDate(SalesOrder order) {
        if (order.isWholesale()) {
            return LocalDateTime.now().plusDays(wholesaleDueDays);
        }
        return LocalDateTime.now().plusDays(retailDueDays);
    }

    /**
     * Builds the invoice description.
     *
     * @param order the sales order
     * @return localized description
     */
    @Override
    public String getDescription(SalesOrder order) {
        return messageService.get("invoice.description.sales", order.getOrderNumber());
    }

    /**
     * Builds the invoice notes.
     *
     * @param order the sales order
     * @return localized notes with dates
     */
    @Override
    public String getNotes(SalesOrder order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
        String orderType = order.getOrderType().getKey();
        return messageService.get("invoice.notes.sales." + orderType,
                order.getOrderNumber(),
                LocalDateTime.now().format(formatter),
                calculateDueDate(order).format(formatter));
    }

    /**
     * Determines if an invoice should be created for a sales order.
     *
     * @param order the sales order
     * @return true (always create for sales orders)
     */
    @Override
    public boolean shouldCreateInvoice(SalesOrder order) {
        return true;
    }
}