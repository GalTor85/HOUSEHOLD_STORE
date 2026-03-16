package ru.galtor85.household_store.advice.exception;

public class SupplierProductNotFoundException extends RuntimeException {
    private final Long supplierProductId;

    public SupplierProductNotFoundException(Long supplierProductId) {
        super();
        this.supplierProductId = supplierProductId;
    }

    public Long getSupplierProductId() {
        return supplierProductId;
    }
}