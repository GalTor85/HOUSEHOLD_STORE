package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.StockMovementBuilder;
import ru.galtor85.household_store.dto.ReceiveStockItem;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.StockMovementRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.util.BatchNumberGenerator;
import ru.galtor85.household_store.util.EntityFinder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CellBasedReceivingProcessor {

    private final EntityFinder entityFinder;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementBuilder movementBuilder;
    private final BatchNumberGenerator batchNumberGenerator;
    private final CellAutoSelector cellAutoSelector;
    private final CellAssignmentProcessor assignmentProcessor;
    private final MessageService messageService;

    @Transactional
    public CellBasedReceivingResult processReceivingWithCells(Order order,
                                                              List<ReceiveStockItem> items,
                                                              Long warehouseId,
                                                              Long managerId) {

        log.info(messageService.get("cell.receiving.processor.start",
                order.getOrderNumber(), items.size(), warehouseId, managerId));

        List<StockMovement> movements = new ArrayList<>();
        List<CellPlacementInfo> placements = new ArrayList<>();
        List<Long> failedItems = new ArrayList<>();

        for (ReceiveStockItem item : items) {
            try {
                OrderItem orderItem = entityFinder.findOrderItem(order, item.getProductId());

                if (orderItem == null) {
                    log.warn(messageService.get("cell.receiving.processor.product.not.found",
                            item.getProductId(), order.getId()));
                    failedItems.add(item.getProductId());
                    continue;
                }

                Product product = entityFinder.findProductById(orderItem.getProductId());

                // 1. Автоматический подбор ячейки
                StorageCell cell = cellAutoSelector.selectCellForProduct(
                        warehouseId, product, item.getQuantity());

                log.debug(messageService.get("cell.receiving.processor.cell.selected",
                        cell.getCode(), cell.getId(), product.getSku()));

                // 2. Размещение в ячейку
                StorageCell updatedCell = assignmentProcessor.assignProductToCell(
                        cell, product, item.getQuantity(), managerId);

                // 3. Обновление остатка продукта
                int oldQuantity = product.getQuantityInStock();
                int newQuantity = oldQuantity + item.getQuantity();
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                log.debug(messageService.get("cell.receiving.processor.stock.updated",
                        product.getSku(), oldQuantity, newQuantity));

                // 4. Создание движения с привязкой к ячейке
                StockMovement movement = createStockMovementWithCell(
                        product, orderItem, order, updatedCell, warehouseId,
                        managerId, item.getBatchNumber()
                );
                movements.add(stockMovementRepository.save(movement));

                placements.add(new CellPlacementInfo(
                        product.getId(),
                        product.getSku(),
                        updatedCell.getId(),
                        updatedCell.getCode(),
                        item.getQuantity()
                ));

            } catch (Exception e) {
                log.error(messageService.get("cell.receiving.processor.item.failed",
                        item.getProductId(), e.getMessage()), e);
                failedItems.add(item.getProductId());
            }
        }

        boolean allSuccess = failedItems.isEmpty();
        boolean isFullyReceived = isFullyReceived(order, items);

        log.info(messageService.get("cell.receiving.processor.complete",
                order.getOrderNumber(), movements.size(), placements.size(),
                allSuccess ? "ALL SUCCESS" : "PARTIAL"));

        return CellBasedReceivingResult.builder()
                .movements(movements)
                .placements(placements)
                .failedItems(failedItems)
                .allSuccess(allSuccess)
                .isFullyReceived(isFullyReceived)
                .build();
    }

    private StockMovement createStockMovementWithCell(Product product, OrderItem item,
                                                      Order order, StorageCell cell,
                                                      Long warehouseId, Long performedBy,
                                                      String batchNumber) {

        if (batchNumber == null || batchNumber.isEmpty()) {
            batchNumber = batchNumberGenerator.generateBatchNumber();
            log.debug(messageService.get("cell.receiving.processor.batch.generated",
                    batchNumber));
        }

        String notes = messageService.get("cell.receiving.processor.movement.notes",
                order.getOrderNumber(), cell.getCode());

        return movementBuilder.buildFullMovement(
                product.getId(),
                null,
                cell.getId(),
                warehouseId,
                item.getQuantity(),
                MovementType.RECEIPT,
                "PURCHASE",
                order.getId(),
                order.getOrderNumber(),
                performedBy,
                notes,
                batchNumber,
                order.getOrderNumber()
        );
    }

    private boolean isFullyReceived(Order order, List<ReceiveStockItem> receivedItems) {
        for (OrderItem orderItem : order.getItems()) {
            ReceiveStockItem received = receivedItems.stream()
                    .filter(item -> item.getProductId().equals(orderItem.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (received == null) {
                log.debug(messageService.get("cell.receiving.processor.missing.product",
                        orderItem.getProductId()));
                return false;
            }

            if (received.getQuantity() < orderItem.getQuantity()) {
                log.debug(messageService.get("cell.receiving.processor.partial.quantity",
                        orderItem.getProductId(), received.getQuantity(), orderItem.getQuantity()));
                return false;
            }
        }
        return true;
    }

    @lombok.Value
    @lombok.Builder
    public static class CellBasedReceivingResult {
        List<StockMovement> movements;
        List<CellPlacementInfo> placements;
        List<Long> failedItems;
        boolean allSuccess;
        boolean isFullyReceived;
    }

    @lombok.Value
    public static class CellPlacementInfo {
        Long productId;
        String productSku;
        Long cellId;
        String cellCode;
        int quantity;
    }
}