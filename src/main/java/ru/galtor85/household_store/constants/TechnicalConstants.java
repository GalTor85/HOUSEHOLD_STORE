package ru.galtor85.household_store.constants;

import java.math.BigDecimal;

/**
 * Technical constants for the application.
 *
 * <p>This class contains constants that are purely technical and do not represent
 * business logic configuration. These values are fixed and should not be changed
 * without careful consideration of the impact on system functionality.</p>
 *
 * <p><b>Note:</b> Business configuration (cart expiry days, pagination defaults,
 * discount percentages) should be stored in {@code application.properties} and
 * loaded via {@link ru.galtor85.household_store.config.BusinessConfig}.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
public final class TechnicalConstants {

    private TechnicalConstants() {
        throw new UnsupportedOperationException("Utility class - instantiation not allowed");
    }

    // =========================================================================
    // BARCODE
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

    /**
     * Minimum barcode length
     */
    public static final int MIN_BARCODE_LENGTH = 8;

    /**
     * Maximum barcode length
     */
    public static final int MAX_BARCODE_LENGTH = 20;

    // =========================================================================
    // PRODUCT
    // =========================================================================

    /**
     * Minimum product name length
     */
    public static final int MIN_NAME_LENGTH = 2;

    /**
     * Maximum product name length
     */
    public static final int MAX_PRODUCT_NAME_LENGTH = 100;

    /**
     * Minimum SKU length
     */
    public static final int MIN_SKU_LENGTH = 3;

    /**
     * Maximum SKU length
     */
    public static final int MAX_SKU_LENGTH = 50;

    /**
     * Minimum price value
     */
    public static final String MIN_PRICE_STR = "0.01";

    /**
     * Maximum price value
     */
    public static final String MAX_PRICE_STR = "999999.99";

    /**
     * Maximum quantity value
     */
    public static final int MAX_QUANTITY = 999999;

    /**
     * Minimum quantity (0 removes item)
     */
    public static final int MIN_QUANTITY = 0;

    /**
     * Minimum positive quantity for adding items
     */
    public static final int MIN_POSITIVE_QUANTITY = 1;

    /**
     * Default quantity value
     */
    public static final int DEFAULT_QUANTITY = 0;

    /**
     * Maximum category name length
     */
    public static final int MAX_CATEGORY_LENGTH = 50;

    /**
     * Maximum brand name length
     */
    public static final int MAX_BRAND_LENGTH = 50;

    /**
     * Maximum URL length
     */
    public static final int MAX_URL_LENGTH = 255;

    /**
     * Minimum weight value
     */
    public static final String MIN_WEIGHT_STR = "0.0";

    /**
     * Maximum weight value
     */
    public static final String MAX_WEIGHT_STR = "10000.0";

    /**
     * Minimum volume value
     */
    public static final String MIN_VOLUME_STR = "0.0";

    /**
     * Maximum volume value
     */
    public static final String MAX_VOLUME_STR = "1000.0";

    /**
     * Default active flag
     */
    public static final boolean DEFAULT_ACTIVE = true;

    /**
     * Default hasVariants flag
     */
    public static final boolean DEFAULT_HAS_VARIANTS = false;

    /**
     * Default requiresRefrigeration flag
     */
    public static final boolean DEFAULT_REQUIRES_REFRIGERATION = false;

    /**
     * Default requiresFreezing flag
     */
    public static final boolean DEFAULT_REQUIRES_FREEZING = false;

    /**
     * Default hazardous flag
     */
    public static final boolean DEFAULT_HAZARDOUS = false;

    /**
     * Default oversize flag
     */
    public static final boolean DEFAULT_OVERSIZE = false;

    /**
     * Default liquid flag
     */
    public static final boolean DEFAULT_LIQUID = false;

    /**
     * Default palletized flag
     */
    public static final boolean DEFAULT_PALLETIZED = false;

    // =========================================================================
    // PRODUCT ATTRIBUTES
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
    // PRODUCT MEDIA
    // =========================================================================

    /**
     * Default sort order
     */
    public static final int DEFAULT_SORT_ORDER = 0;

    /**
     * Default isMain flag
     */
    public static final boolean DEFAULT_IS_MAIN = false;

    /**
     * Default isThumbnail flag
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

    public static final long BYTES_IN_KB = 1024L;
    public static final long BYTES_IN_MB = BYTES_IN_KB * 1024L;
    public static final long BYTES_IN_GB = BYTES_IN_MB * 1024L;

    // =========================================================================
    // REGEX PATTERNS
    // =========================================================================

    /**
     * Email validation pattern
     */
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";

    /**
     * Phone number validation pattern (international format)
     */
    public static final String PHONE_PATTERN = "^\\+?[0-9\\-\\s]{10,15}$";

    /**
     * Date validation pattern (YYYY-MM-DD)
     */
    public static final String DATE_PATTERN = "^\\d{4}-\\d{2}-\\d{2}$";

    /**
     * Date format pattern (DD.MM.YYYY HH:mm)
     */
    public static final String DATE_FORMAT_PATTERN = "dd.MM.yyyy HH:mm";

    /**
     * Password validation pattern (digit, lowercase, uppercase, special char, min 6 chars)
     */
    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&*!])[A-Za-z\\d@#$%^&*!]{6,}$";

    /**
     * Cell code pattern (SECTION-RACK-SHELF-POSITION)
     */
    public static final String CELL_CODE_PATTERN = "^[A-Z0-9]+(-[A-Z0-9]+)*$";

    /**
     * Pattern to check if string contains at least one digit
     */
    public static final String CONTAINS_DIGIT = ".*\\d.*";

    /**
     * Pattern to check if string contains at least one lowercase letter
     */
    public static final String CONTAINS_LOWERCASE = ".*[a-z].*";

    /**
     * Pattern to check if string contains at least one uppercase letter
     */
    public static final String CONTAINS_UPPERCASE = ".*[A-Z].*";

    /**
     * Pattern to keep only digits (remove all non-digit characters)
     */
    public static final String KEEP_ONLY_DIGITS = "[^0-9]";

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
     * Cash register number pattern (uppercase, numbers, hyphens, 3-20 chars)
     */
    public static final String CASH_REGISTER_NUMBER_PATTERN = "^[A-Z0-9-]{3,20}$";

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

    /**
     * Credit card number pattern (16 digits)
     */
    public static final String CREDIT_CARD_NUMBER_PATTERN = "^\\d{16}$";

    /**
     * Credit card expiry date pattern (MM/YY)
     */
    public static final String CREDIT_CARD_EXPIRY_PATTERN = "^(0[1-9]|1[0-2])/([0-9]{2})$";

    /**
     * Credit card CVV/CVC pattern (3 or 4 digits)
     */
    public static final String CREDIT_CARD_CVV_PATTERN = "^\\d{3,4}$";

    /**
     * Bank BIC pattern (9 digits)
     */
    public static final String BANK_BIC_PATTERN = "^\\d{9}$";

    // =========================================================================
    // STRING FORMATTING CONSTANTS
    // =========================================================================

    /**
     * Separator for order notes
     */
    public static final String NOTE_SEPARATOR = " --- ";

    /**
     * Line separator for multi-line notes
     */
    public static final String LINE_SEPARATOR = "\n";

    // =========================================================================
    // PASSWORD & SECURITY
    // =========================================================================

    /**
     * BCrypt strength for password encoding
     */
    public static final int BCRYPT_STRENGTH = 10;

    /**
     * Minimum password length (technical minimum)
     */
    public static final int TECHNICAL_MIN_PASSWORD_LENGTH = 6;

    /**
     * JWT key length in bytes (256 bits = 32 bytes for AES-256)
     */
    public static final int JWT_KEY_LENGTH = 32;

    /**
     * JWT token prefix for Authorization header
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT token type
     */
    public static final String TOKEN_TYPE = "Bearer";

    // =========================================================================
    // JWT CLAIMS
    // =========================================================================

    /**
     * JWT claim key for user role
     */
    public static final String JWT_CLAIM_ROLE = "role";

    /**
     * JWT claim key for user identifier (email or phone)
     */
    public static final String JWT_CLAIM_IDENTIFY = "identify";

    /**
     * JWT claim key for user ID
     */
    public static final String JWT_CLAIM_USER_ID = "userId";

    /**
     * JWT claim key for token type (access/refresh)
     */
    public static final String JWT_CLAIM_TYPE = "type";

    /**
     * JWT claim value for access token
     */
    public static final String JWT_TOKEN_TYPE_ACCESS = "access";

    /**
     * JWT claim value for refresh token
     */
    public static final String JWT_TOKEN_TYPE_REFRESH = "refresh";

    // =========================================================================
    // SECURITY ROLES
    // =========================================================================

    /**
     * Administrator role – full system access
     */
    public static final String ROLE_ADMIN = "ADMIN";

    /**
     * Manager role – operational access
     */
    public static final String ROLE_MANAGER = "MANAGER";

    /**
     * Regular user role – basic access
     */
    public static final String ROLE_USER = "USER";

    // =========================================================================
    // CORS CONFIGURATION
    // =========================================================================

    /**
     * Flag to allow credentials (cookies, authorization headers) in CORS requests.
     * When true, the browser includes cookies and HTTP authentication credentials.
     */
    public static final boolean CORS_ALLOW_CREDENTIALS = true;

    /**
     * URL pattern for applying CORS configuration to all endpoints.
     * Matches any path including sub-paths.
     */
    public static final String CORS_URL_PATTERN = "/**";

    // =========================================================================
    // USER
    // =========================================================================

    /**
     * Maximum name length
     */
    public static final int MAX_NAME_LENGTH = 50;

    /**
     * Maximum surname length
     */
    public static final int MAX_SURNAME_LENGTH = 50;

    /**
     * Maximum email length
     */
    public static final int MAX_EMAIL_LENGTH = 100;

    /**
     * Default source for self-registration.
     */
    public static final String SELF_REGISTRATION_SOURCE = "self-registration";

    // =========================================================================
    // PHONE
    // =========================================================================

    /**
     * Minimum phone number length (digits only)
     */
    public static final int MIN_PHONE_LENGTH = 5;

    /**
     * Maximum phone number length (digits only)
     */
    public static final int MAX_PHONE_LENGTH = 15;

    // =========================================================================
    // ADDRESS & NOTES
    // =========================================================================

    /**
     * Maximum address length (shipping, billing)
     */
    public static final int MAX_ADDRESS_LENGTH = 255;

    /**
     * Maximum notes length
     */
    public static final int MAX_NOTES_LENGTH = 1000;

    /**
     * Maximum reason length
     */
    public static final int MAX_REASON_LENGTH = 500;

    /**
     * Maximum comments length
     */
    public static final int MAX_COMMENTS_LENGTH = 1000;

    /**
     * Maximum description length
     */
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    // =========================================================================
    // WAREHOUSE & CELL
    // =========================================================================

    /**
     * Maximum warehouse code length
     */
    public static final int MAX_WAREHOUSE_CODE_LENGTH = 50;

    /**
     * Maximum warehouse name length
     */
    public static final int MAX_WAREHOUSE_NAME_LENGTH = 100;

    /**
     * Maximum barcode format length
     */
    public static final int MAX_BARCODE_FORMAT_LENGTH = 20;

    /**
     * Maximum cell code length
     */
    public static final int MAX_CELL_CODE_LENGTH = 50;

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

    /**
     * Maximum warehouse location length
     */
    public static final int MAX_WAREHOUSE_LOCATION_LENGTH = 100;

    // =========================================================================
    // SUPPLIER
    // =========================================================================

    /**
     * Minimum supplier name length
     */
    public static final int MIN_SUPPLIER_NAME_LENGTH = 2;

    /**
     * Maximum supplier name length
     */
    public static final int MAX_SUPPLIER_NAME_LENGTH = 200;

    /**
     * Maximum website length
     */
    public static final int MAX_WEBSITE_LENGTH = 255;

    /**
     * Maximum contact person length
     */
    public static final int MAX_CONTACT_PERSON_LENGTH = 100;

    /**
     * Maximum bank name length
     */
    public static final int MAX_BANK_NAME_LENGTH = 200;

    // =========================================================================
    // SUPPLIER PRODUCT
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
    // CATEGORY
    // =========================================================================

    /**
     * Maximum category name length
     */
    public static final int MAX_CATEGORY_NAME_LENGTH = 100;

    // =========================================================================
    // CATEGORY WAREHOUSE
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
    // PAYMENT & CURRENCY
    // =========================================================================

    /**
     * Maximum payment method name length
     */
    public static final int MAX_PAYMENT_METHOD_LENGTH = 50;

    /**
     * Currency code length (ISO 4217)
     */
    public static final int CURRENCY_CODE_LENGTH = 3;

    /**
     * Maximum currency name length
     */
    public static final int MAX_CURRENCY_NAME_LENGTH = 100;

    /**
     * Minimum currency symbol length
     */
    public static final int MIN_SYMBOL_LENGTH = 1;

    /**
     * Maximum currency symbol length
     */
    public static final int MAX_SYMBOL_LENGTH = 5;

    /**
     * Default currency
     */
    public static final String DEFAULT_CURRENCY = "RUB";

    /**
     * Default currency code
     */
    public static final String DEFAULT_CURRENCY_CODE = "RUB";

    /**
     * Order status cancelled
     */
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    // =========================================================================
    // CASH REGISTER
    // =========================================================================

    /**
     * Maximum cash register number length
     */
    public static final int MAX_REGISTER_NUMBER_LENGTH = 20;

    /**
     * Maximum location length
     */
    public static final int MAX_LOCATION_LENGTH = 200;

    /**
     * Minimum balance value
     */
    public static final String MIN_BALANCE_STR = "0.00";

    /**
     * Default balance value
     */
    public static final String DEFAULT_BALANCE_STR = "0.00";

    /**
     * Minimum amount for any transaction (1 cent/kopek)
     */
    public static final String MIN_AMOUNT_STR = "0.01";

    /**
     * Minimum amount as BigDecimal
     */
    public static final BigDecimal MIN_AMOUNT = new BigDecimal(MIN_AMOUNT_STR);

    // =========================================================================
    // STOCK MOVEMENT
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

    /**
     * Maximum serial number length
     */
    public static final int MAX_SERIAL_NUMBER_LENGTH = 100;

    /**
     * Maximum quality certificate length
     */
    public static final int MAX_QUALITY_CERTIFICATE_LENGTH = 100;

    // =========================================================================
    // STOCK WRITE-OFF
    // =========================================================================

    /**
     * Maximum reason length for write-off
     */
    public static final int MAX_REASON_WRITEOFF_LENGTH = 100;

    // =========================================================================
    // PROMO & PRICE
    // =========================================================================

    /**
     * Maximum promo code length
     */
    public static final int MAX_PROMO_CODE_LENGTH = 50;

    // =========================================================================
    // ID GENERATION
    // =========================================================================

    /**
     * Default length for random ID suffix
     */
    public static final int DEFAULT_RANDOM_SUFFIX_LENGTH = 8;

    // =========================================================================
    // HTTP & ENCODING
    // =========================================================================

    /**
     * UTF-8 character encoding
     */
    public static final String UTF_8_ENCODING = "UTF-8";

    /**
     * JSON content type
     */
    public static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * JSON content type with UTF-8 charset
     */
    public static final String CONTENT_TYPE_JSON_UTF8 = CONTENT_TYPE_JSON + ";charset=" + UTF_8_ENCODING;

    /**
     * Maximum endpoint length
     */
    public static final int MAX_ENDPOINT_LENGTH = 40;

    // =========================================================================
    // TRANSACTION & PAYMENT TYPES
    // =========================================================================

    /**
     * Bank account type identifier
     */
    public static final String BANK_ACCOUNT_TYPE = "BANK_ACCOUNT";

    /**
     * Cash register type identifier
     */
    public static final String CASH_REGISTER_TYPE = "CASH_REGISTER";

    /**
     * Cash payment type identifier
     */
    public static final String CASH_PAYMENT_TYPE = "CASH";

    // =========================================================================
    // TRANSACTION ID PREFIXES
    // =========================================================================

    /**
     * Bank transaction ID prefix
     */
    public static final String BANK_TXN_PREFIX = "BANK_TXN_";

    /**
     * Cash register transaction ID prefix
     */
    public static final String CASH_REGISTER_TXN_PREFIX = "CASH_REGISTER_TXN_";

    /**
     * Cash transaction ID prefix
     */
    public static final String CASH_TXN_PREFIX = "CASH_TXN_";

    /**
     * Refund transaction ID prefix
     */
    public static final String REFUND_TXN_PREFIX = "REFUND_TXN_";

    /**
     * Fallback transaction ID prefix
     */
    public static final String FALLBACK_TXN_PREFIX = "TXN_";

    // =========================================================================
    // PAYMENT METHOD VALIDATION
    // =========================================================================

    /**
     * Maximum length for payment method name
     */
    public static final int MAX_PAYMENT_METHOD_NAME_LENGTH = 100;

    /**
     * Minimum number of months for installment payment
     */
    public static final int MIN_INSTALLMENT_MONTHS = 1;

    /**
     * Maximum number of months for installment payment
     */
    public static final int MAX_INSTALLMENT_MONTHS = 36;

    /**
     * Maximum length for text fields (description, reason, etc.)
     */
    public static final int MAX_TEXT_LENGTH = 500;

    /**
     * Maximum length for cardholder name
     */
    public static final int MAX_CARDHOLDER_NAME_LENGTH = 100;

    // =========================================================================
    // RESERVATION CONSTANTS
    // =========================================================================

    /**
     * Default reservation period in days for cash payment
     */
    public static final int DEFAULT_RESERVATION_DAYS = 7;

    // =========================================================================
    // SYSTEM & AUDIT
    // =========================================================================

    /**
     * System creator identifier for audit trail
     */
    public static final String SYSTEM_CREATOR = "system";

    public static final String DEFAULT_REASON_FOR_CREATE = "creation by " + SYSTEM_CREATOR;

    /**
     * Default active status
     */
    public static final boolean DEFAULT_ACTIVE_STATUS = true;
}