package ru.galtor85.household_store.dto.response.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAssignmentDto {
    private Long id;
    private String category;
    private Boolean isDefault;
    private Integer priority;
}