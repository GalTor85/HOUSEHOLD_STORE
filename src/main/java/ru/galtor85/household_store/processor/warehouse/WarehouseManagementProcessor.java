package ru.galtor85.household_store.processor.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.config.WarehouseConfig;
import ru.galtor85.household_store.dto.request.warehouse.WarehouseCreateRequest;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseDto;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.mapper.warehouse.WarehouseMapper;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.util.generator.NumberGenerator;

import java.util.UUID;


/**
 * Processor for warehouse management operations.
 *
 * <p>Handles the creation and configuration of warehouses including
 * barcode generation and persistence.</p>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseManagementProcessor {

    private static final String WAREHOUSE_BARCODE_PREFIX = "WH-";
    private static final String BARCODE_SEPARATOR = "-";
    private static final int UUID_SUBSTRING_LENGTH = 8;

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;
    private final LogMessageService logMsg;
    private final NumberGenerator numberGenerator;
    private final WarehouseConfig warehouseConfig;

    /**
     * Creates a new warehouse.
     *
     * <p>Converts the request to an entity, saves it to the database,
     * and returns the created warehouse as a DTO.</p>
     *
     * @param request the warehouse creation request containing code, name, address, etc.
     * @param createdBy the ID of the user creating the warehouse
     * @return DTO representing the created warehouse
     */
    @Transactional
    public WarehouseDto createWarehouse(WarehouseCreateRequest request, Long createdBy) {
        log.debug(logMsg.get("warehouse.creation.start", request.getCode(), createdBy));

        Warehouse warehouse = warehouseMapper.toEntity(request, createdBy);

        if (warehouse.getBarcodeFormat() == null) {
            warehouse.setBarcodeFormat(warehouseConfig.getDefaultWarehouseBarcodeFormat());
        }

        if (warehouse.getBarcode() == null) {
            warehouse.setBarcode(numberGenerator.generateWarehouseBarcode());
        }

        if (warehouse.getTotalCapacity() == null) {
            warehouse.setTotalCapacity(warehouseConfig.getDefaultWarehouseCapacity());
        }
        if (warehouse.getUsedCapacity() == null) {
            warehouse.setUsedCapacity(warehouseConfig.getDefaultWarehouseCapacity());
        }

        if (warehouse.getIsVisibleForSale() == null) {
            warehouse.setIsVisibleForSale(warehouseConfig.getDefaultWarehouseVisibleForSale());
        }

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        log.info(logMsg.get("warehouse.log.created",
                savedWarehouse.getCode(), savedWarehouse.getId(), createdBy));

        return warehouseMapper.toDto(savedWarehouse);
    }

    /**
     * Generates a unique barcode for a warehouse.
     *
     * <p>Format: WH-{warehouseCode}-{randomUUID}</p>
     * <p>Example: WH-MAIN-a1b2c3d4</p>
     *
     * @param warehouseCode the warehouse code to include in the barcode
     * @return generated unique barcode string
     */
    public String generateWarehouseBarcode(String warehouseCode) {
        String randomPart = UUID.randomUUID().toString().substring(0, UUID_SUBSTRING_LENGTH);
        return WAREHOUSE_BARCODE_PREFIX + warehouseCode + BARCODE_SEPARATOR + randomPart;
    }
}