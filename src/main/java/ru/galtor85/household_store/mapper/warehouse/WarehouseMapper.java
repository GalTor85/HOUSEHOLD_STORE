package ru.galtor85.household_store.mapper.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.warehouse.WarehouseCreateRequest;
import ru.galtor85.household_store.dto.response.warehouse.StorageCellDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseDto;
import ru.galtor85.household_store.entity.warehouse.Warehouse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Warehouse entity to/from DTO.
 *
 * <p>Handles conversion between Warehouse entities and DTOs including
 * storage cell mapping and capacity calculations.</p>
 *
 * @author G@LTor85
 
 */
@Component
@RequiredArgsConstructor
public class WarehouseMapper {

    private final StorageCellMapper cellMapper;

    /**
     * Converts WarehouseCreateRequest to Warehouse entity.
     *
     * @param request the creation request
     * @param createdBy ID of the user creating the warehouse
     * @return warehouse entity
     */
    public Warehouse toEntity(WarehouseCreateRequest request, Long createdBy) {
        if (request == null) {
            return null;
        }

        return Warehouse.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .barcode(request.getBarcode())
                .barcodeFormat(request.getBarcodeFormat())
                .address(request.getAddress())
                .contactPerson(request.getContactPerson())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .isActive(true)
                .totalCapacity(request.getTotalCapacity())
                .usedCapacity(0)
                .createdBy(createdBy)
                .isVisibleForSale(true)
                .build();
    }

    /**
     * Converts Warehouse entity to DTO.
     *
     * @param warehouse the warehouse entity
     * @return warehouse DTO
     */
    public WarehouseDto toDto(Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }

        List<StorageCellDto> cellDtos = mapCells(warehouse);
        int availableCapacity = calculateAvailableCapacity(warehouse);
        double occupancyPercentage = calculateOccupancyPercentage(warehouse);

        return WarehouseDto.builder()
                .id(warehouse.getId())
                .code(warehouse.getCode())
                .name(warehouse.getName())
                .description(warehouse.getDescription())
                .barcode(warehouse.getBarcode())
                .barcodeFormat(warehouse.getBarcodeFormat())
                .address(warehouse.getAddress())
                .contactPerson(warehouse.getContactPerson())
                .contactPhone(warehouse.getContactPhone())
                .contactEmail(warehouse.getContactEmail())
                .isActive(warehouse.getIsActive())
                .isVisibleForSale(warehouse.getIsVisibleForSale())
                .totalCapacity(warehouse.getTotalCapacity())
                .usedCapacity(warehouse.getUsedCapacity())
                .availableCapacity(availableCapacity)
                .occupancyPercentage(occupancyPercentage)
                .cells(cellDtos)
                .createdAt(warehouse.getCreatedAt())
                .build();
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Maps storage cells to DTOs.
     *
     * @param warehouse the warehouse entity
     * @return list of storage cell DTOs
     */
    private List<StorageCellDto> mapCells(Warehouse warehouse) {
        if (warehouse.getCells() == null) {
            return null;
        }
        return warehouse.getCells().stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculates available capacity (total - used).
     *
     * @param warehouse the warehouse entity
     * @return available capacity
     */
    private int calculateAvailableCapacity(Warehouse warehouse) {
        int total = warehouse.getTotalCapacity() != null ? warehouse.getTotalCapacity() : 0;
        int used = warehouse.getUsedCapacity() != null ? warehouse.getUsedCapacity() : 0;
        return total - used;
    }

    /**
     * Calculates occupancy percentage.
     *
     * @param warehouse the warehouse entity
     * @return occupancy percentage (0-100)
     */
    private double calculateOccupancyPercentage(Warehouse warehouse) {
        int total = warehouse.getTotalCapacity() != null ? warehouse.getTotalCapacity() : 0;
        if (total == 0) {
            return 0.0;
        }
        int used = warehouse.getUsedCapacity() != null ? warehouse.getUsedCapacity() : 0;
        return (used * 100.0) / total;
    }
}