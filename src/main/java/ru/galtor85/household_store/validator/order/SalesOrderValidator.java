package ru.galtor85.household_store.validator.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.advice.exception.cart.CartEmptyException;
import ru.galtor85.household_store.advice.exception.order.*;
import ru.galtor85.household_store.advice.exception.stock.InsufficientStockException;
import ru.galtor85.household_store.advice.exception.validation.InvalidDateRangeException;
import ru.galtor85.household_store.advice.exception.validation.InvalidPriceException;
import ru.galtor85.household_store.dto.common.SalesOrderItemCreateDto;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartItem;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.product.ProductValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for sales order operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderValidator {

    private final UserRepository userRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final ProductValidator productValidator;

    /**
     * Validates cart is not empty.
     *
     * @param cart cart entity
     * @throws CartEmptyException if empty
     */
    public void validateCartNotEmpty(Cart cart) {
        if (cart == null) {
            log.error(logMsg.get("sales.validator.cart.null"));
            throw new IllegalArgumentException(messageService.get("sales.validator.cart.null"));
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.error(logMsg.get("sales.validator.cart.empty"));
            throw new CartEmptyException();
        }
    }

    /**
     * Validates cart items have valid quantity and price.
     *
     * @param cart cart entity
     * @throws IllegalArgumentException if invalid
     */
    public void validateCartItems(Cart cart) {
        for (CartItem item : cart.getItems()) {
            if (item.getQuantity() <= 0) {
                log.error(logMsg.get("sales.validator.cart.item.invalid.quantity", item.getProductId()));
                throw new IllegalArgumentException(
                        messageService.get("sales.validator.cart.item.invalid.quantity", item.getProductId()));
            }
            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                log.error(logMsg.get("sales.validator.cart.item.invalid.price", item.getProductId()));
                throw new IllegalArgumentException(
                        messageService.get("sales.validator.cart.item.invalid.price", item.getProductId()));
            }
        }
    }

    /**
     * Validates user exists.
     *
     * @param userId user ID
     * @return user entity
     * @throws UserNotFoundException if not found
     */
    public User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("user.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    /**
     * Validates products exist and have sufficient stock.
     *
     * @param items order items
     * @return validation result with products and prices
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
     * Validates product exists.
     *
     * @param productId product ID
     * @return product entity
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException if not found
     */
    public Product validateProductExists(Long productId) {
        return productValidator.validateProductExists(productId);
    }

    /**
     * Validates sufficient stock availability.
     *
     * @param product product entity
     * @param requestedQuantity requested quantity
     * @throws InsufficientStockException if insufficient stock
     */
    public void validateProductAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(logMsg.get("sales.validation.insufficient.stock",
                    product.getName(), product.getQuantityInStock(), requestedQuantity));
            throw new InsufficientStockException(product.getName(), product.getQuantityInStock());
        }
    }

    private BigDecimal determinePrice(Product product, SalesOrderItemCreateDto item) {
        return item.getCustomPrice() != null ? item.getCustomPrice() : product.getPrice();
    }

    private void validatePrice(BigDecimal price, Product product) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(logMsg.get("sales.validation.invalid.price", product.getSku()));
            throw new InvalidPriceException(price);
        }
    }

    /**
     * Validates order has items.
     *
     * @param request creation request
     * @throws IllegalArgumentException if no items
     */
    public void validateNotEmpty(SalesOrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.error(logMsg.get("sales.validation.items.empty"));
            throw new IllegalArgumentException(messageService.get("sales.validation.items.empty"));
        }
    }

    /**
     * Validates order creation request.
     *
     * @param request creation request
     * @throws IllegalArgumentException if invalid
     */
    public void validateCreateRequest(SalesOrderCreateRequest request) {
        validateNotEmpty(request);
        if (request.getUserId() == null) {
            log.error(logMsg.get("sales.validation.user.id.empty"));
            throw new IllegalArgumentException(messageService.get("sales.validation.user.id.empty"));
        }
    }

    /**
     * Validates sales order exists.
     *
     * @param orderId order ID
     * @return sales order entity
     * @throws SalesOrderNotFoundException if not found
     */
    public SalesOrder validateSalesOrderExists(Long orderId) {
        return salesOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("sales.order.not.found", orderId));
                    return new SalesOrderNotFoundException(orderId);
                });
    }

    /**
     * Validates order item exists in order.
     *
     * @param order sales order
     * @param itemId item ID
     * @return order item entity
     * @throws OrderItemNotFoundException if not found
     */
    public SalesOrderItem validateOrderItemExists(SalesOrder order, Long itemId) {
        return order.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(logMsg.get("sales.order.item.not.found", itemId, order.getId()));
                    return new OrderItemNotFoundException(itemId);
                });
    }

    /**
     * Validates date range is valid.
     *
     * @param start start date
     * @param end end date
     * @throws InvalidDateRangeException if start after end
     */
    public void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            log.warn(logMsg.get("sales.validation.date.range.invalid", start, end));
            throw new InvalidDateRangeException(start, end);
        }
    }

    /**
     * Parses order status from string.
     *
     * @param status status string
     * @return OrderStatus or null if invalid
     */
    public OrderStatus parseOrderStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn(logMsg.get("sales.validation.status.parse.failed", status));
            return null;
        }
    }

    /**
     * Parses and validates order status.
     *
     * @param status status string
     * @return OrderStatus
     * @throws InvalidOrderStatusException if invalid
     */
    public OrderStatus parseAndValidateOrderStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn(logMsg.get("sales.validation.status.invalid", status));
            throw new InvalidOrderStatusException(status);
        }
    }

    /**
     * Validates status transition is allowed.
     *
     * @param currentStatus current status
     * @param newStatus new status
     * @throws InvalidOrderStatusException if transition not allowed
     */
    public void validateStatusTransitionForSale(OrderStatus currentStatus, OrderStatus newStatus) {
        if (!currentStatus.isValidTransitionForSale(newStatus)) {
            log.warn(logMsg.get("sales.validation.status.transition.invalid", currentStatus, newStatus));
            throw new InvalidOrderStatusException(
                    messageService.get("sales.validation.status.transition.invalid", currentStatus, newStatus));
        }
    }

    /**
     * Validates order can be modified.
     *
     * @param order sales order
     * @param allowedStatuses allowed statuses for modification
     * @throws IllegalStateException if you cannot modify
     */
    public void validateOrderModifiable(SalesOrder order, OrderStatus... allowedStatuses) {
        for (OrderStatus allowed : allowedStatuses) {
            if (order.getStatus() == allowed) {
                return;
            }
        }
        log.warn(logMsg.get("sales.validation.order.cannot.modify", order.getStatus()));
        throw new IllegalStateException(
                messageService.get("sales.validation.order.cannot.modify", order.getStatus()));
    }

    /**
     * Validates price is not negative.
     *
     * @param price price to validate
     * @throws InvalidPriceException if invalid
     */
    public void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            log.warn(logMsg.get("sales.validation.price.invalid", price));
            throw new InvalidPriceException(price);
        }
    }

    public record ProductValidationResult(List<Product> products, List<BigDecimal> prices) {}
}