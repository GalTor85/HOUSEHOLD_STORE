package ru.galtor85.household_store.processor.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.file.FileDeleteException;
import ru.galtor85.household_store.advice.exception.file.FileReadException;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.file.FileSystemHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileOperationProcessor {

    private final FileSystemHelper fileSystemHelper;
    private final MessageService messageService;

    /**
     * Удаление файла и пустой директории
     */
    public void deleteFileAndCleanup(String filePath, Long productId) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        Path path = Paths.get(filePath);
        String fileName = path.getFileName() != null ? path.getFileName().toString() : "unknown";

        try {
            if (fileSystemHelper.fileExists(path)) {
                fileSystemHelper.deleteFile(path);
                log.debug(messageService.get("file.storage.delete.success", fileName));

                // Удаляем пустую директорию продукта
                Path parentDir = path.getParent();
                if (parentDir != null && fileSystemHelper.isDirectoryEmpty(parentDir)) {
                    fileSystemHelper.deleteFile(parentDir);
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
     * Получение пути к файлу
     */
    public Path getFilePath(String fileName, Long productId) {
        Path path = Paths.get("uploads", String.valueOf(productId), fileName).normalize();

        if (!fileSystemHelper.fileExists(path)) {
            throw new FileReadException(
                    messageService.get("file.storage.file.not.found", fileName, productId),
                    fileName, productId
            );
        }

        return path;
    }
}