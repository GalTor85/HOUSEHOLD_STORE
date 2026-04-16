package ru.galtor85.household_store.processor.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.product.ProductMediaException;
import ru.galtor85.household_store.dto.common.ProductMediaUploadDto;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.dto.response.user.SavedFileInfo;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.mapper.product.ProductMediaMapper;
import ru.galtor85.household_store.repository.product.ProductMediaRepository;
import ru.galtor85.household_store.service.file.FileStorageService;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.validator.media.MediaValidator;

import java.util.ArrayList;
import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.UNKNOWN_FILE_NAME;

/**
 * Processor for uploading product media files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaUploadProcessor {

    private final FileStorageService fileStorageService;
    private final ProductMediaRepository mediaRepository;
    private final ProductMediaMapper mediaMapper;
    private final MediaValidator validator;
    private final LogMessageService logMsg;

    /**
     * Processes upload of multiple media files.
     *
     * @param productId    the product ID
     * @param files        list of files to upload
     * @param metadataList list of metadata for each file
     * @param uploadedBy   ID of the user uploading
     * @return UploadResult with successful and failed files
     */
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

            String fileName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : UNKNOWN_FILE_NAME;

            try {
                log.debug(logMsg.get("product.media.service.processing.file", i, fileName));

                validator.validateFileNotEmpty(file, productId, fileName);

                SavedFileInfo fileInfo = fileStorageService.storeFileAndGetInfo(file, productId, uploadedBy);

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

                log.debug(logMsg.get("product.media.service.file.saved", fileName, savedMedia.getId()));

            } catch (ProductMediaException e) {
                failedFiles.add(fileName);
                log.error(logMsg.get("product.media.service.file.failed", fileName, e.getMessage()));
            }
        }

        validator.validateUploadResult(result, failedFiles, productId);

        return new UploadResult(result, failedFiles);
    }

    /**
     * Result of media upload operation.
     */
    public record UploadResult(List<ProductMediaDto> successful, List<String> failedFiles) {
    }
}