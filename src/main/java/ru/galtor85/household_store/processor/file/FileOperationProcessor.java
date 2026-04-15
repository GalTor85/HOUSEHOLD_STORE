package ru.galtor85.household_store.processor.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.file.FileDeleteException;
import ru.galtor85.household_store.advice.exception.file.FileReadException;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.file.FileSystemHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.galtor85.household_store.constants.TechnicalConstants.UNKNOWN_FILE_NAME;

/**
 * Processor for file operations (delete, read, path resolution).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileOperationProcessor {

    private final FileSystemHelper fileSystemHelper;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Deletes a file and its parent directory if empty.
     *
     * @param filePath  full path to the file
     * @param productId the product ID (for error context)
     * @throws IOException if deletion fails
     */
    public void deleteFileAndCleanup(String filePath, Long productId) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        Path path = Paths.get(filePath);
        String fileName = path.getFileName() != null ? path.getFileName().toString() : UNKNOWN_FILE_NAME;

        try {
            if (fileSystemHelper.fileExists(path)) {
                fileSystemHelper.deleteFile(path);
                log.debug(logMsg.get("file.storage.delete.success", fileName));

                // Remove empty product directory
                Path parentDir = path.getParent();
                if (parentDir != null && fileSystemHelper.isDirectoryEmpty(parentDir)) {
                    fileSystemHelper.deleteFile(parentDir);
                    log.debug(logMsg.get("file.storage.directory.deleted", parentDir.toString()));
                }
            }
        } catch (IOException e) {
            log.error(logMsg.get("file.storage.delete.error", fileName, e.getMessage()), e);
            throw new FileDeleteException(
                    messageService.get("file.storage.delete.error", fileName, e.getMessage()),
                    e, fileName, productId
            );
        }
    }

    /**
     * Resolves the file path for a given file name and product ID.
     *
     * @param fileName  the file name
     * @param productId the product ID
     * @return Path to the file
     * @throws FileReadException if file does not exist
     */
    public Path getFilePath(String fileName, Long productId) {
        Path path = Paths.get(uploadDir, String.valueOf(productId), fileName).normalize();

        if (!fileSystemHelper.fileExists(path)) {
            throw new FileReadException(
                    messageService.get("file.storage.file.not.found", fileName, productId),
                    fileName, productId
            );
        }

        return path;
    }
}