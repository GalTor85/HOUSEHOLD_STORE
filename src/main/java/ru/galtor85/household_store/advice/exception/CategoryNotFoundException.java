package ru.galtor85.household_store.advice.exception;

import lombok.Getter;

@Getter
public class CategoryNotFoundException extends RuntimeException {
    private final String category;

    public CategoryNotFoundException(String category) {
        super("error.category.not.found");
        this.category = category;
    }
}