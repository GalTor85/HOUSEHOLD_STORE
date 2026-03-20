package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.RollbackInvalidTransitionException;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RollbackTargetStatusProcessor {

    private final MessageService messageService;

    public OrderStatus determineTargetStatus(Order order) {
        OrderStatus currentStatus = order.getStatus();

        log.debug(messageService.get("rollback.target.determining", currentStatus));

        return switch (currentStatus) {
            case PAID -> OrderStatus.PENDING;
            case PROCESSING -> OrderStatus.PAID;
            case SHIPPED -> OrderStatus.PROCESSING;
            case DELIVERED -> OrderStatus.SHIPPED;
            default -> {
                log.error(messageService.get("rollback.target.cannot.determine", currentStatus));
                throw new RollbackInvalidTransitionException(currentStatus, null);
            }
        };
    }
}