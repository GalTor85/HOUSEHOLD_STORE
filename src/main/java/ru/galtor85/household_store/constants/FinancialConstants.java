package ru.galtor85.household_store.constants;

/**
 * Financial constants for the application.
 *
 * <p>This class contains constants that are purely technical and do not represent
 * business logic configuration. These values are fixed and should not be changed
 * without careful consideration of the impact on financial calculations.</p>
 *
 * <p><b>Note:</b> Business configuration (like default currency, discount percentages,
 * invoice due days) should be stored in {@code application.properties} and
 * loaded via {@link ru.galtor85.household_store.config.FinancialConfig}.</p>
 *
 * @author G@LTor85
 
 */
public final class FinancialConstants {

    private FinancialConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // =========================================================================
    // CURRENCY CONSTANTS
    // =========================================================================

    /**
     * Currency code pattern (3 uppercase letters)
     */
    public static final String CURRENCY_CODE_PATTERN = "^[A-Z]{3}$";

    /**
     * Minimum exchange rate as string for DecimalMin annotation
     */
    public static final String MIN_EXCHANGE_RATE_STR = "0.0001";

    /**
     * Default number of decimal places for currency
     */
    public static final int DEFAULT_DECIMAL_PLACES = 2;

    /**
     * Default isBase value
     */
    public static final boolean DEFAULT_IS_BASE = false;

}