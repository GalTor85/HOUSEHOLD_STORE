package ru.galtor85.household_store.entity.user;

/**
 * User role enumeration with hierarchy management.
 */
public enum Role {
    ADMIN,
    USER,
    MANAGER,
    CUSTOMER;

    /**
     * Checks if this role can manage the target role.
     *
     * @param targetRole role to manage
     * @return true if management is allowed
     */
    public boolean canManage(Role targetRole) {
        return switch (this) {
            case ADMIN -> true;
            case MANAGER -> targetRole == USER || targetRole == CUSTOMER;
            case USER -> targetRole == CUSTOMER;
            case CUSTOMER -> false;
        };
    }

    /**
     * Checks if this role CANNOT manage the target role.
     * This method is designed to be used in validation checks without negation.
     *
     * @param targetRole role to manage
     * @return true if management is NOT allowed
     */
    public boolean cannotManage(Role targetRole) {
        return !canManage(targetRole);
    }
}
