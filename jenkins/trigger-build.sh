#!/usr/bin/env bash

/**
 * Description: Trigger Jenkins Build via CLI - triggers Jenkins builds for deployment, build-only, or rollback actions
 *
 * Author: Dean Ammons
 * Date: October 2025
 */

################################################################################
# Trigger Jenkins Build via CLI
#
# Usage: ./trigger-build.sh <environment> <action>
# Example: ./trigger-build.sh production deploy
#
# Actions: deploy, build-only, rollback
#
# Prerequisites:
# - Jenkins CLI jar downloaded
# - JENKINS_URL environment variable set
# - JENKINS_USER and JENKINS_TOKEN for authentication
################################################################################

set -e

# Configuration
JENKINS_URL=${JENKINS_URL:-"http://localhost:8080"}
JENKINS_USER=${JENKINS_USER:-"admin"}
JENKINS_TOKEN=${JENKINS_TOKEN:-""}
JENKINS_CLI_JAR="jenkins-cli.jar"
JOB_NAME="TaskActivity-Pipeline"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse arguments
if [ $# -lt 2 ]; then
    echo "Usage: $0 <environment> <action>"
    echo ""
    echo "Environments: dev, staging, production"
    echo "Actions: deploy, build-only, rollback"
    echo ""
    echo "Example: $0 production deploy"
    exit 1
fi

ENVIRONMENT=$1
ACTION=$2

# Validate environment
if [[ ! "${ENVIRONMENT}" =~ ^(dev|staging|production)$ ]]; then
    echo -e "${RED}ERROR: Invalid environment. Must be dev, staging, or production${NC}"
    exit 1
fi

# Validate action
if [[ ! "${ACTION}" =~ ^(deploy|build-only|rollback)$ ]]; then
    echo -e "${RED}ERROR: Invalid action. Must be deploy, build-only, or rollback${NC}"
    exit 1
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Trigger Jenkins Build${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Jenkins URL: ${JENKINS_URL}"
echo -e "Job: ${JOB_NAME}"
echo -e "Environment: ${GREEN}${ENVIRONMENT}${NC}"
echo -e "Action: ${GREEN}${ACTION}${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Jenkins CLI jar exists
if [ ! -f "${JENKINS_CLI_JAR}" ]; then
    echo -e "${YELLOW}Jenkins CLI jar not found. Downloading...${NC}"
    curl -o "${JENKINS_CLI_JAR}" "${JENKINS_URL}/jnlpJars/jenkins-cli.jar"
    echo -e "${GREEN}✓ Downloaded Jenkins CLI${NC}"
fi

# Check authentication
if [ -z "${JENKINS_TOKEN}" ]; then
    echo -e "${RED}ERROR: JENKINS_TOKEN not set${NC}"
    echo ""
    echo "To generate a token:"
    echo "1. Go to ${JENKINS_URL}/me/configure"
    echo "2. Click 'Add new Token'"
    echo "3. Copy the token and export it:"
    echo "   export JENKINS_TOKEN='your-token-here'"
    exit 1
fi

# Build parameters
PARAMS="-p ENVIRONMENT=${ENVIRONMENT} -p DEPLOY_ACTION=${ACTION}"

# Additional parameters based on environment
if [ "${ENVIRONMENT}" == "production" ]; then
    PARAMS="${PARAMS} -p SKIP_TESTS=false -p NO_CACHE=false"
else
    PARAMS="${PARAMS} -p SKIP_TESTS=false -p NO_CACHE=false"
fi

echo -e "${YELLOW}Triggering build with parameters:${NC}"
echo -e "  ENVIRONMENT=${ENVIRONMENT}"
echo -e "  DEPLOY_ACTION=${ACTION}"
echo -e "  SKIP_TESTS=false"
echo -e "  NO_CACHE=false"
echo ""

# Trigger build
echo -e "${YELLOW}Sending build request...${NC}"
BUILD_OUTPUT=$(java -jar "${JENKINS_CLI_JAR}" \
    -s "${JENKINS_URL}" \
    -auth "${JENKINS_USER}:${JENKINS_TOKEN}" \
    build "${JOB_NAME}" \
    ${PARAMS} \
    -s -v 2>&1)

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build triggered successfully${NC}"
    echo ""
    
    # Extract build number if available
    if echo "${BUILD_OUTPUT}" | grep -q "Started"; then
        BUILD_NUMBER=$(echo "${BUILD_OUTPUT}" | grep -oP '#\K[0-9]+' | head -1)
        echo -e "${GREEN}Build Number: #${BUILD_NUMBER}${NC}"
        echo -e "Monitor at: ${JENKINS_URL}/job/${JOB_NAME}/${BUILD_NUMBER}/console"
    fi
else
    echo -e "${RED}✗ Failed to trigger build${NC}"
    echo "${BUILD_OUTPUT}"
    exit 1
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Next Steps${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "1. Monitor build progress in Jenkins UI"
echo -e "2. Check logs: ./get-build-logs.sh <build-number>"
echo -e "3. Verify deployment: ./check-deployment.sh ${ENVIRONMENT}"
echo ""
