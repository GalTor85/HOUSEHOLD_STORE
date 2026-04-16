package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.StockDisplayConfig;
import ru.galtor85.household_store.dto.response.stock.ProductAvailabilityDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Converter for creating ProductAvailabilityDto from Product and stock data.
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAvailabilityConverter {

    private final MessageService messageService;
    private final StockDisplayConfig config;

    /**
     * Converts product and available quantity to availability DTO.
     *
     * @param product           the product entity
     * @param availableQuantity the calculated available quantity
     * @return product availability DTO
     */
    public ProductAvailabilityDto toDto(Product product, Integer availableQuantity) {
        if (product == null) {
            return null;
        }

        int quantity = availableQuantity != null ? availableQuantity : 0;
        boolean inStock = quantity > 0;
        String status = determineStatus(quantity);
        String localizedStatus = getLocalizedStatus(status);
        String localizedMessage = buildLocalizedMessage(quantity, status);

        return ProductAvailabilityDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .inStock(inStock)
                .availableQuantity(config.isShowExactQuantity() ? quantity : null)
                .status(status)
                .localizedStatus(localizedStatus)
                .localizedMessage(localizedMessage)
                .build();
    }

    /**
     * Determines stock status based on available quantity.
     *
     * @param quantity available quantity
     * @return status code
     */
    private String determineStatus(int quantity) {
        if (quantity <= 0) {
            return "OUT_OF_STOCK";
        }
        if (quantity < config.getLowStockThreshold()) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }

    /**
     * Gets localized status string.
     *
     * @param status status code
     * @return localized status
     */
    private String getLocalizedStatus(String status) {
        return messageService.get("stock.status." + status.toLowerCase());
    }

    /**
     * Builds localized availability message.
     *
     * @param quantity available quantity
     * @param status   status code
     * @return localized message
     */
    private String buildLocalizedMessage(int quantity, String status) {
        if (!config.isShowExactQuantity()) {
            return messageService.get("stock.message." + status.toLowerCase());
        }

        return switch (status) {
            case "IN_STOCK" -> messageService.get("stock.available.message", quantity);
            case "LOW_STOCK" -> messageService.get("stock.low.stock.message", quantity);
            default -> messageService.get("stock.out.of.stock.message");
        };
    }
}