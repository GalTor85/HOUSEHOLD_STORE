package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.ProductMediaException;
import ru.galtor85.household_store.dto.ProductMediaDto;
import ru.galtor85.household_store.dto.ProductMediaUploadDto;
import ru.galtor85.household_store.dto.SavedFileInfo;
import ru.galtor85.household_store.entity.ProductMedia;
import ru.galtor85.household_store.mapper.ProductMediaMapper;
import ru.galtor85.household_store.repository.ProductMediaRepository;
import ru.galtor85.household_store.service.FileStorageService;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.validator.MediaValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaUploadProcessor {

    private final FileStorageService fileStorageService;
    private final ProductMediaRepository mediaRepository;
    private final ProductMediaMapper mediaMapper;
    private final MediaValidator validator;
    private final MessageService messageService;

    @Transactional
    public UploadResult processUpload(Long productId, List<MultipartFile> files,
                                      List<ProductMediaUploadDto> metadataList,
                                      Long uploadedBy) {

        List<ProductMediaDto> result = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            ProductMediaUploadDto metadata = (metadataList != null && i < metadataList.size())
                    ? metadataList.get(i)
                    : new ProductMediaUploadDto();

            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

            try {
                log.debug(messageService.get("product.media.service.processing.file", i, fileName));

                // Валидация файла
                validator.validateFileNotEmpty(file, productId, fileName);

                // Сохраняем файл
                SavedFileInfo fileInfo = fileStorageService.storeFileAndGetInfo(file, productId, uploadedBy);

                // Создаем сущность
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

                log.debug(messageService.get("product.media.service.file.saved", fileName, savedMedia.getId()));

            } catch (IOException e) {
                log.error(messageService.get("product.media.service.file.error", fileName, e.getMessage()), e);
                failedFiles.add(fileName);

                if (files.size() == 1) {
                    throw new ProductMediaException(
                            messageService.get("product.media.service.error.single", fileName, e.getMessage()),
                            e, productId, fileName
                    );
                }
            } catch (ProductMediaException e) {
                failedFiles.add(fileName);
                log.error(messageService.get("product.media.service.file.failed", fileName, e.getMessage()));
            }
        }

        validator.validateUploadResult(result, failedFiles, files.size(), productId);

        return new UploadResult(result, failedFiles);
    }

    @lombok.Value
    public static class UploadResult {
        List<ProductMediaDto> successful;
        List<String> failedFiles;
    }
}