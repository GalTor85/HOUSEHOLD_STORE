package ru.galtor85.household_store.dto.request.product;

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
@Schema(description = "Request DTO for updating a product attribute", title = "Attribute Update Request")
public class AttributeUpdateRequest {

    @NotNull(message = "Attribute ID cannot be empty")
    @Schema(description = "Unique identifier of the attribute to update",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "New name for the attribute (e.g., Color, Size, Weight)",
            example = "Color")
    private String name;

    @Schema(description = "New value for the attribute (e.g., Red, XL, 1.5kg)",
            example = "Red")
    private String value;

    @Schema(description = "Display order for sorting attributes",
            example = "1")
    private Integer order;

    @Schema(description = "Whether this attribute is required when creating a product",
            example = "true")
    private Boolean required;

    @Schema(description = "Whether this attribute is used as a variant attribute for product variations",
            example = "false")
    private Boolean variant;
}