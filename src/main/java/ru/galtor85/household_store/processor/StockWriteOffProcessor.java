package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.WriteOffInsufficientStockException;
import ru.galtor85.household_store.builder.StockMovementBuilder;
import ru.galtor85.household_store.dto.StockWriteOffItem;
import ru.galtor85.household_store.dto.StockWriteOffRequest;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.ProductStockRepository;
import ru.galtor85.household_store.repository.StockMovementRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.service.WarehouseService;
import ru.galtor85.household_store.util.EntityFinder;
import ru.galtor85.household_store.util.NumberGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockWriteOffProcessor {

    private final EntityFinder entityFinder;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementBuilder movementBuilder;
    private final NumberGenerator numberGenerator;
    private final MessageService messageService;

    // =========================================================================
    // ОСНОВНОЙ МЕТОД СПИСАНИЯ
    // =========================================================================

    /**
     * Обрабатывает списание товаров
     */
    @Transactional
    public WriteOffResult processWriteOff(StockWriteOffRequest request, Long managerId) {

        log.info(messageService.get("writeoff.processor.start",
                request.getItems().size(), request.getReason(), managerId));

        List<StockMovement> movements = new ArrayList<>();
        List<WriteOffFailedItem> failedItems = new ArrayList<>();
        List<WriteOffSuccessItem> successItems = new ArrayList<>();

        String documentNumber = numberGenerator.generateWriteOffNumber();

        for (StockWriteOffItem item : request.getItems()) {
            try {
                // 1. Проверяем товар
                Product product = entityFinder.findProductById(item.getProductId());

                // 2. Проверяем остаток
                validateStockAvailability(product, item.getQuantity());

                // 3. Обновляем остаток в Product
                int oldQuantity = product.getQuantityInStock();
                int newQuantity = oldQuantity - item.getQuantity();
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                // 4. Обновляем остатки по складам (если есть привязка к складу)
                if (request.getWarehouseId() != null) {
                    updateProductStock(product, item.getQuantity(), request.getWarehouseId());
                }

                // 5. Создаем движение списания
                StockMovement movement = createWriteOffMovement(
                        product, item, request, documentNumber, managerId);
                movements.add(stockMovementRepository.save(movement));

                // 6. Добавляем в успешные
                successItems.add(new WriteOffSuccessItem(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        item.getQuantity(),
                        oldQuantity,
                        newQuantity
                ));

                log.debug(messageService.get("writeoff.processor.item.processed",
                        product.getSku(), item.getQuantity(), oldQuantity, newQuantity));

            } catch (Exception e) {
                log.error(messageService.get("writeoff.processor.item.failed",
                        item.getProductId(), e.getMessage()), e);

                failedItems.add(new WriteOffFailedItem(
                        item.getProductId(),
                        item.getQuantity(),
                        e.getMessage()
                ));
            }
        }

        log.info(messageService.get("writeoff.processor.complete",
                successItems.size(), movements.size(), failedItems.size()));

        return WriteOffResult.builder()
                .movements(movements)
                .successItems(successItems)
                .failedItems(failedItems)
                .allSuccess(failedItems.isEmpty())
                .documentNumber(documentNumber)
                .build();
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Проверяет наличие достаточного количества товара
     */
    private void validateStockAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.error(messageService.get("writeoff.processor.insufficient.stock",
                    product.getSku(), product.getQuantityInStock(), requestedQuantity));
            throw new WriteOffInsufficientStockException(
                    product.getId(),
                    product.getQuantityInStock(),
                    requestedQuantity
            );
        }
    }

    /**
     * Обновляет или создает запись в product_stocks
     */
    private void updateProductStock(Product product, int quantity, Long warehouseId) {
        ProductStock stock = productStockRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouseId)
                .orElse(null);

        if (stock == null) {
            log.warn(messageService.get("writeoff.processor.stock.not.found",
                    product.getSku(), warehouseId));
            return;
        }

        int oldQuantity = stock.getQuantity();
        int newQuantity = oldQuantity - quantity;

        if (newQuantity < 0) {
            log.error(messageService.get("writeoff.processor.stock.negative",
                    product.getSku(), warehouseId, oldQuantity, quantity));
            throw new WriteOffInsufficientStockException(
                    product.getId(), oldQuantity, quantity
            );
        }

        stock.setQuantity(newQuantity);
        stock.setAvailableQuantity(newQuantity -
                (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0));
        stock.setUpdatedAt(LocalDateTime.now());

        productStockRepository.save(stock);

        log.debug(messageService.get("writeoff.processor.stock.updated",
                product.getSku(), warehouseId, oldQuantity, newQuantity));
    }

    /**
     * Создает движение списания
     */
    private StockMovement createWriteOffMovement(Product product,
                                                 StockWriteOffItem item,
                                                 StockWriteOffRequest request,
                                                 String documentNumber,
                                                 Long managerId) {

        String notes = messageService.get("writeoff.processor.movement.notes",
                request.getReason(),
                item.getReason() != null ? item.getReason() : request.getReason(),
                documentNumber);

        return movementBuilder.buildFullMovement(
                product.getId(),
                null,                           // fromCellId
                null,                           // toCellId
                request.getWarehouseId(),       // warehouseId
                item.getQuantity(),
                MovementType.WRITE_OFF,
                "WRITE_OFF",
                request.getRelatedOrderId(),
                documentNumber,
                managerId,
                notes,
                item.getBatchNumber(),
                documentNumber
        );
    }

    // =========================================================================
    // МЕТОДЫ ДЛЯ ГРУППОВОГО СПИСАНИЯ
    // =========================================================================

    /**
     * Списание всех товаров по заказу
     */
    @Transactional
    public List<StockMovement> writeOffOrderItems(PurchaseOrder order,
                                                  String reason,
                                                  Long managerId) {

        List<StockMovement> movements = new ArrayList<>();
        String documentNumber = numberGenerator.generateWriteOffNumber();

        for (PurchaseOrderItem item : order.getItems()) {
            Product product = entityFinder.findProductById(item.getProductId());

            int remainingToWriteOff = item.getRemainingQuantity();
            if (remainingToWriteOff > 0) {
                StockMovement movement = createWriteOffMovement(
                        product,
                        remainingToWriteOff,
                        reason,
                        documentNumber,
                        managerId
                );
                movements.add(stockMovementRepository.save(movement));

                // Обновляем остатки
                product.setQuantityInStock(product.getQuantityInStock() - remainingToWriteOff);
                productRepository.save(product);

                log.debug(messageService.get("writeoff.processor.order.item.written.off",
                        product.getSku(), remainingToWriteOff, order.getOrderNumber()));
            }
        }

        return movements;
    }

    private StockMovement createWriteOffMovement(Product product,
                                                 int quantity,
                                                 String reason,
                                                 String documentNumber,
                                                 Long managerId) {

        String notes = messageService.get("writeoff.processor.movement.notes",
                reason, reason, documentNumber);

        return movementBuilder.buildFullMovement(
                product.getId(),
                null, null, null, quantity,
                MovementType.WRITE_OFF,
                "WRITE_OFF",
                null,
                documentNumber,
                managerId,
                notes,
                null,
                documentNumber
        );
    }

    // =========================================================================
    // ВНУТРЕННИЕ КЛАССЫ
    // =========================================================================

    @lombok.Value
    @lombok.Builder
    public static class WriteOffResult {
        List<StockMovement> movements;
        List<WriteOffSuccessItem> successItems;
        List<WriteOffFailedItem> failedItems;
        boolean allSuccess;
        String documentNumber;
    }

    @lombok.Value
    public static class WriteOffSuccessItem {
        Long productId;
        String productSku;
        String productName;
        int quantity;
        int oldStock;
        int newStock;
    }

    @lombok.Value
    public static class WriteOffFailedItem {
        Long productId;
        int requestedQuantity;
        String errorMessage;
    }
}