-- ============================================================================
-- Create payment_method_user_types table
-- ============================================================================

CREATE TABLE IF NOT EXISTS household_schema.payment_method_user_types (
                                                                          id BIGSERIAL PRIMARY KEY,
                                                                          payment_method_id BIGINT NOT NULL,
                                                                          user_type VARCHAR(20) NOT NULL,
                                                                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                                                          sort_order INTEGER NOT NULL DEFAULT 0,
                                                                          created_by BIGINT NOT NULL,
                                                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Create indexes
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_pmut_payment_method_id
    ON household_schema.payment_method_user_types(payment_method_id);

CREATE INDEX IF NOT EXISTS idx_pmut_user_type
    ON household_schema.payment_method_user_types(user_type);

CREATE INDEX IF NOT EXISTS idx_pmut_active
    ON household_schema.payment_method_user_types(is_active);

CREATE INDEX IF NOT EXISTS idx_pmut_sort_order
    ON household_schema.payment_method_user_types(sort_order);