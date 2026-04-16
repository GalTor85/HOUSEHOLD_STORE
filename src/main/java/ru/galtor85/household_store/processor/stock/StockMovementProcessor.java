package ru.galtor85.household_store.processor.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.repository.stock.StockMovementRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.util.stock.StockMovementEnricher;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Processor for stock movement queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMovementProcessor {

    private static final String CREATED_AT_FIELD = "createdAt";

    private final StockMovementRepository movementRepository;
    private final StockMovementEnricher movementEnricher;
    private final LogMessageService logMsg;

    /**
     * Retrieves paginated stock movements for a product.
     *
     * @param productId the product ID
     * @param page      page number
     * @param size      page size
     * @return page of StockMovementDto
     */
    @Transactional(readOnly = true)
    public Page<StockMovementDto> getProductMovements(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT_FIELD).descending());
        Page<StockMovement> movements = movementRepository.findByProductId(productId, pageable);

        log.debug(logMsg.get("stock.movements.product.fetched",
                movements.getTotalElements(), productId));

        return movements.map(movementEnricher::enrichMovementDto);
    }

    /**
     * Retrieves paginated stock movements for a warehouse.
     *
     * @param warehouseId the warehouse ID
     * @param page        page number
     * @param size        page size
     * @return page of StockMovementDto
     */
    @Transactional(readOnly = true)
    public Page<StockMovementDto> getWarehouseMovements(Long warehouseId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT_FIELD).descending());
        Page<StockMovement> movements = movementRepository.findByWarehouseId(warehouseId, pageable);

        log.debug(logMsg.get("stock.movements.warehouse.fetched",
                movements.getTotalElements(), warehouseId));

        return movements.map(movementEnricher::enrichMovementDto);
    }

    /**
     * Retrieves stock movements by reference type and ID.
     *
     * @param refType reference type (e.g., ORDER, PURCHASE)
     * @param refId   reference ID
     * @return list of StockMovementDto
     */
    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByReference(String refType, Long refId) {
        List<StockMovement> movements = movementRepository.findByReference(refType, refId);
        return movements.stream()
                .map(movementEnricher::enrichMovementDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves stock movements by batch number.
     *
     * @param batchNumber the batch/lot number
     * @return list of StockMovementDto
     */
    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByBatch(String batchNumber) {
        List<StockMovement> movements = movementRepository.findByBatchNumber(batchNumber);

        log.debug(logMsg.get("stock.movements.batch.fetched",
                movements.size(), batchNumber));

        return movements.stream()
                .map(movementEnricher::enrichMovementDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all distinct batch numbers for a product.
     *
     * @param productId the product ID
     * @return list of batch numbers
     */
    @Transactional(readOnly = true)
    public List<String> getProductBatches(Long productId) {
        List<String> batches = movementRepository.findBatchNumbersByProduct(productId);
        log.debug(logMsg.get("stock.product.batches.fetched", productId));
        return batches;
    }

    /**
     * Gets stock movements for a cell.
     *
     * @param cellId cell ID
     * @return list of stock movement DTOs
     */
    @Transactional(readOnly = true)
    public List<StockMovementDto> getCellMovements(Long cellId) {
        List<StockMovement> movements = movementRepository.findByCellId(cellId);
        log.debug(logMsg.get("stock.movements.cell.fetched", movements.size(), cellId));
        return movements.stream()
                .map(movementEnricher::enrichMovementDto)
                .collect(Collectors.toList());
    }
}