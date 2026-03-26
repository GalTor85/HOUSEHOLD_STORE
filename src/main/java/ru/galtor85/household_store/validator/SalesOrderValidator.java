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
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.error.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
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
}