# Makefile for Household Store
# ====================================================

.PHONY: help ssl build run docker-build docker-up docker-logs docker-down clean clean-all

# Colors for output
GREEN := \033[0;32m
YELLOW := \033[1;33m
RED := \033[0;31m
NC := \033[0m # No Color

# Default target
help:
	@echo "$(GREEN)Available commands:$(NC)"
	@echo "  $(YELLOW)make ssl$(NC)         - generate SSL certificate (if missing)"
	@echo "  $(YELLOW)make build$(NC)       - build JAR file"
	@echo "  $(YELLOW)make run$(NC)         - run application locally (local profile)"
	@echo "  $(YELLOW)make docker-build$(NC)- build Docker image"
	@echo "  $(YELLOW)make docker-up$(NC)   - start application in Docker (detached mode)"
	@echo "  $(YELLOW)make docker-logs$(NC) - show Docker container logs"
	@echo "  $(YELLOW)make docker-down$(NC) - stop Docker containers"
	@echo "  $(YELLOW)make clean$(NC)       - clean keystore and logs"
	@echo "  $(YELLOW)make clean-all$(NC)   - full cleanup (including Docker volumes)"

# Generate SSL certificate
ssl:
	@echo "🔐 Checking SSL certificate..."
	@if [ ! -f "keystore.p12" ]; then \
		echo "Certificate not found, generating..."; \
		chmod +x generate-ssl.sh 2>/dev/null || true; \
		./generate-ssl.sh; \
	else \
		echo "✅ SSL certificate already exists"; \
	fi

# Build JAR file
build:
	@echo "📦 Building JAR file..."
	@./mvnw clean package -DskipTests

# Run locally
run: ssl
	@echo "🚀 Starting application locally (local profile)..."
	@./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Build Docker image
docker-build: build
	@echo "🏗️  Building Docker image..."
	@docker-compose build --no-cache

# Start in Docker (detached mode)
docker-up: ssl docker-build
	@echo "🐳 Starting application in Docker..."
	@docker-compose up -d
	@echo "✅ Application started in background"
	@echo "📋 View logs: make docker-logs"
	@echo "🔗 URL: https://localhost:8443"

# Show Docker logs
docker-logs:
	@docker-compose logs -f

# Stop Docker containers
docker-down:
	@echo "🛑 Stopping Docker containers..."
	@docker-compose down

# Clean temporary files (without Docker volumes)
clean:
	@echo "🧹 Cleaning temporary files..."
	@rm -f keystore.p12 2>/dev/null || true
	@rm -rf logs/*.log 2>/dev/null || true
	@echo "✅ Cleanup completed"

# Full cleanup (including Docker)
clean-all: docker-down
	@echo "🧹 Full cleanup..."
	@rm -f keystore.p12 2>/dev/null || true
	@rm -rf logs/ 2>/dev/null || true
	@rm -rf uploads/ 2>/dev/null || true
	@docker-compose down -v 2>/dev/null || true
	@./mvnw clean 2>/dev/null || true
	@echo "✅ Full cleanup completed"