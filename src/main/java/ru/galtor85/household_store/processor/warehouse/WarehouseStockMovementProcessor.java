package ru.galtor85.household_store.processor.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.stock.StockMovementRepository;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseStockMovementProcessor {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final StorageCellRepository storageCellRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public List<StockMovementDto> getProductMovements(Long productId) {
        List<StockMovement> movements = stockMovementRepository.findByProductId(productId);

        log.debug(messageService.get("movement.log.fetched.product",
                movements.size(), productId));

        return movements.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getCellMovements(Long cellId) {
        List<StockMovement> movements = stockMovementRepository
                .findByFromCellIdOrToCellId(cellId, cellId);

        log.debug(messageService.get("movement.log.fetched.cell",
                movements.size(), cellId));

        return movements.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private StockMovementDto convertToDto(StockMovement movement) {
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
            performedByName = user != null ? user.getEmail() :
                    messageService.get("stock.user.unknown");
        }

        String localizedType = messageService.get("movement.type." + movement.getMovementType().name());

        return StockMovementDto.builder()
                .id(movement.getId())
                .productId(movement.getProductId())
                .productName(product != null ? product.getName() :
                        messageService.get("stock.product.unknown"))
                .productSku(product != null ? product.getSku() :
                        messageService.get("stock.product.unknown.sku"))
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