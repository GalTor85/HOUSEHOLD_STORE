package ru.galtor85.household_store.processor.invoice;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.generator.NumberGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Processor for automatic invoice creation when orders are created.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceAutoCreationProcessor {

    private final InvoiceRepository invoiceRepository;
    private final NumberGenerator numberGenerator;
    private final MessageService messageService;
    private final PurchaseOrderInvoiceCreator purchaseOrderInvoiceCreator;
    private final SalesOrderInvoiceCreator salesOrderInvoiceCreator;
    private final LogMessageService logMsg;

    private final Map<Class<?>, InvoiceCreator<?>> creators = new HashMap<>();

    @PostConstruct
    public void init() {
        registerCreator(PurchaseOrder.class, purchaseOrderInvoiceCreator);
        registerCreator(SalesOrder.class, salesOrderInvoiceCreator);
        log.info(logMsg.get("invoice.creator.initialized"));
    }

    /**
     * Registers an invoice creator for a specific order type.
     *
     * @param orderClass the order class
     * @param creator    the invoice creator
     */
    public void registerCreator(Class<?> orderClass, InvoiceCreator<?> creator) {
        creators.put(orderClass, creator);
        log.debug(logMsg.get("invoice.creator.registered", orderClass.getSimpleName()));
    }

    /**
     * Creates an invoice for an order.
     *
     * @param order     the order entity
     * @param createdBy ID of the user creating the invoice
     * @param <T>       the order type
     * @return created Invoice entity or null if not needed
     * @throws IllegalArgumentException if order is null or no creator registered
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> Invoice createInvoiceForOrder(T order, Long createdBy) {
        if (order == null) {
            throw new IllegalArgumentException(messageService.get("invoice.creator.order.null"));
        }

        InvoiceCreator<T> creator = (InvoiceCreator<T>) creators.get(order.getClass());
        if (creator == null) {
            throw new IllegalArgumentException(
                    messageService.get("invoice.creator.not.found", order.getClass().getSimpleName()));
        }

        if (!creator.shouldCreateInvoice(order)) {
            log.info(logMsg.get("invoice.creator.skip", order.getClass().getSimpleName()));
            return null;
        }

        // Check if invoice already exists
        if (hasExistingInvoice(order)) {
            log.warn(logMsg.get("invoice.creator.already.exists"));
            return getExistingInvoice(order);
        }

        Invoice invoice = creator.createInvoice(order, createdBy);
        invoice.setInvoiceNumber(numberGenerator.generateInvoiceNumber());

        Invoice saved = invoiceRepository.save(invoice);
        linkInvoiceToOrder(saved, order);

        log.info(logMsg.get("invoice.creator.created",
                saved.getInvoiceNumber(), order.getClass().getSimpleName()));

        return saved;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private boolean hasExistingInvoice(Object order) {
        if (order instanceof PurchaseOrder) {
            return !invoiceRepository.findByPurchaseOrderId(((PurchaseOrder) order).getId()).isEmpty();
        } else if (order instanceof SalesOrder) {
            return !invoiceRepository.findBySalesOrderId(((SalesOrder) order).getId()).isEmpty();
        }
        return false;
    }

    private Invoice getExistingInvoice(Object order) {
        if (order instanceof PurchaseOrder) {
            return invoiceRepository.findByPurchaseOrderId(((PurchaseOrder) order).getId()).getFirst();
        } else if (order instanceof SalesOrder) {
            return invoiceRepository.findBySalesOrderId(((SalesOrder) order).getId()).getFirst();
        }
        return null;
    }

    private void linkInvoiceToOrder(Invoice invoice, Object order) {
        if (order instanceof PurchaseOrder purchaseOrder) {
            purchaseOrder.addInvoice(invoice);
        } else if (order instanceof SalesOrder salesOrder) {
            salesOrder.addInvoice(invoice);
        }
    }
}