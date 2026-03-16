package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.OrderDto;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.mapper.OrderMapper;
import ru.galtor85.household_store.repository.OrderItemRepository;
import ru.galtor85.household_store.repository.OrderRepository;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.PurchaseOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final MessageService messageService;

    // ========== GET CUSTOMER ORDERS ==========

    @Transactional(readOnly = true)
    public Page<OrderDto> getCustomerOrders(String status, Long customerId,
                                            String startDate, String endDate,
                                            int page, int size, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // 1. ПРОВЕРКА ПАРАМЕТРОВ СТАТУСА
        OrderStatus orderStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                orderStatus = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn(messageService.get("manager.order.log.invalid.status", status));
                return Page.empty();
            }
        }

        // 2. ПАРСИНГ ДАТ
        LocalDateTime start = parseDate(startDate);
        LocalDateTime end = parseDate(endDate);

        // 3. ПРОВЕРКА ДИАПАЗОНА ДАТ
        if (start != null && end != null && start.isAfter(end)) {
            log.warn(messageService.get("manager.order.log.date.range", startDate, endDate));
            throw new InvalidDateRangeException(startDate, endDate);
        }

        // 4. СОЗДАНИЕ ЗАПРОСА В ЗАВИСИМОСТИ ОТ ПАРАМЕТРОВ
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> ordersPage;

        // Определяем, какие параметры переданы
        boolean hasCustomerId = customerId != null;
        boolean hasStatus = orderStatus != null;
        boolean hasStartDate = start != null;
        boolean hasEndDate = end != null;

        // Выбираем соответствующий метод репозитория
        if (hasCustomerId && hasStatus && hasStartDate && hasEndDate) {
            ordersPage = orderRepository.findByUserIdAndStatusAndCreatedAtBetween(
                    customerId, orderStatus, start, end, pageable);
        } else if (hasCustomerId && hasStatus && hasStartDate) {
            ordersPage = orderRepository.findByUserIdAndStatusAndCreatedAtAfter(
                    customerId, orderStatus, start, pageable);
        } else if (hasCustomerId && hasStatus && hasEndDate) {
            ordersPage = orderRepository.findByUserIdAndStatusAndCreatedAtBefore(
                    customerId, orderStatus, end, pageable);
        } else if (hasCustomerId && hasStartDate && hasEndDate) {
            ordersPage = orderRepository.findByUserIdAndCreatedAtBetween(
                    customerId, start, end, pageable);
        } else if (hasStatus && hasStartDate && hasEndDate) {
            ordersPage = orderRepository.findByStatusAndCreatedAtBetween(
                    orderStatus, start, end, pageable);
        } else if (hasCustomerId && hasStatus) {
            ordersPage = orderRepository.findByUserIdAndStatus(
                    customerId, orderStatus, pageable);
        } else if (hasCustomerId && hasStartDate) {
            ordersPage = orderRepository.findByUserIdAndCreatedAtAfter(
                    customerId, start, pageable);
        } else if (hasCustomerId && hasEndDate) {
            ordersPage = orderRepository.findByUserIdAndCreatedAtBefore(
                    customerId, end, pageable);
        } else if (hasStatus && hasStartDate) {
            ordersPage = orderRepository.findByStatusAndCreatedAtAfter(
                    orderStatus, start, pageable);
        } else if (hasStatus && hasEndDate) {
            ordersPage = orderRepository.findByStatusAndCreatedAtBefore(
                    orderStatus, end, pageable);
        } else if (hasStartDate && hasEndDate) {
            ordersPage = orderRepository.findByCreatedAtBetween(
                    start, end, pageable);
        } else if (hasCustomerId) {
            ordersPage = orderRepository.findByUserId(customerId, pageable);
        } else if (hasStatus) {
            ordersPage = orderRepository.findByStatus(orderStatus, pageable);
        } else if (hasStartDate) {
            ordersPage = orderRepository.findByCreatedAtAfter(start, pageable);
        } else if (hasEndDate) {
            ordersPage = orderRepository.findByCreatedAtBefore(end, pageable);
        } else {
            ordersPage = orderRepository.findAll(pageable);
        }

        // 5. ФИЛЬТРАЦИЯ ПО ТИПУ ЗАКАЗА (RETAIL/WHOLESALE)
        List<Order> filteredOrders = ordersPage.getContent().stream()
                .filter(order -> order.getOrderType() == OrderType.RETAIL ||
                        order.getOrderType() == OrderType.WHOLESALE)
                .collect(Collectors.toList());

        // 6. СОЗДАНИЕ НОВОЙ СТРАНИЦЫ С ОТФИЛЬТРОВАННЫМИ ДАННЫМИ
        Page<Order> resultPage = new PageImpl<>(
                filteredOrders,
                pageable,
                filteredOrders.size()
        );

        log.debug(messageService.get(
                "manager.order.fetched.log",
                resultPage.getTotalElements()
        ));

        Locale finalLocale = locale;
        return resultPage.map(order -> orderMapper.toDto(order, finalLocale));
    }

    // ========== GET SINGLE ORDER ==========

    @Transactional(readOnly = true)
    public OrderDto getCustomerOrderById(Long orderId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getOrderType() != OrderType.RETAIL && order.getOrderType() != OrderType.WHOLESALE) {
            log.error(messageService.get("manager.order.log.not.customer.order", orderId));
            throw new InvalidOrderTypeException(orderId, "RETAIL or WHOLESALE");
        }

        return orderMapper.toDto(order, locale);
    }

    // ========== UPDATE ORDER STATUS ==========

    @Transactional
    public OrderDto updateOrderStatus(Long orderId, String status, String trackingNumber,
                                      String reason, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // ПРОВЕРКА СТАТУСА
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn(messageService.get("manager.order.log.invalid.status", status));
            throw new InvalidOrderStatusException(status);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });

        OrderStatus oldStatus = order.getStatus();

        validateStatusTransition(order, newStatus, locale);

        order.setStatus(newStatus);

        switch (newStatus) {
            case SHIPPED:
                if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
                    order.setTrackingNumber(trackingNumber);
                }
                break;
            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                break;
            case CANCELLED:
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(reason);
                restoreStockForCancelledOrder(order);
                break;
            default:
                break;
        }

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.order.status.updated.log",
                orderId,
                oldStatus,
                newStatus,
                managerId
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

        // ПРОВЕРКА ЦЕНЫ
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            log.warn(messageService.get("manager.order.log.invalid.price", newPrice));
            throw new InvalidPriceException(newPrice);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            log.warn(messageService.get("manager.order.log.cannot.modify", order.getStatus()));
            throw new OrderModificationNotAllowedException(order.getStatus());
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.item.not.found", itemId));
                    return new OrderItemNotFoundException(itemId);
                });

        BigDecimal oldPrice = item.getPrice();
        item.setPrice(newPrice);
        item.calculateTotal();

        order.recalculateTotals();

        Order updatedOrder = orderRepository.save(order);

        String reasonText = reason != null ? reason :
                messageService.get("manager.order.reason.default");

        log.info(messageService.get(
                "manager.order.price.updated.log",
                orderId,
                itemId,
                oldPrice,
                newPrice,
                managerId,
                reasonText
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    @Transactional
    public OrderDto updateOrderItemQuantity(Long orderId, Long itemId, Integer newQuantity,
                                            String reason, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        if (newQuantity == null || newQuantity < 0) {
            log.warn(messageService.get("manager.order.log.invalid.quantity", newQuantity));
            throw new InvalidQuantityException(newQuantity);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn(messageService.get("manager.order.log.cannot.modify", order.getStatus()));
            throw new OrderModificationNotAllowedException(order.getStatus());
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.item.not.found", itemId));
                    return new OrderItemNotFoundException(itemId);
                });

        int oldQuantity = item.getQuantity();
        int quantityDiff = newQuantity - oldQuantity;

        if (quantityDiff > 0) {
            checkProductAvailability(item.getProductId(), quantityDiff, locale);
        }

        item.setQuantity(newQuantity);
        item.calculateTotal();

        if (newQuantity == 0) {
            order.removeItem(item);
            orderItemRepository.delete(item);
        }

        order.recalculateTotals();

        Order updatedOrder = orderRepository.save(order);

        String reasonText = reason != null ? reason :
                messageService.get("manager.order.reason.default");

        log.info(messageService.get(
                "manager.order.quantity.updated.log",
                orderId,
                itemId,
                oldQuantity,
                newQuantity,
                managerId,
                reasonText
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    @Transactional
    public OrderDto addItemToOrder(Long orderId, Long productId, Integer quantity,
                                   BigDecimal customPrice, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        if (quantity == null || quantity <= 0) {
            log.warn(messageService.get("manager.order.log.invalid.quantity", quantity));
            throw new InvalidQuantityException(quantity);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn(messageService.get("manager.order.log.cannot.modify", order.getStatus()));
            throw new OrderModificationNotAllowedException(order.getStatus());
        }

        boolean itemExists = order.getItems().stream()
                .anyMatch(i -> i.getProductId().equals(productId));

        if (itemExists) {
            log.warn(messageService.get("manager.order.log.item.exists", productId));
            throw new OrderItemAlreadyExistsException(productId);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        checkProductAvailability(productId, quantity, locale);

        BigDecimal price = customPrice != null ? customPrice : product.getPrice();

        OrderItem item = OrderItem.builder()
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .productName(product.getName())
                .productSku(product.getSku())
                .build();

        order.addItem(item);
        order.recalculateTotals();

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.order.item.added.log",
                orderId,
                productId,
                quantity,
                managerId
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    @Transactional
    public OrderDto removeItemFromOrder(Long orderId, Long itemId, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn(messageService.get("manager.order.log.cannot.modify", order.getStatus()));
            throw new OrderModificationNotAllowedException(order.getStatus());
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.item.not.found", itemId));
                    return new OrderItemNotFoundException(itemId);
                });

        order.removeItem(item);
        orderItemRepository.delete(item);
        order.recalculateTotals();

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.order.item.removed.log",
                orderId,
                itemId,
                managerId
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    // ========== ORDER NOTES ==========

    @Transactional
    public OrderDto addOrderNote(Long orderId, String note, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });

        String currentNotes = order.getNotes();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String newNote = timestamp + " - " + messageService.get("manager.order.note.manager.prefix", managerId) + ": " + note;

        if (currentNotes == null || currentNotes.isEmpty()) {
            order.setNotes(newNote);
        } else {
            order.setNotes(currentNotes + "\n" + newNote);
        }

        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.order.note.added.log",
                orderId,
                managerId
        ));

        return orderMapper.toDto(updatedOrder, locale);
    }

    // ========== ORDER STATISTICS ==========

    @Transactional(readOnly = true)
    public OrderStatisticsDto getOrderStatistics(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfWeek = LocalDateTime.now().minusWeeks(1);
        LocalDateTime startOfMonth = LocalDateTime.now().minusMonths(1);

        long totalOrdersToday = orderRepository.countOrdersByDateRange(startOfDay, LocalDateTime.now());
        long totalOrdersWeek = orderRepository.countOrdersByDateRange(startOfWeek, LocalDateTime.now());
        long totalOrdersMonth = orderRepository.countOrdersByDateRange(startOfMonth, LocalDateTime.now());

        BigDecimal revenueToday = orderRepository.sumRevenueByDateRange(startOfDay, LocalDateTime.now());
        BigDecimal revenueWeek = orderRepository.sumRevenueByDateRange(startOfWeek, LocalDateTime.now());
        BigDecimal revenueMonth = orderRepository.sumRevenueByDateRange(startOfMonth, LocalDateTime.now());

        return OrderStatisticsDto.builder()
                .totalOrdersToday(totalOrdersToday)
                .totalOrdersWeek(totalOrdersWeek)
                .totalOrdersMonth(totalOrdersMonth)
                .revenueToday(revenueToday)
                .revenueWeek(revenueWeek)
                .revenueMonth(revenueMonth)
                .build();
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateStatusTransition(Order order, OrderStatus newStatus, Locale locale) {
        OrderStatus currentStatus = order.getStatus();

        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.PAID || newStatus == OrderStatus.CANCELLED;
            case PAID -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REFUNDED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED -> newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.REFUNDED;
            default -> false;
        };

        if (!isValid) {
            log.warn(messageService.get(
                    "manager.order.log.invalid.status.transition",
                    currentStatus,
                    newStatus
            ));
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }
    }

    private void restoreStockForCancelledOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int newQuantity = product.getQuantityInStock() + item.getQuantity();
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                log.debug(messageService.get(
                        "manager.order.log.stock.restored",
                        item.getQuantity(),
                        item.getProductId()
                ));
            });
        }
    }

    private void checkProductAvailability(Long productId, int requestedQuantity, Locale locale) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(messageService.get(
                    "manager.order.log.insufficient.stock",
                    product.getName(),
                    product.getQuantityInStock(),
                    requestedQuantity
            ));
            throw new InsufficientStockException(product.getName(), product.getQuantityInStock());
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Пробуем разные форматы дат
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException e) {
                // Пробуем следующий формат
            }
        }

        // Если не получилось распарсить, пробуем как дату без времени
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr.trim());
            return date.atStartOfDay();
        } catch (DateTimeParseException e) {
            log.debug(messageService.get("manager.order.log.date.parse.failed", dateStr));
            return null;
        }
    }

    // ========== INNER DTO CLASS ==========

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OrderStatisticsDto {
        private long totalOrdersToday;
        private long totalOrdersWeek;
        private long totalOrdersMonth;
        private BigDecimal revenueToday;
        private BigDecimal revenueWeek;
        private BigDecimal revenueMonth;
    }
}