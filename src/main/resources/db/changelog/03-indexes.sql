-- Индексы для производительности
CREATE INDEX IF NOT EXISTS idx_users_email ON household_schema.users(email);
CREATE INDEX IF NOT EXISTS idx_users_mobile ON household_schema.users(mobile_number);

CREATE INDEX IF NOT EXISTS idx_products_sku ON household_schema.products(sku);
CREATE INDEX IF NOT EXISTS idx_products_category ON household_schema.products(category);
CREATE INDEX IF NOT EXISTS idx_products_active ON household_schema.products(active);

CREATE INDEX IF NOT EXISTS idx_sales_orders_user_id ON household_schema.sales_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_sales_orders_status ON household_schema.sales_orders(status);
CREATE INDEX IF NOT EXISTS idx_sales_orders_created_at ON household_schema.sales_orders(created_at);

CREATE INDEX IF NOT EXISTS idx_invoices_status ON household_schema.invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_due_date ON household_schema.invoices(due_date);

CREATE INDEX IF NOT EXISTS idx_cash_transactions_cash_register ON household_schema.cash_transactions(cash_register_id);
CREATE INDEX IF NOT EXISTS idx_cash_transactions_created_at ON household_schema.cash_transactions(created_at);

CREATE INDEX IF NOT EXISTS idx_stock_movements_product ON household_schema.stock_movements(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON household_schema.stock_movements(created_at);

CREATE INDEX IF NOT EXISTS idx_product_stocks_product_warehouse ON household_schema.product_stocks(product_id, warehouse_id);