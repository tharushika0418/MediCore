# ===================================================================
# Billing Service - multi-stage Docker build
#   Stage 1 (builder): generate gRPC code from .proto, compile, package
#   Stage 2 (runtime): run the jar on a small JRE-only image
# ===================================================================

# ---------- Stage 1: build ----------
# Maven 3.9.x is required: the protobuf plugin needs Maven 3.9.6 or newer.
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy only pom.xml first and download dependencies.
# This layer is cached, so dependencies are re-downloaded only when pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source (including src/main/proto) and build.
# The protobuf plugin generates the gRPC classes during this step.
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Run as a non-root user (security best practice).
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/billing-service-0.0.1-SNAPSHOT.jar app.jar

USER spring

# 4001 = HTTP (REST, health checks), 9001 = gRPC (called by patient and appointment services)
EXPOSE 4001
EXPOSE 9001

ENTRYPOINT ["java", "-jar", "app.jar"]
