package ru.galtor85.household_store.advice.exception;

public class FileStorageException extends RuntimeException {

  private final String fileName;
  private final Long productId;

  public FileStorageException(String message, String fileName, Long productId) {
    super(message);
    this.fileName = fileName;
    this.productId = productId;
  }

  public FileStorageException(String message, Throwable cause, String fileName, Long productId) {
    super(message, cause);
    this.fileName = fileName;
    this.productId = productId;
  }

  public String getFileName() {
    return fileName;
  }

  public Long getProductId() {
    return productId;
  }
}