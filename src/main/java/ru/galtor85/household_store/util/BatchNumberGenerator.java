package ru.galtor85.household_store.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class BatchNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Генерация номера партии на основе даты и случайного числа
     */
    public String generateBatchNumber() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BATCH-" + datePart + "-" + randomPart;
    }

    /**
     * Генерация номера партии с префиксом
     */
    public String generateBatchNumber(String prefix) {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "-" + datePart + "-" + randomPart;
    }

    /**
     * Генерация номера партии на основе даты поставки
     */
    public String generateBatchNumber(LocalDateTime date) {
        String datePart = date.format(DATE_FORMAT);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BATCH-" + datePart + "-" + randomPart;
    }
}