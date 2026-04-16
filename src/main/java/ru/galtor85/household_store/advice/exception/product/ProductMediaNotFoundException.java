package ru.galtor85.household_store.advice.exception.product;

import lombok.Getter;

@Getter
public class ProductMediaNotFoundException extends RuntimeException {

    private final Long mediaId;

    public ProductMediaNotFoundException(String message, Long mediaId) {
        super(message);
        this.mediaId = mediaId;
    }

}