package ru.galtor85.household_store.processor.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cart.CartNotFoundException;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartStatus;
import ru.galtor85.household_store.repository.cart.CartRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartProcessor {

    private final CartRepository cartRepository;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public Cart findActiveCart(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn(messageService.get("order.log.cart.not.found", userId));
                    return new CartNotFoundException(userId);
                });
    }

    @Transactional
    public void completeCart(Cart cart) {
        cart.setStatus(CartStatus.COMPLETED);
        cartRepository.save(cart);
        log.debug(messageService.get("order.log.cart.completed", cart.getId()));
    }
}