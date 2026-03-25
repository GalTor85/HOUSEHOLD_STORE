package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.SalesOrder;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderValidationHelper {

    private final MessageService messageService;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    // =========================================================================
    // ПАРСИНГ ДАТ
    // =========================================================================

    /**
     * Парсит строку в LocalDateTime
     */
    public LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String trimmed = dateStr.trim();

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException e) {
                // пробуем следующий формат
            }
        }

        log.debug(messageService.get("sales.validation.date.parse.failed", dateStr));
        return null;
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

    // =========================================================================
    // ПАРСИНГ И ВАЛИДАЦИЯ СТАТУСОВ
    // =========================================================================

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
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn(messageService.get("sales.validation.status.invalid", status));
            throw new InvalidOrderStatusException(status);
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ПЕРЕХОДОВ СТАТУСОВ
    // =========================================================================

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
     * Проверяет, можно ли откатить статус для заказа на продажу
     */
    public void validateRollbackAllowedForSale(OrderStatus currentStatus) {
        if (!currentStatus.isRollbackAllowedForSale()) {
            log.warn(messageService.get("sales.validation.rollback.not.allowed", currentStatus));
            throw new IllegalStateException(
                    messageService.get("sales.validation.rollback.not.allowed", currentStatus)
            );
        }
    }

    /**
     * Проверяет, что статус не является финальным
     */
    public void validateNotFinalStatusForSale(OrderStatus status) {
        if (status.isFinalForSale()) {
            log.warn(messageService.get("sales.validation.status.final", status));
            throw new IllegalStateException(
                    messageService.get("sales.validation.status.final", status)
            );
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ МОДИФИКАЦИИ ЗАКАЗА
    // =========================================================================

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

    // =========================================================================
    // ВАЛИДАЦИЯ ЦЕН И КОЛИЧЕСТВ
    // =========================================================================

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

    // =========================================================================
    // ВАЛИДАЦИЯ НАЛИЧИЯ ТОВАРА
    // =========================================================================

    /**
     * Проверяет наличие товара на складе
     */
    public void validateProductAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(messageService.get("sales.validation.product.insufficient.stock",
                    product.getName(), product.getQuantityInStock(), requestedQuantity));
            throw new InsufficientStockException(product.getName(), product.getQuantityInStock());
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ АДРЕСА
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
     * Проверяет адрес для выставления счета
     */
    public void validateBillingAddress(String billingAddress) {
        if (billingAddress == null || billingAddress.trim().isEmpty()) {
            log.warn(messageService.get("sales.validation.billing.address.empty"));
            throw new IllegalArgumentException(
                    messageService.get("sales.validation.billing.address.empty")
            );
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ СПОСОБА ОПЛАТЫ
    // =========================================================================

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
}