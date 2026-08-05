# # ==========================================
# # STAGE 1: Build Environment
# # ==========================================
# FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
# WORKDIR /build

# COPY pom.xml .
# COPY common-lib/ ./common-lib/

# RUN mvn clean install -N -DskipTests
# RUN mvn -f common-lib/pom.xml clean install -DskipTests

# # 🟢 FIXED: Ensure this points precisely to your university-service module subdirectory
# COPY university-service/ ./university-service/
# RUN mvn -f university-service/pom.xml clean package -DskipTests

# # ==========================================
# # STAGE 2: Lightweight Production Runtime
# # ==========================================
# FROM eclipse-temurin:17-jre-alpine
# WORKDIR /app

# RUN mkdir -p /app/logs
# RUN addgroup -S spring && adduser -S spring -G spring
# RUN chown -R spring:spring /app

# USER spring:spring

# # 🟢 FIXED: Pulls the true compiled university jar instead of a cached content module jar
# COPY --from=builder /build/university-service/target/*.jar app.jar
# EXPOSE 8082
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

COPY university-service/ ./university-service/

RUN mvn -f university-service/pom.xml clean package -DskipTests


# ==========================================
# STAGE 2: Lightweight Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN mkdir -p /app/logs /app/certs

RUN addgroup -S spring && adduser -S spring -G spring


# Copy application jar
COPY --from=builder /build/university-service/target/*.jar app.jar


# Copy Kafka SSL certificate
COPY university-service/kafka-truststore.jks /app/certs/kafka-truststore.jks

# Give spring user access
RUN chown -R spring:spring /app


USER spring:spring


EXPOSE 8082


ENTRYPOINT ["java", \
 "-XX:+UseG1GC", \
 "-XX:+ExitOnOutOfMemoryError", \
 "-jar", \
 "app.jar"]