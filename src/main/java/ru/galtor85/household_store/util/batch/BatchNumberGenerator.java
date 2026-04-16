package ru.galtor85.household_store.util.batch;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generator for batch/lot numbers.
 */
@Component
public class BatchNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String BATCH_PREFIX = "BATCH-";
    private static final String SEPARATOR = "-";
    private static final int UUID_LENGTH = 8;

    /**
     * Generates batch number based on current date and random UUID.
     *
     * @return generated batch number
     */
    public String generateBatchNumber() {
        return generateBatchNumber(BATCH_PREFIX);
    }

    /**
     * Generates batch number with custom prefix.
     *
     * @param prefix custom prefix
     * @return generated batch number
     */
    public String generateBatchNumber(String prefix) {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String randomPart = UUID.randomUUID().toString().substring(0, UUID_LENGTH).toUpperCase();
        return prefix + SEPARATOR + datePart + SEPARATOR + randomPart;
    }
}