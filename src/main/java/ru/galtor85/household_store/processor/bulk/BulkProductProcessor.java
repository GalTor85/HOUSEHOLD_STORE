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
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.product.ProductValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkProductProcessor {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductValidator validator;
    private final MessageService messageService;

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

        log.info(messageService.get(
                "manager.bulk.price.updated.log",
                updatedProducts.size(),
                reasonText
        ));

        return updatedProducts.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ProductDto> bulkToggleActive(List<Long> productIds, boolean active) {
        List<Product> products = productRepository.findAllById(productIds);
        validator.validateBulkProducts(products, productIds);

        for (Product product : products) {
            product.setActive(active);
        }

        List<Product> updatedProducts = productRepository.saveAll(products);

        checkBulkOperationResult(updatedProducts, productIds);

        log.info(messageService.get(
                active ? "manager.bulk.activated.log" : "manager.bulk.deactivated.log",
                updatedProducts.size()
        ));

        return updatedProducts.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    private void checkBulkOperationResult(List<Product> updatedProducts, List<Long> requestedIds) {
        if (updatedProducts.size() < requestedIds.size()) {
            log.warn(messageService.get(
                    "manager.bulk.log.partial",
                    updatedProducts.size(),
                    requestedIds.size()
            ));
            throw new BulkOperationException(requestedIds, updatedProducts.size());
        }
    }
}