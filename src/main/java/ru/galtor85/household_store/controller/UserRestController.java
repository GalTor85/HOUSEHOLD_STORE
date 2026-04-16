package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.advice.exception.order.OrderCancellationNotAllowedException;
import ru.galtor85.household_store.dto.request.cart.AddToCartRequest;
import ru.galtor85.household_store.dto.request.cart.UpdateCartItemRequest;
import ru.galtor85.household_store.dto.request.payment.PaymentMethodRequest;
import ru.galtor85.household_store.dto.request.payment.PaymentProcessRequest;
import ru.galtor85.household_store.dto.request.user.UserEditRequest;
import ru.galtor85.household_store.dto.request.user.UserUpdatePasswordRequest;
import ru.galtor85.household_store.dto.response.cart.CartDto;
import ru.galtor85.household_store.dto.response.finance.InvoiceDto;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.dto.response.payment.PaymentMethodForUserDto;
import ru.galtor85.household_store.dto.response.payment.PaymentTransactionDto;
import ru.galtor85.household_store.dto.response.stock.ProductAvailabilityDto;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.dto.response.user.UserResponse;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.mapper.user.UserMapper;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.auth.UserService;
import ru.galtor85.household_store.service.cart.CartService;
import ru.galtor85.household_store.service.finance.InvoiceService;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.order.SalesOrderService;
import ru.galtor85.household_store.service.payment.PaymentMethodService;
import ru.galtor85.household_store.service.payment.PaymentService;
import ru.galtor85.household_store.service.stock.StockDisplayService;
import ru.galtor85.household_store.service.user.UserSearchService;
import ru.galtor85.household_store.service.user.UserTypeAssignmentService;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_USERS;

