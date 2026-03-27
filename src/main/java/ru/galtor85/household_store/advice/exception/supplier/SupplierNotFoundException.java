package ru.galtor85.household_store.advice.exception.supplier;

public class SupplierNotFoundException extends RuntimeException {
    private final Long supplierId;

    public SupplierNotFoundException(Long supplierId) {
        super();
        this.supplierId = supplierId;
    }

    public SupplierNotFoundException() {
        super();
        this.supplierId = null;
    }

    public Long getSupplierId() {
        return supplierId;
    }
}