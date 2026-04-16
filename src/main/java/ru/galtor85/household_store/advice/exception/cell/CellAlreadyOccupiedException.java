package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;

@Getter
public class CellAlreadyOccupiedException extends RuntimeException {
    private final Long cellId;
    private final String cellCode;
    private final Long currentProductId;

    public CellAlreadyOccupiedException(Long cellId, Long currentProductId) {
        super();
        this.cellId = cellId;
        this.currentProductId = currentProductId;
        this.cellCode = null;
    }
}