/**
 * REST controller for user profile, shopping cart, order management and payment operations.
 *
 * <p>This controller provides endpoints for authenticated users to:</p>
 * <ul>
 *   <li>View and update their profile information</li>
 *   <li>Manage shopping cart (add, update, remove items)</li>
 *   <li>Create orders from cart</li>
 *   <li>View order history and order details</li>
 *   <li>Cancel pending orders</li>
 *   <li>Pay invoices</li>
 *   <li>View available payment methods</li>
 *   <li>Browse products with availability information</li>
 * </ul>
 *
 * <p>All endpoints require authentication via JWT token.</p>
 *
 * @author G@LTor85
 */
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping(CONTROL_USERS)
@RequiredArgsConstructor
@Tag(name = "Users", description = "API for Users")
public class UserRestController extends BaseController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final UserSearchService userSearchService;
    private final CartService cartService;
    private final SalesOrderService salesOrderService;
    private final PaymentService paymentService;
    private final PaymentMethodService paymentMethodService;
    private final UserTypeAssignmentService userTypeAssignmentService;
    private final StockDisplayService stockDisplayService;
    private final ProductRepository productRepository;
    private final InvoiceService invoiceService;

    // =========================================================================
    // USER PROFILE OPERATIONS
    // =========================================================================

    /**
     * Retrieves the current authenticated user's profile information.
     *
     * @return user profile data
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user information")
    public ResponseEntity<ApiResponse<UserResponse>> getUserInfo() {
        log.info(logMsg.get("user-rest-controller.log.profile.fetch.start"));

        User user = getCurrentUser();

        log.info(logMsg.get("user-rest-controller.log.profile.fetch.success", user.getId()));

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("user-rest-controller.msg.profile.fetch.success"),
                        userMapper.build(user)
                )
        );
    }

    /**
     * Updates the current user's profile information.
     *
     * @param request the edit request containing updated fields
     * @return updated user profile data
     */
    @PutMapping("/profile")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserResponse>> editUser(
            @Valid @RequestBody UserEditRequest request) {

        log.info(logMsg.get("user-rest-controller.log.profile.update.start"));

        SecurityUser securityUser = getCurrentSecurityUser();
        User updatedUser = userService.edit(
                userSearchService.getUserById(securityUser.getUserId()),
                request
        );

        log.info(logMsg.get("user-rest-controller.log.profile.update.success", updatedUser.getId()));

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("user-rest-controller.msg.profile.update.success"),
                        userMapper.build(updatedUser)
                )
        );
    }

    /**
     * Updates the current user's password.
     *
     * @param request the password update request containing current and new passwords
     * @return updated user profile data
     */
    @PutMapping("/password")
    @Operation(summary = "Update user password")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(
            @Valid @RequestBody UserUpdatePasswordRequest request) {

        log.info(logMsg.get("user-rest-controller.log.password.update.start"));

        SecurityUser securityUser = getCurrentSecurityUser();
        User updatedUser = userService.passwordUpdate(
                userSearchService.getUserById(securityUser.getUserId()),
                request
        );

        log.info(logMsg.get("user-rest-controller.log.password.update.success", updatedUser.getId()));

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("user-rest-controller.msg.password.update.success"),
                        userMapper.build(updatedUser)
                )
        );
    }

    /**
     * Retrieves a paginated list of products with availability information.
     *
     * @param category optional category filter
     * @param page     page number (0-indexed)
     * @param size     page size
     * @param sortBy   field to sort by
     * @param sortDir  sort direction (asc/desc)
     * @return page of products with availability status
     */
    @GetMapping("/products")
    @Operation(summary = "Get all products with availability")
    public ResponseEntity<ApiResponse<Page<ProductAvailabilityDto>>> getAllProductsWithAvailability(
            @Parameter(description = "Category filter", example = "Electronics")
            @RequestParam(required = false) String category,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "name")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.debug(logMsg.get("user.stock.products.start", page, size, category));

        Page<ProductAvailabilityDto> products = stockDisplayService.getAllProductsWithAvailability(
                category, page, size, sortBy, sortDir);

        log.debug(logMsg.get("user.stock.products.complete", products.getTotalElements()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.stock.products.fetched"),
                products));
    }

    /**
     * Retrieves all available product categories.
     *
     * @return list of category names
     */
    @GetMapping("/products/categories")
    @Operation(summary = "Get all product categories")
    public ResponseEntity<ApiResponse<List<String>>> getProductCategories() {
        log.debug(logMsg.get("user.stock.categories.start"));

        List<String> categories = productRepository.findAllCategories();

        log.debug(logMsg.get("user.stock.categories.complete", categories.size()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.stock.categories.fetched"),
                categories));
    }

    // =========================================================================
    // SHOPPING CART OPERATIONS
    // =========================================================================

    /**
     * Retrieves the current user's active shopping cart.
     *
     * @return cart data with items and total amount
     */
    @GetMapping("/cart")
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartDto>> getCart() {
        Long userId = getCurrentUserId();
        log.debug(logMsg.get("cart.controller.get", userId));

        CartDto cart = cartService.getActiveCart(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.fetched"),
                cart));
    }

    /**
     * Adds a product to the user's shopping cart.
     *
     * @param request add to cart request with product ID and quantity
     * @return updated cart data
     */
    @PostMapping("/cart/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartDto>> addToCart(
            @Valid @RequestBody AddToCartRequest request) {

        Long userId = getCurrentUserId();
        log.info(logMsg.get("cart.controller.add.start", userId, request.getProductId()));

        CartDto cart = cartService.addToCart(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("cart.item.added"),
                        cart));
    }

    /**
     * Updates the quantity of an item in the shopping cart.
     *
     * @param productId ID of the product to update
     * @param request   update request with new quantity
     * @return updated cart data
     */
    @PutMapping("/cart/items/{productId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartDto>> updateCartItem(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        Long userId = getCurrentUserId();
        log.info(logMsg.get("cart.controller.update.start", userId, productId));

        CartDto cart = cartService.updateCartItem(userId, productId, request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.item.updated"),
                cart));
    }

    /**
     * Removes an item from the shopping cart.
     *
     * @param productId ID of the product to remove
     * @return updated cart data
     */
    @DeleteMapping("/cart/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartDto>> removeFromCart(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        Long userId = getCurrentUserId();
        log.info(logMsg.get("cart.controller.remove.start", userId, productId));

        CartDto cart = cartService.removeFromCart(userId, productId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.item.removed"),
                cart));
    }

    /**
     * Clears all items from the shopping cart.
     *
     * @return empty response
     */
    @DeleteMapping("/cart")
    @Operation(summary = "Clear cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        Long userId = getCurrentUserId();
        log.info(logMsg.get("cart.controller.clear.start", userId));

        cartService.clearCart(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.cleared"),
                null));
    }

    /**
     * Prepares the cart for checkout.
     *
     * @return cart data with CHECKOUT status
     */
    @PostMapping("/cart/checkout")
    @Operation(summary = "Checkout cart (prepare for order)")
    public ResponseEntity<ApiResponse<CartDto>> checkoutCart() {
        Long userId = getCurrentUserId();
        log.info(logMsg.get("cart.controller.checkout.start", userId));

        CartDto cart = cartService.checkoutCart(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.checkout"),
                cart));
    }

    // =========================================================================
    // ORDER CREATION FROM CART
    // =========================================================================

    /**
     * Creates a new order from the user's active cart.
     *
     * @param shippingAddress the shipping address for the order
     * @return created order data
     */
    @PostMapping("/orders/from-cart")
    @Operation(summary = "Create order from cart")
    public ResponseEntity<ApiResponse<SalesOrderDto>> createOrderFromCart(
            @Parameter(description = "Shipping address", example = "123 Main St, Moscow, Russia", required = true)
            @RequestParam String shippingAddress) {

        Long userId = getCurrentUserId();
        log.info(logMsg.get("user.order.create.from.cart.start", userId));

        SalesOrderDto order = salesOrderService.createOrderFromCart(userId, shippingAddress);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("user.order.created.from.cart"),
                        order));
    }

    /**
     * Creates a new order from the user's active cart with a promo code.
     *
     * @param shippingAddress the shipping address for the order
     * @param promoCode       promo code to apply
     * @return created order data with applied discount
     */
    @PostMapping("/orders/from-cart/promo")
    @Operation(summary = "Create order from cart with promo code")
    public ResponseEntity<ApiResponse<SalesOrderDto>> createOrderFromCartWithPromo(
            @Parameter(description = "Shipping address", example = "123 Main St, Moscow, Russia", required = true)
            @RequestParam String shippingAddress,
            @Parameter(description = "Promo code", example = "WELCOME10")
            @RequestParam String promoCode) {

        Long userId = getCurrentUserId();
        log.info(logMsg.get("user.order.create.from.cart.promo.start", userId, promoCode));

        SalesOrderDto order = salesOrderService.createOrderFromCartWithPromo(userId, shippingAddress, promoCode);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("user.order.created.from.cart.promo"),
                        order));
    }

    // =========================================================================
    // USER ORDER MANAGEMENT
    // =========================================================================

    /**
     * Retrieves a paginated list of the user's orders.
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @return page of order data
     */
    @GetMapping("/orders")
    @Operation(summary = "Get user's orders")
    public ResponseEntity<ApiResponse<Page<SalesOrderDto>>> getUserOrders(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();
        log.debug(logMsg.get("user.orders.fetch.start", userId));

        Page<SalesOrderDto> orders = salesOrderService.getCustomerOrders(userId, null, null, null, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.orders.fetched"),
                orders));
    }

    /**
     * Retrieves a specific order by its ID.
     *
     * @param orderId the order ID
     * @return order data (only if owned by the current user)
     */
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<SalesOrderDto>> getOrder(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long orderId) {

        Long userId = getCurrentUserId();
        log.debug(logMsg.get("user.order.fetch.start", userId, orderId));

        SalesOrderDto order = salesOrderService.getSalesOrderById(orderId);

        return validateOrderAccess(order, userId, orderId,
                () -> ResponseEntity.ok(ApiResponse.success(
                        messageService.get("user.order.fetched"),
                        order)));
    }

    /**
     * Retrieves a specific order by its order number.
     *
     * @param orderNumber the unique order number
     * @return order data (only if owned by the current user)
     */
    @GetMapping("/orders/by-number/{orderNumber}")
    @Operation(summary = "Get order by number")
    public ResponseEntity<ApiResponse<SalesOrderDto>> getOrderByNumber(
            @Parameter(description = "Order number", example = "SO-20240330-001", required = true)
            @PathVariable String orderNumber) {

        Long userId = getCurrentUserId();
        log.debug(logMsg.get("user.order.fetch.by.number.start", userId, orderNumber));

        SalesOrderDto order = salesOrderService.getSalesOrderByNumber(orderNumber);

        return validateOrderAccess(order, userId, orderNumber,
                () -> ResponseEntity.ok(ApiResponse.success(
                        messageService.get("user.order.fetched"),
                        order)));
    }

    /**
     * Cancels a pending order.
     *
     * @param orderId the order ID
     * @param reason  cancellation reason
     * @return cancelled order data or error response
     */
    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Cancel order")
    public ResponseEntity<ApiResponse<SalesOrderDto>> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam String reason) {

        Long userId = getCurrentUserId();

        try {
            SalesOrderDto cancelledOrder = salesOrderService.cancelOrderWithOwnershipCheck(
                    orderId, reason, userId, userId);

            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("user.order.cancelled"),
                    cancelledOrder));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (OrderCancellationNotAllowedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // =========================================================================
    // INVOICE PAYMENT
    // =========================================================================

    /**
     * Pays an invoice by its invoice number.
     *
     * @param invoiceNumber the invoice number
     * @param request       payment method selection
     * @return payment transaction data
     */
    @PostMapping("/invoices/{invoiceNumber}/pay")
    @Operation(summary = "Pay invoice by number")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> payInvoiceByNumber(
            @Parameter(description = "Invoice number", example = "INV-20240330-001", required = true)
            @PathVariable String invoiceNumber,
            @Valid @RequestBody PaymentMethodRequest request) {

        Long currentUserId = getCurrentUserId();
        log.info(logMsg.get("user.payment.start.by.invoice", invoiceNumber, currentUserId));

        InvoiceDto invoice = invoiceService.getInvoiceByNumber(invoiceNumber);

        if (invoice.getSalesOrderId() == null) {
            throw new IllegalArgumentException(messageService.get("payment.error.invoice.not.sales"));
        }

        SalesOrderDto order = salesOrderService.getSalesOrderById(invoice.getSalesOrderId());
        if (!order.getUserId().equals(currentUserId)) {
            log.warn(logMsg.get("payment.security.invoice.access.denied",
                    currentUserId, invoiceNumber, order.getUserId()));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(messageService.get("payment.error.access.denied")));
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(messageService.get("payment.error.invoice.already.paid")));
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(messageService.get("payment.error.invoice.cancelled")));
        }

        PaymentProcessRequest paymentRequest = PaymentProcessRequest.builder()
                .invoiceId(invoice.getId())
                .paymentMethodId(request.getPaymentMethodId())
                .amount(request.hasAmount() ? request.getAmount() : null)
                .build();

        PaymentTransactionDto transaction = paymentService.processPayment(paymentRequest, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.processed"), transaction));
    }

    /**
     * Retrieves all invoices for a specific order.
     *
     * @param orderNumber the order number
     * @return list of invoices (only if order belongs to current user)
     */
    @GetMapping("/orders/{orderNumber}/invoices")
    @Operation(summary = "Get order invoices")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getOrderInvoices(
            @Parameter(description = "Order number", example = "SO-20240330-001", required = true)
            @PathVariable String orderNumber) {

        Long currentUserId = getCurrentUserId();
        log.debug(logMsg.get("user.order.invoices.fetch.start", currentUserId, orderNumber));

        SalesOrderDto order = salesOrderService.getSalesOrderByNumber(orderNumber);

        return validateOrderAccess(order, currentUserId, orderNumber,
                () -> {
                    List<InvoiceDto> invoices = invoiceService.getInvoicesBySalesOrder(order.getId());
                    return ResponseEntity.ok(ApiResponse.success(
                            messageService.get("user.order.invoices.fetched"), invoices));
                });
    }

    /**
     * Retrieves detailed information about an invoice.
     *
     * @param invoiceNumber the invoice number
     * @return invoice details (only if accessible by current user)
     */
    @GetMapping("/invoices/{invoiceNumber}")
    @Operation(summary = "Get invoice details")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceDetails(
            @Parameter(description = "Invoice number", example = "INV-20240330-001", required = true)
            @PathVariable String invoiceNumber) {

        Long currentUserId = getCurrentUserId();
        log.debug(logMsg.get("user.invoice.fetch.start", currentUserId, invoiceNumber));

        InvoiceDto invoice = invoiceService.getInvoiceByNumber(invoiceNumber);

        if (invoice.getSalesOrderId() != null) {
            SalesOrderDto order = salesOrderService.getSalesOrderById(invoice.getSalesOrderId());
            if (!order.getUserId().equals(currentUserId)) {
                log.warn(logMsg.get("user.invoice.access.denied", currentUserId, invoiceNumber));
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(messageService.get("user.order.access.denied.message")));
            }
        }

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.invoice.fetched"), invoice));
    }

    // =========================================================================
    // PAYMENT METHODS FOR CUSTOMERS
    // =========================================================================

    /**
     * Retrieves available payment methods for the current user based on their user type.
     *
     * @return list of available payment methods
     */
    @GetMapping("/payment-methods")
    @Operation(summary = "Get available payment methods")
    public ResponseEntity<ApiResponse<List<PaymentMethodForUserDto>>> getAvailablePaymentMethods() {

        User user = getCurrentUser();
        UserType userType = getUserType(user);

        log.debug(logMsg.get("user.payment.methods.fetch.start", user.getId(), userType));

        List<PaymentMethodForUserDto> result = paymentMethodService.getPaymentMethodsForUserType(userType);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.payment.methods.fetched"),
                result));
    }

    /**
     * Retrieves detailed information about a specific payment method.
     *
     * @param methodId the payment method ID
     * @return payment method details (only if available for the user's type)
     */
    @GetMapping("/payment-methods/{methodId}")
    @Operation(summary = "Get payment method details")
    public ResponseEntity<ApiResponse<PaymentMethodForUserDto>> getPaymentMethodDetails(
            @Parameter(description = "Payment method ID", example = "1", required = true)
            @PathVariable Long methodId) {

        User user = getCurrentUser();
        UserType userType = getUserType(user);

        log.debug(logMsg.get("user.payment.method.details.start", methodId, userType));

        PaymentMethodForUserDto result = paymentMethodService.getPaymentMethodForUserType(methodId, userType);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.payment.method.details.fetched"),
                result));
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Retrieves the total amount spent by the current user across all orders.
     *
     * @return total spent amount
     */
    @GetMapping("/stats/spent")
    @Operation(summary = "Get total amount spent by user")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalSpent() {
        Long userId = getCurrentUserId();
        log.debug(logMsg.get("user.stats.spent.start", userId));

        BigDecimal totalSpent = salesOrderService.getUserTotalSpent(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.stats.spent.fetched"),
                totalSpent));
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private UserType getUserType(User user) {
        if (user == null) {
            return UserType.RETAIL;
        }
        UserTypeAssignmentDto assignment = userTypeAssignmentService.getCurrentUserType(user.getId());
        if (assignment != null && assignment.getUserType() != null) {
            return assignment.getUserType();
        }
        return UserType.RETAIL;
    }

    private <T> ResponseEntity<ApiResponse<T>> validateOrderAccess(
            SalesOrderDto order, Long userId, Object identifier, Supplier<ResponseEntity<ApiResponse<T>>> successResponse) {
        if (!order.getUserId().equals(userId)) {
            log.warn(logMsg.get("user.order.access.denied", userId, identifier));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(messageService.get("user.order.access.denied.message")));
        }
        return successResponse.get();
    }
}