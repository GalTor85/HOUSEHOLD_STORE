package ru.galtor85.household_store.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.ProductMediaDto;
import ru.galtor85.household_store.service.MediaService;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.Locale;

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
            @PathVariable Long mediaId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        locale = locale != null ? locale : Locale.getDefault();

        log.info(messageService.get("media.controller.log.request.file", mediaId, locale));

        Resource resource = mediaService.getMediaFile(mediaId, locale);

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
            @PathVariable Long mediaId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        locale = locale != null ? locale : Locale.getDefault();

        log.info(messageService.get("media.controller.log.request.info", mediaId, locale));

        ProductMediaDto mediaInfo = mediaService.getMediaInfo(mediaId, locale);

        return ResponseEntity.ok(mediaInfo);
    }

    /**
     * Получить все медиа продукта
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductMediaDto>> getProductMedia(
            @PathVariable Long productId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        locale = locale != null ? locale : Locale.getDefault();

        log.info(messageService.get("media.controller.log.request.product", productId, locale));

        List<ProductMediaDto> mediaList = mediaService.getProductMedia(productId, locale);

        return ResponseEntity.ok(mediaList);
    }

    /**
     * Получить главное изображение продукта
     */
    @GetMapping("/product/{productId}/main")
    public ResponseEntity<ProductMediaDto> getProductMainImage(
            @PathVariable Long productId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        locale = locale != null ? locale : Locale.getDefault();

        log.info(messageService.get("media.controller.log.request.main", productId, locale));

        ProductMediaDto mainImage = mediaService.getProductMainImage(productId, locale);

        return ResponseEntity.ok(mainImage);
    }

    /**
     * Получить файл по имени (альтернативный способ)
     */
    @GetMapping("/file/{productId}/{fileName}")
    public ResponseEntity<Resource> getMediaFileByName(
            @PathVariable Long productId,
            @PathVariable String fileName,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        locale = locale != null ? locale : Locale.getDefault();

        log.info(messageService.get("media.controller.log.request.file.by.name",
                fileName, productId, locale));

        Resource resource = mediaService.getMediaFileByName(productId, fileName, locale);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}