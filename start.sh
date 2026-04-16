#!/bin/bash

# ====================================================
# Docker startup script for Household Store
# ====================================================

set -e

echo "🚀 Starting Household Store in Docker..."

# Check for keystore.p12
if [ ! -f "keystore.p12" ]; then
    echo "🔐 SSL certificate not found. Generating..."
    if [ -f "generate-ssl.sh" ]; then
        chmod +x generate-ssl.sh
        ./generate-ssl.sh
    else
        echo "❌ Error: keystore.p12 and generate-ssl.sh not found!"
        exit 1
    fi
fi

# Stop old containers
echo "🛑 Stopping old containers..."
docker-compose down

# Clean DB volume (optional)
if [ "$1" == "--clean" ]; then
    echo "🧹 Cleaning database volume..."
    docker volume rm household-store_postgres_data 2>/dev/null || true
    echo "   Volume cleaned (if existed)"
fi

# Build image
echo "📦 Building application image..."
docker-compose build --no-cache

# Start containers
echo "🐳 Starting containers..."
docker-compose up -d

# Wait for PostgreSQL
echo "⏳ Waiting for PostgreSQL..."
until docker exec household-postgres pg_isready -U postgres -d household_store 2>/dev/null; do
    echo "   PostgreSQL not ready yet..."
    sleep 2
done
echo "✅ PostgreSQL is ready!"

# Wait for application
echo "⏳ Waiting for application..."
ATTEMPTS=0
MAX_ATTEMPTS=120
APP_READY=false

while [ $ATTEMPTS -lt $MAX_ATTEMPTS ]; do
    ATTEMPTS=$((ATTEMPTS + 1))

    # Use curl with -k flag to ignore SSL certificate
    HTTP_CODE=$(curl -k -s -o /dev/null -w "%{http_code}" https://localhost:8443/actuator/health 2>/dev/null || echo "000")

    if [ "$HTTP_CODE" == "200" ]; then
        APP_READY=true
        break
    fi

    echo "   Application not ready yet... (HTTP $HTTP_CODE, attempt $ATTEMPTS/$MAX_ATTEMPTS)"
    sleep 3
done

if [ "$APP_READY" != "true" ]; then
    echo "❌ Application failed to start within timeout!"
    echo "📝 Last application logs:"
    docker-compose logs --tail=50 app
    exit 1
fi
echo "✅ Application is ready!"

# Show status
echo "📊 Container status:"
docker-compose ps

# Show Liquibase logs
echo "📝 Liquibase migrations:"
docker-compose logs app 2>/dev/null | grep -i "liquibase\|changeset" | tail -10 || echo "   No migration logs found"

# Healthcheck with curl
echo "🏥 Healthcheck:"
HEALTH=$(curl -k -s https://localhost:8443/actuator/health)
echo "   $HEALTH"

# Done
echo ""
echo "✅ Ready!"
echo "🔗 Application URL: https://localhost:8443"
echo "📚 Swagger UI: https://localhost:8443/swagger-ui.html"
echo ""
echo "🔐 Test accounts:"
echo "   ADMIN:  admin@household.store / Admin123!"
echo "   MANAGER: manager@household.store / Manager123!"
echo ""
echo "📋 Useful commands:"
echo "   docker-compose logs -f app     # View application logs"
echo "   docker-compose down -v         # Stop and remove database"
echo "   ./start.sh --clean             # Restart with clean database"