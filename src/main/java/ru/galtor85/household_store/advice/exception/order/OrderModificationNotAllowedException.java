package ru.galtor85.household_store.advice.exception.order;

import lombok.Getter;
import ru.galtor85.household_store.entity.order.OrderStatus;

@Getter
public class OrderModificationNotAllowedException extends RuntimeException {
  private final OrderStatus currentStatus;

  public OrderModificationNotAllowedException(OrderStatus currentStatus) {
    super();
    this.currentStatus = currentStatus;
  }

}