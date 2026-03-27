package ru.galtor85.household_store.processor.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.product.ProductVariantException;
import ru.galtor85.household_store.dto.request.product.ProductCreateRequest;
import ru.galtor85.household_store.dto.response.product.ProductDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductAttribute;
import ru.galtor85.household_store.mapper.product.ProductAttributeMapper;
import ru.galtor85.household_store.mapper.product.ProductMapper;
import ru.galtor85.household_store.repository.product.ProductAttributeRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.product.ProductValidator;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductVariantProcessor {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductMapper productMapper;
    private final ProductAttributeMapper attributeMapper;
    private final ProductValidator validator;
    private final MessageService messageService;

    @Transactional
    public ProductDto createVariant(Long parentProductId, ProductCreateRequest variantRequest,
                                    Long managerId) {

        Product parentProduct = validator.validateProductExists(parentProductId);

        if (!parentProduct.getHasVariants()) {
            parentProduct.setHasVariants(true);
            productRepository.save(parentProduct);
        }

        validator.validateUniqueSku(variantRequest.getSku());

        try {
            Product variant = productMapper.toEntity(variantRequest, managerId);
            variant.setParentProduct(parentProduct);
            variant.setHasVariants(false);

            Product savedVariant = productRepository.save(variant);

            if (variantRequest.getAttributes() != null && !variantRequest.getAttributes().isEmpty()) {
                List<ProductAttribute> attributes = attributeMapper.toEntityList(variantRequest.getAttributes(), savedVariant);
                attributeRepository.saveAll(attributes);
            }

            log.info(messageService.get(
                    "manager.product.variant.created.log",
                    savedVariant.getSku(),
                    savedVariant.getId(),
                    parentProductId
            ));

            return productMapper.toDto(savedVariant);

        } catch (Exception e) {
            log.error(messageService.get(
                    "manager.product.variant.log.error",
                    parentProductId,
                    e.getMessage()
            ));
            throw new ProductVariantException(parentProductId);
        }
    }

    public void createVariants(Product parentProduct, List<ProductCreateRequest> variantRequests,
                               Long managerId) {
        for (ProductCreateRequest variantRequest : variantRequests) {
            createVariant(parentProduct.getId(), variantRequest, managerId);
        }
    }
}