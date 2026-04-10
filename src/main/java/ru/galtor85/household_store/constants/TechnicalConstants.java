package ru.galtor85.household_store.constants;

import java.math.BigDecimal;

/**
 * Technical constants for the application.
 *
 * <p>This class contains constants that are purely technical and do not represent
 * business logic configuration. These values are fixed and should not be changed
 * without careful consideration of the impact on system functionality.</p>
 *
 * <p>Examples of technical constants:</p>
 * <ul>
 *   <li>Barcode formats and lengths</li>
 *   <li>File upload constraints</li>
 *   <li>Technical timeouts and limits</li>
 *   <li>Format patterns and regular expressions</li>
 * </ul>
 *
 * <p><b>Note:</b> Business configuration (like cart expiry days, pagination defaults,
 * discount percentages) should be stored in {@code application.properties} and
 * loaded via {@link ru.galtor85.household_store.config.BusinessConfig}.</p>
 *
 * @author Household Store Team
 * @since 1.0
 */
public final class TechnicalConstants {

    private TechnicalConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // =========================================================================
    // BARCODE CONSTANTS
    // =========================================================================

    /**
     * Default barcode format for warehouse and cell identification
     */
    public static final String DEFAULT_BARCODE_FORMAT = "CODE_128";

    /**
     * Standard EAN-13 barcode length
     */
    public static final int BARCODE_EAN13_LENGTH = 13;

    /**
     * Standard UPC-A barcode length
     */
    public static final int BARCODE_UPC_LENGTH = 12;

    // =========================================================================
    // REGEX PATTERNS
    // =========================================================================

    /**
     * Pattern for email validation
     */
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";

    /**
     * Pattern for phone number validation (international format)
     */
    public static final String PHONE_PATTERN = "^\\+?[0-9\\-\\s]{10,15}$";

    /**
     * Pattern for date validation (YYYY-MM-DD)
     */
    public static final String DATE_PATTERN = "^\\d{4}-\\d{2}-\\d{2}$";

