package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.SalesOrderCreateRequest;
import ru.galtor85.household_store.dto.SalesOrderItemCreateDto;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.SalesOrderRepository;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderValidator {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final MessageService messageService;

    // =========================================================================
    // ВАЛИДАЦИЯ ПОЛЬЗОВАТЕЛЯ
    // =========================================================================

    /**
     * Проверяет существование пользователя
     */
    public User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    /**
     * Проверяет, что пользователь активен
     */
    public void validateUserActive(Long userId) {
        User user = validateUserExists(userId);
        // TODO: добавить проверку активности пользователя, если есть поле active в User
        // if (!user.isActive()) {
        //     throw new UserNotActiveException("User is not active");
        // }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ТОВАРОВ
    // =========================================================================

    /**
     * Проверяет все товары в заказе и возвращает список продуктов и цен
     */
    public ProductValidationResult validateProducts(List<SalesOrderItemCreateDto> items) {
        List<Product> products = new ArrayList<>();
        List<BigDecimal> prices = new ArrayList<>();

        for (SalesOrderItemCreateDto item : items) {
            Product product = validateProductExists(item.getProductId());
            validateProductAvailability(product, item.getQuantity());

            BigDecimal price = determinePrice(product, item);
            validatePrice(price, product);

            products.add(product);
            prices.add(price);
        }

        return new ProductValidationResult(products, prices);
    }

    /**
     * Проверяет существование товара
     */
    public Product validateProductExists(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.error.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    /**
     * Проверяет наличие товара на складе
     */
    public void validateProductAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(messageService.get("sales.validation.insufficient.stock",
                    product.getName(), product.getQuantityInStock(), requestedQuantity));
            throw new InsufficientStockException(product.getName(), product.getQuantityInStock());
        }
    }

    /**
     * Определяет цену товара (кастомная или из продукта)
     */
    private BigDecimal determinePrice(Product product, SalesOrderItemCreateDto item) {
        if (item.getCustomPrice() != null) {
            return item.getCustomPrice();
        }
        return product.getPrice();
    }

    /**
     * Проверяет цену
     */
    private void validatePrice(BigDecimal price, Product product) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(messageService.get("sales.validation.invalid.price", product.getSku()));
            throw new InvalidPriceException(price);
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ЗАКАЗА
    // =========================================================================

    /**
     * Проверяет, что заказ не пустой
     */
    public void validateNotEmpty(SalesOrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.error(messageService.get("sales.validation.items.empty"));
            throw new IllegalArgumentException(
                    messageService.get("sales.validation.items.empty")
            );
        }
    }

    /**
     * Проверяет существование заказа на продажу
     */
    public SalesOrder validateSalesOrderExists(Long orderId) {
        return salesOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("sales.order.not.found", orderId));
                    return new SalesOrderNotFoundException(orderId);
                });
    }

    /**
     * Проверяет, что заказ принадлежит пользователю
     */
    public void validateOrderBelongsToUser(Long orderId, Long userId) {
        SalesOrder order = validateSalesOrderExists(orderId);
        if (!order.getUserId().equals(userId)) {
            log.error(messageService.get("sales.order.not.belong.to.user", orderId, userId));
            throw new UserAccessException(
                    messageService.get("sales.order.not.belong.to.user", orderId, userId)
            );
        }
    }

    /**
     * Проверяет, можно ли модифицировать заказ
     */
    public void validateOrderModifiable(SalesOrder order, OrderStatus... allowedStatuses) {
        for (OrderStatus allowed : allowedStatuses) {
            if (order.getStatus() == allowed) {
                return;
            }
        }
        log.warn(messageService.get("sales.order.cannot.modify", order.getStatus()));
        throw new OrderModificationNotAllowedException(order.getStatus());
    }

    /**
     * Проверяет, что товар еще не добавлен в заказ
     */
    public void validateItemNotExists(SalesOrder order, Long productId) {
        boolean exists = order.getItems().stream()
                .anyMatch(item -> item.getProductId().equals(productId));
        if (exists) {
            log.warn(messageService.get("sales.order.item.exists", productId));
            throw new OrderItemAlreadyExistsException(productId);
        }
    }

    /**
     * Проверяет существование позиции в заказе
     */
    public SalesOrderItem validateOrderItemExists(SalesOrder order, Long itemId) {
        return order.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("sales.order.item.not.found", itemId));
                    return new OrderItemNotFoundException(itemId);
                });
    }

    // =========================================================================
    // ВАЛИДАЦИЯ СТАТУСОВ
    // =========================================================================

    /**
     * Проверяет, что статус допустим для перехода
     */
    public void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (!currentStatus.isValidTransitionForSale(newStatus)) {
            log.warn(messageService.get("sales.order.invalid.status.transition",
                    currentStatus, newStatus));
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }
    }

    /**
     * Проверяет, что статус не является финальным
     */
    public void validateNotFinalStatus(OrderStatus status) {
        if (status.isFinalForSale()) {
            log.warn(messageService.get("sales.order.final.status", status));
            throw new IllegalStateException(
                    messageService.get("sales.order.final.status", status)
            );
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ДАННЫХ ЗАКАЗА
    // =========================================================================

    /**
     * Проверяет адрес доставки
     */
    public void validateShippingAddress(String shippingAddress) {
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            log.warn(messageService.get("sales.validation.shipping.address.empty"));
            throw new IllegalArgumentException(
                    messageService.get("sales.validation.shipping.address.empty")
            );
        }
    }

    /**
     * Проверяет способ оплаты
     */
    public void validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            log.warn(messageService.get("sales.validation.payment.method.empty"));
            throw new IllegalArgumentException(
                    messageService.get("sales.validation.payment.method.empty")
            );
        }
    }

    // =========================================================================
    // ВНУТРЕННИЕ КЛАССЫ
    // =========================================================================

    /**
     * Результат валидации товаров
     */
    @lombok.Value
    public static class ProductValidationResult {
        List<Product> products;
        List<BigDecimal> prices;
    }
}