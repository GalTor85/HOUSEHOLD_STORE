package ru.galtor85.household_store.processor.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cell.CellNotFoundException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.builder.stock.StockMovementBuilder;
import ru.galtor85.household_store.dto.common.ReceiveStockItem;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.stock.StockMovementRepository;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.batch.BatchNumberGenerator;
import ru.galtor85.household_store.validator.cell.CellValidationHelper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CellBasedReceivingProcessor {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StorageCellRepository storageCellRepository;
    private final StockMovementBuilder movementBuilder;
    private final BatchNumberGenerator batchNumberGenerator;
    private final CellValidationHelper cellValidationHelper;
    private final CellAutoSelector cellAutoSelector;
    private final MessageService messageService;

    // =========================================================================
    // ОСНОВНОЙ МЕТОД ПРИЕМКИ С РАЗМЕЩЕНИЕМ ПО ЯЧЕЙКАМ
    // =========================================================================

    /**
     * Обрабатывает приемку заказа с размещением товаров по ячейкам
     */
    @Transactional
    public CellBasedReceivingResult processReceivingWithCells(PurchaseOrder order,
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
                // 1. Находим позицию в заказе
                PurchaseOrderItem orderItem = order.getItems().stream()
                        .filter(oi -> oi.getProductId().equals(item.getProductId()))
                        .findFirst()
                        .orElse(null);

                if (orderItem == null) {
                    log.warn(messageService.get("cell.receiving.processor.product.not.found",
                            item.getProductId(), order.getId()));
                    failedItems.add(item.getProductId());
                    continue;
                }

                // 2. Получаем товар
                Product product = productRepository.findById(orderItem.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException(orderItem.getProductId()));

                // 3. Получаем уже принятое количество
                int alreadyReceived = orderItem.getReceivedQuantity() != null ?
                        orderItem.getReceivedQuantity() : 0;
                int orderedQuantity = orderItem.getQuantity();
                int remainingToReceive = orderedQuantity - alreadyReceived;

                int receivingQuantity = item.getQuantity();

                // 4. Проверяем, не превышает ли принимаемое количество остаток
                if (receivingQuantity > remainingToReceive) {
                    log.warn(messageService.get("cell.receiving.processor.quantity.exceeds.remaining",
                            product.getSku(), receivingQuantity, remainingToReceive));
                    receivingQuantity = remainingToReceive;
                }

                if (receivingQuantity <= 0) {
                    log.info(messageService.get("cell.receiving.processor.already.received",
                            product.getSku()));
                    continue;
                }

                // 5. Определяем ячейку для размещения
                StorageCell cell = determineCell(item, product, warehouseId, receivingQuantity);

                // 6. Размещаем товар в ячейке
                StorageCell updatedCell = assignProductToCell(cell, product, receivingQuantity, managerId);

                // 7. Обновляем количество принятого в позиции заказа
                orderItem.setReceivedQuantity(alreadyReceived + receivingQuantity);

                // 8. Обновляем остаток в Product
                int oldQuantity = product.getQuantityInStock();
                int newQuantity = oldQuantity + receivingQuantity;
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                log.debug(messageService.get("cell.receiving.processor.stock.updated",
                        product.getSku(), oldQuantity, newQuantity));

                // 9. Обновляем product_stocks
                updateProductStock(product, receivingQuantity, warehouseId);

                // 10. Создаем движение товара
                StockMovement movement = createStockMovementWithCell(
                        product, orderItem, order, updatedCell, warehouseId,
                        managerId, item.getBatchNumber(), receivingQuantity
                );
                movements.add(stockMovementRepository.save(movement));

                // 11. Сохраняем информацию о размещении
                placements.add(new CellPlacementInfo(
                        product.getId(),
                        product.getSku(),
                        updatedCell.getId(),
                        updatedCell.getCode(),
                        receivingQuantity
                ));

                log.debug(messageService.get("cell.receiving.processor.item.placed",
                        product.getSku(), receivingQuantity, updatedCell.getCode()));

            } catch (Exception e) {
                log.error(messageService.get("cell.receiving.processor.item.failed",
                        item.getProductId(), e.getMessage()), e);
                failedItems.add(item.getProductId());
            }
        }

        boolean isFullyReceived = isFullyReceived(order);
        boolean allSuccess = failedItems.isEmpty();

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

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Определяет ячейку для размещения товара
     */
    private StorageCell determineCell(ReceiveStockItem item, Product product,
                                      Long warehouseId, int quantity) {

        if (item.getCellId() != null) {
            // Используем указанную ячейку по ID
            return storageCellRepository.findById(item.getCellId())
                    .orElseThrow(() -> new CellNotFoundException(item.getCellId()));

        } else if (item.getCellCode() != null) {
            // Используем указанную ячейку по коду
            return storageCellRepository.findByCodeAndWarehouseId(item.getCellCode(), warehouseId)
                    .orElseThrow(() -> new CellNotFoundException(item.getCellCode(), warehouseId));

        } else {
            // Автоматический подбор ячейки
            return cellAutoSelector.selectCellForProduct(warehouseId, product, quantity);
        }
    }

    /**
     * Размещает товар в ячейке
     */
    private StorageCell assignProductToCell(StorageCell cell, Product product,
                                            int quantity, Long assignedBy) {

        // Проверки
        cellValidationHelper.validateCellActive(cell);
        cellValidationHelper.validateCellNotOccupied(cell);
        cellValidationHelper.validateCellTypeCompatibility(cell, product);
        cellValidationHelper.validateWeightLimit(cell, product, quantity);
        cellValidationHelper.validateVolumeLimit(cell, product, quantity);

        // Назначение товара
        cell.setCurrentProductId(product.getId());
        cell.setCurrentQuantity(quantity);
        cell.setIsOccupied(true);
        cell.setLastInventoryDate(LocalDateTime.now());

        return storageCellRepository.save(cell);
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

            log.debug(messageService.get("cell.receiving.processor.stock.created",
                    product.getSku(), warehouseId, quantity));
        } else {
            int oldQuantity = stock.getQuantity();
            stock.setQuantity(oldQuantity + quantity);
            stock.setAvailableQuantity(stock.getQuantity() -
                    (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0));
            stock.setUpdatedAt(LocalDateTime.now());

            log.debug(messageService.get("cell.receiving.processor.stock.updated.detail",
                    product.getSku(), warehouseId, oldQuantity, stock.getQuantity()));
        }

        productStockRepository.save(stock);
    }

    /**
     * Создает движение товара с привязкой к ячейке
     */
    private StockMovement createStockMovementWithCell(Product product,
                                                      PurchaseOrderItem item,
                                                      PurchaseOrder order,
                                                      StorageCell cell,
                                                      Long warehouseId,
                                                      Long performedBy,
                                                      String batchNumber,
                                                      int quantity) {

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
                quantity,
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

    // =========================================================================
    // ВНУТРЕННИЕ КЛАССЫ
    // =========================================================================

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