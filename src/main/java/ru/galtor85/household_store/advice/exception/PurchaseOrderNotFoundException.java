package ru.galtor85.household_store.advice.exception;

import lombok.Getter;

@Getter
public class PurchaseOrderNotFoundException extends RuntimeException {

    private final Long purchaseOrderId;
    private final String orderNumber;

    public PurchaseOrderNotFoundException(Long purchaseOrderId) {
        super();
        this.purchaseOrderId = purchaseOrderId;
        this.orderNumber = null;
    }

    public PurchaseOrderNotFoundException(String orderNumber) {
        super();
        this.orderNumber = orderNumber;
        this.purchaseOrderId = null;
    }

    public PurchaseOrderNotFoundException(Long purchaseOrderId, String message) {
        super(message);
        this.purchaseOrderId = purchaseOrderId;
        this.orderNumber = null;
    }
}