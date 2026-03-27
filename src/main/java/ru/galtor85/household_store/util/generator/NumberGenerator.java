package ru.galtor85.household_store.util.generator;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NumberGenerator {

    public String generatePurchaseOrderNumber() {
        return "PO-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String generateWriteOffNumber() {
        return "WO-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String generateSalesOrderNumber() {
        return "SO-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}