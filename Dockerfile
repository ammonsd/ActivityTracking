# Multi-stage build for optimized production image
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src /app/src
RUN mvn clean package -DskipTests -B

# Production runtime image - using Eclipse Temurin JRE (more compatible than distroless)
FROM eclipse-temurin:21-jre-jammy

# Create non-root user for security (similar to distroless)
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /opt

# Copy built JAR
COPY --from=build /app/target/*.jar /opt/app.jar

# Set ownership to non-root user
RUN chown -R appuser:appuser /opt

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Set production profile
ENV SPRING_PROFILES_ACTIVE=production

# JVM optimizations for containers
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# Run application
ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
