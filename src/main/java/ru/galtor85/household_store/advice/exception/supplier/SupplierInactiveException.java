package ru.galtor85.household_store.advice.exception.supplier;

import lombok.Getter;
import ru.galtor85.household_store.entity.supplier.SupplierStatus;

@Getter
public class SupplierInactiveException extends RuntimeException {
    private final SupplierStatus currentStatus;

    public SupplierInactiveException(SupplierStatus currentStatus) {
        super();
        this.currentStatus = currentStatus;
    }

}