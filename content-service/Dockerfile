# # ==========================================
# # STAGE 1: Build Environment
# # ==========================================
# FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
# WORKDIR /build

# COPY pom.xml .
# COPY common-lib/ ./common-lib/

# RUN mvn clean install -N -DskipTests
# RUN mvn -f common-lib/pom.xml clean install -DskipTests

# COPY content-service/ ./content-service/
# RUN mvn -f content-service/pom.xml clean package -DskipTests

# # ==========================================
# # STAGE 2: Lightweight Production Runtime
# # ==========================================
# FROM eclipse-temurin:17-jre-alpine
# WORKDIR /app

# RUN mkdir -p /app/logs
# RUN addgroup -S spring && adduser -S spring -G spring
# RUN chown -R spring:spring /app

# USER spring:spring

# COPY --from=builder /build/content-service/target/*.jar app.jar
# EXPOSE 8090
# ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]


# ==========================================
# STAGE 1: Build
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy root pom first (for dependency cache)
COPY pom.xml .

# Copy modules
COPY common-lib ./common-lib
COPY content-service ./content-service


# Build complete Maven reactor
# This builds:
# PrevPaperApp
#   ├── common-lib
#   └── content-service
RUN mvn clean install -DskipTests


# ==========================================
# STAGE 2: Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app


# Create folders
RUN mkdir -p /app/logs /app/certs


# Create non-root user
RUN addgroup -S spring \
    && adduser -S spring -G spring


# Copy generated jar
COPY --from=builder \
    /build/content-service/target/content-service-*.jar \
    app.jar


# Copy Kafka SSL certificate
COPY content-service/kafka-truststore.jks \
    /app/certs/kafka-truststore.jks


# Permission
RUN chown -R spring:spring /app


USER spring:spring


EXPOSE 8090

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]

