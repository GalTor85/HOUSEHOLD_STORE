-- ====================================================
-- Инициализация базы данных для Docker
-- Выполняется при первом запуске контейнера
-- ====================================================

-- Устанавливаем кодировку
SET client_encoding = 'UTF8';

-- Создаем схему (Liquibase тоже создаст, но пусть будет)
CREATE SCHEMA IF NOT EXISTS household_schema;

-- Устанавливаем права
GRANT ALL ON SCHEMA household_schema TO postgres;

-- Создаем отдельного пользователя для приложения (опционально)
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

-- Устанавливаем русскую локаль
-- UPDATE pg_database SET datcollate = 'ru_RU.UTF-8', datctype = 'ru_RU.UTF-8' WHERE datname = 'household_store';