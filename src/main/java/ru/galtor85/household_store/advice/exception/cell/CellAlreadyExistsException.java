package ru.galtor85.household_store.advice.exception.cell;

public class CellAlreadyExistsException extends RuntimeException {
  private final String cellCode;
  private final Long warehouseId;

  public CellAlreadyExistsException(String cellCode, Long warehouseId) {
    super();
    this.cellCode = cellCode;
    this.warehouseId = warehouseId;
  }

  public String getCellCode() {
    return cellCode;
  }

  public Long getWarehouseId() {
    return warehouseId;
  }
}