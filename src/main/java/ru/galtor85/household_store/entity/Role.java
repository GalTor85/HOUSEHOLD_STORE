package ru.galtor85.household_store.entity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Role {
    ADMIN,
    USER,
    MANAGER,
    CUSTOMER;


    // Метод для проверки прав
    public boolean canManage(Role other) {
        return this.ordinal() >= other.ordinal();
    }

    // Получить роли, которыми можно управлять
    public List<Role> getManageableRoles() {
        return Arrays.stream(Role.values())
                .filter(role -> this.canManage(role))
                .collect(Collectors.toList());
    }
}
