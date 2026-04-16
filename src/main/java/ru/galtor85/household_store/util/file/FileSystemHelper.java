package ru.galtor85.household_store.util.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.file.FileStorageException;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Helper for file system operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemHelper {

    private static final String FILE_EXTENSION_SEPARATOR = ".";

    private final MessageService messageService;
    private final LogMessageService logMsg;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Creates directory for product files.
     *
     * @param productId product ID
     * @return path to product directory
     */
    public Path createProductDirectory(Long productId) {
        Path productUploadPath = Paths.get(uploadDir, String.valueOf(productId))
                .toAbsolutePath().normalize();

        try {
            if (!Files.exists(productUploadPath)) {
                Files.createDirectories(productUploadPath);
                log.debug(logMsg.get("file.storage.directory.created", productUploadPath.toString()));
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
     * Saves file to disk.
     *
     * @param inputStream    file input stream
     * @param targetLocation target path
     * @throws IOException if save fails
     */
    public void saveFile(InputStream inputStream, Path targetLocation) throws IOException {
        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Deletes file from disk.
     *
     * @param path path to file
     * @throws IOException if deletion fails
     */
    public void deleteFile(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    /**
     * Checks if file exists.
     *
     * @param path path to file
     * @return true if file exists
     */
    public boolean fileExists(Path path) {
        return Files.exists(path);
    }

    /**
     * Checks if directory is empty.
     *
     * @param directory path to directory
     * @return true if directory is empty or doesn't exist
     * @throws IOException if check fails
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
     * Generates unique file name.
     *
     * @param originalFileName original file name
     * @return unique file name with extension
     */
    public String generateUniqueFileName(String originalFileName) {
        String fileExtension = getFileExtension(originalFileName);
        return UUID.randomUUID() + (fileExtension != null ? fileExtension : "");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return null;
    }
}