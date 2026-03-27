package ru.galtor85.household_store.advice.exception.file;

public class FileDeleteException extends FileStorageException {

    public FileDeleteException(String message, String fileName, Long productId) {
        super(message, fileName, productId);
    }

    public FileDeleteException(String message, Throwable cause, String fileName, Long productId) {
        super(message, cause, fileName, productId);
    }
}