package ru.galtor85.household_store.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a product attribute", title = "Attribute Create Request")
public class AttributeCreateRequest {

    @NotBlank(message = "Attribute name cannot be empty")
    @Schema(description = "Name of the attribute (e.g., Color, Size, Weight)",
            example = "Color",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Attribute value cannot be empty")
    @Schema(description = "Value of the attribute (e.g., Red, XL, 1.5kg)",
            example = "Red",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;

    @Schema(description = "Display order for sorting attributes",
            example = "1",
            defaultValue = "0")
    @Builder.Default
    private Integer order = 0;

    @Schema(description = "Whether this attribute is required when creating a product",
            example = "false",
            defaultValue = "false")
    @Builder.Default
    private Boolean required = false;

    @Schema(description = "Whether this attribute is used as a variant attribute for product variations",
            example = "false",
            defaultValue = "false")
    @Builder.Default
    private Boolean variant = false;
}