package ru.galtor85.household_store.service.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cart.CartNotFoundException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.rollback.RollbackNotAllowedException;
import ru.galtor85.household_store.converter.SalesOrderConverter;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.dto.response.order.SalesOrderStatisticsDto;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartStatus;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.processor.sales.SalesOrderProcessor;
import ru.galtor85.household_store.repository.cart.CartRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.order.SalesOrderValidator;
import ru.galtor85.household_store.validator.order.SalesOrderValidationHelper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerSalesOrderService {

    // Репозитории
    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    // Валидаторы
    private final SalesOrderValidator salesOrderValidator;
    private final SalesOrderValidationHelper validationHelper;

    // Процессоры
    private final SalesOrderProcessor salesOrderProcessor;  // ← ДОБАВЛЕНО

    // Конвертеры
    private final SalesOrderConverter salesOrderConverter;

    // Утилиты
    private final MessageService messageService;

    // =========================================================================
    // СОЗДАНИЕ ЗАКАЗА
    // =========================================================================

    /**
     * Создает новый заказ на продажу
     */
    @Transactional
    public SalesOrderDto createSalesOrder(SalesOrderCreateRequest request, Long userId) {

        log.info(messageService.get("sales.order.create.start", userId));

        // 1. Валидация
        salesOrderValidator.validateNotEmpty(request);
        salesOrderValidator.validateUserExists(userId);
        SalesOrderValidator.ProductValidationResult validationResult =
                salesOrderValidator.validateProducts(request.getItems());

        // 2. Создаем заказ через процессор
        SalesOrder order = salesOrderProcessor.createSalesOrder(
                request,
                validationResult.getProducts(),
                validationResult.getPrices(),
                userId
        );

        // 3. Конвертация в DTO
        SalesOrderDto result = salesOrderConverter.toDto(order);

        log.info(messageService.get("sales.order.created.log",
                order.getOrderNumber(), userId, order.getItems().size(), order.getTotalAmount()));

        return result;
    }

    /**
     * Создает заказ из корзины
     */
    @Transactional
    public SalesOrderDto createOrderFromCart(Long userId, String shippingAddress, String promoCode) {

        log.info(messageService.get("sales.order.create.from.cart.start", userId));

        // Получаем корзину пользователя
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(userId));

        // Создаем заказ через процессор
        SalesOrder order = salesOrderProcessor.createOrderFromCart(cart, shippingAddress, userId);

        // Очищаем корзину
        cart.setStatus(CartStatus.COMPLETED);
        cartRepository.save(cart);

        log.info(messageService.get("sales.order.create.from.cart.complete",
                order.getOrderNumber(), userId, order.getTotalAmount()));

        return salesOrderConverter.toDto(order);
    }

    // =========================================================================
    // ПОЛУЧЕНИЕ ЗАКАЗОВ
    // =========================================================================

    /**
     * Получает заказы клиента с фильтрацией
     */
    @Transactional(readOnly = true)
    public Page<SalesOrderDto> getCustomerOrders(Long userId,
                                                 String status,
                                                 String startDate,
                                                 String endDate,
                                                 int page,
                                                 int size) {

        // Парсим параметры
        OrderStatus orderStatus = validationHelper.parseOrderStatus(status);
        LocalDateTime start = validationHelper.parseDate(startDate);
        LocalDateTime end = validationHelper.parseDate(endDate);

        // Валидируем диапазон дат
        validationHelper.validateDateRange(start, end);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Поиск заказов через репозиторий
        Page<SalesOrder> orders = salesOrderRepository.search(userId, orderStatus, start, end, pageable);

        log.debug(messageService.get("manager.orders.fetched.log", orders.getTotalElements()));

        return orders.map(salesOrderConverter::toDto);
    }

    /**
     * Получает заказ по ID
     */
    @Transactional(readOnly = true)
    public SalesOrderDto getSalesOrderById(Long orderId) {
        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        return salesOrderConverter.toDto(order);
    }

    // =========================================================================
    // ОБНОВЛЕНИЕ СТАТУСА
    // =========================================================================

    /**
     * Обновляет статус заказа
     */
    @Transactional
    public SalesOrderDto updateOrderStatus(Long orderId,
                                           String status,
                                           String trackingNumber,
                                           String reason,
                                           Long managerId) {

        OrderStatus newStatus = validationHelper.parseAndValidateOrderStatus(status);
        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        OrderStatus oldStatus = order.getStatus();

        // Проверяем возможность перехода
        validationHelper.validateStatusTransitionForSale(order.getStatus(), newStatus);

        // Обновляем статус
        order.setStatus(newStatus);

        if (trackingNumber != null) {
            order.setTrackingNumber(trackingNumber);
        }

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(LocalDateTime.now());
            order.setCancellationReason(reason);
            // Возвращаем товары на склад
            restoreStockForCancelledOrder(order);
        }

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.status.updated.log",
                orderId, oldStatus, newStatus, managerId));

        return salesOrderConverter.toDto(updatedOrder);
    }

    /**
     * Отменяет заказ
     */
    @Transactional
    public SalesOrderDto cancelOrder(Long orderId, String reason, Long managerId) {
        return updateOrderStatus(orderId, "CANCELLED", null, reason, managerId);
    }

    // =========================================================================
    // ОБНОВЛЕНИЕ ПОЗИЦИЙ
    // =========================================================================

    /**
     * Обновляет цену позиции заказа
     */
    @Transactional
    public SalesOrderDto updateOrderItemPrice(Long orderId,
                                              Long itemId,
                                              BigDecimal newPrice,
                                              String reason,
                                              Long managerId) {

        validationHelper.validatePrice(newPrice);

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING, OrderStatus.PAID);

        SalesOrderItem item = salesOrderValidator.validateOrderItemExists(order, itemId);
        BigDecimal oldPrice = item.getPrice();

        item.setPrice(newPrice);
        item.calculateTotal();
        order.recalculateTotals();

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.price.updated.log",
                orderId, itemId, oldPrice, newPrice, managerId, reason));

        return salesOrderConverter.toDto(updatedOrder);
    }

    /**
     * Обновляет количество позиции заказа
     */
    @Transactional
    public SalesOrderDto updateOrderItemQuantity(Long orderId,
                                                 Long itemId,
                                                 Integer newQuantity,
                                                 String reason,
                                                 Long managerId) {

        validationHelper.validateQuantity(newQuantity);

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING);

        SalesOrderItem item = salesOrderValidator.validateOrderItemExists(order, itemId);
        int oldQuantity = item.getQuantity();
        int quantityDiff = newQuantity - oldQuantity;

        if (quantityDiff > 0) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));
            validationHelper.validateProductAvailability(product, quantityDiff);
        }

        item.setQuantity(newQuantity);
        item.calculateTotal();

        if (newQuantity == 0) {
            order.removeItem(item);
        }

        order.recalculateTotals();
        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.quantity.updated.log",
                orderId, itemId, oldQuantity, newQuantity, managerId, reason));

        return salesOrderConverter.toDto(updatedOrder);
    }

    /**
     * Добавляет товар в заказ
     */
    @Transactional
    public SalesOrderDto addItemToOrder(Long orderId,
                                        Long productId,
                                        Integer quantity,
                                        BigDecimal customPrice,
                                        Long managerId) {

        validationHelper.validatePositiveQuantity(quantity);

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING);
        salesOrderValidator.validateItemNotExists(order, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        validationHelper.validateProductAvailability(product, quantity);

        BigDecimal price = customPrice != null ? customPrice : product.getPrice();

        SalesOrderItem item = SalesOrderItem.builder()
                .salesOrder(order)
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .productName(product.getName())
                .productSku(product.getSku())
                .build();

        order.addItem(item);
        order.recalculateTotals();

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.item.added.log",
                orderId, productId, quantity, managerId));

        return salesOrderConverter.toDto(updatedOrder);
    }

    /**
     * Удаляет товар из заказа
     */
    @Transactional
    public SalesOrderDto removeItemFromOrder(Long orderId, Long itemId, Long managerId) {

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING);

        SalesOrderItem item = salesOrderValidator.validateOrderItemExists(order, itemId);

        order.removeItem(item);
        order.recalculateTotals();

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.item.removed.log",
                orderId, itemId, managerId));

        return salesOrderConverter.toDto(updatedOrder);
    }

    // =========================================================================
    // ОТКАТ СТАТУСА
    // =========================================================================

    /**
     * Откатывает статус заказа
     */
    @Transactional
    public SalesOrderDto rollbackOrderStatus(Long orderId, String reason, Long managerId) {

        log.info(messageService.get("sales.order.rollback.start.log", orderId, managerId, reason));

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        OrderStatus currentStatus = order.getStatus();

        // Проверяем возможность отката
        if (!currentStatus.isRollbackAllowedForSale()) {
            throw new RollbackNotAllowedException(currentStatus);
        }

        OrderStatus targetStatus = currentStatus.getRollbackTargetForSale();
        OrderStatus oldStatus = order.getStatus();

        // Выполняем откат
        performRollbackActions(order, oldStatus, targetStatus, reason, managerId);
        order.setStatus(targetStatus);
        addRollbackNote(order, oldStatus, targetStatus, reason, managerId);

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.rollback.success.log",
                orderId, oldStatus, targetStatus, managerId, reason));

        return salesOrderConverter.toDto(updatedOrder);
    }

    // =========================================================================
    // ЗАМЕТКИ
    // =========================================================================

    /**
     * Добавляет заметку к заказу
     */
    @Transactional
    public SalesOrderDto addOrderNote(Long orderId, String note, Long managerId) {

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String newNote = String.format("[%s] Manager %s: %s", timestamp, managerId, note);

        if (order.getNotes() == null || order.getNotes().isEmpty()) {
            order.setNotes(newNote);
        } else {
            order.setNotes(order.getNotes() + "\n" + newNote);
        }

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.note.added.log", orderId, managerId));

        return salesOrderConverter.toDto(updatedOrder);
    }

    // =========================================================================
    // СТАТИСТИКА
    // =========================================================================

    /**
     * Получает статистику заказов
     */
    @Transactional(readOnly = true)
    public SalesOrderStatisticsDto getOrderStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfWeek = now.minusWeeks(1);
        LocalDateTime startOfMonth = now.minusMonths(1);

        return SalesOrderStatisticsDto.builder()
                .totalOrdersToday(salesOrderRepository.countByCreatedAtBetween(startOfDay, now))
                .totalOrdersWeek(salesOrderRepository.countByCreatedAtBetween(startOfWeek, now))
                .totalOrdersMonth(salesOrderRepository.countByCreatedAtBetween(startOfMonth, now))
                .revenueToday(salesOrderRepository.sumTotalAmountByCreatedAtBetween(startOfDay, now))
                .revenueWeek(salesOrderRepository.sumTotalAmountByCreatedAtBetween(startOfWeek, now))
                .revenueMonth(salesOrderRepository.sumTotalAmountByCreatedAtBetween(startOfMonth, now))
                .build();
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Возвращает товары на склад при отмене заказа
     */
    private void restoreStockForCancelledOrder(SalesOrder order) {
        for (SalesOrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int newQuantity = product.getQuantityInStock() + item.getQuantity();
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                log.debug(messageService.get("sales.order.stock.restored",
                        item.getProductId(), item.getQuantity()));
            });
        }
    }

    /**
     * Выполняет действия при откате статуса
     */
    private void performRollbackActions(SalesOrder order,
                                        OrderStatus oldStatus,
                                        OrderStatus newStatus,
                                        String reason,
                                        Long managerId) {
        switch (oldStatus) {
            case PAID:
                reversePayment(order);
                break;
            case PROCESSING:
                releaseReservedStock(order);
                break;
            case SHIPPED:
                cancelShipment(order);
                order.setTrackingNumber(null);
                break;
            case DELIVERED:
                order.setDeliveredAt(null);
                break;
        }
    }

    /**
     * Добавляет заметку об откате
     */
    private void addRollbackNote(SalesOrder order,
                                 OrderStatus oldStatus,
                                 OrderStatus newStatus,
                                 String reason,
                                 Long managerId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String rollbackNote = String.format(
                "[%s] ROLLBACK: %s → %s by manager %s. Reason: %s",
                timestamp, oldStatus, newStatus, managerId, reason
        );

        if (order.getNotes() == null || order.getNotes().isEmpty()) {
            order.setNotes(rollbackNote);
        } else {
            order.setNotes(order.getNotes() + "\n" + rollbackNote);
        }
    }

    private void reversePayment(SalesOrder order) {
        log.debug(messageService.get("sales.order.rollback.payment.reversed", order.getId()));
    }

    private void releaseReservedStock(SalesOrder order) {
        log.debug(messageService.get("sales.order.rollback.stock.released", order.getId()));
    }

    private void cancelShipment(SalesOrder order) {
        log.debug(messageService.get("sales.order.rollback.shipment.cancelled", order.getId()));
    }
}