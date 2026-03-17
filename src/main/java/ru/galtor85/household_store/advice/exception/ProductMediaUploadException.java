package ru.galtor85.household_store.advice.exception;

import java.util.List;

public class ProductMediaUploadException extends RuntimeException {

    private final Long productId;
    private final List<String> failedFiles;

    public ProductMediaUploadException(String message, Long productId, List<String> failedFiles) {
        super(message);
        this.productId = productId;
        this.failedFiles = failedFiles;
    }

    public Long getProductId() {
        return productId;
    }

    public List<String> getFailedFiles() {
        return failedFiles;
    }
}