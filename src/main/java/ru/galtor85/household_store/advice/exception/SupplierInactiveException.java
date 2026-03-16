package ru.galtor85.household_store.advice.exception;

import ru.galtor85.household_store.entity.SupplierStatus;

public class SupplierInactiveException extends RuntimeException {
    private final SupplierStatus currentStatus;

    public SupplierInactiveException(SupplierStatus currentStatus) {
        super();
        this.currentStatus = currentStatus;
    }

    public SupplierStatus getCurrentStatus() {
        return currentStatus;
    }
}