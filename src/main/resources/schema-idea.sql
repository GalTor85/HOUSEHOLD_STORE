-- Создаем схему, если её нет
CREATE SCHEMA IF NOT EXISTS household_schema;

-- Даем права на схему (опционально, если нужно)
GRANT ALL ON SCHEMA household_schema TO postgres;

-- Устанавливаем поиск по схеме для текущей сессии
SET search_path TO household_schema;