package ru.galtor85.household_store.mapper.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.warehouse.StorageCellDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.repository.product.ProductRepository;

/**
 * Mapper for storage cell entity to DTO.
 */
@Component
@RequiredArgsConstructor
public class StorageCellMapper {

    private static final String LOCATION_SEPARATOR = " > ";
    private static final String SECTION_PREFIX = "Section ";
    private static final String RACK_PREFIX = "Rack ";
    private static final String SHELF_PREFIX = "Shelf ";
    private static final String POSITION_PREFIX = "Position ";

    private final ProductRepository productRepository;

    /**
     * Converts storage cell entity to DTO.
     *
     * @param cell storage cell entity
     * @return storage cell DTO
     */
    public StorageCellDto toDto(StorageCell cell) {
        if (cell == null) {
            return null;
        }

        String productName = null;
        if (cell.getCurrentProductId() != null) {
            productName = productRepository.findById(cell.getCurrentProductId())
                    .map(Product::getName)
                    .orElse(null);
        }

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
                .fullLocationPath(buildFullLocationPath(cell))
                .lastInventoryDate(cell.getLastInventoryDate())
                .notes(cell.getNotes())
                .build();
    }

    private String buildFullLocationPath(StorageCell cell) {
        StringBuilder path = new StringBuilder(cell.getWarehouse().getName());

        if (cell.getSection() != null) {
            path.append(LOCATION_SEPARATOR).append(SECTION_PREFIX).append(cell.getSection());
        }
        if (cell.getRack() != null) {
            path.append(LOCATION_SEPARATOR).append(RACK_PREFIX).append(cell.getRack());
        }
        if (cell.getShelf() != null) {
            path.append(LOCATION_SEPARATOR).append(SHELF_PREFIX).append(cell.getShelf());
        }
        if (cell.getPosition() != null) {
            path.append(LOCATION_SEPARATOR).append(POSITION_PREFIX).append(cell.getPosition());
        }

        return path.toString();
    }
}