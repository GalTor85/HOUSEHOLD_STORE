package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.CartRepository;
import ru.galtor85.household_store.repository.OrderItemRepository;
import ru.galtor85.household_store.repository.OrderRepository;
import ru.galtor85.household_store.repository.ProductRepository;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final MessageService messageService;

    @Transactional
    public Order createOrderFromCart(Long userId, String shippingAddress, Locale locale) {
        // Получаем активную корзину пользователя
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException(messageService.get("cart.not.found", userId)));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException(messageService.get("cart.empty"));
        }

        // Генерируем номер заказа
        String orderNumber = generateOrderNumber();

        // Создаем заказ
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .orderType(determineOrderType(userId))
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .subtotal(cart.getTotalAmount())
                .totalAmount(cart.getTotalAmount())
                .build();

        // Копируем товары из корзины
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .productName(cartItem.getProductName())
                    .productSku(cartItem.getSku())
                    .build();
            order.addItem(orderItem);

            // Обновляем остатки (если нужно)
            updateProductStock(cartItem.getProductId(), cartItem.getQuantity());
        }

        Order savedOrder = orderRepository.save(order);

        // Очищаем корзину или меняем статус
        cart.setStatus(CartStatus.COMPLETED);
        cartRepository.save(cart);

        log.info(messageService.get("order.created", orderNumber, userId));
        return savedOrder;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private OrderType determineOrderType(Long userId) {
        // Определяем тип заказа по UserType
        // Например, если пользователь оптовый - то WHOLESALE
        return OrderType.RETAIL; // По умолчанию
    }

    private void updateProductStock(Long productId, Integer quantity) {
        // Уменьшаем остатки
        // Можно добавить логику
    }
}