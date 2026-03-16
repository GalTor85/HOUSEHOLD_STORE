package ru.galtor85.household_store.advice.exception;

public class ProductNotFromSupplierException extends RuntimeException {
    private final Long productId;
    private final Long supplierId;

    public ProductNotFromSupplierException(Long productId, Long supplierId) {
        super();
        this.productId = productId;
        this.supplierId = supplierId;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getSupplierId() {
        return supplierId;
    }
}