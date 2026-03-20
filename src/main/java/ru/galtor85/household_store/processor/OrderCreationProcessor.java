package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.OrderRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.util.OrderNumberGenerator;
import ru.galtor85.household_store.util.OrderTypeDeterminer;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreationProcessor {

    private final OrderRepository orderRepository;
    private final MessageService messageService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderTypeDeterminer orderTypeDeterminer;

    @Transactional
    public Order createOrderFromCart(Cart cart, Long userId, String shippingAddress) {
        String orderNumber = orderNumberGenerator.generateOrderNumber();
        OrderType orderType = orderTypeDeterminer.determineOrderType(userId);

        log.debug(messageService.get("order.log.generated.number", orderNumber));

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .orderType(orderType)
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
        }

        Order savedOrder = orderRepository.save(order);
        log.debug(messageService.get("order.log.saved", savedOrder.getId()));

        return savedOrder;
    }
}