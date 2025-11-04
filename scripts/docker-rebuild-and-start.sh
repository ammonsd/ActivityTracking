#!/bin/bash
# Full rebuild and restart script for WSL2
# Usage: wsl -u root, then ./rebuild-and-start.sh
# Optional: Add --no-cache to force complete rebuild

CACHE_OPTION=$1
DOCKER_PROFILE=$2

if [ "$CACHE_OPTION" != "--no-cache" ]; then
    CACHE_OPTION=
    DOCKER_PROFILE=$1
fi

echo "======================================================="
echo "Rebuild and Restart Application  "$DOCKER_PROFILE""
echo "======================================================="
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo "ERROR: Run as root: wsl -u root"
    exit 1
fi

# Navigate to project directory
cd /mnt/c/Users/deana/GitHub/ActivityTracking

# Step 1: Stop existing containers and remove volumes
echo "Step 1: Stopping existing containers..."
if [ "$DOCKER_PROFILE" == "containerized-db" ]; then
    echo "Removing volumes for containerized-db profile to ensure fresh database..."
    docker compose --profile $DOCKER_PROFILE down -v
    
    # Remove old images to force complete rebuild
    echo "Removing old Docker images for clean rebuild..."
    docker rmi activitytracking-app-with-postgres 2>/dev/null || true
    docker rmi activitytracking-postgres 2>/dev/null || true
else
    docker compose --profile $DOCKER_PROFILE down
    
    # Remove old image for host-db profile to ensure fresh build
    echo "Removing old Docker image for clean rebuild..."
    docker rmi taskactivity:latest 2>/dev/null || true
fi
echo ""

# Step 2: Rebuild Docker image
echo "Step 2: Rebuilding Docker image (this may take 10-15 minutes with Angular build)..."
echo "Note: Docker will use cached layers when possible for speed."
echo "      To force complete rebuild, use: ./scripts/rebuild-and-start.sh --no-cache <profile>"
echo ""
export DOCKER_BUILDKIT=1

# Check for --no-cache flag
if [ "$CACHE_OPTION" == "--no-cache" ]; then
    echo "⚠️  Running with --no-cache (slower but ensures fresh build)"
    echo ""
fi

# For containerized-db, use docker compose build to rebuild all services
# For other profiles, use docker build for backward compatibility
if [ "$DOCKER_PROFILE" == "containerized-db" ]; then
    echo "Using docker compose build for containerized-db profile..."
    docker compose --profile $DOCKER_PROFILE build $CACHE_OPTION
    BUILD_EXIT_CODE=$?
else
    echo "Using docker build for $DOCKER_PROFILE profile..."
    docker build $CACHE_OPTION -t taskactivity:latest .
    BUILD_EXIT_CODE=$?
fi

echo ""

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo "ERROR: Docker build failed with exit code $BUILD_EXIT_CODE"
    exit 1
fi

echo "✓ Docker build completed successfully!"

echo ""
echo "========================================="
echo "Build successful! Starting application..."
echo "========================================="
echo ""

# Step 3: Start application
if [ "$DOCKER_PROFILE" == "containerized-db" ]; then
    # For containerized-db, start services directly
    echo "Starting containerized-db services..."
    
    # Set environment variables
    export DB_USERNAME=postgres
    export DB_PASSWORD=N1ghrd01-1948
    export ENABLE_FILE_LOGGING=${ENABLE_FILE_LOGGING:-true}
    
    # Create logs directory
    mkdir -p /mnt/c/Logs
    chmod 777 /mnt/c/Logs
    
    # Start services
    docker compose --profile containerized-db up -d
    
    echo ""
    echo "Waiting for services to start..."
    sleep 10
    
    echo ""
    docker compose ps
    
    echo ""
    echo "========================================="
    echo "Checking Application Status"
    echo "========================================="
    echo ""
    
    # Wait and check for startup
    for i in {1..12}; do
        if docker compose logs app-with-postgres 2>&1 | grep -q "Started TaskactivityApplication"; then
            echo "✓✓✓ Application Started Successfully! ✓✓✓"
            echo ""
            echo "Access your application at:"
            echo "  http://localhost:8080"
            echo ""
            echo "View logs:"
            echo "  docker compose logs -f app-with-postgres"
            echo "  docker compose logs -f postgres"
            echo ""
            echo "Stop application:"
            echo "  docker compose --profile containerized-db down"
            exit 0
        fi
        sleep 5
    done
    
    echo "⚠️  Application may still be starting. Check logs:"
    echo "  docker compose logs -f app-with-postgres"
else
    # For other profiles, use start-wsl2.sh
    ./scripts/start-wsl2.sh $DOCKER_PROFILE
fi
