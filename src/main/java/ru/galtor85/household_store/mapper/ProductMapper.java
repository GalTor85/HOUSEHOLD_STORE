package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.ProductAttributeDto;
import ru.galtor85.household_store.dto.ProductCreateRequest;
import ru.galtor85.household_store.dto.ProductDto;
import ru.galtor85.household_store.dto.ProductUpdateRequest;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductAttribute;
import ru.galtor85.household_store.service.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final MessageService messageService;
    private final ProductAttributeMapper attributeMapper;

    /**
     * Преобразование запроса на создание в сущность Product
     */
    public Product toEntity(ProductCreateRequest request, Long creatorId) {
        if (request == null) {
            return null;
        }

        return Product.builder()
                .sku(request.getSku())
                .barcode(request.getBarcode())
                .barcodeFormat(request.getBarcodeFormat())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantityInStock(request.getQuantityInStock() != null ? request.getQuantityInStock() : 0)
                .category(request.getCategory())
                .brand(request.getBrand())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(String.valueOf(creatorId))
                .hasVariants(request.getHasVariants() != null ? request.getHasVariants() : false)
                .build();
    }

    /**
     * Обновление сущности из запроса на обновление
     */
    public void updateEntity(Product product, ProductUpdateRequest request) {
        if (product == null || request == null) {
            return;
        }

        if (request.getSku() != null) {
            product.setSku(request.getSku());
        }
        if (request.getBarcode() != null) {
            product.setBarcode(request.getBarcode());
        }
        if (request.getBarcodeFormat() != null) {
            product.setBarcodeFormat(request.getBarcodeFormat());
        }
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getQuantityInStock() != null) {
            product.setQuantityInStock(request.getQuantityInStock());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getHasVariants() != null) {
            product.setHasVariants(request.getHasVariants());
        }
    }

    /**
     * Преобразование сущности в DTO
     */
    public ProductDto toDto(Product product, Locale locale) {
        if (product == null) {
            return null;
        }

        List<ProductAttributeDto> attributeDtos = product.getAttributes() != null ?
                attributeMapper.toDtoList(product.getAttributes(), locale) : new ArrayList<>();

        List<ProductDto> variantDtos = product.getVariants() != null ?
                product.getVariants().stream()
                        .map(v -> toDto(v, locale))
                        .collect(Collectors.toList()) : new ArrayList<>();

        return ProductDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .barcodeFormat(product.getBarcodeFormat())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantityInStock(product.getQuantityInStock())
                .category(product.getCategory())
                .brand(product.getBrand())
                .imageUrl(product.getImageUrl())
                .active(product.isActive())
                .hasVariants(product.getHasVariants())
                .parentProductId(product.getParentProduct() != null ? product.getParentProduct().getId() : null)
                .attributes(attributeDtos)
                .variants(variantDtos)
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().toString() : null)
                .updatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt().toString() : null)
                .createdBy(product.getCreatedBy())
                .build();
    }
}