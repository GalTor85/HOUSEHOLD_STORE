package ru.galtor85.household_store.advice.exception.stock;

import lombok.Getter;

import java.util.List;

@Getter
public class BulkOperationException extends RuntimeException {
    private final List<Long> productIds;
    private final int successfulCount;

    public BulkOperationException(List<Long> productIds, int successfulCount) {
        super();
        this.productIds = productIds;
        this.successfulCount = successfulCount;
    }

}