package ru.galtor85.household_store.validator.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.file.FileSizeExceededException;
import ru.galtor85.household_store.advice.exception.file.FileStorageException;
import ru.galtor85.household_store.advice.exception.file.InvalidFileTypeException;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileValidator {

    private final MessageService messageService;

    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;

    @Value("${app.file.allowed-types:image/jpeg,image/png,image/gif,image/webp,video/mp4,video/quicktime,application/pdf}")
    private String allowedTypesConfig;

    public void validateFile(MultipartFile file, Long productId, String fileName) {
        validateFileSize(file, productId, fileName);
        validateFileType(file, productId, fileName);
        validateFileNotEmpty(file, productId, fileName);
    }

    private void validateFileSize(MultipartFile file, Long productId, String fileName) {
        if (file.getSize() > maxFileSize) {
            String message = messageService.get("file.storage.size.exceeded",
                    formatFileSize(maxFileSize), formatFileSize(file.getSize()));
            throw new FileSizeExceededException(
                    message, fileName, productId, maxFileSize, file.getSize()
            );
        }
    }

    private void validateFileType(MultipartFile file, Long productId, String fileName) {
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

        private void validateFileNotEmpty(MultipartFile file, Long productId, String fileName) {
        if (file.isEmpty()) {
            String message = messageService.get("file.storage.file.empty", fileName);
            throw new FileStorageException(message, null, fileName, productId);
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return (size / 1024) + " KB";
        if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)) + " MB";
        return (size / (1024 * 1024 * 1024)) + " GB";
    }
}