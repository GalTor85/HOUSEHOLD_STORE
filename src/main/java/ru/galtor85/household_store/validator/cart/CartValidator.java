package ru.galtor85.household_store.validator.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cart.CartEmptyException;
import ru.galtor85.household_store.advice.exception.product.ProductInactiveException;
import ru.galtor85.household_store.advice.exception.stock.InsufficientStockException;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.product.Product;
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

    /**
     * Validates that a cart is not empty.
     *
     * @param cart the cart to validate
     * @throws CartEmptyException if cart is null, has no items, or items list is empty
     */
    public void validateCartNotEmpty(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn(messageService.get("cart.validation.empty"));
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
            log.warn(messageService.get("cart.validation.product.inactive", product.getId()));
            throw new ProductInactiveException(product.getId());
        }
    }

    /**
     * Validates that sufficient stock is available for a product.
     *
     * @param product the product to validate
     * @param requestedQuantity the quantity requested to add to cart
     * @throws InsufficientStockException if requested quantity exceeds available stock
     */
    public void validateStockAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(messageService.get("cart.validation.insufficient.stock",
                    product.getName(), product.getQuantityInStock(), requestedQuantity));
            throw new InsufficientStockException(product.getName(), product.getQuantityInStock());
        }
    }
}