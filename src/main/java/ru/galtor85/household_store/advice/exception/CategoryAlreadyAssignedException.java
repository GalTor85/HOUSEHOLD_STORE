package ru.galtor85.household_store.advice.exception;

import lombok.Getter;

@Getter
public class CategoryAlreadyAssignedException extends RuntimeException {
    private final String category;

    public CategoryAlreadyAssignedException(String category) {
        super("error.category.already.assigned");
        this.category = category;
    }
}