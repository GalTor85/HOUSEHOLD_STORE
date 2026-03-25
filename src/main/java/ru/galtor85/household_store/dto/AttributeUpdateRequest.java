package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product attribute update request", title = "Attribute Update Request")
public class AttributeUpdateRequest {

    @NotNull(message = "{attribute.validation.id.empty}")
    @Schema(description = "Attribute ID", example = "1", required = true)
    private Long id;

    @Schema(description = "Attribute name", example = "Color")
    private String name;

    @Schema(description = "Attribute value", example = "Red")
    private String value;

    @Schema(description = "Display salesOrder", example = "1")
    private Integer order;

    @Schema(description = "Is required", example = "true")
    private Boolean required;

    @Schema(description = "Is variant attribute", example = "false")
    private Boolean variant;
}