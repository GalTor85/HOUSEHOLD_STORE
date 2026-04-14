package ru.galtor85.household_store.validator.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.order.OrderCancellationNotAllowedException;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

/**
 * Validator for order cancellation operations.
 * Ensures that only cancellable orders can be cancelled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSalesCancellationValidator {

    private final MessageService messageService;

    /**
     * Validates that an order can be cancelled.
     *
     * @param order the order to validate
     * @throws OrderCancellationNotAllowedException if order cannot be cancelled
     */
    public void validateCancellable(SalesOrderDto order) {
        validateStatus(order);
        validateNoPayments(order);
    }

    /**
     * Validates that order status allows cancellation.
     * Only PENDING orders can be cancelled.
     */
    private void validateStatus(SalesOrderDto order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn(messageService.get("order.cancel.validator.invalid.status",
                    order.getId(), order.getStatus()));
            throw new OrderCancellationNotAllowedException(
                    messageService.get("order.cancel.error.invalid.status",
                            order.getId(),
                            messageService.get("order.status." + order.getStatus().name())));
        }
    }

    /**
     * Validates that order has no payments.
     */
    private void validateNoPayments(SalesOrderDto order) {
        if (hasPayments(order)) {
            log.warn(messageService.get("order.cancel.validator.has.payments",
                    order.getId(), order.getPaymentSummary().getTotalPaid()));
            throw new OrderCancellationNotAllowedException(
                    messageService.get("order.cancel.error.has.payments", order.getId()));
        }
    }

    /**
     * Checks if order has any payments.
     */
    public boolean hasPayments(SalesOrderDto order) {
        if (order.getPaymentSummary() == null) {
            return false;
        }
        BigDecimal totalPaid = order.getPaymentSummary().getTotalPaid();
        return totalPaid != null && totalPaid.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if order is cancellable without throwing exception.
     */
    public boolean isCancellable(SalesOrderDto order) {
        return order.getStatus() == OrderStatus.PENDING && !hasPayments(order);
    }
}