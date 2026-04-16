package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;
import ru.galtor85.household_store.entity.warehouse.CellType;

@Getter
public class NoAvailableCellException extends RuntimeException {
    private final Long warehouseId;
    private final CellType requiredType;

    public NoAvailableCellException(Long warehouseId, CellType requiredType) {
        super();
        this.warehouseId = warehouseId;
        this.requiredType = requiredType;
    }

}