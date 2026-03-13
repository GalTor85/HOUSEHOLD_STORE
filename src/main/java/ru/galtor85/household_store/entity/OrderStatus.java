package ru.galtor85.household_store.entity;

public enum OrderStatus {
    PENDING,        // Ожидает оплаты
    PAID,           // Оплачен
    PROCESSING,     // В обработке
    SHIPPED,        // Отправлен
    DELIVERED,      // Доставлен
    COMPLETED,      // Завершен
    CANCELLED,      // Отменен
    REFUNDED        // Возвращен
}