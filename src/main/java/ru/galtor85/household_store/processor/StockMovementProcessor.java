package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.StockMovementDto;
import ru.galtor85.household_store.entity.StockMovement;
import ru.galtor85.household_store.repository.StockMovementRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.util.StockMovementEnricher;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockMovementProcessor {

    private final StockMovementRepository movementRepository;
    private final StockMovementEnricher movementEnricher;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public Page<StockMovementDto> getProductMovements(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StockMovement> movements = movementRepository.findByProductId(productId, pageable);

        log.debug(messageService.get("stock.movements.product.fetched",
                movements.getTotalElements(), productId));

        return movements.map(movementEnricher::enrichMovementDto);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> getWarehouseMovements(Long warehouseId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StockMovement> movements = movementRepository.findByWarehouseId(warehouseId, pageable);

        log.debug(messageService.get("stock.movements.warehouse.fetched",
                movements.getTotalElements(), warehouseId));

        return movements.map(movementEnricher::enrichMovementDto);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByReference(String refType, Long refId) {
        List<StockMovement> movements = movementRepository.findByReference(refType, refId);

        return movements.stream()
                .map(movementEnricher::enrichMovementDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByBatch(String batchNumber) {
        List<StockMovement> movements = movementRepository.findByBatchNumber(batchNumber);

        log.debug(messageService.get("stock.movements.batch.fetched",
                movements.size(), batchNumber));

        return movements.stream()
                .map(movementEnricher::enrichMovementDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getProductBatches(Long productId) {
        List<String> batches = movementRepository.findBatchNumbersByProduct(productId);

        log.debug(messageService.get("stock.product.batches.fetched",
                batches.size(), productId));

        return batches;
    }

    @Transactional(readOnly = true)
    public StockMovementDto getLatestBatchMovement(Long productId, String batchNumber) {
        Pageable pageable = PageRequest.of(0, 1);
        List<StockMovement> movements = movementRepository
                .findLatestByBatchAndProduct(batchNumber, productId, pageable);

        if (movements.isEmpty()) {
            return null;
        }

        return movementEnricher.enrichMovementDto(movements.get(0));
    }
}