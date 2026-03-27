package ru.galtor85.household_store.processor.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.repository.product.ProductMediaRepository;
import ru.galtor85.household_store.service.file.FileStorageService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaDeleteProcessor {

    private final FileStorageService fileStorageService;
    private final ProductMediaRepository mediaRepository;
    private final MainImageProcessor mainImageProcessor;

    @Transactional
    public void deleteMedia(ProductMedia media, Long deletedBy) throws IOException {
        // Удаляем файл с диска
        fileStorageService.deleteFile(media.getFilePath(), media.getProductId());

        // Удаляем запись из БД
        mediaRepository.delete(media);

        // Если это было главное изображение, сбрасываем imageUrl у продукта
        if (Boolean.TRUE.equals(media.getIsMain())) {
            mainImageProcessor.resetMainImage(media.getProductId());
        }
    }
}