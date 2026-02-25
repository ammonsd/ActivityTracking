# Multi-stage build for optimized production image
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build

# Set working directory
WORKDIR /app

# Install Node.js v20 LTS via NodeSource (separate cached layer)
# Only re-runs when the Node.js version needs to change
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl gnupg && \
    curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | \
        gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" | \
        tee /etc/apt/sources.list.d/nodesource.list && \
    apt-get update && apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

# Copy pom.xml and download Maven dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy npm config and package definition files for Docker layer caching.
# These layers only rebuild when dependency files change, not on source code changes.
# This is the key to avoiding re-downloading all npm packages on every build.
COPY frontend/.npmrc frontend/package.json frontend/package-lock.json frontend/
COPY frontend-react/.npmrc frontend-react/package.json frontend-react/package-lock.json frontend-react/

# Pre-install npm dependencies using npm ci (Docker layer cached until package.json changes).
# npm ci is deterministic, lock-file-based, and preferred for CI/CD.
# When package.json is unchanged, Docker reuses this cached layer - no network required.
RUN npm ci --prefix frontend
RUN npm ci --prefix frontend-react

# Copy source code AND frontend directories for Angular and React builds.
# Docker COPY merges into existing directories, so node_modules installed above are preserved.
COPY src /app/src
COPY frontend /app/frontend
COPY frontend-react /app/frontend-react

# Build with frontend (Maven will build both Angular and React).
# npm install phases are fast since node_modules were pre-populated above.
RUN mvn package -DskipTests -B

# Production runtime image - using Eclipse Temurin JRE (more compatible than distroless)
FROM eclipse-temurin:21-jre-jammy

# Install PostgreSQL client, cloudflared, and setup directories in one layer
RUN apt-get update && \
    apt-get install -y postgresql-client curl && \
    # Install cloudflared with retry and increased timeout
    curl -L --retry 3 --retry-delay 2 --connect-timeout 30 --max-time 600 \
        https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 \
        -o /usr/local/bin/cloudflared && \
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
