package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.ProductNotFoundException;
import ru.galtor85.household_store.builder.StockMovementBuilder;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.StockMovementRepository;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseStockProcessor {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementBuilder movementBuilder;
    private final MessageService messageService;

    /**
     * Обновляет остатки товаров на складе после приемки
     */
    @Transactional
    public void processStockUpdate(Order order, Long warehouseId, Long performedBy) {
        log.info(messageService.get("purchase.stock.processing.start",
                order.getOrderNumber(), order.getItems().size()));

        for (OrderItem item : order.getItems()) {
            Product product = findProductById(item.getProductId());

            // Сохраняем старое значение для лога
            int oldQuantity = product.getQuantityInStock();

            // 1. Увеличиваем количество товара на складе
            int newQuantity = oldQuantity + item.getQuantity();
            product.setQuantityInStock(newQuantity);

            // 2. Обновляем информацию о поставщике
            updateSupplierInfo(product, item, order);

            // 3. Сохраняем изменения
            productRepository.save(product);

            // 4. Создаем запись о движении товара
            createStockMovement(product, item, order, warehouseId, performedBy);

            log.debug(messageService.get("purchase.stock.item.updated",
                    product.getSku(), oldQuantity, newQuantity, item.getQuantity()));
        }

        log.info(messageService.get("purchase.stock.processing.complete",
                order.getOrderNumber()));
    }

    /**
     * Обновление информации о товаре от поставщика
     */
    private void updateSupplierInfo(Product product, OrderItem item, Order order) {
        boolean updated = false;

        // Обновляем закупочную цену, если она изменилась
        if (item.getSupplierPrice() != null &&
                (product.getSupplierPrice() == null ||
                        product.getSupplierPrice().compareTo(item.getSupplierPrice()) != 0)) {
            product.setSupplierPrice(item.getSupplierPrice());
            updated = true;
        }

        // Обновляем ID поставщика (основной поставщик)
        if (order.getSupplierId() != null &&
                !order.getSupplierId().equals(product.getSupplierId())) {
            product.setSupplierId(order.getSupplierId());
            updated = true;
        }

        // Обновляем артикул поставщика
        if (item.getSupplierSku() != null &&
                !item.getSupplierSku().equals(product.getSupplierSku())) {
            product.setSupplierSku(item.getSupplierSku());
            updated = true;
        }

        if (updated) {
            log.debug(messageService.get("purchase.stock.supplier.info.updated",
                    product.getId(), order.getSupplierId()));
        }
    }

    /**
     * Создание записи о движении товара
     */
    private void createStockMovement(Product product, OrderItem item,
                                     Order order, Long warehouseId,
                                     Long performedBy) {

        StockMovement movement = movementBuilder.buildMovement(
                product.getId(),
                null,                    // fromCellId = null (поступление)
                null,                    // toCellId = null (общий склад)
                item.getQuantity(),
                MovementType.RECEIPT,
                "PURCHASE",
                performedBy
        );

        movement.setReferenceId(order.getId());
        movement.setNotes(String.format(
                "Purchase order %s, product: %s, supplier: %d",
                order.getOrderNumber(),
                product.getSku(),
                order.getSupplierId()
        ));

        stockMovementRepository.save(movement);

        log.debug(messageService.get("purchase.stock.movement.created",
                movement.getId(), product.getId(), item.getQuantity()));
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
     * Откат изменений при ошибке приемки
     */
    @Transactional
    public void rollbackStockUpdate(Order order, Long performedBy) {
        log.warn(messageService.get("purchase.stock.rollback.start",
                order.getOrderNumber()));

        for (OrderItem item : order.getItems()) {
            Product product = findProductById(item.getProductId());

            int oldQuantity = product.getQuantityInStock();
            int newQuantity = oldQuantity - item.getQuantity();

            if (newQuantity >= 0) {
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                // Создаем движение на списание (откат)
                StockMovement movement = movementBuilder.buildMovement(
                        product.getId(),
                        null,
                        null,
                        item.getQuantity(),
                        MovementType.WRITE_OFF,
                        "ROLLBACK",
                        performedBy
                );
                movement.setReferenceId(order.getId());
                movement.setNotes("Rollback of purchase order " + order.getOrderNumber());
                stockMovementRepository.save(movement);

                log.debug(messageService.get("purchase.stock.rollback.item",
                        product.getSku(), oldQuantity, newQuantity));
            }
        }

        log.info(messageService.get("purchase.stock.rollback.complete",
                order.getOrderNumber()));
    }

    /**
     * Проверка достаточности остатков для отката
     */
    public boolean canRollback(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = findProductById(item.getProductId());
            if (product.getQuantityInStock() < item.getQuantity()) {
                log.warn(messageService.get("purchase.stock.rollback.impossible",
                        product.getSku(), product.getQuantityInStock(), item.getQuantity()));
                return false;
            }
        }
        return true;
    }
}