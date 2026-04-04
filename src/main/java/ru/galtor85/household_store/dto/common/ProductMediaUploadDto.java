package ru.galtor85.household_store.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product media upload DTO", title = "Product Media Upload")
public class ProductMediaUploadDto {

    @NotNull(message = "{product.media.validation.file.empty}")
    @Schema(description = "Media file", required = true)
    private MultipartFile file;

    @Schema(description = "Is main image", example = "true", defaultValue = "false")
    private Boolean isMain;

    @PositiveOrZero(message = "{product.media.validation.sortOrder.positive}")
    @Schema(description = "Sort salesOrder", example = "1", defaultValue = "0")
    private Integer sortOrder;

    @Schema(description = "Alt text for image", example = "iPhone 13 Pro Graphite front view")
    private String altText;

    @Schema(description = "Caption/description", example = "iPhone 13 Pro in Graphite color")
    private String caption;

    @Schema(description = "Media type (IMAGE, VIDEO)", example = "IMAGE", defaultValue = "IMAGE")
    private String mediaType;

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
    private Boolean isThumbnail;

    @Schema(description = "Additional metadata as JSON string")
    private String metadata;
}