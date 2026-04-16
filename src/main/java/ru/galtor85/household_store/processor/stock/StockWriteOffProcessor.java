package ru.galtor85.household_store.processor.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.order.WriteOffInsufficientStockException;
import ru.galtor85.household_store.builder.stock.StockMovementBuilder;
import ru.galtor85.household_store.dto.common.StockWriteOffItem;
import ru.galtor85.household_store.dto.request.stock.StockWriteOffRequest;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.stock.StockMovementRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.entity.EntityFinder;
import ru.galtor85.household_store.util.generator.NumberGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_QUANTITY;
import static ru.galtor85.household_store.constants.TechnicalConstants.WRITE_OFF_REFERENCE_TYPE;

/**
 * Processor for stock write-off operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockWriteOffProcessor {

    private final EntityFinder entityFinder;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementBuilder movementBuilder;
    private final NumberGenerator numberGenerator;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    // =========================================================================
    // MAIN WRITE-OFF METHOD
    // =========================================================================

    /**
     * Processes stock write-off request.
     *
     * @param request   the write-off request
     * @param managerId the manager ID
     */
    @Transactional
    public void processWriteOff(StockWriteOffRequest request, Long managerId) {

        log.info(logMsg.get("writeoff.processor.start",
                request.getItems().size(), request.getReason(), managerId));

        List<StockMovement> movements = new ArrayList<>();
        List<WriteOffFailedItem> failedItems = new ArrayList<>();
        List<WriteOffSuccessItem> successItems = new ArrayList<>();

        String documentNumber = numberGenerator.generateWriteOffNumber();

        for (StockWriteOffItem item : request.getItems()) {
            try {
                Product product = entityFinder.findProductById(item.getProductId());
                validateStockAvailability(product, item.getQuantity());

                int oldQuantity = product.getQuantityInStock();
                int newQuantity = oldQuantity - item.getQuantity();
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                if (request.getWarehouseId() != null) {
                    updateProductStock(product, item.getQuantity(), request.getWarehouseId());
                }

                StockMovement movement = createWriteOffMovement(
                        product, item, request, documentNumber, managerId);
                movements.add(stockMovementRepository.save(movement));

                successItems.add(new WriteOffSuccessItem(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        item.getQuantity(),
                        oldQuantity,
                        newQuantity
                ));

                log.debug(logMsg.get("writeoff.processor.item.processed",
                        product.getSku(), item.getQuantity(), oldQuantity, newQuantity));

            } catch (Exception e) {
                log.error(logMsg.get("writeoff.processor.item.failed",
                        item.getProductId(), e.getMessage()), e);

                failedItems.add(new WriteOffFailedItem(
                        item.getProductId(),
                        item.getQuantity(),
                        e.getMessage()
                ));
            }
        }

        log.info(logMsg.get("writeoff.processor.complete",
                successItems.size(), movements.size(), failedItems.size()));

        WriteOffResult.builder()
                .movements(movements)
                .successItems(successItems)
                .failedItems(failedItems)
                .allSuccess(failedItems.isEmpty())
                .documentNumber(documentNumber)
                .build();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Validates sufficient stock availability.
     *
     * @param product           the product
     * @param requestedQuantity the requested quantity
     * @throws WriteOffInsufficientStockException if stock is insufficient
     */
    private void validateStockAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.error(logMsg.get("writeoff.processor.insufficient.stock",
                    product.getSku(), product.getQuantityInStock(), requestedQuantity));
            throw new WriteOffInsufficientStockException(
                    product.getId(),
                    product.getQuantityInStock(),
                    requestedQuantity
            );
        }
    }

    /**
     * Updates product stock record in product_stocks table.
     *
     * @param product     the product
     * @param quantity    the quantity to subtract
     * @param warehouseId the warehouse ID
     */
    private void updateProductStock(Product product, int quantity, Long warehouseId) {
        ProductStock stock = productStockRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouseId)
                .orElse(null);

        if (stock == null) {
            log.warn(logMsg.get("writeoff.processor.stock.not.found",
                    product.getSku(), warehouseId));
            return;
        }

        int oldQuantity = stock.getQuantity();
        int newQuantity = oldQuantity - quantity;

        if (newQuantity < DEFAULT_QUANTITY) {
            log.error(logMsg.get("writeoff.processor.stock.negative",
                    product.getSku(), warehouseId, oldQuantity, quantity));
            throw new WriteOffInsufficientStockException(
                    product.getId(), oldQuantity, quantity
            );
        }

        stock.setQuantity(newQuantity);
        stock.setAvailableQuantity(newQuantity -
                (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : DEFAULT_QUANTITY));
        stock.setUpdatedAt(LocalDateTime.now());

        productStockRepository.save(stock);

        log.debug(logMsg.get("writeoff.processor.stock.updated",
                product.getSku(), warehouseId, oldQuantity, newQuantity));
    }

    /**
     * Creates a stock movement record for write-off.
     *
     * @param product        the product
     * @param item           the write-off item
     * @param request        the write-off request
     * @param documentNumber the document number
     * @param managerId      the manager ID
     * @return StockMovement entity
     */
    private StockMovement createWriteOffMovement(Product product,
                                                 StockWriteOffItem item,
                                                 StockWriteOffRequest request,
                                                 String documentNumber,
                                                 Long managerId) {

        String notes = messageService.get("writeoff.processor.movement.notes",
                request.getReason(),
                item.getReason() != null ? item.getReason() : request.getReason(),
                documentNumber);

        return movementBuilder.buildFullMovement(
                product.getId(),
                null,
                null,
                request.getWarehouseId(),
                item.getQuantity(),
                MovementType.WRITE_OFF,
                WRITE_OFF_REFERENCE_TYPE,
                request.getRelatedOrderId(),
                documentNumber,
                managerId,
                notes,
                item.getBatchNumber(),
                documentNumber
        );
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    /**
     * Result of write-off operation.
     */
    @lombok.Value
    @lombok.Builder
    public static class WriteOffResult {
        List<StockMovement> movements;
        List<WriteOffSuccessItem> successItems;
        List<WriteOffFailedItem> failedItems;
        boolean allSuccess;
        String documentNumber;
    }

    /**
     * Successfully written-off item.
     */
    public record WriteOffSuccessItem(
            Long productId,
            String productSku,
            String productName,
            int quantity,
            int oldStock,
            int newStock
    ) {}

    /**
     * Failed write-off item.
     */
    public record WriteOffFailedItem(
            Long productId,
            int requestedQuantity,
            String errorMessage
    ) {}
}