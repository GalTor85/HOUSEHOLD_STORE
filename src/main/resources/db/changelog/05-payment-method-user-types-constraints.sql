-- ============================================================================
-- Add foreign key constraint (PostgreSQL compatible)
-- ============================================================================

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_pmut_payment_method_id'
              AND table_schema = 'household_schema'
        ) THEN
            ALTER TABLE household_schema.payment_method_user_types
                ADD CONSTRAINT fk_pmut_payment_method_id
                    FOREIGN KEY (payment_method_id)
                        REFERENCES household_schema.payment_methods(id)
                        ON DELETE CASCADE;
        END IF;
    END $$;

-- ============================================================================
-- Add unique constraint (PostgreSQL compatible)
-- ============================================================================

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'uk_pmut_payment_method_user_type'
              AND table_schema = 'household_schema'
        ) THEN
            ALTER TABLE household_schema.payment_method_user_types
                ADD CONSTRAINT uk_pmut_payment_method_user_type
                    UNIQUE (payment_method_id, user_type);
        END IF;
    END $$;