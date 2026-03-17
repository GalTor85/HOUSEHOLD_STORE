package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.StockMovementBuilder;
import ru.galtor85.household_store.dto.ReceiveStockItem;
import ru.galtor85.household_store.entity.*;
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

        List<StockMovement> movements = new ArrayList<>();

        for (ReceiveStockItem item : items) {
            // Находим товар в заказе
            OrderItem orderItem = findOrderItem(purchaseOrder, item.getProductId());

            if (orderItem == null) {
                log.error(messageService.get("purchase.error.item.not.found",
                        item.getProductId(), purchaseOrder.getId()));
                throw new IllegalArgumentException("Item not found in order");
            }

            // Проверяем количество
            if (item.getQuantity() > orderItem.getQuantity()) {
                log.error(messageService.get("purchase.error.quantity.exceeded",
                        item.getQuantity(), orderItem.getQuantity()));
                throw new IllegalArgumentException("Quantity exceeds ordered quantity");
            }

            // Получаем или создаем продукт (он уже должен существовать)
            Product product = findProduct(orderItem.getProductId());

            // Определяем ячейку для размещения
            StorageCell cell = determineTargetCell(item, warehouseId, product, item.getQuantity());

            // Размещаем товар в ячейке
            StorageCell updatedCell = assignmentProcessor.assignProductToCell(
                    cell, product, item.getQuantity(), performedBy);

            // Создаем запись о движении
            StockMovement movement = movementBuilder.buildMovement(
                    product.getId(),
                    null,  // fromCell = null (поступление)
                    updatedCell.getId(),
                    item.getQuantity(),
                    MovementType.RECEIPT,
                    "PURCHASE_" + purchaseOrder.getOrderNumber(),
                    performedBy
            );

            // Добавляем дополнительную информацию
            movement.setReferenceId(purchaseOrder.getId());
            movement.setNotes(String.format("Batch: %s, Expiry: %s",
                    item.getBatchNumber(), item.getExpiryDate()));

            stockMovementRepository.save(movement);
            movements.add(movement);

            log.info(messageService.get("purchase.log.item.stocked",
                    item.getProductId(), item.getQuantity(), updatedCell.getCode()));
        }

        return movements;
    }

    /**
     * Определение целевой ячейки для размещения
     */
    private StorageCell determineTargetCell(ReceiveStockItem item, Long warehouseId,
                                            Product product, int quantity) {

        // Если указана конкретная ячейка - используем её
        if (item.getCellId() != null) {
            return storageCellRepository.findById(item.getCellId())
                    .orElseThrow(() -> new IllegalArgumentException("Cell not found"));
        }

        // Если указан код ячейки - ищем по коду
        if (item.getCellCode() != null) {
            return storageCellRepository.findByCodeAndWarehouseId(item.getCellCode(), warehouseId)
                    .orElseThrow(() -> new IllegalArgumentException("Cell not found"));
        }

        // Иначе автоматический подбор
        return cellAutoSelector.selectCellForProduct(warehouseId, product, quantity);
    }

    private OrderItem findOrderItem(Order order, Long productId) {
        return order.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private Product findProduct(Long productId) {
        // TODO: Inject ProductRepository
        return null; // Реализация будет добавлена
    }
}