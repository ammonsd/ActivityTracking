#!/bin/bash

/**
 * Description: Docker deployment script for starting application with correct database connection for WSL2
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

# Start application with correct database connection for WSL2
#
# Usage: 
#   wsl -u root bash /mnt/c/Users/YOUR_USERNAME/GitHub/ActivityTracking/scripts/start-wsl2.sh <profile>
#
# Profiles:
#   host-db    - Full rebuild from source (slow ~90s, ensures latest code)
#   local-fast - Uses pre-built JAR (fast ~7s, requires manual mvnw package first)
#
# This script removes old Docker images to ensure you're always running the latest code

DOCKER_PROFILE=${1:-""}
DOCKER_COMPOSE_OPTIONS=${2:-""}
DOCKER_CONTAINER_NAME="activitytracking"

echo "========================================="
echo "Starting Application $DOCKER_PROFILE"
echo "========================================="
echo ""

if [ "$EUID" -ne 0 ]; then 
    echo "ERROR: Run as root: wsl -u root"
    exit 1
fi

# Start Docker
echo "Starting Docker service..."
service docker start
sleep 2

# Get Windows host IP
WINDOWS_IP=$(ip route show | grep -i default | awk '{ print $3}')
echo "Windows host IP: $WINDOWS_IP"
echo ""

# Stop any running containers
echo "Stopping existing containers..."
# Force remove any existing containers first to avoid network conflicts
docker ps -a --filter "name=$DOCKER_CONTAINER_NAME*" --format "{{.Names}}" | xargs -r docker rm -f 2>/dev/null || true
docker compose --profile "$DOCKER_PROFILE" down

# Remove old Docker images to force rebuild with latest code
echo "Removing old Docker images..."
docker rmi ${DOCKER_CONTAINER_NAME}-app 2>/dev/null || true
docker rmi ${DOCKER_CONTAINER_NAME}-app-local-fast 2>/dev/null || true

# Set environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=N1ghrd01-1948
export SPRING_DATASOURCE_URL="jdbc:postgresql://${WINDOWS_IP}:5432/AmmoP1DB"

# File logging control (set to 'false' to disable file logging)
# Uncomment the line below to disable file logging:
# export ENABLE_FILE_LOGGING=false
export ENABLE_FILE_LOGGING=${ENABLE_FILE_LOGGING:-true}

echo "Database connection: $SPRING_DATASOURCE_URL"
echo "Profile: $DOCKER_PROFILE"
echo "File logging: $ENABLE_FILE_LOGGING"
if [ -n "$DOCKER_COMPOSE_OPTIONS" ]; then
    echo "Docker Compose options: $DOCKER_COMPOSE_OPTIONS"
fi
echo ""

# Create logs directory
mkdir -p /mnt/c/Logs
chmod 777 /mnt/c/Logs

# Start application
echo "Starting application..."
docker compose --profile "$DOCKER_PROFILE" up -d $DOCKER_COMPOSE_OPTIONS

echo ""
echo "Waiting for application to start (this may take 30-60 seconds)..."
sleep 5

# Show status
echo ""
docker compose ps

echo ""
echo "========================================="
echo "Checking Application Status"
echo "========================================="
echo ""

# Wait and check for startup
for i in {1..12}; do
    if docker compose logs app 2>&1 | grep -q "Started TaskactivityApplication"; then
        echo "✓✓✓ Application Started Successfully! ✓✓✓"
        echo ""
        echo "Access your application at:"
        echo "  http://localhost:8080"
        echo ""
        echo "Stop application:"
        echo "  docker compose --profile \"$DOCKER_PROFILE\" down"
        echo ""
        echo "========================================="
        echo "Following application logs..."
        echo "Press Ctrl+C to stop viewing logs"
        echo "(Docker will continue running in background)"
        echo "========================================="
        echo ""
        # Follow logs - this keeps the WSL session and Docker stable
        docker compose logs -f app
        exit 0
    fi
    echo "Still starting... ($i/12)"
    sleep 5
done

echo ""
echo "Application is taking longer than expected to start."
echo "Check logs for errors:"
echo "  docker compose logs app"
echo ""
