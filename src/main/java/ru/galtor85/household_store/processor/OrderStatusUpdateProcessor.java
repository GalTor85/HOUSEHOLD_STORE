package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.ReceiveAndStockRequest;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderItem;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.PurchaseOrder;
import ru.galtor85.household_store.service.MessageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusUpdateProcessor {

    private final MessageService messageService;

    public void updateAfterReceiving(Order order, PurchaseOrder purchaseOrder,
                                     PurchaseReceivingProcessor.ReceivingResult result,
                                     ReceiveAndStockRequest request, Long managerId) {

        log.info(messageService.get("order.status.update.processor.start",
                order.getOrderNumber(), managerId));

        // 1. Обновляем статус заказа
        if (result.isFullyReceived()) {
            order.setStatus(OrderStatus.DELIVERED);
            log.info(messageService.get("order.status.update.processor.fully.received",
                    order.getId()));
        } else {
            order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
            log.info(messageService.get("order.status.update.processor.partially.received",
                    order.getId()));

            if (!result.getUnreceivedItems().isEmpty()) {
                log.warn(messageService.get("order.status.update.processor.unreceived.count",
                        result.getUnreceivedItems().size(), order.getId()));

                // Логируем детали по непринятым товарам
                for (OrderItem item : result.getUnreceivedItems()) {
                    log.debug(messageService.get("order.status.update.processor.unreceived.details",
                            item.getProductId(), item.getQuantity()));
                }
            }

            if (!result.getMissingProducts().isEmpty()) {
                log.warn(messageService.get("order.status.update.processor.missing.products",
                        result.getMissingProducts().size(), order.getId()));
            }

            if (!result.getPartiallyReceived().isEmpty()) {
                log.info(messageService.get("order.status.update.processor.partial.count",
                        result.getPartiallyReceived().size(), order.getId()));
            }
        }

        // 2. Устанавливаем дату приемки
        LocalDateTime now = LocalDateTime.now();
        order.setDeliveredAt(now);
        log.debug(messageService.get("order.status.update.processor.delivered.at",
                order.getId(), now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        // 3. Обновляем информацию в purchaseOrder
        purchaseOrder.setActualDelivery(LocalDate.now());
        purchaseOrder.setReceivedBy(managerId);
        purchaseOrder.setQualityCheck(request.getQualityCheck());

        if (request.getPaymentStatus() != null) {
            purchaseOrder.setPaymentStatus(request.getPaymentStatus());
            log.debug(messageService.get("order.status.update.processor.payment.status",
                    request.getPaymentStatus()));
        }

        log.info(messageService.get("order.status.update.processor.quality.check",
                request.getQualityCheck() != null ?
                        (request.getQualityCheck() ? "PASSED" : "FAILED") : "NOT SPECIFIED"));

        // 4. Добавляем заметку о приемке
        addReceivingNote(order, request, managerId, result);

        log.info(messageService.get("order.status.update.processor.complete",
                order.getOrderNumber()));
    }

    private void addReceivingNote(Order order, ReceiveAndStockRequest request, Long managerId,
                                  PurchaseReceivingProcessor.ReceivingResult result) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        StringBuilder note = new StringBuilder();
        note.append(messageService.get("order.status.update.processor.note.header",
                timestamp, managerId));

        // Информация о полноте приемки
        if (result.isFullyReceived()) {
            note.append(messageService.get("order.status.update.processor.note.fully.received"));
        } else {
            note.append(messageService.get("order.status.update.processor.note.partially.received"));

            if (!result.getUnreceivedItems().isEmpty()) {
                note.append(messageService.get("order.status.update.processor.note.unreceived.count",
                        result.getUnreceivedItems().size()));
            }
        }

        // Результат проверки качества
        if (request.getQualityCheck() != null) {
            note.append(request.getQualityCheck() ?
                    messageService.get("order.status.update.processor.note.quality.passed") :
                    messageService.get("order.status.update.processor.note.quality.failed"));
        }

        // Дополнительные заметки
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            note.append(messageService.get("order.status.update.processor.note.notes",
                    request.getNotes()));
        }

        String currentNotes = order.getNotes();
        if (currentNotes == null || currentNotes.isEmpty()) {
            order.setNotes(note.toString());
            log.debug(messageService.get("order.status.update.processor.note.added"));
        } else {
            order.setNotes(currentNotes + "\n" + note.toString());
            log.debug(messageService.get("order.status.update.processor.note.appended"));
        }
    }
}