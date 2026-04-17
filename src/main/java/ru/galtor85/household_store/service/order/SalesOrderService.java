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
import ru.galtor85.household_store.advice.exception.rollback.RollbackNotAllowedException;
import ru.galtor85.household_store.config.WarehouseConfig;
import ru.galtor85.household_store.converter.SalesOrderConverter;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.dto.response.finance.InvoiceDto;
import ru.galtor85.household_store.dto.response.order.PaymentSummaryDto;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.dto.response.product.ProductStockDto;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartStatus;
import ru.galtor85.household_store.entity.finance.CashTransaction;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.TransactionType;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.processor.sales.SalesOrderProcessor;
import ru.galtor85.household_store.repository.cart.CartRepository;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.cart.CartService;
import ru.galtor85.household_store.service.finance.InvoiceService;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.stock.StockService;
import ru.galtor85.household_store.util.date.DateParser;
import ru.galtor85.household_store.validator.order.OrderSalesCancellationValidator;
import ru.galtor85.household_store.validator.order.SalesOrderValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private final LogMessageService logMsg;
    private final DateParser dateParser;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final OrderSalesCancellationValidator orderCancellationValidator;
    private final StockService stockService;
    private final WarehouseConfig warehouseConfig;

    // =========================================================================
    // ORDER CREATION
    // =========================================================================

    /**
     * Creates a new sales order directly (without cart).
     *
     * @param request the order creation request containing items and customer info
     * @param userId  the ID of the customer placing the order
     * @return the created order as a DTO
     */
    @Transactional
    public SalesOrderDto createSalesOrder(SalesOrderCreateRequest request, Long userId) {

        log.info(logMsg.get("sales.order.create.start", userId));

        // Validate request
        salesOrderValidator.validateNotEmpty(request);
        salesOrderValidator.validateUserExists(userId);
        SalesOrderValidator.ProductValidationResult validationResult =
                salesOrderValidator.validateProducts(request.getItems());

        // Create order via processor
        SalesOrder order = salesOrderProcessor.createSalesOrder(
                request,
                validationResult.products(),
                validationResult.prices(),
                userId
        );

        // Add creation note
        addOrderNote(order.getId(), messageService.get("order.sales.creation.manger.from.user", userId, request.getUserId()), userId);

        SalesOrderDto result = salesOrderConverter.toDto(order);

        log.info(logMsg.get("sales.order.created.log",
                order.getOrderNumber(), userId, order.getItems().size(), order.getTotalAmount()));

        return result;
    }

    /**
     * Creates an order from the user's active cart.
     *
     * @param userId          the ID of the customer
     * @param shippingAddress the shipping address for the order
     * @return the created order as a DTO
     * @throws CartNotFoundException if no active cart exists for the user
     */
    @Transactional
    public SalesOrderDto createOrderFromCart(Long userId, String shippingAddress) {

        log.info(logMsg.get("sales.order.create.from.cart.start", userId));

        // Get user's active cart
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(userId));

        // Create order via processor
        SalesOrder order = salesOrderProcessor.createOrderFromCart(cart, shippingAddress, userId);

        // Mark cart as completed
        cart.setStatus(CartStatus.COMPLETED);
        cartRepository.save(cart);

        log.info(logMsg.get("sales.order.create.from.cart.complete",
                order.getOrderNumber(), userId, order.getTotalAmount()));

        return salesOrderConverter.toDto(order);
    }

    /**
     * Creates an order from cart with a promo code discount.
     *
     * @param userId          the ID of the customer
     * @param shippingAddress the shipping address for the order
     * @param promoCode       the promo code to apply
     * @return the created order as a DTO
     */
    @Transactional
    public SalesOrderDto createOrderFromCartWithPromo(Long userId, String shippingAddress, String promoCode) {
        log.info(logMsg.get("sales.order.create.from.cart.promo.start", userId, promoCode));

        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(userId));

        SalesOrder order = salesOrderProcessor.createOrderFromCartWithPromo(cart, shippingAddress, promoCode, userId);

        cartService.completeCart(userId);

        log.info(logMsg.get("sales.order.create.from.cart.promo.complete",
                order.getOrderNumber(), userId));

        return salesOrderConverter.toDto(order);
    }

    // =========================================================================
    // ORDER RETRIEVAL
    // =========================================================================

    /**
     * Retrieves paginated orders for a customer with optional filtering.
     *
     * @param userId    the customer ID (optional)
     * @param status    order status filter (optional)
     * @param startDate start date for filtering (optional)
     * @param endDate   end date for filtering (optional)
     * @param page      page number (0-indexed)
     * @param size      page size
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

        log.debug(logMsg.get("manager.orders.fetched.log", orders.getTotalElements()));

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
        SalesOrderDto dto = salesOrderConverter.toDto(order);
        return enrichWithPaymentSummary(dto);
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
        SalesOrderDto dto = salesOrderConverter.toDto(order);
        return enrichWithPaymentSummary(dto);
    }

    /**
     * Enriches SalesOrderDto with payment summary
     */
    public SalesOrderDto enrichWithPaymentSummary(SalesOrderDto order) {
        try {
            List<InvoiceDto> invoices = invoiceService.getInvoicesBySalesOrder(order.getId());

            BigDecimal totalPaid = invoices.stream()
                    .map(inv -> inv.getTotalPaid() != null ? inv.getTotalPaid() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal remainingAmount = order.getTotalAmount().subtract(totalPaid);

            InvoiceDto payableInvoice = invoices.stream()
                    .filter(inv -> inv.getStatus() == InvoiceStatus.PENDING ||
                            inv.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                    .findFirst()
                    .orElse(null);

            PaymentSummaryDto summary = PaymentSummaryDto.builder()
                    .totalPaid(totalPaid)
                    .remainingAmount(remainingAmount.max(BigDecimal.ZERO))
                    .hasPayableInvoices(payableInvoice != null)
                    .nextInvoiceNumber(payableInvoice != null ? payableInvoice.getInvoiceNumber() : null)
                    .invoiceStatus(payableInvoice != null ? payableInvoice.getStatus().name() : null)
                    .paymentUrl(payableInvoice != null ?
                            "/app/users/invoices/" + payableInvoice.getInvoiceNumber() + "/pay" : null)
                    .build();

            order.setPaymentSummary(summary);
        } catch (Exception e) {
            log.warn("Failed to enrich order with payment summary: {}", e.getMessage());
            // Если не удалось получить счета, ставим пустую сводку
            order.setPaymentSummary(PaymentSummaryDto.builder()
                    .totalPaid(BigDecimal.ZERO)
                    .remainingAmount(order.getTotalAmount())
                    .hasPayableInvoices(false)
                    .build());
        }

        return order;
    }

    /**
     * Cancels order with ownership check.
     *
     * @param orderId     order identifier
     * @param reason      cancellation reason
     * @param userId      ID of user cancelling the order
     * @param ownerUserId ID of order owner
     * @return cancelled order DTO
     */
    @Transactional
    public SalesOrderDto cancelOrderWithOwnershipCheck(Long orderId, String reason,
                                                       Long userId, Long ownerUserId) {
        log.info(logMsg.get("order.cancel.service.ownership.start", orderId, userId, ownerUserId));

        SalesOrderDto order = getSalesOrderById(orderId);

        // Check ownership
        if (!order.getUserId().equals(ownerUserId)) {
            log.warn(logMsg.get("order.cancel.service.access.denied",
                    orderId, userId, ownerUserId));
            throw new SecurityException(messageService.get("order.cancel.error.access.denied"));
        }

        // Validate cancellation is allowed
        orderCancellationValidator.validateCancellable(order);

        return cancelOrder(orderId, reason, userId);
    }

    // =========================================================================
    // ORDER STATUS MANAGEMENT
    // =========================================================================

    /**
     * Updates the status of an existing order.
     *
     * @param orderId        the order ID
     * @param status         the new status
     * @param trackingNumber tracking number for shipped orders (optional)
     * @param reason         reason for cancellation or refund (optional)
     * @param managerId      ID of the manager performing the update
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

        log.info(logMsg.get("sales.order.status.updated.log",
                orderId, oldStatus, newStatus, managerId));

        return salesOrderConverter.toDto(updatedOrder);
    }

    /**
     * Cancels an existing order.
     *
     * @param orderId   the order ID
     * @param reason    the cancellation reason
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
     * @param orderId   the order ID
     * @param itemId    the order item ID
     * @param newPrice  the new price
     * @param reason    the reason for the price change
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

        log.info(logMsg.get("sales.order.price.updated.log",
                orderId, itemId, oldPrice, newPrice, managerId, reason));

        return salesOrderConverter.toDto(updatedOrder);
    }

    // =========================================================================
    // ROLLBACK OPERATIONS
    // =========================================================================

    /**
     * Rolls back the status of an order.
     *
     * @param orderId   the order ID
     * @param reason    the reason for rollback
     * @param managerId ID of the manager performing the rollback
     * @throws RollbackNotAllowedException if rollback is not allowed for the current status
     */
    @Transactional
    public void rollbackOrderStatus(Long orderId, String reason, Long managerId) {

        log.info(logMsg.get("sales.order.rollback.start.log", orderId, managerId, reason));

        SalesOrder order = salesOrderValidator.validateSalesOrderExists(orderId);
        OrderStatus currentStatus = order.getStatus();

        if (!currentStatus.isRollbackAllowedForSale()) {
            throw new RollbackNotAllowedException(currentStatus);
        }

        OrderStatus targetStatus = currentStatus.getRollbackTargetForSale();
        OrderStatus oldStatus = order.getStatus();

        performRollbackActions(order, oldStatus);
        order.setStatus(targetStatus);
        addRollbackNote(order, oldStatus, targetStatus, reason, managerId);

        SalesOrder updatedOrder = salesOrderRepository.save(order);

        log.info(logMsg.get("sales.order.rollback.success.log",
                orderId, oldStatus, targetStatus, managerId, reason));

        salesOrderConverter.toDto(updatedOrder);
    }

    // =========================================================================
    // ORDER NOTES
    // =========================================================================

    /**
     * Adds a note to an existing order.
     *
     * @param orderId   the order ID
     * @param note      the note text
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

        log.info(logMsg.get("sales.order.note.added.log", orderId, managerId));

        return salesOrderConverter.toDto(updatedOrder);
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    @Transactional(readOnly = true)
    public SalesOrder getSalesOrderEntityByNumber(String orderNumber) {
        return salesOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
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
        // Get default warehouse (temporary solution until reservation tracking is implemented)
        Long defaultWarehouseId = warehouseConfig.getDefaultWarehouseId();

        for (SalesOrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                // Get current stock using existing method
                ProductStockDto stockDto = stockService.getProductStockAtWarehouse(product.getId(), defaultWarehouseId);
                int oldQuantity = stockDto.getQuantity();

                // Update stock (increase)
                int newQuantity = stockService.updateProductStock(product, item.getQuantity(), defaultWarehouseId, true);

                log.debug(logMsg.get("sales.order.stock.restored",
                        product.getSku(), defaultWarehouseId, oldQuantity, newQuantity, item.getQuantity()));
            });
        }
    }

    /**
     * Performs specific actions based on the status being rolled back.
     *
     * @param order     the order being rolled back
     * @param oldStatus the original status
     */
    private void performRollbackActions(SalesOrder order,
                                        OrderStatus oldStatus
    ) {
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
                log.debug(logMsg.get("sales.order.rollback.no.action", oldStatus));
        }
    }

    /**
     * Adds a rollback note to the order.
     *
     * @param order     the order
     * @param oldStatus the original status
     * @param newStatus the target status
     * @param reason    the rollback reason
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

    /**
     * Reverses payment during order rollback.
     *
     * @param order the sales order
     */
    private void reversePayment(SalesOrder order) {
        log.debug(logMsg.get("sales.order.rollback.payment.reversed", order.getId()));
    }

    /**
     * Releases reserved stock during order rollback.
     *
     * @param order the sales order
     */
    private void releaseReservedStock(SalesOrder order) {
        log.debug(logMsg.get("sales.order.rollback.stock.released", order.getId()));
    }

    /**
     * Cancels shipment during order rollback.
     *
     * @param order the sales order
     */
    private void cancelShipment(SalesOrder order) {
        log.debug(logMsg.get("sales.order.rollback.shipment.cancelled", order.getId()));
    }

    /**
     * Calculates total amount spent by user on all orders.
     *
     * @param userId the user ID
     * @return total spent amount
     */
    @Transactional(readOnly = true)
    public BigDecimal getUserTotalSpent(Long userId) {
        List<SalesOrder> orders = salesOrderRepository.findByUserId(userId);
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (SalesOrder order : orders) {
            List<Invoice> invoices = invoiceRepository.findBySalesOrderId(order.getId());
            for (Invoice invoice : invoices) {
                BigDecimal paid = cashTransactionRepository.findByInvoiceId(invoice.getId()).stream()
                        .filter(tx -> tx.getTransactionType() == TransactionType.INCOME)
                        .map(CashTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalSpent = totalSpent.add(paid);
            }
        }
        return totalSpent;
    }

}