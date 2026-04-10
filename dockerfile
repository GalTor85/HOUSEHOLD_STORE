# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code (включая миграции в src/main/resources/db/)
COPY src ./src

# Build JAR (миграции упакуются в JAR)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from builder (миграции уже внутри JAR)
COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/logs /app/uploads && chown spring:spring /app/logs /app/uploads

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8443

# Run application (Liquibase выполнится при старте)
ENTRYPOINT ["java", "-jar", "app.jar"]