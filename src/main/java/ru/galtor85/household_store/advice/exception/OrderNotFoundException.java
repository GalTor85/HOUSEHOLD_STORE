package ru.galtor85.household_store.advice.exception;

public class OrderNotFoundException extends RuntimeException {
  private final Long orderId;

  public OrderNotFoundException(Long orderId) {
    super();
    this.orderId = orderId;
  }

  public Long getOrderId() {
    return orderId;
  }
}