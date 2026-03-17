package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.OrderItemBuilder;
import ru.galtor85.household_store.builder.OrderUpdateBuilder;
import ru.galtor85.household_store.dto.OrderDto;
import ru.galtor85.household_store.dto.OrderStatisticsDto;
import ru.galtor85.household_store.dto.params.OrderQueryParams;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.mapper.OrderMapper;
import ru.galtor85.household_store.processor.OrderFilterProcessor;
import ru.galtor85.household_store.processor.OrderQueryBuilder;

import ru.galtor85.household_store.processor.StockProcessor;
import ru.galtor85.household_store.repository.OrderItemRepository;
import ru.galtor85.household_store.repository.OrderRepository;
import ru.galtor85.household_store.util.OrderDateParser;
import ru.galtor85.household_store.util.OrderEntityFinder;
import ru.galtor85.household_store.util.OrderValidationHelper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final MessageService messageService;

    // Утилиты
    private final OrderEntityFinder entityFinder;
    private final OrderValidationHelper validationHelper;
    private final OrderDateParser dateParser;

    // Билдеры
    private final OrderItemBuilder orderItemBuilder;
    private final OrderUpdateBuilder orderUpdateBuilder;

    // Процессоры
    private final OrderFilterProcessor filterProcessor;
    private final OrderQueryBuilder queryBuilder;
    private final StockProcessor stockProcessor;

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

        Order order = entityFinder.findCustomerOrderById(orderId);
        return orderMapper.toDto(order, locale);
    }

    // ========== UPDATE ORDER STATUS ==========

    @Transactional
    public OrderDto updateOrderStatus(Long orderId, String status, String trackingNumber,
                                      String reason, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        OrderStatus newStatus = validationHelper.parseAndValidateOrderStatus(status);
        Order order = entityFinder.findOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();

        validationHelper.validateStatusTransition(order, newStatus);

        orderUpdateBuilder.updateOrderStatus(order, newStatus, trackingNumber, reason);

        if (newStatus == OrderStatus.CANCELLED) {
            stockProcessor.restoreStockForCancelledOrder(order);
        }

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.order.status.updated.log",
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

        Order order = entityFinder.findOrderById(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING, OrderStatus.PAID);

        OrderItem item = entityFinder.findOrderItem(order, itemId);
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

        Order order = entityFinder.findOrderById(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING);

        OrderItem item = entityFinder.findOrderItem(order, itemId);
        int oldQuantity = item.getQuantity();
        int quantityDiff = newQuantity - oldQuantity;

        if (quantityDiff > 0) {
            Product product = entityFinder.findProductById(item.getProductId());
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

        Order order = entityFinder.findOrderById(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING);
        validationHelper.validateItemNotExists(order, productId);

        Product product = entityFinder.findProductById(productId);
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

        Order order = entityFinder.findOrderById(orderId);
        validationHelper.validateOrderModifiable(order, OrderStatus.PENDING);

        OrderItem item = entityFinder.findOrderItem(order, itemId);

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

    // ========== ORDER NOTES ==========

    @Transactional
    public OrderDto addOrderNote(Long orderId, String note, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = entityFinder.findOrderById(orderId);

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
}