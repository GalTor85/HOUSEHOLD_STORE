package ru.galtor85.household_store.advice.exception.order;

import lombok.Getter;
import ru.galtor85.household_store.entity.order.OrderStatus;

@Getter
public class CannotReceivePurchaseOrderException extends RuntimeException {
    private final OrderStatus currentStatus;

    public CannotReceivePurchaseOrderException(OrderStatus currentStatus) {
        super();
        this.currentStatus = currentStatus;
    }

}