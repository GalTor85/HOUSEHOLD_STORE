package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;
import ru.galtor85.household_store.entity.warehouse.CellType;

@Getter
public class NoSuitableCellException extends RuntimeException {
    private final Long warehouseId;
    private final CellType requiredType;
    private final Long productId;

    public NoSuitableCellException(Long warehouseId, CellType requiredType, Long productId) {
        super();
        this.warehouseId = warehouseId;
        this.requiredType = requiredType;
        this.productId = productId;
    }

}