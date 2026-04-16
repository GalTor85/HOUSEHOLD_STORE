package ru.galtor85.household_store.processor.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.product.ProductVariantException;
import ru.galtor85.household_store.dto.request.product.ProductCreateRequest;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductAttribute;
import ru.galtor85.household_store.mapper.product.ProductAttributeMapper;
import ru.galtor85.household_store.mapper.product.ProductMapper;
import ru.galtor85.household_store.repository.product.ProductAttributeRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.validator.product.ProductValidator;

import java.util.List;

/**
 * Processor for creating product variants.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductVariantProcessor {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductMapper productMapper;
    private final ProductAttributeMapper attributeMapper;
    private final ProductValidator validator;
    private final LogMessageService logMsg;

    /**
     * Creates multiple variants for a parent product.
     *
     * @param parentProduct   the parent product
     * @param variantRequests list of variant creation requests
     * @param managerId       the manager ID
     */
    @Transactional
    public void createVariants(Product parentProduct, List<ProductCreateRequest> variantRequests,
                               Long managerId) {
        for (ProductCreateRequest variantRequest : variantRequests) {
            createVariantInternal(parentProduct.getId(), variantRequest, managerId);
        }
    }

    private void createVariantInternal(Long parentProductId, ProductCreateRequest variantRequest,
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
                List<ProductAttribute> attributes = attributeMapper.toEntityList(
                        variantRequest.getAttributes(), savedVariant);
                attributeRepository.saveAll(attributes);
            }

            log.info(logMsg.get(
                    "manager.product.variant.created.log",
                    savedVariant.getSku(),
                    savedVariant.getId(),
                    parentProductId
            ));

            productMapper.toDto(savedVariant);

        } catch (Exception e) {
            log.error(logMsg.get(
                    "manager.product.variant.log.error",
                    parentProductId,
                    e.getMessage()
            ));
            throw new ProductVariantException(parentProductId);
        }
    }
}