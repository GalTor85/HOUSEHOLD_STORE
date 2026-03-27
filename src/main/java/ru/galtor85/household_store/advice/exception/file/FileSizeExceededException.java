package ru.galtor85.household_store.advice.exception.file;

public class FileSizeExceededException extends FileStorageException {

    private final long maxSize;
    private final long actualSize;

    public FileSizeExceededException(String message, String fileName, Long productId,
                                     long maxSize, long actualSize) {
        super(message, fileName, productId);
        this.maxSize = maxSize;
        this.actualSize = actualSize;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public long getActualSize() {
        return actualSize;
    }
}