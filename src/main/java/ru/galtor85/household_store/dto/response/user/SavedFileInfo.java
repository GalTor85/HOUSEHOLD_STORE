package ru.galtor85.household_store.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "File storage information", title = "Saved File Info")
public class SavedFileInfo {
    @Schema(description = "Stored file name", example = "image_123.jpg")
    private String storedFileName;
    @Schema(description = "Original file name", example = "image.jpg")
    private String originalFileName;
    @Schema(description = "File path", example = "/uploads/image_123.jpg")
    private String filePath;
    @Schema(description = "File size in bytes", example = "123456")
    private Long fileSize;
}