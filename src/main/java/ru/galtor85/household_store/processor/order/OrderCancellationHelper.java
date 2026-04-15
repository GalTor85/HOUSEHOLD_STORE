package ru.galtor85.household_store.processor.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Helper for order cancellation operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancellationHelper {

    private final LogMessageService logMsg;

    /**
     * Cancels a purchase order and its pending invoices.
     *
     * @param order  the purchase order
     * @param reason cancellation reason
     */
    public void cancelPurchaseOrder(PurchaseOrder order, String reason) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);
        cancelPendingInvoices(order.getInvoices());

        log.debug(logMsg.get("order.cancellation.purchase.invoices.cancelled", order.getId()));
    }

    /**
     * Cancels a sales order and its pending invoices.
     *
     * @param order  the sales order
     * @param reason cancellation reason
     */
    public void cancelSalesOrder(SalesOrder order, String reason) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);
        cancelPendingInvoices(order.getInvoices());

        log.debug(logMsg.get("order.cancellation.sales.invoices.cancelled", order.getId()));
    }

    private void cancelPendingInvoices(List<Invoice> invoices) {
        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.PENDING) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
            }
        }
    }
}