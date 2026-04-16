package ru.galtor85.household_store.advice.exception.supplier;

import lombok.Getter;

@Getter
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

}