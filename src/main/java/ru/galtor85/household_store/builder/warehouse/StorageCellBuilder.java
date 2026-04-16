package ru.galtor85.household_store.builder.warehouse;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.warehouse.StorageCellCreateRequest;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.entity.warehouse.Warehouse;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_BARCODE_FORMAT;

/**
 * Builder for storage cell entities.
 */
@Component
public class StorageCellBuilder {

    /**
     * Builds storage cell from creation request.
     *
     * @param request creation request
     * @param warehouse parent warehouse
     * @param barcode generated barcode
     * @return storage cell entity
     */
    public StorageCell buildFromRequest(StorageCellCreateRequest request,
                                        Warehouse warehouse,
                                        String barcode) {
        return StorageCell.builder()
                .warehouse(warehouse)
                .code(request.getCode())
                .barcode(barcode)
                .barcodeFormat(DEFAULT_BARCODE_FORMAT)
                .section(request.getSection())
                .rack(request.getRack())
                .shelf(request.getShelf())
                .position(request.getPosition())
                .cellType(request.getCellType())
                .maxWeightKg(request.getMaxWeightKg())
                .maxVolumeM3(request.getMaxVolumeM3())
                .isOccupied(false)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .notes(request.getNotes())
                .build();
    }

    /**
     * Clears cell by removing product and resetting quantity.
     *
     * @param cell storage cell to clear
     */
    public void clearCell(StorageCell cell) {
        cell.setCurrentProductId(null);
        cell.setCurrentQuantity(0);
        cell.setIsOccupied(false);
    }
}