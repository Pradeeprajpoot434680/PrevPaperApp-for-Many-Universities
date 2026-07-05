# ==========================================
# STAGE 1: Dependency Resolver & Build Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY common-lib/ ./common-lib/

RUN mvn -f common-lib/pom.xml clean install -DskipTests

# Copy the target microservice source code
COPY university-service/ ./university-service/

RUN mvn -f university-service/pom.xml clean package -DskipTests

# ==========================================
# STAGE 2: Lightweight Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /build/university-service/target/*.jar app.jar

# Expose the university-service internal port
EXPOSE 8084

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]