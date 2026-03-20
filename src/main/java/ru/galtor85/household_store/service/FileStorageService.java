package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.FileStorageException;
import ru.galtor85.household_store.builder.ProductMediaFactory;
import ru.galtor85.household_store.dto.SavedFileInfo;
import ru.galtor85.household_store.entity.MediaType;
import ru.galtor85.household_store.entity.ProductMedia;
import ru.galtor85.household_store.processor.FileOperationProcessor;
import ru.galtor85.household_store.util.FileSystemHelper;
import ru.galtor85.household_store.validator.FileValidator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileValidator fileValidator;
    private final FileSystemHelper fileSystemHelper;
    private final FileOperationProcessor fileOperationProcessor;
    private final ProductMediaFactory mediaFactory;
    private final MessageService messageService;

    /**
     * Сохраняет файл на диск и возвращает сущность ProductMedia
     */
    public ProductMedia storeFile(MultipartFile file, Long productId, Long uploadedBy)
            throws IOException {
        String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

        try {
            // Валидация
            fileValidator.validateFile(file, productId, originalFileName);

            // Создание директории
            Path productUploadPath = fileSystemHelper.createProductDirectory(productId);

            // Генерация имени файла
            String storedFileName = fileSystemHelper.generateUniqueFileName(originalFileName);
            Path targetLocation = productUploadPath.resolve(storedFileName);

            // Сохранение файла
            try (InputStream inputStream = file.getInputStream()) {
                fileSystemHelper.saveFile(inputStream, targetLocation);
            }

            log.debug(messageService.get("file.storage.upload.success", originalFileName));

            // Создание сущности через фабрику
            return mediaFactory.createProductMedia(
                    productId,
                    uploadedBy,
                    detectMediaType(file.getContentType()),
                    storedFileName,
                    targetLocation.toString(),
                    file.getSize(),
                    file.getContentType(),
                    null,
                    false
            );

        } catch (FileStorageException e) {
            throw e;
        } catch (IOException e) {
            log.error(messageService.get("file.storage.upload.error", originalFileName, e.getMessage()), e);
            throw new FileStorageException(
                    messageService.get("file.storage.upload.error", originalFileName, e.getMessage()),
                    e, originalFileName, productId
            );
        }
    }

    /**
     * Удаляет файл с диска
     */
    public void deleteFile(String filePath, Long productId) throws IOException {
        fileOperationProcessor.deleteFileAndCleanup(filePath, productId);
    }

    /**
     * Получает файл как Path
     */
    public Path getFilePath(String fileName, Long productId) {
        return fileOperationProcessor.getFilePath(fileName, productId);
    }

    /**
     * Сохраняет файл и возвращает SavedFileInfo
     */
    public SavedFileInfo storeFileAndGetInfo(MultipartFile file, Long productId, Long uploadedBy)
            throws IOException {

        ProductMedia productMedia = storeFile(file, productId, uploadedBy);
        String originalFileName = file.getOriginalFilename();

        return mediaFactory.createSavedFileInfo(productMedia, originalFileName);
    }

    private MediaType detectMediaType(String contentType) {
        if (contentType == null) return MediaType.DOCUMENT;

        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.DOCUMENT;
    }
}