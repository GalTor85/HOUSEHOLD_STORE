package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.FileReadException;
import ru.galtor85.household_store.dto.ProductMediaDto;
import ru.galtor85.household_store.entity.ProductMedia;
import ru.galtor85.household_store.mapper.ProductMediaMapper;
import ru.galtor85.household_store.repository.ProductMediaRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

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
    public Resource getMediaFile(Long mediaId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        ProductMedia media = getMediaEntity(mediaId, locale);

        try {
            Path filePath = Paths.get(media.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error(messageService.get("media.service.log.file.not.readable",
                        media.getFileName(), mediaId, locale));
                throw new FileReadException(
                        messageService.get("media.service.error.file.not.readable", mediaId),
                        null, null
                );
            }

            return resource;

        } catch (Exception e) {
            log.error(messageService.get("media.service.log.file.error", e.getMessage(), locale));
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
    public ProductMediaDto getMediaInfo(Long mediaId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        ProductMedia media = getMediaEntity(mediaId, locale);
        return mediaMapper.toDto(media);
    }

    /**
     * Получить все медиа продукта
     */
    @Transactional(readOnly = true)
    public List<ProductMediaDto> getProductMedia(Long productId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<ProductMedia> mediaList = mediaRepository.findByProductIdOrdered(productId);
        return mediaMapper.toDtoList(mediaList);
    }

    /**
     * Получить главное изображение продукта
     */
    @Transactional(readOnly = true)
    public ProductMediaDto getProductMainImage(Long productId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

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
    public Resource getMediaFileByName(Long productId, String fileName, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        try {
            Path filePath = Paths.get("uploads", String.valueOf(productId), fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error(messageService.get("media.service.log.file.not.found.by.name",
                        fileName, productId, locale));
                throw new FileReadException(
                        messageService.get("media.service.error.file.not.found.by.name", fileName, productId),
                        null, null
                );
            }

            return resource;

        } catch (Exception e) {
            log.error(messageService.get("media.service.log.file.error.by.name",
                    fileName, productId, e.getMessage(), locale));
            throw new FileReadException(
                    messageService.get("media.service.error.file.read.by.name", fileName, productId, e.getMessage()),
                    null, null
            );
        }
    }

    private ProductMedia getMediaEntity(Long mediaId, Locale locale) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> {
                    log.error(messageService.get("media.service.log.file.not.found", mediaId, locale));
                    return new FileReadException(
                            messageService.get("media.service.error.file.not.found", mediaId),
                            null, null
                    );
                });
    }
}