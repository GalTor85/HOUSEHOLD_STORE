package ru.galtor85.household_store.constants;

/**
 * Technical constants for pagination sorting.
 *
 * <p><b>Note:</b> Business defaults (default page, default size, max size) are stored
 * in {@code application.properties} and loaded via {@link ru.galtor85.household_store.config.BusinessConfig}.</p>
 *
 * @author G@LTor85
 
 */
public final class PaginationConstants {

    private PaginationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Default sort field name
     */
    public static final String DEFAULT_SORT_FIELD = "id";

    /**
     * Default sort direction (ascending)
     */
    public static final String DEFAULT_SORT_DIRECTION = "asc";

    /**
     * Descending sort direction
     */
    public static final String DESC_SORT_DIRECTION = "desc";

    public static final String CODE_DIRECTION = "code";
}