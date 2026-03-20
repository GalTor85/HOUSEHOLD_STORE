package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderValidationHelper {

    private final MessageService messageService;

    // ========== ПАРСИНГ И ВАЛИДАЦИЯ СТАТУСОВ ==========

    public OrderStatus parseOrderStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn(messageService.get("manager.order.log.invalid.status", status));
            return null;
        }
    }

    public OrderStatus parseAndValidateOrderStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn(messageService.get("manager.order.log.invalid.status", status));
            throw new InvalidOrderStatusException(status);
        }
    }

    /**
     * Валидация перехода статуса для клиентского заказа (RETAIL/WHOLESALE)
     */
    public void validateStatusTransition(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();

        boolean isValid = switch (currentStatus) {
            case PENDING ->
                    newStatus == OrderStatus.PAID ||
                            newStatus == OrderStatus.CANCELLED;

            case PAID ->
                    newStatus == OrderStatus.PROCESSING ||
                            newStatus == OrderStatus.CANCELLED ||
                            newStatus == OrderStatus.REFUNDED;

            case PROCESSING ->
                    newStatus == OrderStatus.SHIPPED ||
                            newStatus == OrderStatus.CANCELLED;

            case SHIPPED ->
                    newStatus == OrderStatus.DELIVERED;

            case DELIVERED ->
                    newStatus == OrderStatus.COMPLETED ||
                            newStatus == OrderStatus.REFUNDED;

            case COMPLETED ->
                    newStatus == OrderStatus.REFUNDED;

            default -> false;
        };

        if (!isValid) {
            log.warn(messageService.get(
                    "order.error.invalid.status.transition",
                    currentStatus, newStatus
            ));
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }
    }

    /**
     * Валидация перехода статуса для закупки (PURCHASE)
     */
    public void validatePurchaseStatusTransition(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();

        boolean isValid = switch (currentStatus) {
            case PENDING ->
                    newStatus == OrderStatus.PAID ||           // Оплатили поставщику
                            newStatus == OrderStatus.PROCESSING ||     // Начали обработку
                            newStatus == OrderStatus.CANCELLED;        // Отменили закупку

            case PAID ->
                    newStatus == OrderStatus.PROCESSING ||     // Начали обработку после оплаты
                            newStatus == OrderStatus.SHIPPED ||        // Поставщик отгрузил
                            newStatus == OrderStatus.CANCELLED ||      // Отмена с возвратом денег
                            newStatus == OrderStatus.REFUNDED;         // Возврат денег

            case PROCESSING ->
                    newStatus == OrderStatus.SHIPPED ||        // Поставщик отгрузил
                            newStatus == OrderStatus.DELIVERED ||      // Товар принят на склад
                            newStatus == OrderStatus.CANCELLED;        // Отмена в процессе

            case SHIPPED ->
                    newStatus == OrderStatus.DELIVERED ||           // Товар доставлен
                            newStatus == OrderStatus.PARTIALLY_RECEIVED;    // Частично получен

            case PARTIALLY_RECEIVED ->
                    newStatus == OrderStatus.DELIVERED ||      // Полностью получен
                            newStatus == OrderStatus.COMPLETED;        // Завершен (если частично достаточно)

            case DELIVERED ->
                    newStatus == OrderStatus.COMPLETED ||      // Закупка завершена
                            newStatus == OrderStatus.RETURNED;         // Возврат поставщику

            case COMPLETED ->
                    newStatus == OrderStatus.RETURNED;         // Возврат поставщику

            default -> false;
        };

        if (!isValid) {
            log.warn(messageService.get(
                    "purchase.error.invalid.status.transition",
                    currentStatus, newStatus
            ));
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }
    }

    // ========== ВАЛИДАЦИЯ ЦЕН И КОЛИЧЕСТВА ==========

    public void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            log.warn(messageService.get("manager.order.log.invalid.price", price));
            throw new InvalidPriceException(price);
        }
    }

    public void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 0) {
            log.warn(messageService.get("manager.order.log.invalid.quantity", quantity));
            throw new InvalidQuantityException(quantity);
        }
    }

    public void validatePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            log.warn(messageService.get("manager.order.log.invalid.quantity", quantity));
            throw new InvalidQuantityException(quantity);
        }
    }

    // ========== ВАЛИДАЦИЯ МОДИФИКАЦИИ ЗАКАЗА ==========

    public void validateOrderModifiable(Order order, OrderStatus... allowedStatuses) {
        for (OrderStatus allowed : allowedStatuses) {
            if (order.getStatus() == allowed) {
                return;
            }
        }
        log.warn(messageService.get("manager.order.log.cannot.modify", order.getStatus()));
        throw new OrderModificationNotAllowedException(order.getStatus());
    }

    public void validateItemNotExists(Order order, Long productId) {
        boolean itemExists = order.getItems().stream()
                .anyMatch(i -> i.getProductId().equals(productId));
        if (itemExists) {
            log.warn(messageService.get("manager.order.log.item.exists", productId));
            throw new OrderItemAlreadyExistsException(productId);
        }
    }

    // ========== ВАЛИДАЦИЯ НАЛИЧИЯ ТОВАРА ==========

    public void validateProductAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(messageService.get(
                    "manager.order.log.insufficient.stock",
                    product.getName(),
                    product.getQuantityInStock(),
                    requestedQuantity
            ));
            throw new InsufficientStockException(product.getName(), product.getQuantityInStock());
        }
    }
}