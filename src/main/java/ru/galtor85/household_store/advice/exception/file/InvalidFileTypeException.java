package ru.galtor85.household_store.advice.exception.file;

import lombok.Getter;

@Getter
public class InvalidFileTypeException extends FileStorageException {

    private final String contentType;
    private final String allowedTypes;

    public InvalidFileTypeException(String message, String fileName, Long productId,
                                    String contentType, String allowedTypes) {
        super(message, fileName, productId);
        this.contentType = contentType;
        this.allowedTypes = allowedTypes;
    }

}