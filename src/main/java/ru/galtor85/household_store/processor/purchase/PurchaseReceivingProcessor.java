package ru.galtor85.household_store.processor.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.builder.stock.StockMovementBuilder;
import ru.galtor85.household_store.dto.request.order.ReceiveAndStockRequest;
import ru.galtor85.household_store.dto.common.ReceiveStockItem;
import ru.galtor85.household_store.dto.request.order.ReverseReceiptItem;
import ru.galtor85.household_store.dto.request.order.ReverseReceiptRequest;
import ru.galtor85.household_store.entity.order.OrderType;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.stock.StockMovementRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.stock.StockService;
import ru.galtor85.household_store.service.warehouse.WarehouseService;
import ru.galtor85.household_store.util.batch.BatchNumberGenerator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Processor for purchase order receiving and return operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseReceivingProcessor {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementBuilder movementBuilder;
    private final BatchNumberGenerator batchNumberGenerator;
    private final MessageService messageService;
    private final StockService stockService;
    private final WarehouseService warehouseService;

    // =========================================================================
    // RECEIVE PURCHASE ORDER
    // =========================================================================

    /**
     * Processes receiving of a purchase order
     *
     * @param order     purchase order to receive
     * @param request   receiving request with items and warehouse
     * @param managerId ID of the manager processing the receipt
     * @return receiving result with movements and status
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
            // Find order item
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

            // Calculate received quantity
            int alreadyReceived = orderItem.getReceivedQuantity() != null ?
                    orderItem.getReceivedQuantity() : 0;
            int orderedQuantity = orderItem.getQuantity();
            int remainingToReceive = orderedQuantity - alreadyReceived;

            int receivingQuantity = item.getQuantity();

            // Check if quantity exceeds remaining
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

            // Check for partial receipt
            boolean isPartial = (alreadyReceived + receivingQuantity) < orderedQuantity;
            if (isPartial) {
                log.debug(messageService.get("purchase.receiving.processor.partial.receipt",
                        product.getSku(), alreadyReceived + receivingQuantity, orderedQuantity));
                partiallyReceived.add(orderItem);
            }

            // Update order item received quantity
            orderItem.setReceivedQuantity(alreadyReceived + receivingQuantity);

            // Update product stock
            int oldQuantity = product.getQuantityInStock();
            int newQuantity = oldQuantity + receivingQuantity;
            product.setQuantityInStock(newQuantity);
            productRepository.save(product);

            log.debug(messageService.get("purchase.receiving.processor.stock.updated",
                    product.getSku(), oldQuantity, newQuantity));

            // Update product_stocks
            stockService.updateProductStock(product, receivingQuantity, request.getWarehouseId(), true);

            // Create stock movement
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
     * Creates a stock movement record for receiving
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
                MovementType.RECEIPT, OrderType.PURCHASE.name(), order.getId(), order.getOrderNumber(),
                performedBy, notes, batchNumber, order.getOrderNumber()
        );
    }

    /**
     * Checks if all items in the order have been received
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
     * Gets list of items that have not been fully received
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
    // REVERSE RECEIPT (RETURN TO SUPPLIER)
    // =========================================================================

    /**
     * Reverses a purchase order receipt (returns goods to supplier)
     * Uses information from original receipt transactions
     *
     * @param order     purchase order to reverse
     * @param request   reversal request with reason and items
     * @param managerId ID of the manager processing the reversal
     * @return reversal result with movements and status
     */
    @Transactional
    public ReverseReceiptResult reverseReceipt(PurchaseOrder order,
                                               ReverseReceiptRequest request,
                                               Long managerId) {

        log.info(messageService.get("purchase.receiving.reverse.start",
                order.getOrderNumber(), request.getReason(), managerId));

        List<StockMovement> movements = new ArrayList<>();
        List<ReverseReceiptItemResult> reversedItems = new ArrayList<>();
        List<Long> failedItems = new ArrayList<>();

        // Get all receipt transactions for this order
        List<StockMovement> receiptTransactions = stockMovementRepository
                .findReceiptTransactionsByOrderId(order.getId());

        if (receiptTransactions.isEmpty()) {
            throw new IllegalStateException(
                    messageService.get("purchase.receiving.reverse.no.receipts", order.getId())
            );
        }

        List<ReverseReceiptItem> itemsToReverse = request.getItems();
        boolean reverseAll = (itemsToReverse == null || itemsToReverse.isEmpty());

        // Validate that all products in reversal request exist in the order
        assert itemsToReverse != null;
        for (ReverseReceiptItem reverseItem : itemsToReverse) {
            boolean productExists = order.getItems().stream()
                    .anyMatch(item -> item.getProductId().equals(reverseItem.getProductId()));

            if (!productExists) {
                throw new IllegalArgumentException(
                        messageService.get("purchase.reverse.product.not.in.order",
                                reverseItem.getProductId(), order.getOrderNumber())
                );
            }
        }

        // Group transactions by product
        Map<Long, List<StockMovement>> receiptsByProduct = receiptTransactions.stream()
                .collect(Collectors.groupingBy(StockMovement::getProductId));

        for (PurchaseOrderItem orderItem : order.getItems()) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(orderItem.getProductId()));

            int receivedQuantity = orderItem.getReceivedQuantity() != null ? orderItem.getReceivedQuantity() : 0;

            if (receivedQuantity == 0) {
                continue;
            }

            // Get receipt transactions for this produc
            List<StockMovement> productReceipts = receiptsByProduct.getOrDefault(
                    orderItem.getProductId(), Collections.emptyList());

            if (productReceipts.isEmpty()) {
                log.warn(messageService.get("purchase.receiving.reverse.no.receipts.for.product",
                        orderItem.getProductId(), order.getId()));
                continue;
            }

            int quantityToReverse;

            if (reverseAll) {
                quantityToReverse = receivedQuantity;
            } else {
                ReverseReceiptItem reverseItem = itemsToReverse.stream()
                        .filter(i -> i.getProductId().equals(orderItem.getProductId()))
                        .findFirst()
                        .orElse(null);

                if (reverseItem == null) {
                    continue;
                }
                quantityToReverse = Math.min(reverseItem.getQuantity(), receivedQuantity);
            }

            if (quantityToReverse <= 0) {
                continue;
            }

            try {
                // Return goods using LIFO (last in, first out)
                int remainingToReverse = quantityToReverse;
                List<StockMovement> reversedMovements = new ArrayList<>();

                for (StockMovement receipt : productReceipts) {
                    if (remainingToReverse <= 0) break;

                    int receiptQuantity = receipt.getQuantity();
                    int reverseFromThisReceipt = Math.min(remainingToReverse, receiptQuantity);

                    // Get data from receipt transaction
                    Long warehouseId = receipt.getWarehouseId();
                    Long cellId = receipt.getToCellId();

                    // Create reverse movement
                    StockMovement reverseMovement = createReverseMovementFromReceipt(
                            product, orderItem, order, receipt, managerId,
                            reverseFromThisReceipt, request.getReason()
                    );
                    reversedMovements.add(stockMovementRepository.save(reverseMovement));
                    movements.add(reverseMovement);

                    // Update product stock (decrease)
                    stockService.updateProductStock(product, reverseFromThisReceipt, warehouseId, false);

                    // Update receipt transaction
                    updateReceiptTransaction(receipt, reverseFromThisReceipt);

                    // Clear cell if it becomes empty
                    if (cellId != null && reverseFromThisReceipt == receiptQuantity) {
                        warehouseService.clearCell(cellId, null);
                    }

                    remainingToReverse -= reverseFromThisReceipt;
                }

                // Update product stock
                int oldQuantity = product.getQuantityInStock();
                int newQuantity = oldQuantity - quantityToReverse;
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                // Update order item received quantity
                orderItem.setReceivedQuantity(receivedQuantity - quantityToReverse);

                reversedItems.add(ReverseReceiptItemResult.builder()
                        .productId(product.getId())
                        .productSku(product.getSku())
                        .productName(product.getName())
                        .quantity(quantityToReverse)
                        .oldReceivedQuantity(receivedQuantity)
                        .newReceivedQuantity(receivedQuantity - quantityToReverse)
                        .build());

                log.debug(messageService.get("purchase.receiving.reverse.item.processed",
                        product.getSku(), quantityToReverse, reversedMovements.size()));

            } catch (Exception e) {
                log.error(messageService.get("purchase.receiving.reverse.item.failed",
                        product.getId(), e.getMessage()), e);
                failedItems.add(product.getId());
            }
        }

        boolean allReversed = order.getItems().stream()
                .allMatch(item -> item.getReceivedQuantity() == null || item.getReceivedQuantity() == 0);

        log.info(messageService.get("purchase.receiving.reverse.complete",
                order.getOrderNumber(), movements.size(), allReversed));

        return ReverseReceiptResult.builder()
                .movements(movements)
                .reversedItems(reversedItems)
                .failedItems(failedItems)
                .allSuccess(failedItems.isEmpty())
                .allReversed(allReversed)
                .build();
    }

    /**
     * Creates a reverse movement from a receipt transaction
     */
    private StockMovement createReverseMovementFromReceipt(Product product,
                                                           PurchaseOrderItem item,
                                                           PurchaseOrder order,
                                                           StockMovement receipt,
                                                           Long performedBy,
                                                           int quantity,
                                                           String reason) {

        String notes = messageService.get("purchase.receiving.reverse.movement.notes",
                order.getOrderNumber(), reason, receipt.getBatchNumber());

        return StockMovement.builder()
                .productId(product.getId())
                .warehouseId(receipt.getWarehouseId())
                .fromCellId(receipt.getToCellId())
                .toCellId(null)
                .quantity(quantity)
                .movementType(MovementType.RETURN)
                .referenceType("RETURN")
                .referenceId(order.getId())
                .referenceNumber(order.getOrderNumber())
                .originalMovementId(receipt.getId())
                .performedBy(performedBy)
                .notes(notes)
                .batchNumber(receipt.getBatchNumber())
                .documentNumber(order.getOrderNumber() + "-RETURN")
                .build();
    }

    /**
     * Updates receipt transaction after partial return
     */
    private void updateReceiptTransaction(StockMovement receipt, int returnedQuantity) {
        // TODO: Implement tracking of returned quantities
    }

    // =========================================================================
    // INNER CLASSES
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

    @lombok.Value
    @lombok.Builder
    public static class ReverseReceiptResult {
        List<StockMovement> movements;
        List<ReverseReceiptItemResult> reversedItems;
        List<Long> failedItems;
        boolean allSuccess;
        boolean allReversed;
    }

    @lombok.Value
    @lombok.Builder
    public static class ReverseReceiptItemResult {
        Long productId;
        String productSku;
        String productName;
        int quantity;
        int oldReceivedQuantity;
        int newReceivedQuantity;
    }
}