package ru.galtor85.household_store.service.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.product.ProductMediaNotFoundException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.dto.common.ProductMediaUploadDto;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.processor.media.MainImageProcessor;
import ru.galtor85.household_store.processor.media.MediaDeleteProcessor;
import ru.galtor85.household_store.processor.media.MediaUploadProcessor;
import ru.galtor85.household_store.repository.product.ProductMediaRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.json.MediaMetadataParser;

import java.util.List;

/**
 * Service for managing product media.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMediaService {

    private final ProductMediaRepository mediaRepository;
    private final ProductRepository productRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final MediaUploadProcessor uploadProcessor;
    private final MediaDeleteProcessor deleteProcessor;
    private final MainImageProcessor mainImageProcessor;
    private final MediaMetadataParser metadataParser;

    /**
     * Uploads media files for a product.
     *
     * @param productId product ID
     * @param files media files to upload
     * @param metadataJson JSON metadata for files
     * @param uploadedBy user ID who uploaded
     * @return list of uploaded media DTOs
     */
    @Transactional
    public List<ProductMediaDto> uploadMedia(Long productId, List<MultipartFile> files,
                                             String metadataJson, Long uploadedBy) {
        log.info(logMsg.get("product.media.service.upload.start", productId, files.size()));

        Product product = findProductById(productId);
        List<ProductMediaUploadDto> metadataList = metadataParser.parseMetadata(metadataJson);
        MediaUploadProcessor.UploadResult uploadResult = uploadProcessor.processUpload(
                productId, files, metadataList, uploadedBy);

        mainImageProcessor.updateMainImage(product, uploadResult.successful());

        log.info(logMsg.get("product.media.service.upload.complete",
                uploadResult.successful().size(), productId, uploadedBy));

        return uploadResult.successful();
    }

    /**
     * Deletes a media file.
     *
     * @param mediaId media ID
     * @param deletedBy user ID who deleted
     */
    @Transactional
    public void deleteMedia(Long mediaId, Long deletedBy) {
        log.info(logMsg.get("product.media.service.delete.start", mediaId, deletedBy));

        ProductMedia media = findMediaById(mediaId);

        deleteProcessor.deleteMedia(media);
        log.info(logMsg.get("product.media.service.delete.success", mediaId, deletedBy));
    }

    /**
     * Sets media as main product image.
     *
     * @param mediaId media ID
     * @param setBy user ID who set
     */
    @Transactional
    public void setMainMedia(Long mediaId, Long setBy) {
        log.info(logMsg.get("product.media.service.setmain.start", mediaId, setBy));

        try {
            mainImageProcessor.setMainImage(mediaId);
            log.info(logMsg.get("product.media.service.setmain.success", mediaId, setBy));
        } catch (Exception e) {
            log.error(logMsg.get("product.media.service.setmain.error", mediaId, e.getMessage()), e);
            throw e;
        }
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("product.media.service.product.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    private ProductMedia findMediaById(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("product.media.service.media.not.found", mediaId));
                    return new ProductMediaNotFoundException(
                            messageService.get("product.media.service.error.not.found", mediaId),
                            mediaId
                    );
                });
    }
}