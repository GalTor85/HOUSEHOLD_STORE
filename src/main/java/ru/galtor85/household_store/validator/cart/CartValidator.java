package ru.galtor85.household_store.validator.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cart.CartEmptyException;
import ru.galtor85.household_store.advice.exception.product.ProductInactiveException;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Validator for shopping cart operations.
 *
 * <p>This validator ensures that cart operations are performed on valid data:</p>
 * <ul>
 *   <li>Cart is not empty before checkout</li>
 *   <li>Products added to cart are active</li>
 *   <li>Sufficient stock is available for products</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartValidator {

    private final MessageService messageService;
    private final BusinessConfig businessConfig;
    private final LogMessageService logMsg;

    /**
     * Validates that a cart is not empty.
     *
     * @param cart the cart to validate
     * @throws CartEmptyException if cart is null, has no items, or items list is empty
     */
    public void validateCartNotEmpty(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn(logMsg.get("cart.validation.empty"));
            throw new CartEmptyException();
        }
    }

    /**
     * Validates that a product is active and available for purchase.
     *
     * @param product the product to validate
     * @throws ProductInactiveException if product is not active
     */
    public void validateProductActive(Product product) {
        if (!product.isActive()) {
            log.warn(logMsg.get("cart.validation.product.inactive", product.getId()));
            throw new ProductInactiveException(product.getId());
        }
    }

    /**
     * Validates that adding items to cart does not exceed the maximum allowed items.
     *
     * @param cart            the current cart
     * @param additionalItems number of items to be added
     * @throws IllegalArgumentException if cart would exceed maximum items limit
     */
    public void validateCartMaxItems(Cart cart, int additionalItems) {
        if (cart == null) {
            return;
        }

        int currentItems = cart.getItemsCount() != null ? cart.getItemsCount() : 0;
        Integer maxItems = businessConfig.getCart().getMaxItems();

        // If maxItems is not configured, skip validation
        if (maxItems == null) {
            return;
        }

        if (currentItems + additionalItems > maxItems) {
            log.warn(logMsg.get("cart.validation.max.items.exceeded", maxItems));
            throw new IllegalArgumentException(
                    messageService.get("cart.validation.max.items.exceeded", maxItems)
            );
        }
    }

    /**
     * Validates that adding one new item to cart does not exceed the maximum allowed items.
     *
     * @param cart the current cart
     * @throws IllegalArgumentException if cart would exceed maximum items limit
     */
    public void validateCartMaxItems(Cart cart) {
        validateCartMaxItems(cart, businessConfig.getCart().getDefaultIncrementValue());
    }

    /**
     * Validates that the quantity of a single product does not exceed the maximum allowed.
     *
     * @param quantity the requested quantity
     * @throws IllegalArgumentException if quantity exceeds maximum allowed per item
     */
    public void validateMaxQuantityPerItem(int quantity) {
        Integer maxQuantityPerItem = businessConfig.getCart().getMaxQuantityPerItem();

        // If maxQuantityPerItem is not configured, skip validation
        if (maxQuantityPerItem == null) {
            return;
        }

        if (quantity > maxQuantityPerItem) {
            log.warn(logMsg.get("cart.validation.max.quantity.per.item.exceeded",
                    maxQuantityPerItem, quantity));
            throw new IllegalArgumentException(
                    messageService.get("cart.validation.max.quantity.per.item.exceeded",
                            maxQuantityPerItem, quantity)
            );
        }
    }
}