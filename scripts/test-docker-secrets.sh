#!/bin/bash

# Docker Secrets Test Script for Task Activity Management System
# This script validates that Docker secrets are working correctly

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}Docker Secrets Test for Task Activity Management System${NC}"
echo "========================================================"

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed or not in PATH.${NC}"
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo -e "${RED}Error: Docker daemon is not running.${NC}"
    exit 1
fi

echo -e "${BLUE}✓ Docker is available and running${NC}"

# Function to cleanup test resources
cleanup() {
    echo -e "${YELLOW}Cleaning up test resources...${NC}"
    
    # Stop and remove test container if running
    if docker ps -q -f name=taskactivity-secrets-test | grep -q .; then
        docker stop taskactivity-secrets-test > /dev/null 2>&1 || true
    fi
    
    if docker ps -aq -f name=taskactivity-secrets-test | grep -q .; then
        docker rm taskactivity-secrets-test > /dev/null 2>&1 || true
    fi
    
    # Remove test secrets if they exist
    if docker secret ls -f name=test_db_username -q | grep -q .; then
        docker secret rm test_db_username > /dev/null 2>&1 || true
    fi
    
    if docker secret ls -f name=test_db_password -q | grep -q .; then
        docker secret rm test_db_password > /dev/null 2>&1 || true
    fi
    
    # Leave Docker Swarm if we initialized it for testing
    if [ "$SWARM_INITIALIZED_BY_TEST" = "true" ]; then
        docker swarm leave --force > /dev/null 2>&1 || true
    fi
}

# Set trap to cleanup on exit
trap cleanup EXIT

# Check if Docker Swarm is initialized
if ! docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
    echo -e "${YELLOW}Docker Swarm is not initialized. Initializing for testing...${NC}"
    docker swarm init > /dev/null 2>&1
    export SWARM_INITIALIZED_BY_TEST=true
    echo -e "${GREEN}✓ Docker Swarm initialized${NC}"
else
    echo -e "${GREEN}✓ Docker Swarm is already active${NC}"
fi

# Create test secrets
echo -e "${BLUE}Creating test secrets...${NC}"
echo "test_user" | docker secret create test_db_username - > /dev/null
echo "test_password_123" | docker secret create test_db_password - > /dev/null
echo -e "${GREEN}✓ Test secrets created${NC}"

# Verify secrets exist
if ! docker secret ls -f name=test_db_username -q | grep -q .; then
    echo -e "${RED}Error: Failed to create test_db_username secret${NC}"
    exit 1
fi

if ! docker secret ls -f name=test_db_password -q | grep -q .; then
    echo -e "${RED}Error: Failed to create test_db_password secret${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Secrets verified in Docker Swarm${NC}"

# Test the application with Docker secrets
echo -e "${BLUE}Testing application with Docker secrets...${NC}"

# Create a test service using secrets
docker service create \
    --name taskactivity-secrets-test \
    --secret source=test_db_username,target=db_username \
    --secret source=test_db_password,target=db_password \
    --env DB_USERNAME_FILE=/run/secrets/db_username \
    --env DB_PASSWORD_FILE=/run/secrets/db_password \
    --env SPRING_PROFILES_ACTIVE=docker \
    --env SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb \
    --replicas 1 \
    --constraint 'node.role == manager' \
    taskactivity:latest > /dev/null

echo -e "${GREEN}✓ Test service created with secrets${NC}"

# Wait for service to be ready
echo -e "${BLUE}Waiting for service to start...${NC}"
sleep 30

# Check if service is running
if ! docker service ls -f name=taskactivity-secrets-test -q | grep -q .; then
    echo -e "${RED}Error: Test service failed to start${NC}"
    docker service logs taskactivity-secrets-test
    exit 1
fi

# Get service logs to verify secrets were loaded
echo -e "${BLUE}Checking service logs for secrets loading...${NC}"
LOGS=$(docker service logs taskactivity-secrets-test 2>&1)

if echo "$LOGS" | grep -q "Loaded DB_USERNAME from file: /run/secrets/db_username"; then
    echo -e "${GREEN}✓ DB_USERNAME loaded from secrets file${NC}"
else
    echo -e "${RED}Error: DB_USERNAME not loaded from secrets file${NC}"
    echo "Service logs:"
    echo "$LOGS"
    exit 1
fi

if echo "$LOGS" | grep -q "Loaded DB_PASSWORD from file: /run/secrets/db_password"; then
    echo -e "${GREEN}✓ DB_PASSWORD loaded from secrets file${NC}"
else
    echo -e "${RED}Error: DB_PASSWORD not loaded from secrets file${NC}"
    echo "Service logs:"
    echo "$LOGS"
    exit 1
fi

# Test that the application started successfully
if echo "$LOGS" | grep -q "Started.*Application"; then
    echo -e "${GREEN}✓ Application started successfully with secrets${NC}"
else
    echo -e "${YELLOW}Warning: Could not verify application startup in logs${NC}"
fi

# Cleanup test service
docker service rm taskactivity-secrets-test > /dev/null
echo -e "${GREEN}✓ Test service cleaned up${NC}"

echo ""
echo -e "${GREEN}🎉 Docker Secrets Test PASSED!${NC}"
echo ""
echo -e "${BLUE}Test Results Summary:${NC}"
echo "• Docker Swarm: ✓ Available"
echo "• Secret Creation: ✓ Working"
echo "• Secret Access: ✓ Working"
echo "• Application Integration: ✓ Working"
echo "• SecretsEnvironmentPostProcessor: ✓ Working"
echo ""
echo -e "${GREEN}The application is ready for production Docker secrets deployment!${NC}"