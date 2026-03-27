package ru.galtor85.household_store.validator.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cell.*;
import ru.galtor85.household_store.entity.warehouse.CellType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CellValidationHelper {

    private final MessageService messageService;

    /**
     * Проверка активности ячейки
     */
    public void validateCellActive(StorageCell cell) {

        if (!cell.getIsActive()) {
            log.warn(messageService.get("cell.validation.inactive.log", cell.getId()));
            throw new CellInactiveException(cell.getId());
        }
    }

    /**
     * Проверка, что ячейка не занята
     */
    public void validateCellNotOccupied(StorageCell cell) {


        if (cell.getIsOccupied()) {
            log.warn(messageService.get("cell.validation.occupied.log",
                    cell.getId(), cell.getCurrentProductId()));
            throw new CellAlreadyOccupiedException(cell.getId(), cell.getCurrentProductId());
        }
    }

    /**
     * Определение требуемого типа ячейки на основе характеристик продукта
     */
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

    /**
     * Проверка совместимости типа ячейки с продуктом
     */
    public void validateCellTypeCompatibility(StorageCell cell, Product product) {

        CellType requiredType = determineRequiredCellType(product);

        if (cell.getCellType() != requiredType) {
            String cellTypeLocalized = messageService.get("cell.type." + cell.getCellType().name());
            String requiredTypeLocalized = messageService.get("cell.type." + requiredType.name());

            log.warn(messageService.get("cell.validation.incompatible.type.log",
                    cell.getId(), cellTypeLocalized, requiredTypeLocalized));

            throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(), requiredType.name());
        }

        // Дополнительные проверки для специальных типов
        switch (cell.getCellType()) {
            case FRIDGE:
                if (!product.getRequiresRefrigeration()) {
                    log.warn(messageService.get("cell.validation.fridge.not.needed.log", cell.getId()));
                    throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(),
                            messageService.get("cell.validation.error.fridge.not.needed"));
                }
                break;

            case FREEZER:
                if (!product.getRequiresFreezing()) {
                    log.warn(messageService.get("cell.validation.freezer.not.needed.log", cell.getId()));
                    throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(),
                            messageService.get("cell.validation.error.freezer.not.needed"));
                }
                break;

            case DANGEROUS:
                if (!product.getIsHazardous()) {
                    log.warn(messageService.get("cell.validation.dangerous.not.needed.log", cell.getId()));
                    throw new IncompatibleCellTypeException(cell.getId(), cell.getCellType(),
                            messageService.get("cell.validation.error.dangerous.not.needed"));
                }
                break;

            default:
                // Для остальных типов дополнительных проверок не требуется
                break;
        }
    }

    /**
     * Проверка лимита веса
     */
    public void validateWeightLimit(StorageCell cell, Product product, int quantity) {

        if (cell.getMaxWeightKg() == null || product.getWeightKg() == null) {
            return;
        }

        double totalWeight = product.getWeightKg() * quantity;
        if (totalWeight > cell.getMaxWeightKg()) {
            log.warn(messageService.get("cell.validation.weight.limit.exceeded.log",
                    cell.getId(), cell.getMaxWeightKg(), totalWeight));

            throw new CellWeightLimitExceededException(cell.getId(), cell.getMaxWeightKg(), totalWeight);
        }
    }

    /**
     * Проверка лимита объема
     */
    public void validateVolumeLimit(StorageCell cell, Product product, int quantity) {

        if (cell.getMaxVolumeM3() == null || product.getVolumeM3() == null) {
            return;
        }

        double totalVolume = product.getVolumeM3() * quantity;
        if (totalVolume > cell.getMaxVolumeM3()) {
            log.warn(messageService.get("cell.validation.volume.limit.exceeded.log",
                    cell.getId(), cell.getMaxVolumeM3(), totalVolume));

            throw new CellVolumeLimitExceededException(cell.getId(), cell.getMaxVolumeM3(), totalVolume);
        }
    }

    /**
     * Проверка, что товар уже не находится в другой ячейке этого же склада
     */
    public void checkProductNotInOtherCells(List<StorageCell> cellsWithProduct,
                                            Long currentCellId, Long productId,
                                            Long warehouseId) {

        List<StorageCell> otherCells = cellsWithProduct.stream()
                .filter(cell -> !cell.getId().equals(currentCellId))
                .collect(Collectors.toList());

        if (!otherCells.isEmpty()) {
            StorageCell existingCell = otherCells.get(0);
            log.warn(messageService.get("cell.validation.product.already.in.warehouse.log",
                    productId, existingCell.getCode(), warehouseId));

            throw new ProductAlreadyInWarehouseException(productId, warehouseId, existingCell.getCode());
        }
    }
}