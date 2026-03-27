package ru.galtor85.household_store.advice.exception.supplier;

import ru.galtor85.household_store.entity.supplier.SupplierStatus;

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