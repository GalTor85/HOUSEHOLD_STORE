package ru.galtor85.household_store.processor.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.stock.StockMovementBuilder;
import ru.galtor85.household_store.dto.request.stock.StockTransferRequest;
import ru.galtor85.household_store.dto.response.stock.StockTransferResponseDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.stock.StockMovementRepository;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.stock.StockTransferValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Processor for stock transfer operations.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockTransferProcessor {

    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StorageCellRepository storageCellRepository;
    private final StockMovementBuilder movementBuilder;
    private final StockTransferValidator validator;
    private final MessageService messageService;

    /**
     * Transfers stock between warehouses or cells.
     *
     * @param request     transfer request
     * @param performedBy ID of user performing the transfer
     * @return transfer response DTO
     */
    @Transactional
    public StockTransferResponseDto transferStock(StockTransferRequest request, Long performedBy) {
        log.info(messageService.get("stock.transfer.start",
                request.getProductId(), request.getQuantity(),
                request.getFromWarehouseId(), request.getToWarehouseId()));

        // Validate request
        validator.validateTransferRequest(request);

        // Validate entities
        Product product = validator.validateProductExists(request.getProductId());
        Warehouse fromWarehouse = validator.validateSourceWarehouseExists(request.getFromWarehouseId());
        Warehouse toWarehouse = validator.validateDestinationWarehouseExists(request.getToWarehouseId());

        StorageCell fromCell = validator.validateSourceCell(
                request.getFromCellId(), request.getFromCellCode(), request.getFromWarehouseId());
        StorageCell toCell = validator.validateDestinationCell(
                request.getToCellId(), request.getToCellCode(), request.getToWarehouseId());

        // Validate sufficient stock
        validator.validateSufficientStock(product, request.getFromWarehouseId(), request.getQuantity());

        // Decrease stock in source
        int updated = productStockRepository.decreaseStock(
                product.getId(), request.getFromWarehouseId(), request.getQuantity());

        if (updated == 0) {
            throw new IllegalStateException(
                    messageService.get("stock.transfer.decrease.failed", product.getId()));
        }

        // Increase stock in destination (create if not exists)
        ProductStock destStock = productStockRepository
                .findByProductIdAndWarehouseId(product.getId(), request.getToWarehouseId())
                .orElse(null);

        if (destStock == null) {
            destStock = ProductStock.builder()
                    .productId(product.getId())
                    .warehouseId(request.getToWarehouseId())
                    .quantity(request.getQuantity())
                    .reservedQuantity(0)
                    .availableQuantity(request.getQuantity())
                    .build();
            productStockRepository.save(destStock);
        } else {
            productStockRepository.increaseStock(
                    product.getId(), request.getToWarehouseId(), request.getQuantity());
        }

        // Update source cell if specified
        if (fromCell != null) {
            updateSourceCell(fromCell, request.getQuantity());
        }

        // Update destination cell if specified
        if (toCell != null) {
            updateDestinationCell(toCell, product.getId(), request.getQuantity());
        }

        // Create stock movement records (incoming and outgoing)
        List<StockMovement> movements = createStockMovements(
                product, fromWarehouse, toWarehouse, fromCell, toCell, request, performedBy);

        stockMovementRepository.saveAll(movements);

        log.info(messageService.get("stock.transfer.complete",
                product.getId(), request.getQuantity(),
                fromWarehouse.getName(), toWarehouse.getName()));

        return buildResponse(fromWarehouse, toWarehouse, fromCell, toCell,
                movements.isEmpty() ? null : movements.getFirst(), request);
    }

    /**
     * Updates source cell after stock removal.
     */
    private void updateSourceCell(StorageCell fromCell, int quantity) {
        int currentQuantity = fromCell.getCurrentQuantity() != null ? fromCell.getCurrentQuantity() : 0;
        int newQuantity = currentQuantity - quantity;

        if (newQuantity <= 0) {
            fromCell.setCurrentProductId(null);
            fromCell.setCurrentQuantity(0);
            fromCell.setIsOccupied(false);
        } else {
            fromCell.setCurrentQuantity(newQuantity);
        }
        storageCellRepository.save(fromCell);

        log.debug(messageService.get("stock.transfer.source.cell.updated",
                fromCell.getCode(), currentQuantity, newQuantity));
    }

    /**
     * Updates destination cell after stock addition.
     */
    private void updateDestinationCell(StorageCell toCell, Long productId, int quantity) {
        int currentQuantity = toCell.getCurrentQuantity() != null ? toCell.getCurrentQuantity() : 0;
        int newQuantity = currentQuantity + quantity;

        toCell.setCurrentProductId(productId);
        toCell.setCurrentQuantity(newQuantity);
        toCell.setIsOccupied(true);
        storageCellRepository.save(toCell);

        log.debug(messageService.get("stock.transfer.dest.cell.updated",
                toCell.getCode(), currentQuantity, newQuantity));
    }

    /**
     * Creates stock movement records for transfer.
     * Creates two records: OUTGOING from source warehouse and INCOMING to destination warehouse.
     */
    private List<StockMovement> createStockMovements(Product product,
                                                     Warehouse fromWarehouse,
                                                     Warehouse toWarehouse,
                                                     StorageCell fromCell,
                                                     StorageCell toCell,
                                                     StockTransferRequest request,
                                                     Long performedBy) {

        List<StockMovement> movements = new ArrayList<>();

        String reason = request.getReason() != null ? request.getReason() :
                messageService.get("stock.transfer.default.reason");

        String outgoingNotes = messageService.get("stock.transfer.outgoing.notes",
                reason, request.getQuantity(), toWarehouse.getName());

        String incomingNotes = messageService.get("stock.transfer.incoming.notes",
                reason, request.getQuantity(), fromWarehouse.getName());

        // 1. OUTGOING movement
        StockMovement outgoing = movementBuilder.buildFullMovement(
                product.getId(),
                fromCell != null ? fromCell.getId() : null,
                null,
                fromWarehouse.getId(),
                request.getQuantity(),
                MovementType.TRANSFER,
                "TRANSFER_OUT",
                null,
                null,
                performedBy,
                outgoingNotes,
                request.getBatchNumber(),
                null
        );
        movements.add(outgoing);

        // 2. INCOMING movement
        StockMovement incoming = movementBuilder.buildFullMovement(
                product.getId(),
                null,
                toCell != null ? toCell.getId() : null,
                toWarehouse.getId(),
                request.getQuantity(),
                MovementType.TRANSFER,
                "TRANSFER_IN",
                null,
                null,
                performedBy,
                incomingNotes,
                request.getBatchNumber(),
                null
        );
        movements.add(incoming);

        // Link the two movements
        outgoing.setReferenceId(incoming.getId());
        incoming.setReferenceId(outgoing.getId());

        log.debug(messageService.get("stock.transfer.movements.created",
                outgoing.getId(), incoming.getId(), product.getId(), request.getQuantity()));

        return movements;
    }

    /**
     * Builds response DTO for stock transfer.
     */
    private StockTransferResponseDto buildResponse(Warehouse fromWarehouse,
                                                   Warehouse toWarehouse,
                                                   StorageCell fromCell,
                                                   StorageCell toCell,
                                                   StockMovement movement,
                                                   StockTransferRequest request) {
        return StockTransferResponseDto.builder()
                .fromWarehouseId(fromWarehouse.getId())
                .fromWarehouseName(fromWarehouse.getName())
                .fromCellId(fromCell != null ? fromCell.getId() : null)
                .fromCellCode(fromCell != null ? fromCell.getCode() : null)
                .toWarehouseId(toWarehouse.getId())
                .toWarehouseName(toWarehouse.getName())
                .toCellId(toCell != null ? toCell.getId() : null)
                .toCellCode(toCell != null ? toCell.getCode() : null)
                .quantity(request.getQuantity())
                .movementId(movement != null ? movement.getId() : null)
                .transferredAt(LocalDateTime.now())
                .localizedMessage(messageService.get("stock.transfer.success",
                        request.getQuantity(),
                        fromWarehouse.getName(),
                        toWarehouse.getName()))
                .build();
    }
}