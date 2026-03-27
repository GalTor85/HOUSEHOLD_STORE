package ru.galtor85.household_store.advice.exception.order;

import ru.galtor85.household_store.entity.order.OrderStatus;

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