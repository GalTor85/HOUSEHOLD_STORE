package ru.galtor85.household_store.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.ProductMediaDto;
import ru.galtor85.household_store.dto.ProductMediaUploadDto;
import ru.galtor85.household_store.dto.SavedFileInfo;
import ru.galtor85.household_store.entity.MediaType;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductMedia;
import ru.galtor85.household_store.mapper.ProductMediaMapper;
import ru.galtor85.household_store.repository.ProductMediaRepository;
import ru.galtor85.household_store.repository.ProductRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMediaService {

    private final ProductMediaRepository mediaRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final ProductMediaMapper mediaMapper;
    private final MessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Загрузка медиафайлов для продукта
     */
    @Transactional
    public List<ProductMediaDto> uploadMedia(Long productId, List<MultipartFile> files,
                                             String metadataJson, Long uploadedBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.info(messageService.get("product.media.service.upload.start", productId, files.size(), locale));

        // Проверяем существование продукта
        Locale finalLocale = locale;
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("product.media.service.product.not.found", productId, finalLocale));
                    return new ProductNotFoundException(productId);
                });

        List<ProductMediaDto> result = new ArrayList<>();
        List<ProductMediaUploadDto> metadataList = parseMetadata(metadataJson, locale);
        List<String> failedFiles = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            ProductMediaUploadDto metadata = (metadataList != null && i < metadataList.size())
                    ? metadataList.get(i)
                    : new ProductMediaUploadDto();

            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

            try {
                log.debug(messageService.get("product.media.service.processing.file", i, fileName, locale));

                // Проверка файла
                validateFile(file, productId, fileName, locale);

                // Сохраняем файл и получаем информацию
                SavedFileInfo fileInfo = fileStorageService.storeFileAndGetInfo(file, productId, uploadedBy, locale);

                // Создаем сущность через маппер
                ProductMedia media = mediaMapper.toEntity(
                        metadata,
                        productId,
                        uploadedBy,
                        fileInfo.getStoredFileName(),
                        fileInfo.getFilePath(),
                        fileInfo.getFileSize()
                );

                ProductMedia savedMedia = mediaRepository.save(media);
                result.add(mediaMapper.toDto(savedMedia));

                log.debug(messageService.get("product.media.service.file.saved", fileName, savedMedia.getId(), locale));

            } catch (IOException e) {
                log.error(messageService.get("product.media.service.file.error", fileName, e.getMessage(), locale), e);
                failedFiles.add(fileName);

                if (files.size() == 1) {
                    throw new ProductMediaException(
                            messageService.get("product.media.service.error.single", fileName, e.getMessage(), locale),
                            e, productId, fileName
                    );
                }
            } catch (ProductMediaException e) {
                failedFiles.add(fileName);
                log.error(messageService.get("product.media.service.file.failed", fileName, e.getMessage(), locale));
            }
        }

        // Проверяем результат загрузки
        checkUploadResult(result, failedFiles, files.size(), productId, locale);

        // Обновляем главное изображение продукта, если нужно
        updateMainImage(product, result, locale);

        log.info(messageService.get("product.media.service.upload.complete",
                result.size(), productId, uploadedBy, locale));

        return result;
    }

    /**
     * Удаление медиафайла
     */
    @Transactional
    public void deleteMedia(Long mediaId, Long deletedBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.info(messageService.get("product.media.service.delete.start", mediaId, deletedBy, locale));

        Locale finalLocale = locale;
        ProductMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> {
                    log.error(messageService.get("product.media.service.media.not.found", mediaId, finalLocale));
                    return new ProductMediaNotFoundException(
                            messageService.get("product.media.service.error.not.found", mediaId, finalLocale),
                            mediaId
                    );
                });

        try {
            // Удаляем файл с диска
            fileStorageService.deleteFile(media.getFilePath(), media.getProductId(), locale);

            // Удаляем запись из БД
            mediaRepository.delete(media);

            // Если это было главное изображение, сбрасываем imageUrl у продукта
            if (Boolean.TRUE.equals(media.getIsMain())) {
                resetMainImage(media.getProductId(), locale);
            }

            log.info(messageService.get("product.media.service.delete.success", mediaId, deletedBy, locale));

        } catch (IOException e) {
            log.error(messageService.get("product.media.service.delete.error", mediaId, e.getMessage(), locale), e);
            throw new ProductMediaException(
                    messageService.get("product.media.service.error.delete", mediaId, e.getMessage(), locale),
                    e, media.getProductId(), null
            );
        }
    }

    /**
     * Установка главного изображения
     */
    @Transactional
    public void setMainMedia(Long mediaId, Long setBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.info(messageService.get("product.media.service.setmain.start", mediaId, setBy, locale));

        Locale finalLocale = locale;
        ProductMedia newMainMedia = mediaRepository.findById(mediaId)
                .orElseThrow(() -> {
                    log.error(messageService.get("product.media.service.media.not.found", mediaId, finalLocale));
                    return new ProductMediaNotFoundException(
                            messageService.get("product.media.service.error.not.found", mediaId, finalLocale),
                            mediaId
                    );
                });

        // Проверяем, что это изображение
        if (newMainMedia.getMediaType() != MediaType.IMAGE) {
            log.error(messageService.get("product.media.service.not.image", mediaId, newMainMedia.getMediaType(), locale));
            throw new ProductMediaException(
                    messageService.get("product.media.service.error.not.image", mediaId, locale),
                    newMainMedia.getProductId(), null
            );
        }

        try {
            // Сбрасываем флаг main у всех медиа этого продукта
            mediaRepository.resetMainImage(newMainMedia.getProductId());
            log.debug(messageService.get("product.media.service.reset.main", newMainMedia.getProductId(), locale));

            // Устанавливаем новый main
            newMainMedia.setIsMain(true);
            mediaRepository.save(newMainMedia);
            log.debug(messageService.get("product.media.service.set.new.main", mediaId, locale));

            // Обновляем imageUrl в продукте
            Locale finalLocale1 = locale;
            Product product = productRepository.findById(newMainMedia.getProductId())
                    .orElseThrow(() -> {
                        log.error(messageService.get("product.media.service.product.not.found",
                                newMainMedia.getProductId(), finalLocale1));
                        return new ProductNotFoundException(newMainMedia.getProductId());
                    });

            product.setImageUrl("/api/v1/media/" + mediaId);
            productRepository.save(product);

            log.info(messageService.get("product.media.service.setmain.success", mediaId, setBy, locale));

        } catch (Exception e) {
            log.error(messageService.get("product.media.service.setmain.error", mediaId, e.getMessage(), locale), e);
            throw new ProductMediaException(
                    messageService.get("product.media.service.error.setmain", mediaId, e.getMessage(), locale),
                    e, newMainMedia.getProductId(), null
            );
        }
    }

    /**
     * Получение всех медиа продукта
     */
    @Transactional(readOnly = true)
    public List<ProductMediaDto> getProductMedia(Long productId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("product.media.service.getmedia.start", productId, locale));

        List<ProductMedia> mediaList = mediaRepository.findByProductIdOrdered(productId);

        log.debug(messageService.get("product.media.service.getmedia.found", productId, mediaList.size(), locale));

        return mediaMapper.toDtoList(mediaList);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private List<ProductMediaUploadDto> parseMetadata(String metadataJson, Locale locale) {
        if (metadataJson == null || metadataJson.trim().isEmpty()) {
            return null;
        }
        try {
            log.debug(messageService.get("product.media.service.parse.metadata", metadataJson, locale));
            return objectMapper.readValue(metadataJson,
                    new TypeReference<List<ProductMediaUploadDto>>() {});
        } catch (Exception e) {
            log.warn(messageService.get("product.media.service.parse.metadata.error", metadataJson, e.getMessage(), locale), e);
            return null;
        }
    }

    private void validateFile(MultipartFile file, Long productId, String fileName, Locale locale) {
        if (file.isEmpty()) {
            log.error(messageService.get("product.media.service.file.empty", fileName, locale));
            throw new ProductMediaException(
                    messageService.get("product.media.service.error.file.empty", fileName, locale),
                    productId, fileName
            );
        }
    }

    private void checkUploadResult(List<ProductMediaDto> result, List<String> failedFiles,
                                   int totalFiles, Long productId, Locale locale) {
        if (result.isEmpty() && !failedFiles.isEmpty()) {
            log.error(messageService.get("product.media.service.all.failed",
                    String.join(", ", failedFiles), locale));
            throw new ProductMediaUploadException(
                    messageService.get("product.media.service.error.all.failed",
                            String.join(", ", failedFiles), locale),
                    productId, failedFiles
            );
        }

        if (!failedFiles.isEmpty()) {
            log.warn(messageService.get("product.media.service.partial.success",
                    result.size(), totalFiles, String.join(", ", failedFiles), locale));
        }
    }

    private void updateMainImage(Product product, List<ProductMediaDto> uploadedMedia, Locale locale) {
        uploadedMedia.stream()
                .filter(ProductMediaDto::isMain)
                .findFirst()
                .ifPresent(mainMedia -> {
                    product.setImageUrl(mainMedia.getFileUrl());
                    productRepository.save(product);
                    log.debug(messageService.get("product.media.service.main.updated",
                            product.getId(), mainMedia.getId(), locale));
                });
    }

    private void resetMainImage(Long productId, Locale locale) {
        mediaRepository.findByProductIdAndIsMainTrue(productId)
                .ifPresent(media -> {
                    Product product = productRepository.findById(productId).orElse(null);
                    if (product != null) {
                        product.setImageUrl(null);
                        productRepository.save(product);
                        log.debug(messageService.get("product.media.service.main.reset",
                                productId, locale));
                    }
                });
    }
}