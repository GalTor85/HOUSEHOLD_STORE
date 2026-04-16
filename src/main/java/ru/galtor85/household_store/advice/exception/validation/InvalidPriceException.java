package ru.galtor85.household_store.advice.exception.validation;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class InvalidPriceException extends RuntimeException {
    private final BigDecimal invalidPrice;

    public InvalidPriceException(BigDecimal invalidPrice) {
        super();
        this.invalidPrice = invalidPrice;
    }

}