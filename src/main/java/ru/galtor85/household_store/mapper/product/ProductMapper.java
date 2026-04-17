package ru.galtor85.household_store.mapper.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.product.ProductCreateRequest;
import ru.galtor85.household_store.dto.request.product.ProductUpdateRequest;
import ru.galtor85.household_store.dto.response.product.ProductAttributeDto;
import ru.galtor85.household_store.dto.response.product.ProductDto;
import ru.galtor85.household_store.entity.product.MediaType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductAttribute;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.repository.product.ProductMediaRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;

import java.util.List;
import java.util.stream.Collectors;

import static ru.galtor85.household_store.constants.ApiConstants.MEDIA_PATH;

/**
 * Mapper for Product entity to/from DTO.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ProductMediaRepository mediaRepository;
    private final ProductMediaMapper mediaMapper;
    private final ProductStockRepository stockRepository;

    /**
     * Converts ProductCreateRequest to Product entity.
     *
     * @param request   the creation request
     * @param creatorId ID of the user creating the product
     * @return Product entity
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
                .category(request.getCategory())
                .brand(request.getBrand())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(String.valueOf(creatorId))
                .hasVariants(request.getHasVariants() != null ? request.getHasVariants() : false)
                .weightKg(request.getWeightKg())
                .volumeM3(request.getVolumeM3())
                .requiresRefrigeration(request.getRequiresRefrigeration())
                .requiresFreezing(request.getRequiresFreezing())
                .isHazardous(request.getHazardous())
                .isOversize(request.getOversize())
                .isLiquid(request.getLiquid())
                .isPalletized(request.getPalletized())
                .build();
    }

    /**
     * Updates an existing Product entity from update request.
     *
     * @param product the existing product entity
     * @param request the update request
     */
    public void updateEntity(Product product, ProductUpdateRequest request) {
        if (product == null || request == null) {
            return;
        }

        if (request.getSku() != null) product.setSku(request.getSku());
        if (request.getBarcode() != null) product.setBarcode(request.getBarcode());
        if (request.getBarcodeFormat() != null) product.setBarcodeFormat(request.getBarcodeFormat());
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getHasVariants() != null) product.setHasVariants(request.getHasVariants());
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

    /**
     * Converts Product entity to DTO.
     *
     * @param product the product entity
     * @return ProductDto
     */
    public ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }

        List<ProductMedia> mediaList = mediaRepository.findByProductIdOrdered(product.getId());

        String mainImageUrl = mediaList.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsMain()))
                .findFirst()
                .map(m -> MEDIA_PATH + m.getId())
                .orElse(product.getImageUrl());

        List<ProductAttributeDto> attributeDtos = product.getAttributes().stream()
                .map(this::toAttributeDto)
                .collect(Collectors.toList());

        // Calculate total stock from ProductStock table
        Integer totalStock = stockRepository.getTotalStockForProduct(product.getId());
        totalStock = totalStock != null ? totalStock : 0;

        return ProductDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .barcodeFormat(product.getBarcodeFormat())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantityInStock(totalStock)
                .category(product.getCategory())
                .brand(product.getBrand())
                .imageUrl(product.getImageUrl())
                .active(product.isActive())
                .hasVariants(product.getHasVariants())
                .parentProductId(product.getParentProduct() != null ? product.getParentProduct().getId() : null)
                .attributes(attributeDtos)
                .variants(product.getVariants().stream()
                        .map(this::toDto)
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

    /**
     * Converts ProductAttribute entity to DTO.
     *
     * @param attribute the product attribute entity
     * @return ProductAttributeDto
     */
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