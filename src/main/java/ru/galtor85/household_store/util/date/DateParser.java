package ru.galtor85.household_store.util.date;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Utility for parsing date strings into LocalDateTime objects.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DateParser {

    private final LogMessageService logMsg;

    public LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.warn(logMsg.get("manager.purchase.log.date.parse.failed", dateStr));
            return null;
        }
    }

    public OrderStatus parseOrderStatus(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.warn(logMsg.get("manager.order.log.invalid.status", status));
            return null;
        }
    }
}