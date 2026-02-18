# Makefile для Household Store

.PHONY: help ssl run docker-build docker-up clean

# Цель по умолчанию
help:
	@echo "Доступные команды:"
	@echo "  make ssl         - сгенерировать SSL-сертификат (если отсутствует)"
	@echo "  make run         - запустить приложение локально (с профилем local)"
	@echo "  make docker-build- собрать Docker-образ"
	@echo "  make docker-up   - запустить приложение в Docker (с профилем docker)"
	@echo "  make clean       - очистить сгенерированные файлы (keystore, логи)"

# Генерация SSL-сертификата
ssl:
	@echo "🔐 Проверка SSL-сертификата..."
	@chmod +x generate-ssl.sh
	@./generate-ssl.sh

# Запуск локально (сначала генерируем сертификат)
run: ssl
	@echo "🚀 Запуск приложения локально (профиль local)..."
	@./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Сборка Docker-образа
docker-build:
	@echo "🏗️  Сборка Docker-образа..."
	@./mvnw clean package
	@docker-compose build

# Запуск в Docker (сначала генерируем сертификат)
docker-up: ssl
	@echo "🐳 Запуск приложения в Docker..."
	@docker-compose up

# Очистка
clean:
	@echo "🧹 Очистка..."
	@rm -f keystore.p12
	@rm -rf logs/
	@docker-compose down -v 2>/dev/null || true
	@echo "✅ Очистка завершена"