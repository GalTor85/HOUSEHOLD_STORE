package ru.galtor85.household_store.processor.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cell.CellAlreadyOccupiedException;
import ru.galtor85.household_store.advice.exception.cell.CellNotFoundException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.builder.stock.StockMovementBuilder;
import ru.galtor85.household_store.dto.common.ReceiveStockItem;
import ru.galtor85.household_store.entity.order.OrderType;
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
     * Processes receiving of a purchase order with automatic placement into warehouse cells.
     *
     * @param order       purchase order to receive
     * @param items       list of received items with quantities and cell preferences
     * @param warehouseId warehouse where items should be stored
     * @param managerId   ID of the manager processing the receipt
     * @return receiving result with movements, placements, and any failures
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
        List<String> errorMessages = new ArrayList<>();

        for (ReceiveStockItem item : items) {
            try {
                // 1. Find order item
                PurchaseOrderItem orderItem = order.getItems().stream()
                        .filter(oi -> oi.getProductId().equals(item.getProductId()))
                        .findFirst()
                        .orElse(null);

                if (orderItem == null) {
                    String error = messageService.get("cell.receiving.processor.product.not.found",
                            item.getProductId(), order.getId());
                    log.warn(error);
                    failedItems.add(item.getProductId());
                    errorMessages.add(error);
                    continue;
                }

                // 2. Get product
                Product product = productRepository.findById(orderItem.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException(orderItem.getProductId()));

                // 3. Calculate already received and remaining quantities
                int alreadyReceived = orderItem.getReceivedQuantity() != null ?
                        orderItem.getReceivedQuantity() : 0;
                int orderedQuantity = orderItem.getQuantity();
                int remainingToReceive = orderedQuantity - alreadyReceived;

                int receivingQuantity = item.getQuantity();

                // 4. Check if receiving quantity exceeds remaining
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

                // 5. Determine target cell
                StorageCell cell = determineCell(item, product, warehouseId, receivingQuantity);

                // 6. Assign product to cell
                StorageCell updatedCell = assignProductToCell(cell, product, receivingQuantity, managerId);

                // 7. Update order item received quantity
                orderItem.setReceivedQuantity(alreadyReceived + receivingQuantity);

                // 8. Update product stock quantity
                int oldQuantity = product.getQuantityInStock();
                int newQuantity = oldQuantity + receivingQuantity;
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                log.debug(messageService.get("cell.receiving.processor.stock.updated",
                        product.getSku(), oldQuantity, newQuantity));

                // 9. Update product_stocks table
                updateProductStock(product, receivingQuantity, warehouseId);

                // 10. Create stock movement record
                StockMovement movement = createStockMovementWithCell(
                        product, orderItem, order, updatedCell, warehouseId,
                        managerId, item.getBatchNumber(), receivingQuantity
                );
                movements.add(stockMovementRepository.save(movement));

                // 11. Save placement info
                placements.add(new CellPlacementInfo(
                        product.getId(),
                        product.getSku(),
                        updatedCell.getId(),
                        updatedCell.getCode(),
                        receivingQuantity
                ));

                log.debug(messageService.get("cell.receiving.processor.item.placed",
                        product.getSku(), receivingQuantity, updatedCell.getCode()));

            } catch (CellNotFoundException e) {
                String error = messageService.get("cell.receiving.processor.cell.not.found",
                        item.getProductId(), e.getMessage());
                log.error(error);
                failedItems.add(item.getProductId());
                errorMessages.add(error);

            } catch (CellAlreadyOccupiedException e) {
                String error = messageService.get("cell.receiving.processor.cell.already.occupied",
                        item.getProductId(), e.getCellId(), e.getCurrentProductId());
                log.error(error);
                failedItems.add(item.getProductId());
                errorMessages.add(error);

            } catch (Exception e) {
                String error = messageService.get("cell.receiving.processor.item.failed",
                        item.getProductId(), e.getMessage());
                log.error(error, e);
                failedItems.add(item.getProductId());
                errorMessages.add(error);
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
                .errorMessages(errorMessages)
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
     * Assigns a product to a storage cell or increases quantity if the same product.
     * Validates cell occupancy, activity, type compatibility, weight and volume limits.
     *
     * @param cell        target storage cell
     * @param product     product to place
     * @param quantity    quantity to add
     * @param assignedBy  ID of user performing the assignment
     * @return updated storage cell
     * @throws CellAlreadyOccupiedException if cell contains a different product
     * @throws CellInactiveException if cell is inactive
     * @throws IncompatibleCellTypeException if cell type is not compatible
     * @throws CellWeightLimitExceededException if weight limit would be exceeded
     * @throws CellVolumeLimitExceededException if volume limit would be exceeded
     */
    private StorageCell assignProductToCell(StorageCell cell, Product product,
                                            int quantity, Long assignedBy) {

        // Validate cell can accept this product
        cellValidationHelper.validateCellNotOccupied(cell, product);
        cellValidationHelper.validateCellActive(cell);
        cellValidationHelper.validateCellTypeCompatibility(cell, product);
        cellValidationHelper.validateWeightLimit(cell, product, quantity);
        cellValidationHelper.validateVolumeLimit(cell, product, quantity);

        // Calculate new quantity
        int currentQty = cell.getCurrentQuantity() != null ? cell.getCurrentQuantity() : 0;
        int newQty = currentQty + quantity;

        // Update cell
        cell.setCurrentProductId(product.getId());
        cell.setCurrentQuantity(newQty);
        cell.setIsOccupied(true);
        cell.setLastInventoryDate(LocalDateTime.now());

        log.info(messageService.get("cell.assignment.updated.log",
                cell.getCode(), currentQty, newQty, product.getId()));

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
                OrderType.PURCHASE.name(),
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
        List<String> errorMessages;
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