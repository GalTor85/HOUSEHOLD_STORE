package ru.galtor85.household_store.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "{update-status-request.validation.status.empty}")
    @Schema(description = "Account active status", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean active;

    /**
     * Checks if the account should be activated.
     *
     * @return true if active is true, false otherwise
     */
    public boolean isActive() {
        return active != null && active;
    }
}