package ru.galtor85.household_store.processor.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.builder.stock.StockMovementBuilder;
import ru.galtor85.household_store.dto.request.order.ReceiveAndStockRequest;
import ru.galtor85.household_store.dto.common.ReceiveStockItem;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.stock.StockMovementRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.batch.BatchNumberGenerator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseReceivingProcessor {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementBuilder movementBuilder;
    private final BatchNumberGenerator batchNumberGenerator;
    private final MessageService messageService;

    /**
     * Обрабатывает приемку заказа на закупку
     */
    @Transactional
    public ReceivingResult processReceiving(PurchaseOrder order,
                                            ReceiveAndStockRequest request,
                                            Long managerId) {

        log.info(messageService.get("purchase.receiving.processor.start",
                order.getOrderNumber(), request.getItems().size(), managerId));

        List<StockMovement> movements = new ArrayList<>();
        List<PurchaseOrderItem> partiallyReceived = new ArrayList<>();
        List<Long> missingProducts = new ArrayList<>();

        for (ReceiveStockItem item : request.getItems()) {
            // Ищем позицию в заказе
            PurchaseOrderItem orderItem = order.getItems().stream()
                    .filter(oi -> oi.getProductId().equals(item.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (orderItem == null) {
                log.warn(messageService.get("purchase.receiving.processor.product.not.found",
                        item.getProductId(), order.getId()));
                missingProducts.add(item.getProductId());
                continue;
            }

            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(orderItem.getProductId()));

            // Получаем уже принятое количество
            int alreadyReceived = orderItem.getReceivedQuantity() != null ?
                    orderItem.getReceivedQuantity() : 0;
            int orderedQuantity = orderItem.getQuantity();
            int remainingToReceive = orderedQuantity - alreadyReceived;

            int receivingQuantity = item.getQuantity();

            // Проверяем, не превышает ли принимаемое количество остаток
            if (receivingQuantity > remainingToReceive) {
                log.warn(messageService.get("purchase.receiving.processor.quantity.exceeds.remaining",
                        product.getSku(), receivingQuantity, remainingToReceive));
                receivingQuantity = remainingToReceive;
            }

            if (receivingQuantity <= 0) {
                log.info(messageService.get("purchase.receiving.processor.already.received",
                        product.getSku()));
                continue;
            }

            // Проверка на частичную приемку
            boolean isPartial = (alreadyReceived + receivingQuantity) < orderedQuantity;
            if (isPartial) {
                log.debug(messageService.get("purchase.receiving.processor.partial.receipt",
                        product.getSku(), alreadyReceived + receivingQuantity, orderedQuantity));
                partiallyReceived.add(orderItem);
            }

            // Обновляем количество принятого в позиции заказа
            orderItem.setReceivedQuantity(alreadyReceived + receivingQuantity);

            // Обновляем остаток в Product
            int oldQuantity = product.getQuantityInStock();
            int newQuantity = oldQuantity + receivingQuantity;
            product.setQuantityInStock(newQuantity);
            productRepository.save(product);

            log.debug(messageService.get("purchase.receiving.processor.stock.updated",
                    product.getSku(), oldQuantity, newQuantity));

            // Обновляем product_stocks
            updateProductStock(product, receivingQuantity, request.getWarehouseId());

            // Создаем движение
            StockMovement movement = createStockMovement(
                    product, orderItem, order, request.getWarehouseId(), managerId,
                    item.getBatchNumber(), receivingQuantity
            );
            movements.add(stockMovementRepository.save(movement));
        }

        boolean isFullyReceived = isFullyReceived(order);
        List<PurchaseOrderItem> unreceivedItems = getUnreceivedItems(order);

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

    /**
     * Обновляет или создает запись в product_stocks
     */
    private void updateProductStock(Product product, int quantity, Long warehouseId) {
        ProductStock stock = productStockRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouseId)
                .orElse(null);

        if (stock == null) {
            stock = ProductStock.builder()
                    .productId(product.getId())
                    .warehouseId(warehouseId)
                    .quantity(quantity)
                    .reservedQuantity(0)
                    .availableQuantity(quantity)
                    .createdAt(LocalDateTime.now())
                    .build();

            log.debug(messageService.get("purchase.receiving.processor.stock.created",
                    product.getSku(), warehouseId, quantity));
        } else {
            int oldQuantity = stock.getQuantity();
            stock.setQuantity(oldQuantity + quantity);
            stock.setAvailableQuantity(stock.getQuantity() -
                    (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0));
            stock.setUpdatedAt(LocalDateTime.now());

            log.debug(messageService.get("purchase.receiving.processor.stock.updated.detail",
                    product.getSku(), warehouseId, oldQuantity, stock.getQuantity()));
        }

        productStockRepository.save(stock);
    }

    /**
     * Создает запись о движении товара
     */
    private StockMovement createStockMovement(Product product,
                                              PurchaseOrderItem item,
                                              PurchaseOrder order,
                                              Long warehouseId,
                                              Long performedBy,
                                              String batchNumber,
                                              int quantity) {

        if (batchNumber == null || batchNumber.isEmpty()) {
            batchNumber = batchNumberGenerator.generateBatchNumber();
            log.debug(messageService.get("purchase.receiving.processor.batch.generated",
                    batchNumber));
        }

        String notes = messageService.get("purchase.receiving.processor.movement.notes",
                order.getOrderNumber());

        return movementBuilder.buildFullMovement(
                product.getId(), null, null, warehouseId, quantity,
                MovementType.RECEIPT, "PURCHASE", order.getId(), order.getOrderNumber(),
                performedBy, notes, batchNumber, order.getOrderNumber()
        );
    }

    /**
     * Проверяет, полностью ли принят заказ
     */
    private boolean isFullyReceived(PurchaseOrder order) {
        for (PurchaseOrderItem item : order.getItems()) {
            int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
            if (received < item.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Получает список непринятых позиций
     */
    private List<PurchaseOrderItem> getUnreceivedItems(PurchaseOrder order) {
        List<PurchaseOrderItem> unreceived = new ArrayList<>();

        for (PurchaseOrderItem item : order.getItems()) {
            int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
            int remaining = item.getQuantity() - received;

            if (remaining > 0) {
                log.debug(messageService.get("purchase.receiving.processor.remaining.quantity",
                        item.getProductId(), remaining));
                unreceived.add(item);
            }
        }

        return unreceived;
    }

    // =========================================================================
    // ВНУТРЕННИЕ КЛАССЫ
    // =========================================================================

    @lombok.Value
    @lombok.Builder
    public static class ReceivingResult {
        List<StockMovement> movements;
        boolean isFullyReceived;
        List<PurchaseOrderItem> unreceivedItems;
        List<PurchaseOrderItem> partiallyReceived;
        List<Long> missingProducts;
    }
}