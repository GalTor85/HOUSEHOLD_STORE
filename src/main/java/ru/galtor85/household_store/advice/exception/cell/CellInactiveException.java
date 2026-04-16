package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;

@Getter
public class CellInactiveException extends RuntimeException {
    private final Long cellId;

    public CellInactiveException(Long cellId) {
        super();
        this.cellId = cellId;
    }

}