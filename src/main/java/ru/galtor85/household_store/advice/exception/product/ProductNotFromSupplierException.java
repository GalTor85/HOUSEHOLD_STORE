package ru.galtor85.household_store.advice.exception.product;

import lombok.Getter;

@Getter
public class ProductNotFromSupplierException extends RuntimeException {
    private final Long productId;
    private final Long supplierId;

    public ProductNotFromSupplierException(Long productId, Long supplierId) {
        super();
        this.productId = productId;
        this.supplierId = supplierId;
    }

}