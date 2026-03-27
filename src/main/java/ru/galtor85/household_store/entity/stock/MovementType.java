package ru.galtor85.household_store.entity.stock;

public enum MovementType {
    RECEIPT,        // Поступление
    SHIPMENT,       // Отгрузка
    TRANSFER,       // Перемещение между ячейками
    WRITE_OFF,      // Списание
    INVENTORY,      // Инвентаризация
    RETURN          // Возврат
}