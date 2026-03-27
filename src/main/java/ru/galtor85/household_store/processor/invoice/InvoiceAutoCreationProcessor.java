package ru.galtor85.household_store.processor.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.generator.NumberGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceAutoCreationProcessor {

    private final InvoiceRepository invoiceRepository;
    private final NumberGenerator numberGenerator;
    private final MessageService messageService;

    private final Map<Class<?>, InvoiceCreator<?>> creators = new HashMap<>();

    /**
     * Регистрирует создателей счетов для разных типов заказов
     */
    public void registerCreator(Class<?> orderClass, InvoiceCreator<?> creator) {
        creators.put(orderClass, creator);
        log.debug(messageService.get("invoice.creator.registered", orderClass.getSimpleName()));
    }

    /**
     * Создает счет для заказа (общий метод)
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> Invoice createInvoiceForOrder(T order, Long createdBy) {
        if (order == null) {
            throw new IllegalArgumentException(
                    messageService.get("invoice.creator.order.null"));
        }

        InvoiceCreator<T> creator = (InvoiceCreator<T>) creators.get(order.getClass());
        if (creator == null) {
            throw new IllegalArgumentException(
                    messageService.get("invoice.creator.not.found", order.getClass().getSimpleName()));
        }

        if (!creator.shouldCreateInvoice(order)) {
            log.info(messageService.get("invoice.creator.skip",
                    order.getClass().getSimpleName()));
            return null;
        }

        // Проверяем, не создан ли уже счет
        if (hasExistingInvoice(order)) {
            log.warn(messageService.get("invoice.creator.already.exists"));
            return getExistingInvoice(order);
        }

        Invoice invoice = creator.createInvoice(order, createdBy);
        invoice.setInvoiceNumber(numberGenerator.generateInvoiceNumber());

        Invoice saved = invoiceRepository.save(invoice);
        linkInvoiceToOrder(saved, order);

        log.info(messageService.get("invoice.creator.created",
                saved.getInvoiceNumber(), order.getClass().getSimpleName()));

        return saved;
    }

    /**
     * Обновляет сумму счета при изменении заказа
     */
    @Transactional
    public Invoice updateInvoiceAmount(Invoice invoice, BigDecimal newAmount, String reason, Long updatedBy) {
        log.info(messageService.get("invoice.creator.update.start",
                invoice.getInvoiceNumber(), invoice.getAmount(), newAmount));

        BigDecimal oldAmount = invoice.getAmount();
        invoice.setAmount(newAmount);
        invoice.setUpdatedAt(LocalDateTime.now());

        String updateNote = messageService.get("invoice.creator.update.note",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                oldAmount, newAmount, reason, updatedBy);

        if (invoice.getNotes() == null) {
            invoice.setNotes(updateNote);
        } else {
            invoice.setNotes(invoice.getNotes() + "\n" + updateNote);
        }

        Invoice updated = invoiceRepository.save(invoice);

        log.info(messageService.get("invoice.creator.updated",
                updated.getInvoiceNumber(), oldAmount, newAmount));

        return updated;
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
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
            return invoiceRepository.findByPurchaseOrderId(((PurchaseOrder) order).getId()).get(0);
        } else if (order instanceof SalesOrder) {
            return invoiceRepository.findBySalesOrderId(((SalesOrder) order).getId()).get(0);
        }
        return null;
    }

    private void linkInvoiceToOrder(Invoice invoice, Object order) {
        if (order instanceof PurchaseOrder) {
            PurchaseOrder purchaseOrder = (PurchaseOrder) order;
            purchaseOrder.addInvoice(invoice);
        } else if (order instanceof SalesOrder) {
            SalesOrder salesOrder = (SalesOrder) order;
            salesOrder.addInvoice(invoice);
        }
    }
}