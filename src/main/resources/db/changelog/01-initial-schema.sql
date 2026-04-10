-- ====================================================
-- DATABASE SCHEMA INITIALIZATION
-- ====================================================

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS household_schema;

-- ====================================================
-- USERS & AUTHENTICATION
-- ====================================================

-- Users table
CREATE TABLE IF NOT EXISTS household_schema.users (
                                                      id BIGSERIAL PRIMARY KEY,
                                                      email VARCHAR(255) UNIQUE,
    mobile_number VARCHAR(20) UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    surname VARCHAR(50),
    birth_date DATE,
    address VARCHAR(500),
    creator VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Security Users table
CREATE TABLE IF NOT EXISTS household_schema.security_users (
                                                               id BIGSERIAL PRIMARY KEY,
                                                               user_id BIGINT NOT NULL UNIQUE,
                                                               password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_security_user FOREIGN KEY (user_id) REFERENCES household_schema.users(id) ON DELETE CASCADE
    );

-- User Type Assignments table
CREATE TABLE IF NOT EXISTS household_schema.user_type_assignments (
                                                                      id BIGSERIAL PRIMARY KEY,
                                                                      user_id BIGINT NOT NULL,
                                                                      user_type VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    reason TEXT,
    UNIQUE(user_id, user_type),
    CONSTRAINT fk_user_type_user FOREIGN KEY (user_id) REFERENCES household_schema.users(id)
    );

-- ====================================================
-- PRODUCTS & CATALOG
-- ====================================================

-- Products table
CREATE TABLE IF NOT EXISTS household_schema.products (
                                                         id BIGSERIAL PRIMARY KEY,
                                                         sku VARCHAR(50) NOT NULL UNIQUE,
    barcode VARCHAR(20) UNIQUE,
    barcode_format VARCHAR(20),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    quantity_in_stock INTEGER DEFAULT 0,
    category VARCHAR(50),
    brand VARCHAR(50),
    image_url VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    has_variants BOOLEAN DEFAULT FALSE,
    parent_product_id BIGINT,
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    weight_kg DOUBLE PRECISION,
    volume_m3 DOUBLE PRECISION,
    requires_refrigeration BOOLEAN DEFAULT FALSE,
    requires_freezing BOOLEAN DEFAULT FALSE,
    is_hazardous BOOLEAN DEFAULT FALSE,
    is_oversize BOOLEAN DEFAULT FALSE,
    is_liquid BOOLEAN DEFAULT FALSE,
    is_palletized BOOLEAN DEFAULT FALSE,
    supplier_id BIGINT,
    supplier_price DECIMAL(10,2),
    supplier_sku VARCHAR(50),
    warehouse_id BIGINT,
    category_warehouse_id BIGINT,
    CONSTRAINT fk_product_parent FOREIGN KEY (parent_product_id) REFERENCES household_schema.products(id)
    );

-- Product Attributes table
CREATE TABLE IF NOT EXISTS household_schema.product_attributes (
                                                                   id BIGSERIAL PRIMARY KEY,
                                                                   product_id BIGINT NOT NULL,
                                                                   name VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL,
    attribute_order INTEGER,
    is_required BOOLEAN,
    is_variant BOOLEAN,
    CONSTRAINT fk_attribute_product FOREIGN KEY (product_id) REFERENCES household_schema.products(id) ON DELETE CASCADE
    );

-- Product Media table
CREATE TABLE IF NOT EXISTS household_schema.product_media (
                                                              id BIGSERIAL PRIMARY KEY,
                                                              product_id BIGINT NOT NULL,
                                                              uploaded_by BIGINT,
                                                              media_type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255),
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    alt_text VARCHAR(255),
    caption VARCHAR(255),
    sort_order INTEGER,
    is_main BOOLEAN,
    width INTEGER,
    height INTEGER,
    duration INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_media_product FOREIGN KEY (product_id) REFERENCES household_schema.products(id) ON DELETE CASCADE
    );

