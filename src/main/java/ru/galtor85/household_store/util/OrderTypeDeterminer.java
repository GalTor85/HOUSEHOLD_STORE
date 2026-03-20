package ru.galtor85.household_store.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.OrderType;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTypeDeterminer {

    private final MessageService messageService;

    public OrderType determineOrderType(Long userId) {
        // TODO: Реализовать определение типа пользователя
        // Например, проверка роли или настроек пользователя
        log.debug(messageService.get("order.log.type.default", userId));
        return OrderType.RETAIL;
    }

    public OrderType determineOrderTypeByUserRole(String userRole) {
        return switch (userRole) {
            case "WHOLESALE" -> OrderType.WHOLESALE;
            case "RETAIL" -> OrderType.RETAIL;
            default -> {
                log.debug(messageService.get("order.log.type.default.role", userRole));
                yield OrderType.RETAIL;
            }
        };
    }
}