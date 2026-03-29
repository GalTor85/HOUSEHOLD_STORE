-- Создаём пользователя приложения, если его нет
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'household_app') THEN
        CREATE USER household_app WITH PASSWORD 'household123';
        RAISE NOTICE 'User household_app created successfully';
END IF;
END $$;

-- Создаём схему для приложения
CREATE SCHEMA IF NOT EXISTS household_schema AUTHORIZATION household_app;

-- Настраиваем права
GRANT CONNECT ON DATABASE household_store TO household_app;
GRANT USAGE, CREATE ON SCHEMA household_schema TO household_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA household_schema GRANT ALL ON TABLES TO household_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA household_schema GRANT ALL ON SEQUENCES TO household_app;
SET client_encoding = 'UTF8';