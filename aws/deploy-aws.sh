#!/bin/bash
###############################################################################
# AWS Deployment Script for Task Activity Application
# 
# This script automates the deployment of the Task Activity application to AWS
# using Amazon ECS Fargate with Application Load Balancer.
#
# Prerequisites:
# - AWS CLI installed and configured with IAM user credentials (aws configure)
#   Note: Use an IAM user with TaskActivityDeveloperPolicy, NOT root account
#   See ../localdocs/IAM_USER_SETUP.md for setup instructions
# - Docker installed and running
# - Appropriate AWS IAM permissions (see taskactivity-developer-policy.json)
# - ECR repository created
# - RDS database created and initialized
# - Secrets stored in AWS Secrets Manager
#
# Usage:
#   ./deploy-aws.sh [environment]
#   
# Examples:
#   ./deploy-aws.sh dev
#   ./deploy-aws.sh production
#
# Author: Dean Ammons
# Date: October 8, 2025
###############################################################################

set -e  # Exit on error

# ========================================
# Configuration Variables
# ========================================

# Default environment
ENVIRONMENT=${1:-dev}

# AWS Configuration
AWS_REGION="us-east-1"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Application Configuration
APP_NAME="taskactivity"
ECR_REPOSITORY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${APP_NAME}"
ECS_CLUSTER="${APP_NAME}-cluster"
ECS_SERVICE="${APP_NAME}-service"
TASK_FAMILY="${APP_NAME}"

# Build version (timestamp-based)
BUILD_VERSION=$(date +%Y%m%d-%H%M%S)
IMAGE_TAG="${BUILD_VERSION}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ========================================
# Helper Functions
# ========================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install it first."
        exit 1
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        log_error "AWS credentials not configured. Run 'aws configure' first."
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# ========================================
# Build and Push Docker Image
# ========================================

build_and_push_image() {
    log_info "Building Docker image..."
    
    # Build the application
    log_info "Building Spring Boot application with Maven..."
    ./mvnw.cmd clean package -DskipTests
    
    # Build Docker image
    log_info "Building Docker image: ${APP_NAME}:${IMAGE_TAG}"
    docker build -t ${APP_NAME}:${IMAGE_TAG} -t ${APP_NAME}:latest .
    
    log_success "Docker image built successfully"
    
    # Login to ECR
    log_info "Logging into Amazon ECR..."
    aws ecr get-login-password --region ${AWS_REGION} | \
        docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
    
    # Tag images for ECR
    log_info "Tagging images for ECR..."
    docker tag ${APP_NAME}:${IMAGE_TAG} ${ECR_REPOSITORY}:${IMAGE_TAG}
    docker tag ${APP_NAME}:latest ${ECR_REPOSITORY}:latest
    
    # Push to ECR
    log_info "Pushing images to Amazon ECR..."
    docker push ${ECR_REPOSITORY}:${IMAGE_TAG}
    docker push ${ECR_REPOSITORY}:latest
    
    log_success "Images pushed to ECR successfully"
    log_info "Image: ${ECR_REPOSITORY}:${IMAGE_TAG}"
}

# ========================================
# Update ECS Task Definition
# ========================================

update_task_definition() {
    log_info "Updating ECS task definition..."
    
    # Read the task definition template
    TASK_DEF_FILE="aws/taskactivity-task-definition.json"
    
    if [ ! -f "$TASK_DEF_FILE" ]; then
        log_error "Task definition file not found: $TASK_DEF_FILE"
        exit 1
    fi
    
    # Replace placeholders in task definition
    TEMP_TASK_DEF=$(mktemp)
    sed -e "s|ACCOUNT_ID|${AWS_ACCOUNT_ID}|g" \
        -e "s|REGION|${AWS_REGION}|g" \
        -e "s|taskactivity:latest|taskactivity:${IMAGE_TAG}|g" \
        "$TASK_DEF_FILE" > "$TEMP_TASK_DEF"
    
    # Register new task definition
    log_info "Registering new task definition..."
    TASK_REVISION=$(aws ecs register-task-definition \
        --cli-input-json file://${TEMP_TASK_DEF} \
        --query 'taskDefinition.revision' \
        --output text)
    
    rm "$TEMP_TASK_DEF"
    
    log_success "Task definition registered: ${TASK_FAMILY}:${TASK_REVISION}"
}

# ========================================
# Deploy to ECS
# ========================================

