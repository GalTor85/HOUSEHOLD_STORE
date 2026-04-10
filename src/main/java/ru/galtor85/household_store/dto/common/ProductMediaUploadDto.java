package ru.galtor85.household_store.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_ALT_TEXT_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_CAPTION_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_METADATA_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_SORT_ORDER;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_IS_MAIN;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_IS_THUMBNAIL;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_MEDIA_TYPE;

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
    private Boolean isMain = DEFAULT_IS_MAIN;

    @PositiveOrZero(message = "{product.media.validation.sortOrder.positive}")
    @Schema(description = "Sort order", example = "1", defaultValue = "0")
    private Integer sortOrder = DEFAULT_SORT_ORDER;

    @Size(max = MAX_ALT_TEXT_LENGTH, message = "{product.media.validation.altText.max}")
    @Schema(description = "Alt text for image", example = "iPhone 13 Pro Graphite front view")
    private String altText;

    @Size(max = MAX_CAPTION_LENGTH, message = "{product.media.validation.caption.max}")
    @Schema(description = "Caption/description", example = "iPhone 13 Pro in Graphite color")
    private String caption;

    @Schema(description = "Media type (IMAGE, VIDEO)", example = "IMAGE", defaultValue = "IMAGE")
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

    @Schema(description = "Is this a thumbnail", example = "false", defaultValue = "false")
    private Boolean isThumbnail = DEFAULT_IS_THUMBNAIL;

    @Size(max = MAX_METADATA_LENGTH, message = "{product.media.validation.metadata.max}")
    @Schema(description = "Additional metadata as JSON string")
    private String metadata;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasFile() {
        return file != null && !file.isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasIsMain() {
        return isMain != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasSortOrder() {
        return sortOrder != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasAltText() {
        return altText != null && !altText.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCaption() {
        return caption != null && !caption.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasMediaType() {
        return mediaType != null && !mediaType.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasWidth() {
        return width != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasHeight() {
        return height != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDuration() {
        return duration != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasIsThumbnail() {
        return isThumbnail != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasMetadata() {
        return metadata != null && !metadata.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isMainTrue() {
        return Boolean.TRUE.equals(isMain);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isThumbnailTrue() {
        return Boolean.TRUE.equals(isThumbnail);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedAltText() {
        return altText != null ? altText.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedCaption() {
        return caption != null ? caption.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedMediaType() {
        return mediaType != null ? mediaType.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedMetadata() {
        return metadata != null ? metadata.trim() : null;
    }
}