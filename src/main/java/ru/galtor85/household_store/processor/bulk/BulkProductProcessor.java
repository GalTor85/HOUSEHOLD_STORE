package ru.galtor85.household_store.processor.bulk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.stock.BulkOperationException;
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
 * Processor for bulk product operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BulkProductProcessor {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductValidator validator;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Updates prices for multiple products in bulk.
     *
     * @param productIds list of product IDs to update
     * @param newPrice   the new price for all specified products
     * @param reason     the reason for the bulk price update
     * @return list of updated product DTOs
     * @throws BulkOperationException if some products were not found
     */
    @Transactional
    public List<ProductDto> bulkUpdatePrices(List<Long> productIds, BigDecimal newPrice,
                                             String reason) {
        validator.validatePrice(newPrice);

        List<Product> products = productRepository.findAllById(productIds);
        validator.validateBulkProducts(products, productIds);

        for (Product product : products) {
            product.setPrice(newPrice);
        }

        List<Product> updatedProducts = productRepository.saveAll(products);

        checkBulkOperationResult(updatedProducts, productIds);

        String reasonText = reason != null ? reason :
                messageService.get("manager.price.reason.default");

        log.info(logMsg.get(
                "manager.bulk.price.updated.log",
                updatedProducts.size(),
                reasonText
        ));

        return updatedProducts.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Toggles active status for multiple products in bulk.
     *
     * @param productIds list of product IDs to update
     * @param active     the new active status for all specified products
     * @return list of updated product DTOs
     * @throws BulkOperationException if some products were not found
     */
    @Transactional
    public List<ProductDto> bulkToggleActive(List<Long> productIds, boolean active) {
        List<Product> products = productRepository.findAllById(productIds);
        validator.validateBulkProducts(products, productIds);

        for (Product product : products) {
            product.setActive(active);
        }

        List<Product> updatedProducts = productRepository.saveAll(products);

        checkBulkOperationResult(updatedProducts, productIds);

        log.info(logMsg.get(
                active ? "manager.bulk.activated.log" : "manager.bulk.deactivated.log",
                updatedProducts.size()
        ));

        return updatedProducts.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Checks the result of bulk operation and throws exception if partial.
     *
     * @param updatedProducts list of successfully updated products
     * @param requestedIds    list of originally requested product IDs
     * @throws BulkOperationException if not all products were updated
     */
    private void checkBulkOperationResult(List<Product> updatedProducts, List<Long> requestedIds) {
        if (updatedProducts.size() < requestedIds.size()) {
            log.warn(logMsg.get(
                    "manager.bulk.log.partial",
                    updatedProducts.size(),
                    requestedIds.size()
            ));
            throw new BulkOperationException(requestedIds, updatedProducts.size());
        }
    }
}