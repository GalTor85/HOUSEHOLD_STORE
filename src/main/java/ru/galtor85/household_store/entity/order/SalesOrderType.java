package ru.galtor85.household_store.entity.order;

public enum SalesOrderType {
    RETAIL,
    WHOLESALE;

    /**
     * Gets the order type key for localization.
     *
     * @return "wholesale" for WHOLESALE, "retail" otherwise
     */
    public String getKey() {
        return this == WHOLESALE ? "wholesale" : "retail";
    }
}