package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.AttributeCreateRequest;
import ru.galtor85.household_store.dto.AttributeUpdateRequest;
import ru.galtor85.household_store.dto.ProductAttributeDto;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductAttribute;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAttributeMapper {

    private final MessageService messageService;

    public ProductAttribute toEntity(AttributeCreateRequest request, Product product) {
        if (request == null) {
            return null;
        }

        return ProductAttribute.builder()
                .name(request.getName())
                .value(request.getValue())
                .order(request.getOrder() != null ? request.getOrder() : 0)
                .required(request.getRequired() != null ? request.getRequired() : false)
                .variant(request.getVariant() != null ? request.getVariant() : false)
                .product(product)
                .build();
    }

    public ProductAttribute toEntity(AttributeUpdateRequest request, Product product) {
        if (request == null) {
            return null;
        }

        return ProductAttribute.builder()
                .id(request.getId())  // ID нужен для обновления
                .name(request.getName())
                .value(request.getValue())
                .order(request.getOrder())
                .required(request.getRequired())
                .variant(request.getVariant())
                .product(product)
                .build();
    }

    public void updateEntity(ProductAttribute attribute, AttributeUpdateRequest request) {
        if (attribute == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            attribute.setName(request.getName());
        }
        if (request.getValue() != null) {
            attribute.setValue(request.getValue());
        }
        if (request.getOrder() != null) {
            attribute.setOrder(request.getOrder());
        }
        if (request.getRequired() != null) {
            attribute.setRequired(request.getRequired());
        }
        if (request.getVariant() != null) {
            attribute.setVariant(request.getVariant());
        }
    }

    public List<ProductAttribute> toEntityList(List<?> requests, Product product) {
        if (requests == null) {
            return null;
        }

        return requests.stream()
                .map(req -> {
                    if (req instanceof AttributeCreateRequest) {
                        return toEntity((AttributeCreateRequest) req, product);
                    } else if (req instanceof AttributeUpdateRequest) {
                        return toEntity((AttributeUpdateRequest) req, product);
                    }
                    return null;
                })
                .collect(Collectors.toList());
    }

    public ProductAttributeDto toDto(ProductAttribute attribute, Locale locale) {
        if (attribute == null) {
            return null;
        }

        return ProductAttributeDto.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .value(attribute.getValue())
                .order(attribute.getOrder())
                .required(attribute.getRequired())
                .variant(attribute.getVariant())
                .build();
    }

    public List<ProductAttributeDto> toDtoList(List<ProductAttribute> attributes, Locale locale) {
        if (attributes == null) {
            return null;
        }

        return attributes.stream()
                .map(attr -> toDto(attr, locale))
                .collect(Collectors.toList());
    }
}