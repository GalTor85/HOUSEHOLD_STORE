package ru.galtor85.household_store.dto.request.cleanup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Soft delete request")
public class SoftDeleteRequest {

    @NotBlank(message = "{cleanup.validation.reason.required}")
    @Size(max = 500, message = "{cleanup.validation.reason.max}")
    @Schema(description = "Reason for deletion", example = "Data retention policy", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}