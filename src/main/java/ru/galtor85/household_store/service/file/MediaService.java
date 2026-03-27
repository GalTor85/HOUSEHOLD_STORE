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
import ru.galtor85.household_store.service.i18n.MessageService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final ProductMediaRepository mediaRepository;
    private final ProductMediaMapper mediaMapper;
    private final MessageService messageService;

    /**
     * Получить файл по ID медиа
     */
    @Transactional(readOnly = true)
    public Resource getMediaFile(Long mediaId) {
        ProductMedia media = getMediaEntity(mediaId);

        try {
            Path filePath = Paths.get(media.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error(messageService.get("media.service.log.file.not.readable",
                        media.getFileName(), mediaId));
                throw new FileReadException(
                        messageService.get("media.service.error.file.not.readable", mediaId),
                        null, null
                );
            }

            return resource;

        } catch (Exception e) {
            log.error(messageService.get("media.service.log.file.error", e.getMessage()));
            throw new FileReadException(
                    messageService.get("media.service.error.file.read", e.getMessage()),
                    null, null
            );
        }
    }

    /**
     * Получить информацию о медиа
     */
    @Transactional(readOnly = true)
    public ProductMediaDto getMediaInfo(Long mediaId) {
        ProductMedia media = getMediaEntity(mediaId);
        return mediaMapper.toDto(media);
    }

    /**
     * Получить все медиа продукта
     */
    @Transactional(readOnly = true)
    public List<ProductMediaDto> getProductMedia(Long productId) {
        List<ProductMedia> mediaList = mediaRepository.findByProductIdOrdered(productId);
        return mediaMapper.toDtoList(mediaList);
    }

    /**
     * Получить главное изображение продукта
     */
    @Transactional(readOnly = true)
    public ProductMediaDto getProductMainImage(Long productId) {
        return mediaRepository.findByProductIdAndIsMainTrue(productId)
                .map(mediaMapper::toDto)
                .orElse(null);
    }

    /**
     * Получить имя файла по ID
     */
    @Transactional(readOnly = true)
    public String getMediaFileName(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .map(ProductMedia::getFileName)
                .orElse("unknown");
    }

    /**
     * Получить файл по имени продукта и имени файла
     */
    @Transactional(readOnly = true)
    public Resource getMediaFileByName(Long productId, String fileName) {
        try {
            Path filePath = Paths.get("uploads", String.valueOf(productId), fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error(messageService.get("media.service.log.file.not.found.by.name",
                        fileName, productId));
                throw new FileReadException(
                        messageService.get("media.service.error.file.not.found.by.name", fileName, productId),
                        null, null
                );
            }

            return resource;

        } catch (Exception e) {
            log.error(messageService.get("media.service.log.file.error.by.name",
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
                    log.error(messageService.get("media.service.log.file.not.found", mediaId));
                    return new FileReadException(
                            messageService.get("media.service.error.file.not.found", mediaId),
                            null, null
                    );
                });
    }
}