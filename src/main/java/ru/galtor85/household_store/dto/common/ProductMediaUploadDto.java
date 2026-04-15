package ru.galtor85.household_store.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * DTO for product media upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product media upload DTO", title = "Product Media Upload")
public class ProductMediaUploadDto {

    @NotNull(message = "{product.media.validation.file.empty}")
    @Schema(description = "Media file", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;

    @Schema(description = "Is main image", example = "true", defaultValue = "false")
    @Builder.Default
    private Boolean isMain = DEFAULT_IS_MAIN;

    @PositiveOrZero(message = "{product.media.validation.sortOrder.positive}")
    @Schema(description = "Sort order", example = "1", defaultValue = "0")
    @Builder.Default
    private Integer sortOrder = DEFAULT_SORT_ORDER;

    @Size(max = MAX_ALT_TEXT_LENGTH, message = "{product.media.validation.altText.max}")
    @Schema(description = "Alt text for image", example = "iPhone 13 Pro Graphite front view")
    private String altText;

    @Size(max = MAX_CAPTION_LENGTH, message = "{product.media.validation.caption.max}")
    @Schema(description = "Caption/description", example = "iPhone 13 Pro in Graphite color")
    private String caption;

    @Schema(description = "Media type (IMAGE, VIDEO)", example = "IMAGE", defaultValue = "IMAGE")
    @Builder.Default
    private String mediaType = DEFAULT_MEDIA_TYPE;

    @PositiveOrZero(message = "{product.media.validation.width.positive}")
    @Schema(description = "Width in pixels (for images)", example = "1920")
    private Integer width;

    @PositiveOrZero(message = "{product.media.validation.height.positive}")
    @Schema(description = "Height in pixels (for images)", example = "1080")
    private Integer height;

    @PositiveOrZero(message = "{product.media.validation.duration.positive}")
    @Schema(description = "Duration in seconds (for videos)", example = "30")
    private Integer duration;
}