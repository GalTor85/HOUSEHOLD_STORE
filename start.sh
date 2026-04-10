#!/bin/bash

# ====================================================
# Скрипт для запуска Docker контейнеров с миграциями
# ====================================================

set -e

echo "🚀 Запуск Household Store в Docker..."

# Останавливаем старые контейнеры
echo "🛑 Останавливаем старые контейнеры..."
docker-compose down

# Очищаем volume с БД (опционально, для полного сброса)
if [ "$1" == "--clean" ]; then
    echo "🧹 Очищаем volume с БД..."
    docker volume rm household-postgres-data || true
fi

# Собираем образ приложения
echo "📦 Собираем образ приложения..."
docker-compose build --no-cache

# Запускаем контейнеры
echo "🐳 Запускаем контейнеры..."
docker-compose up -d

# Ждем готовности PostgreSQL
echo "⏳ Ожидаем запуск PostgreSQL..."
until docker exec household-postgres pg_isready -U postgres -d household_store; do
    sleep 2
done

# Ждем готовности приложения
echo "⏳ Ожидаем запуск приложения и выполнение миграций..."
sleep 10

# Проверяем статус
echo "📊 Проверяем статус..."
docker-compose ps

# Смотрим логи Liquibase
echo "📝 Логи миграций Liquibase:"
docker-compose logs app | grep -i "liquibase\|migration" || echo "Миграции не найдены в логах"

# Проверяем healthcheck
echo "🏥 Проверяем healthcheck..."
curl -k https://localhost:8443/actuator/health || echo "Healthcheck не доступен"

echo "✅ Готово!"
echo "🔗 Приложение доступно по адресу: https://localhost:8443"
echo "📚 Swagger UI: https://localhost:8443/swagger-ui.html"