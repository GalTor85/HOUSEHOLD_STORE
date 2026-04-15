package ru.galtor85.household_store.processor.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cell.CellVolumeLimitExceededException;
import ru.galtor85.household_store.advice.exception.cell.CellWeightLimitExceededException;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.validator.cell.CellValidationHelper;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CellAssignmentProcessor {

    private final StorageCellRepository storageCellRepository;
    private final CellValidationHelper cellValidationHelper;
    private final LogMessageService logMsg;

    /**
     * Assigns a product to a storage cell or increases quantity if the same product.
     * Validates weight and volume limits before placement.
     *
     * @param cell               target storage cell
     * @param product            product to place
     * @param additionalQuantity quantity to add
     * @return updated storage cell
     * @throws CellWeightLimitExceededException if weight limit would be exceeded
     * @throws CellVolumeLimitExceededException if volume limit would be exceeded
     */
    public StorageCell assignProductToCell(StorageCell cell, Product product,
                                            int additionalQuantity) {

        // Validate cell is active
        cellValidationHelper.validateCellActive(cell);

        // Validate cell type compatibility
        cellValidationHelper.validateCellTypeCompatibility(cell, product);

        int currentQuantity = cell.getCurrentQuantity() != null ? cell.getCurrentQuantity() : 0;
        int newQuantity = currentQuantity + additionalQuantity;

        double currentWeight;
        double newWeight = 0;
        double currentVolume;
        double newVolume = 0;

        // Check weight limit
        if (cell.getMaxWeightKg() != null && product.getWeightKg() != null) {
            currentWeight = currentQuantity * product.getWeightKg();
            newWeight = currentWeight + (additionalQuantity * product.getWeightKg());
            if (newWeight > cell.getMaxWeightKg()) {
                log.warn(logMsg.get("cell.validation.weight.limit.exceeded.log",
                        cell.getId(), cell.getMaxWeightKg(), newWeight));
                throw new CellWeightLimitExceededException(
                        cell.getId(), cell.getMaxWeightKg(), newWeight);
            }
        }

        // Check volume limit
        if (cell.getMaxVolumeM3() != null && product.getVolumeM3() != null) {
            currentVolume = currentQuantity * product.getVolumeM3();
            newVolume = currentVolume + (additionalQuantity * product.getVolumeM3());
            if (newVolume > cell.getMaxVolumeM3()) {
                log.warn(logMsg.get("cell.validation.volume.limit.exceeded.log",
                        cell.getId(), cell.getMaxVolumeM3(), newVolume));
                throw new CellVolumeLimitExceededException(
                        cell.getId(), cell.getMaxVolumeM3(), newVolume);
            }
        }

        // Update cell
        cell.setCurrentProductId(product.getId());
        cell.setCurrentQuantity(newQuantity);
        cell.setIsOccupied(true);
        cell.setLastInventoryDate(LocalDateTime.now());

        log.info(logMsg.get("cell.assignment.updated.log",
                cell.getCode(), currentQuantity, newQuantity,
                newWeight, cell.getMaxWeightKg(), newVolume, cell.getMaxVolumeM3()));

        return storageCellRepository.save(cell);
    }
}