package ru.galtor85.household_store.builder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.OrderType;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.processor.PurchaseStockProcessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderUpdateBuilder {

    private final MessageService messageService;
    private final PurchaseStockProcessor purchaseStockProcessor; // Вместо ManagerOrderService

    public void updateOrderStatus(Order order, OrderStatus newStatus,
                                  String trackingNumber, String reason,
                                  Long managerId, Long warehouseId) { // Добавлены параметры
        order.setStatus(newStatus);

        if (order.getOrderType() == OrderType.PURCHASE) {
            handlePurchaseStatusActions(order, newStatus, trackingNumber, reason, managerId, warehouseId);
        } else {
            handleCustomerStatusActions(order, newStatus, trackingNumber, reason);
        }
    }

    // Перегруженный метод для обратной совместимости (без managerId и warehouseId)
    public void updateOrderStatus(Order order, OrderStatus newStatus,
                                  String trackingNumber, String reason) {
        updateOrderStatus(order, newStatus, trackingNumber, reason, null, null);
    }

    private void handlePurchaseStatusActions(Order order, OrderStatus newStatus,
                                             String trackingNumber, String reason,
                                             Long managerId, Long warehouseId) {

        switch (newStatus) {
            case SHIPPED:
                if (trackingNumber != null) {
                    order.setTrackingNumber(trackingNumber);
                }
                break;

            case PARTIALLY_RECEIVED:
                // Логика частичной приемки
                order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
                log.info("Partially received order: {}", order.getId());
                break;

            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                // Обновляем остатки на складе через процессор
                if (managerId != null) {
                    purchaseStockProcessor.processStockUpdate(order, warehouseId, managerId);
                } else {
                    log.warn("Cannot update stock: managerId is null for order {}", order.getId());
                }
                break;

            case CANCELLED:
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(reason);
                // Отмена платежа поставщику
                cancelPaymentToSupplier(order);
                break;

            default:
                log.debug("No specific actions for status: {}", newStatus);
        }
    }

    private void handleCustomerStatusActions(Order order, OrderStatus newStatus,
                                             String trackingNumber, String reason) {
        switch (newStatus) {
            case SHIPPED:
                if (trackingNumber != null) {
                    order.setTrackingNumber(trackingNumber);
                }
                break;

            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                break;

            case CANCELLED:
            case REFUNDED:
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(reason);
                // Возврат денег клиенту
                restoreStockForCancelledOrder(order);
                break;
        }
    }

    public String formatOrderNote(String note, Long managerId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return timestamp + " - " +
                messageService.get("manager.order.note.manager.prefix", managerId) +
                ": " + note;
    }

    // Вспомогательные методы
    private void cancelPaymentToSupplier(Order order) {
        log.info("Cancelling payment to supplier for order: {}", order.getId());
        // TODO: Реализовать логику отмены платежа поставщику
    }

    private void restoreStockForCancelledOrder(Order order) {
        log.info("Restoring stock for cancelled order: {}", order.getId());
        // TODO: Реализовать логику восстановления товаров на складе
        for (var item : order.getItems()) {
            log.debug("Restoring {} units of product {}", item.getQuantity(), item.getProductId());
        }
    }
}