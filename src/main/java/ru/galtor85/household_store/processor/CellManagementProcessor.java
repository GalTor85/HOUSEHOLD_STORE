package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.CellAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.CellNotFoundException;
import ru.galtor85.household_store.builder.StorageCellBuilder;
import ru.galtor85.household_store.dto.StorageCellCreateRequest;
import ru.galtor85.household_store.dto.StorageCellDto;
import ru.galtor85.household_store.entity.StorageCell;
import ru.galtor85.household_store.entity.Warehouse;
import ru.galtor85.household_store.mapper.StorageCellMapper;
import ru.galtor85.household_store.repository.StorageCellRepository;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class CellManagementProcessor {

    private final StorageCellRepository storageCellRepository;
    private final StorageCellMapper cellMapper;
    private final StorageCellBuilder cellBuilder;
    private final MessageService messageService;

    @Transactional
    public StorageCellDto addCell(Warehouse warehouse, StorageCellCreateRequest request,
                                  Long createdBy) {

        // Проверка уникальности кода ячейки в рамках склада
        if (storageCellRepository.findByCodeAndWarehouseId(request.getCode(), warehouse.getId()).isPresent()) {
            log.warn(messageService.get("cell.log.code.exists", request.getCode(), warehouse.getId()));
            throw new CellAlreadyExistsException(request.getCode(), warehouse.getId());
        }

        // Генерация штрих-кода для ячейки
        String cellBarcode = generateCellBarcode(warehouse.getCode(), request.getCode());

        StorageCell cell = cellBuilder.buildFromRequest(request, warehouse, cellBarcode);
        StorageCell savedCell = storageCellRepository.save(cell);

        log.info(messageService.get("cell.log.created",
                savedCell.getCode(), warehouse.getId(), createdBy));

        return cellMapper.toDto(savedCell);
    }

    @Transactional
    public StorageCell clearCell(StorageCell cell, Long clearedBy) {
        Long productId = cell.getCurrentProductId();
        int quantity = cell.getCurrentQuantity();

        cellBuilder.clearCell(cell);
        StorageCell updatedCell = storageCellRepository.save(cell);

        log.info(messageService.get("cell.log.cleared", cell.getId(), productId, quantity, clearedBy));

        return updatedCell;
    }

    public StorageCell findCellById(Long cellId) {
        return storageCellRepository.findById(cellId)
                .orElseThrow(() -> {
                    log.error(messageService.get("cell.log.not.found", cellId));
                    return new CellNotFoundException(cellId);
                });
    }

    private String generateCellBarcode(String warehouseCode, String cellCode) {
        return "CELL-" + warehouseCode + "-" + cellCode;
    }
}