#!/bin/bash
# Check application status and logs

DOCKER_PROFILE=$1

echo "=================================================="
echo "Application Status Check $DOCKER_PROFILE"
echo "=================================================="
echo ""

cd /mnt/c/Users/deana/GitHub/ActivityTracking

echo "Step 1: Container Status"
echo "------------------------"
docker compose ps
echo ""

echo "Step 2: Check if container is running"
echo "--------------------------------------"
CONTAINER_ID=$(docker ps --filter "name=activitytracking-app" --format "{{.ID}}")
if [ -z "$CONTAINER_ID" ]; then
    echo "✗ Container is not running!"
    echo ""
    echo "Checking why..."
    docker compose logs --tail=50
else
    echo "✓ Container is running: $CONTAINER_ID"
    echo ""
    
    echo "Step 3: Check application logs"
    echo "-------------------------------"
    docker compose logs --tail=100 app
    echo ""
    
    echo "Step 4: Check Java process"
    echo "-----------------------------------------------"
    docker exec $CONTAINER_ID ps aux 2>/dev/null | grep java || echo "Could not check Java process"
    echo ""
    
    echo "Step 5: Test connection from host"
    echo "----------------------------------------------"
    # Test from host machine instead of inside container
    if command -v curl &> /dev/null; then
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 2>/dev/null)
        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "401" ]; then
            echo "✓ Application is responding on http://localhost:8080 (HTTP $HTTP_CODE)"
        else
            echo "✗ Application not responding on http://localhost:8080 (HTTP code: $HTTP_CODE)"
            echo "  (This is normal if app is still starting up)"
        fi
    else
        # Fallback: check if port is accessible using /dev/tcp
        if timeout 2 bash -c "echo > /dev/tcp/localhost/8080" 2>/dev/null; then
            echo "✓ Port 8080 is accessible on localhost"
        else
            echo "✗ Port 8080 is not accessible"
            echo "  (This is normal if app is still starting up)"
        fi
    fi
fi

echo ""
echo "Step 6: Port forwarding check"
echo "------------------------------"
echo "Ports exposed by container:"
docker port $(docker ps --filter "name=activitytracking-app" --format "{{.ID}}") 2>/dev/null || echo "No ports found"

echo ""
echo "========================================="
echo "Troubleshooting Commands:"
echo "========================================="
echo ""
echo "View full logs:"
echo "  docker compose logs -f"
echo ""
echo "View last 50 lines:"
echo "  docker compose logs --tail=50 app"
echo ""
echo "Check if Spring Boot started:"
echo "  docker compose logs app | grep 'Started'"
echo ""
echo "Restart the application:"
echo "  docker compose --profile $DOCKER_PROFILE restart"
echo ""
echo "Stop and check logs:"
echo "  docker compose --profile $DOCKER_PROFILE down"
echo "  docker compose --profile $DOCKER_PROFILE up"
echo ""
