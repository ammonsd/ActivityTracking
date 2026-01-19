---
name: docker-operations
description: "Manages Docker build, run, troubleshoot operations including multi-stage builds, docker-compose profiles, container debugging, and image optimization"
---

# Docker Operations Skill

This skill handles Docker build strategies, troubleshooting, and optimization for ActivityTracking.

## When to Use

- Building Docker images
- Running with docker-compose
- Troubleshooting container issues
- Optimizing image size
- Debugging network/connectivity issues

## Build Strategies

### Development Build (Fast iteration)

```bash
# Build without tests (fastest)
docker build -t taskactivity:dev .

# Run with hot reload
docker-compose up
```

### Production Build (Optimized)

```bash
# Multi-stage build with tests
docker build -t taskactivity:prod --target production .

# With build cache
docker build -t taskactivity:prod --cache-from taskactivity:latest .
```

### CI/CD Build

```bash
# Jenkins automated build
docker build -t taskactivity:${BUILD_NUMBER} \
  --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
  --build-arg VCS_REF=${GIT_COMMIT} \
  .
```

## Docker Compose Profiles

### Profile: Development

```bash
docker-compose --profile dev up

# Includes:
# - PostgreSQL
# - Application with hot reload
# - Frontend dev server
```

### Profile: Local Testing

```bash
docker-compose --profile test up

# Includes:
# - PostgreSQL
# - Application
# - Test containers
```

### Profile: Production Simulation

```bash
docker-compose --profile prod up

# Includes:
# - PostgreSQL
# - Optimized application build
# - Nginx reverse proxy
# - Cloudflared tunnel (optional)
```

## Dockerfile Templates

### Multi-Stage Dockerfile (Current)

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Frontend Build
FROM node:20-alpine AS frontend
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# Stage 3: Production
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=frontend /app/dist/frontend ./static
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Development Dockerfile

```dockerfile
FROM maven:3.9-eclipse-temurin-21
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
CMD ["mvn", "spring-boot:run"]
```

## Docker Compose Configuration

### docker-compose.yml Structure

```yaml
version: "3.8"

services:
    postgres:
        image: postgres:15-alpine
        environment:
            POSTGRES_DB: taskactivity
            POSTGRES_USER: postgres
            POSTGRES_PASSWORD: ${DB_PASSWORD}
        volumes:
            - postgres_data:/var/lib/postgresql/data
            - ./sql:/docker-entrypoint-initdb.d
        ports:
            - "5432:5432"
        healthcheck:
            test: ["CMD-SHELL", "pg_isready -U postgres"]
            interval: 10s
            timeout: 5s
            retries: 5

    app:
        build:
            context: .
            dockerfile: Dockerfile
        environment:
            SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/taskactivity
            SPRING_DATASOURCE_USERNAME: postgres
            SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
            JWT_SECRET: ${JWT_SECRET}
        depends_on:
            postgres:
                condition: service_healthy
        ports:
            - "8080:8080"
        profiles:
            - dev
            - prod

    frontend:
        build:
            context: ./frontend
            dockerfile: Dockerfile.dev
        volumes:
            - ./frontend/src:/app/src
        ports:
            - "4200:4200"
        profiles:
            - dev

volumes:
    postgres_data:
```

## Troubleshooting Guide

### Container Won't Start

```bash
# Check logs
docker logs taskactivity-app

# Common issues:
# 1. Database not ready
docker-compose up postgres
docker exec -it postgres_container pg_isready -U postgres

# 2. Port already in use
netstat -ano | findstr :8080
# Kill process using port

# 3. Environment variables missing
docker exec taskactivity-app env | grep JWT_SECRET
```

### Database Connection Issues

```bash
# Test database connectivity from app container
docker exec -it taskactivity-app bash
psql -h postgres -U postgres -d taskactivity

# Check network
docker network inspect bridge

# Verify DNS resolution
docker exec -it taskactivity-app nslookup postgres
```

### Build Failures

```bash
# Clear build cache
docker builder prune -a

# Build with no cache
docker build --no-cache -t taskactivity:latest .

# Check disk space
docker system df
docker system prune -a
```

### Performance Issues

```bash
# Check resource usage
docker stats

# Limit resources
docker run -m 512m --cpus=1 taskactivity:latest

# In docker-compose:
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

### Image Size Too Large

```bash
# Check image layers
docker history taskactivity:latest

# Analyze image size
docker images | grep taskactivity

# Common fixes:
# 1. Use alpine base images
# 2. Multi-stage builds
# 3. Combine RUN commands
# 4. Remove build dependencies
# 5. Use .dockerignore
```

## Image Optimization

### .dockerignore File

```
# Git
.git
.gitignore

# IDE
.idea
*.iml
.vscode

