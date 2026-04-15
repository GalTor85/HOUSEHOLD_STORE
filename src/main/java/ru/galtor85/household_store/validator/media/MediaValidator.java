package ru.galtor85.household_store.validator.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.product.ProductMediaException;
import ru.galtor85.household_store.advice.exception.product.ProductMediaUploadException;
import ru.galtor85.household_store.entity.product.MediaType;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;

/**
 * Validator for media operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaValidator {

    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Validates file is not empty.
     *
     * @param file uploaded file
     * @param productId product ID
     * @param fileName file name
     * @throws ProductMediaException if file is empty
     */
    public void validateFileNotEmpty(MultipartFile file, Long productId, String fileName) {
        if (file.isEmpty()) {
            log.error(logMsg.get("product.media.service.file.empty", fileName));
            throw new ProductMediaException(
                    messageService.get("product.media.service.error.file.empty", fileName),
                    productId, fileName
            );
        }
    }

    /**
     * Validates media is an image.
     *
     * @param media product media entity
     * @throws ProductMediaException if not an image
     */
    public void validateMediaIsImage(ProductMedia media) {
        if (media.getMediaType() != MediaType.IMAGE) {
            log.error(logMsg.get("product.media.service.not.image", media.getId()));
            throw new ProductMediaException(
                    messageService.get("product.media.service.error.not.image", media.getId()),
                    media.getProductId(), null
            );
        }
    }

    /**
     * Validates upload result has at least one successful file.
     *
     * @param result list of successfully uploaded files
     * @param failedFiles list of failed file names
     * @param productId product ID
     * @throws ProductMediaUploadException if all files failed
     */
    public void validateUploadResult(List<?> result, List<String> failedFiles, Long productId) {
        if (result.isEmpty() && !failedFiles.isEmpty()) {
            log.error(logMsg.get("product.media.service.all.failed",
                    String.join(", ", failedFiles)));
            throw new ProductMediaUploadException(
                    messageService.get("product.media.service.error.all.failed",
                            String.join(", ", failedFiles)),
                    productId, failedFiles
            );
        }
    }
}