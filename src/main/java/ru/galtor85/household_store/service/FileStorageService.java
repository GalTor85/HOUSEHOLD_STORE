package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.SavedFileInfo;
import ru.galtor85.household_store.entity.MediaType;
import ru.galtor85.household_store.entity.ProductMedia;
import ru.galtor85.household_store.mapper.ProductMediaMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.max-size:10485760}") // 10MB по умолчанию
    private long maxFileSize;

    @Value("${app.file.allowed-types:image/jpeg,image/png,image/gif,image/webp,video/mp4,video/quicktime,application/pdf}")
    private String allowedTypesConfig;

    private final MessageService messageService;
    private final ProductMediaMapper mediaMapper;

    /**
     * Сохраняет файл на диск и возвращает сущность ProductMedia
     */
    public ProductMedia storeFile(MultipartFile file, Long productId, Long uploadedBy, Locale locale)
            throws IOException {
        locale = locale != null ? locale : Locale.getDefault();
        String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

        try {
            // Проверка размера файла
            validateFileSize(file, productId, originalFileName, locale);

            // Проверка типа файла
            validateFileType(file, productId, originalFileName, locale);

            // Создаем директорию для продукта, если её нет
            Path productUploadPath = createProductDirectory(productId, locale);

            // Генерируем уникальное имя файла
            String fileExtension = getFileExtension(originalFileName);
            String storedFileName = UUID.randomUUID() + (fileExtension != null ? fileExtension : "");

            Path targetLocation = productUploadPath.resolve(storedFileName);

            // Копируем файл
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.debug(messageService.get("file.storage.upload.success", originalFileName));

            // Создаем и возвращаем сущность ProductMedia
            return   mediaMapper.createEntity(
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
    public void deleteFile(String filePath, Long productId, Locale locale) throws IOException {
        locale = locale != null ? locale : Locale.getDefault();

        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        Path path = Paths.get(filePath);
        String fileName = path.getFileName() != null ? path.getFileName().toString() : "unknown";

        try {
            if (Files.exists(path)) {
                Files.delete(path);
                log.debug(messageService.get("file.storage.delete.success", fileName));

                // Пытаемся удалить пустую директорию продукта
                Path parentDir = path.getParent();
                if (parentDir != null && isDirectoryEmpty(parentDir)) {
                    Files.delete(parentDir);
                    log.debug(messageService.get("file.storage.directory.deleted", parentDir.toString()));
                }
            }
        } catch (IOException e) {
            log.error(messageService.get("file.storage.delete.error", fileName, e.getMessage()), e);
            throw new FileDeleteException(
                    messageService.get("file.storage.delete.error", fileName, e.getMessage()),
                    e, fileName, productId
            );
        }
    }

    /**
     * Получает файл как Path
     */
    public Path getFilePath(String fileName, Long productId, Locale locale) {
        Path path = Paths.get(uploadDir, String.valueOf(productId), fileName).normalize();

        if (!Files.exists(path)) {
            throw new FileReadException(
                    messageService.get("file.storage.file.not.found", fileName, productId),
                    fileName, productId
            );
        }

        return path;
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateFileSize(MultipartFile file, Long productId, String fileName, Locale locale) {
        if (file.getSize() > maxFileSize) {
            String message = messageService.get("file.storage.size.exceeded",
                    formatFileSize(maxFileSize), formatFileSize(file.getSize()));
            throw new FileSizeExceededException(
                    message, fileName, productId, maxFileSize, file.getSize()
            );
        }
    }

    private void validateFileType(MultipartFile file, Long productId, String fileName, Locale locale) {
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(allowedTypesConfig.split(","));

        if (contentType == null || allowedTypes.stream().noneMatch(contentType::startsWith)) {
            String message = messageService.get("file.storage.invalid.type",
                    contentType, allowedTypesConfig);
            throw new InvalidFileTypeException(
                    message, fileName, productId, contentType, allowedTypesConfig
            );
        }
    }

    private Path createProductDirectory(Long productId, Locale locale) throws IOException {
        Path productUploadPath = Paths.get(uploadDir, String.valueOf(productId)).toAbsolutePath().normalize();

        try {
            if (!Files.exists(productUploadPath)) {
                Files.createDirectories(productUploadPath);
            }
            return productUploadPath;
        } catch (IOException e) {
            throw new FileStorageException(
                    messageService.get("file.storage.directory.create.error", productId, e.getMessage()),
                    e, null, productId
            );
        }
    }

    public SavedFileInfo storeFileAndGetInfo(MultipartFile file, Long productId, Long uploadedBy, Locale locale)
            throws IOException {

        ProductMedia productMedia = storeFile(file, productId, uploadedBy, locale);
        String originalFileName = file.getOriginalFilename();

        return SavedFileInfo.builder()
                .storedFileName(productMedia.getFileName())
                .originalFileName(originalFileName)
                .filePath(productMedia.getFilePath())
                .fileSize(productMedia.getFileSize())
                .build();
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

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return null;
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return true;
        }
        try (var stream = Files.list(directory)) {
            return stream.findAny().isEmpty();
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return (size / 1024) + " KB";
        if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)) + " MB";
        return (size / (1024 * 1024 * 1024)) + " GB";
    }
}