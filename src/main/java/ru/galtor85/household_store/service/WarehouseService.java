package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.builder.StorageCellBuilder;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.mapper.StorageCellMapper;
import ru.galtor85.household_store.mapper.WarehouseMapper;
import ru.galtor85.household_store.processor.CellAssignmentProcessor;
import ru.galtor85.household_store.repository.*;
import ru.galtor85.household_store.util.CellValidationHelper;
import ru.galtor85.household_store.util.ProductValidator;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
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
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // Утилиты
    private final ProductValidator productValidator;
    private final CellValidationHelper cellValidationHelper;

    // Билдеры
    private final StorageCellBuilder cellBuilder;

    // Процессоры
    private final CellAssignmentProcessor assignmentProcessor;

    // ========== WAREHOUSE MANAGEMENT ==========

    @Transactional
    public WarehouseDto createWarehouse(WarehouseCreateRequest request, Long createdBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Проверка уникальности кода
        if (warehouseRepository.existsByCode(request.getCode())) {
            log.warn(messageService.get("warehouse.log.code.exists", request.getCode()));
            throw new WarehouseAlreadyExistsException("code", request.getCode());
        }

        // Генерация штрих-кода, если не указан
        if (request.getBarcode() == null || request.getBarcode().isEmpty()) {
            request.setBarcode(generateWarehouseBarcode(request.getCode()));
            request.setBarcodeFormat("CODE_128");
        } else {
            // Проверка уникальности штрих-кода
            if (warehouseRepository.existsByBarcode(request.getBarcode())) {
                log.warn(messageService.get("warehouse.log.barcode.exists", request.getBarcode()));
                throw new WarehouseAlreadyExistsException("barcode", request.getBarcode());
            }
        }

        Warehouse warehouse = warehouseMapper.toEntity(request, createdBy);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        log.info(messageService.get("warehouse.log.created",
                savedWarehouse.getCode(), savedWarehouse.getId(), createdBy));

        return warehouseMapper.toDto(savedWarehouse);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseDto> getWarehouses(String search, int page, int size, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Pageable pageable = PageRequest.of(page, size, Sort.by("code").ascending());

        Page<Warehouse> warehouses;
        if (search != null && !search.isEmpty()) {
            warehouses = warehouseRepository.searchWarehouses(search, pageable);
            if (warehouses.isEmpty()) {
                log.warn(messageService.get("warehouse.log.not.found", search));
                throw new WarehouseNotFoundException(search);
            }
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
                                  Long createdBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Warehouse warehouse = findWarehouseById(warehouseId);

        // Проверка уникальности кода ячейки в рамках склада
        if (storageCellRepository.findByCodeAndWarehouseId(request.getCode(), warehouseId).isPresent()) {
            log.warn(messageService.get("cell.log.code.exists", request.getCode(), warehouseId));
            throw new CellAlreadyExistsException(request.getCode(), warehouseId);
        }

        // Генерация штрих-кода для ячейки
        String cellBarcode = generateCellBarcode(warehouse.getCode(), request.getCode());

        StorageCell cell = cellBuilder.buildFromRequest(request, warehouse, cellBarcode);
        StorageCell savedCell = storageCellRepository.save(cell);

        // Обновляем использованную емкость склада
        warehouse.setUsedCapacity(warehouse.getUsedCapacity() + 1);
        warehouseRepository.save(warehouse);

        log.info(messageService.get("cell.log.created",
                savedCell.getCode(), warehouseId, createdBy));

        return cellMapper.toDto(savedCell);
    }

    @Transactional
    public StorageCellDto assignProductToCell(Long cellId, Long productId,
                                              int quantity, Long assignedBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Находим ячейку
        StorageCell cell = findCellById(cellId);

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
    public StorageCellDto clearCell(Long cellId, Long clearedBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        StorageCell cell = findCellById(cellId);

        if (!cell.getIsOccupied()) {
            log.warn(messageService.get("cell.log.already.empty", cellId));
            return cellMapper.toDto(cell);
        }

        Long productId = cell.getCurrentProductId();
        int quantity = cell.getCurrentQuantity();

        cellBuilder.clearCell(cell);
        StorageCell updatedCell = storageCellRepository.save(cell);

        log.info(messageService.get("cell.log.cleared", cellId, productId, quantity, clearedBy));

        return cellMapper.toDto(updatedCell);
    }

    @Transactional(readOnly = true)
    public List<StorageCellDto> getAvailableCells(Long warehouseId, CellType cellType, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<StorageCell> cells = storageCellRepository
                .findAvailableCellsByType(warehouseId, cellType);

        log.debug(messageService.get("cell.log.available.fetched",
                cells.size(), warehouseId, cellType));

        return cells.stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());
    }

    // ========== PRIVATE METHODS ==========

    private Warehouse findWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> {
                    log.error(messageService.get("warehouse.log.not.found", id));
                    return new WarehouseNotFoundException(id);
                });
    }

    private StorageCell findCellById(Long id) {
        return storageCellRepository.findById(id)
                .orElseThrow(() -> {
                    log.error(messageService.get("cell.log.not.found", id));
                    return new CellNotFoundException(id);
                });
    }

    private String generateWarehouseBarcode(String warehouseCode) {
        return "WH-" + warehouseCode + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateCellBarcode(String warehouseCode, String cellCode) {
        return "CELL-" + warehouseCode + "-" + cellCode;
    }

    // В WarehouseService.java добавьте:

    public StorageCellDto getCellById(Long cellId, Locale locale) {
        StorageCell cell = storageCellRepository.findById(cellId)
                .orElseThrow(() -> new CellNotFoundException(cellId));
        return cellMapper.toDto(cell);
    }

    public List<StorageCellDto> getWarehouseCells(Long warehouseId, Locale locale) {
        List<StorageCell> cells = storageCellRepository.findByWarehouseId(warehouseId);

        if (cells.isEmpty()) {
            log.warn(messageService.get("cells.error.not.found", warehouseId));
            throw new CellNotFoundException();
        }

        return cells.stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getProductMovements(Long productId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<StockMovement> movements = stockMovementRepository.findByProductId(productId);

        log.debug(messageService.get("movement.log.fetched.product",
                movements.size(), productId));

        Locale finalLocale = locale;
        return movements.stream()
                .map(movement -> convertToMovementDto(movement, finalLocale))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getCellMovements(Long cellId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<StockMovement> movements = stockMovementRepository
                .findByFromCellIdOrToCellId(cellId, cellId);

        log.debug(messageService.get("movement.log.fetched.cell",
                movements.size(), cellId));

        Locale finalLocale = locale;
        return movements.stream()
                .map(movement -> convertToMovementDto(movement, finalLocale))
                .collect(Collectors.toList());
    }

    private StockMovementDto convertToMovementDto(StockMovement movement, Locale locale) {
        Product product = productRepository.findById(movement.getProductId()).orElse(null);

        String fromCellCode = null;
        String fromWarehouseName = null;
        if (movement.getFromCellId() != null) {
            StorageCell fromCell = storageCellRepository.findById(movement.getFromCellId()).orElse(null);
            if (fromCell != null) {
                fromCellCode = fromCell.getCode();
                fromWarehouseName = fromCell.getWarehouse().getName();
            }
        }

        String toCellCode = null;
        String toWarehouseName = null;
        if (movement.getToCellId() != null) {
            StorageCell toCell = storageCellRepository.findById(movement.getToCellId()).orElse(null);
            if (toCell != null) {
                toCellCode = toCell.getCode();
                toWarehouseName = toCell.getWarehouse().getName();
            }
        }

        String performedByName = null;
        if (movement.getPerformedBy() != null) {
            User user = userRepository.findById(movement.getPerformedBy()).orElse(null);
            performedByName = user != null ? user.getEmail() : null;
        }

        String localizedType = messageService.get("movement.type." + movement.getMovementType().name());

        return StockMovementDto.builder()
                .id(movement.getId())
                .productId(movement.getProductId())
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .fromCellId(movement.getFromCellId())
                .fromCellCode(fromCellCode)
                .fromWarehouseName(fromWarehouseName)
                .toCellId(movement.getToCellId())
                .toCellCode(toCellCode)
                .toWarehouseName(toWarehouseName)
                .quantity(movement.getQuantity())
                .movementType(movement.getMovementType())
                .localizedMovementType(localizedType)
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .performedBy(movement.getPerformedBy())
                .performedByName(performedByName)
                .notes(movement.getNotes())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}