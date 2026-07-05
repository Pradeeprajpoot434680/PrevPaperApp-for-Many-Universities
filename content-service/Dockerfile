# ==========================================
# STAGE 1: Dependency Resolver & Build Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# 1. Copy the Root Parent POM and Common Library first
COPY pom.xml .
COPY common-lib/ ./common-lib/

# 2. Install common-lib to the local container Maven repository cache
RUN mvn -f common-lib/pom.xml clean install -DskipTests

# 3. Copy the target microservice source code
COPY content-service/ ./content-service/

# 4. Compile and package the isolated production runnable fat jar
RUN mvn -f content-service/pom.xml clean package -DskipTests

# ==========================================
# STAGE 2: Lightweight Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a secure, non-privileged system group and user to run the application safely
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the lightweight executable jar from Stage 1
COPY --from=builder /build/content-service/target/*.jar app.jar

# Expose the content-service default port
EXPOSE 8083

# Production optimizations: Adjust JVM garbage collection and heap tracking parameters
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]


# docker build -t prevpaper-content-service:1.0.0 -f content-service/Dockerfile .