package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.WarehouseCreateRequest;
import ru.galtor85.household_store.dto.WarehouseDto;
import ru.galtor85.household_store.entity.Warehouse;
import ru.galtor85.household_store.mapper.WarehouseMapper;
import ru.galtor85.household_store.repository.WarehouseRepository;
import ru.galtor85.household_store.service.MessageService;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseManagementProcessor {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;
    private final MessageService messageService;

    @Transactional
    public WarehouseDto createWarehouse(WarehouseCreateRequest request, Long createdBy) {
        Warehouse warehouse = warehouseMapper.toEntity(request, createdBy);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        log.info(messageService.get("warehouse.log.created",
                savedWarehouse.getCode(), savedWarehouse.getId(), createdBy));

        return warehouseMapper.toDto(savedWarehouse);
    }

    public String generateWarehouseBarcode(String warehouseCode) {
        return "WH-" + warehouseCode + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}