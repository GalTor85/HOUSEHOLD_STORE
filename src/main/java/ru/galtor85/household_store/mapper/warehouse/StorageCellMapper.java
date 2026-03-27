package ru.galtor85.household_store.mapper.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.warehouse.StorageCellDto;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.repository.product.ProductRepository;

@Component
@RequiredArgsConstructor
public class StorageCellMapper {

    private final ProductRepository productRepository;

    public StorageCellDto toDto(StorageCell cell) {
        if (cell == null) {
            return null;
        }

        String productName = null;
        if (cell.getCurrentProductId() != null) {
            productName = productRepository.findById(cell.getCurrentProductId())
                    .map(p -> p.getName())
                    .orElse(null);
        }

        String fullLocationPath = buildFullLocationPath(cell);

        return StorageCellDto.builder()
                .id(cell.getId())
                .warehouseId(cell.getWarehouse().getId())
                .warehouseName(cell.getWarehouse().getName())
                .code(cell.getCode())
                .barcode(cell.getBarcode())
                .barcodeFormat(cell.getBarcodeFormat())
                .section(cell.getSection())
                .rack(cell.getRack())
                .shelf(cell.getShelf())
                .position(cell.getPosition())
                .cellType(cell.getCellType())
                .maxWeightKg(cell.getMaxWeightKg())
                .maxVolumeM3(cell.getMaxVolumeM3())
                .currentProductId(cell.getCurrentProductId())
                .currentProductName(productName)
                .currentQuantity(cell.getCurrentQuantity())
                .isOccupied(cell.getIsOccupied())
                .isActive(cell.getIsActive())
                .fullLocationPath(fullLocationPath)
                .lastInventoryDate(cell.getLastInventoryDate())
                .notes(cell.getNotes())
                .build();
    }

    private String buildFullLocationPath(StorageCell cell) {
        StringBuilder path = new StringBuilder();
        path.append(cell.getWarehouse().getName());

        if (cell.getSection() != null) {
            path.append(" > Section ").append(cell.getSection());
        }
        if (cell.getRack() != null) {
            path.append(" > Rack ").append(cell.getRack());
        }
        if (cell.getShelf() != null) {
            path.append(" > Shelf ").append(cell.getShelf());
        }
        if (cell.getPosition() != null) {
            path.append(" > Position ").append(cell.getPosition());
        }

        return path.toString();
    }
}