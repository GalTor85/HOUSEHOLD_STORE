package ru.galtor85.household_store.mapper.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.common.ProductMediaUploadDto;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.entity.product.MediaType;
import ru.galtor85.household_store.entity.product.ProductMedia;

import java.util.List;
import java.util.stream.Collectors;

import static ru.galtor85.household_store.constants.ApiConstants.MEDIA_PATH;

/**
 * Mapper for ProductMedia entity to/from DTO.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMediaMapper {

    /**
     * Converts ProductMedia entity to DTO.
     *
     * @param media the product media entity
     * @return ProductMediaDto
     */
    public ProductMediaDto toDto(ProductMedia media) {
        if (media == null) {
            return null;
        }

        return ProductMediaDto.builder()
                .id(media.getId())
                .mediaType(media.getMediaType())
                .fileName(media.getFileName())
                .fileUrl(MEDIA_PATH + media.getId())
                .fileSize(media.getFileSize())
                .mimeType(media.getMimeType())
                .altText(media.getAltText())
                .caption(media.getCaption())
                .sortOrder(media.getSortOrder())
                .isMain(media.getIsMain())
                .width(media.getWidth())
                .height(media.getHeight())
                .duration(media.getDuration())
                .build();
    }

    /**
     * Converts a list of ProductMedia entities to DTOs.
     *
     * @param mediaList list of product media entities
     * @return list of ProductMediaDto
     */
    public List<ProductMediaDto> toDtoList(List<ProductMedia> mediaList) {
        if (mediaList == null) {
            return null;
        }
        return mediaList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Converts ProductMediaUploadDto to ProductMedia entity.
     *
     * @param uploadDto     the upload DTO
     * @param productId     the product ID
     * @param uploadedBy    ID of the user uploading the file
     * @param storedFileName stored file name on disk
     * @param filePath      full path to the file
     * @param fileSize      file size in bytes
     * @return ProductMedia entity
     */
    public ProductMedia toEntity(ProductMediaUploadDto uploadDto, Long productId, Long uploadedBy,
                                 String storedFileName, String filePath, Long fileSize) {
        if (uploadDto == null) {
            return null;
        }

        return ProductMedia.builder()
                .productId(productId)
                .uploadedBy(uploadedBy)
                .mediaType(detectMediaType(uploadDto.getMediaType()))
                .fileName(storedFileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .mimeType(uploadDto.getFile() != null ? uploadDto.getFile().getContentType() : null)
                .altText(uploadDto.getAltText())
                .caption(uploadDto.getCaption())
                .sortOrder(uploadDto.getSortOrder() != null ? uploadDto.getSortOrder() : 0)
                .isMain(uploadDto.getIsMain() != null ? uploadDto.getIsMain() : false)
                .width(uploadDto.getWidth())
                .height(uploadDto.getHeight())
                .duration(uploadDto.getDuration())
                .build();
    }

    /**
     * Creates a ProductMedia entity from parameters.
     *
     * @param productId     the product ID
     * @param uploadedBy    ID of the user uploading the file
     * @param mediaType     media type
     * @param storedFileName stored file name on disk
     * @param filePath      full path to the file
     * @param fileSize      file size in bytes
     * @param mimeType      MIME type
     * @param sortOrder     sort order
     * @param isMain        is main image flag
     * @return ProductMedia entity
     */
    public ProductMedia createEntity(Long productId, Long uploadedBy, MediaType mediaType,
                                     String storedFileName, String filePath, Long fileSize,
                                     String mimeType, Integer sortOrder, Boolean isMain) {
        return ProductMedia.builder()
                .productId(productId)
                .uploadedBy(uploadedBy)
                .mediaType(mediaType)
                .fileName(storedFileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isMain(isMain != null ? isMain : false)
                .build();
    }

    /**
     * Detects media type from string.
     *
     * @param mediaTypeStr media type as string
     * @return MediaType enum value, defaults to IMAGE
     */
    private MediaType detectMediaType(String mediaTypeStr) {
        if (mediaTypeStr == null) {
            return MediaType.IMAGE;
        }
        try {
            return MediaType.valueOf(mediaTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MediaType.IMAGE;
        }
    }
}