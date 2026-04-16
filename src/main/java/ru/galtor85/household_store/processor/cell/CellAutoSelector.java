package ru.galtor85.household_store.processor.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cell.NoAvailableCellException;
import ru.galtor85.household_store.advice.exception.cell.NoSuitableCellException;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.CellType;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.validator.cell.CellValidationHelper;

import java.util.Comparator;
import java.util.List;

/**
 * Processor for automatic selection of storage cells for products.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CellAutoSelector {

    private final StorageCellRepository storageCellRepository;
    private final CellValidationHelper validationHelper;
    private final LogMessageService logMsg;

    /**
     * Automatically selects a suitable storage cell for a product.
     *
     * @param warehouseId the warehouse ID
     * @param product     the product to store
     * @param quantity    the quantity to store
     * @return selected StorageCell
     * @throws NoAvailableCellException if no cells of required type exist
     * @throws NoSuitableCellException  if no cells can accommodate the product
     */
    @SuppressWarnings("unused")
    public StorageCell selectCellForProduct(Long warehouseId, Product product, int quantity) {
        log.debug(logMsg.get("cell.selector.start",
                product.getId(), quantity, warehouseId));

        // 1. Determine required cell type
        CellType requiredType = validationHelper.determineRequiredCellType(product);
        log.debug(logMsg.get("cell.selector.required.type", requiredType));

        // 2. Find available cells of required type
        List<StorageCell> availableCells = storageCellRepository
                .findAvailableCellsByType(warehouseId, requiredType);

        if (availableCells.isEmpty()) {
            log.error(logMsg.get("cell.error.no.available",
                    warehouseId, requiredType));
            throw new NoAvailableCellException(warehouseId, requiredType);
        }

        log.debug(logMsg.get("cell.selector.available.count", availableCells.size()));

        // 3. Filter by capacity
        List<StorageCell> suitableCells = availableCells.stream()
                .filter(cell -> isCellSuitable(cell, product, quantity))
                .toList();

        if (suitableCells.isEmpty()) {
            log.error(logMsg.get("cell.error.no.suitable",
                    warehouseId, requiredType, product.getId()));
            throw new NoSuitableCellException(warehouseId, requiredType, product.getId());
        }

        log.debug(logMsg.get("cell.selector.suitable.count", suitableCells.size()));

        // 4. Select optimal cell
        StorageCell selectedCell = selectOptimalCell(suitableCells, product);

        log.info(logMsg.get("cell.selector.selected",
                selectedCell.getCode(), selectedCell.getId()));

        return selectedCell;
    }

    /**
     * Checks if a cell can accommodate the product by weight and volume.
     *
     * @param cell     the storage cell
     * @param product  the product
     * @param quantity the quantity
     * @return true if cell is suitable
     */
    private boolean isCellSuitable(StorageCell cell, Product product, int quantity) {
        // Weight check
        if (cell.getMaxWeightKg() != null && product.getWeightKg() != null) {
            double totalWeight = product.getWeightKg() * quantity;
            if (totalWeight > cell.getMaxWeightKg()) {
                log.debug(logMsg.get("cell.selector.weight.exceeded",
                        cell.getCode(), totalWeight, cell.getMaxWeightKg()));
                return false;
            }
        }

        // Volume check
        if (cell.getMaxVolumeM3() != null && product.getVolumeM3() != null) {
            double totalVolume = product.getVolumeM3() * quantity;
            if (totalVolume > cell.getMaxVolumeM3()) {
                log.debug(logMsg.get("cell.selector.volume.exceeded",
                        cell.getCode(), totalVolume, cell.getMaxVolumeM3()));
                return false;
            }
        }

        return true;
    }

    /**
     * Selects the optimal cell from suitable candidates.
     * Priority:
     * 1. Empty cells
     * 2. Cells with the same product
     * 3. Cells with minimal fill level
     *
     * @param cells   list of suitable cells
     * @param product the product to store
     * @return the optimal StorageCell
     */
    private StorageCell selectOptimalCell(List<StorageCell> cells, Product product) {
        // First, try empty cells
        List<StorageCell> emptyCells = cells.stream()
                .filter(cell -> !cell.getIsOccupied())
                .toList();

        if (!emptyCells.isEmpty()) {
            log.debug(logMsg.get("cell.selector.using.empty", emptyCells.size()));
            return emptyCells.stream().min(Comparator.comparing(StorageCell::getCode))
                    .orElse(null);
        }

        // Second, try cells with the same product
        List<StorageCell> sameProductCells = cells.stream()
                .filter(cell -> cell.getIsOccupied() &&
                        product.getId().equals(cell.getCurrentProductId()))
                .toList();

        if (!sameProductCells.isEmpty()) {
            log.debug(logMsg.get("cell.selector.using.same.product", sameProductCells.size()));
            return sameProductCells.stream().min(Comparator.comparing(StorageCell::getCode))
                    .orElse(null);
        }

        // Finally, take cell with minimal current quantity
        log.debug(logMsg.get("cell.selector.using.partial"));
        return cells.stream()
                .min(Comparator.comparing(StorageCell::getCurrentQuantity))
                .orElse(null);
    }
}