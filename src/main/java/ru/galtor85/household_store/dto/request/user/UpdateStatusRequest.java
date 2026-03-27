package ru.galtor85.household_store.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "{update-status-request.validation.status.empty}")
    private Boolean active;

    public boolean isActive() {
        return active != null && active;
    }
}