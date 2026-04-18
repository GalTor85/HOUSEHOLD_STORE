package ru.galtor85.household_store.constants;

import java.math.BigDecimal;

/**
 * Payment-related constants.
 *
 * @author G@LTor85
 
 */
public final class PaymentConstants {

    private PaymentConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // =========================================================================
    // PROVIDER CONSTANTS
    // =========================================================================

    public static final String CASH_PROVIDER_CODE = "cash";
    public static final String CASH_REGISTER_NAME = "Cash Register";
    public static final String FALLBACK_PAYMENT_URL_PATH = "/payment/";

    // =========================================================================
    // HTTP HEADERS
    // =========================================================================

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_API_SECRET = "X-API-Secret";
    public static final String MOCK_PAYMENT_URL = "https://mock-payment-gateway.com/pay";

    // =========================================================================
    // JSON FIELD NAMES
    // =========================================================================

    public static final String JSON_AMOUNT = "amount";
    public static final String JSON_CURRENCY = "currency";
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_PAYMENT_METHOD_ID = "payment_method_id";
    public static final String JSON_PAYMENT_IDENTIFIER = "payment_identifier";

    // =========================================================================
    // DEFAULT VALUES
    // =========================================================================

    public static final int DEFAULT_RANDOM_LENGTH = 8;
    public static final BigDecimal DEFAULT_FEE_PERCENT = BigDecimal.ZERO;
    public static final int DEFAULT_TIMEOUT = 30000;
    public static final int MAX_TRANSACTION_PREFIX_LENGTH = 4;
}

