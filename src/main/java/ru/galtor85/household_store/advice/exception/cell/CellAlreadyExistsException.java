package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;

@Getter
public class CellAlreadyExistsException extends RuntimeException {
    private final String cellCode;
    private final Long warehouseId;

  public CellAlreadyExistsException(String cellCode, Long warehouseId) {
    super();
    this.cellCode = cellCode;
    this.warehouseId = warehouseId;
  }

}