package ru.galtor85.household_store.advice.exception;

public class FileReadException extends FileStorageException {

    public FileReadException(String message, String fileName, Long productId) {
        super(message, fileName, productId);
    }

    public FileReadException(String message, Throwable cause, String fileName, Long productId) {
        super(message, cause, fileName, productId);
    }
}