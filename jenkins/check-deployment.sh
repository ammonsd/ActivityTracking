#!/usr/bin/env bash

################################################################################
# Check ECS Deployment Status
#
# Usage: ./check-deployment.sh <environment>
# Example: ./check-deployment.sh production
#
# This script checks the status of an ECS deployment including:
# - Running task count
# - Task definition version
# - Deployment status
# - Recent task events
# - Health check status
################################################################################

set -e

# Configuration
AWS_REGION=${AWS_REGION:-us-east-1}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
if [ $# -lt 1 ]; then
    echo "Usage: $0 <environment>"
    echo "Example: $0 production"
    exit 1
fi

ENVIRONMENT=$1
CLUSTER_NAME="taskactivity-cluster-${ENVIRONMENT}"
SERVICE_NAME="taskactivity-service-${ENVIRONMENT}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ECS Deployment Status Check${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Environment: ${GREEN}${ENVIRONMENT}${NC}"
echo -e "Cluster: ${CLUSTER_NAME}"
echo -e "Service: ${SERVICE_NAME}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if cluster exists
echo -e "${YELLOW}Checking if cluster exists...${NC}"
if ! aws ecs describe-clusters \
    --clusters "${CLUSTER_NAME}" \
    --region "${AWS_REGION}" \
    --query 'clusters[0].clusterName' \
    --output text 2>/dev/null | grep -q "${CLUSTER_NAME}"; then
    echo -e "${RED}ERROR: Cluster ${CLUSTER_NAME} not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Cluster exists${NC}"
echo ""

# Get service information
echo -e "${YELLOW}Fetching service information...${NC}"
SERVICE_INFO=$(aws ecs describe-services \
    --cluster "${CLUSTER_NAME}" \
    --services "${SERVICE_NAME}" \
    --region "${AWS_REGION}" \
    --query 'services[0]' \
    --output json)

if [ "$(echo "${SERVICE_INFO}" | jq -r '.serviceName')" == "null" ]; then
    echo -e "${RED}ERROR: Service ${SERVICE_NAME} not found${NC}"
    exit 1
fi

# Extract service details
RUNNING_COUNT=$(echo "${SERVICE_INFO}" | jq -r '.runningCount')
DESIRED_COUNT=$(echo "${SERVICE_INFO}" | jq -r '.desiredCount')
PENDING_COUNT=$(echo "${SERVICE_INFO}" | jq -r '.pendingCount')
TASK_DEFINITION=$(echo "${SERVICE_INFO}" | jq -r '.taskDefinition' | awk -F/ '{print $NF}')
STATUS=$(echo "${SERVICE_INFO}" | jq -r '.status')

echo -e "${GREEN}✓ Service found${NC}"
echo ""

# Display service status
echo -e "${BLUE}Service Status:${NC}"
echo -e "  Status: ${GREEN}${STATUS}${NC}"
echo -e "  Desired Tasks: ${DESIRED_COUNT}"
echo -e "  Running Tasks: ${RUNNING_COUNT}"
echo -e "  Pending Tasks: ${PENDING_COUNT}"
echo -e "  Task Definition: ${TASK_DEFINITION}"
echo ""

# Check if deployment is stable
if [ "${RUNNING_COUNT}" -eq "${DESIRED_COUNT}" ] && [ "${PENDING_COUNT}" -eq 0 ]; then
    echo -e "${GREEN}✓ Deployment is STABLE${NC}"
else
    echo -e "${YELLOW}⚠ Deployment is IN PROGRESS or UNSTABLE${NC}"
fi
echo ""

# Get deployment information
echo -e "${BLUE}Deployment Information:${NC}"
echo "${SERVICE_INFO}" | jq -r '.deployments[] | 
    "  ID: \(.id | split("/") | .[-1])\n" +
    "  Status: \(.rolloutState // .status)\n" +
    "  Desired: \(.desiredCount)\n" +
    "  Running: \(.runningCount)\n" +
    "  Pending: \(.pendingCount)\n" +
    "  Task Definition: \(.taskDefinition | split("/") | .[-1])\n" +
    "  Created: \(.createdAt)\n" +
    "  Updated: \(.updatedAt)\n" +
    "---"'
echo ""

# Get running tasks
echo -e "${BLUE}Running Tasks:${NC}"
TASK_ARNS=$(aws ecs list-tasks \
    --cluster "${CLUSTER_NAME}" \
    --service-name "${SERVICE_NAME}" \
    --region "${AWS_REGION}" \
    --query 'taskArns[]' \
    --output json)

if [ "$(echo "${TASK_ARNS}" | jq 'length')" -eq 0 ]; then
    echo -e "${YELLOW}  No running tasks${NC}"
else
    TASK_DETAILS=$(aws ecs describe-tasks \
        --cluster "${CLUSTER_NAME}" \
        --tasks $(echo "${TASK_ARNS}" | jq -r '.[]') \
        --region "${AWS_REGION}" \
        --query 'tasks[]' \
        --output json)
    
    echo "${TASK_DETAILS}" | jq -r '.[] | 
        "  Task ID: \(.taskArn | split("/") | .[-1])\n" +
        "  Status: \(.lastStatus)\n" +
        "  Health: \(.healthStatus // "N/A")\n" +
        "  Started: \(.startedAt // "Not started")\n" +
        "  CPU: \(.cpu)\n" +
        "  Memory: \(.memory)\n" +
        "---"'
fi
echo ""

# Get recent events
echo -e "${BLUE}Recent Service Events:${NC}"
echo "${SERVICE_INFO}" | jq -r '.events[0:5][] | 
    "  \(.createdAt | split(".")[0]): \(.message)"'
echo ""

# Check task health
echo -e "${BLUE}Task Health Status:${NC}"
if [ "$(echo "${TASK_DETAILS}" | jq -r '.[].healthStatus' | grep -c "HEALTHY")" -gt 0 ]; then
    HEALTHY_COUNT=$(echo "${TASK_DETAILS}" | jq -r '.[].healthStatus' | grep -c "HEALTHY")
    echo -e "${GREEN}✓ ${HEALTHY_COUNT} task(s) are healthy${NC}"
else
    echo -e "${YELLOW}⚠ No healthy tasks detected${NC}"
fi
echo ""

# Get CloudWatch logs for latest task
echo -e "${BLUE}Recent Logs (last 10 lines from latest task):${NC}"
LOG_GROUP="/ecs/taskactivity-${ENVIRONMENT}"
LATEST_TASK=$(echo "${TASK_ARNS}" | jq -r '.[0]' | awk -F/ '{print $NF}')

if [ -n "${LATEST_TASK}" ] && [ "${LATEST_TASK}" != "null" ]; then
    LOG_STREAM="ecs/taskactivity-${ENVIRONMENT}/${LATEST_TASK}"
    
    if aws logs get-log-events \
        --log-group-name "${LOG_GROUP}" \
        --log-stream-name "${LOG_STREAM}" \
        --limit 10 \
        --region "${AWS_REGION}" \
        --query 'events[].[timestamp,message]' \
        --output text 2>/dev/null; then
        echo -e "${GREEN}✓ Logs retrieved${NC}"
    else
        echo -e "${YELLOW}⚠ Could not retrieve logs (stream may not exist yet)${NC}"
    fi
else
    echo -e "${YELLOW}⚠ No tasks available to retrieve logs${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ "${RUNNING_COUNT}" -eq "${DESIRED_COUNT}" ] && [ "${PENDING_COUNT}" -eq 0 ]; then
    echo -e "${GREEN}✓ Deployment Status: STABLE${NC}"
    echo -e "${GREEN}✓ All tasks are running as expected${NC}"
    exit 0
elif [ "${PENDING_COUNT}" -gt 0 ]; then
    echo -e "${YELLOW}⚠ Deployment Status: IN PROGRESS${NC}"
    echo -e "${YELLOW}⚠ Waiting for ${PENDING_COUNT} task(s) to start${NC}"
    exit 0
else
    echo -e "${RED}✗ Deployment Status: UNSTABLE${NC}"
    echo -e "${RED}✗ Running: ${RUNNING_COUNT}, Desired: ${DESIRED_COUNT}${NC}"
    exit 1
fi
