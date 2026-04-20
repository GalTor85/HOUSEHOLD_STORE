# 🏪 Household Store

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-✓-2496ED.svg)](https://www.docker.com/)

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

git clone https://github.com/GalTor85/household_store.git`
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