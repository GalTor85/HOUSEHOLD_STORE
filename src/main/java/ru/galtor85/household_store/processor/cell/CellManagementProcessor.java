package ru.galtor85.household_store.processor.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cell.CellAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.cell.CellNotFoundException;
import ru.galtor85.household_store.builder.warehouse.StorageCellBuilder;
import ru.galtor85.household_store.dto.request.warehouse.StorageCellCreateRequest;
import ru.galtor85.household_store.dto.response.warehouse.StorageCellDto;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.mapper.warehouse.StorageCellMapper;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import static ru.galtor85.household_store.constants.TechnicalConstants.CELL_BARCODE_PREFIX;
import static ru.galtor85.household_store.constants.TechnicalConstants.CELL_BARCODE_SEPARATOR;

/**
 * Processor for storage cell management operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CellManagementProcessor {

    private final StorageCellRepository storageCellRepository;
    private final StorageCellMapper cellMapper;
    private final StorageCellBuilder cellBuilder;
    private final LogMessageService logMsg;

    /**
     * Adds a new storage cell to a warehouse.
     *
     * @param warehouse the target warehouse
     * @param request   the cell creation request
     * @param createdBy ID of the user creating the cell
     * @return created StorageCellDto
     * @throws CellAlreadyExistsException if cell code already exists in warehouse
     */
    @Transactional
    public StorageCellDto addCell(Warehouse warehouse, StorageCellCreateRequest request,
                                  Long createdBy) {

        // Check cell code uniqueness within warehouse
        if (storageCellRepository.findByCodeAndWarehouseId(request.getCode(), warehouse.getId()).isPresent()) {
            log.warn(logMsg.get("cell.log.code.exists", request.getCode(), warehouse.getId()));
            throw new CellAlreadyExistsException(request.getCode(), warehouse.getId());
        }

        // Generate barcode for the cell
        String cellBarcode = generateCellBarcode(warehouse.getCode(), request.getCode());

        StorageCell cell = cellBuilder.buildFromRequest(request, warehouse, cellBarcode);
        StorageCell savedCell = storageCellRepository.save(cell);

        log.info(logMsg.get("cell.log.created",
                savedCell.getCode(), warehouse.getId(), createdBy));

        return cellMapper.toDto(savedCell);
    }

    /**
     * Clears a storage cell (removes product).
     *
     * @param cell      the cell to clear
     * @param clearedBy ID of the user clearing the cell
     * @return updated StorageCell entity
     */
    @Transactional
    public StorageCell clearCell(StorageCell cell, Long clearedBy) {
        Long productId = cell.getCurrentProductId();
        int quantity = cell.getCurrentQuantity();

        cellBuilder.clearCell(cell);
        StorageCell updatedCell = storageCellRepository.save(cell);

        log.info(logMsg.get("cell.log.cleared", cell.getId(), productId, quantity, clearedBy));

        return updatedCell;
    }

    /**
     * Finds a storage cell by ID.
     *
     * @param cellId the cell ID
     * @return StorageCell entity
     * @throws CellNotFoundException if cell not found
     */
    public StorageCell findCellById(Long cellId) {
        return storageCellRepository.findById(cellId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("cell.log.not.found", cellId));
                    return new CellNotFoundException(cellId);
                });
    }

    /**
     * Generates a unique barcode for a storage cell.
     *
     * @param warehouseCode the warehouse code
     * @param cellCode      the cell code
     * @return generated barcode string
     */
    private String generateCellBarcode(String warehouseCode, String cellCode) {
        return CELL_BARCODE_PREFIX + warehouseCode + CELL_BARCODE_SEPARATOR + cellCode;
    }
}