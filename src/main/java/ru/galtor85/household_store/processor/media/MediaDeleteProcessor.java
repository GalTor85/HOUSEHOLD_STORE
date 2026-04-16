package ru.galtor85.household_store.processor.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.repository.product.ProductMediaRepository;
import ru.galtor85.household_store.service.file.FileStorageService;


/**
 * Processor for deleting media files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaDeleteProcessor {

    private final FileStorageService fileStorageService;
    private final ProductMediaRepository mediaRepository;
    private final MainImageProcessor mainImageProcessor;

    /**
     * Deletes media file from disk and database.
     *
     * @param media product media entity to delete
     */
    @Transactional
    public void deleteMedia(ProductMedia media) {
        fileStorageService.deleteFile(media.getFilePath(), media.getProductId());
        mediaRepository.delete(media);

        if (Boolean.TRUE.equals(media.getIsMain())) {
            mainImageProcessor.resetMainImage(media.getProductId());
        }
    }
}