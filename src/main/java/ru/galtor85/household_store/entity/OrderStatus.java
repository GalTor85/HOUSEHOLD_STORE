package ru.galtor85.household_store.entity;

public enum OrderStatus {
    // Общие статусы
    PENDING,        // Ожидание
    PAID,           // Оплачен
    PROCESSING,     // В обработке
    DELIVERED,      // Доставлен
    COMPLETED,      // Завершен
    CANCELLED,      // Отменен

    // Статусы для заказов покупателей
    SHIPPED,        // Отгружен
    REFUNDED,       // Возврат денег
    RETURNED,       // Товар возвращен

    // Статусы для закупок
    PARTIALLY_RECEIVED  // Частично получен
}