-- ====================================================
-- WAREHOUSE & INVENTORY
-- ====================================================

-- Warehouses table
CREATE TABLE IF NOT EXISTS household_schema.warehouses (
                                                           id BIGSERIAL PRIMARY KEY,
                                                           code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    barcode VARCHAR(50) NOT NULL UNIQUE,
    barcode_format VARCHAR(20),
    address VARCHAR(500) NOT NULL,
    contact_person VARCHAR(100),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    total_capacity INTEGER,
    used_capacity INTEGER DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Storage Cells table
CREATE TABLE IF NOT EXISTS household_schema.storage_cells (
                                                              id BIGSERIAL PRIMARY KEY,
                                                              warehouse_id BIGINT NOT NULL,
                                                              code VARCHAR(20) NOT NULL,
    barcode VARCHAR(50) NOT NULL UNIQUE,
    barcode_format VARCHAR(20),
    section VARCHAR(10),
    rack VARCHAR(10),
    shelf VARCHAR(10),
    position VARCHAR(10),
    cell_type VARCHAR(20) NOT NULL,
    max_weight_kg DOUBLE PRECISION,
    max_volume_m3 DOUBLE PRECISION,
    current_product_id BIGINT,
    current_quantity INTEGER DEFAULT 0,
    is_occupied BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    last_inventory_date TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(warehouse_id, code),
    CONSTRAINT fk_cell_warehouse FOREIGN KEY (warehouse_id) REFERENCES household_schema.warehouses(id),
    CONSTRAINT fk_cell_product FOREIGN KEY (current_product_id) REFERENCES household_schema.products(id)
    );

-- Category Warehouse table
CREATE TABLE IF NOT EXISTS household_schema.category_warehouse (
                                                                   id BIGSERIAL PRIMARY KEY,
                                                                   category VARCHAR(100) NOT NULL UNIQUE,
    warehouse_id BIGINT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    priority INTEGER DEFAULT 0,
    CONSTRAINT fk_category_warehouse FOREIGN KEY (warehouse_id) REFERENCES household_schema.warehouses(id)
    );

-- ====================================================
-- ORDERS & SALES
-- ====================================================

-- Sales Orders table
CREATE TABLE IF NOT EXISTS household_schema.sales_orders (
                                                             id BIGSERIAL PRIMARY KEY,
                                                             order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    order_type VARCHAR(20) NOT NULL DEFAULT 'RETAIL',
    status VARCHAR(30) NOT NULL,
    subtotal DECIMAL(10,2),
    discount_amount DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    shipping_amount DECIMAL(10,2),
    tax_amount DECIMAL(10,2),
    payment_method VARCHAR(50),
    payment_details VARCHAR(255),
    shipping_address VARCHAR(500),
    billing_address VARCHAR(500),
    tracking_number VARCHAR(100),
    estimated_delivery TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_by BIGINT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_order_user FOREIGN KEY (user_id) REFERENCES household_schema.users(id)
    );

-- Sales Order Items table
CREATE TABLE IF NOT EXISTS household_schema.sales_order_items (
                                                                  id BIGSERIAL PRIMARY KEY,
                                                                  sales_order_id BIGINT NOT NULL,
                                                                  product_id BIGINT NOT NULL,
                                                                  quantity INTEGER NOT NULL,
                                                                  price DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2),
    product_name VARCHAR(100),
    product_sku VARCHAR(50),
    total_price DECIMAL(10,2),
    notes TEXT,
    UNIQUE(sales_order_id, product_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (sales_order_id) REFERENCES household_schema.sales_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES household_schema.products(id)
    );

-- ====================================================
-- SHOPPING CART
-- ====================================================

-- Carts table
CREATE TABLE IF NOT EXISTS household_schema.carts (
                                                      id BIGSERIAL PRIMARY KEY,
                                                      user_id BIGINT NOT NULL,
                                                      status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_amount DECIMAL(10,2),
    items_count INTEGER,
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES household_schema.users(id)
    );

-- Cart Items table
CREATE TABLE IF NOT EXISTS household_schema.cart_items (
                                                           id BIGSERIAL PRIMARY KEY,
                                                           cart_id BIGINT NOT NULL,
                                                           product_id BIGINT NOT NULL,
                                                           quantity INTEGER NOT NULL,
                                                           price DECIMAL(10,2) NOT NULL,
    product_name VARCHAR(100),
    sku VARCHAR(50),
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(cart_id, product_id),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES household_schema.carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES household_schema.products(id)
    );

-- ====================================================
-- FINANCE & PAYMENTS
-- ====================================================

-- Bank Accounts table
CREATE TABLE IF NOT EXISTS household_schema.bank_accounts (
                                                              id BIGSERIAL PRIMARY KEY,
                                                              account_number VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    bank_name VARCHAR(200) NOT NULL,
    bic VARCHAR(9),
    correspondent_account VARCHAR(20),
    iban VARCHAR(34),
    swift_code VARCHAR(11),
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
    is_active BOOLEAN DEFAULT TRUE,
    account_type VARCHAR(20) NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Bank Transactions table
CREATE TABLE IF NOT EXISTS household_schema.bank_transactions (
                                                                  id BIGSERIAL PRIMARY KEY,
                                                                  bank_account_id BIGINT NOT NULL,
                                                                  transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_before DECIMAL(15,2),
    balance_after DECIMAL(15,2),
    description VARCHAR(500),
    reference_id BIGINT,
    reference_type VARCHAR(50),
    from_account_id BIGINT,
    to_account_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_txn_account FOREIGN KEY (bank_account_id) REFERENCES household_schema.bank_accounts(id)
    );

-- Invoices table
CREATE TABLE IF NOT EXISTS household_schema.invoices (
                                                         id BIGSERIAL PRIMARY KEY,
                                                         invoice_number VARCHAR(50) NOT NULL UNIQUE,
    purchase_order_id BIGINT,
    sales_order_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    issue_date TIMESTAMP NOT NULL,
    due_date TIMESTAMP,
    paid_date TIMESTAMP,
    description VARCHAR(500),
    notes TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Cash Registers table
CREATE TABLE IF NOT EXISTS household_schema.cash_registers (
                                                               id BIGSERIAL PRIMARY KEY,
                                                               register_number VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(200),
    is_active BOOLEAN DEFAULT FALSE,
    opening_balance DECIMAL(10,2),
    closing_balance DECIMAL(10,2),
    cashier_id BIGINT,
    opened_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Cash Transactions table
CREATE TABLE IF NOT EXISTS household_schema.cash_transactions (
                                                                  id BIGSERIAL PRIMARY KEY,
                                                                  cash_register_id BIGINT NOT NULL,
                                                                  invoice_id BIGINT,
                                                                  transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
    payment_method VARCHAR(20),
    cashier_id BIGINT,
    description VARCHAR(500),
    notes TEXT,
    original_transaction_id BIGINT,
    balance_before DECIMAL(10,2),
    balance_after DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cash_txn_register FOREIGN KEY (cash_register_id) REFERENCES household_schema.cash_registers(id),
    CONSTRAINT fk_cash_txn_invoice FOREIGN KEY (invoice_id) REFERENCES household_schema.invoices(id)
    );

-- Currencies table
CREATE TABLE IF NOT EXISTS household_schema.currencies (
                                                           id BIGSERIAL PRIMARY KEY,
                                                           code VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(5) NOT NULL,
    is_base BOOLEAN DEFAULT FALSE,
    exchange_rate DECIMAL(10,4),
    is_active BOOLEAN DEFAULT TRUE,
    decimal_places INTEGER DEFAULT 2,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- ====================================================
-- SUPPLIERS & PROCUREMENT
-- ====================================================

-- Suppliers table
CREATE TABLE IF NOT EXISTS household_schema.suppliers (
                                                          id BIGSERIAL PRIMARY KEY,
                                                          name VARCHAR(200) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    website VARCHAR(200),
    contact_person VARCHAR(100),
    inn VARCHAR(12) UNIQUE,
    kpp VARCHAR(9),
    ogrn VARCHAR(13),
    legal_address VARCHAR(500),
    actual_address VARCHAR(500),
    bank_name VARCHAR(200),
    bank_bic VARCHAR(9),
    bank_account VARCHAR(20),
    correspondent_account VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rating DOUBLE PRECISION,
    rating_count INTEGER,
    delivery_time INTEGER,
    min_order_amount DECIMAL(10,2),
    payment_delay INTEGER,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Supplier Products table
CREATE TABLE IF NOT EXISTS household_schema.supplier_products (
                                                                  id BIGSERIAL PRIMARY KEY,
                                                                  supplier_id BIGINT NOT NULL,
                                                                  product_id BIGINT NOT NULL,
                                                                  supplier_price DECIMAL(10,2),
    supplier_sku VARCHAR(50),
    is_main_supplier BOOLEAN DEFAULT FALSE,
    delivery_time INTEGER,
    min_order_quantity INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(supplier_id, product_id),
    CONSTRAINT fk_supplier_product_supplier FOREIGN KEY (supplier_id) REFERENCES household_schema.suppliers(id) ON DELETE CASCADE,
    CONSTRAINT fk_supplier_product_product FOREIGN KEY (product_id) REFERENCES household_schema.products(id) ON DELETE CASCADE
    );

-- ====================================================
-- INVENTORY & STOCK MANAGEMENT
-- ====================================================

-- Product Stocks table
CREATE TABLE IF NOT EXISTS household_schema.product_stocks (
                                                               id BIGSERIAL PRIMARY KEY,
                                                               product_id BIGINT NOT NULL,
                                                               warehouse_id BIGINT NOT NULL,
                                                               quantity INTEGER NOT NULL DEFAULT 0,
                                                               reserved_quantity INTEGER DEFAULT 0,
                                                               available_quantity INTEGER,
                                                               min_stock_level INTEGER,
                                                               max_stock_level INTEGER,
                                                               reorder_point INTEGER,
                                                               location_in_warehouse VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, warehouse_id),
    CONSTRAINT fk_product_stock_product FOREIGN KEY (product_id) REFERENCES household_schema.products(id),
    CONSTRAINT fk_product_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES household_schema.warehouses(id)
    );

-- Stock Movements table
CREATE TABLE IF NOT EXISTS household_schema.stock_movements (
                                                                id BIGSERIAL PRIMARY KEY,
                                                                product_id BIGINT NOT NULL,
                                                                warehouse_id BIGINT,
                                                                from_cell_id BIGINT,
                                                                to_cell_id BIGINT,
                                                                quantity INTEGER NOT NULL,
                                                                movement_type VARCHAR(20) NOT NULL,
    reference_number VARCHAR(50),
    reference_type VARCHAR(20),
    reference_id BIGINT,
    batch_number VARCHAR(50),
    document_number VARCHAR(50),
    performed_by BIGINT,
    notes TEXT,
    original_movement_id BIGINT,
    returned_quantity INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movement_product FOREIGN KEY (product_id) REFERENCES household_schema.products(id),
    CONSTRAINT fk_movement_from_cell FOREIGN KEY (from_cell_id) REFERENCES household_schema.storage_cells(id),
    CONSTRAINT fk_movement_to_cell FOREIGN KEY (to_cell_id) REFERENCES household_schema.storage_cells(id)
    );

-- ====================================================
-- SECURITY & TOKENS
-- ====================================================

-- Blacklisted Tokens table
CREATE TABLE IF NOT EXISTS household_schema.blacklisted_tokens (
                                                                   id BIGSERIAL PRIMARY KEY,
                                                                   token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL
    );

-- ====================================================
-- ORDER ROLLBACKS
-- ====================================================

-- Rollback Approvals table
CREATE TABLE IF NOT EXISTS household_schema.rollback_approvals (
                                                                   id BIGSERIAL PRIMARY KEY,
                                                                   order_id BIGINT NOT NULL,
                                                                   current_status VARCHAR(30) NOT NULL,
    target_status VARCHAR(30) NOT NULL,
    requested_by BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    comments TEXT,
    approval_status VARCHAR(20) NOT NULL,
    reviewed_by BIGINT,
    admin_comments VARCHAR(500),
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP
    );

-- ====================================================
-- PRICE RULES & PROMOTIONS
-- ====================================================

-- Price Rules table
CREATE TABLE IF NOT EXISTS household_schema.price_rules (
                                                            id BIGSERIAL PRIMARY KEY,
                                                            name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    min_quantity INTEGER,
    max_quantity INTEGER,
    min_order_amount DECIMAL(10,2),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Price Rule User Types table
CREATE TABLE IF NOT EXISTS household_schema.price_rule_user_types (
                                                                      price_rule_id BIGINT NOT NULL,
                                                                      user_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_price_rule_user_type FOREIGN KEY (price_rule_id) REFERENCES household_schema.price_rules(id)
    );

-- Product Price Rules table (FIXED - with excluded column)
CREATE TABLE IF NOT EXISTS household_schema.product_price_rules (
                                                                    id BIGSERIAL PRIMARY KEY,
                                                                    product_id BIGINT NOT NULL,
                                                                    price_rule_id BIGINT NOT NULL,
                                                                    excluded BOOLEAN DEFAULT FALSE,
                                                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                                    CONSTRAINT fk_product_price_rule_product FOREIGN KEY (product_id) REFERENCES household_schema.products(id),
    CONSTRAINT fk_product_price_rule_rule FOREIGN KEY (price_rule_id) REFERENCES household_schema.price_rules(id)
    );

-- Promo Codes table
CREATE TABLE IF NOT EXISTS household_schema.promo_codes (
                                                            id BIGSERIAL PRIMARY KEY,
                                                            code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    max_uses INTEGER,
    used_count INTEGER DEFAULT 0,
    per_user_limit INTEGER,
    min_order_amount DECIMAL(10,2),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    is_combined BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Promo Code User Types table
CREATE TABLE IF NOT EXISTS household_schema.promo_code_user_types (
                                                                      promo_code_id BIGINT NOT NULL,
                                                                      user_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_promo_code_user_type FOREIGN KEY (promo_code_id) REFERENCES household_schema.promo_codes(id)
    );

-- Promo Code Usages table
CREATE TABLE IF NOT EXISTS household_schema.promo_code_usages (
                                                                  id BIGSERIAL PRIMARY KEY,
                                                                  promo_code_id BIGINT NOT NULL,
                                                                  user_id BIGINT NOT NULL,
                                                                  order_id BIGINT,
                                                                  used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                                  CONSTRAINT fk_promo_usage_code FOREIGN KEY (promo_code_id) REFERENCES household_schema.promo_codes(id),
    CONSTRAINT fk_promo_usage_user FOREIGN KEY (user_id) REFERENCES household_schema.users(id)
    );

-- ====================================================
-- PAYMENT METHODS
-- ====================================================

-- Payment Methods table
CREATE TABLE IF NOT EXISTS household_schema.payment_methods (
                                                                id BIGSERIAL PRIMARY KEY,
                                                                name VARCHAR(255) NOT NULL,
    method_type VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,
    currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
    processing_fee DECIMAL(5,2),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Credit Cards table
CREATE TABLE IF NOT EXISTS household_schema.credit_cards (
                                                             id BIGSERIAL PRIMARY KEY,
                                                             card_number VARCHAR(19) NOT NULL,
    card_holder_name VARCHAR(100) NOT NULL,
    expiry_month INTEGER NOT NULL,
    expiry_year INTEGER NOT NULL,
    cvv VARCHAR(4) NOT NULL,
    card_brand VARCHAR(20),
    issuer_bank VARCHAR(100),
    tokenized BOOLEAN DEFAULT FALSE,
    payment_token VARCHAR(255),
    CONSTRAINT fk_credit_card_method FOREIGN KEY (id) REFERENCES household_schema.payment_methods(id)
    );

-- Electronic Payments table
CREATE TABLE IF NOT EXISTS household_schema.electronic_payments (
                                                                    id BIGSERIAL PRIMARY KEY,
                                                                    wallet_id VARCHAR(100) NOT NULL,
    wallet_type VARCHAR(30) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(100),
    api_key VARCHAR(255),
    webhook_url VARCHAR(500),
    is_verified BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_electronic_method FOREIGN KEY (id) REFERENCES household_schema.payment_methods(id)
    );

-- Payment Transactions table
CREATE TABLE IF NOT EXISTS household_schema.payment_transactions (
                                                                     id BIGSERIAL PRIMARY KEY,
                                                                     payment_method_id BIGINT NOT NULL,
                                                                     invoice_id BIGINT,
                                                                     order_id BIGINT,
                                                                     order_type VARCHAR(10),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_transaction_id VARCHAR(255),
    provider_payment_url VARCHAR(500),
    description VARCHAR(1000),
    error_message TEXT,
    processing_fee DECIMAL(10,2),
    net_amount DECIMAL(10,2),
    original_transaction_id BIGINT,
    refund_transaction_id BIGINT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_method FOREIGN KEY (payment_method_id) REFERENCES household_schema.payment_methods(id),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES household_schema.invoices(id)
    );

-- ====================================================
-- SUPPLIER RATINGS
-- ====================================================

-- Supplier Ratings table
CREATE TABLE IF NOT EXISTS household_schema.supplier_ratings (
                                                                 id BIGSERIAL PRIMARY KEY,
                                                                 supplier_id BIGINT NOT NULL,
                                                                 user_id BIGINT NOT NULL,
                                                                 rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(1000),
    order_id BIGINT,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rating_supplier FOREIGN KEY (supplier_id) REFERENCES household_schema.suppliers(id),
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES household_schema.users(id)
    );

-- ====================================================
-- PURCHASE ORDERS
-- ====================================================

-- Purchase Orders table
CREATE TABLE IF NOT EXISTS household_schema.purchase_orders (
                                                                id BIGSERIAL PRIMARY KEY,
                                                                order_number VARCHAR(50) NOT NULL UNIQUE,
    supplier_id BIGINT NOT NULL,
    order_type VARCHAR(20) NOT NULL DEFAULT 'PURCHASE',
    status VARCHAR(30) NOT NULL,
    expected_delivery DATE,
    actual_delivery DATE,
    warehouse_location VARCHAR(255),
    received_by BIGINT,
    quality_check BOOLEAN,
    invoice_number VARCHAR(50),
    payment_due DATE,
    payment_status VARCHAR(20),
    subtotal DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    created_by BIGINT,
    notes TEXT,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_order_supplier FOREIGN KEY (supplier_id) REFERENCES household_schema.suppliers(id)
    );

-- Purchase Order Items table
CREATE TABLE IF NOT EXISTS household_schema.purchase_order_items (
                                                                     id BIGSERIAL PRIMARY KEY,
                                                                     purchase_order_id BIGINT NOT NULL,
                                                                     product_id BIGINT NOT NULL,
                                                                     quantity INTEGER NOT NULL,
                                                                     price DECIMAL(10,2) NOT NULL,
    supplier_price DECIMAL(10,2),
    supplier_sku VARCHAR(50),
    received_quantity INTEGER DEFAULT 0,
    product_name VARCHAR(100),
    product_sku VARCHAR(50),
    CONSTRAINT fk_po_item_order FOREIGN KEY (purchase_order_id) REFERENCES household_schema.purchase_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_po_item_product FOREIGN KEY (product_id) REFERENCES household_schema.products(id)
    );