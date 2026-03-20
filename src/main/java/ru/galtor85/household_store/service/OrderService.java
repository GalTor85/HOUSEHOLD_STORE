package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.OrderCreationException;
import ru.galtor85.household_store.entity.Cart;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.processor.CartProcessor;
import ru.galtor85.household_store.processor.OrderCreationProcessor;
import ru.galtor85.household_store.processor.StockUpdateProcessor;
import ru.galtor85.household_store.validator.OrderValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderValidator orderValidator;
    private final CartProcessor cartProcessor;
    private final OrderCreationProcessor orderCreationProcessor;
    private final StockUpdateProcessor stockUpdateProcessor;
    private final MessageService messageService;

    @Transactional
    public Order createOrderFromCart(Long userId, String shippingAddress) {
        log.debug(messageService.get("order.log.creation.start", userId));

        try {
            // 1. Получаем активную корзину
            Cart cart = cartProcessor.findActiveCart(userId);

            // 2. Валидация
            orderValidator.validateCartNotEmpty(cart, userId);
            orderValidator.validateStockAvailability(cart);

            // 3. Создаем заказ
            Order savedOrder = orderCreationProcessor.createOrderFromCart(
                    cart, userId, shippingAddress);

            // 4. Обновляем остатки товаров
            stockUpdateProcessor.updateStockForCart(cart);

            // 5. Очищаем корзину
            cartProcessor.completeCart(cart);

            log.info(messageService.get("order.log.created.success",
                    savedOrder.getOrderNumber(), userId));

            return savedOrder;

        } catch (Exception e) {
            log.error(messageService.get("order.log.creation.error", e.getMessage()), e);
            throw new OrderCreationException("UNKNOWN",
                    messageService.get("order.error.creation.failed", e.getMessage()));
        }
    }
}