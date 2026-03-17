package ru.galtor85.household_store.builder;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.ReceiveOrderRequest;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.PurchaseOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class PurchaseOrderBuilder {

    public PurchaseOrder buildFromOrder(Order order, PurchaseOrderCreateRequest request) {
        return PurchaseOrder.builder()
                .order(order)
                .expectedDelivery(request.getExpectedDelivery())
                .warehouseLocation(request.getWarehouseLocation())
                .invoiceNumber(request.getInvoiceNumber())
                .paymentDue(request.getPaymentDue())
                .paymentStatus("PENDING")
                .build();
    }

    public void updateForReceiving(PurchaseOrder purchaseOrder,
                                   ReceiveOrderRequest request, Long managerId) {
        purchaseOrder.setActualDelivery(LocalDate.from(request.getReceivedAt() != null ?
                request.getReceivedAt() : LocalDateTime.now()));
        purchaseOrder.setReceivedBy(managerId);
        purchaseOrder.setQualityCheck(request.getQualityCheck());
        purchaseOrder.setPaymentStatus(request.getPaymentStatus() != null ?
                request.getPaymentStatus() : purchaseOrder.getPaymentStatus());
    }
}