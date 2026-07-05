# ==========================================
# STAGE 1: Dependency Resolver & Build Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# 1. Copy the tracking build structures
COPY pom.xml .
COPY common-lib/ ./common-lib/

# 2. Build and install common-lib to the local container repository cache
RUN mvn -f common-lib/pom.xml clean install -DskipTests

# 3. Copy the target microservice source code
COPY auth-service/ ./auth-service/

# 4. 🟢 FIXED: Removed dependency:go-offline to bypass the parent POM resolution bug
RUN mvn -f auth-service/pom.xml clean package -DskipTests

# ==========================================
# STAGE 2: Lightweight Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /build/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]

# docker build -t prevpaper-auth-service:1.0.0 -f auth-service/Dockerfile .