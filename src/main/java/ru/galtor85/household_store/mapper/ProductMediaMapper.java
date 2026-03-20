package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.ProductMediaDto;
import ru.galtor85.household_store.dto.ProductMediaUploadDto;
import ru.galtor85.household_store.entity.MediaType;
import ru.galtor85.household_store.entity.ProductMedia;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMediaMapper {

    /**
     * Преобразование сущности в DTO
     */
    public ProductMediaDto toDto(ProductMedia media) {
        if (media == null) {
            return null;
        }

        return ProductMediaDto.builder()
                .id(media.getId())
                .mediaType(media.getMediaType())
                .fileName(media.getFileName())
                .fileUrl("/api/v1/media/" + media.getId())
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
     * Преобразование списка сущностей в список DTO
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
     * Преобразование Upload DTO в сущность (для создания)
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
     * Создание сущности из параметров (без Upload DTO)
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
     * Обновление сущности из Upload DTO
     */
    public void updateEntity(ProductMedia media, ProductMediaUploadDto uploadDto) {
        if (media == null || uploadDto == null) {
            return;
        }

        if (uploadDto.getAltText() != null) {
            media.setAltText(uploadDto.getAltText());
        }
        if (uploadDto.getCaption() != null) {
            media.setCaption(uploadDto.getCaption());
        }
        if (uploadDto.getSortOrder() != null) {
            media.setSortOrder(uploadDto.getSortOrder());
        }
        if (uploadDto.getIsMain() != null) {
            media.setIsMain(uploadDto.getIsMain());
        }
        if (uploadDto.getMediaType() != null) {
            try {
                media.setMediaType(MediaType.valueOf(uploadDto.getMediaType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid media type: {}", uploadDto.getMediaType());
            }
        }
        if (uploadDto.getWidth() != null) {
            media.setWidth(uploadDto.getWidth());
        }
        if (uploadDto.getHeight() != null) {
            media.setHeight(uploadDto.getHeight());
        }
        if (uploadDto.getDuration() != null) {
            media.setDuration(uploadDto.getDuration());
        }
    }

    /**
     * Определение типа медиа из строки
     */
    private MediaType detectMediaType(String mediaTypeStr) {
        if (mediaTypeStr == null) {
            return MediaType.IMAGE; // по умолчанию
        }
        try {
            return MediaType.valueOf(mediaTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MediaType.IMAGE;
        }
    }
}