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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.request.cart.AddToCartRequest;
import ru.galtor85.household_store.dto.request.cart.UpdateCartItemRequest;
import ru.galtor85.household_store.dto.request.user.UserEditRequest;
import ru.galtor85.household_store.dto.request.user.UserUpdatePasswordRequest;
import ru.galtor85.household_store.dto.response.cart.CartDto;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.dto.response.user.UserResponse;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.mapper.user.UserMapper;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.cart.CartService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.order.SalesOrderService;
import ru.galtor85.household_store.service.user.UserSearchService;
import ru.galtor85.household_store.service.auth.UserService;

import static ru.galtor85.household_store.config.ApiConstants.API_BASE;

/**
 * REST controller for user profile, cart, and order operations.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>User profile management (view, edit, password change)</li>
 *   <li>Shopping cart operations (add, update, remove, clear, checkout)</li>
 *   <li>Order creation from cart (with or without promo code)</li>
 *   <li>Viewing user's orders and order details</li>
 *   <li>Cancelling orders</li>
 *   <li>User spending statistics</li>
 * </ul>
 *
 * <p>All endpoints require authentication via Bearer token.</p>
 */
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping(API_BASE + "/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API for Users")
public class UserRestController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final UserSearchService userSearchService;
    private final CartService cartService;
    private final SalesOrderService salesOrderService;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private SecurityUser getCurrentSecurityUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (SecurityUser) auth.getPrincipal();
    }

    private Long getCurrentUserId() {
        return getCurrentSecurityUser().getUserId();
    }

    // =========================================================================
    // USER PROFILE OPERATIONS
    // =========================================================================

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @return user profile DTO
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user information",
            description = "Retrieves the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {

        log.info(messageService.get("user-rest-controller.log.profile.fetch.start"));

        SecurityUser securityUser = getCurrentSecurityUser();
        User user = userSearchService.getUserById(securityUser.getUserId());

        log.info(messageService.get("user-rest-controller.log.profile.fetch.success", user.getId()));

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("user-rest-controller.msg.profile.fetch.success"),
                        userMapper.build(user)
                )
        );
    }

    /**
     * Updates the profile of the currently authenticated user.
     *
     * @param request profile update request with fields to update
     * @return updated user profile DTO
     */
    @PutMapping("/profile")
    @Operation(summary = "Update user profile",
            description = "Updates the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> editUser(
            @Valid @RequestBody UserEditRequest request) {

        log.info(messageService.get("user-rest-controller.log.profile.update.start"));

        SecurityUser securityUser = getCurrentSecurityUser();
        User updatedUser = userService.edit(
                userSearchService.getUserById(securityUser.getUserId()),
                request
        );

        log.info(messageService.get("user-rest-controller.log.profile.update.success", updatedUser.getId()));

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("user-rest-controller.msg.profile.update.success"),
                        userMapper.build(updatedUser)
                )
        );
    }

    /**
     * Updates the password of the currently authenticated user.
     *
     * @param request password update request with current and new password
     * @return updated user profile DTO
     */
    @PutMapping("/password")
    @Operation(summary = "Update user password",
            description = "Updates the password of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(
            @Valid @RequestBody UserUpdatePasswordRequest request) {

        log.info(messageService.get("user-rest-controller.log.password.update.start"));

        SecurityUser securityUser = getCurrentSecurityUser();
        User updatedUser = userService.passwordUpdate(
                userSearchService.getUserById(securityUser.getUserId()),
                request
        );

        log.info(messageService.get("user-rest-controller.log.password.update.success", updatedUser.getId()));

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("user-rest-controller.msg.password.update.success"),
                        userMapper.build(updatedUser)
                )
        );
    }

    // =========================================================================
    // SHOPPING CART OPERATIONS
    // =========================================================================

    /**
     * Retrieves the active shopping cart for the authenticated user.
     *
     * @return cart DTO with items and totals
     */
    @GetMapping("/cart")
    @Operation(summary = "Get current user's cart",
            description = "Retrieves the active shopping cart for the authenticated user")
    public ResponseEntity<ApiResponse<CartDto>> getCart() {
        Long userId = getCurrentUserId();
        log.debug(messageService.get("cart.controller.get", userId));

        CartDto cart = cartService.getActiveCart(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.fetched"),
                cart));
    }

    /**
     * Adds a product to the user's shopping cart.
     *
     * @param request add to cart request with product ID and quantity
     * @return updated cart DTO
     */
    @PostMapping("/cart/items")
    @Operation(summary = "Add item to cart",
            description = "Adds a product to the user's shopping cart")
    public ResponseEntity<ApiResponse<CartDto>> addToCart(
            @Valid @RequestBody AddToCartRequest request) {

        Long userId = getCurrentUserId();
        log.info(messageService.get("cart.controller.add.start", userId, request.getProductId()));

        CartDto cart = cartService.addToCart(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("cart.item.added"),
                        cart));
    }

    /**
     * Updates the quantity of a specific item in the shopping cart.
     *
     * @param productId product ID to update
     * @param request   update request with new quantity (0 to remove)
     * @return updated cart DTO
     */
    @PutMapping("/cart/items/{productId}")
    @Operation(summary = "Update cart item quantity",
            description = "Updates the quantity of a specific item in the shopping cart")
    public ResponseEntity<ApiResponse<CartDto>> updateCartItem(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        Long userId = getCurrentUserId();
        log.info(messageService.get("cart.controller.update.start", userId, productId));

        CartDto cart = cartService.updateCartItem(userId, productId, request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.item.updated"),
                cart));
    }

    /**
     * Removes a specific product from the shopping cart.
     *
     * @param productId product ID to remove
     * @return updated cart DTO
     */
    @DeleteMapping("/cart/items/{productId}")
    @Operation(summary = "Remove item from cart",
            description = "Removes a specific product from the shopping cart")
    public ResponseEntity<ApiResponse<CartDto>> removeFromCart(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        Long userId = getCurrentUserId();
        log.info(messageService.get("cart.controller.remove.start", userId, productId));

        CartDto cart = cartService.removeFromCart(userId, productId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.item.removed"),
                cart));
    }

    /**
     * Removes all items from the user's shopping cart.
     *
     * @return success response
     */
    @DeleteMapping("/cart")
    @Operation(summary = "Clear cart",
            description = "Removes all items from the user's shopping cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        Long userId = getCurrentUserId();
        log.info(messageService.get("cart.controller.clear.start", userId));

        cartService.clearCart(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.cleared"),
                null));
    }

    /**
     * Prepares the cart for checkout (changes status to CHECKOUT).
     *
     * @return cart DTO with CHECKOUT status
     */
    @PostMapping("/cart/checkout")
    @Operation(summary = "Checkout cart (prepare for order)",
            description = "Prepares the cart for checkout and initiates the ordering process")
    public ResponseEntity<ApiResponse<CartDto>> checkoutCart() {
        Long userId = getCurrentUserId();
        log.info(messageService.get("cart.controller.checkout.start", userId));

        CartDto cart = cartService.checkoutCart(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cart.checkout"),
                cart));
    }

    // =========================================================================
    // ORDER CREATION FROM CART
    // =========================================================================

    /**
     * Creates a new sales order from the items in the user's shopping cart.
     *
     * @param shippingAddress shipping address for the order
     * @return created sales order DTO
     */
    @PostMapping("/orders/from-cart")
    @Operation(summary = "Create order from cart",
            description = "Creates a new sales order from the items in the user's shopping cart")
    public ResponseEntity<ApiResponse<SalesOrderDto>> createOrderFromCart(
            @Parameter(description = "Shipping address", example = "123 Main St, Moscow, Russia", required = true)
            @RequestParam String shippingAddress) {

        Long userId = getCurrentUserId();
        log.info(messageService.get("user.order.create.from.cart.start", userId));

        SalesOrderDto order = salesOrderService.createOrderFromCart(userId, shippingAddress);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("user.order.created.from.cart"),
                        order));
    }

    /**
     * Creates a new sales order from the cart with a promo code discount.
     *
     * @param shippingAddress shipping address for the order
     * @param promoCode       promo code to apply
     * @return created sales order DTO
     */
    @PostMapping("/orders/from-cart/promo")
    @Operation(summary = "Create order from cart with promo code",
            description = "Creates a new sales order from the cart with an optional promo code discount")
    public ResponseEntity<ApiResponse<SalesOrderDto>> createOrderFromCartWithPromo(
            @Parameter(description = "Shipping address", example = "123 Main St, Moscow, Russia", required = true)
            @RequestParam String shippingAddress,
            @Parameter(description = "Promo code", example = "WELCOME10")
            @RequestParam String promoCode) {

        Long userId = getCurrentUserId();
        log.info(messageService.get("user.order.create.from.cart.promo.start", userId, promoCode));

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
     * Retrieves a paginated list of all orders for the authenticated user.
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @return page of sales order DTOs
     */
    @GetMapping("/orders")
    @Operation(summary = "Get user's orders",
            description = "Retrieves a paginated list of all orders for the authenticated user")
    public ResponseEntity<ApiResponse<Page<SalesOrderDto>>> getUserOrders(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();
        log.debug(messageService.get("user.orders.fetch.start", userId));

        Page<SalesOrderDto> orders = salesOrderService.getCustomerOrders(userId, null, null, null, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.orders.fetched"),
                orders));
    }

    /**
     * Retrieves detailed information about a specific order by its ID.
     *
     * @param orderId order ID
     * @return sales order DTO
     */
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order by ID",
            description = "Retrieves detailed information about a specific order by its ID")
    public ResponseEntity<ApiResponse<SalesOrderDto>> getOrder(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long orderId) {

        Long userId = getCurrentUserId();
        log.debug(messageService.get("user.order.fetch.start", userId, orderId));

        SalesOrderDto order = salesOrderService.getSalesOrderById(orderId);

        // Verify order belongs to the user
        if (!order.getUserId().equals(userId)) {
            log.warn(messageService.get("user.order.access.denied", userId, orderId));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(messageService.get("user.order.access.denied.message")));
        }

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.order.fetched"),
                order));
    }

    /**
     * Retrieves detailed information about a specific order by its order number.
     *
     * @param orderNumber order number
     * @return sales order DTO
     */
    @GetMapping("/orders/by-number/{orderNumber}")
    @Operation(summary = "Get order by number",
            description = "Retrieves detailed information about a specific order by its order number")
    public ResponseEntity<ApiResponse<SalesOrderDto>> getOrderByNumber(
            @Parameter(description = "Order number", example = "SO-20240330-001", required = true)
            @PathVariable String orderNumber) {

        Long userId = getCurrentUserId();
        log.debug(messageService.get("user.order.fetch.by.number.start", userId, orderNumber));

        SalesOrderDto order = salesOrderService.getSalesOrderByNumber(orderNumber);

        if (!order.getUserId().equals(userId)) {
            log.warn(messageService.get("user.order.access.denied", userId, orderNumber));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(messageService.get("user.order.access.denied.message")));
        }

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.order.fetched"),
                order));
    }

    /**
     * Cancels an existing order that is in a cancellable state.
     *
     * @param orderId order ID
     * @param reason  cancellation reason
     * @return cancelled sales order DTO
     */
    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Cancel order",
            description = "Cancels an existing order that is in a cancellable state")
    public ResponseEntity<ApiResponse<SalesOrderDto>> cancelOrder(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "Cancellation reason", example = "Customer changed mind", required = true)
            @RequestParam String reason) {

        Long userId = getCurrentUserId();
        log.info(messageService.get("user.order.cancel.start", userId, orderId));

        SalesOrderDto order = salesOrderService.getSalesOrderById(orderId);

        if (!order.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(messageService.get("user.order.access.denied.message")));
        }

        SalesOrderDto cancelledOrder = salesOrderService.cancelOrder(orderId, reason, userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.order.cancelled"),
                cancelledOrder));
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Calculates the total amount of money the user has spent on all completed orders.
     *
     * @return total amount spent
     */
    @GetMapping("/stats/spent")
    @Operation(summary = "Get total amount spent by user",
            description = "Calculates the total amount of money the user has spent on all completed orders")
    public ResponseEntity<ApiResponse<java.math.BigDecimal>> getTotalSpent() {
        Long userId = getCurrentUserId();
        log.debug(messageService.get("user.stats.spent.start", userId));

        java.math.BigDecimal totalSpent = salesOrderService.getUserTotalSpent(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("user.stats.spent.fetched"),
                totalSpent));
    }
}