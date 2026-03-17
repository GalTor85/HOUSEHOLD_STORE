package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.StorageCellDto;
import ru.galtor85.household_store.dto.WarehouseCreateRequest;
import ru.galtor85.household_store.dto.WarehouseDto;
import ru.galtor85.household_store.entity.StorageCell;
import ru.galtor85.household_store.entity.Warehouse;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WarehouseMapper {

    private final StorageCellMapper cellMapper;

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
                .build();
    }

    public WarehouseDto toDto(Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }

        List<StorageCellDto> cellDtos = warehouse.getCells().stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());

        int availableCapacity = warehouse.getTotalCapacity() - warehouse.getUsedCapacity();
        double occupancyPercentage = warehouse.getTotalCapacity() > 0 ?
                (warehouse.getUsedCapacity() * 100.0) / warehouse.getTotalCapacity() : 0;

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
                .totalCapacity(warehouse.getTotalCapacity())
                .usedCapacity(warehouse.getUsedCapacity())
                .availableCapacity(availableCapacity)
                .occupancyPercentage(occupancyPercentage)
                .cells(cellDtos)
                .createdAt(warehouse.getCreatedAt())
                .build();
    }
}