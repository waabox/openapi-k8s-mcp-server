# Multi-stage Dockerfile for OpenAPI MCP Server
# @author waabox(emiliano[at]fanki[dot]co)

# =============================================================================
# Stage 1: Build
# =============================================================================
FROM maven:3-amazoncorretto-21 AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests -B

# =============================================================================
# Stage 2: Runtime
# =============================================================================
FROM amazoncorretto:21

WORKDIR /app

# Create non-root user
RUN yum install -y procps-ng shadow-utils && \
    groupadd -g 1000 appgroup && \
    useradd -u 1000 -g appgroup -m appuser && \
    yum clean all

# Copy the built JAR from builder stage
COPY --from=builder /app/target/openapi-mcp-server-*.jar app.jar

# Create the data directory for embedded Derby
RUN mkdir -p /app/data && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose MCP server port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    --enable-preview \
    -Djava.security.egd=file:/dev/./urandom"

ENV DERBY_DB_PATH=/app/data/openapi_mcp

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]