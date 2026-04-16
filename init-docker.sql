-- noinspection SpellCheckingInspectionForFile

-- noinspection SpellCheckingInspectionForFile

-- noinspection SpellCheckingInspectionForFile

-- noinspection SpellCheckingInspectionForFile

-- ====================================================
-- Database initialization for Docker
-- Executed on first container startup
-- ====================================================

-- Set encoding
SET client_encoding = 'UTF8';

-- Create schema (Liquibase will also create it, but just in case)
CREATE SCHEMA IF NOT EXISTS household_schema;

-- Grant permissions
GRANT ALL ON SCHEMA household_schema TO postgres;

-- Create application user (optional)
-- DO $$
-- BEGIN
--    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'household_app') THEN
--        CREATE USER household_app WITH PASSWORD 'household123';
--    END IF;
-- END
-- $$;

-- GRANT CONNECT ON DATABASE household_store TO household_app;
-- GRANT USAGE ON SCHEMA household_schema TO household_app;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA household_schema TO household_app;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA household_schema TO household_app;

-- Set Russian locale (commented out - requires superuser)
-- UPDATE pg_database SET datcollate = 'ru_RU.UTF-8', datctype = 'ru_RU.UTF-8' WHERE datname = 'household_store';