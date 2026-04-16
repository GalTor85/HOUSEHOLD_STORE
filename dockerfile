# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build JAR
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from builder
COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

# Create directories and set permissions
RUN mkdir -p /app/logs /app/uploads /app/keystore && \
    chown -R spring:spring /app/logs /app/uploads /app/keystore

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8443

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]