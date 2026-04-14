package ru.galtor85.household_store.constants;

import static ru.galtor85.household_store.constants.ApiConstants.API_BASE;

/**
 * Centralized constants for API endpoints.
 * Used by both SecurityConfig and Controllers to ensure consistency.
 */
public final class EndpointConstants {

    private EndpointConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // =========================================================================
    // AUTH ENDPOINTS
    // =========================================================================
    public static final String AUTH_REGISTER = API_BASE + "/auth/register";
    public static final String AUTH_LOGIN = API_BASE + "/auth/login";
    public static final String AUTH_REFRESH = API_BASE + "/auth/refresh";

    // =========================================================================
    // PUBLIC ENDPOINTS
    // =========================================================================
    public static final String HOME = API_BASE;
    public static final String HEALTH = API_BASE + "/health";
    public static final String INFO = API_BASE + "/info";
    public static final String PING = API_BASE + "/ping";
    public static final String MEDIA = API_BASE + "/media/**";

    // =========================================================================
    // ADMIN ENDPOINTS
    // =========================================================================
    public static final String ADMIN_ROOT = API_BASE + "/admin/**";
    public static final String ADMIN_USERS = API_BASE + "/admin/users/**";

    // =========================================================================
    // MANAGER ENDPOINTS
    // =========================================================================
    public static final String MANAGER_ROOT = API_BASE + "/manager/**";
    public static final String MANAGER_PRODUCTS = API_BASE + "/manager/products/**";
    public static final String MANAGER_ORDERS = API_BASE + "/manager/orders/**";

    // =========================================================================
    // USER ENDPOINTS
    // =========================================================================
    public static final String USER_ROOT = API_BASE + "/users/**";
    public static final String USER_PAY_INVOICE = API_BASE + "/users/invoices/{invoiceNumber}/pay";

    // =========================================================================
    // SWAGGER & ACTUATOR
    // =========================================================================
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String API_DOCS = "/v3/api-docs/**";
    public static final String API_DOCS_JSON = "/api-docs/**";
    public static final String ACTUATOR = "/actuator/**";
    public static final String ERROR = "/error";
    public static final String DEBUG = API_BASE + "/debug/**";

    // =========================================================================
    // BASE PATHS (for @RequestMapping in controllers)
    // =========================================================================
    public static final String CONTROL_AUTH = API_BASE + "/auth";
    public static final String CONTROL_ADMIN = API_BASE + "/admin/users";
    public static final String CONTROL_BANK_ACCOUNTS = API_BASE + "/finance/bank-accounts";
    public static final String CONTROL_CURRENCIES = API_BASE + "/currencies";
    public static final String CONTROL_FINANCE = API_BASE + "/finance";
    public static final String CONTROL_HOME = API_BASE;
    public static final String CONTROL_MANAGER = API_BASE + "/manager";
    public static final String CONTROL_MANAGER_PAYMENTS = API_BASE + "/manager/payments";
    public static final String CONTROL_MANAGER_PURCHASES = API_BASE + "/manager/purchases";
    public static final String CONTROL_MANAGER_ORDERS = API_BASE + "/manager/orders";
    public static final String CONTROL_MEDIA = API_BASE + "/media";
    public static final String CONTROL_USERS = API_BASE + "/users";

    // =========================================================================
// USER ENDPOINTS (дополнительные)
// =========================================================================
    public static final String USER_ME = API_BASE + "/users/me";
    public static final String USER_CART = API_BASE + "/users/cart";
    public static final String USER_ORDER_FROM_CART = API_BASE + "/users/orders/from-cart";

    // =========================================================================
// FINANCE ENDPOINTS (дополнительные)
// =========================================================================
    public static final String FINANCE_INVOICES = API_BASE + "/finance/invoices";

    // =========================================================================
// CURRENCY ENDPOINTS (дополнительные)
// =========================================================================
    public static final String CURRENCIES = API_BASE + "/currencies";

    // =========================================================================
// BANK ACCOUNT ENDPOINTS (дополнительные)
// =========================================================================
    public static final String BANK_ACCOUNTS = API_BASE + "/finance/bank-accounts";

    // =========================================================================
// MANAGER PURCHASE ENDPOINTS (дополнительные)
// =========================================================================
    public static final String MANAGER_PURCHASE_ROOT = API_BASE + "/manager/purchases";

    // =========================================================================
// MANAGER PAYMENT ENDPOINTS (дополнительные)
// =========================================================================
    public static final String MANAGER_PAYMENT_SUPPLIER_BANK = API_BASE + "/manager/payments/supplier/bank";
    public static final String MANAGER_PAYMENT_CUSTOMER_CASH = API_BASE + "/manager/payments/customer/cash";

    // =========================================================================
// MANAGER STOCK ENDPOINTS (дополнительные)
// =========================================================================
    public static final String MANAGER_STOCK_SUMMARY = API_BASE + "/manager/warehouses/stock/summary";
    public static final String MANAGER_WAREHOUSES = API_BASE + "/manager/warehouses";

    // =========================================================================
// MEDIA ENDPOINTS (дополнительные)
// =========================================================================
    public static final String MEDIA_PRODUCT = API_BASE + "/media/product/{productId}";

    // =========================================================================
    // ARRAY FOR SECURITY CONFIG (ALL PUBLIC ENDPOINTS)
    // =========================================================================
    public static final String[] PUBLIC_ENDPOINTS = {
            AUTH_REGISTER,
            AUTH_LOGIN,
            AUTH_REFRESH,
            HOME,
            MEDIA,
            HEALTH,
            INFO,
            PING,
            SWAGGER_UI,
            SWAGGER_UI_HTML,
            API_DOCS,
            API_DOCS_JSON,
            ACTUATOR,
            ERROR,
            DEBUG
    };
}