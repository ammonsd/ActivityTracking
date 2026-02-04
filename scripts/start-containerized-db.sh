#!/bin/bash
# Start containerized database environment
# Usage: wsl -u root, then ./start-containerized-db.sh
#
# This script:
# - Stops existing containers and removes volumes
# - Rebuilds the Docker images (use --no-cache for complete rebuild)
# - Starts the PostgreSQL container and application
# - Shows logs to verify startup

CACHE_OPTION=$1

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo "ERROR: Run as root: wsl -u root"
    exit 1
fi

# Navigate to project directory
cd /mnt/c/Users/deana/GitHub/ActivityTracking

echo "========================================="
echo "Starting Containerized Database Setup"
echo "========================================="
echo ""

# Step 1: Clean up existing containers
echo "Step 1: Stopping and removing existing containers and volumes..."
docker compose --profile containerized-db down -v

# Remove old images to ensure fresh build
echo "Removing old Docker images..."
docker rmi activitytracking-app-with-postgres 2>/dev/null || true
docker rmi activitytracking-postgres 2>/dev/null || true

echo ""

# Step 2: Set environment variables
echo "Step 2: Validating environment variables..."

# Validate required environment variables
if [ -z "$DB_USERNAME" ]; then
    echo "ERROR: DB_USERNAME environment variable is not set"
    echo "Load environment variables first using set-env-values.ps1 with -EncryptionKey"
    exit 1
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "ERROR: DB_PASSWORD environment variable is not set"
    echo "Load environment variables first using set-env-values.ps1 with -EncryptionKey"
    exit 1
fi

# Set optional environment variables
export ENABLE_FILE_LOGGING=${ENABLE_FILE_LOGGING:-true}
export DOCKER_BUILDKIT=0

echo "  DB_USERNAME: $DB_USERNAME"
echo "  ENABLE_FILE_LOGGING: $ENABLE_FILE_LOGGING"
if [ -n "$JWT_SECRET" ]; then
    echo "  JWT_SECRET: [set from .env file]"
fi
if [ -n "$APP_ADMIN_INITIAL_PASSWORD" ]; then
    echo "  APP_ADMIN_INITIAL_PASSWORD: [set from .env file]"
fi
echo ""

# Step 3: Create logs directory
echo "Step 3: Creating logs directory..."
mkdir -p /mnt/c/Logs
chmod 777 /mnt/c/Logs
echo ""

# Step 4: Rebuild Docker images
echo "Step 4: Rebuilding Docker images..."
if [ "$CACHE_OPTION" == "--no-cache" ]; then
    echo "⚠️  Building with --no-cache (slower but ensures fresh build)"
    docker compose --profile containerized-db build --no-cache
else
    echo "Building with cache (faster, use --no-cache for complete rebuild)"
    docker compose --profile containerized-db build
fi

BUILD_EXIT_CODE=$?
echo ""

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo "❌ ERROR: Docker build failed with exit code $BUILD_EXIT_CODE"
    exit 1
fi

echo "✓ Docker build completed successfully!"
echo ""

# Step 5: Start services
echo "Step 5: Starting PostgreSQL and application containers..."
docker compose --profile containerized-db up -d

echo ""
echo "Waiting for services to start..."
sleep 5

# Show status
echo ""
docker compose ps
echo ""

# Step 6: Monitor startup
echo "========================================="
echo "Monitoring Application Startup"
echo "========================================="
echo ""
echo "Checking for successful startup (this may take 30-60 seconds)..."
echo ""

# Wait and check for startup
for i in {1..15}; do
    # Check if application started
    if docker compose logs app-with-postgres 2>&1 | grep -q "Started TaskactivityApplication"; then
        echo ""
        echo "✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓"
        echo "✓  Application Started Successfully!  ✓"
        echo "✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓"
        echo ""
        echo "Access your application at:"
        echo "  http://localhost:8080"
        echo ""
        echo "View logs:"
        echo "  docker compose logs -f app-with-postgres  (application logs)"
        echo "  docker compose logs -f postgres           (database logs)"
        echo ""
        echo "Verify schema was created:"
        echo "  docker compose logs app-with-postgres | grep 'schema.sql'"
        echo ""
        echo "Stop application:"
        echo "  docker compose --profile containerized-db down"
        echo ""
        echo "Stop and remove volumes (fresh start next time):"
        echo "  docker compose --profile containerized-db down -v"
        echo ""
        docker compose logs -f app-with-postgres
        exit 0
    fi
    
    # Check for errors
    if docker compose logs app-with-postgres 2>&1 | grep -q "Schema-validation: missing table"; then
        echo ""
        echo "❌ ERROR: Schema validation error detected!"
        echo ""
        echo "This usually means the Docker image wasn't rebuilt properly."
        echo "Try running again with --no-cache:"
        echo "  ./start-containerized-db.sh --no-cache"
        echo ""
        echo "Full logs:"
        docker compose logs app-with-postgres
        exit 1
    fi
    
    echo -n "."
    sleep 5
done

echo ""
echo ""
echo "⚠️  Application may still be starting."
echo ""
echo "Check logs manually:"
echo "  docker compose logs -f app-with-postgres"
echo ""
echo "Look for:"
echo "  ✓ 'Executing SQL script from URL [classpath:schema.sql]'"
echo "  ✓ 'Started TaskactivityApplication'"
echo ""
echo "If you see 'Schema-validation: missing table' errors:"
echo "  Stop and rebuild with --no-cache:"
echo "  docker compose --profile containerized-db down -v"
echo "  ./start-containerized-db.sh --no-cache"
echo ""

exit 0
