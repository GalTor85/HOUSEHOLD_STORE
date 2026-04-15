package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.service.file.MediaService;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.util.List;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_MEDIA;

/**
 * REST controller for serving media files (images, videos, documents).
 *
 * <p>This controller provides public endpoints for:</p>
 * <ul>
 *   <li>Retrieving media files by ID (direct file download/display)</li>
 *   <li>Getting media information (metadata without the file)</li>
 *   <li>Listing all media for a specific product</li>
 *   <li>Getting the main product image</li>
 *   <li>Alternative file retrieval by product ID and file name</li>
 * </ul>
 *
 * <p>All endpoints are public and do not require authentication.</p>
 */
@Slf4j
@RestController
@RequestMapping(CONTROL_MEDIA)
@RequiredArgsConstructor
@Tag(name = "Media", description = "Public endpoints for accessing media files")
public class MediaController {

    private final MediaService mediaService;
    private final LogMessageService logMsg;

    /**
     * Retrieves a media file by its ID.
     * <p>The file is returned with appropriate content type and inline disposition.</p>
     *
     * @param mediaId media identifier
     * @return file resource for download or inline display
     */
    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media file by ID",
            description = "Retrieves the actual media file (image, video, document) by its ID")
    public ResponseEntity<Resource> getMediaFile(
            @Parameter(description = "Media ID", example = "1", required = true)
            @PathVariable Long mediaId) {

        log.info(logMsg.get("media.controller.log.request.file", mediaId));

        Resource resource = mediaService.getMediaFile(mediaId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" +
                        mediaService.getMediaFileName(mediaId) + "\"")
                .body(resource);
    }

    /**
     * Retrieves media information (metadata) without the actual file.
     *
     * @param mediaId media identifier
     * @return media information DTO with metadata
     */
    @GetMapping("/{mediaId}/info")
    @Operation(summary = "Get media info",
            description = "Retrieves media metadata (file name, type, size) without the file content")
    public ResponseEntity<ProductMediaDto> getMediaInfo(
            @Parameter(description = "Media ID", example = "1", required = true)
            @PathVariable Long mediaId) {

        log.info(logMsg.get("media.controller.log.request.info", mediaId));

        ProductMediaDto mediaInfo = mediaService.getMediaInfo(mediaId);

        return ResponseEntity.ok(mediaInfo);
    }

    /**
     * Retrieves all media files associated with a product.
     *
     * @param productId product identifier
     * @return list of media DTOs for the product
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "Get all media for a product",
            description = "Retrieves all media files (images, videos) associated with a product")
    public ResponseEntity<List<ProductMediaDto>> getProductMedia(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        log.info(logMsg.get("media.controller.log.request.product", productId));

        List<ProductMediaDto> mediaList = mediaService.getProductMedia(productId);

        return ResponseEntity.ok(mediaList);
    }

    /**
     * Retrieves the main image for a product.
     *
     * @param productId product identifier
     * @return main image media DTO
     */
    @GetMapping("/product/{productId}/main")
    @Operation(summary = "Get product main image",
            description = "Retrieves the main (primary) image for a product")
    public ResponseEntity<ProductMediaDto> getProductMainImage(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId) {

        log.info(logMsg.get("media.controller.log.request.main", productId));

        ProductMediaDto mainImage = mediaService.getProductMainImage(productId);

        return ResponseEntity.ok(mainImage);
    }

    /**
     * Retrieves a media file by product ID and file name (alternative method).
     *
     * @param productId product identifier
     * @param fileName  file name
     * @return file resource for download or inline display
     */
    @GetMapping("/file/{productId}/{fileName}")
    @Operation(summary = "Get media file by product ID and file name",
            description = "Retrieves a media file using product ID and original file name")
    public ResponseEntity<Resource> getMediaFileByName(
            @Parameter(description = "Product ID", example = "1", required = true)
            @PathVariable Long productId,
            @Parameter(description = "File name", example = "product-image.jpg", required = true)
            @PathVariable String fileName) {

        log.info(logMsg.get("media.controller.log.request.file.by.name",
                fileName, productId));

        Resource resource = mediaService.getMediaFileByName(productId, fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}