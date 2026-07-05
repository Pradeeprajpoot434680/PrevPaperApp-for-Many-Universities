# ==========================================
# STAGE 1: Build Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy structural project descriptors
COPY pom.xml .
COPY common-lib/ ./common-lib/

# 1. 🟢 FIXED: Install parent coordinates to container cache non-recursively
RUN mvn clean install -N -DskipTests

# 2. 🟢 FIXED: Compile and install the shared common core module
RUN mvn -f common-lib/pom.xml clean install -DskipTests

# Copy target service layer source
COPY university-service/ ./university-service/

# 3. Package the target service
RUN mvn -f university-service/pom.xml clean package -DskipTests

# ==========================================
# STAGE 2: Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /build/university-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]