# Multi-stage build for optimized production image
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code AND frontend directory for Angular build
COPY src /app/src
COPY frontend /app/frontend

# Debug: Show what files are in frontend/dist before Maven runs
RUN ls -la /app/frontend/dist/app/browser/main-*.js || echo "No main-*.js files found"

RUN mvn clean package -DskipTests -Dskip.frontend.build=true -B

# Production runtime image - using Eclipse Temurin JRE (more compatible than distroless)
FROM eclipse-temurin:21-jre-jammy

# Install PostgreSQL client, cloudflared, and setup directories in one layer
RUN apt-get update && \
    apt-get install -y postgresql-client curl && \
    # Install cloudflared
    curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o /usr/local/bin/cloudflared && \
    chmod +x /usr/local/bin/cloudflared && \
    # Create non-root user
    groupadd -r appuser && useradd -r -g appuser appuser && \
    # Create cloudflared config directory
    mkdir -p /etc/cloudflared && \
    chown -R appuser:appuser /etc/cloudflared && \
    # Cleanup
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /opt

# Copy built JAR and startup script
COPY --from=build /app/target/*.jar /opt/app.jar
COPY scripts/docker-entrypoint.sh /opt/docker-entrypoint.sh

# Set permissions and ownership in one layer
RUN chmod +x /opt/docker-entrypoint.sh && \
    chown -R appuser:appuser /opt

# Switch to non-root user
USER appuser

# Expose port 8080 (standard Spring Boot port - no special privileges needed)
EXPOSE 8080

# JVM optimizations for containers
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# Run startup script that launches both cloudflared and the app
ENTRYPOINT ["/opt/docker-entrypoint.sh"]
