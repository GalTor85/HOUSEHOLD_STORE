package ru.galtor85.household_store.validator.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.order.WriteOffInsufficientStockException;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.stock.StockService;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAvailabilityValidator {

    private final StockService stockService;
    private final LogMessageService logMsg;


    /**
     * Validates sufficient stock availability.
     *
     * @param product           the product
     * @param requestedQuantity the requested quantity
     * @throws WriteOffInsufficientStockException if stock is insufficient
     */
    public void validateStockAvailability(Product product, int requestedQuantity) {
        Integer totalStock = stockService.getTotalStockForProduct(product.getId());
        if (totalStock == null || totalStock < requestedQuantity) {
            log.error(logMsg.get("writeoff.processor.insufficient.stock",
                    product.getSku(), totalStock != null ? totalStock : 0, requestedQuantity));
            throw new WriteOffInsufficientStockException(
                    product.getId(),
                    totalStock != null ? totalStock : 0,
                    requestedQuantity
            );
        }
    }
}
