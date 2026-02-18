#!/bin/bash

echo "🚀 Starting Household Store..."

# Функция для локального запуска
start_local() {
    echo "📱 Starting locally..."
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
}

# Функция для Docker
start_docker() {
    echo "🐳 Starting with Docker..."
    docker-compose up --build
}

# Меню выбора
case "$1" in
    local)
        start_local
        ;;
    docker)
        start_docker
        ;;
    *)
        echo "Usage: ./start.sh [local|docker]"
        echo "  local  - Run locally with Spring Boot"
        echo "  docker - Run with Docker Compose"
        exit 1
        ;;
esac