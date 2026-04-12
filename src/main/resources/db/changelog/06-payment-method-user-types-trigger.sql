-- ============================================================================
-- Function for automatic updated_at update
-- ============================================================================

CREATE OR REPLACE FUNCTION household_schema.update_payment_method_user_types_updated_at()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$function$;

-- ============================================================================
-- Trigger for automatic updated_at update
-- ============================================================================

DROP TRIGGER IF EXISTS trigger_update_payment_method_user_types
    ON household_schema.payment_method_user_types;

CREATE TRIGGER trigger_update_payment_method_user_types
    BEFORE UPDATE ON household_schema.payment_method_user_types
    FOR EACH ROW
EXECUTE FUNCTION household_schema.update_payment_method_user_types_updated_at();