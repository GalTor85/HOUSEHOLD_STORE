package ru.galtor85.household_store.builder.media;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.user.SavedFileInfo;
import ru.galtor85.household_store.entity.product.MediaType;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.mapper.product.ProductMediaMapper;

/**
 * Factory for creating product media entities and DTOs.
 */
@Component
@RequiredArgsConstructor
public class ProductMediaFactory {

    private final ProductMediaMapper mediaMapper;

    /**
     * Creates ProductMedia entity from parameters.
     *
     * @param productId product ID
     * @param uploadedBy user ID who uploaded
     * @param mediaType media type
     * @param storedFileName stored file name
     * @param filePath file path
     * @param fileSize file size in bytes
     * @param mimeType MIME type
     * @param sortOrder sort order
     * @param isMain is main image flag
     * @return ProductMedia entity
     */
    public ProductMedia createProductMedia(Long productId, Long uploadedBy,
                                           MediaType mediaType, String storedFileName,
                                           String filePath, Long fileSize,
                                           String mimeType, Integer sortOrder,
                                           Boolean isMain) {
        return mediaMapper.createEntity(
                productId, uploadedBy, mediaType, storedFileName,
                filePath, fileSize, mimeType, sortOrder, isMain
        );
    }

    /**
     * Creates SavedFileInfo from ProductMedia.
     *
     * @param media product media entity
     * @param originalFileName original file name
     * @return SavedFileInfo DTO
     */
    public SavedFileInfo createSavedFileInfo(ProductMedia media, String originalFileName) {
        return SavedFileInfo.builder()
                .storedFileName(media.getFileName())
                .originalFileName(originalFileName)
                .filePath(media.getFilePath())
                .fileSize(media.getFileSize())
                .build();
    }
}