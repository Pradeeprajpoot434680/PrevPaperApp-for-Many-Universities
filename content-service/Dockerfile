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
# STAGE 1: Build Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY common-lib/ ./common-lib/

RUN mvn clean install -N -DskipTests
RUN mvn -f common-lib/pom.xml clean install -DskipTests

COPY content-service/ ./content-service/

RUN mvn -f content-service/pom.xml clean package -DskipTests


# ==========================================
# STAGE 2: Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN mkdir -p /app/logs /app/certs

RUN addgroup -S spring && adduser -S spring -G spring

# Copy application
COPY --from=builder /build/content-service/target/*.jar app.jar

# Copy Kafka truststore
COPY content-service/kafka-truststore.jks /app/certs/kafka-truststore.jks

# Permissions
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8090

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]