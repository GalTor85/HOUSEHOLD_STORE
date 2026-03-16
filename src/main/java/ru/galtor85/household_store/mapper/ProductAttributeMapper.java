package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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

    /**
     * Преобразование DTO в сущность
     */
    public ProductAttribute toEntity(ProductAttributeDto dto, Product product) {
        if (dto == null) {
            return null;
        }

        return ProductAttribute.builder()
                .id(dto.getId())
                .product(product)
                .name(dto.getName())
                .value(dto.getValue())
                .order(dto.getOrder() != null ? dto.getOrder() : 0)
                .required(dto.getRequired())
                .variant(dto.getVariant())
                .build();
    }

    /**
     * Преобразование сущности в DTO
     */
    public ProductAttributeDto toDto(ProductAttribute entity, Locale locale) {
        if (entity == null) {
            return null;
        }

        return ProductAttributeDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .value(entity.getValue())
                .order(entity.getOrder())
                .required(entity.getRequired())
                .variant(entity.getVariant())
                .build();
    }

    /**
     * Преобразование списка сущностей в список DTO
     */
    public List<ProductAttributeDto> toDtoList(List<ProductAttribute> entities, Locale locale) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(e -> toDto(e, locale))
                .collect(Collectors.toList());
    }

    /**
     * Преобразование списка DTO в список сущностей
     */
    public List<ProductAttribute> toEntityList(List<ProductAttributeDto> dtos, Product product) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(d -> toEntity(d, product))
                .collect(Collectors.toList());
    }
}