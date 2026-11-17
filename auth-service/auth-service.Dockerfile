# ===================================================================
# Auth Service - multi-stage Docker build
#   Stage 1 (builder): compile and package the jar with Maven
#   Stage 2 (runtime): run the jar on a small JRE-only image
# ===================================================================

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy only pom.xml first and download dependencies.
# This layer is cached, so dependencies are re-downloaded only when pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source (including data.sql) and build.
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Run as a non-root user (security best practice).
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/auth-service-0.0.1-SNAPSHOT.jar app.jar

USER spring

# 4005 = HTTP (POST /login, GET /validate). Reached only via the API gateway.
EXPOSE 4005

# Secrets (DB_PASSWORD, JWT_SECRET) are passed at runtime with -e / --env-file,
# never baked into the image.
ENTRYPOINT ["java", "-jar", "app.jar"]
