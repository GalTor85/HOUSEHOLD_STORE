package ru.galtor85.household_store.mapper.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.product.AttributeCreateRequest;
import ru.galtor85.household_store.dto.request.product.AttributeUpdateRequest;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductAttribute;

import java.util.List;
import java.util.Objects;

/**
 * Mapper for product attribute entity to/from DTO.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAttributeMapper {

    private static final int DEFAULT_ORDER = 0;
    private static final boolean DEFAULT_REQUIRED = false;
    private static final boolean DEFAULT_VARIANT = false;

    /**
     * Converts creation request to entity.
     *
     * @param request creation request
     * @param product parent product
     * @return product attribute entity
     */
    public ProductAttribute toEntity(AttributeCreateRequest request, Product product) {
        if (request == null) {
            return null;
        }
        return ProductAttribute.builder()
                .name(request.getName())
                .value(request.getValue())
                .order(request.getOrder() != null ? request.getOrder() : DEFAULT_ORDER)
                .required(request.getRequired() != null ? request.getRequired() : DEFAULT_REQUIRED)
                .variant(request.getVariant() != null ? request.getVariant() : DEFAULT_VARIANT)
                .product(product)
                .build();
    }

    /**
     * Converts update request to entity.
     *
     * @param request update request
     * @param product parent product
     * @return product attribute entity
     */
    public ProductAttribute toEntity(AttributeUpdateRequest request, Product product) {
        if (request == null) {
            return null;
        }
        return ProductAttribute.builder()
                .id(request.getId())
                .name(request.getName())
                .value(request.getValue())
                .order(request.getOrder())
                .required(request.getRequired())
                .variant(request.getVariant())
                .product(product)
                .build();
    }

    /**
     * Converts list of requests to list of entities.
     *
     * @param requests list of create or update requests
     * @param product parent product
     * @return list of product attribute entities
     */
    public List<ProductAttribute> toEntityList(List<?> requests, Product product) {
        if (requests == null) {
            return null;
        }
        return requests.stream()
                .map(req -> mapRequest(req, product))
                .filter(Objects::nonNull)
                .toList();
    }

    private ProductAttribute mapRequest(Object request, Product product) {
        if (request instanceof AttributeCreateRequest createRequest) {
            return toEntity(createRequest, product);
        }
        if (request instanceof AttributeUpdateRequest updateRequest) {
            return toEntity(updateRequest, product);
        }
        return null;
    }
}