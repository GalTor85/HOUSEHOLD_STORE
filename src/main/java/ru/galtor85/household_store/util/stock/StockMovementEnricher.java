package ru.galtor85.household_store.util.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.builder.stock.StockMovementDtoBuilder;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Enricher for stock movement DTOs.
 */
@Component
@RequiredArgsConstructor
public class StockMovementEnricher {

    private final ProductRepository productRepository;
    private final StorageCellRepository storageCellRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final StockMovementDtoBuilder movementDtoBuilder;
    private final MessageService messageService;

    /**
     * Enriches stock movement with related entity data.
     *
     * @param movement stock movement entity
     * @return enriched stock movement DTO
     */
    public StockMovementDto enrichMovementDto(StockMovement movement) {
        Product product = productRepository.findById(movement.getProductId()).orElse(null);

        String fromCellCode = null;
        String fromWarehouseName = null;
        if (movement.getFromCellId() != null) {
            StorageCell cell = storageCellRepository.findById(movement.getFromCellId()).orElse(null);
            if (cell != null) {
                fromCellCode = cell.getCode();
                fromWarehouseName = cell.getWarehouse().getName();
            } else {
                fromWarehouseName = messageService.get("stock.cell.unknown");
            }
        }

        String toCellCode = null;
        String toWarehouseName = null;
        if (movement.getToCellId() != null) {
            StorageCell cell = storageCellRepository.findById(movement.getToCellId()).orElse(null);
            if (cell != null) {
                toCellCode = cell.getCode();
                toWarehouseName = cell.getWarehouse().getName();
            } else {
                toWarehouseName = messageService.get("stock.cell.unknown");
            }
        }

        String warehouseName = null;
        if (movement.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findById(movement.getWarehouseId()).orElse(null);
            warehouseName = warehouse != null ? warehouse.getName() :
                    messageService.get("stock.warehouse.unknown");
        }

        String performedByName = null;
        if (movement.getPerformedBy() != null) {
            User user = userRepository.findById(movement.getPerformedBy()).orElse(null);
            performedByName = user != null ? user.getEmail() :
                    messageService.get("stock.user.unknown");
        }

        String localizedType = messageService.get("movement.type." + movement.getMovementType().name());

        return movementDtoBuilder.buildDto(
                movement.getId(),
                movement.getProductId(),
                product != null ? product.getName() : messageService.get("stock.product.unknown"),
                product != null ? product.getSku() : messageService.get("stock.product.unknown.sku"),
                movement.getFromCellId(),
                fromCellCode,
                fromWarehouseName,
                movement.getToCellId(),
                toCellCode,
                toWarehouseName,
                movement.getWarehouseId(),
                warehouseName,
                movement.getQuantity(),
                movement.getMovementType(),
                localizedType,
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getReferenceNumber(),
                movement.getPerformedBy(),
                performedByName,
                movement.getNotes(),
                movement.getBatchNumber(),
                movement.getDocumentNumber(),
                movement.getCreatedAt()
        );
    }
}