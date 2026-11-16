# ===================================================================
# Patient Service - multi-stage Docker build
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

# Now copy the source and build (tests run in CI, not in the image build).
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Run as a non-root user (security best practice).
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/patient-service-0.0.1-SNAPSHOT.jar app.jar

USER spring

EXPOSE 4000

ENTRYPOINT ["java", "-jar", "app.jar"]
