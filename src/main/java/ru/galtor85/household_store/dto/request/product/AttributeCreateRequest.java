package ru.galtor85.household_store.dto.request.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_ATTRIBUTE_NAME_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_ATTRIBUTE_VALUE_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_ORDER;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_REQUIRED;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_VARIANT;

/**
 * Request DTO for creating a product attribute.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a product attribute", title = "Attribute Create Request")
public class AttributeCreateRequest {

    @NotBlank(message = "{attribute.validation.name.empty}")
    @Size(max = MAX_ATTRIBUTE_NAME_LENGTH, message = "{attribute.validation.name.max}")
    @Schema(description = "Name of the attribute (e.g., Color, Size, Weight)",
            example = "Color",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "{attribute.validation.value.empty}")
    @Size(max = MAX_ATTRIBUTE_VALUE_LENGTH, message = "{attribute.validation.value.max}")
    @Schema(description = "Value of the attribute (e.g., Red, XL, 1.5kg)",
            example = "Red",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;

    @Schema(description = "Display order for sorting attributes", example = "1")
    @Builder.Default
    private Integer order = DEFAULT_ORDER;

    @Schema(description = "Whether this attribute is required when creating a product", example = "false")
    @Builder.Default
    private Boolean required = DEFAULT_REQUIRED;

    @Schema(description = "Whether this attribute is used as a variant attribute for product variations", example = "false")
    @Builder.Default
    private Boolean variant = DEFAULT_VARIANT;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasOrder() {
        return order != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasRequired() {
        return required != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasVariant() {
        return variant != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isRequiredTrue() {
        return Boolean.TRUE.equals(required);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isVariantTrue() {
        return Boolean.TRUE.equals(variant);
    }
}