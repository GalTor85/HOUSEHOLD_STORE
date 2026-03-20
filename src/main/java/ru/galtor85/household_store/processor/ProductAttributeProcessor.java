package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.AttributeCreateRequest;
import ru.galtor85.household_store.dto.AttributeUpdateRequest;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductAttribute;
import ru.galtor85.household_store.mapper.ProductAttributeMapper;
import ru.galtor85.household_store.repository.ProductAttributeRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAttributeProcessor {

    private final ProductAttributeRepository attributeRepository;
    private final ProductAttributeMapper attributeMapper;

    @Transactional
    public void addAttributes(Product product, List<AttributeCreateRequest> attributes) {
        if (attributes != null && !attributes.isEmpty()) {
            List<ProductAttribute> attributeEntities = attributeMapper.toEntityList(attributes, product);
            attributeRepository.saveAll(attributeEntities);
            log.debug("Added {} attributes to product {}", attributes.size(), product.getId());
        }
    }

    @Transactional
    public void updateAttributes(Product product, List<AttributeUpdateRequest> attributes) {
        if (attributes != null) {
            attributeRepository.deleteByProductId(product.getId());
            List<ProductAttribute> attributeEntities = attributeMapper.toEntityList(attributes, product);
            attributeRepository.saveAll(attributeEntities);
            log.debug("Updated {} attributes for product {}", attributes.size(), product.getId());
        }
    }
}