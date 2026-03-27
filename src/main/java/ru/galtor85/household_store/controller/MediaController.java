package ru.galtor85.household_store.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.service.file.MediaService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final MessageService messageService;

    /**
     * Получить файл по ID медиа
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<Resource> getMediaFile(
            @PathVariable Long mediaId) {

        log.info(messageService.get("media.controller.log.request.file", mediaId));

        Resource resource = mediaService.getMediaFile(mediaId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" +
                        mediaService.getMediaFileName(mediaId) + "\"")
                .body(resource);
    }

    /**
     * Получить информацию о медиа (без файла)
     */
    @GetMapping("/{mediaId}/info")
    public ResponseEntity<ProductMediaDto> getMediaInfo(
            @PathVariable Long mediaId) {

        log.info(messageService.get("media.controller.log.request.info", mediaId));

        ProductMediaDto mediaInfo = mediaService.getMediaInfo(mediaId);

        return ResponseEntity.ok(mediaInfo);
    }

    /**
     * Получить все медиа продукта
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductMediaDto>> getProductMedia(
            @PathVariable Long productId) {

        log.info(messageService.get("media.controller.log.request.product", productId));

        List<ProductMediaDto> mediaList = mediaService.getProductMedia(productId);

        return ResponseEntity.ok(mediaList);
    }

    /**
     * Получить главное изображение продукта
     */
    @GetMapping("/product/{productId}/main")
    public ResponseEntity<ProductMediaDto> getProductMainImage(
            @PathVariable Long productId) {

        log.info(messageService.get("media.controller.log.request.main", productId));

        ProductMediaDto mainImage = mediaService.getProductMainImage(productId);

        return ResponseEntity.ok(mainImage);
    }

    /**
     * Получить файл по имени (альтернативный способ)
     */
    @GetMapping("/file/{productId}/{fileName}")
    public ResponseEntity<Resource> getMediaFileByName(
            @PathVariable Long productId,
            @PathVariable String fileName) {

        log.info(messageService.get("media.controller.log.request.file.by.name",
                fileName, productId));

        Resource resource = mediaService.getMediaFileByName(productId, fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}