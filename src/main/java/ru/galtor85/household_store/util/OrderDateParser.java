package ru.galtor85.household_store.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.InvalidDateRangeException;
import ru.galtor85.household_store.service.MessageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDateParser {

    private final MessageService messageService;

    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    public LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String trimmed = dateStr.trim();

        // Пробуем разные форматы дат
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException e) {
                // Пробуем следующий формат
            }
        }

        // Если не получилось распарсить, пробуем как дату без времени
        try {
            LocalDate date = LocalDate.parse(trimmed);
            return date.atStartOfDay();
        } catch (DateTimeParseException e) {
            log.debug(messageService.get("manager.order.log.date.parse.failed", dateStr));
            return null;
        }
    }

    public void validateDateRange(LocalDateTime start, LocalDateTime end,
                                  String startDate, String endDate) {
        if (start != null && end != null && start.isAfter(end)) {
            log.warn(messageService.get("manager.order.log.date.range", startDate, endDate));
            throw new InvalidDateRangeException(start, end);
        }
    }
}