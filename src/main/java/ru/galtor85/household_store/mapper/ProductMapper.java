package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.ProductAttributeDto;
import ru.galtor85.household_store.dto.ProductCreateRequest;
import ru.galtor85.household_store.dto.ProductDto;
import ru.galtor85.household_store.dto.ProductUpdateRequest;
import ru.galtor85.household_store.entity.MediaType;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductAttribute;
import ru.galtor85.household_store.entity.ProductMedia;
import ru.galtor85.household_store.repository.ProductMediaRepository;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ProductMediaRepository mediaRepository;
    private final ProductMediaMapper mediaMapper;

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
                // НОВЫЕ ПОЛЯ
                .weightKg(request.getWeightKg())
                .volumeM3(request.getVolumeM3())
                .requiresRefrigeration(request.isRequiresRefrigeration())
                .requiresFreezing(request.isRequiresFreezing())
                .isHazardous(request.isHazardous())
                .isOversize(request.isOversize())
                .isLiquid(request.isLiquid())
                .isPalletized(request.isPalletized())
                .build();
    }

    public void updateEntity(Product product, ProductUpdateRequest request) {
        if (product == null || request == null) {
            return;
        }

        // существующие поля
        if (request.getSku() != null) product.setSku(request.getSku());
        if (request.getBarcode() != null) product.setBarcode(request.getBarcode());
        if (request.getBarcodeFormat() != null) product.setBarcodeFormat(request.getBarcodeFormat());
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getQuantityInStock() != null) product.setQuantityInStock(request.getQuantityInStock());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getHasVariants() != null) product.setHasVariants(request.getHasVariants());

        // НОВЫЕ ПОЛЯ
        if (request.getWeightKg() != null) product.setWeightKg(request.getWeightKg());
        if (request.getVolumeM3() != null) product.setVolumeM3(request.getVolumeM3());
        if (request.getRequiresRefrigeration() != null)
            product.setRequiresRefrigeration(request.getRequiresRefrigeration());
        if (request.getRequiresFreezing() != null)
            product.setRequiresFreezing(request.getRequiresFreezing());
        if (request.getHazardous() != null) product.setIsHazardous(request.getHazardous());
        if (request.getOversize() != null) product.setIsOversize(request.getOversize());
        if (request.getLiquid() != null) product.setIsLiquid(request.getLiquid());
        if (request.getPalletized() != null) product.setIsPalletized(request.getPalletized());
    }

    public ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }

        List<ProductMedia> mediaList = mediaRepository.findByProductIdOrdered(product.getId());

        String mainImageUrl = mediaList.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsMain()))
                .findFirst()
                .map(m -> "/api/v1/media/" + m.getId())
                .orElse(product.getImageUrl());

        List<ProductAttributeDto> attributeDtos = product.getAttributes().stream()
                .map(this::toAttributeDto)
                .collect(Collectors.toList());

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
                .variants(product.getVariants().stream()
                        .map(v -> toDto(v))
                        .collect(Collectors.toList()))
                .media(mediaMapper.toDtoList(mediaList))
                .mainImageUrl(mainImageUrl)
                .images(mediaList.stream()
                        .filter(m -> m.getMediaType() == MediaType.IMAGE)
                        .map(mediaMapper::toDto)
                        .collect(Collectors.toList()))
                .videos(mediaList.stream()
                        .filter(m -> m.getMediaType() == MediaType.VIDEO)
                        .map(mediaMapper::toDto)
                        .collect(Collectors.toList()))
                .supplierId(product.getSupplierId())
                .supplierPrice(product.getSupplierPrice())
                .supplierSku(product.getSupplierSku())
                // НОВЫЕ ПОЛЯ
                .weightKg(product.getWeightKg())
                .volumeM3(product.getVolumeM3())
                .requiresRefrigeration(product.getRequiresRefrigeration())
                .requiresFreezing(product.getRequiresFreezing())
                .isHazardous(product.getIsHazardous())
                .isOversize(product.getIsOversize())
                .isLiquid(product.getIsLiquid())
                .isPalletized(product.getIsPalletized())
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().toString() : null)
                .updatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt().toString() : null)
                .createdBy(product.getCreatedBy())
                .build();
    }

    private ProductAttributeDto toAttributeDto(ProductAttribute attribute) {
        return ProductAttributeDto.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .value(attribute.getValue())
                .order(attribute.getOrder())
                .required(attribute.getRequired())
                .variant(attribute.getVariant())
                .build();
    }
}