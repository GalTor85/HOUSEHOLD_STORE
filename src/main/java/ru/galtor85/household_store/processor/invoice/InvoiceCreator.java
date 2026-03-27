package ru.galtor85.household_store.processor.invoice;

import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.PaymentMethod;

import java.time.LocalDateTime;

public interface InvoiceCreator<T> {

    /**
     * Создает счет для заказа
     */
    Invoice createInvoice(T order, Long createdBy);

    /**
     * Определяет способ оплаты для заказа
     */
    PaymentMethod determinePaymentMethod(T order);

    /**
     * Определяет срок оплаты
     */
    LocalDateTime calculateDueDate(T order);

    /**
     * Формирует описание счета
     */
    String getDescription(T order);

    /**
     * Формирует примечания к счету
     */
    String getNotes(T order);

    /**
     * Проверяет, нужно ли создавать счет автоматически
     */
    boolean shouldCreateInvoice(T order);
}