package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.ProductMediaException;
import ru.galtor85.household_store.advice.exception.ProductMediaNotFoundException;
import ru.galtor85.household_store.advice.exception.ProductNotFoundException;
import ru.galtor85.household_store.dto.ProductMediaDto;
import ru.galtor85.household_store.dto.ProductMediaUploadDto;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductMedia;
import ru.galtor85.household_store.mapper.ProductMediaMapper;
import ru.galtor85.household_store.processor.MainImageProcessor;
import ru.galtor85.household_store.processor.MediaDeleteProcessor;
import ru.galtor85.household_store.processor.MediaUploadProcessor;
import ru.galtor85.household_store.repository.ProductMediaRepository;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.util.MediaMetadataParser;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMediaService {

    private final ProductMediaRepository mediaRepository;
    private final ProductRepository productRepository;
    private final ProductMediaMapper mediaMapper;
    private final MessageService messageService;

    // Процессоры
    private final MediaUploadProcessor uploadProcessor;
    private final MediaDeleteProcessor deleteProcessor;
    private final MainImageProcessor mainImageProcessor;

    // Утилиты
    private final MediaMetadataParser metadataParser;

    // ========== UPLOAD MEDIA ==========

    @Transactional
    public List<ProductMediaDto> uploadMedia(Long productId, List<MultipartFile> files,
                                             String metadataJson, Long uploadedBy) {
        log.info(messageService.get("product.media.service.upload.start", productId, files.size()));

        // Проверяем существование продукта
        Product product = findProductById(productId);

        // Парсим метаданные
        List<ProductMediaUploadDto> metadataList = metadataParser.parseMetadata(metadataJson);

        // Загружаем файлы
        MediaUploadProcessor.UploadResult uploadResult = uploadProcessor.processUpload(
                productId, files, metadataList, uploadedBy);

        // Обновляем главное изображение
        mainImageProcessor.updateMainImage(product, uploadResult.getSuccessful());

        log.info(messageService.get("product.media.service.upload.complete",
                uploadResult.getSuccessful().size(), productId, uploadedBy));

        return uploadResult.getSuccessful();
    }

    // ========== DELETE MEDIA ==========

    @Transactional
    public void deleteMedia(Long mediaId, Long deletedBy) {
        log.info(messageService.get("product.media.service.delete.start", mediaId, deletedBy));

        ProductMedia media = findMediaById(mediaId);

        try {
            deleteProcessor.deleteMedia(media, deletedBy);
            log.info(messageService.get("product.media.service.delete.success", mediaId, deletedBy));
        } catch (IOException e) {
            log.error(messageService.get("product.media.service.delete.error", mediaId, e.getMessage()), e);
            throw new ProductMediaException(
                    messageService.get("product.media.service.error.delete", mediaId, e.getMessage()),
                    e, media.getProductId(), null
            );
        }
    }

    // ========== SET MAIN MEDIA ==========

    @Transactional
    public void setMainMedia(Long mediaId, Long setBy) {
        log.info(messageService.get("product.media.service.setmain.start", mediaId, setBy));

        try {
            mainImageProcessor.setMainImage(mediaId, setBy);
            log.info(messageService.get("product.media.service.setmain.success", mediaId, setBy));
        } catch (Exception e) {
            log.error(messageService.get("product.media.service.setmain.error", mediaId, e.getMessage()), e);
            throw e;
        }
    }

    // ========== GET PRODUCT MEDIA ==========

    @Transactional(readOnly = true)
    public List<ProductMediaDto> getProductMedia(Long productId) {
        log.debug(messageService.get("product.media.service.getmedia.start", productId));

        List<ProductMedia> mediaList = mediaRepository.findByProductIdOrdered(productId);

        log.debug(messageService.get("product.media.service.getmedia.found", productId, mediaList.size()));

        return mediaMapper.toDtoList(mediaList);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("product.media.service.product.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    private ProductMedia findMediaById(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> {
                    log.error(messageService.get("product.media.service.media.not.found", mediaId));
                    return new ProductMediaNotFoundException(
                            messageService.get("product.media.service.error.not.found", mediaId),
                            mediaId
                    );
                });
    }
}