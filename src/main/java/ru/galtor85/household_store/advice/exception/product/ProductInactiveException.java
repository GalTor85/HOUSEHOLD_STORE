package ru.galtor85.household_store.advice.exception.product;

public class ProductInactiveException extends RuntimeException {
  private final Long productId;

  public ProductInactiveException(Long productId) {
    super();
    this.productId = productId;
  }

  public Long getProductId() {
    return productId;
  }
}