    /**
     * Pattern for password validation (at least one digit, one lowercase, one uppercase, one special character, min 6 chars)
     */
    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&*!])[A-Za-z\\d@#$%^&*!]{6,}$";

    /**
     * Pattern for cell code validation (format: SECTION-RACK-SHELF-POSITION)
     */
    public static final String CELL_CODE_PATTERN = "^[A-Z0-9]+(-[A-Z0-9]+)*$";

    /**
     * Pattern for checking if string contains at least one digit
     */
    public static final String CONTAINS_DIGIT = ".*\\d.*";

    /**
     * Pattern for checking if string contains at least one lowercase letter
     */
    public static final String CONTAINS_LOWERCASE = ".*[a-z].*";

    /**
     * Pattern for checking if string contains at least one uppercase letter
     */
    public static final String CONTAINS_UPPERCASE = ".*[A-Z].*";

    /**
     * Pattern to keep only digits (remove all non-digit characters)
     */
    public static final String KEEP_ONLY_DIGITS = "[^0-9]";

    /**
     * Minimum phone number length (digits only)
     */
    public static final int MIN_PHONE_LENGTH = 5;

    /**
     * Maximum phone number length (digits only)
     */
    public static final int MAX_PHONE_LENGTH = 15;

    // =========================================================================
    // PASSWORD CONSTANTS
    // =========================================================================

    /**
     * BCrypt strength for password encoding
     */
    public static final int BCRYPT_STRENGTH = 10;

    /**
     * Minimum password length (technical minimum, business value in config)
     */
    public static final int TECHNICAL_MIN_PASSWORD_LENGTH = 6;

    // =========================================================================
    // TOKEN CONSTANTS
    // =========================================================================

    /**
     * JWT token prefix for Authorization header
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT token type
     */
    public static final String TOKEN_TYPE = "Bearer";

    // =========================================================================
    // ID GENERATION CONSTANTS
    // =========================================================================

    /**
     * Default length for random ID suffix
     */
    public static final int DEFAULT_RANDOM_SUFFIX_LENGTH = 8;

    /**
     * Maximum length for random ID suffix
     */
    public static final int MAX_RANDOM_SUFFIX_LENGTH = 32;

    // =========================================================================
    // HTTP CONSTANTS
    // =========================================================================

    /**
     * UTF-8 character encoding
     */
    public static final String UTF_8_ENCODING = "UTF-8";

    /**
     * JSON content type
     */
    public static final String CONTENT_TYPE_JSON = "application/json";

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Validates if a string matches the EAN-13 barcode format.
     *
     * @param barcode the barcode to validate
     * @return true if barcode is valid EAN-13
     */
    public static boolean isValidEan13(String barcode) {
        return barcode != null && barcode.length() == BARCODE_EAN13_LENGTH && barcode.matches("\\d+");
    }

    /**
     * Validates if a string matches the UPC-A barcode format.
     *
     * @param barcode the barcode to validate
     * @return true if barcode is valid UPC-A
     */
    public static boolean isValidUpcA(String barcode) {
        return barcode != null && barcode.length() == BARCODE_UPC_LENGTH && barcode.matches("\\d+");
    }

    /**
     * System creator identifier for audit trail
     */
    public static final String SYSTEM_CREATOR = "system";

    /**
     * Minimum quantity value (0 means remove item)
     */
    public static final int MIN_QUANTITY = 0;

    // =========================================================================
    // BANK ACCOUNT CONSTANTS
    // =========================================================================

    /**
     * Bank account number pattern (20 digits)
     */
    public static final String BANK_ACCOUNT_NUMBER_PATTERN = "^[0-9]{20}$";

    /**
     * BIC pattern (9 digits)
     */
    public static final String BIC_PATTERN = "^[0-9]{9}$";

    /**
     * Correspondent account pattern (20 digits)
     */
    public static final String CORRESPONDENT_ACCOUNT_PATTERN = "^[0-9]{20}$";

    /**
     * Default active status
     */
    public static final boolean DEFAULT_ACTIVE_STATUS = true;

    // =========================================================================
    // CASH REGISTER CONSTANTS
    // =========================================================================

    /**
     * Cash register number pattern (uppercase letters, numbers, hyphens, 3-20 chars)
     */
    public static final String CASH_REGISTER_NUMBER_PATTERN = "^[A-Z0-9-]{3,20}$";

    /**
     * Minimum cash register number length
     */
    public static final int MIN_REGISTER_NUMBER_LENGTH = 3;

    /**
     * Maximum cash register number length
     */
    public static final int MAX_REGISTER_NUMBER_LENGTH = 20;

    /**
     * Maximum location length
     */
    public static final int MAX_LOCATION_LENGTH = 200;

    /**
     * Minimum balance as string for DecimalMin annotation
     */
    public static final String MIN_BALANCE_STR = "0.00";

    /**
     * Default balance as string
     */
    public static final String DEFAULT_BALANCE_STR = "0.00";

    /**
     * Minimum amount for any transaction (technical limit - 1 cent/kopek)
     */
    public static final String MIN_AMOUNT_STR = "0.01";
    public static final BigDecimal MIN_AMOUNT = new BigDecimal(MIN_AMOUNT_STR);

    // =========================================================================
    // CURRENCY CONSTANTS
    // =========================================================================

    /**
     * Maximum currency name length
     */
    public static final int MAX_CURRENCY_NAME_LENGTH = 100;

    /**
     * Minimum symbol length
     */
    public static final int MIN_SYMBOL_LENGTH = 1;

    /**
     * Maximum symbol length
     */
    public static final int MAX_SYMBOL_LENGTH = 5;

    // =========================================================================
    // ADDRESS & NOTES CONSTANTS
    // =========================================================================

    /**
     * Maximum address length (shipping, billing)
     */
    public static final int MAX_ADDRESS_LENGTH = 255;

    /**
     * Maximum payment method name length
     */
    public static final int MAX_PAYMENT_METHOD_LENGTH = 50;

    /**
     * Maximum notes length
     */
    public static final int MAX_NOTES_LENGTH = 1000;

    // =========================================================================
    // STOCK MOVEMENT CONSTANTS
    // =========================================================================

    /**
     * Minimum quantity for stock movement
     */
    public static final int MIN_MOVEMENT_QUANTITY = 1;

    /**
     * Maximum reference type length
     */
    public static final int MAX_REFERENCE_TYPE_LENGTH = 50;

    /**
     * Maximum batch number length
     */
    public static final int MAX_BATCH_NUMBER_LENGTH = 100;

    // =========================================================================
    // PAYMENT CONSTANTS
    // =========================================================================

    /**
     * Currency code length (ISO 4217)
     */
    public static final int CURRENCY_CODE_LENGTH = 3;

    /**
     * Maximum description length
     */
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    // =========================================================================
    // PRICE CALCULATION CONSTANTS
    // =========================================================================

    /**
     * Maximum promo code length
     */
    public static final int MAX_PROMO_CODE_LENGTH = 50;

    // =========================================================================
    // PRODUCT ATTRIBUTE CONSTANTS
    // =========================================================================

    /**
     * Maximum attribute name length
     */
    public static final int MAX_ATTRIBUTE_NAME_LENGTH = 100;

    /**
     * Maximum attribute value length
     */
    public static final int MAX_ATTRIBUTE_VALUE_LENGTH = 255;

    /**
     * Default display order
     */
    public static final int DEFAULT_ORDER = 0;

    /**
     * Default required flag
     */
    public static final boolean DEFAULT_REQUIRED = false;

    /**
     * Default variant flag
     */
    public static final boolean DEFAULT_VARIANT = false;

    // =========================================================================
    // PRODUCT CONSTANTS
    // =========================================================================

    /**
     * SKU length range
     */
    public static final int MIN_SKU_LENGTH = 3;
    public static final int MAX_SKU_LENGTH = 50;

    /**
     * Barcode length range
     */
    public static final int MIN_BARCODE_LENGTH = 8;
    public static final int MAX_BARCODE_LENGTH = 20;

    /**
     * Product name length
     */
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_PRODUCT_NAME_LENGTH = 100;


    /**
     * Price range
     */
    public static final String MIN_PRICE_STR = "0.01";
    public static final String MAX_PRICE_STR = "999999.99";

    /**
     * Quantity range
     */
    public static final int MAX_QUANTITY = 999999;

    /**
     * Category max length
     */
    public static final int MAX_CATEGORY_LENGTH = 50;

    /**
     * Brand max length
     */
    public static final int MAX_BRAND_LENGTH = 50;

    /**
     * URL max length
     */
    public static final int MAX_URL_LENGTH = 255;

    /**
     * Weight range
     */
    public static final String MIN_WEIGHT_STR = "0.0";
    public static final String MAX_WEIGHT_STR = "10000.0";

    /**
     * Volume range
     */
    public static final String MIN_VOLUME_STR = "0.0";
    public static final String MAX_VOLUME_STR = "1000.0";

    /**
     * Default values
     */
    public static final int DEFAULT_QUANTITY = 0;
    public static final boolean DEFAULT_ACTIVE = true;
    public static final boolean DEFAULT_HAS_VARIANTS = false;
    public static final boolean DEFAULT_REQUIRES_REFRIGERATION = false;
    public static final boolean DEFAULT_REQUIRES_FREEZING = false;
    public static final boolean DEFAULT_HAZARDOUS = false;
    public static final boolean DEFAULT_OVERSIZE = false;
    public static final boolean DEFAULT_LIQUID = false;
    public static final boolean DEFAULT_PALLETIZED = false;

    // =========================================================================
    // QUANTITY CONSTANTS
    // =========================================================================

    /**
     * Minimum positive quantity for adding items
     */
    public static final int MIN_POSITIVE_QUANTITY = 1;

    // =========================================================================
    // REVERSE RECEIPT CONSTANTS
    // =========================================================================

    /**
     * Maximum reason length
     */
    public static final int MAX_REASON_LENGTH = 500;

    /**
     * Maximum comments length
     */
    public static final int MAX_COMMENTS_LENGTH = 1000;

    // =========================================================================
    // STOCK WRITE-OFF CONSTANTS
    // =========================================================================

    /**
     * Maximum warehouse location length
     */
    public static final int MAX_WAREHOUSE_LOCATION_LENGTH = 100;

    /**
     * Maximum reason length
     */
    public static final int MAX_REASON_WRITEOFF_LENGTH = 100;

    // =========================================================================
    // SUPPLIER CONSTANTS
    // =========================================================================

    /**
     * Supplier name length range
     */
    public static final int MIN_SUPPLIER_NAME_LENGTH = 2;
    public static final int MAX_SUPPLIER_NAME_LENGTH = 200;

    /**
     * Email max length
     */
    public static final int MAX_EMAIL_LENGTH = 100;

    /**
     * Website max length
     */
    public static final int MAX_WEBSITE_LENGTH = 255;

    /**
     * Contact person max length
     */
    public static final int MAX_CONTACT_PERSON_LENGTH = 100;

    /**
     * Bank name max length
     */
    public static final int MAX_BANK_NAME_LENGTH = 200;

    /**
     * INN pattern (10 or 12 digits)
     */
    public static final String INN_PATTERN = "^\\d{10}$|^\\d{12}$";

    /**
     * KPP pattern (9 digits)
     */
    public static final String KPP_PATTERN = "^\\d{9}$";

    /**
     * OGRN pattern (13 digits)
     */
    public static final String OGRN_PATTERN = "^\\d{13}$";

    // =========================================================================
    // SUPPLIER PRODUCT CONSTANTS
    // =========================================================================

    /**
     * Maximum supplier SKU length
     */
    public static final int MAX_SUPPLIER_SKU_LENGTH = 100;

    /**
     * Maximum supplier category length
     */
    public static final int MAX_SUPPLIER_CATEGORY_LENGTH = 100;

    /**
     * Maximum supplier product name length
     */
    public static final int MAX_SUPPLIER_PRODUCT_NAME_LENGTH = 200;

    /**
     * Maximum country name length
     */
    public static final int MAX_COUNTRY_NAME_LENGTH = 100;

    /**
     * Maximum HS code length
     */
    public static final int MAX_HS_CODE_LENGTH = 50;

    // =========================================================================
    // USER CONSTANTS
    // =========================================================================


    /**
     * Name length range
     */
    public static final int MAX_NAME_LENGTH = 50;

    /**
     * Surname max length
     */
    public static final int MAX_SURNAME_LENGTH = 50;

// =========================================================================
    // CATEGORY WAREHOUSE CONSTANTS
    // =========================================================================

    /**
     * Default isDefault value
     */
    public static final boolean DEFAULT_IS_DEFAULT = false;

    /**
     * Default priority value
     */
    public static final int DEFAULT_PRIORITY = 0;

    // =========================================================================
    // CATEGORY CONSTANTS
    // =========================================================================

    /**
     * Maximum category name length
     */
    public static final int MAX_CATEGORY_NAME_LENGTH = 100;

    // =========================================================================
    // CELL CONSTANTS
    // =========================================================================

    /**
     * Maximum section length
     */
    public static final int MAX_SECTION_LENGTH = 10;

    /**
     * Maximum rack length
     */
    public static final int MAX_RACK_LENGTH = 10;

    /**
     * Maximum shelf length
     */
    public static final int MAX_SHELF_LENGTH = 10;

    /**
     * Maximum position length
     */
    public static final int MAX_POSITION_LENGTH = 10;

    // =========================================================================
    // WAREHOUSE CONSTANTS
    // =========================================================================

    /**
     * Warehouse code max length
     */
    public static final int MAX_WAREHOUSE_CODE_LENGTH = 50;

    /**
     * Warehouse name max length
     */
    public static final int MAX_WAREHOUSE_NAME_LENGTH = 100;

    /**
     * Barcode format max length
     */
    public static final int MAX_BARCODE_FORMAT_LENGTH = 20;

    // =========================================================================
    // PRODUCT MEDIA CONSTANTS
    // =========================================================================

    /**
     * Default sort order
     */
    public static final int DEFAULT_SORT_ORDER = 0;

    /**
     * Default isMain value
     */
    public static final boolean DEFAULT_IS_MAIN = false;

    /**
     * Default isThumbnail value
     */
    public static final boolean DEFAULT_IS_THUMBNAIL = false;

    /**
     * Default media type
     */
    public static final String DEFAULT_MEDIA_TYPE = "IMAGE";

    /**
     * Maximum alt text length
     */
    public static final int MAX_ALT_TEXT_LENGTH = 255;

    /**
     * Maximum caption length
     */
    public static final int MAX_CAPTION_LENGTH = 500;

    /**
     * Maximum metadata JSON length
     */
    public static final int MAX_METADATA_LENGTH = 2000;

    // =========================================================================
    // RECEIVE STOCK CONSTANTS
    // =========================================================================

    /**
     * Maximum cell code length
     */
    public static final int MAX_CELL_CODE_LENGTH = 50;

    /**
     * Maximum serial number length
     */
    public static final int MAX_SERIAL_NUMBER_LENGTH = 100;

    /**
     * Maximum quality certificate length
     */
    public static final int MAX_QUALITY_CERTIFICATE_LENGTH = 100;

    public static final int MAX_ENDPOINT_LENGTH =40;

    public static final String BANK_ACCOUNT_TYPE = "BANK_ACCOUNT";
    public static final String CASH_REGISTER_TYPE = "CASH_REGISTER";
    public static final String CASH_PAYMENT_TYPE = "CASH";

    // Transaction ID prefixes
    public static final String BANK_TXN_PREFIX = "BANK_TXN_";
    public static final String CASH_REGISTER_TXN_PREFIX = "CASH_REGISTER_TXN_";
    public static final String CASH_TXN_PREFIX = "CASH_TXN_";
    public static final String REFUND_TXN_PREFIX = "REFUND_TXN_";
    public static final String FALLBACK_TXN_PREFIX = "TXN_";


}