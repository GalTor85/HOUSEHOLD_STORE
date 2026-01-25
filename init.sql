-- init.sql (только для Docker)
-- =============================================
-- БАЗОВАЯ ИНИЦИАЛИЗАЦИЯ - ТОЛЬКО СХЕМА И ПОЛЬЗОВАТЕЛЬ
-- =============================================

-- 1. Создаем пользователя для приложения
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'household_app') THEN
        CREATE USER household_app WITH PASSWORD 'household123';
        RAISE NOTICE 'User household_app created successfully';
END IF;
END $$;

-- 2. Создаем схему для приложения
CREATE SCHEMA IF NOT EXISTS household_schema AUTHORIZATION household_app;

-- 3. Настраиваем поисковый путь
ALTER USER household_app SET search_path TO household_schema;

-- 4. Выдаем базовые права
GRANT CONNECT ON DATABASE household_store TO household_app;
GRANT USAGE, CREATE ON SCHEMA household_schema TO household_app;

-- 5. Права на будущие объекты (для Hibernate)
ALTER DEFAULT PRIVILEGES IN SCHEMA household_schema
    GRANT ALL PRIVILEGES ON TABLES TO household_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA household_schema
    GRANT ALL PRIVILEGES ON SEQUENCES TO household_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA household_schema
    GRANT EXECUTE ON FUNCTIONS TO household_app;

-- 6. Комментарий к схеме
COMMENT ON SCHEMA household_schema IS 'Схема для приложения Household Store (управляется Hibernate)';

-- 7. Сообщение об успехе
DO $$
BEGIN
    RAISE NOTICE '=========================================';
    RAISE NOTICE 'Base initialization completed!';
    RAISE NOTICE 'Schema: household_schema';
    RAISE NOTICE 'Application user: household_app';
    RAISE NOTICE 'Tables will be created by Hibernate';
    RAISE NOTICE '=========================================';
END $$;