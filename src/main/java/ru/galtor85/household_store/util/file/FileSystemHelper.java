package ru.galtor85.household_store.util.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.file.FileStorageException;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemHelper {

    private final MessageService messageService;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Создание директории для продукта
     */
    public Path createProductDirectory(Long productId) throws IOException {
        Path productUploadPath = Paths.get(uploadDir, String.valueOf(productId)).toAbsolutePath().normalize();

        try {
            if (!Files.exists(productUploadPath)) {
                Files.createDirectories(productUploadPath);
                log.debug(messageService.get("file.storage.directory.created", productUploadPath.toString()));
            }
            return productUploadPath;
        } catch (IOException e) {
            throw new FileStorageException(
                    messageService.get("file.storage.directory.create.error", productId, e.getMessage()),
                    e, null, productId
            );
        }
    }

    /**
     * Сохранение файла на диск
     */
    public Path saveFile(InputStream inputStream, Path targetLocation) throws IOException {
        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return targetLocation;
    }

    /**
     * Удаление файла
     */
    public boolean deleteFile(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    /**
     * Проверка существования файла
     */
    public boolean fileExists(Path path) {
        return Files.exists(path);
    }

    /**
     * Проверка, пуста ли директория
     */
    public boolean isDirectoryEmpty(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return true;
        }
        try (var stream = Files.list(directory)) {
            return stream.findAny().isEmpty();
        }
    }

    /**
     * Генерация уникального имени файла
     */
    public String generateUniqueFileName(String originalFileName) {
        String fileExtension = getFileExtension(originalFileName);
        return UUID.randomUUID() + (fileExtension != null ? fileExtension : "");
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
}