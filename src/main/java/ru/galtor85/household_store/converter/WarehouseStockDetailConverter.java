package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.stock.WarehouseStockDetailDto;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Converter for warehouse stock details.
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseStockDetailConverter {

    private final MessageService messageService;

    /**
     * Converts warehouse and stock data to DTO.
     *
     * @param warehouse         the warehouse entity
     * @param quantity          quantity in stock
     * @param reservedQuantity  reserved quantity
     * @param availableQuantity available quantity
     * @return warehouse stock detail DTO
     */
    public WarehouseStockDetailDto toDto(Warehouse warehouse, Integer quantity,
                                         Integer reservedQuantity, Integer availableQuantity) {
        if (warehouse == null) {
            return null;
        }

        String localizedStatus = getLocalizedStatus(availableQuantity);

        return WarehouseStockDetailDto.builder()
                .warehouseId(warehouse.getId())
                .warehouseName(warehouse.getName())
                .isVisibleForSale(warehouse.getIsVisibleForSale())
                .quantity(quantity != null ? quantity : 0)
                .reservedQuantity(reservedQuantity != null ? reservedQuantity : 0)
                .availableQuantity(availableQuantity)
                .localizedStatus(localizedStatus)
                .build();
    }

    private String getLocalizedStatus(int availableQuantity) {
        if (availableQuantity <= 0) {
            return messageService.get("stock.status.out_of_stock");
        }
        return messageService.get("stock.status.in_stock");
    }
}