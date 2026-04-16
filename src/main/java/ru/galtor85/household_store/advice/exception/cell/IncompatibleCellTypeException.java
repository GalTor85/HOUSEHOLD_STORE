package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;
import ru.galtor85.household_store.entity.warehouse.CellType;

@Getter
public class IncompatibleCellTypeException extends RuntimeException {
    private final Long cellId;
    private final String cellCode;
    private final CellType cellType;
    private final String requiredType;

    public IncompatibleCellTypeException(Long cellId, CellType cellType, String requiredType) {
        super();
        this.cellId = cellId;
        this.cellCode = null;
        this.cellType = cellType;
        this.requiredType = requiredType;
    }

    public IncompatibleCellTypeException(String cellCode,
                                         CellType cellType, String requiredType) {
        super();
        this.cellCode = cellCode;
        this.cellType = cellType;
        this.requiredType = requiredType;
        this.cellId = null;
    }

}