package ru.galtor85.household_store.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.file.FileReadException;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.mapper.product.ProductMediaMapper;
import ru.galtor85.household_store.repository.product.ProductMediaRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.UNKNOWN_FILE_NAME;

/**
 * Service for serving media files and metadata.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private static final String UPLOAD_DIR = "uploads";

    private final ProductMediaRepository mediaRepository;
    private final ProductMediaMapper mediaMapper;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Gets media file by ID.
     *
     * @param mediaId media ID
     * @return file resource
     */
    @Transactional(readOnly = true)
    public Resource getMediaFile(Long mediaId) {
        ProductMedia media = getMediaEntity(mediaId);

        try {
            Path filePath = Paths.get(media.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error(logMsg.get("media.service.log.file.not.readable",
                        media.getFileName(), mediaId));
                throw new FileReadException(
                        messageService.get("media.service.error.file.not.readable", media.getFileName(), mediaId),
                        null, null
                );
            }

            return resource;

        } catch (FileReadException e) {
            throw e;
        } catch (Exception e) {
            log.error(logMsg.get("media.service.log.file.error", e.getMessage()));
            throw new FileReadException(
                    messageService.get("media.service.error.file.read", e.getMessage()),
                    null, null
            );
        }
    }

    /**
     * Gets media metadata by ID.
     *
     * @param mediaId media ID
     * @return media DTO
     */
    @Transactional(readOnly = true)
    public ProductMediaDto getMediaInfo(Long mediaId) {
        ProductMedia media = getMediaEntity(mediaId);
        return mediaMapper.toDto(media);
    }

    /**
     * Gets all media for a product.
     *
     * @param productId product ID
     * @return list of media DTOs
     */
    @Transactional(readOnly = true)
    public List<ProductMediaDto> getProductMedia(Long productId) {
        List<ProductMedia> mediaList = mediaRepository.findByProductIdOrdered(productId);
        return mediaMapper.toDtoList(mediaList);
    }

    /**
     * Gets main image for a product.
     *
     * @param productId product ID
     * @return main image DTO or null if not found
     */
    @Transactional(readOnly = true)
    public ProductMediaDto getProductMainImage(Long productId) {
        return mediaRepository.findByProductIdAndIsMainTrue(productId)
                .map(mediaMapper::toDto)
                .orElse(null);
    }

    /**
     * Gets media file name by ID.
     *
     * @param mediaId media ID
     * @return file name
     */
    @Transactional(readOnly = true)
    public String getMediaFileName(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .map(ProductMedia::getFileName)
                .orElse(UNKNOWN_FILE_NAME);
    }

    /**
     * Gets media file by product ID and file name.
     *
     * @param productId product ID
     * @param fileName file name
     * @return file resource
     */
    @Transactional(readOnly = true)
    public Resource getMediaFileByName(Long productId, String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR, String.valueOf(productId), fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error(logMsg.get("media.service.log.file.not.found.by.name",
                        fileName, productId));
                throw new FileReadException(
                        messageService.get("media.service.error.file.not.found.by.name", fileName, productId),
                        null, null
                );
            }

            return resource;

        } catch (FileReadException e) {
            throw e;
        } catch (Exception e) {
            log.error(logMsg.get("media.service.log.file.error.by.name",
                    fileName, productId, e.getMessage()));
            throw new FileReadException(
                    messageService.get("media.service.error.file.read.by.name", fileName, productId, e.getMessage()),
                    null, null
            );
        }
    }

    private ProductMedia getMediaEntity(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("media.service.log.file.not.found", mediaId));
                    return new FileReadException(
                            messageService.get("media.service.error.file.not.found", mediaId),
                            null, null
                    );
                });
    }
}