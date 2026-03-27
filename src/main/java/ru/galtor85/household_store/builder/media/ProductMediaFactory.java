package ru.galtor85.household_store.builder.media;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.user.SavedFileInfo;
import ru.galtor85.household_store.entity.product.MediaType;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.mapper.product.ProductMediaMapper;

@Component
@RequiredArgsConstructor
public class ProductMediaFactory {

    private final ProductMediaMapper mediaMapper;

    /**
     * Создание ProductMedia из параметров
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
     * Создание SavedFileInfo из ProductMedia
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