# Build artifacts
target/
node_modules/
frontend/dist/

# Documentation
docs/
*.md

# Tests
src/test/

# CI/CD
.github/
jenkins/
```

### Size Optimization Techniques

**Before:**

```dockerfile
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get install -y vim
```

Size: 500MB

**After:**

```dockerfile
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl vim && \
    rm -rf /var/lib/apt/lists/*
```

Size: 350MB

### Best Practices

1. **Use alpine images**

    ```dockerfile
    FROM eclipse-temurin:21-jre-alpine  # 180MB
    # vs
    FROM eclipse-temurin:21-jre  # 450MB
    ```

2. **Order layers by change frequency**

    ```dockerfile
    # Dependencies change rarely - cache this
    COPY pom.xml .
    RUN mvn dependency:go-offline

    # Source changes often - do this last
    COPY src ./src
    RUN mvn package
    ```

3. **Minimize layers**

    ```dockerfile
    # Good: 1 layer
    RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

    # Bad: 3 layers
    RUN apt-get update
    RUN apt-get install -y curl
    RUN rm -rf /var/lib/apt/lists/*
    ```

## Health Checks

### Application Health Check

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

### In docker-compose

```yaml
services:
    app:
        healthcheck:
            test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
            interval: 30s
            timeout: 10s
            retries: 3
            start_period: 40s
```

## Network Configuration

### Custom Network

```yaml
networks:
    taskactivity-network:
        driver: bridge

services:
    app:
        networks:
            - taskactivity-network
    postgres:
        networks:
            - taskactivity-network
```

### DNS Configuration

```yaml
services:
    app:
        dns:
            - 8.8.8.8
            - 8.8.4.4
```

## Volume Management

### Named Volumes (Recommended)

```yaml
volumes:
    postgres_data:
    maven_cache:

services:
    postgres:
        volumes:
            - postgres_data:/var/lib/postgresql/data
    app:
        volumes:
            - maven_cache:/root/.m2
```

### Bind Mounts (Development)

```yaml
services:
    app:
        volumes:
            - ./src:/app/src # Hot reload
            - maven_cache:/root/.m2 # Cache dependencies
```

## Environment Variables

### .env File

```bash
# Database
DB_PASSWORD=secure_password_here
POSTGRES_USER=postgres
POSTGRES_DB=taskactivity

# Application
JWT_SECRET=your-256-bit-secret-key-here
JWT_EXPIRATION=86400000

# AWS (Production)
AWS_REGION=us-east-1
S3_BUCKET_NAME=taskactivity-receipts-prod
```

### Override in docker-compose

```yaml
services:
    app:
        environment:
            - SPRING_PROFILES_ACTIVE=docker
            - LOG_LEVEL=DEBUG
        env_file:
            - .env
```

## Cleanup Commands

```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune -a

# Remove unused volumes
docker volume prune

# Full cleanup (CAUTION: removes everything)
docker system prune -a --volumes

# Remove specific container
docker rm -f taskactivity-app

# Remove specific image
docker rmi taskactivity:latest
```

## Docker Commands Reference

```bash
# Build
docker build -t taskactivity:latest .

# Run
docker run -p 8080:8080 --name taskactivity-app taskactivity:latest

# Compose
docker-compose up -d
docker-compose down
docker-compose restart app

# Logs
docker logs -f taskactivity-app
docker-compose logs -f app

# Shell access
docker exec -it taskactivity-app bash
docker exec -it taskactivity-app sh  # Alpine

# Inspect
docker inspect taskactivity-app
docker stats taskactivity-app

# Copy files
docker cp local-file.txt taskactivity-app:/app/
docker cp taskactivity-app:/app/logs.txt ./
```

## Production Deployment

### Push to ECR

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Tag image
docker tag taskactivity:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/taskactivity:latest

# Push
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/taskactivity:latest
```

### ECS Task Definition

```json
{
    "containerDefinitions": [
        {
            "name": "taskactivity",
            "image": "123456789012.dkr.ecr.us-east-1.amazonaws.com/taskactivity:latest",
            "memory": 512,
            "cpu": 256,
            "essential": true,
            "portMappings": [
                {
                    "containerPort": 8080,
                    "protocol": "tcp"
                }
            ],
            "environment": [
                { "name": "SPRING_PROFILES_ACTIVE", "value": "prod" }
            ],
            "secrets": [
                {
                    "name": "JWT_SECRET",
                    "valueFrom": "arn:aws:secretsmanager:..."
                }
            ]
        }
    ]
}
```

## Memory Bank References

- Check `ai/devops-practices.md` for Docker best practices
- Check `ai/project-overview.md` for build requirements
- Check `Dockerfile` for current multi-stage setup
