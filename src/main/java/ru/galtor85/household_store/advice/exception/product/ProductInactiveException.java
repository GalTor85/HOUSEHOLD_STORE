package ru.galtor85.household_store.advice.exception.product;

import lombok.Getter;

@Getter
public class ProductInactiveException extends RuntimeException {
  private final Long productId;

  public ProductInactiveException(Long productId) {
    super();
    this.productId = productId;
  }

}