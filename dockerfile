# Multi-stage build для минимального образа
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Копируем pom.xml для кэширования зависимостей
COPY pom.xml .

# Скачиваем все зависимости (используем кэш)
RUN mvn dependency:go-offline -B

# Копируем исходный код
COPY src ./src

# Собираем приложение (JAR файл)
RUN mvn clean package -DskipTests

# Финальный образ
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Создаем непривилегированного пользователя
RUN addgroup -S spring && adduser -S spring -G spring

# Устанавливаем необходимые утилиты для работы с SSL
RUN apk add --no-cache openssl curl netcat-openbsd

# Копируем JAR файл из builder
COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

# Создаем директорию для SSL сертификата и логов
RUN mkdir -p /app/keystore && chown spring:spring /app/keystore
RUN mkdir -p /app/logs && chown spring:spring /app/logs

# Копируем SSL-сертификат
COPY --chown=spring:spring keystore.p12 /app/keystore/keystore.p12
RUN chmod 400 /app/keystore/keystore.p12

# Переключаемся на непривилегированного пользователя
USER spring:spring

# Порт для HTTPS
EXPOSE 8443

# Переменные окружения по умолчанию
ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8443 \
    SSL_KEYSTORE_PASSWORD=changeit \
    SSL_KEYSTORE_PATH=file:/app/keystore/keystore.p12

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]