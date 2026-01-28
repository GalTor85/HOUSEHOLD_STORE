package ru.galtor85.household_store.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;

@Data
public class ChangeRoleRequest {
    @NotNull(message = "ID пользователя обязательно")
    private Long userId;

    @NotNull(message = "Роль обязательна")
    private Role newRole;
}

