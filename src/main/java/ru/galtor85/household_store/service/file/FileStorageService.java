package ru.galtor85.household_store.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.file.FileStorageException;
import ru.galtor85.household_store.builder.media.ProductMediaFactory;
import ru.galtor85.household_store.dto.response.user.SavedFileInfo;
import ru.galtor85.household_store.entity.product.MediaType;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.processor.file.FileOperationProcessor;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.file.FileSystemHelper;
import ru.galtor85.household_store.validator.file.FileValidator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static ru.galtor85.household_store.constants.TechnicalConstants.UNKNOWN_FILE_NAME;

/**
 * Service for storing and managing files on disk.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final String IMAGE_CONTENT_TYPE = "image/";
    private static final String VIDEO_CONTENT_TYPE = "video/";

    private final FileValidator fileValidator;
    private final FileSystemHelper fileSystemHelper;
    private final FileOperationProcessor fileOperationProcessor;
    private final ProductMediaFactory mediaFactory;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Stores file on disk and returns ProductMedia entity.
     *
     * @param file file to store
     * @param productId product ID
     * @param uploadedBy user ID who uploaded
     * @return ProductMedia entity
     */
    public ProductMedia storeFile(MultipartFile file, Long productId, Long uploadedBy) {
        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : UNKNOWN_FILE_NAME;

        try {
            fileValidator.validateFile(file, productId, originalFileName);

            Path productUploadPath = fileSystemHelper.createProductDirectory(productId);
            String storedFileName = fileSystemHelper.generateUniqueFileName(originalFileName);
            Path targetLocation = productUploadPath.resolve(storedFileName);

            try (InputStream inputStream = file.getInputStream()) {
                fileSystemHelper.saveFile(inputStream, targetLocation);
            }

            log.debug(logMsg.get("file.storage.upload.success", originalFileName));

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

        } catch (IOException e) {
            log.error(logMsg.get("file.storage.upload.error", originalFileName, e.getMessage()), e);
            throw new FileStorageException(
                    messageService.get("file.storage.upload.error", originalFileName, e.getMessage()),
                    e, originalFileName, productId
            );
        }
    }

    /**
     * Deletes file from disk.
     *
     * @param filePath path to file
     * @param productId product ID
     */
    public void deleteFile(String filePath, Long productId) {
        fileOperationProcessor.deleteFileAndCleanup(filePath, productId);
    }

    /**
     * Stores file and returns SavedFileInfo.
     *
     * @param file file to store
     * @param productId product ID
     * @param uploadedBy user ID who uploaded
     * @return SavedFileInfo DTO
     */
    public SavedFileInfo storeFileAndGetInfo(MultipartFile file, Long productId, Long uploadedBy) {
        ProductMedia productMedia = storeFile(file, productId, uploadedBy);
        String originalFileName = file.getOriginalFilename();
        return mediaFactory.createSavedFileInfo(productMedia, originalFileName);
    }

    private MediaType detectMediaType(String contentType) {
        if (contentType == null) {
            return MediaType.DOCUMENT;
        }
        if (contentType.startsWith(IMAGE_CONTENT_TYPE)) {
            return MediaType.IMAGE;
        }
        if (contentType.startsWith(VIDEO_CONTENT_TYPE)) {
            return MediaType.VIDEO;
        }
        return MediaType.DOCUMENT;
    }
}