# =============================================================================
# QuickStack Backend - Multi-stage Dockerfile
# Security: OWASP Container Security best practices
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom files first (for layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY quickstack-common/pom.xml quickstack-common/
COPY quickstack-tenant/pom.xml quickstack-tenant/
COPY quickstack-branch/pom.xml quickstack-branch/
COPY quickstack-user/pom.xml quickstack-user/
COPY quickstack-product/pom.xml quickstack-product/
COPY quickstack-pos/pom.xml quickstack-pos/
COPY quickstack-app/pom.xml quickstack-app/

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY quickstack-common/src quickstack-common/src
COPY quickstack-tenant/src quickstack-tenant/src
COPY quickstack-branch/src quickstack-branch/src
COPY quickstack-user/src quickstack-user/src
COPY quickstack-product/src quickstack-product/src
COPY quickstack-pos/src quickstack-pos/src
COPY quickstack-app/src quickstack-app/src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Extract layers for efficient Docker caching
RUN java -Djarmode=layertools -jar quickstack-app/target/*.jar extract --destination extracted

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: Run as non-root user (ASVS V14.1.5)
RUN addgroup -g 1000 quickstack && \
    adduser -u 1000 -G quickstack -s /bin/sh -D quickstack

WORKDIR /app

# Copy extracted layers from builder (in order of change frequency)
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Security: Non-root user
USER quickstack:quickstack

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
