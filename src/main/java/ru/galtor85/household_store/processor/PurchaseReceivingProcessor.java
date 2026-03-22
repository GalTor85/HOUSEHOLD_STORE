package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.StockMovementBuilder;
import ru.galtor85.household_store.dto.ReceiveAndStockRequest;
import ru.galtor85.household_store.dto.ReceiveStockItem;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.StockMovementRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.util.BatchNumberGenerator;
import ru.galtor85.household_store.util.EntityFinder;
import ru.galtor85.household_store.validator.WarehouseValidator;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseReceivingProcessor {

    private final EntityFinder entityFinder;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementBuilder movementBuilder;
    private final BatchNumberGenerator batchNumberGenerator;
    private final MessageService messageService;
    private final WarehouseValidator warehouseValidator;

    @Transactional
    public ReceivingResult processReceiving(Order order, PurchaseOrder purchaseOrder,
                                            ReceiveAndStockRequest request, Long managerId) {

        log.info(messageService.get("purchase.receiving.processor.start",
                order.getOrderNumber(), request.getItems().size(), managerId));

        List<StockMovement> movements = new ArrayList<>();
        List<OrderItem> partiallyReceived = new ArrayList<>();
        List<Long> missingProducts = new ArrayList<>();

        warehouseValidator.validateWarehouseExists(request.getWarehouseId());

        for (ReceiveStockItem item : request.getItems()) {
            OrderItem orderItem = entityFinder.findOrderItem(order, item.getProductId());

            if (orderItem == null) {
                log.warn(messageService.get("purchase.receiving.processor.product.not.found",
                        item.getProductId(), order.getId()));
                missingProducts.add(item.getProductId());
                continue;
            }

            Product product = entityFinder.findProductById(orderItem.getProductId());

            // Проверка на частичную приемку
            if (item.getQuantity() < orderItem.getQuantity()) {
                log.debug(messageService.get("purchase.receiving.processor.partial.receipt",
                        product.getSku(), item.getQuantity(), orderItem.getQuantity()));
                partiallyReceived.add(orderItem);
            }

            // Обновляем остаток
            int oldQuantity = product.getQuantityInStock();
            int newQuantity = oldQuantity + item.getQuantity();
            product.setQuantityInStock(newQuantity);
            productRepository.save(product);

            log.debug(messageService.get("purchase.receiving.processor.stock.updated",
                    product.getSku(), oldQuantity, newQuantity));

            // Создаем движение
            StockMovement movement = createStockMovement(
                    product, orderItem, order, request.getWarehouseId(), managerId,
                    item.getBatchNumber()
            );
            movements.add(stockMovementRepository.save(movement));
        }

        boolean isFullyReceived = isFullyReceived(order, request.getItems());
        List<OrderItem> unreceivedItems = getUnreceivedItems(order, request.getItems());

        log.info(messageService.get("purchase.receiving.processor.complete",
                order.getOrderNumber(), movements.size(), isFullyReceived));

        return ReceivingResult.builder()
                .movements(movements)
                .isFullyReceived(isFullyReceived)
                .unreceivedItems(unreceivedItems)
                .partiallyReceived(partiallyReceived)
                .missingProducts(missingProducts)
                .build();
    }

    private StockMovement createStockMovement(Product product, OrderItem item,
                                              Order order, Long warehouseId,
                                              Long performedBy, String batchNumber) {

        if (batchNumber == null || batchNumber.isEmpty()) {
            batchNumber = batchNumberGenerator.generateBatchNumber();
            log.debug(messageService.get("purchase.receiving.processor.batch.generated",
                    batchNumber));
        }

        String notes = messageService.get("purchase.receiving.processor.movement.notes",
                order.getOrderNumber());

        return movementBuilder.buildFullMovement(
                product.getId(), null, null, warehouseId, item.getQuantity(),
                MovementType.RECEIPT, "PURCHASE", order.getId(), order.getOrderNumber(),
                performedBy, notes, batchNumber, order.getOrderNumber()
        );
    }

    private boolean isFullyReceived(Order order, List<ReceiveStockItem> receivedItems) {
        for (OrderItem orderItem : order.getItems()) {
            ReceiveStockItem received = receivedItems.stream()
                    .filter(item -> item.getProductId().equals(orderItem.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (received == null) {
                log.debug(messageService.get("purchase.receiving.processor.missing.product",
                        orderItem.getProductId()));
                return false;
            }

            if (received.getQuantity() < orderItem.getQuantity()) {
                log.debug(messageService.get("purchase.receiving.processor.quantity.mismatch",
                        orderItem.getProductId(), received.getQuantity(), orderItem.getQuantity()));
                return false;
            }
        }
        return true;
    }

    private List<OrderItem> getUnreceivedItems(Order order, List<ReceiveStockItem> receivedItems) {
        List<OrderItem> unreceived = new ArrayList<>();

        for (OrderItem orderItem : order.getItems()) {
            ReceiveStockItem received = receivedItems.stream()
                    .filter(item -> item.getProductId().equals(orderItem.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (received == null) {
                log.debug(messageService.get("purchase.receiving.processor.item.not.received",
                        orderItem.getProductId()));
                unreceived.add(orderItem);
            } else if (received.getQuantity() < orderItem.getQuantity()) {
                int remaining = orderItem.getQuantity() - received.getQuantity();
                log.debug(messageService.get("purchase.receiving.processor.partial.remaining",
                        orderItem.getProductId(), remaining));

                OrderItem remainingItem = new OrderItem();
                remainingItem.setProductId(orderItem.getProductId());
                remainingItem.setQuantity(remaining);
                remainingItem.setPrice(orderItem.getPrice());
                remainingItem.setProductName(orderItem.getProductName());
                remainingItem.setProductSku(orderItem.getProductSku());
                unreceived.add(remainingItem);
            }
        }

        return unreceived;
    }

    @lombok.Value
    @lombok.Builder
    public static class ReceivingResult {
        List<StockMovement> movements;
        boolean isFullyReceived;
        List<OrderItem> unreceivedItems;
        List<OrderItem> partiallyReceived;
        List<Long> missingProducts;
    }
}