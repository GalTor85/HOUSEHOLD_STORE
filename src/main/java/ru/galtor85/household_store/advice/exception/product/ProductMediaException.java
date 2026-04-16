package ru.galtor85.household_store.advice.exception.product;

import lombok.Getter;

@Getter
public class ProductMediaException extends RuntimeException {

  private final Long productId;
  private final String fileName;

  public ProductMediaException(String message, Long productId, String fileName) {
    super(message);
    this.productId = productId;
    this.fileName = fileName;
  }

  public ProductMediaException(String message, Throwable cause, Long productId, String fileName) {
    super(message, cause);
    this.productId = productId;
    this.fileName = fileName;
  }

}