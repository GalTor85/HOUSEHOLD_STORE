package ru.galtor85.household_store.config;

/**
 * Centralized constants for API path configurations.
 *
 * <p>This class holds all API version and endpoint constants to ensure consistency
 * across all controllers. Using constants prevents hardcoded string values and
 * makes API version management easier.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code @RequestMapping(API_BASE + "/users")}
 * </pre>
 */
public final class ApiConstants {

    /**
     * Base path for all API endpoints.
     * <p>Current version: v1</p>
     */
    public static final String API_BASE = "/app";

    /**
     * Base path for API version 2 (reserved for future use).
     */
    public static final String API_V2 = "/app/v2";

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class and should not be instantiated.
     */
    private ApiConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}