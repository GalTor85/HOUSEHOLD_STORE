package ru.galtor85.household_store.entity.warehouse;

public enum CellType {
    STANDARD,    // Обычная ячейка
    PALLET,      // Паллетное место
    FRIDGE,      // Холодильная камера
    FREEZER,     // Морозильная камера
    DANGEROUS,   // Для опасных грузов
    BULK,        // Навалочный груз
    LIQUID,      // Для жидкостей
    OVERSIZE     // Для негабаритных грузов
}