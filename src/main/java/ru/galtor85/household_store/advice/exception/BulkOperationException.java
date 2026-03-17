package ru.galtor85.household_store.advice.exception;

import java.util.List;

public class BulkOperationException extends RuntimeException {
    private final List<Long> productIds;
    private final int successfulCount;

    public BulkOperationException(List<Long> productIds, int successfulCount) {
        super();
        this.productIds = productIds;
        this.successfulCount = successfulCount;
    }

    public List<Long> getProductIds() {
        return productIds;
    }

    public int getSuccessfulCount() {
        return successfulCount;
    }
}