package ru.galtor85.household_store.processor.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cell.CellAlreadyOccupiedException;
import ru.galtor85.household_store.advice.exception.cell.CellNotFoundException;
import ru.galtor85.household_store.builder.stock.StockMovementBuilder;
import ru.galtor85.household_store.calculator.ReceivingQuantityCalculator;
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
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.batch.BatchNumberGenerator;
import ru.galtor85.household_store.validator.cell.CellValidationHelper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Processor for receiving purchase orders with automatic cell placement.
 */
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
    private final LogMessageService logMsg;
    private final ReceivingQuantityCalculator quantityCalculator;

    // =========================================================================
    // MAIN RECEIVING METHOD WITH CELL PLACEMENT
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

        log.info(logMsg.get("cell.receiving.processor.start",
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

                // 2. Calculate receiving quantity
                ReceivingQuantityCalculator.ReceivingResult result = quantityCalculator.calculate(orderItem, item);

                if (result.hasNoQuantity()) {
                    continue;
                }

                Product product = result.product();
                int receivingQuantity = result.receivingQuantity();
                int alreadyReceived = result.alreadyReceived();

                // 3. Determine target cell
                StorageCell cell = determineCell(item, product, warehouseId, receivingQuantity);

                // 4. Assign product to cell
                StorageCell updatedCell = assignProductToCell(cell, product, receivingQuantity);

                // 5. Update order item received quantity
                orderItem.setReceivedQuantity(alreadyReceived + receivingQuantity);

                // 6. Update product stock quantity
                int oldQuantity = product.getQuantityInStock();
                int newQuantity = oldQuantity + receivingQuantity;
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                log.debug(logMsg.get("cell.receiving.processor.stock.updated",
                        product.getSku(), oldQuantity, newQuantity));

                // 7. Update product_stocks table
                updateProductStock(product, receivingQuantity, warehouseId);

                // 8. Create stock movement record
                StockMovement movement = createStockMovementWithCell(
                        product, order, updatedCell, warehouseId,
                        managerId, item.getBatchNumber(), receivingQuantity
                );
                movements.add(stockMovementRepository.save(movement));

                // 9. Save placement info
                placements.add(new CellPlacementInfo(
                        product.getId(),
                        product.getSku(),
                        updatedCell.getId(),
                        updatedCell.getCode(),
                        receivingQuantity
                ));

                log.debug(logMsg.get("cell.receiving.processor.item.placed",
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

        log.info(logMsg.get("cell.receiving.processor.complete",
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
    // HELPER METHODS
    // =========================================================================

    /**
     * Determines the target cell for product placement.
     *
     * @param item        the received item
     * @param product     the product
     * @param warehouseId the warehouse ID
     * @param quantity    the quantity
     * @return selected StorageCell
     * @throws CellNotFoundException if specified cell not found
     */
    private StorageCell determineCell(ReceiveStockItem item, Product product,
                                      Long warehouseId, int quantity) {

        if (item.getCellId() != null) {
            return storageCellRepository.findById(item.getCellId())
                    .orElseThrow(() -> new CellNotFoundException(item.getCellId()));

        } else if (item.getCellCode() != null) {
            return storageCellRepository.findByCodeAndWarehouseId(item.getCellCode(), warehouseId)
                    .orElseThrow(() -> new CellNotFoundException(item.getCellCode(), warehouseId));

        } else {
            return cellAutoSelector.selectCellForProduct(warehouseId, product, quantity);
        }
    }

    /**
     * Assigns a product to a storage cell or increases quantity if the same product.
     *
     * @param cell     target storage cell
     * @param product  product to place
     * @param quantity quantity to add
     * @return updated storage cell
     * @throws CellAlreadyOccupiedException if cell contains a different product
     */
    private StorageCell assignProductToCell(StorageCell cell, Product product, int quantity) {

        cellValidationHelper.validateCellNotOccupied(cell, product);
        cellValidationHelper.validateCellActive(cell);
        cellValidationHelper.validateCellTypeCompatibility(cell, product);
        cellValidationHelper.validateWeightLimit(cell, product, quantity);
        cellValidationHelper.validateVolumeLimit(cell, product, quantity);

        int currentQty = cell.getCurrentQuantity() != null ? cell.getCurrentQuantity() : 0;
        int newQty = currentQty + quantity;

        cell.setCurrentProductId(product.getId());
        cell.setCurrentQuantity(newQty);
        cell.setIsOccupied(true);
        cell.setLastInventoryDate(LocalDateTime.now());

        log.info(logMsg.get("cell.assignment.updated.log",
                cell.getCode(), currentQty, newQty, product.getId()));

        return storageCellRepository.save(cell);
    }

    /**
     * Updates or creates a record in product_stocks.
     *
     * @param product     the product
     * @param quantity    the quantity to add
     * @param warehouseId the warehouse ID
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

            log.debug(logMsg.get("cell.receiving.processor.stock.created",
                    product.getSku(), warehouseId, quantity));
        } else {
            int oldQuantity = stock.getQuantity();
            stock.setQuantity(oldQuantity + quantity);
            stock.setAvailableQuantity(stock.getQuantity() -
                    (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0));
            stock.setUpdatedAt(LocalDateTime.now());

            log.debug(logMsg.get("cell.receiving.processor.stock.updated.detail",
                    product.getSku(), warehouseId, oldQuantity, stock.getQuantity()));
        }

        productStockRepository.save(stock);
    }

    /**
     * Creates a stock movement record with cell reference.
     *
     * @param product     the product
     * @param order       the purchase order
     * @param cell        the storage cell
     * @param warehouseId the warehouse ID
     * @param performedBy ID of the user performing the operation
     * @param batchNumber the batch number
     * @param quantity    the quantity
     * @return created StockMovement entity
     */
    private StockMovement createStockMovementWithCell(Product product,
                                                      PurchaseOrder order,
                                                      StorageCell cell,
                                                      Long warehouseId,
                                                      Long performedBy,
                                                      String batchNumber,
                                                      int quantity) {

        if (batchNumber == null || batchNumber.isEmpty()) {
            batchNumber = batchNumberGenerator.generateBatchNumber();
            log.debug(logMsg.get("cell.receiving.processor.batch.generated", batchNumber));
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
     * Checks if the order is fully received.
     *
     * @param order the purchase order
     * @return true if all items are fully received
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
    // INNER CLASSES
    // =========================================================================

    /**
     * Result of cell-based receiving operation.
     */
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

    /**
     * Information about a placed item.
     */
    public record CellPlacementInfo(Long productId, String productSku, Long cellId, String cellCode, int quantity) {
    }
}