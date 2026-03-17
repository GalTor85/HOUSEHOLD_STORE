package ru.galtor85.household_store.builder;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.StorageCellCreateRequest;
import ru.galtor85.household_store.entity.StorageCell;
import ru.galtor85.household_store.entity.Warehouse;

@Component
public class StorageCellBuilder {

    public StorageCell buildFromRequest(StorageCellCreateRequest request,
                                        Warehouse warehouse,
                                        String barcode) {
        return StorageCell.builder()
                .warehouse(warehouse)
                .code(request.getCode())
                .barcode(barcode)
                .barcodeFormat("CODE_128")
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

    public void assignProductToCell(StorageCell cell, Long productId, int quantity) {
        cell.setCurrentProductId(productId);
        cell.setCurrentQuantity(quantity);
        cell.setIsOccupied(true);
        cell.setLastInventoryDate(java.time.LocalDateTime.now());
    }

    public void clearCell(StorageCell cell) {
        cell.setCurrentProductId(null);
        cell.setCurrentQuantity(0);
        cell.setIsOccupied(false);
    }
}