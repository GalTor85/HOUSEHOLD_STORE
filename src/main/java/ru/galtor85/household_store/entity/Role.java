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
    public boolean canManage(Role targetRole) {
        // Иерархия прав: ADMIN > MANAGER > USER > CUSTOMER
        return switch (this) {
            case ADMIN -> true;  // Админ может управлять всеми
            case MANAGER -> targetRole == USER || targetRole == CUSTOMER;
            case USER -> targetRole == CUSTOMER;
            case CUSTOMER -> false;  // Клиенты не могут управлять никем
            default -> false;
        };
    }

    // Получить роли, которыми можно управлять
    public List<Role> getManageableRoles() {
        return Arrays.stream(Role.values())
                .filter(role -> this.canManage(role))
                .collect(Collectors.toList());
    }
}
