package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.entity.MediaType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Upload media request DTO", title = "Upload Media Request")
public class UploadMediaRequest {

    @NotNull(message = "{media.validation.file.empty}")
    @Schema(description = "Media file")
    private MultipartFile file;

    @Schema(description = "Media type", example = "IMAGE")
    private MediaType mediaType;

    @Schema(description = "Alt text", example = "iPhone 13 Pro Graphite")
    private String altText;

    @Schema(description = "Caption", example = "iPhone 13 Pro в цвете Graphite")
    private String caption;

    @Schema(description = "Is main image", example = "true")
    private Boolean isMain;
}