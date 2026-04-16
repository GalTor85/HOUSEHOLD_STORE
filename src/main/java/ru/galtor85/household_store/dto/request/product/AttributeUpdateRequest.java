package ru.galtor85.household_store.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_ATTRIBUTE_NAME_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_ATTRIBUTE_VALUE_LENGTH;

/**
 * Request DTO for updating a product attribute.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for updating a product attribute", title = "Attribute Update Request")
public class AttributeUpdateRequest {

    @NotNull(message = "{attribute.validation.id.empty}")
    @Schema(description = "Unique identifier of the attribute to update",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Size(max = MAX_ATTRIBUTE_NAME_LENGTH, message = "{attribute.validation.name.max}")
    @Schema(description = "New name for the attribute (e.g., Color, Size, Weight)",
            example = "Color")
    private String name;

    @Size(max = MAX_ATTRIBUTE_VALUE_LENGTH, message = "{attribute.validation.value.max}")
    @Schema(description = "New value for the attribute (e.g., Red, XL, 1.5kg)",
            example = "Red")
    private String value;

    @Schema(description = "Display order for sorting attributes", example = "1")
    private Integer order;

    @Schema(description = "Whether this attribute is required when creating a product", example = "true")
    private Boolean required;

    @Schema(description = "Whether this attribute is used as a variant attribute for product variations", example = "false")
    private Boolean variant;
}