package ru.galtor85.household_store.builder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.service.MessageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class OrderUpdateBuilder {

    private final MessageService messageService;

    public void updateOrderStatus(Order order, OrderStatus newStatus,
                                  String trackingNumber, String reason) {
        order.setStatus(newStatus);

        switch (newStatus) {
            case SHIPPED:
                if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
                    order.setTrackingNumber(trackingNumber);
                }
                break;
            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                break;
            case CANCELLED:
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(reason);
                break;
            default:
                break;
        }
    }

    public String formatOrderNote(String note, Long managerId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return timestamp + " - " +
                messageService.get("manager.order.note.manager.prefix", managerId) +
                ": " + note;
    }
}