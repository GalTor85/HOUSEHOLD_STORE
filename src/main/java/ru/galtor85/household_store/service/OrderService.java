package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
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
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("order.log.creation.start", userId));

        // Получаем активную корзину пользователя
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn(messageService.get("order.log.cart.not.found", userId));
                    return new CartNotFoundException(userId);
                });

        if (cart.getItems().isEmpty()) {
            log.warn(messageService.get("order.log.cart.empty", userId));
            throw new CartEmptyException();
        }

        // Проверяем наличие товаров на складе
        validateStockAvailability(cart, locale);

        // Генерируем номер заказа
        String orderNumber = generateOrderNumber();
        log.debug(messageService.get("order.log.generated.number", orderNumber));

        try {
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

                // Обновляем остатки
                updateProductStock(cartItem.getProductId(), cartItem.getQuantity(), locale);
            }

            Order savedOrder = orderRepository.save(order);

            // Очищаем корзину или меняем статус
            cart.setStatus(CartStatus.COMPLETED);
            cartRepository.save(cart);

            log.info(messageService.get("order.log.created.success", orderNumber, userId));
            return savedOrder;

        } catch (Exception e) {
            log.error(messageService.get("order.log.creation.error", orderNumber, e.getMessage()), e);
            throw new OrderCreationException(orderNumber,
                    messageService.get("order.error.creation.failed", e.getMessage()));
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private OrderType determineOrderType(Long userId) {
        // TODO: Реализовать определение типа пользователя
        // Например, проверка роли или настроек пользователя
        return OrderType.RETAIL;
    }

    private void validateStockAvailability(Cart cart, Locale locale) {
        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> {
                        log.error(messageService.get("order.log.product.not.found", item.getProductId()));
                        return new ProductNotFoundException(item.getProductId());
                    });

            if (product.getQuantityInStock() < item.getQuantity()) {
                log.warn(messageService.get(
                        "order.log.insufficient.stock",
                        product.getName(),
                        product.getQuantityInStock(),
                        item.getQuantity()
                ));
                throw new InsufficientStockException(
                        product.getId()+"-"+product.getName()+"-"+product.getQuantityInStock(),
                        item.getQuantity()
                );
            }
        }
    }

    private void updateProductStock(Long productId, Integer quantity, Locale locale) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("order.log.product.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        int newQuantity = product.getQuantityInStock() - quantity;

        if (newQuantity < 0) {
            log.error(messageService.get(
                    "order.log.stock.negative",
                    productId,
                    product.getQuantityInStock(),
                    quantity
            ));
            throw new InsufficientStockException(
                    productId+"-"+product.getName()+"-"+product.getQuantityInStock(),
                    quantity
            );
        }

        product.setQuantityInStock(newQuantity);
        productRepository.save(product);

        log.debug(messageService.get(
                "order.log.stock.updated",
                productId,
                product.getQuantityInStock(),
                newQuantity
        ));
    }
}