package ru.galtor85.household_store.processor.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.product.ProductMediaNotFoundException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.repository.product.ProductMediaRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.media.MediaValidator;

import java.util.List;

import static ru.galtor85.household_store.constants.ApiConstants.MEDIA_PATH;

/**
 * Processor for managing main product images.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MainImageProcessor {

    private final ProductMediaRepository mediaRepository;
    private final ProductRepository productRepository;
    private final MediaValidator validator;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Sets a media file as the main image for its product.
     *
     * @param mediaId the media ID
     * @throws ProductMediaNotFoundException if media not found
     */
    @Transactional
    public void setMainImage(Long mediaId) {
        ProductMedia newMainMedia = findMediaById(mediaId);
        validator.validateMediaIsImage(newMainMedia);

        mediaRepository.resetMainImage(newMainMedia.getProductId());
        log.debug(logMsg.get("product.media.service.reset.main", newMainMedia.getProductId()));

        newMainMedia.setIsMain(true);
        mediaRepository.save(newMainMedia);
        log.debug(logMsg.get("product.media.service.set.new.main", mediaId));

        updateProductImageUrl(newMainMedia.getProductId(), mediaId);
    }

    /**
     * Updates product main image from uploaded media list.
     *
     * @param product       the product
     * @param uploadedMedia list of uploaded media DTOs
     */
    @Transactional
    public void updateMainImage(Product product, List<ProductMediaDto> uploadedMedia) {
        uploadedMedia.stream()
                .filter(ProductMediaDto::isMain)
                .findFirst()
                .ifPresent(mainMedia -> {
                    product.setImageUrl(mainMedia.getFileUrl());
                    productRepository.save(product);
                    log.debug(logMsg.get("product.media.service.main.updated",
                            product.getId(), mainMedia.getId()));
                });
    }

    /**
     * Resets the main image for a product.
     *
     * @param productId the product ID
     */
    @Transactional
    public void resetMainImage(Long productId) {
        mediaRepository.findByProductIdAndIsMainTrue(productId)
                .ifPresent(media -> {
                    Product product = productRepository.findById(productId).orElse(null);
                    if (product != null) {
                        product.setImageUrl(null);
                        productRepository.save(product);
                        log.debug(logMsg.get("product.media.service.main.reset", productId));
                    }
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

    private void updateProductImageUrl(Long productId, Long mediaId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("product.media.service.product.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        product.setImageUrl(MEDIA_PATH + mediaId);
        productRepository.save(product);
    }
}