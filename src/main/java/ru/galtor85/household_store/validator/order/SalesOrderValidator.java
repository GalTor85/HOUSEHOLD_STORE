package ru.galtor85.household_store.validator.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.advice.exception.cart.CartEmptyException;
import ru.galtor85.household_store.advice.exception.order.*;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.stock.InsufficientStockException;
import ru.galtor85.household_store.advice.exception.validation.InvalidDateRangeException;
import ru.galtor85.household_store.advice.exception.validation.InvalidPriceException;
import ru.galtor85.household_store.advice.exception.validation.InvalidQuantityException;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.dto.common.SalesOrderItemCreateDto;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartItem;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.product.ProductValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final ProductValidator productValidator;

    // =========================================================================
    // ВАЛИДАЦИЯ КОРЗИНЫ
    // =========================================================================

    public void validateCartNotEmpty(Cart cart) {
        if (cart == null) {
            log.error(messageService.get("sales.validator.cart.null"));
            throw new IllegalArgumentException(
                    messageService.get("sales.validator.cart.null")
            );
        }

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.error(messageService.get("sales.validator.cart.empty"));
            throw new CartEmptyException();
        }
    }

    public void validateCartItems(Cart cart) {
        for (CartItem item : cart.getItems()) {
            if (item.getQuantity() <= 0) {
                log.error(messageService.get("sales.validator.cart.item.invalid.quantity",
                        item.getProductId()));
                throw new IllegalArgumentException(
                        messageService.get("sales.validator.cart.item.invalid.quantity",
                                item.getProductId())
                );
            }

            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                log.error(messageService.get("sales.validator.cart.item.invalid.price",
                        item.getProductId()));
                throw new IllegalArgumentException(
                        messageService.get("sales.validator.cart.item.invalid.price",
                                item.getProductId())
                );
            }
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ПОЛЬЗОВАТЕЛЯ
    // =========================================================================

    public User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ТОВАРОВ
    // =========================================================================

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

    public Product validateProductExists(Long productId) {
        return productValidator.validateProductExists(productId);
    }

    public void validateProductAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(messageService.get("sales.validation.insufficient.stock",
                    product.getName(), product.getQuantityInStock(), requestedQuantity));
            throw new InsufficientStockException(product.getName(), product.getQuantityInStock());
        }
    }

    private BigDecimal determinePrice(Product product, SalesOrderItemCreateDto item) {
        if (item.getCustomPrice() != null) {
            return item.getCustomPrice();
        }
        return product.getPrice();
    }

    private void validatePrice(BigDecimal price, Product product) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(messageService.get("sales.validation.invalid.price", product.getSku()));
            throw new InvalidPriceException(price);
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ЗАКАЗА
    // =========================================================================

    public void validateNotEmpty(SalesOrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.error(messageService.get("sales.validation.items.empty"));
            throw new IllegalArgumentException(
                    messageService.get("sales.validation.items.empty")
            );
        }
    }

    public void validateCreateRequest(SalesOrderCreateRequest request) {
        validateNotEmpty(request);

        if (request.getUserId() == null) {
            log.error(messageService.get("sales.validation.user.id.empty"));
            throw new IllegalArgumentException(
                    messageService.get("sales.validation.user.id.empty")
            );
        }
    }

    public SalesOrder validateSalesOrderExists(Long orderId) {
        return salesOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("sales.order.not.found", orderId));
                    return new SalesOrderNotFoundException(orderId);
                });
    }

    public void validateOrderModifiable(SalesOrder order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn(messageService.get("sales.order.cannot.modify", order.getStatus()));
            throw new OrderModificationNotAllowedException(order.getStatus());
        }
    }

    public void validateOrderCancellable(SalesOrder order) {
        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.COMPLETED) {
            log.warn(messageService.get("sales.order.cannot.cancel", order.getStatus()));
            throw new IllegalStateException(
                    messageService.get("sales.order.cannot.cancel", order.getStatus())
            );
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ПОЗИЦИЙ ЗАКАЗА
    // =========================================================================

    /**
     * Проверяет существование позиции в заказе по ID
     */
    public SalesOrderItem validateOrderItemExists(SalesOrder order, Long itemId) {
        return order.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("sales.order.item.not.found", itemId, order.getId()));
                    return new OrderItemNotFoundException(itemId);
                });
    }

    /**
     * Проверяет существование позиции в заказе по ID продукта
     */
    public SalesOrderItem validateOrderItemByProductId(SalesOrder order, Long productId) {
        return order.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("sales.order.item.not.found.by.product",
                            productId, order.getId()));
                    return new OrderItemNotFoundException(productId);
                });
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

    // =========================================================================
    // ВНУТРЕННИЕ КЛАССЫ
    // =========================================================================

    @lombok.Value
    public static class ProductValidationResult {
        List<Product> products;
        List<BigDecimal> prices;
    }

    /**
     * Валидирует диапазон дат
     */
    public void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            log.warn(messageService.get("sales.validation.date.range.invalid", start, end));
            throw new InvalidDateRangeException(start, end);
        }
    }


    /**
     * Парсит строку в OrderStatus
     */
    public OrderStatus parseOrderStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn(messageService.get("sales.validation.status.parse.failed", status));
            return null;
        }
    }

    /**
     * Парсит и валидирует статус
     */
    public OrderStatus parseAndValidateOrderStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn(messageService.get("sales.validation.status.invalid", status));
            throw new InvalidOrderStatusException(status);
        }
    }

    /**
     * Проверяет, допустим ли переход статуса для заказа на продажу
     */
    public void validateStatusTransitionForSale(OrderStatus currentStatus, OrderStatus newStatus) {
        if (!currentStatus.isValidTransitionForSale(newStatus)) {
            log.warn(messageService.get("sales.validation.status.transition.invalid",
                    currentStatus, newStatus));
            throw new InvalidOrderStatusException(
                    messageService.get("sales.validation.status.transition.invalid",
                            currentStatus, newStatus)
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
        log.warn(messageService.get("sales.validation.order.cannot.modify", order.getStatus()));
        throw new IllegalStateException(
                messageService.get("sales.validation.order.cannot.modify", order.getStatus())
        );
    }

    /**
     * Проверяет цену
     */
    public void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            log.warn(messageService.get("sales.validation.price.invalid", price));
            throw new InvalidPriceException(price);
        }
    }

    /**
     * Проверяет количество
     */
    public void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 0) {
            log.warn(messageService.get("sales.validation.quantity.invalid", quantity));
            throw new InvalidQuantityException(quantity);
        }
    }

    /**
     * Проверяет положительное количество
     */
    public void validatePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            log.warn(messageService.get("sales.validation.quantity.positive", quantity));
            throw new InvalidQuantityException(quantity);
        }
    }

}