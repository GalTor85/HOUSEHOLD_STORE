package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;

@Getter
public class CellAlreadyEmptyException extends RuntimeException {
    private final Long cellId;
    private final String cellCode;

    public CellAlreadyEmptyException(Long cellId) {
        super();
        this.cellId = cellId;
        this.cellCode = null;
    }

    public CellAlreadyEmptyException(String cellCode) {
        super();
        this.cellCode = cellCode;
        this.cellId = null;
    }

}