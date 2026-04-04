package ru.galtor85.household_store.service.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cart.CartEmptyException;
import ru.galtor85.household_store.advice.exception.cart.CartNotFoundException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.dto.request.cart.AddToCartRequest;
import ru.galtor85.household_store.dto.request.cart.UpdateCartItemRequest;
import ru.galtor85.household_store.dto.response.cart.CartDto;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartItem;
import ru.galtor85.household_store.entity.cart.CartStatus;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.mapper.cart.CartMapper;
import ru.galtor85.household_store.processor.cart.CartProcessor;
import ru.galtor85.household_store.repository.cart.CartItemRepository;
import ru.galtor85.household_store.repository.cart.CartRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.cart.CartValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing shopping cart operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;
    private final CartValidator cartValidator;
    private final CartProcessor cartProcessor;
    private final MessageService messageService;

    private static final int CART_EXPIRY_DAYS = 30;

    // =========================================================================
    // CART RETRIEVAL
    // =========================================================================

    /**
     * Retrieves the active cart for a user
     *
     * @param userId user identifier
     * @return active cart DTO
     * @throws CartNotFoundException if no active cart exists
     */
    @Transactional(readOnly = true)
    public CartDto getActiveCart(Long userId) {
        Cart cart = cartProcessor.findActiveCart(userId);
        return cartMapper.toDto(cart);
    }

    /**
     * Retrieves all carts (including inactive) for a user
     *
     * @param userId user identifier
     * @return list of cart DTOs
     */
    @Transactional(readOnly = true)
    public List<CartDto> getUserCarts(Long userId) {
        return cartRepository.findByUserId(userId).stream()
                .map(cartMapper::toDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // CART OPERATIONS
    // =========================================================================

    /**
     * Adds a product to the user's cart
     *
     * @param userId  user identifier
     * @param request add to cart request with product ID and quantity
     * @return updated cart DTO
     */
    @Transactional
    public CartDto addToCart(Long userId, AddToCartRequest request) {
        log.info(messageService.get("cart.service.add.start", userId, request.getProductId(), request.getQuantity()));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(request.getProductId()));

        cartValidator.validateProductActive(product);
        cartValidator.validateStockAvailability(product, request.getQuantity());

        Cart cart = getOrCreateActiveCart(userId);
        CartItem existingItem = findCartItem(cart.getId(), request.getProductId());

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            log.debug(messageService.get("cart.service.item.updated", request.getProductId(), newQuantity));
        } else {
            CartItem newItem = createCartItem(cart, product, request.getQuantity());
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
            log.debug(messageService.get("cart.service.item.added", request.getProductId()));
        }

        cart.recalculateTotal();
        Cart savedCart = cartRepository.save(cart);

        log.info(messageService.get("cart.service.add.complete", userId, savedCart.getItemsCount()));

        return cartMapper.toDto(savedCart);
    }

    /**
     * Updates the quantity of an item in the cart
     *
     * @param userId    user identifier
     * @param productId product identifier
     * @param request   update request with new quantity
     * @return updated cart DTO
     */
    @Transactional
    public CartDto updateCartItem(Long userId, Long productId, UpdateCartItemRequest request) {
        log.info(messageService.get("cart.service.update.start", userId, productId, request.getQuantity()));

        Cart cart = cartProcessor.findActiveCart(userId);
        CartItem item = findCartItemOrThrow(cart.getId(), productId);

        if (request.getQuantity() <= 0) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
            log.debug(messageService.get("cart.service.item.removed", productId));
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            cartValidator.validateStockAvailability(product, request.getQuantity());

            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
            log.debug(messageService.get("cart.service.item.updated", productId, request.getQuantity()));
        }

        cart.recalculateTotal();
        Cart savedCart = cartRepository.save(cart);

        log.info(messageService.get("cart.service.update.complete", userId, savedCart.getItemsCount()));

        return cartMapper.toDto(savedCart);
    }

    /**
     * Removes a product from the cart
     *
     * @param userId    user identifier
     * @param productId product identifier
     * @return updated cart DTO
     */
    @Transactional
    public CartDto removeFromCart(Long userId, Long productId) {
        log.info(messageService.get("cart.service.remove.start", userId, productId));

        Cart cart = cartProcessor.findActiveCart(userId);
        CartItem item = findCartItemOrThrow(cart.getId(), productId);

        cart.removeItem(item);
        cartItemRepository.delete(item);

        cart.recalculateTotal();
        Cart savedCart = cartRepository.save(cart);

        log.info(messageService.get("cart.service.remove.complete", userId));

        return cartMapper.toDto(savedCart);
    }

    /**
     * Clears all items from the cart
     *
     * @param userId user identifier
     */
    @Transactional
    public void clearCart(Long userId) {
        log.info(messageService.get("cart.service.clear.start", userId));

        Cart cart = cartProcessor.findActiveCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.clear();
        cartRepository.save(cart);

        log.info(messageService.get("cart.service.clear.complete", userId));
    }

    // =========================================================================
    // CHECKOUT OPERATIONS
    // =========================================================================

    /**
     * Prepares the cart for checkout
     *
     * @param userId user identifier
     * @return cart DTO with CHECKOUT status
     * @throws CartEmptyException if cart is empty
     */
    @Transactional
    public CartDto checkoutCart(Long userId) {
        log.info(messageService.get("cart.service.checkout.start", userId));

        Cart cart = cartProcessor.findActiveCart(userId);
        cartValidator.validateCartNotEmpty(cart);

        cart.setStatus(CartStatus.CHECKOUT);
        cart.setExpiresAt(LocalDateTime.now().plusDays(CART_EXPIRY_DAYS));
        Cart savedCart = cartRepository.save(cart);

        log.info(messageService.get("cart.service.checkout.complete", userId, savedCart.getId()));

        return cartMapper.toDto(savedCart);
    }

    /**
     * Marks the cart as completed (after order creation)
     *
     * @param userId user identifier
     */
    @Transactional
    public void completeCart(Long userId) {
        Cart cart = cartProcessor.findActiveCart(userId);
        cartProcessor.completeCart(cart);
        log.info(messageService.get("cart.service.completed", userId, cart.getId()));
    }

    /**
     * Marks the cart as abandoned (user left checkout)
     *
     * @param userId user identifier
     */
    @Transactional
    public void abandonCart(Long userId) {
        Cart cart = cartProcessor.findActiveCart(userId);
        cart.setStatus(CartStatus.ABANDONED);
        cartRepository.save(cart);
        log.info(messageService.get("cart.service.abandoned", userId, cart.getId()));
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Gets the total number of items in the user's cart
     *
     * @param userId user identifier
     * @return number of items (0 if no active cart)
     */
    @Transactional(readOnly = true)
    public int getCartItemsCount(Long userId) {
        try {
            Cart cart = cartProcessor.findActiveCart(userId);
            return cart.getItemsCount() != null ? cart.getItemsCount() : 0;
        } catch (CartNotFoundException e) {
            return 0;
        }
    }

    /**
     * Gets the total amount of the user's cart
     *
     * @param userId user identifier
     * @return total amount (0 if no active cart)
     */
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(Long userId) {
        try {
            Cart cart = cartProcessor.findActiveCart(userId);
            return cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO;
        } catch (CartNotFoundException e) {
            return BigDecimal.ZERO;
        }
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Gets the active cart or creates a new one if none exists
     *
     * @param userId user identifier
     * @return active cart entity
     */
    private Cart getOrCreateActiveCart(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(userId));
    }

    /**
     * Creates a new empty cart for a user
     *
     * @param userId user identifier
     * @return newly created cart entity
     */
    private Cart createNewCart(Long userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .status(CartStatus.ACTIVE)
                .totalAmount(BigDecimal.ZERO)
                .itemsCount(0)
                .expiresAt(LocalDateTime.now().plusDays(CART_EXPIRY_DAYS))
                .build();
        return cartRepository.save(cart);
    }

    /**
     * Finds a cart item by cart ID and product ID
     *
     * @param cartId    cart identifier
     * @param productId product identifier
     * @return cart item or null if not found
     */
    private CartItem findCartItem(Long cartId, Long productId) {
        return cartItemRepository.findByCartIdAndProductId(cartId, productId).orElse(null);
    }

    /**
     * Finds a cart item or throws exception
     *
     * @param cartId    cart identifier
     * @param productId product identifier
     * @return cart item
     * @throws IllegalArgumentException if item not found
     */
    private CartItem findCartItemOrThrow(Long cartId, Long productId) {
        return cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("cart.service.item.not.found", productId)));
    }

    /**
     * Creates a new cart item from product data
     *
     * @param cart     cart entity
     * @param product  product entity
     * @param quantity quantity to add
     * @return newly created cart item
     */
    private CartItem createCartItem(Cart cart, Product product, int quantity) {
        return CartItem.builder()
                .cart(cart)
                .productId(product.getId())
                .quantity(quantity)
                .price(product.getPrice())
                .productName(product.getName())
                .sku(product.getSku())
                .category(product.getCategory())
                .build();
    }
}