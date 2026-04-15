package ru.galtor85.household_store.util.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.common.ProductMediaUploadDto;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.util.List;

/**
 * Parser for media metadata JSON.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaMetadataParser {

    private final ObjectMapper objectMapper;
    private final LogMessageService logMsg;

    /**
     * Parses JSON metadata to list of ProductMediaUploadDto.
     *
     * @param metadataJson JSON metadata string
     * @return list of parsed DTOs or null if parsing fails
     */
    public List<ProductMediaUploadDto> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.trim().isEmpty()) {
            return null;
        }
        try {
            log.debug(logMsg.get("product.media.service.parse.metadata", metadataJson));
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn(logMsg.get("product.media.service.parse.metadata.error",
                    metadataJson, e.getMessage()), e);
            return null;
        }
    }
}
