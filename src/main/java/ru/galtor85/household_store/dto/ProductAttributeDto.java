package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product attribute DTO", title = "Product Attribute")
public class ProductAttributeDto {

    @Schema(description = "Attribute ID", example = "1")
    private Long id;

    @Schema(description = "Attribute name", example = "Color")
    private String name;

    @Schema(description = "Attribute value", example = "Red")
    private String value;

    @Schema(description = "Display order", example = "1")
    private Integer order;

    @Schema(description = "Is required", example = "true")
    private boolean required;

    @Schema(description = "Is variant attribute", example = "false")
    private boolean variant;
}