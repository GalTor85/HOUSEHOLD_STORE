package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.ProductNotFoundException;
import ru.galtor85.household_store.builder.StockMovementBuilder;
import ru.galtor85.household_store.dto.ReceiveStockItem;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.StorageCellRepository;
import ru.galtor85.household_store.repository.StockMovementRepository;
import ru.galtor85.household_store.service.MessageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseReceivingProcessor {

    private final StorageCellRepository storageCellRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;  // ДОБАВЛЕНО
    private final CellAssignmentProcessor assignmentProcessor;
    private final CellAutoSelector cellAutoSelector;
    private final StockMovementBuilder movementBuilder;
    private final MessageService messageService;

    /**
     * Обработка приемки товара и размещения на складе
     */
    @Transactional
    public List<StockMovement> processReceiving(Order purchaseOrder,
                                                List<ReceiveStockItem> items,
                                                Long warehouseId,
                                                Long performedBy) {

        log.info(messageService.get("purchase.receiving.start",
                purchaseOrder.getOrderNumber(), items.size(), performedBy));

        List<StockMovement> movements = new ArrayList<>();

        for (ReceiveStockItem item : items) {
            // Находим товар в заказе
            OrderItem orderItem = findOrderItem(purchaseOrder, item.getProductId());

            if (orderItem == null) {
                log.error(messageService.get("purchase.error.item.not.found",
                        item.getProductId(), purchaseOrder.getId()));
                throw new IllegalArgumentException(
                        messageService.get("purchase.error.item.not.found.message",
                                item.getProductId(), purchaseOrder.getOrderNumber())
                );
            }

            // Проверяем количество
            if (item.getQuantity() > orderItem.getQuantity()) {
                log.error(messageService.get("purchase.error.quantity.exceeded",
                        item.getQuantity(), orderItem.getQuantity()));
                throw new IllegalArgumentException(
                        messageService.get("purchase.error.quantity.exceeded.message",
                                item.getQuantity(), orderItem.getQuantity())
                );
            }

            // Получаем продукт из репозитория
            Product product = findProductById(orderItem.getProductId());

            // Определяем ячейку для размещения
            StorageCell cell = determineTargetCell(item, warehouseId, product, item.getQuantity());

            // Размещаем товар в ячейке
            StorageCell updatedCell = assignmentProcessor.assignProductToCell(
                    cell, product, item.getQuantity(), performedBy);

            // Создаем запись о движении
            StockMovement movement = createStockMovement(item, product, updatedCell, purchaseOrder, performedBy);
            stockMovementRepository.save(movement);
            movements.add(movement);

            log.info(messageService.get("purchase.log.item.stocked",
                    item.getProductId(), item.getQuantity(), updatedCell.getCode()));
        }

        log.info(messageService.get("purchase.receiving.complete",
                purchaseOrder.getOrderNumber(), movements.size()));

        return movements;
    }

    /**
     * Создание записи о движении товара
     */
    private StockMovement createStockMovement(ReceiveStockItem item, Product product,
                                              StorageCell cell, Order purchaseOrder,
                                              Long performedBy) {

        StockMovement movement = movementBuilder.buildMovement(
                product.getId(),
                null,  // fromCell = null (поступление)
                cell.getId(),
                item.getQuantity(),
                MovementType.RECEIPT,
                "PURCHASE",
                performedBy
        );

        movement.setReferenceId(purchaseOrder.getId());

        // Формируем заметку с дополнительной информацией
        StringBuilder notes = new StringBuilder();
        notes.append("Received from purchase order: ").append(purchaseOrder.getOrderNumber());

        if (item.getBatchNumber() != null && !item.getBatchNumber().isEmpty()) {
            notes.append(", Batch: ").append(item.getBatchNumber());
        }
        if (item.getExpiryDate() != null) {
            notes.append(", Expiry: ").append(item.getExpiryDate().toLocalDate());
        }
        if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
            notes.append(", Serial: ").append(item.getSerialNumber());
        }
        if (item.getQualityCertificateNumber() != null) {
            notes.append(", Certificate: ").append(item.getQualityCertificateNumber());
        }

        movement.setNotes(notes.toString());

        return movement;
    }

    /**
     * Определение целевой ячейки для размещения
     */
    private StorageCell determineTargetCell(ReceiveStockItem item, Long warehouseId,
                                            Product product, int quantity) {

        // Если указана конкретная ячейка - используем её
        if (item.getCellId() != null) {
            log.debug(messageService.get("purchase.receiving.using.cell.id", item.getCellId()));
            return storageCellRepository.findById(item.getCellId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            messageService.get("purchase.error.cell.not.found.id", item.getCellId())));
        }

        // Если указан код ячейки - ищем по коду
        if (item.getCellCode() != null && !item.getCellCode().isEmpty()) {
            log.debug(messageService.get("purchase.receiving.using.cell.code",
                    item.getCellCode(), warehouseId));
            return storageCellRepository.findByCodeAndWarehouseId(item.getCellCode(), warehouseId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            messageService.get("purchase.error.cell.not.found.code",
                                    item.getCellCode(), warehouseId)));
        }

        // Иначе автоматический подбор
        log.debug(messageService.get("purchase.receiving.auto.select",
                product.getId(), quantity));
        return cellAutoSelector.selectCellForProduct(warehouseId, product, quantity);
    }

    /**
     * Поиск товара в заказе
     */
    private OrderItem findOrderItem(Order order, Long productId) {
        return order.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Поиск продукта по ID
     */
    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("product.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    /**
     * Проверка, все ли товары из заказа были приняты
     */
    public boolean isFullyReceived(Order purchaseOrder, List<ReceiveStockItem> items) {
        for (OrderItem orderItem : purchaseOrder.getItems()) {
            ReceiveStockItem receivedItem = items.stream()
                    .filter(item -> item.getProductId().equals(orderItem.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (receivedItem == null || receivedItem.getQuantity() < orderItem.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Получение списка непринятых товаров
     */
    public List<OrderItem> getUnreceivedItems(Order purchaseOrder, List<ReceiveStockItem> items) {
        List<OrderItem> unreceived = new ArrayList<>();

        for (OrderItem orderItem : purchaseOrder.getItems()) {
            ReceiveStockItem receivedItem = items.stream()
                    .filter(item -> item.getProductId().equals(orderItem.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (receivedItem == null) {
                // Товар вообще не принимали
                unreceived.add(orderItem);
            } else if (receivedItem.getQuantity() < orderItem.getQuantity()) {
                // Товар принят частично, нужно создать отдельный объект с оставшимся количеством
                OrderItem remainingItem = new OrderItem();
                remainingItem.setProductId(orderItem.getProductId());
                remainingItem.setQuantity(orderItem.getQuantity() - receivedItem.getQuantity());
                remainingItem.setPrice(orderItem.getPrice());
                remainingItem.setProductName(orderItem.getProductName());
                remainingItem.setProductSku(orderItem.getProductSku());
                unreceived.add(remainingItem);
            }
        }

        return unreceived;
    }
}