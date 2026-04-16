-- Default base currency
INSERT INTO household_schema.currencies (code, name, symbol, is_base, exchange_rate, is_active, decimal_places, created_by)
SELECT 'RUB', 'Russian Ruble', '₽', true, 1.0000, true, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM household_schema.currencies WHERE code = 'RUB');
-- Default warehouse
INSERT INTO household_schema.warehouses (code, name, description, barcode, barcode_format, address, is_active, total_capacity, used_capacity, created_by)
SELECT 'WH-DEFAULT', 'Main Warehouse', 'Default warehouse for the system',
       'WH-DEFAULT-BARCODE-' || EXTRACT(EPOCH FROM NOW())::bigint, 'CODE_128',
       'System Default Address', true, 1000, 0, 1
    WHERE NOT EXISTS (SELECT 1 FROM household_schema.warehouses WHERE code = 'WH-DEFAULT');