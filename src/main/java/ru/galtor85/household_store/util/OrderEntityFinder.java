package ru.galtor85.household_store.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderItem;
import ru.galtor85.household_store.entity.OrderType;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.repository.OrderItemRepository;
import ru.galtor85.household_store.repository.OrderRepository;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEntityFinder {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MessageService messageService;

    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });
    }

    public Order findCustomerOrderById(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderType() != OrderType.RETAIL && order.getOrderType() != OrderType.WHOLESALE) {
            log.error(messageService.get("manager.order.log.not.customer.order", orderId));
            throw new InvalidOrderTypeException(orderId, "RETAIL or WHOLESALE");
        }
        return order;
    }

    public OrderItem findOrderItem(Order order, Long itemId) {
        return order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.item.not.found", itemId));
                    return new OrderItemNotFoundException(itemId);
                });
    }

    public Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }
}