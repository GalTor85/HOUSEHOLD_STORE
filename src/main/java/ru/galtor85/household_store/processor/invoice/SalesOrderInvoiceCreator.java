package ru.galtor85.household_store.processor.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderInvoiceCreator implements InvoiceCreator<SalesOrder> {

    private final SalesOrderRepository salesOrderRepository;
    private final MessageService messageService;

    @Override
    public Invoice createInvoice(SalesOrder order, Long createdBy) {
        log.info(messageService.get("invoice.creator.sales.start",
                order.getOrderNumber(), order.getTotalAmount()));

        LocalDateTime issueDate = LocalDateTime.now();
        LocalDateTime dueDate = calculateDueDate(order);

        return Invoice.builder()
                .salesOrderId(order.getId())
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
    public PaymentMethod determinePaymentMethod(SalesOrder order) {
        if (order.isWholesale()) {
            return PaymentMethod.BANK_TRANSFER;
        }
        return PaymentMethod.CARD;
    }

    @Override
    public LocalDateTime calculateDueDate(SalesOrder order) {
        if (order.isWholesale()) {
            // Оптовые заказы - 30 дней на оплату
            return LocalDateTime.now().plusDays(30);
        }
        // Розничные заказы - 7 дней на оплату
        return LocalDateTime.now().plusDays(7);
    }

    @Override
    public String getDescription(SalesOrder order) {
        return messageService.get("invoice.description.sales", order.getOrderNumber());
    }

    @Override
    public String getNotes(SalesOrder order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String orderType = order.isWholesale() ? "wholesale" : "retail";
        return messageService.get("invoice.notes.sales." + orderType,
                order.getOrderNumber(),
                LocalDateTime.now().format(formatter),
                calculateDueDate(order).format(formatter));
    }

    @Override
    public boolean shouldCreateInvoice(SalesOrder order) {
        // Для продаж создаем счет только для оптовых заказов
        // Розничные заказы оплачиваются сразу
        return order.isWholesale();
    }
}