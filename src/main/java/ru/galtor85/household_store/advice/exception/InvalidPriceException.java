package ru.galtor85.household_store.advice.exception;

import java.math.BigDecimal;

public class InvalidPriceException extends RuntimeException {
    private final BigDecimal invalidPrice;

    public InvalidPriceException(BigDecimal invalidPrice) {
        super(); // Не передаем хардкодное сообщение
        this.invalidPrice = invalidPrice;
    }

    public BigDecimal getInvalidPrice() {
        return invalidPrice;
    }
}