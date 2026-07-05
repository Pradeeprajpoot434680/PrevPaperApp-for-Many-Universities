# ==========================================
# STAGE 1: Dependency Resolver & Build Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# 1. Copy the tracking parent POM structure and shared core module
COPY pom.xml .
COPY common-lib/ ./common-lib/

# 2. 🟢 FIXED: Install the parent POM locally *Non-Recursively* to cache the dependency management definitions
RUN mvn clean install -N -DskipTests

# 3. 🟢 FIXED: Build and install the common library to the local container cache
RUN mvn -f common-lib/pom.xml clean install -DskipTests

# 4. Copy the target microservice source code
COPY auth-service/ ./auth-service/

# 5. 🟢 FIXED: Package the target microservice using its explicit path file definition
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