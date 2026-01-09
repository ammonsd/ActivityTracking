#!/bin/bash

/**
 * Description: Docker Secrets Setup Script - helps create and manage Docker secrets for production deployment
 *
 * Author: Dean Ammons
 * Date: October 2025
 */

# Docker Secrets Setup Script for Task Activity Management System
# This script helps create and manage Docker secrets for production deployment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Docker Secrets Setup for Task Activity Management System${NC}"
echo "=========================================================="

# Check if Docker Swarm is initialized
if ! docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
    echo -e "${YELLOW}Warning: Docker Swarm is not initialized.${NC}"
    echo "To use Docker secrets, you need to initialize Docker Swarm:"
    echo "  docker swarm init"
    echo ""
    read -p "Initialize Docker Swarm now? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker swarm init
        echo -e "${GREEN}Docker Swarm initialized successfully.${NC}"
    else
        echo -e "${RED}Exiting. Docker Swarm is required for secrets.${NC}"
        exit 1
    fi
fi

# Function to create a secret
create_secret() {
    local secret_name=$1
    local secret_value=$2
    
    echo "Creating secret: $secret_name"
    echo "$secret_value" | docker secret create "$secret_name" -
    echo -e "${GREEN}Secret '$secret_name' created successfully.${NC}"
}

# Check if secrets already exist
if docker secret ls --format "{{.Name}}" | grep -q "taskactivity_db_username"; then
    echo -e "${YELLOW}Secret 'taskactivity_db_username' already exists.${NC}"
    read -p "Remove and recreate? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker secret rm taskactivity_db_username
        echo "Removed existing secret."
    else
        echo "Keeping existing secret."
    fi
fi

if docker secret ls --format "{{.Name}}" | grep -q "taskactivity_db_password"; then
    echo -e "${YELLOW}Secret 'taskactivity_db_password' already exists.${NC}"
    read -p "Remove and recreate? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker secret rm taskactivity_db_password
        echo "Removed existing secret."
    else
        echo "Keeping existing secret."
    fi
fi

# Create database username secret
if ! docker secret ls --format "{{.Name}}" | grep -q "taskactivity_db_username"; then
    echo ""
    read -p "Enter database username: " db_username
    if [ -z "$db_username" ]; then
        echo -e "${RED}Error: Database username cannot be empty.${NC}"
        exit 1
    fi
    create_secret "taskactivity_db_username" "$db_username"
fi

# Create database password secret
if ! docker secret ls --format "{{.Name}}" | grep -q "taskactivity_db_password"; then
    echo ""
    echo "Enter database password (input will be hidden):"
    read -s db_password
    if [ -z "$db_password" ]; then
        echo -e "${RED}Error: Database password cannot be empty.${NC}"
        exit 1
    fi
    create_secret "taskactivity_db_password" "$db_password"
fi

echo ""
echo -e "${GREEN}All secrets created successfully!${NC}"
echo ""
echo "To deploy the application with secrets, run:"
echo "  docker-compose --profile production up -d"
echo ""
echo "To view secrets (names only):"
echo "  docker secret ls"
echo ""
echo "To remove secrets when no longer needed:"
echo "  docker secret rm taskactivity_db_username taskactivity_db_password"