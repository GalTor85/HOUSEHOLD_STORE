package ru.galtor85.household_store.processor.rollback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.rollback.RollbackInvalidTransitionException;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.service.i18n.LogMessageService;

/**
 * Processor for determining target status for rollback operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RollbackTargetStatusProcessor {

    private final LogMessageService logMsg;

    /**
     * Determines the target status for a rollback based on current status.
     *
     * @param salesOrder the sales order
     * @return target OrderStatus
     * @throws RollbackInvalidTransitionException if current status cannot be rolled back
     */
    public OrderStatus determineTargetStatus(SalesOrder salesOrder) {
        OrderStatus currentStatus = salesOrder.getStatus();

        log.debug(logMsg.get("rollback.target.determining", currentStatus));

        return switch (currentStatus) {
            case PAID -> OrderStatus.PENDING;
            case PROCESSING -> OrderStatus.PAID;
            case SHIPPED -> OrderStatus.PROCESSING;
            case DELIVERED -> OrderStatus.SHIPPED;
            default -> {
                log.error(logMsg.get("rollback.target.cannot.determine", currentStatus));
                throw new RollbackInvalidTransitionException(currentStatus, null);
            }
        };
    }
}