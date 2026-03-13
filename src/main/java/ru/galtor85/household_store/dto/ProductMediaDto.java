package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.MediaType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product media DTO", title = "Product Media")
public class ProductMediaDto {

    @Schema(description = "Media ID", example = "1")
    private Long id;

    @Schema(description = "Media type", example = "IMAGE")
    private MediaType mediaType;

    @Schema(description = "File name", example = "iphone-13-pro.jpg")
    private String fileName;

    @Schema(description = "File URL", example = "/uploads/products/iphone-13-pro.jpg")
    private String fileUrl;

    @Schema(description = "File size in bytes", example = "1024000")
    private Long fileSize;

    @Schema(description = "MIME type", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "Alt text", example = "iPhone 13 Pro Graphite")
    private String altText;

    @Schema(description = "Caption", example = "iPhone 13 Pro в цвете Graphite")
    private String caption;

    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;

    @Schema(description = "Is main image", example = "true")
    private boolean isMain;

    @Schema(description = "Width in pixels", example = "1200")
    private Integer width;

    @Schema(description = "Height in pixels", example = "800")
    private Integer height;

    @Schema(description = "Video duration in seconds", example = "30")
    private Integer duration;
}