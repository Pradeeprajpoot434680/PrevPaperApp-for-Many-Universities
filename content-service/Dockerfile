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


# Copy root pom because common-lib parent needs it
COPY pom.xml .


# Copy required modules only
COPY common-lib ./common-lib
COPY content-service ./content-service


# Install root parent POM into the local repo (required by common-lib's descriptor)
RUN mvn clean install -N -DskipTests


# Install common-lib
RUN mvn -f common-lib/pom.xml clean install -DskipTests


# Build content-service
RUN mvn -f content-service/pom.xml clean package -DskipTests



# ==========================================
# STAGE 2: Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app


RUN mkdir -p /app/logs /app/certs


RUN addgroup -S spring && adduser -S spring -G spring


COPY --from=builder \
    /build/content-service/target/content-service-*.jar \
    app.jar


COPY content-service/kafka-truststore.jks \
    /app/certs/kafka-truststore.jks


RUN chown -R spring:spring /app


USER spring:spring


EXPOSE 8090


ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
