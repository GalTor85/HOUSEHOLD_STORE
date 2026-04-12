package ru.galtor85.household_store.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cart.CartNotFoundException;
import ru.galtor85.household_store.advice.exception.order.OrderNotFoundException;
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
import ru.galtor85.household_store.service.cart.CartService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.date.DateParser;
import ru.galtor85.household_store.validator.order.SalesOrderValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Service for managing sales orders.
 *
 * <p>This service provides comprehensive order management functionality including:</p>
 * <ul>
 *   <li>Order creation from cart or direct creation</li>
 *   <li>Order retrieval with filtering and pagination</li>
 *   <li>Order status updates (processing, shipping, delivery, cancellation)</li>
 *   <li>Order item management (price updates, quantity changes, item removal)</li>
 *   <li>Order notes and rollback functionality</li>
 *   <li>Order statistics for reporting</li>
 * </ul>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderService {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final SalesOrderValidator salesOrderValidator;
    private final SalesOrderProcessor salesOrderProcessor;
    private final SalesOrderConverter salesOrderConverter;
    private final MessageService messageService;
    private final DateParser dateParser;

    // =========================================================================
    // ORDER CREATION
    // =========================================================================

    /**
     * Creates a new sales order directly (without cart).
     *
     * @param request the order creation request containing items and customer info
     * @param userId the ID of the customer placing the order
     * @return the created order as a DTO
     */
    @Transactional
    public SalesOrderDto createSalesOrder(SalesOrderCreateRequest request, Long userId) {

        log.info(messageService.get("sales.order.create.start", userId));

        // Validate request
        salesOrderValidator.validateNotEmpty(request);
        salesOrderValidator.validateUserExists(userId);
        SalesOrderValidator.ProductValidationResult validationResult =
                salesOrderValidator.validateProducts(request.getItems());

        // Create order via processor
        SalesOrder order = salesOrderProcessor.createSalesOrder(
                request,
                validationResult.getProducts(),
                validationResult.getPrices(),
                userId
        );

        // Add creation note
        addOrderNote(order.getId(), messageService.get("order.sales.creation.manger.from.user", userId, request.getUserId()), userId);

        SalesOrderDto result = salesOrderConverter.toDto(order);

        log.info(messageService.get("sales.order.created.log",
                order.getOrderNumber(), userId, order.getItems().size(), order.getTotalAmount()));

        return result;
    }

    /**
     * Creates an order from the user's active cart.
     *
     * @param userId the ID of the customer
     * @param shippingAddress the shipping address for the order
     * @return the created order as a DTO
     * @throws CartNotFoundException if no active cart exists for the user
     */
    @Transactional
    public SalesOrderDto createOrderFromCart(Long userId, String shippingAddress) {

        log.info(messageService.get("sales.order.create.from.cart.start", userId));

        // Get user's active cart
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(userId));

        // Create order via processor
        SalesOrder order = salesOrderProcessor.createOrderFromCart(cart, shippingAddress, userId);

        // Mark cart as completed
        cart.setStatus(CartStatus.COMPLETED);
        cartRepository.save(cart);

        log.info(messageService.get("sales.order.create.from.cart.complete",
                order.getOrderNumber(), userId, order.getTotalAmount()));

        return salesOrderConverter.toDto(order);
    }

    /**
     * Creates an order from cart with a promo code discount.
     *
     * @param userId the ID of the customer
     * @param shippingAddress the shipping address for the order
     * @param promoCode the promo code to apply
     * @return the created order as a DTO
     */
    @Transactional
    public SalesOrderDto createOrderFromCartWithPromo(Long userId, String shippingAddress, String promoCode) {
        log.info(messageService.get("sales.order.create.from.cart.promo.start", userId, promoCode));

        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(userId));

        SalesOrder order = salesOrderProcessor.createOrderFromCartWithPromo(cart, shippingAddress, promoCode, userId);

        cartService.completeCart(userId);

        log.info(messageService.get("sales.order.create.from.cart.promo.complete",
                order.getOrderNumber(), userId));

        return salesOrderConverter.toDto(order);
    }

    // =========================================================================
    // ORDER RETRIEVAL
    // =========================================================================

    /**
     * Retrieves paginated orders for a customer with optional filtering.
     *
     * @param userId the customer ID (optional)
     * @param status order status filter (optional)
     * @param startDate start date for filtering (optional)
     * @param endDate end date for filtering (optional)
     * @param page page number (0-indexed)
     * @param size page size
     * @return page of order DTOs
     */
    @Transactional(readOnly = true)
    public Page<SalesOrderDto> getCustomerOrders(Long userId,
                                                 String status,
                                                 String startDate,
                                                 String endDate,
                                                 int page,
                                                 int size) {

        // Parse the parameters
        OrderStatus orderStatus = salesOrderValidator.parseOrderStatus(status);
        LocalDateTime start = dateParser.parseDate(startDate);
        LocalDateTime end = dateParser.parseDate(endDate);

        // Validate the date range
        salesOrderValidator.validateDateRange(start, end);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Search for orders through the repository
        Page<SalesOrder> orders = salesOrderRepository.search(userId, orderStatus, start, end, pageable);

        log.debug(messageService.get("manager.orders.fetched.log", orders.getTotalElements()));

        return orders.map(salesOrderConverter::toDto);
    }

    /**
     * Retrieves an order by its ID.
     *
     * @param orderId the order ID
     * @return the order as a DTO
     */
    @Transactional(readOnly = true)
    public SalesOrderDto getSalesOrderById(Long orderId) {
        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        return salesOrderConverter.toDto(order);
    }

    /**
     * Retrieves an order by its ID as entity.
     *
     * @param orderId the order ID
     * @return the order entity
     */
    @Transactional(readOnly = true)
    public SalesOrder getSalesOrderEntityById(Long orderId) {
        return salesOrderValidator.validateSalesOrderExists(orderId);
    }

    /**
     * Retrieves an order by its order number.
     *
     * @param orderNumber the unique order number
     * @return the order as a DTO
     */
    @Transactional(readOnly = true)
    public SalesOrderDto getSalesOrderByNumber(String orderNumber) {
        SalesOrder order = salesOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(OrderNotFoundException::new);
        return salesOrderConverter.toDto(order);
    }

    // =========================================================================
    // ORDER STATUS MANAGEMENT
    // =========================================================================

    /**
     * Updates the status of an existing order.
     *
     * @param orderId the order ID
     * @param status the new status
     * @param trackingNumber tracking number for shipped orders (optional)
     * @param reason reason for cancellation or refund (optional)
     * @param managerId ID of the manager performing the update
     * @return the updated order as a DTO
     */
    @Transactional
    public SalesOrderDto updateOrderStatus(Long orderId,
                                           String status,
                                           String trackingNumber,
                                           String reason,
                                           Long managerId) {

        OrderStatus newStatus = salesOrderValidator.parseAndValidateOrderStatus(status);
        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        OrderStatus oldStatus = order.getStatus();

        // Checking the possibility of transition
        salesOrderValidator.validateStatusTransitionForSale(order.getStatus(), newStatus);

        // Update the status
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
            restoreStockForCancelledOrder(order);
        }

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.status.updated.log",
                orderId, oldStatus, newStatus, managerId));

        return salesOrderConverter.toDto(updatedOrder);
    }

    /**
     * Cancels an existing order.
     *
     * @param orderId the order ID
     * @param reason the cancellation reason
     * @param managerId ID of the manager cancelling the order
     * @return the cancelled order as a DTO
     */
    @Transactional
    public SalesOrderDto cancelOrder(Long orderId, String reason, Long managerId) {
        return updateOrderStatus(orderId, ORDER_STATUS_CANCELLED, null, reason, managerId);
    }

    // =========================================================================
    // ORDER ITEM MANAGEMENT
    // =========================================================================

    /**
     * Updates the price of an order item.
     *
     * @param orderId the order ID
     * @param itemId the order item ID
     * @param newPrice the new price
     * @param reason the reason for the price change
     * @param managerId ID of the manager making the change
     * @return the updated order as a DTO
     */
    @Transactional
    public SalesOrderDto updateOrderItemPrice(Long orderId,
                                              Long itemId,
                                              BigDecimal newPrice,
                                              String reason,
                                              Long managerId) {

        salesOrderValidator.validatePrice(newPrice);

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        salesOrderValidator.validateOrderModifiable(order, OrderStatus.PENDING, OrderStatus.PAID);

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
     * Updates the quantity of an order item.
     *
     * @param orderId the order ID
     * @param itemId the order item ID
     * @param newQuantity the new quantity
     * @param reason the reason for the quantity change
     * @param managerId ID of the manager making the change
     * @return the updated order as a DTO
     */
    @Transactional
    public SalesOrderDto updateOrderItemQuantity(Long orderId,
                                                 Long itemId,
                                                 Integer newQuantity,
                                                 String reason,
                                                 Long managerId) {

        salesOrderValidator.validateQuantity(newQuantity);

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        salesOrderValidator.validateOrderModifiable(order, OrderStatus.PENDING);

        SalesOrderItem item = salesOrderValidator.validateOrderItemExists(order, itemId);
        int oldQuantity = item.getQuantity();
        int quantityDiff = newQuantity - oldQuantity;

        if (quantityDiff > 0) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));
            salesOrderValidator.validateProductAvailability(product, quantityDiff);
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
     * Adds a new item to an existing order.
     *
     * @param orderId the order ID
     * @param productId the product ID to add
     * @param quantity the quantity to add
     * @param customPrice optional custom price (overrides product price)
     * @param managerId ID of the manager making the change
     * @return the updated order as a DTO
     */
    @Transactional
    public SalesOrderDto addItemToOrder(Long orderId,
                                        Long productId,
                                        Integer quantity,
                                        BigDecimal customPrice,
                                        Long managerId) {

        salesOrderValidator.validatePositiveQuantity(quantity);

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        salesOrderValidator.validateOrderModifiable(order, OrderStatus.PENDING);
        salesOrderValidator.validateItemNotExists(order, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        salesOrderValidator.validateProductAvailability(product, quantity);

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
     * Removes an item from an existing order.
     *
     * @param orderId the order ID
     * @param itemId the order item ID
     * @param managerId ID of the manager making the change
     * @return the updated order as a DTO
     */
    @Transactional
    public SalesOrderDto removeItemFromOrder(Long orderId, Long itemId, Long managerId) {

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        salesOrderValidator.validateOrderModifiable(order, OrderStatus.PENDING);

        SalesOrderItem item = salesOrderValidator.validateOrderItemExists(order, itemId);

        order.removeItem(item);
        order.recalculateTotals();

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.item.removed.log",
                orderId, itemId, managerId));

        return salesOrderConverter.toDto(updatedOrder);
    }

    // =========================================================================
    // ROLLBACK OPERATIONS
    // =========================================================================

    /**
     * Rolls back the status of an order.
     *
     * @param orderId the order ID
     * @param reason the reason for rollback
     * @param managerId ID of the manager performing the rollback
     * @return the updated order as a DTO
     * @throws RollbackNotAllowedException if rollback is not allowed for the current status
     */
    @Transactional
    public SalesOrderDto rollbackOrderStatus(Long orderId, String reason, Long managerId) {

        log.info(messageService.get("sales.order.rollback.start.log", orderId, managerId, reason));

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        OrderStatus currentStatus = order.getStatus();

        if (!currentStatus.isRollbackAllowedForSale()) {
            throw new RollbackNotAllowedException(currentStatus);
        }

        OrderStatus targetStatus = currentStatus.getRollbackTargetForSale();
        OrderStatus oldStatus = order.getStatus();

        performRollbackActions(order, oldStatus, targetStatus, reason, managerId);
        order.setStatus(targetStatus);
        addRollbackNote(order, oldStatus, targetStatus, reason, managerId);

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.rollback.success.log",
                orderId, oldStatus, targetStatus, managerId, reason));

        return salesOrderConverter.toDto(updatedOrder);
    }

    // =========================================================================
    // ORDER NOTES
    // =========================================================================

    /**
     * Adds a note to an existing order.
     *
     * @param orderId the order ID
     * @param note the note text
     * @param managerId ID of the manager adding the note
     * @return the updated order as a DTO
     */
    @Transactional
    public SalesOrderDto addOrderNote(Long orderId, String note, Long managerId) {

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN));
        String newNote = messageService.get("order.note.format", timestamp, managerId, note);

        if (order.getNotes() == null || order.getNotes().isEmpty()) {
            order.setNotes(newNote);
        } else {
            order.setNotes(order.getNotes() + NOTE_SEPARATOR + newNote);
        }

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(messageService.get("sales.order.note.added.log", orderId, managerId));

        return salesOrderConverter.toDto(updatedOrder);
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Retrieves order statistics for dashboard reporting.
     *
     * @return statistics DTO with totals for today, week, and month
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

    /**
     * Calculates the total amount spent by a user across all completed orders.
     *
     * @param userId the user ID
     * @return total amount spent
     */
    @Transactional(readOnly = true)
    public BigDecimal getUserTotalSpent(Long userId) {
        return salesOrderRepository.getTotalSpentByUser(userId);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Restores stock quantities when an order is cancelled.
     *
     * @param order the cancelled order
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
     * Performs specific actions based on the status being rolled back.
     *
     * @param order the order being rolled back
     * @param oldStatus the original status
     * @param newStatus the target status
     * @param reason the rollback reason
     * @param managerId ID of the manager performing the rollback
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
            default:
                log.debug(messageService.get("sales.order.rollback.no.action", oldStatus));
        }
    }

    /**
     * Adds a rollback note to the order.
     *
     * @param order the order
     * @param oldStatus the original status
     * @param newStatus the target status
     * @param reason the rollback reason
     * @param managerId ID of the manager performing the rollback
     */
    private void addRollbackNote(SalesOrder order,
                                 OrderStatus oldStatus,
                                 OrderStatus newStatus,
                                 String reason,
                                 Long managerId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN));
        String rollbackNote = messageService.get("order.rollback.note.format",
                timestamp,
                messageService.get("order.status." + oldStatus.name()),
                messageService.get("order.status." + newStatus.name()),
                managerId,
                reason);

        if (order.getNotes() == null || order.getNotes().isEmpty()) {
            order.setNotes(rollbackNote);
        } else {
            order.setNotes(order.getNotes() + LINE_SEPARATOR + rollbackNote);
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