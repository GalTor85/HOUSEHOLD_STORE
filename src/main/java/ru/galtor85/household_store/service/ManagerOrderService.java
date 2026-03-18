package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.builder.OrderItemBuilder;
import ru.galtor85.household_store.builder.OrderUpdateBuilder;
import ru.galtor85.household_store.dto.OrderDto;
import ru.galtor85.household_store.dto.OrderStatisticsDto;
import ru.galtor85.household_store.dto.params.OrderQueryParams;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.mapper.OrderMapper;
import ru.galtor85.household_store.processor.OrderFilterProcessor;
import ru.galtor85.household_store.processor.OrderQueryBuilder;
import ru.galtor85.household_store.processor.PurchaseStockProcessor;
import ru.galtor85.household_store.processor.StockProcessor;
import ru.galtor85.household_store.repository.OrderItemRepository;
import ru.galtor85.household_store.repository.OrderRepository;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.util.EntityFinder;
import ru.galtor85.household_store.util.OrderDateParser;
import ru.galtor85.household_store.util.OrderEntityFinder;
import ru.galtor85.household_store.util.OrderValidationHelper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final MessageService messageService;
    private final ProductRepository productRepository;
    private final EntityFinder entityFinder;
    private final WarehouseResolver warehouseResolver;

    // Утилиты
    private final OrderEntityFinder orderEntityFinder;
    private final OrderValidationHelper validationHelper;
    private final OrderDateParser dateParser;

    // Билдеры
    private final OrderItemBuilder orderItemBuilder;
    private final OrderUpdateBuilder orderUpdateBuilder;

    // Процессоры
    private final OrderFilterProcessor filterProcessor;
    private final OrderQueryBuilder queryBuilder;
    private final StockProcessor stockProcessor;
    private final PurchaseStockProcessor purchaseStockProcessor; // ДОБАВЛЕНО
    private final OrderValidationHelper orderValidationHelper;

    // ========== GET CUSTOMER ORDERS ==========

    @Transactional(readOnly = true)
    public Page<OrderDto> getCustomerOrders(String status, Long customerId,
                                            String startDate, String endDate,
                                            int page, int size, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        OrderStatus orderStatus = validationHelper.parseOrderStatus(status);
        LocalDateTime start = dateParser.parseDate(startDate);
        LocalDateTime end = dateParser.parseDate(endDate);
        dateParser.validateDateRange(start, end, startDate, endDate);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        OrderQueryParams params = new OrderQueryParams(customerId, orderStatus, start, end);
        Page<Order> ordersPage = queryBuilder.executeQuery(params, pageable);

        Page<Order> resultPage = filterProcessor.filterCustomerOrders(ordersPage, pageable);

        log.debug(messageService.get("manager.order.fetched.log", resultPage.getTotalElements()));

        Locale finalLocale = locale;
        return resultPage.map(order -> orderMapper.toDto(order, finalLocale));
    }

    // ========== GET SINGLE ORDER ==========

    @Transactional(readOnly = true)
    public OrderDto getCustomerOrderById(Long orderId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = orderEntityFinder.findCustomerOrderById(orderId);
        return orderMapper.toDto(order, locale);
    }

    @Transactional(readOnly = true)
    public OrderDto getPurchaseOrderById(Long orderId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = entityFinder.findPurchaseOrderById(orderId);
        return orderMapper.toDto(order, locale);
    }

    // ========== UPDATE ORDER STATUS ==========

    @Transactional
    public OrderDto updateOrderStatus(Long orderId, String status, String trackingNumber,
                                      String reason, Long managerId, Long forcedWarehouseId,
                                      Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        OrderStatus newStatus = validationHelper.parseAndValidateOrderStatus(status);
        Order order = entityFinder.findOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();

        validationHelper.validateStatusTransition(order, newStatus);

        // Определяем склад с возможностью принудительного указания
        Long warehouseId = warehouseResolver.resolveWarehouseId(order, forcedWarehouseId);

        orderUpdateBuilder.updateOrderStatus(order, newStatus, trackingNumber, reason,
                managerId, warehouseId);

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.order.status.updated.log",
                orderId, oldStatus, newStatus, managerId
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    @Transactional
    public OrderDto updateOrderStatus(Long orderId, String status, String trackingNumber,
                                      String reason, Long managerId, Locale locale) {
        return updateOrderStatus(orderId, status, trackingNumber, reason, managerId, null, locale);
    }

    @Transactional
    public OrderDto updatePurchaseOrderStatus(Long orderId, OrderStatus newStatus,
                                              String trackingNumber, String reason,
                                              Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = entityFinder.findPurchaseOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();

        orderValidationHelper.validatePurchaseStatusTransition(order, newStatus);

        orderUpdateBuilder.updateOrderStatus(order, newStatus, trackingNumber, reason);

        if (newStatus == OrderStatus.DELIVERED) {
            // Обновляем остатки при получении закупки
            purchaseStockProcessor.processStockUpdate(order, null, managerId);
            order.setDeliveredAt(LocalDateTime.now());
        }

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.purchase.status.updated.log",
                orderId, oldStatus, newStatus, managerId
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    @Transactional
    public OrderDto cancelOrder(Long orderId, String reason, Long managerId, Locale locale) {
        return updateOrderStatus(orderId, "CANCELLED", null, reason, managerId, locale);
    }

    // ========== UPDATE ORDER ITEMS ==========

    @Transactional
    public OrderDto updateOrderItemPrice(Long orderId, Long itemId, BigDecimal newPrice,
                                         String reason, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        validationHelper.validatePrice(newPrice);

        Order order = orderEntityFinder.findOrderById(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING, OrderStatus.PAID);

        OrderItem item = orderEntityFinder.findOrderItem(order, itemId);
        BigDecimal oldPrice = item.getPrice();

        item.setPrice(newPrice);
        item.calculateTotal();
        order.recalculateTotals();

        Order updatedOrder = orderRepository.save(order);

        String reasonText = reason != null ? reason : messageService.get("manager.order.reason.default");

        log.info(messageService.get(
                "manager.order.price.updated.log",
                orderId, itemId, oldPrice, newPrice, managerId, reasonText
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    @Transactional
    public OrderDto updateOrderItemQuantity(Long orderId, Long itemId, Integer newQuantity,
                                            String reason, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        validationHelper.validateQuantity(newQuantity);

        Order order = orderEntityFinder.findOrderById(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING);

        OrderItem item = orderEntityFinder.findOrderItem(order, itemId);
        int oldQuantity = item.getQuantity();
        int quantityDiff = newQuantity - oldQuantity;

        if (quantityDiff > 0) {
            Product product = orderEntityFinder.findProductById(item.getProductId());
            validationHelper.validateProductAvailability(product, quantityDiff);
        }

        item.setQuantity(newQuantity);
        item.calculateTotal();

        if (newQuantity == 0) {
            order.removeItem(item);
            orderItemRepository.delete(item);
        }

        order.recalculateTotals();
        Order updatedOrder = orderRepository.save(order);

        String reasonText = reason != null ? reason : messageService.get("manager.order.reason.default");

        log.info(messageService.get(
                "manager.order.quantity.updated.log",
                orderId, itemId, oldQuantity, newQuantity, managerId, reasonText
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    @Transactional
    public OrderDto addItemToOrder(Long orderId, Long productId, Integer quantity,
                                   BigDecimal customPrice, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        validationHelper.validatePositiveQuantity(quantity);

        Order order = orderEntityFinder.findOrderById(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING);
        validationHelper.validateItemNotExists(order, productId);

        Product product = orderEntityFinder.findProductById(productId);
        validationHelper.validateProductAvailability(product, quantity);

        OrderItem item = orderItemBuilder.buildFromProductWithCustomPrice(
                productId, quantity, customPrice, product);

        order.addItem(item);
        order.recalculateTotals();

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.order.item.added.log",
                orderId, productId, quantity, managerId
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    @Transactional
    public OrderDto removeItemFromOrder(Long orderId, Long itemId, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = orderEntityFinder.findOrderById(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING);

        OrderItem item = orderEntityFinder.findOrderItem(order, itemId);

        order.removeItem(item);
        orderItemRepository.delete(item);
        order.recalculateTotals();

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.order.item.removed.log",
                orderId, itemId, managerId
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    // ========== ROLLBACK ORDER STATUS ==========

    @Transactional
    public OrderDto rollbackOrderStatus(Long orderId, String reason, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.info(messageService.get("manager.order.rollback.start.log", orderId, managerId, reason));

        Order order = orderEntityFinder.findOrderById(orderId);
        OrderStatus currentStatus = order.getStatus();

        validateRollbackPossibility(order, locale);
        OrderStatus targetStatus = determineRollbackTargetStatus(currentStatus);
        OrderStatus oldStatus = order.getStatus();

        performRollbackActions(order, currentStatus, targetStatus, reason, managerId, locale);
        order.setStatus(targetStatus);
        addRollbackNote(order, oldStatus, targetStatus, reason, managerId, locale);

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.order.rollback.success.log",
                orderId, oldStatus, targetStatus, managerId, reason
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    // ========== ORDER NOTES ==========

    @Transactional
    public OrderDto addOrderNote(Long orderId, String note, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = orderEntityFinder.findOrderById(orderId);

        String newNote = orderUpdateBuilder.formatOrderNote(note, managerId);

        if (order.getNotes() == null || order.getNotes().isEmpty()) {
            order.setNotes(newNote);
        } else {
            order.setNotes(order.getNotes() + "\n" + newNote);
        }

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get("manager.order.note.added.log", orderId, managerId));

        return orderMapper.toDto(updatedOrder, locale);
    }

    // ========== ORDER STATISTICS ==========

    @Transactional(readOnly = true)
    public OrderStatisticsDto getOrderStatistics(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfWeek = now.minusWeeks(1);
        LocalDateTime startOfMonth = now.minusMonths(1);

        return OrderStatisticsDto.builder()
                .totalOrdersToday(orderRepository.countOrdersByDateRange(startOfDay, now))
                .totalOrdersWeek(orderRepository.countOrdersByDateRange(startOfWeek, now))
                .totalOrdersMonth(orderRepository.countOrdersByDateRange(startOfMonth, now))
                .revenueToday(orderRepository.sumRevenueByDateRange(startOfDay, now))
                .revenueWeek(orderRepository.sumRevenueByDateRange(startOfWeek, now))
                .revenueMonth(orderRepository.sumRevenueByDateRange(startOfMonth, now))
                .build();
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Page<Order> findOrdersByParams(Long customerId, OrderStatus status,
                                           LocalDateTime start, LocalDateTime end,
                                           Pageable pageable) {
        boolean hasCustomerId = customerId != null;
        boolean hasStatus = status != null;
        boolean hasStart = start != null;
        boolean hasEnd = end != null;

        if (hasCustomerId && hasStatus && hasStart && hasEnd) {
            return orderRepository.findByUserIdAndStatusAndCreatedAtBetween(
                    customerId, status, start, end, pageable);
        }
        if (hasCustomerId && hasStatus && hasStart) {
            return orderRepository.findByUserIdAndStatusAndCreatedAtAfter(
                    customerId, status, start, pageable);
        }
        if (hasCustomerId && hasStatus && hasEnd) {
            return orderRepository.findByUserIdAndStatusAndCreatedAtBefore(
                    customerId, status, end, pageable);
        }
        if (hasCustomerId && hasStart && hasEnd) {
            return orderRepository.findByUserIdAndCreatedAtBetween(
                    customerId, start, end, pageable);
        }
        if (hasStatus && hasStart && hasEnd) {
            return orderRepository.findByStatusAndCreatedAtBetween(
                    status, start, end, pageable);
        }
        if (hasCustomerId && hasStatus) {
            return orderRepository.findByUserIdAndStatus(
                    customerId, status, pageable);
        }
        if (hasCustomerId && hasStart) {
            return orderRepository.findByUserIdAndCreatedAtAfter(
                    customerId, start, pageable);
        }
        if (hasCustomerId && hasEnd) {
            return orderRepository.findByUserIdAndCreatedAtBefore(
                    customerId, end, pageable);
        }
        if (hasStatus && hasStart) {
            return orderRepository.findByStatusAndCreatedAtAfter(
                    status, start, pageable);
        }
        if (hasStatus && hasEnd) {
            return orderRepository.findByStatusAndCreatedAtBefore(
                    status, end, pageable);
        }
        if (hasStart && hasEnd) {
            return orderRepository.findByCreatedAtBetween(
                    start, end, pageable);
        }
        if (hasCustomerId) {
            return orderRepository.findByUserId(customerId, pageable);
        }
        if (hasStatus) {
            return orderRepository.findByStatus(status, pageable);
        }
        if (hasStart) {
            return orderRepository.findByCreatedAtAfter(start, pageable);
        }
        if (hasEnd) {
            return orderRepository.findByCreatedAtBefore(end, pageable);
        }
        return orderRepository.findAll(pageable);
    }

    // ========== ROLLBACK HELPER METHODS ==========

    private void validateRollbackPossibility(Order order, Locale locale) {
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.COMPLETED ||
                status == OrderStatus.CANCELLED ||
                status == OrderStatus.REFUNDED ||
                status == OrderStatus.RETURNED) {
            log.warn(messageService.get("manager.order.rollback.error.final", status));
            throw new RollbackFinalStatusException(status);
        }

        switch (status) {
            case SHIPPED:
                if (order.getTrackingNumber() != null && isWithCourier(order)) {
                    log.warn(messageService.get("manager.order.rollback.error.shipped.with.courier"));
                    throw new RollbackInvalidStateException(order.getId(), status,
                            "Order already with courier");
                }
                break;
            case DELIVERED:
                if (order.getDeliveredAt() != null &&
                        order.getDeliveredAt().plusHours(24).isBefore(LocalDateTime.now())) {
                    log.warn(messageService.get("manager.order.rollback.error.delivered.timeout"));
                    throw new RollbackTimeoutException(order.getId(), status,
                            order.getDeliveredAt());
                }
                break;
        }
    }

    private OrderStatus determineRollbackTargetStatus(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PAID -> OrderStatus.PENDING;
            case PROCESSING -> OrderStatus.PAID;
            case SHIPPED -> OrderStatus.PROCESSING;
            case DELIVERED -> OrderStatus.SHIPPED;
            default -> throw new RollbackInvalidTransitionException(currentStatus, currentStatus);
        };
    }

    private void performRollbackActions(Order order, OrderStatus oldStatus,
                                        OrderStatus newStatus, String reason,
                                        Long managerId, Locale locale) {
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

    private void addRollbackNote(Order order, OrderStatus oldStatus,
                                 OrderStatus newStatus, String reason,
                                 Long managerId, Locale locale) {
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

    private boolean isWithCourier(Order order) {
        // TODO: Реализовать проверку через службу доставки
        return false;
    }

    private void reversePayment(Order order) {
        log.debug(messageService.get("manager.order.rollback.payment.reversed", order.getId()));
    }

    private void releaseReservedStock(Order order) {
        log.debug(messageService.get("manager.order.rollback.stock.released", order.getId()));
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                log.debug(messageService.get("manager.order.rollback.item.released",
                        item.getProductId(), item.getQuantity()));
            });
        }
    }

    private void cancelShipment(Order order) {
        log.debug(messageService.get("manager.order.rollback.shipment.cancelled", order.getId()));
    }

    /**
     * Обновление остатков при получении закупки
     */
    @Transactional
    public void updateStockFromPurchase(Order order, Long warehouseId, Long performedBy) {
        log.info(messageService.get("purchase.stock.update.start",
                order.getOrderNumber(), order.getItems().size(), performedBy));

        purchaseStockProcessor.processStockUpdate(order, warehouseId, performedBy);

        if (order.getStatus() != OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        log.info(messageService.get("purchase.stock.update.complete",
                order.getOrderNumber()));
    }
}