deploy_to_ecs() {
    log_info "Deploying to ECS cluster: ${ECS_CLUSTER}"
    
    # Check if service exists
    SERVICE_EXISTS=$(aws ecs describe-services \
        --cluster ${ECS_CLUSTER} \
        --services ${ECS_SERVICE} \
        --query 'services[0].status' \
        --output text 2>/dev/null || echo "NONE")
    
    if [ "$SERVICE_EXISTS" == "ACTIVE" ]; then
        # Update existing service
        log_info "Updating existing ECS service..."
        aws ecs update-service \
            --cluster ${ECS_CLUSTER} \
            --service ${ECS_SERVICE} \
            --task-definition ${TASK_FAMILY}:${TASK_REVISION} \
            --force-new-deployment \
            --output text &> /dev/null
        
        log_success "ECS service update initiated"
    else
        log_warning "Service ${ECS_SERVICE} does not exist or is not ACTIVE"
        log_info "Please create the service manually or use the AWS Console"
        log_info "Task Definition: ${TASK_FAMILY}:${TASK_REVISION}"
        return
    fi
    
    # Wait for deployment to complete
    log_info "Waiting for deployment to complete (this may take several minutes)..."
    aws ecs wait services-stable \
        --cluster ${ECS_CLUSTER} \
        --services ${ECS_SERVICE}
    
    log_success "Deployment completed successfully!"
}

# ========================================
# Get Deployment Status
# ========================================

get_deployment_status() {
    log_info "Fetching deployment status..."
    
    # Get service details
    SERVICE_INFO=$(aws ecs describe-services \
        --cluster ${ECS_CLUSTER} \
        --services ${ECS_SERVICE} \
        --output json)
    
    RUNNING_COUNT=$(echo $SERVICE_INFO | jq -r '.services[0].runningCount')
    DESIRED_COUNT=$(echo $SERVICE_INFO | jq -r '.services[0].desiredCount')
    
    echo ""
    echo "=========================================="
    echo "Deployment Status"
    echo "=========================================="
    echo "Cluster:        ${ECS_CLUSTER}"
    echo "Service:        ${ECS_SERVICE}"
    echo "Task Definition: ${TASK_FAMILY}:${TASK_REVISION}"
    echo "Image:          ${ECR_REPOSITORY}:${IMAGE_TAG}"
    echo "Running Tasks:  ${RUNNING_COUNT}/${DESIRED_COUNT}"
    echo "=========================================="
    echo ""
    
    # Get ALB endpoint
    log_info "Getting Application Load Balancer endpoint..."
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --names ${APP_NAME}-alb \
        --query 'LoadBalancers[0].DNSName' \
        --output text 2>/dev/null || echo "Not configured")
    
    if [ "$ALB_DNS" != "Not configured" ]; then
        log_success "Application URL: https://${ALB_DNS}"
        log_info "Health Check: https://${ALB_DNS}/actuator/health"
    fi
}

# ========================================
# Rollback Function
# ========================================

rollback() {
    log_warning "Initiating rollback to previous task definition..."
    
    PREVIOUS_TASK_DEF=$(aws ecs describe-services \
        --cluster ${ECS_CLUSTER} \
        --services ${ECS_SERVICE} \
        --query 'services[0].deployments[1].taskDefinition' \
        --output text)
    
    if [ -z "$PREVIOUS_TASK_DEF" ] || [ "$PREVIOUS_TASK_DEF" == "None" ]; then
        log_error "No previous task definition found for rollback"
        exit 1
    fi
    
    log_info "Rolling back to: ${PREVIOUS_TASK_DEF}"
    
    aws ecs update-service \
        --cluster ${ECS_CLUSTER} \
        --service ${ECS_SERVICE} \
        --task-definition ${PREVIOUS_TASK_DEF} \
        --force-new-deployment \
        --output text &> /dev/null
    
    log_success "Rollback initiated"
}

# ========================================
# Main Execution
# ========================================

main() {
    echo ""
    echo "=========================================="
    echo "AWS Deployment Script"
    echo "Application: Task Activity Management"
    echo "Environment: ${ENVIRONMENT}"
    echo "AWS Region:  ${AWS_REGION}"
    echo "AWS Account: ${AWS_ACCOUNT_ID}"
    echo "=========================================="
    echo ""
    
    # Check prerequisites
    check_prerequisites
    
    # Build and push Docker image
    build_and_push_image
    
    # Update task definition
    update_task_definition
    
    # Deploy to ECS
    deploy_to_ecs
    
    # Show deployment status
    get_deployment_status
    
    log_success "Deployment process completed!"
    log_info "Monitor your deployment in the AWS Console: https://console.aws.amazon.com/ecs"
}

# Handle script arguments
case "${2:-}" in
    --rollback)
        rollback
        exit 0
        ;;
    --status)
        get_deployment_status
        exit 0
        ;;
esac

# Run main deployment
main
