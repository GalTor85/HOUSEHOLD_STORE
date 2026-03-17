package ru.galtor85.household_store.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.entity.CellType;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.StorageCell;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CellValidationHelper {

    private final MessageService messageService;

    public void validateCellActive(StorageCell cell) {
        if (!cell.getIsActive()) {
            log.warn(messageService.get("cell.log.inactive", cell.getId()));
            throw new CellInactiveException(cell.getId());
        }
    }

    public void validateCellNotOccupied(StorageCell cell) {
        if (cell.getIsOccupied()) {
            log.warn(messageService.get("cell.log.already.occupied",
                    cell.getId(), cell.getCurrentProductId()));
            throw new CellAlreadyOccupiedException(cell.getId(), cell.getCurrentProductId());
        }
    }

    public CellType determineRequiredCellType(Product product) {
        if (product.getIsHazardous()) {
            return CellType.DANGEROUS;
        }
        if (product.getRequiresFreezing()) {
            return CellType.FREEZER;
        }
        if (product.getRequiresRefrigeration()) {
            return CellType.FRIDGE;
        }
        if (product.getIsOversize()) {
            return CellType.OVERSIZE;
        }
        if (product.getIsLiquid()) {
            return CellType.LIQUID;
        }
        if (product.getIsPalletized()) {
            return CellType.PALLET;
        }
        return CellType.STANDARD;
    }

    public void validateCellTypeCompatibility(StorageCell cell, Product product) {
        CellType requiredType = determineRequiredCellType(product);

        if (cell.getCellType() != requiredType) {
            log.warn(messageService.get("cell.log.incompatible.type",
                    cell.getId(), cell.getCellType(), requiredType));
            throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(), requiredType.name());
        }

        // Дополнительные проверки для специальных типов
        switch (cell.getCellType()) {
            case FRIDGE:
                if (!product.getRequiresRefrigeration()) {
                    log.warn(messageService.get("cell.log.fridge.not.needed", cell.getId()));
                    throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(),
                            "Product does not require refrigeration");
                }
                break;
            case FREEZER:
                if (!product.getRequiresFreezing()) {
                    log.warn(messageService.get("cell.log.freezer.not.needed", cell.getId()));
                    throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(),
                            "Product does not require freezing");
                }
                break;
            case DANGEROUS:
                if (!product.getIsHazardous()) {
                    log.warn(messageService.get("cell.log.dangerous.not.needed", cell.getId()));
                    throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(),
                            "Product is not hazardous");
                }
                break;
        }
    }

    public void validateWeightLimit(StorageCell cell, Product product, int quantity) {
        if (cell.getMaxWeightKg() == null || product.getWeightKg() == null) {
            return;
        }

        double totalWeight = product.getWeightKg() * quantity;
        if (totalWeight > cell.getMaxWeightKg()) {
            log.warn(messageService.get("cell.log.weight.limit.exceeded",
                    cell.getId(), cell.getMaxWeightKg(), totalWeight));
            throw new CellWeightLimitExceededException(cell.getId(), cell.getMaxWeightKg(), totalWeight);
        }
    }

    public void validateVolumeLimit(StorageCell cell, Product product, int quantity) {
        if (cell.getMaxVolumeM3() == null || product.getVolumeM3() == null) {
            return;
        }

        double totalVolume = product.getVolumeM3() * quantity;
        if (totalVolume > cell.getMaxVolumeM3()) {
            log.warn(messageService.get("cell.log.volume.limit.exceeded",
                    cell.getId(), cell.getMaxVolumeM3(), totalVolume));
            throw new CellVolumeLimitExceededException(cell.getId(), cell.getMaxVolumeM3(), totalVolume);
        }
    }

    public void checkProductNotInOtherCells(List<StorageCell> cellsWithProduct,
                                            Long currentCellId, Long productId,
                                            Long warehouseId) {
        List<StorageCell> otherCells = cellsWithProduct.stream()
                .filter(cell -> !cell.getId().equals(currentCellId))
                .collect(Collectors.toList());

        if (!otherCells.isEmpty()) {
            StorageCell existingCell = otherCells.get(0);
            log.warn(messageService.get("cell.log.product.already.in.warehouse",
                    productId, existingCell.getCode()));
            throw new ProductAlreadyInWarehouseException(productId, warehouseId, existingCell.getCode());
        }
    }
}