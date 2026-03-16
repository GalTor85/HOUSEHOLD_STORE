package ru.galtor85.household_store.advice.exception;

public class SupplierNotFoundException extends RuntimeException {
    private final Long supplierId;

    public SupplierNotFoundException(Long supplierId) {
        super();
        this.supplierId = supplierId;
    }

    public Long getSupplierId() {
        return supplierId;
    }
}