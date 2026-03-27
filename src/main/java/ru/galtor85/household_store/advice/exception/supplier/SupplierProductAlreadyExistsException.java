package ru.galtor85.household_store.advice.exception.supplier;

public class SupplierProductAlreadyExistsException extends RuntimeException {
    private final Long productId;
    private final Long supplierId;

    public SupplierProductAlreadyExistsException(Long productId, Long supplierId) {
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