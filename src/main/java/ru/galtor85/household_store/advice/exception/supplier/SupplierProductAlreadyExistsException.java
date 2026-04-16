package ru.galtor85.household_store.advice.exception.supplier;

import lombok.Getter;

@Getter
public class SupplierProductAlreadyExistsException extends RuntimeException {
    private final Long productId;
    private final Long supplierId;

    public SupplierProductAlreadyExistsException(Long productId, Long supplierId) {
        super();
        this.productId = productId;
        this.supplierId = supplierId;
    }

}