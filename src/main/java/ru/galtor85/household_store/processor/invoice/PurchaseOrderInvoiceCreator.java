package ru.galtor85.household_store.processor.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderInvoiceCreator implements InvoiceCreator<PurchaseOrder> {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MessageService messageService;

    @Override
    public Invoice createInvoice(PurchaseOrder order, Long createdBy) {
        log.info(messageService.get("invoice.creator.purchase.start",
                order.getOrderNumber(), order.getTotalAmount()));

        LocalDateTime issueDate = LocalDateTime.now();
        LocalDateTime dueDate = calculateDueDate(order);

        return Invoice.builder()
                .purchaseOrderId(order.getId())
                .amount(order.getTotalAmount())
                .currency("RUB")
                .status(InvoiceStatus.PENDING)
                .paymentMethod(determinePaymentMethod(order))
                .issueDate(issueDate)
                .dueDate(dueDate)
                .description(getDescription(order))
                .notes(getNotes(order))
                .createdBy(createdBy)
                .build();
    }

    @Override
    public PaymentMethod determinePaymentMethod(PurchaseOrder order) {
        return PaymentMethod.BANK_TRANSFER;
    }

    @Override
    public LocalDateTime calculateDueDate(PurchaseOrder order) {
        // Для закупок - 30 дней на оплату
        return LocalDateTime.now().plusDays(30);
    }

    @Override
    public String getDescription(PurchaseOrder order) {
        return messageService.get("invoice.description.purchase", order.getOrderNumber());
    }

    @Override
    public String getNotes(PurchaseOrder order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return messageService.get("invoice.notes.purchase",
                order.getOrderNumber(),
                LocalDateTime.now().format(formatter),
                calculateDueDate(order).format(formatter));
    }

    @Override
    public boolean shouldCreateInvoice(PurchaseOrder order) {
        // Для закупок всегда создаем счет
        return true;
    }
}