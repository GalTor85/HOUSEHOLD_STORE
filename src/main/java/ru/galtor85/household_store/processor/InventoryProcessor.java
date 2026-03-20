package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.ProductDto;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.mapper.ProductMapper;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.validator.ProductValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryProcessor {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductValidator validator;
    private final MessageService messageService;

    @Transactional
    public ProductDto adjustStock(Product product, int quantity, String reason) {
        validator.validateStockOperation(product, quantity);

        int newQuantity = product.getQuantityInStock() + quantity;
        product.setQuantityInStock(newQuantity);

        Product updatedProduct = productRepository.save(product);

        String reasonText = reason != null ? reason :
                messageService.get("manager.stock.reason.default");

        log.info(messageService.get(
                "manager.stock.adjusted.log",
                product.getId(),
                quantity,
                newQuantity,
                reasonText
        ));

        return productMapper.toDto(updatedProduct);
    }

    @Transactional
    public ProductDto updatePrice(Product product, BigDecimal newPrice, String reason) {
        validator.validatePrice(newPrice);

        BigDecimal oldPrice = product.getPrice();
        product.setPrice(newPrice);

        Product updatedProduct = productRepository.save(product);

        String reasonText = reason != null ? reason :
                messageService.get("manager.price.reason.default");

        log.info(messageService.get(
                "manager.price.updated.log",
                product.getId(),
                oldPrice,
                newPrice,
                reasonText
        ));

        return productMapper.toDto(updatedProduct);
    }

    @Transactional
    public ProductDto toggleActive(Product product, boolean active) {
        product.setActive(active);
        Product updatedProduct = productRepository.save(product);

        log.info(messageService.get(
                active ? "manager.product.activated.log" : "manager.product.deactivated.log",
                product.getId()
        ));

        return productMapper.toDto(updatedProduct);
    }

    public List<ProductDto> getLowStockProducts(int threshold) {
        List<Product> lowStockProducts = productRepository.findByQuantityInStockLessThan(threshold);

        log.debug(messageService.get(
                "manager.low.stock.fetched.log",
                lowStockProducts.size()
        ));

        return lowStockProducts.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }
}