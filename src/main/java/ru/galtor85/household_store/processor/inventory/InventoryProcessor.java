package ru.galtor85.household_store.processor.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.dto.response.product.ProductDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.mapper.product.ProductMapper;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.product.ProductValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Processor for inventory operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryProcessor {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductValidator validator;
    private final MessageService messageService;
    private final BusinessConfig businessConfig;
    private final LogMessageService logMsg;

    /**
     * Adjusts the stock quantity of a product.
     *
     * @param product  the product to adjust
     * @param quantity the amount to adjust (positive or negative)
     * @param reason   the reason for adjustment
     * @return updated ProductDto
     */
    @Transactional
    public ProductDto adjustStock(Product product, int quantity, String reason) {
        validator.validateStockOperation(product, quantity);

        int newQuantity = product.getQuantityInStock() + quantity;
        product.setQuantityInStock(newQuantity);

        Product updatedProduct = productRepository.save(product);

        String reasonText = reason != null ? reason :
                messageService.get("manager.stock.reason.default");

        log.info(logMsg.get(
                "manager.stock.adjusted.log",
                product.getId(),
                quantity,
                newQuantity,
                reasonText
        ));

        return productMapper.toDto(updatedProduct);
    }

    /**
     * Updates the price of a product.
     *
     * @param product  the product to update
     * @param newPrice the new price
     * @param reason   the reason for price change
     * @return updated ProductDto
     */
    @Transactional
    public ProductDto updatePrice(Product product, BigDecimal newPrice, String reason) {
        validator.validatePrice(newPrice);

        BigDecimal oldPrice = product.getPrice();
        product.setPrice(newPrice);

        Product updatedProduct = productRepository.save(product);

        String reasonText = reason != null ? reason :
                messageService.get("manager.price.reason.default");

        log.info(logMsg.get(
                "manager.price.updated.log",
                product.getId(),
                oldPrice,
                newPrice,
                reasonText
        ));

        return productMapper.toDto(updatedProduct);
    }

    /**
     * Toggles the active status of a product.
     *
     * @param product the product to update
     * @param active  the new active status
     * @return updated ProductDto
     */
    @Transactional
    public ProductDto toggleActive(Product product, boolean active) {
        product.setActive(active);
        Product updatedProduct = productRepository.save(product);

        log.info(logMsg.get(
                active ? "manager.product.activated.log" : "manager.product.deactivated.log",
                product.getId()
        ));

        return productMapper.toDto(updatedProduct);
    }

    /**
     * Gets products with low stock levels.
     *
     * @param threshold the stock threshold (uses config default if null)
     * @return list of ProductDto with low stock
     */
    public List<ProductDto> getLowStockProducts(Integer threshold) {
        int effectiveThreshold = threshold != null && threshold > 0 ? threshold :
                businessConfig.getStock().getLowStockThreshold();

        List<Product> lowStockProducts = productRepository.findByQuantityInStockLessThan(effectiveThreshold);

        log.debug(logMsg.get(
                "manager.low.stock.fetched.log",
                lowStockProducts.size()
        ));

        return lowStockProducts.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }
}