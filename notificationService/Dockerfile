# ==========================================
# STAGE 1: Dependency Resolver & Build Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY common-lib/ ./common-lib/

RUN mvn -f common-lib/pom.xml clean install -DskipTests

# Copy the target microservice source code
COPY notificationService/ ./notificationService/

RUN mvn -f notificationService/pom.xml clean package -DskipTests

# ==========================================
# STAGE 2: Lightweight Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /build/notificationService/target/*.jar app.jar

# Expose the notificationService internal port
EXPOSE 8086

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]