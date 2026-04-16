package ru.galtor85.household_store.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.user.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update role request DTO")
public class UpdateRoleRequest {

    @NotNull(message = "{update-role-request.validation.role.empty}")
    @Schema(description = "New role for the user", example = "MANAGER", requiredMode = Schema.RequiredMode.REQUIRED)
    private Role newRole;
}