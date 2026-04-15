package ru.galtor85.household_store.processor.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cart.CartNotFoundException;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartStatus;
import ru.galtor85.household_store.repository.cart.CartRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

/**
 * Processor for shopping cart operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartProcessor {

    private final CartRepository cartRepository;
    private final LogMessageService logMsg;

    /**
     * Finds the active cart for a user.
     *
     * @param userId the user ID
     * @return active Cart entity
     * @throws CartNotFoundException if no active cart exists
     */
    @Transactional(readOnly = true)
    public Cart findActiveCart(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn(logMsg.get("order.log.cart.not.found", userId));
                    return new CartNotFoundException(userId);
                });
    }

    /**
     * Marks a cart as completed.
     *
     * @param cart the cart to complete
     */
    @Transactional
    public void completeCart(Cart cart) {
        cart.setStatus(CartStatus.COMPLETED);
        cartRepository.save(cart);
        log.debug(logMsg.get("order.log.cart.completed", cart.getId()));
    }
}