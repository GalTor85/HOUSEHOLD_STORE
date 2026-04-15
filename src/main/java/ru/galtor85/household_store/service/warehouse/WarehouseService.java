package ru.galtor85.household_store.service.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.constants.TechnicalConstants;
import ru.galtor85.household_store.dto.request.warehouse.StorageCellCreateRequest;
import ru.galtor85.household_store.dto.request.warehouse.WarehouseCreateRequest;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.dto.response.warehouse.StorageCellDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.CellType;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.mapper.warehouse.StorageCellMapper;
import ru.galtor85.household_store.mapper.warehouse.WarehouseMapper;
import ru.galtor85.household_store.processor.cell.CellAssignmentProcessor;
import ru.galtor85.household_store.processor.cell.CellManagementProcessor;
import ru.galtor85.household_store.processor.stock.StockMovementProcessor;
import ru.galtor85.household_store.processor.warehouse.WarehouseManagementProcessor;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.validator.cell.CellValidationHelper;
import ru.galtor85.household_store.validator.product.ProductValidator;
import ru.galtor85.household_store.validator.warehouse.WarehouseValidator;

import java.util.List;
import java.util.stream.Collectors;

import static ru.galtor85.household_store.constants.PaginationConstants.CODE_DIRECTION;

/**
 * Service for warehouse and storage cell management.
 *
 * <p>This service provides comprehensive warehouse management functionality:</p>
 * <ul>
 *   <li>Warehouse CRUD operations</li>
 *   <li>Storage cell management (create, assign, clear)</li>
 *   <li>Stock movement tracking</li>
 *   <li>Warehouse search and pagination</li>
 * </ul>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StorageCellRepository storageCellRepository;
    private final WarehouseMapper warehouseMapper;
    private final StorageCellMapper cellMapper;
    private final LogMessageService logMsg;
    private final BusinessConfig businessConfig;

    // Validators
    private final WarehouseValidator warehouseValidator;
    private final ProductValidator productValidator;
    private final CellValidationHelper cellValidationHelper;

    // Processors
    private final WarehouseManagementProcessor warehouseProcessor;
    private final CellManagementProcessor cellProcessor;
    private final CellAssignmentProcessor assignmentProcessor;
    private final StockMovementProcessor movementProcessor;

    // =========================================================================
    // WAREHOUSE MANAGEMENT
    // =========================================================================

    /**
     * Creates a new warehouse.
     *
     * @param request   warehouse creation request
     * @param createdBy ID of the user creating the warehouse
     * @return created warehouse DTO
     */
    @Transactional
    public WarehouseDto createWarehouse(WarehouseCreateRequest request, Long createdBy) {
        log.info(logMsg.get("warehouse.service.create.start", request.getCode()));

        // Validate uniqueness
        warehouseValidator.validateWarehouseCodeUnique(request.getCode());

        // Generate or validate barcode
        if (request.getBarcode() == null || request.getBarcode().isEmpty()) {
            request.setBarcode(warehouseProcessor.generateWarehouseBarcode(request.getCode()));
            request.setBarcodeFormat(TechnicalConstants.DEFAULT_BARCODE_FORMAT);
        } else {
            warehouseValidator.validateWarehouseBarcodeUnique(request.getBarcode());
        }

        // Create warehouse
        WarehouseDto result = warehouseProcessor.createWarehouse(request, createdBy);

        log.info(logMsg.get("warehouse.service.created", result.getCode(), result.getId()));

        return result;
    }

    /**
     * Retrieves a warehouse by ID.
     *
     * @param warehouseId warehouse identifier
     * @return warehouse DTO
     */
    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseById(Long warehouseId) {
        log.debug(logMsg.get("warehouse.service.get.by.id", warehouseId));
        Warehouse warehouse = warehouseValidator.validateWarehouseExists(warehouseId);
        return warehouseMapper.toDto(warehouse);
    }

    /**
     * Retrieves a paginated list of warehouses with optional search.
     *
     * @param search search term (name, code, or barcode)
     * @param page   page number (0-indexed, optional)
     * @param size   page size (optional)
     * @return page of warehouse DTOs
     */
    @Transactional(readOnly = true)
    public Page<WarehouseDto> getWarehouses(String search, Integer page, Integer size) {
        // Use configuration defaults if parameters are null
        int effectivePage = page != null ? page : businessConfig.getPagination().getDefaultPage();
        int effectiveSize = size != null ? size : businessConfig.getPagination().getDefaultSize();

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize, Sort.by(CODE_DIRECTION).ascending());

        Page<Warehouse> warehouses;
        if (search != null && !search.isEmpty()) {
            warehouses = warehouseRepository.searchWarehouses(search, pageable);
            warehouseValidator.validateWarehouseSearchResult(!warehouses.isEmpty(), search);
        } else {
            warehouses = warehouseRepository.findAll(pageable);
            if (warehouses.isEmpty()) {
                log.warn(logMsg.get("warehouse.error.not.found"));
                throw new WarehouseNotFoundException();
            }
        }

        log.debug(logMsg.get("warehouse.log.fetched", warehouses.getTotalElements()));
        return warehouses.map(warehouseMapper::toDto);
    }

    // =========================================================================
    // CELL MANAGEMENT
    // =========================================================================

    /**
     * Adds a storage cell to a warehouse.
     *
     * @param warehouseId warehouse identifier
     * @param request     cell creation request
     * @param createdBy   ID of the user creating the cell
     * @return created cell DTO
     */
    @Transactional
    public StorageCellDto addCell(Long warehouseId, StorageCellCreateRequest request,
                                  Long createdBy) {
        log.info(logMsg.get("warehouse.service.add.cell.start", warehouseId, request.getCode()));
        Warehouse warehouse = warehouseValidator.validateWarehouseExists(warehouseId);
        StorageCellDto result = cellProcessor.addCell(warehouse, request, createdBy);
        log.info(logMsg.get("warehouse.service.add.cell.success", result.getCode()));
        return result;
    }

    /**
     * Assigns a product to a storage cell.
     *
     * @param cellId     cell identifier
     * @param productId  product identifier
     * @param quantity   quantity to store
     * @return updated cell DTO
     */
    @Transactional
    public StorageCellDto assignProductToCell(Long cellId, Long productId,
                                              int quantity) {
        log.info(logMsg.get("warehouse.service.assign.product.start", cellId, productId, quantity));

        // Find cell
        StorageCell cell = cellProcessor.findCellById(cellId);

        // Find and validate product
        Product product = productValidator.findAndValidateProduct(productId, quantity);

        // Check if product is already in another cell of the same warehouse
        List<StorageCell> cellsWithProduct = storageCellRepository
                .findByWarehouseIdAndCurrentProductId(cell.getWarehouse().getId(), productId);
        cellValidationHelper.checkProductNotInOtherCells(
                cellsWithProduct, cellId, productId, cell.getWarehouse().getId());

        // Assign product to cell via processor
        StorageCell updatedCell = assignmentProcessor.assignProductToCell(cell, product, quantity);

        log.info(logMsg.get("warehouse.service.assign.product.success", productId, cellId));

        return cellMapper.toDto(updatedCell);
    }

    /**
     * Clears a storage cell (removes product).
     *
     * @param cellId    cell identifier
     * @param clearedBy ID of the user clearing the cell
     * @return updated cell DTO
     */
    @Transactional
    public StorageCellDto clearCell(Long cellId, Long clearedBy) {
        log.info(logMsg.get("warehouse.service.clear.cell.start", cellId));

        StorageCell cell = cellProcessor.findCellById(cellId);

        if (!cell.getIsOccupied()) {
            log.warn(logMsg.get("cell.log.already.empty", cellId));
            return cellMapper.toDto(cell);
        }

        StorageCell updatedCell = cellProcessor.clearCell(cell, clearedBy);

        log.info(logMsg.get("warehouse.service.clear.cell.success", cellId));

        return cellMapper.toDto(updatedCell);
    }

    /**
     * Retrieves available (unoccupied) cells in a warehouse.
     *
     * @param warehouseId warehouse identifier
     * @param cellType    cell type filter (optional)
     * @return list of available cell DTOs
     */
    @Transactional(readOnly = true)
    public List<StorageCellDto> getAvailableCells(Long warehouseId, CellType cellType) {
        log.debug(logMsg.get("warehouse.service.get.available.cells", warehouseId, cellType));

        List<StorageCell> cells = storageCellRepository
                .findAvailableCellsByType(warehouseId, cellType);

        log.debug(logMsg.get("cell.log.available.fetched",
                cells.size(), warehouseId, cellType));

        return cells.stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a storage cell by ID.
     *
     * @param cellId cell identifier
     * @return cell DTO
     */
    @Transactional(readOnly = true)
    public StorageCellDto getCellById(Long cellId) {
        log.debug(logMsg.get("warehouse.service.get.cell.by.id", cellId));
        StorageCell cell = cellProcessor.findCellById(cellId);
        return cellMapper.toDto(cell);
    }

    /**
     * Retrieves all cells in a warehouse.
     *
     * @param warehouseId warehouse identifier
     * @return list of cell DTOs
     */
    @Transactional(readOnly = true)
    public List<StorageCellDto> getWarehouseCells(Long warehouseId) {
        log.debug(logMsg.get("warehouse.service.get.warehouse.cells", warehouseId));

        warehouseValidator.validateWarehouseExists(warehouseId);

        List<StorageCell> cells = storageCellRepository.findByWarehouseId(warehouseId);

        log.debug(logMsg.get("warehouse.service.get.warehouse.cells.count", cells.size(), warehouseId));

        return cells.stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // STOCK MOVEMENTS
    // =========================================================================

       /**
     * Retrieves stock movements for a cell.
     *
     * @param cellId cell identifier
     * @return list of stock movement DTOs
     */
    @Transactional(readOnly = true)
    public List<StockMovementDto> getCellMovements(Long cellId) {
        log.debug(logMsg.get("warehouse.service.get.cell.movements", cellId));
        return movementProcessor.getCellMovements(cellId);
    }
}