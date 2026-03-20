package ru.galtor85.household_store.util;

import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {

    public String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
    }

    public String generateOrderNumberWithPrefix(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
    }
}