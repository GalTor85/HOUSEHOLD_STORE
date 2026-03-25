package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product attribute create request", title = "Attribute Create Request")
public class AttributeCreateRequest {

    @NotBlank(message = "{attribute.validation.name.empty}")
    @Schema(description = "Attribute name", example = "Color", required = true)
    private String name;

    @NotBlank(message = "{attribute.validation.value.empty}")
    @Schema(description = "Attribute value", example = "Red", required = true)
    private String value;

    @Schema(description = "Display salesOrder", example = "1", defaultValue = "0")
    private Integer order = 0;

    @Schema(description = "Is required", example = "false", defaultValue = "false")
    private Boolean required = false;

    @Schema(description = "Is variant attribute", example = "false", defaultValue = "false")
    private Boolean variant = false;
}