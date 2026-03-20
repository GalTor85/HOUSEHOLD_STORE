package ru.galtor85.household_store.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.ProductMediaUploadDto;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaMetadataParser {

    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    public List<ProductMediaUploadDto> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.trim().isEmpty()) {
            return null;
        }
        try {
            log.debug(messageService.get("product.media.service.parse.metadata", metadataJson));
            return objectMapper.readValue(metadataJson,
                    new TypeReference<List<ProductMediaUploadDto>>() {});
        } catch (Exception e) {
            log.warn(messageService.get("product.media.service.parse.metadata.error",
                    metadataJson, e.getMessage()), e);
            return null;
        }
    }
}