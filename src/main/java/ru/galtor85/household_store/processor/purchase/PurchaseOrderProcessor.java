package ru.galtor85.household_store.processor.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cell.CellNotFoundException;
import ru.galtor85.household_store.builder.order.PurchaseOrderBuilder;
import ru.galtor85.household_store.dto.request.order.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.request.order.ReceiveAndStockRequest;
import ru.galtor85.household_store.dto.common.ReceiveStockItem;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.processor.invoice.InvoiceAutoCreationProcessor;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.stock.StockMovementRepository;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.batch.BatchNumberGenerator;
import ru.galtor85.household_store.util.entity.EntityFinder;
import ru.galtor85.household_store.validator.order.PurchaseOrderValidator;
import ru.galtor85.household_store.validator.stock.StockValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderProcessor {

    // Репозитории
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StorageCellRepository storageCellRepository;

    // Билдеры
    private final PurchaseOrderBuilder builder;

    // Процессоры
    private final InvoiceAutoCreationProcessor invoiceAutoCreationProcessor;

    // Валидаторы
    private final PurchaseOrderValidator validator;
    private final StockValidator stockValidator;

    // Утилиты
    private final EntityFinder entityFinder;
    private final BatchNumberGenerator batchNumberGenerator;
    private final MessageService messageService;

    // =========================================================================
    // СОЗДАНИЕ ЗАКАЗА НА ЗАКУПКУ
    // =========================================================================

    /**
     * Создает заказ на закупку
     */
    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderCreateRequest request,
                                             Supplier supplier,
                                             List<Product> products,
                                             List<BigDecimal> prices,
                                             Long managerId) {

        log.info(messageService.get("purchase.order.processor.create.start",
                request.getSupplierId(), managerId));

        // 1. Валидация
        validator.validateCreateRequest(request);
        validator.validateSupplierActive(supplier);

        // 2. Создаем заказ через билдер
        PurchaseOrder order = builder.buildOrder(request, managerId);

        // 3. Создаем позиции через билдер
        List<PurchaseOrderItem> items = builder.buildOrderItems(
                order,
                request.getItems(),
                products,
                prices
        );

        // 4. Рассчитываем сумму
        BigDecimal totalAmount = builder.calculateTotalAmount(items);

        // 5. Устанавливаем позиции и суммы
        order.setItems(items);
        order.setSubtotal(totalAmount);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        // 6. Сохраняем заказ
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        // 7. ✅ АВТОМАТИЧЕСКОЕ СОЗДАНИЕ СЧЕТА
        Invoice invoice = invoiceAutoCreationProcessor.createInvoiceForOrder(order, managerId);
        if (invoice != null) {
            savedOrder.addInvoice(invoice);
            purchaseOrderRepository.save(savedOrder);
            log.info(messageService.get("purchase.order.processor.invoice.created",
                    invoice.getInvoiceNumber(), savedOrder.getOrderNumber()));
        }

        log.info(messageService.get("purchase.order.processor.create.complete",
                savedOrder.getOrderNumber(), managerId, items.size(), totalAmount));

        return savedOrder;
    }

    // =========================================================================
    // ПРИЕМКА ЗАКАЗА (БЕЗ ЯЧЕЕК)
    // =========================================================================

    /**
     * Приемка заказа без привязки к ячейкам
     */
    @Transactional
    public PurchaseOrder receiveOrder(PurchaseOrder order,
                                      ReceiveAndStockRequest request,
                                      Long managerId) {

        log.info(messageService.get("purchase.order.processor.receive.start",
                order.getOrderNumber(), managerId));

        // 1. Валидация
        validator.validateOrderForReceiving(order);
        validator.validateWarehouse(request.getWarehouseId());

        List<StockMovement> movements = new ArrayList<>();

        // 2. Обрабатываем каждую позицию
        for (ReceiveStockItem item : request.getItems()) {
            PurchaseOrderItem orderItem = findOrderItem(order, item.getProductId());

            if (orderItem == null) {
                log.warn(messageService.get("purchase.order.processor.item.not.found",
                        item.getProductId(), order.getId()));
                continue;
            }

            Product product = entityFinder.findProductById(orderItem.getProductId());

            // Расчет принимаемого количества
            int alreadyReceived = orderItem.getReceivedQuantity() != null ? orderItem.getReceivedQuantity() : 0;
            int remainingToReceive = orderItem.getQuantity() - alreadyReceived;
            int receivingQuantity = Math.min(item.getQuantity(), remainingToReceive);

            if (receivingQuantity <= 0) {
                log.info(messageService.get("purchase.order.processor.item.already.received",
                        product.getSku()));
                continue;
            }

            // Обновляем количество принятого
            orderItem.setReceivedQuantity(alreadyReceived + receivingQuantity);

            // Обновляем остатки на складе
            updateProductStock(product, receivingQuantity, request.getWarehouseId());

            // Обновляем остаток в продукте
            product.setQuantityInStock(product.getQuantityInStock() + receivingQuantity);
            productRepository.save(product);

            // Создаем движение товара
            StockMovement movement = createStockMovement(
                    product, orderItem, order, request.getWarehouseId(), managerId,
                    item.getBatchNumber(), receivingQuantity
            );
            movements.add(stockMovementRepository.save(movement));

            log.debug(messageService.get("purchase.order.processor.item.received",
                    product.getSku(), receivingQuantity, orderItem.getReceivedQuantity()));
        }

        // 3. Обновляем статус заказа
        updateOrderStatusAfterReceiving(order);

        // 4. Сохраняем
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        log.info(messageService.get("purchase.order.processor.receive.complete",
                order.getOrderNumber(), movements.size(),
                order.getStatus() == OrderStatus.DELIVERED ? "FULLY" : "PARTIALLY"));

        return savedOrder;
    }

    // =========================================================================
    // ПРИЕМКА ЗАКАЗА С РАЗМЕЩЕНИЕМ ПО ЯЧЕЙКАМ
    // =========================================================================

    /**
     * Приемка заказа с размещением по ячейкам
     */
    @Transactional
    public PurchaseOrder receiveOrderWithCells(PurchaseOrder order,
                                               ReceiveAndStockRequest request,
                                               Long managerId) {

        log.info(messageService.get("purchase.order.processor.receive.with.cells.start",
                order.getOrderNumber(), request.getWarehouseId(), managerId));

        // 1. Валидация
        validator.validateOrderForReceiving(order);
        validator.validateWarehouse(request.getWarehouseId());

        List<StockMovement> movements = new ArrayList<>();
        List<CellPlacementInfo> placements = new ArrayList<>();

        // 2. Обрабатываем каждую позицию
        for (ReceiveStockItem item : request.getItems()) {
            PurchaseOrderItem orderItem = findOrderItem(order, item.getProductId());

            if (orderItem == null) {
                log.warn(messageService.get("purchase.order.processor.item.not.found",
                        item.getProductId(), order.getId()));
                continue;
            }

            Product product = entityFinder.findProductById(orderItem.getProductId());

            // Расчет принимаемого количества
            int alreadyReceived = orderItem.getReceivedQuantity() != null ? orderItem.getReceivedQuantity() : 0;
            int remainingToReceive = orderItem.getQuantity() - alreadyReceived;
            int receivingQuantity = Math.min(item.getQuantity(), remainingToReceive);

            if (receivingQuantity <= 0) {
                continue;
            }

            // Определяем ячейку для размещения
            StorageCell cell = determineCell(item, product, request.getWarehouseId());

            // Размещаем товар в ячейке
            assignProductToCell(cell, product, receivingQuantity, managerId);

            // Обновляем количество принятого
            orderItem.setReceivedQuantity(alreadyReceived + receivingQuantity);

            // Обновляем остатки
            product.setQuantityInStock(product.getQuantityInStock() + receivingQuantity);
            productRepository.save(product);
            updateProductStock(product, receivingQuantity, request.getWarehouseId());

            // Создаем движение
            StockMovement movement = createStockMovementWithCell(
                    product, orderItem, order, cell, request.getWarehouseId(),
                    managerId, item.getBatchNumber(), receivingQuantity
            );
            movements.add(stockMovementRepository.save(movement));

            placements.add(new CellPlacementInfo(
                    product.getId(),
                    product.getSku(),
                    cell.getId(),
                    cell.getCode(),
                    receivingQuantity
            ));
        }

        // 3. Обновляем статус заказа
        updateOrderStatusAfterReceiving(order);

        // 4. Сохраняем
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        log.info(messageService.get("purchase.order.processor.receive.with.cells.complete",
                order.getOrderNumber(), movements.size(), placements.size(),
                order.getStatus() == OrderStatus.DELIVERED ? "FULLY" : "PARTIALLY"));

        return savedOrder;
    }

    // =========================================================================
    // ОТМЕНА ЗАКАЗА
    // =========================================================================

    /**
     * Отменяет заказ на закупку
     */
    @Transactional
    public PurchaseOrder cancelOrder(PurchaseOrder order, String reason, Long cancelledBy) {
        log.info(messageService.get("purchase.order.processor.cancel.start",
                order.getOrderNumber(), reason));

        // 1. Проверка возможности отмены
        validator.validateOrderCancellable(order);

        // 2. Обновляем статус
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        // 3. Отменяем все неоплаченные счета
        for (Invoice invoice : order.getInvoices()) {
            if (invoice.getStatus() == InvoiceStatus.PENDING) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
            }
        }

        // 4. Сохраняем
        PurchaseOrder cancelled = purchaseOrderRepository.save(order);

        log.info(messageService.get("purchase.order.processor.cancel.complete",
                order.getOrderNumber(), cancelledBy));

        return cancelled;
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Находит позицию заказа по ID продукта
     */
    private PurchaseOrderItem findOrderItem(PurchaseOrder order, Long productId) {
        return order.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Обновляет остатки в product_stocks
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
        } else {
            stock.setQuantity(stock.getQuantity() + quantity);
            stock.setAvailableQuantity(stock.getQuantity() -
                    (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0));
            stock.setUpdatedAt(LocalDateTime.now());
        }

        productStockRepository.save(stock);
    }

    /**
     * Обновляет статус заказа после приемки
     */
    private void updateOrderStatusAfterReceiving(PurchaseOrder order) {
        boolean allReceived = order.getItems().stream()
                .allMatch(item -> {
                    int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
                    return received >= item.getQuantity();
                });

        if (allReceived) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setActualDelivery(LocalDate.now());
        } else {
            order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
        }
    }

    /**
     * Определяет ячейку для размещения товара
     */
    private StorageCell determineCell(ReceiveStockItem item, Product product, Long warehouseId) {
        if (item.getCellId() != null) {
            return storageCellRepository.findById(item.getCellId())
                    .orElseThrow(() -> new CellNotFoundException(item.getCellId()));
        } else if (item.getCellCode() != null) {
            return storageCellRepository.findByCodeAndWarehouseId(item.getCellCode(), warehouseId)
                    .orElseThrow(() -> new CellNotFoundException(item.getCellCode(), warehouseId));
        } else {
            // TODO: Автоматический подбор ячейки через CellAutoSelector
            throw new IllegalArgumentException(
                    messageService.get("purchase.order.processor.cell.not.specified")
            );
        }
    }

    /**
     * Размещает товар в ячейке
     */
    private void assignProductToCell(StorageCell cell, Product product, int quantity, Long assignedBy) {
        cell.setCurrentProductId(product.getId());
        cell.setCurrentQuantity(quantity);
        cell.setIsOccupied(true);
        cell.setLastInventoryDate(LocalDateTime.now());
        storageCellRepository.save(cell);
    }

    /**
     * Создает движение товара
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
        }

        return StockMovement.builder()
                .productId(product.getId())
                .warehouseId(warehouseId)
                .quantity(quantity)
                .movementType(MovementType.RECEIPT)
                .referenceType("PURCHASE")
                .referenceId(order.getId())
                .referenceNumber(order.getOrderNumber())
                .performedBy(performedBy)
                .notes(messageService.get("purchase.order.processor.movement.notes",
                        order.getOrderNumber()))
                .batchNumber(batchNumber)
                .documentNumber(order.getOrderNumber())
                .build();
    }

    /**
     * Создает движение товара с ячейкой
     */
    private StockMovement createStockMovementWithCell(Product product,
                                                      PurchaseOrderItem item,
                                                      PurchaseOrder order,
                                                      StorageCell cell,
                                                      Long warehouseId,
                                                      Long performedBy,
                                                      String batchNumber,
                                                      int quantity) {

        StockMovement movement = createStockMovement(product, item, order, warehouseId,
                performedBy, batchNumber, quantity);
        movement.setToCellId(cell.getId());
        return movement;
    }

    // =========================================================================
    // ВНУТРЕННИЕ КЛАССЫ
    // =========================================================================

    @lombok.Value
    public static class CellPlacementInfo {
        Long productId;
        String productSku;
        Long cellId;
        String cellCode;
        int quantity;
    }
}