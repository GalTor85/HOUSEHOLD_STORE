package ru.galtor85.household_store.validator.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cell.*;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.CellType;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;

/**
 * Helper for storage cell validation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CellValidationHelper {

    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Validates cell is active.
     *
     * @param cell storage cell
     * @throws CellInactiveException if inactive
     */
    public void validateCellActive(StorageCell cell) {
        if (!cell.getIsActive()) {
            log.warn(logMsg.get("cell.validation.inactive.log", cell.getId()));
            throw new CellInactiveException(cell.getId());
        }
    }

    /**
     * Validates cell can accept the product.
     *
     * @param cell storage cell
     * @param product product to place
     * @throws CellAlreadyOccupiedException if cell contains different product
     */
    public void validateCellNotOccupied(StorageCell cell, Product product) {
        if (!cell.getIsOccupied()) {
            return;
        }
        if (!cell.getCurrentProductId().equals(product.getId())) {
            log.warn(logMsg.get("cell.validation.occupied.log",
                    cell.getId(), cell.getCurrentProductId()));
            throw new CellAlreadyOccupiedException(cell.getId(), cell.getCurrentProductId());
        }
        log.debug(logMsg.get("cell.validation.same.product.log", cell.getId(), product.getId()));
    }

    /**
     * Determines required cell type based on product characteristics.
     *
     * @param product product entity
     * @return required cell type
     */
    public CellType determineRequiredCellType(Product product) {
        if (Boolean.TRUE.equals(product.getIsHazardous())) {
            return CellType.DANGEROUS;
        }
        if (Boolean.TRUE.equals(product.getRequiresFreezing())) {
            return CellType.FREEZER;
        }
        if (Boolean.TRUE.equals(product.getRequiresRefrigeration())) {
            return CellType.FRIDGE;
        }
        if (Boolean.TRUE.equals(product.getIsOversize())) {
            return CellType.OVERSIZE;
        }
        if (Boolean.TRUE.equals(product.getIsLiquid())) {
            return CellType.LIQUID;
        }
        if (Boolean.TRUE.equals(product.getIsPalletized())) {
            return CellType.PALLET;
        }
        return CellType.STANDARD;
    }

    /**
     * Validates cell type is compatible with product.
     *
     * @param cell storage cell
     * @param product product to place
     * @throws IncompatibleCellTypeException if incompatible
     */
    public void validateCellTypeCompatibility(StorageCell cell, Product product) {
        CellType requiredType = determineRequiredCellType(product);

        if (cell.getCellType() != requiredType) {
            String cellTypeLocalized = messageService.get("cell.type." + cell.getCellType().name());
            String requiredTypeLocalized = messageService.get("cell.type." + requiredType.name());

            log.warn(logMsg.get("cell.validation.incompatible.type.log",
                    cell.getId(), cellTypeLocalized, requiredTypeLocalized));

            throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(), requiredType.name());
        }

        switch (cell.getCellType()) {
            case FRIDGE:
                if (Boolean.FALSE.equals(product.getRequiresRefrigeration())) {
                    log.warn(logMsg.get("cell.validation.fridge.not.needed.log", cell.getId()));
                    throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(),
                            messageService.get("cell.validation.error.fridge.not.needed"));
                }
                break;
            case FREEZER:
                if (Boolean.FALSE.equals(product.getRequiresFreezing())) {
                    log.warn(logMsg.get("cell.validation.freezer.not.needed.log", cell.getId()));
                    throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(),
                            messageService.get("cell.validation.error.freezer.not.needed"));
                }
                break;
            case DANGEROUS:
                if (Boolean.FALSE.equals(product.getIsHazardous())) {
                    log.warn(logMsg.get("cell.validation.dangerous.not.needed.log", cell.getId()));
                    throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(),
                            messageService.get("cell.validation.error.dangerous.not.needed"));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Validates weight limit is not exceeded.
     *
     * @param cell storage cell
     * @param product product to place
     * @param quantity quantity to add
     * @throws CellWeightLimitExceededException if limit exceeded
     */
    public void validateWeightLimit(StorageCell cell, Product product, int quantity) {
        if (cell.getMaxWeightKg() == null || product.getWeightKg() == null) {
            return;
        }
        double totalWeight = product.getWeightKg() * quantity;
        if (totalWeight > cell.getMaxWeightKg()) {
            log.warn(logMsg.get("cell.validation.weight.limit.exceeded.log",
                    cell.getId(), cell.getMaxWeightKg(), totalWeight));
            throw new CellWeightLimitExceededException(cell.getId(), cell.getMaxWeightKg(), totalWeight);
        }
    }

    /**
     * Validates volume limit is not exceeded.
     *
     * @param cell storage cell
     * @param product product to place
     * @param quantity quantity to add
     * @throws CellVolumeLimitExceededException if limit exceeded
     */
    public void validateVolumeLimit(StorageCell cell, Product product, int quantity) {
        if (cell.getMaxVolumeM3() == null || product.getVolumeM3() == null) {
            return;
        }
        double totalVolume = product.getVolumeM3() * quantity;
        if (totalVolume > cell.getMaxVolumeM3()) {
            log.warn(logMsg.get("cell.validation.volume.limit.exceeded.log",
                    cell.getId(), cell.getMaxVolumeM3(), totalVolume));
            throw new CellVolumeLimitExceededException(cell.getId(), cell.getMaxVolumeM3(), totalVolume);
        }
    }

    /**
     * Validates product is not already in another cell of the same warehouse.
     *
     * @param cellsWithProduct cells containing the product
     * @param currentCellId current cell ID
     * @param productId product ID
     * @param warehouseId warehouse ID
     * @throws ProductAlreadyInWarehouseException if product found in another cell
     */
    public void checkProductNotInOtherCells(List<StorageCell> cellsWithProduct,
                                            Long currentCellId, Long productId,
                                            Long warehouseId) {
        List<StorageCell> otherCells = cellsWithProduct.stream()
                .filter(cell -> !cell.getId().equals(currentCellId))
                .toList();

        if (!otherCells.isEmpty()) {
            StorageCell existingCell = otherCells.getFirst();
            log.warn(logMsg.get("cell.validation.product.already.in.warehouse.log",
                    productId, existingCell.getCode(), warehouseId));
            throw new ProductAlreadyInWarehouseException(productId, warehouseId, existingCell.getCode());
        }
    }
}