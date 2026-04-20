# 🏪 Household Store / Управление товарами онлайн

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-✓-2496ED.svg)](https://www.docker.com/)

**🇷🇺 [Русская версия](#русская-версия)** | **🇬🇧 [English Version](#english-version)**

---

## 🇷🇺 Русская версия

### 📋 Оглавление
- [Возможности](#-возможности)
- [Технологии](#-технологии)
- [Требования](#-требования)
- [Быстрый старт](#-быстрый-старт)
- [Конфигурация](#-конфигурация)
- [API Документация](#-api-документация)
- [Безопасность](#-безопасность)
- [Разработка](#-разработка)
- [Тестирование](#-тестирование)
- [Лицензия](#-лицензия)
- [Контакты](#-контакты)

---

### 🚀 Возможности

#### Управление товарами
- Полный CRUD для товаров
- Категории и бренды
- Варианты товаров (цвет, размер)
- Загрузка изображений и видео
- Атрибуты товаров

#### Складской учёт
- Управление складами и ячейками хранения
- Приёмка товаров от поставщиков
- Перемещение товаров между складами
- Списание испорченных/утерянных товаров
- Отслеживание остатков в реальном времени

#### Заказы
- Заказы клиентов (розничные и оптовые)
- Заказы поставщикам
- Корзина покупателя
- Статусная модель заказов
- Резервирование товаров при оформлении

#### Финансы
- Управление банковскими счетами
- Кассовые операции
- Счета на оплату
- Платёжные транзакции
- Возвраты и частичные оплаты

#### Пользователи и безопасность
- JWT аутентификация
- Роли: `ADMIN`, `MANAGER`, `USER`
- Типы пользователей (розница, опт, VIP)
- Rate limiting на эндпоинты входа

---

### 💻 Технологии

| Категория | Технологии |
|----------|-----------|
| **Язык** | Java 21 |
| **Фреймворк** | Spring Boot 4.0.4, Spring Security, Spring Data JPA |
| **База данных** | PostgreSQL 15 |
| **Миграции** | Liquibase |
| **Контейнеризация** | Docker, Docker Compose |
| **Документация API** | Swagger/OpenAPI 3 |
| **Тестирование** | JUnit 5, Testcontainers, AssertJ |
| **Логирование** | SLF4J + Logback |

---

### 📦 Требования

- **JDK 21+** (рекомендуется OpenJDK)
- **Maven 3.9+**
- **Docker 24+** и **Docker Compose v2+** (для production)
- **PostgreSQL 15+** (если запуск без Docker)

---

### ⚡ Быстрый старт

#### 1. Клонирование репозитория

```bash
git clone https://github.com/GalTor85/household-store.git
cd household-store
# Копируем конфигурацию
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties

# Запускаем PostgreSQL в контейнере
docker run -d \
  --name postgres-local \
  -p 5432:5432 \
  -e POSTGRES_DB=household_store \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15-alpine

# Сборка и запуск
mvn clean package
mvn spring-boot:run -Dspring-boot.run.profiles=local

➡️ Приложение будет доступно: http://localhost:8080

## Создаём .env
cp .env.example .env
nano .env  # задайте надёжные значения

# Генерация SSL-сертификата (если нет)
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -storepass changeit -dname "CN=localhost"

# Запуск
docker-compose up -d

➡️ Приложение будет доступно: https://localhost:8443

🔧 Конфигурация
Основные переменные окружения (.env)
Переменная
Описание
Пример
DB_PASSWORD
Пароль PostgreSQL
Str0ng!DBP@ss
JWT_SECRET
Секрет JWT (мин. 32 символа)
8kL9mN2pQ5rS8tV1wY4zB7cE0fH3jK6mP9sT2vX5yA8
SSL_KEYSTORE_PASSWORD
Пароль к keystore.p12
K3yst0re!P@ss
ADMIN_EMAIL
Email администратора
admin@your-domain.com
ADMIN_PASSWORD
Пароль администратора
Str0ng!Adm1nP@ss
Профили Spring
Профиль
Описание
Конфигурация
local
Локальная разработка
HTTP, H2 или локальный PostgreSQL
docker
Production в Docker
HTTPS, PostgreSQL в контейнере
test
Тестирование
H2 in-memory, отключён Rate Limiting

📚 API Документация
Swagger UI

https://localhost:8443/swagger-ui.html
http://localhost:8080/swagger-ui.html
Actuator Endpoints

https://localhost:8443/actuator/health
https://localhost:8443/actuator/info
Основные эндпоинты
Метод
Путь
Описание
Доступ
POST
/app/auth/register
Регистрация
Public
POST
/app/auth/login
Вход
Public
GET
/app/users/me
Профиль пользователя
USER+
GET
/app/manager/products
Товары
MANAGER+
GET
/app/admin/users
Пользователи
ADMIN

🔐 Безопасность

JWT — хранение в HttpOnly cookie
Пароли — BCrypt (12 раундов)
HTTPS — обязателен в production (Docker)
Rate Limiting — 10 запросов/мин на /auth/*
CORS — строгая настройка для production
Secrets — только через переменные окружения
CSRF — отключён (REST API, stateless)

🛠 Разработка
Структура проекта
src/
├── main/
│   ├── java/ru/galtor85/household_store/
│   │   ├── advice/         # Глобальная обработка исключений
│   │   ├── config/         # Конфигурации
│   │   ├── controller/     # REST контроллеры
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA сущности
│   │   ├── mapper/         # Мапперы Entity ↔ DTO
│   │   ├── processor/      # Бизнес-процессоры
│   │   ├── repository/     # Spring Data репозитории
│   │   ├── security/       # JWT, Security фильтры
│   │   ├── service/        # Сервисный слой
│   │   └── validator/      # Валидаторы
│   └── resources/
│       ├── db/changelog/   # Liquibase миграции
│       ├── messages*.properties  # Локализация
│       └── application*.properties
└── test/                   # Юнит и интеграционные тесты

🧪 Тестирование
# Запуск всех тестов
mvn test

# Запуск конкретного теста
mvn test -Dtest=MessageKeysConsistencyTest

# Пропуск тестов
mvn clean package -DskipTests

Типы тестов

Юнит-тесты — отдельные компоненты
Интеграционные — @SpringBootTest с H2
MessageKeysConsistencyTest — проверка локализации
SecurityAccessTest — проверка доступа к эндпоинтам

🇬🇧 English Version
📋 Table of Contents

Features
Tech Stack
Requirements
Quick Start
Configuration
API Documentation
Security
Development
Testing
License
Contacts

🚀 Features
Product Management

Full CRUD operations for products
Categories and brands
Product variants (color, size)
Image and video uploads
Custom product attributes
Warehouse Management

Multiple warehouses and storage cells
Goods receiving from suppliers
Stock transfers between warehouses
Write-off of damaged or lost items
Real-time stock tracking
Orders

Customer orders (retail and wholesale)
Supplier purchase orders
Shopping cart with reservation
Order status workflow
Stock reservation on checkout
Finance

Bank account management
Cash register operations
Invoices and payments
Refunds and partial payments
Transaction history
Users & Security

JWT authentication
Roles: ADMIN, MANAGER, USER
User types (retail, wholesale, VIP)
Rate limiting on auth endpoints

💻 Tech Stack
Category
Technologies
Language
Java 21
Framework
Spring Boot 4.0.4, Spring Security, Spring Data JPA
Database
PostgreSQL 15
Migrations
Liquibase
Containerization
Docker, Docker Compose
API Docs
Swagger/OpenAPI 3
Testing
JUnit 5, Testcontainers, AssertJ
Logging
SLF4J + Logback

📦 Requirements

JDK 21+ (OpenJDK recommended)
Maven 3.9+
Docker 24+ and Docker Compose v2+ (for production)
PostgreSQL 15+ (if running without Docker)

⚡ Quick Start
1. Clone Repository

git clone https://github.com/GalTor85/household-store.git
cd household-store

2. Local Run (without Docker)

# Copy configuration
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties

# Start PostgreSQL
docker run -d \
  --name postgres-local \
  -p 5432:5432 \
  -e POSTGRES_DB=household_store \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15-alpine

# Build and run
mvn clean package
mvn spring-boot:run -Dspring-boot.run.profiles=local

➡️ App available at: http://localhost:8080
3. Run with Docker Compose

# Create .env
cp .env.example .env
nano .env  # set strong values

# Generate SSL certificate
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -storepass changeit -dname "CN=localhost"

# Start
docker-compose up -d

➡️ App available at: https://localhost:8443

🔧 Configuration
Environment Variables (.env)
Variable
Description
Example
DB_PASSWORD
PostgreSQL password
Str0ng!DBP@ss
JWT_SECRET
JWT secret (min 32 chars)
8kL9mN2pQ5rS8tV1wY4zB7cE0fH3jK6mP9sT2vX5yA8
SSL_KEYSTORE_PASSWORD
Keystore password
K3yst0re!P@ss
ADMIN_EMAIL
Admin email
admin@your-domain.com
ADMIN_PASSWORD
Admin password
Str0ng!Adm1nP@ss
Spring Profiles
Profile
Description
Configuration
local
Local development
HTTP, local PostgreSQL
docker
Production in Docker
HTTPS, PostgreSQL in container
test
Testing
H2 in-memory, Rate Limiting disabled

📚 API Documentation
Swagger UI

https://localhost:8443/swagger-ui.html
http://localhost:8080/swagger-ui.html
Actuator Endpoints

https://localhost:8443/actuator/health
https://localhost:8443/actuator/info
Main Endpoints
Method
Path
Description
Access
POST
/app/auth/register
Registration
Public
POST
/app/auth/login
Login
Public
GET
/app/users/me
User profile
USER+
GET
/app/manager/products
Products
MANAGER+
GET
/app/admin/users
Users
ADMIN

🔐 Security

JWT — stored in HttpOnly cookie
Passwords — BCrypt (12 rounds)
HTTPS — required in production (Docker)
Rate Limiting — 10 requests/min on /auth/*
CORS — strict policy for production
Secrets — via environment variables only
CSRF — disabled (stateless REST API)

🛠 Development
Project Structure

src/
├── main/
│   ├── java/ru/galtor85/household_store/
│   │   ├── advice/         # Global exception handling
│   │   ├── config/         # Configurations
│   │   ├── controller/     # REST controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA entities
│   │   ├── mapper/         # Entity ↔ DTO mappers
│   │   ├── processor/      # Business processors
│   │   ├── repository/     # Spring Data repositories
│   │   ├── security/       # JWT, Security filters
│   │   ├── service/        # Service layer
│   │   └── validator/      # Validators
│   └── resources/
│       ├── db/changelog/   # Liquibase migrations
│       ├── messages*.properties  # Localization
│       └── application*.properties
└── test/                   # Unit & integration tests

🧪 Testing

# Run all tests
mvn test

# Run specific test
mvn test -Dtest=MessageKeysConsistencyTest

# Skip tests
mvn clean package -DskipTests

Test Types

Unit tests — individual components
Integration tests — @SpringBootTest with H2
MessageKeysConsistencyTest — localization validation
SecurityAccessTest — endpoint access verification

📄 License
© 2026 G@LTor85. All rights reserved.

📞 Contacts

GitHub: https://github.com/GalTor85
Email: gal85@bk.ru