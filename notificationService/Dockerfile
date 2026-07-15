# ==========================================
# STAGE 1: Build Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY common-lib/ ./common-lib/

RUN mvn clean install -N -DskipTests
RUN mvn -f common-lib/pom.xml clean install -DskipTests

COPY notificationService/ ./notificationService/
RUN mvn -f notificationService/pom.xml clean package -DskipTests

# ==========================================
# STAGE 2: Lightweight Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 1. 🟢 FIXED: Pre-create both internal and external root logging targets
RUN mkdir -p /app/logs /logs

# 2. Add your non-privileged system user profile
RUN addgroup -S spring && adduser -S spring -G spring

# 3. 🟢 FIXED: Grant explicit write permissions to both folders
RUN chown -R spring:spring /app /logs

USER spring:spring

COPY --from=builder /build/notificationService/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]