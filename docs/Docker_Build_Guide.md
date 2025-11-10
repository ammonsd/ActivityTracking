# Docker Build Options Quick Reference

## Fast Local Development (Recommended for frequent changes)

**Build Time:** ~10 seconds  
**Best For:** Local development with frequent code changes

### Steps:

```powershell
# 1. Build with Maven
.\mvnw.cmd clean package -DskipTests

# 2. Build and start Docker container
docker-compose --profile local-fast down
docker-compose --profile local-fast build
docker-compose --profile local-fast up -d
```

## Standard Build (Production-ready)

**Build Time:** ~120 seconds (first build), ~20-30 seconds (cached)  
**Best For:** CI/CD, production deployments, when you want a self-contained build

### Steps:

```powershell
# Build and start (Maven builds inside Docker)
docker-compose --profile host-db down
docker-compose --profile host-db build
docker-compose --profile host-db up -d
```

## Why Two Options?

### Dockerfile.local (Fast - 10s)

-   ✅ Uses pre-built JAR from `target/` directory
-   ✅ Very fast rebuild times
-   ✅ Perfect for local development
-   ❌ Requires manual Maven build first
-   ❌ Not self-contained

### Dockerfile (Standard - 120s first, 20-30s cached)

-   ✅ Self-contained (no external dependencies)
-   ✅ Production-ready
-   ✅ Caches dependencies (faster on subsequent builds)
-   ✅ Works in CI/CD without local Maven
-   ❌ Slower first build
-   ❌ Downloads all dependencies inside Docker

## Performance Improvements

### .dockerignore

-   Speeds up context transfer by excluding unnecessary files
-   Reduces build context from ~200MB to ~60MB
-   Already created and configured

### Docker Layer Caching

-   Standard build: First build ~120s, subsequent builds ~20-30s
-   As long as `pom.xml` doesn't change, dependencies are cached
-   Changing only source code triggers only the compilation layer (~10s)

## Recommendation

-   **Daily development:** Use `local-fast` profile (10s builds)
-   **Before commits/PRs:** Test with `host-db` profile (ensures clean build)
-   **Production/CI/CD:** Always use `host-db` profile (self-contained)
