#!/usr/bin/env bash

/**
 * Description: Verify Environment Configuration - verifies that all required AWS resources are properly configured
 *
 * Author: Dean Ammons
 * Date: October 2025
 */

################################################################################
# Verify Environment Configuration
#
# Usage: ./verify-environment.sh <environment>
# Example: ./verify-environment.sh production
#
# This script verifies that all required AWS resources are properly configured
# for the specified environment including:
# - ECS Cluster
# - ECS Service
# - Task Definition
# - ECR Repository
# - Secrets Manager secrets
# - CloudWatch log groups
################################################################################

set -e

# Configuration
AWS_REGION=${AWS_REGION:-us-east-1}

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse arguments
if [ $# -lt 1 ]; then
    echo "Usage: $0 <environment>"
    echo "Example: $0 production"
    exit 1
fi

ENVIRONMENT=$1
ERRORS=0
WARNINGS=0

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Environment Configuration Verification${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Environment: ${GREEN}${ENVIRONMENT}${NC}"
echo -e "Region: ${AWS_REGION}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to check resource
check_resource() {
    local RESOURCE_TYPE=$1
    local RESOURCE_NAME=$2
    local CHECK_COMMAND=$3
    
    echo -n "Checking ${RESOURCE_TYPE}: ${RESOURCE_NAME}... "
    
    if eval "${CHECK_COMMAND}" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${RED}✗${NC}"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

# Check ECS Cluster
echo -e "${BLUE}1. ECS Resources${NC}"
CLUSTER_NAME="taskactivity-cluster-${ENVIRONMENT}"
check_resource "ECS Cluster" "${CLUSTER_NAME}" \
    "aws ecs describe-clusters --clusters ${CLUSTER_NAME} --region ${AWS_REGION} --query 'clusters[0].clusterName' --output text | grep -q ${CLUSTER_NAME}"

# Check ECS Service
SERVICE_NAME="taskactivity-service-${ENVIRONMENT}"
check_resource "ECS Service" "${SERVICE_NAME}" \
    "aws ecs describe-services --cluster ${CLUSTER_NAME} --services ${SERVICE_NAME} --region ${AWS_REGION} --query 'services[0].serviceName' --output text | grep -q ${SERVICE_NAME}"

# Check Task Definition
TASK_DEF_FAMILY="taskactivity-task-${ENVIRONMENT}"
check_resource "Task Definition" "${TASK_DEF_FAMILY}" \
    "aws ecs describe-task-definition --task-definition ${TASK_DEF_FAMILY} --region ${AWS_REGION} --query 'taskDefinition.family' --output text | grep -q ${TASK_DEF_FAMILY}"

echo ""

# Check ECR Repository
echo -e "${BLUE}2. ECR Repository${NC}"
ECR_REPO="taskactivity"
check_resource "ECR Repository" "${ECR_REPO}" \
    "aws ecr describe-repositories --repository-names ${ECR_REPO} --region ${AWS_REGION} --query 'repositories[0].repositoryName' --output text | grep -q ${ECR_REPO}"

echo ""

# Check Secrets Manager
echo -e "${BLUE}3. Secrets Manager${NC}"
check_resource "Database Secret" "taskactivity/${ENVIRONMENT}/database/credentials" \
    "aws secretsmanager describe-secret --secret-id taskactivity/${ENVIRONMENT}/database/credentials --region ${AWS_REGION} 2>&1 | grep -q Name"

check_resource "Admin Secret" "taskactivity/${ENVIRONMENT}/admin/credentials" \
    "aws secretsmanager describe-secret --secret-id taskactivity/${ENVIRONMENT}/admin/credentials --region ${AWS_REGION} 2>&1 | grep -q Name"

if [ "${ENVIRONMENT}" == "production" ]; then
    check_resource "Cloudflare Secret" "taskactivity/${ENVIRONMENT}/cloudflare/tunnel-credentials" \
        "aws secretsmanager describe-secret --secret-id taskactivity/${ENVIRONMENT}/cloudflare/tunnel-credentials --region ${AWS_REGION} 2>&1 | grep -q Name" || WARNINGS=$((WARNINGS + 1))
fi

echo ""

# Check CloudWatch Log Groups
echo -e "${BLUE}4. CloudWatch Logs${NC}"
LOG_GROUP="/ecs/taskactivity-${ENVIRONMENT}"
check_resource "Log Group" "${LOG_GROUP}" \
    "aws logs describe-log-groups --log-group-name-prefix ${LOG_GROUP} --region ${AWS_REGION} --query 'logGroups[0].logGroupName' --output text | grep -q ${LOG_GROUP}"

echo ""

# Check IAM Roles
echo -e "${BLUE}5. IAM Roles${NC}"
check_resource "Task Execution Role" "ecsTaskExecutionRole" \
    "aws iam get-role --role-name ecsTaskExecutionRole 2>&1 | grep -q RoleName" || WARNINGS=$((WARNINGS + 1))

check_resource "Task Role" "ecsTaskRole" \
    "aws iam get-role --role-name ecsTaskRole 2>&1 | grep -q RoleName" || WARNINGS=$((WARNINGS + 1))

echo ""

# Additional checks for production
if [ "${ENVIRONMENT}" == "production" ]; then
    echo -e "${BLUE}6. Production-Specific Checks${NC}"
    
    # Check for ALB
    echo -n "Checking for Load Balancer... "
    ALB_COUNT=$(aws elbv2 describe-load-balancers \
        --region ${AWS_REGION} \
        --query "LoadBalancers[?contains(LoadBalancerName, 'taskactivity')] | length(@)" \
        --output text 2>/dev/null || echo "0")
    
    if [ "${ALB_COUNT}" -gt 0 ]; then
        echo -e "${GREEN}✓ (${ALB_COUNT} found)${NC}"
    else
        echo -e "${YELLOW}⚠ (No ALB found - using direct task access)${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    # Check service scaling
    echo -n "Checking service scaling... "
    DESIRED_COUNT=$(aws ecs describe-services \
        --cluster ${CLUSTER_NAME} \
        --services ${SERVICE_NAME} \
        --region ${AWS_REGION} \
        --query 'services[0].desiredCount' \
        --output text 2>/dev/null || echo "0")
    
    if [ "${DESIRED_COUNT}" -ge 2 ]; then
        echo -e "${GREEN}✓ (${DESIRED_COUNT} tasks)${NC}"
    else
        echo -e "${YELLOW}⚠ (Only ${DESIRED_COUNT} task - consider scaling up)${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    echo ""
fi

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Verification Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ ${ERRORS} -eq 0 ] && [ ${WARNINGS} -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo -e "${GREEN}✓ Environment ${ENVIRONMENT} is properly configured${NC}"
    exit 0
elif [ ${ERRORS} -eq 0 ]; then
    echo -e "${YELLOW}⚠ All critical checks passed${NC}"
    echo -e "${YELLOW}⚠ ${WARNINGS} warning(s) found${NC}"
    echo ""
    echo "Review warnings above. Environment is functional but may need attention."
    exit 0
else
    echo -e "${RED}✗ ${ERRORS} error(s) found${NC}"
    if [ ${WARNINGS} -gt 0 ]; then
        echo -e "${YELLOW}⚠ ${WARNINGS} warning(s) found${NC}"
    fi
    echo ""
    echo "Environment ${ENVIRONMENT} is NOT properly configured."
    echo "Fix the errors above before deploying."
    exit 1
fi
