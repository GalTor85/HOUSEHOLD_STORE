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

# Копируем JAR файл из builder
COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

USER spring:spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]