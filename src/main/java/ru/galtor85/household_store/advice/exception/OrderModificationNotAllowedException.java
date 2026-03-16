package ru.galtor85.household_store.advice.exception;

import ru.galtor85.household_store.entity.OrderStatus;

public class OrderModificationNotAllowedException extends RuntimeException {
  private final OrderStatus currentStatus;

  public OrderModificationNotAllowedException(OrderStatus currentStatus) {
    super();
    this.currentStatus = currentStatus;
  }

  public OrderStatus getCurrentStatus() {
    return currentStatus;
  }
}