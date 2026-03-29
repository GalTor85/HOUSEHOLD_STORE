package ru.galtor85.household_store.dto.response.warehouse;

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
@Schema(description = "DTO representing a category assignment to a warehouse")
public class CategoryAssignmentDto {

    @Schema(description = "Assignment ID", example = "1")
    private Long id;

    @Schema(description = "Category name", example = "Electronics")
    private String category;

    @Schema(description = "Whether this is the default warehouse for this category", example = "true")
    private Boolean isDefault;

    @Schema(description = "Priority of this warehouse for the category (lower number = higher priority)",
            example = "1")
    private Integer priority;
}