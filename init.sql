-- Создаем пользователя для приложения
CREATE USER household_app WITH PASSWORD 'household123';

-- Создаем схему для приложения
CREATE SCHEMA IF NOT EXISTS household_schema AUTHORIZATION household_app;

-- Даем пользователю приложения все необходимые права
GRANT CONNECT ON DATABASE household_store TO household_app;
GRANT USAGE, CREATE ON SCHEMA household_schema TO household_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA household_schema TO household_app;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA household_schema TO household_app;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA household_schema TO household_app;

-- Устанавливаем поисковый путь по умолчанию
ALTER USER household_app SET search_path TO household_schema;

-- Даем права на будущие таблицы
ALTER DEFAULT PRIVILEGES IN SCHEMA household_schema GRANT ALL ON TABLES TO household_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA household_schema GRANT ALL ON SEQUENCES TO household_app;

-- Комментарий к схеме
COMMENT ON SCHEMA household_schema IS 'Схема для приложения Household Store';