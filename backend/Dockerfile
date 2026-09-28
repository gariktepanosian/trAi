# ─────────────────────────────────────────────
# Stage 1 — Build
# ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Gradle wrapper and build files first (layer-cache friendly)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Download dependencies before copying source (maximises cache reuse)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon 2>/dev/null || true

# Copy full source
COPY src/ src/

# Build the fat JAR, skip tests (tests run in CI separately)
RUN ./gradlew clean bootJar --no-daemon -x test

# ─────────────────────────────────────────────
# Stage 2 — Runtime (minimal JRE)
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Install fonts for Cyrillic + Armenian PDF/image rendering support
RUN apk add --no-cache ttf-dejavu fontconfig

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Non-root user for security
RUN addgroup -S trai && adduser -S trai -G trai
USER trai

EXPOSE 8080

# Use prod profile by default; override via env var SPRING_PROFILES_ACTIVE
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]
