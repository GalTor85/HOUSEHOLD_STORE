package ru.galtor85.household_store.advice.exception;

public class InvalidStockOperationException extends RuntimeException {
    private final int currentStock;
    private final int requestedChange;

    public InvalidStockOperationException(int currentStock, int requestedChange) {
        super(); // Не передаем хардкодное сообщение
        this.currentStock = currentStock;
        this.requestedChange = requestedChange;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public int getRequestedChange() {
        return requestedChange;
    }
}