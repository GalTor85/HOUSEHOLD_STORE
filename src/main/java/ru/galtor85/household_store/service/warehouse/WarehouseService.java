package ru.galtor85.household_store.service.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cell.CellNotFoundException;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException;
import ru.galtor85.household_store.dto.request.warehouse.StorageCellCreateRequest;
import ru.galtor85.household_store.dto.request.warehouse.WarehouseCreateRequest;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.dto.response.warehouse.StorageCellDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseDto;
import ru.galtor85.household_store.entity.warehouse.CellType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.mapper.warehouse.StorageCellMapper;
import ru.galtor85.household_store.mapper.warehouse.WarehouseMapper;
import ru.galtor85.household_store.processor.cell.CellAssignmentProcessor;
import ru.galtor85.household_store.processor.cell.CellManagementProcessor;
import ru.galtor85.household_store.processor.warehouse.WarehouseManagementProcessor;
import ru.galtor85.household_store.processor.warehouse.WarehouseStockMovementProcessor;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.cell.CellValidationHelper;
import ru.galtor85.household_store.validator.product.ProductValidator;
import ru.galtor85.household_store.validator.warehouse.WarehouseValidator;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StorageCellRepository storageCellRepository;
    private final WarehouseMapper warehouseMapper;
    private final StorageCellMapper cellMapper;
    private final MessageService messageService;

    // Валидаторы
    private final WarehouseValidator warehouseValidator;
    private final ProductValidator productValidator;
    private final CellValidationHelper cellValidationHelper;

    // Процессоры
    private final WarehouseManagementProcessor warehouseProcessor;
    private final CellManagementProcessor cellProcessor;
    private final CellAssignmentProcessor assignmentProcessor;
    private final WarehouseStockMovementProcessor movementProcessor;

    // ========== WAREHOUSE MANAGEMENT ==========

    @Transactional
    public WarehouseDto createWarehouse(WarehouseCreateRequest request, Long createdBy) {
        // Валидация уникальности
        warehouseValidator.validateWarehouseCodeUnique(request.getCode());

        // Генерация или проверка штрих-кода
        if (request.getBarcode() == null || request.getBarcode().isEmpty()) {
            request.setBarcode(warehouseProcessor.generateWarehouseBarcode(request.getCode()));
            request.setBarcodeFormat("CODE_128");
        } else {
            warehouseValidator.validateWarehouseBarcodeUnique(request.getBarcode());
        }

        // Создание склада
        return warehouseProcessor.createWarehouse(request, createdBy);
    }

    /**
     * Получает склад по ID
     */
    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseById(Long warehouseId) {
        Warehouse warehouse = warehouseValidator.validateWarehouseExists(warehouseId);
        return warehouseMapper.toDto(warehouse);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseDto> getWarehouses(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("code").ascending());

        Page<Warehouse> warehouses;
        if (search != null && !search.isEmpty()) {
            warehouses = warehouseRepository.searchWarehouses(search, pageable);
            warehouseValidator.validateWarehouseSearchResult(!warehouses.isEmpty(), search);
        } else {
            warehouses = warehouseRepository.findAll(pageable);
            if (warehouses.isEmpty()){
                log.warn(messageService.get("warehouse.error.not.found"));
                throw new WarehouseNotFoundException();
            }
        }

        log.debug(messageService.get("warehouse.log.fetched", warehouses.getTotalElements()));
        return warehouses.map(warehouseMapper::toDto);
    }

    // ========== CELL MANAGEMENT ==========

    @Transactional
    public StorageCellDto addCell(Long warehouseId, StorageCellCreateRequest request,
                                  Long createdBy) {
        Warehouse warehouse = warehouseValidator.validateWarehouseExists(warehouseId);
        return cellProcessor.addCell(warehouse, request, createdBy);
    }

    @Transactional
    public StorageCellDto assignProductToCell(Long cellId, Long productId,
                                              int quantity, Long assignedBy) {
        // Находим ячейку
        StorageCell cell = cellProcessor.findCellById(cellId);

        // Находим и валидируем продукт
        Product product = productValidator.findAndValidateProduct(productId, quantity);

        // Проверяем, нет ли уже такого товара в другой ячейке этого же склада
        List<StorageCell> cellsWithProduct = storageCellRepository
                .findByWarehouseIdAndCurrentProductId(cell.getWarehouse().getId(), productId);
        cellValidationHelper.checkProductNotInOtherCells(
                cellsWithProduct, cellId, productId, cell.getWarehouse().getId());

        // Назначаем товар в ячейку через процессор
        StorageCell updatedCell = assignmentProcessor.assignProductToCell(cell, product, quantity, assignedBy);

        return cellMapper.toDto(updatedCell);
    }

    @Transactional
    public StorageCellDto clearCell(Long cellId, Long clearedBy) {
        StorageCell cell = cellProcessor.findCellById(cellId);

        if (!cell.getIsOccupied()) {
            log.warn(messageService.get("cell.log.already.empty", cellId));
            return cellMapper.toDto(cell);
        }

        StorageCell updatedCell = cellProcessor.clearCell(cell, clearedBy);
        return cellMapper.toDto(updatedCell);
    }

    @Transactional(readOnly = true)
    public List<StorageCellDto> getAvailableCells(Long warehouseId, CellType cellType) {
        List<StorageCell> cells = storageCellRepository
                .findAvailableCellsByType(warehouseId, cellType);

        log.debug(messageService.get("cell.log.available.fetched",
                cells.size(), warehouseId, cellType));

        return cells.stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());
    }

    public StorageCellDto getCellById(Long cellId) {
        StorageCell cell = cellProcessor.findCellById(cellId);
        return cellMapper.toDto(cell);
    }

    public List<StorageCellDto> getWarehouseCells(Long warehouseId) {
        warehouseValidator.validateWarehouseExists(warehouseId);

        List<StorageCell> cells = storageCellRepository.findByWarehouseId(warehouseId);

        if (cells.isEmpty()) {
            log.warn(messageService.get("cells.error.not.found", warehouseId));
            throw new CellNotFoundException();
        }

        return cells.stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());
    }

    // ========== STOCK MOVEMENTS ==========

    @Transactional(readOnly = true)
    public List<StockMovementDto> getProductMovements(Long productId) {
        return movementProcessor.getProductMovements(productId);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getCellMovements(Long cellId) {
        return movementProcessor.getCellMovements(cellId);
    }
}