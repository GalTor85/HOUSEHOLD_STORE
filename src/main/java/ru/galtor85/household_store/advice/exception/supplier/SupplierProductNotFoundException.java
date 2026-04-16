package ru.galtor85.household_store.advice.exception.supplier;

import lombok.Getter;

@Getter
public class SupplierProductNotFoundException extends RuntimeException {
    private final Long supplierProductId;

    public SupplierProductNotFoundException(Long supplierProductId) {
        super();
        this.supplierProductId = supplierProductId;
    }

}