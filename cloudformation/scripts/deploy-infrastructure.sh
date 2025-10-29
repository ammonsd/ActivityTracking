#!/bin/bash
###############################################################################
# CloudFormation Infrastructure Deployment Script for Task Activity Application
#
# This script manages CloudFormation stacks for the Task Activity application.
# It handles stack creation, updates, deletion, and validation.
#
# Prerequisites:
# - AWS CLI installed and configured
# - Appropriate AWS IAM permissions for CloudFormation
# - Parameters configured in cloudformation/parameters/*.json
#
# Usage:
#   ./cloudformation/scripts/deploy-infrastructure.sh <environment> <action>
#
# Examples:
#   # Create new infrastructure stack
#   ./cloudformation/scripts/deploy-infrastructure.sh dev create
#
#   # Update existing infrastructure
#   ./cloudformation/scripts/deploy-infrastructure.sh production update
#
#   # Preview changes before applying
#   ./cloudformation/scripts/deploy-infrastructure.sh dev preview
#
#   # Delete infrastructure (careful!)
#   ./cloudformation/scripts/deploy-infrastructure.sh dev delete
#
#   # Check stack status
#   ./cloudformation/scripts/deploy-infrastructure.sh dev status
#
# Author: Dean Ammons
# Date: October 28, 2025
###############################################################################

set -e  # Exit on error

# ========================================
# Configuration Variables
# ========================================

ENVIRONMENT=${1:-}
ACTION=${2:-}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
CF_DIR="$PROJECT_ROOT/cloudformation"
TEMPLATE_FILE="$CF_DIR/templates/infrastructure.yaml"
PARAMETERS_FILE="$CF_DIR/parameters/$ENVIRONMENT.json"

STACK_NAME="taskactivity-$ENVIRONMENT"
AWS_REGION="us-east-1"

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

show_usage() {
    echo "Usage: $0 <environment> <action>"
    echo ""
    echo "Environments:"
    echo "  dev          - Development environment"
    echo "  staging      - Staging environment"
    echo "  production   - Production environment"
    echo ""
    echo "Actions:"
    echo "  create       - Create new CloudFormation stack"
    echo "  update       - Update existing stack"
    echo "  delete       - Delete stack"
    echo "  status       - Show stack status"
    echo "  preview      - Preview changes without applying"
    echo "  validate     - Validate template syntax"
    echo ""
    echo "Examples:"
    echo "  $0 dev create"
    echo "  $0 production update"
    echo "  $0 dev preview"
    echo ""
    exit 1
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        log_error "AWS credentials not configured. Run 'aws configure' first."
        exit 1
    fi
    
    # Check template file exists
    if [ ! -f "$TEMPLATE_FILE" ]; then
        log_error "Template file not found: $TEMPLATE_FILE"
        exit 1
    fi
    
    # Check parameters file exists
    if [ ! -f "$PARAMETERS_FILE" ]; then
        log_error "Parameters file not found: $PARAMETERS_FILE"
        log_info "Expected location: $PARAMETERS_FILE"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

get_stack_status() {
    aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION" \
        --query 'Stacks[0].StackStatus' \
        --output text 2>/dev/null || echo ""
}

wait_for_stack_operation() {
    local operation=$1
    log_info "Waiting for stack operation to complete..."
    
    local wait_state=""
    case $operation in
        create) wait_state="stack-create-complete" ;;
        update) wait_state="stack-update-complete" ;;
        delete) wait_state="stack-delete-complete" ;;
    esac
    
    if aws cloudformation wait "$wait_state" \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION"; then
        log_success "Stack operation completed successfully"
        return 0
    else
        log_error "Stack operation failed or timed out"
        show_stack_events 10
        return 1
    fi
}

show_stack_events() {
    local limit=${1:-20}
    log_info "Recent stack events:"
    echo ""
    
    aws cloudformation describe-stack-events \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION" \
        --max-items "$limit" \
        --query 'StackEvents[*].[Timestamp,ResourceStatus,ResourceType,LogicalResourceId,ResourceStatusReason]' \
        --output table 2>/dev/null || log_warning "Unable to retrieve stack events"
}

show_stack_outputs() {
    log_info "Stack outputs:"
    echo ""
    
    aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION" \
        --query 'Stacks[0].Outputs[*].[OutputKey,OutputValue,Description]' \
        --output table 2>/dev/null || log_warning "Unable to retrieve stack outputs"
}

validate_template() {
    log_info "Validating CloudFormation template..."
    
    if aws cloudformation validate-template \
        --template-body "file://$TEMPLATE_FILE" \
        --region "$AWS_REGION" > /dev/null; then
        log_success "Template validation passed"
        return 0
    else
        log_error "Template validation failed"
        return 1
    fi
}

create_stack() {
    log_info "Creating CloudFormation stack: $STACK_NAME"
    
    # Validate template first
    if ! validate_template; then
        exit 1
    fi
    
    # Check if stack already exists
    local status=$(get_stack_status)
    if [ -n "$status" ]; then
        log_error "Stack already exists with status: $status"
        log_info "Use 'update' action to update the stack, or 'delete' to remove it first"
        exit 1
    fi
    
    # Confirm action
    log_warning "This will create a new CloudFormation stack with the following resources:"
    echo "  - VPC with subnets, NAT gateway, and Internet gateway"
    echo "  - RDS PostgreSQL database"
    echo "  - ECS Fargate cluster and service"
    echo "  - Application Load Balancer"
    echo "  - ECR repository"
    echo "  - Secrets Manager secrets"
    echo "  - IAM roles and security groups"
    echo ""
    log_warning "This operation may take 10-15 minutes and will incur AWS costs."
    echo ""
    
    read -p "Do you want to continue? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_info "Operation cancelled"
        exit 0
    fi
    
    log_info "Creating stack..."
    
    aws cloudformation create-stack \
        --stack-name "$STACK_NAME" \
        --template-body "file://$TEMPLATE_FILE" \
        --parameters "file://$PARAMETERS_FILE" \
        --capabilities CAPABILITY_NAMED_IAM \
        --region "$AWS_REGION" \
        --tags Key=Environment,Value="$ENVIRONMENT" Key=ManagedBy,Value=CloudFormation
    
    log_success "Stack creation initiated"
    
    if wait_for_stack_operation create; then
        log_success "Stack created successfully!"
        show_stack_outputs
    else
        exit 1
    fi
}

update_stack() {
    log_info "Updating CloudFormation stack: $STACK_NAME"
    
    # Validate template first
    if ! validate_template; then
        exit 1
    fi
    
    # Check if stack exists
    local status=$(get_stack_status)
    if [ -z "$status" ]; then
        log_error "Stack does not exist. Use 'create' action to create it first."
        exit 1
    fi
    
    if [[ "$status" == *"PROGRESS"* ]]; then
        log_error "Stack is currently in progress: $status"
        log_info "Wait for the current operation to complete before updating"
        exit 1
    fi
    
    log_info "Creating change set..."
    
    local change_set_name="changeset-$(date +%Y%m%d-%H%M%S)"
    
    aws cloudformation create-change-set \
        --stack-name "$STACK_NAME" \
        --change-set-name "$change_set_name" \
        --template-body "file://$TEMPLATE_FILE" \
        --parameters "file://$PARAMETERS_FILE" \
        --capabilities CAPABILITY_NAMED_IAM \
        --region "$AWS_REGION"
    
    log_info "Waiting for change set to be created..."
    sleep 5
    
    # Describe changes
    log_info "Proposed changes:"
    echo ""
    
    aws cloudformation describe-change-set \
        --stack-name "$STACK_NAME" \
        --change-set-name "$change_set_name" \
        --region "$AWS_REGION" \
        --query 'Changes[*].[Type,ResourceChange.Action,ResourceChange.LogicalResourceId,ResourceChange.ResourceType,ResourceChange.Replacement]' \
        --output table
    
    # Confirm execution
    echo ""
    read -p "Execute these changes? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_info "Deleting change set..."
        aws cloudformation delete-change-set \
            --stack-name "$STACK_NAME" \
            --change-set-name "$change_set_name" \
            --region "$AWS_REGION"
        log_info "Operation cancelled"
        exit 0
    fi
    
    log_info "Executing change set..."
    aws cloudformation execute-change-set \
        --stack-name "$STACK_NAME" \
        --change-set-name "$change_set_name" \
        --region "$AWS_REGION"
    
    log_success "Change set execution initiated"
    
    if wait_for_stack_operation update; then
        log_success "Stack updated successfully!"
        show_stack_outputs
    else
        exit 1
    fi
}

delete_stack() {
    log_info "Deleting CloudFormation stack: $STACK_NAME"
    
    # Check if stack exists
    local status=$(get_stack_status)
    if [ -z "$status" ]; then
        log_warning "Stack does not exist"
        exit 0
    fi
    
    # Strong warning for production
    if [ "$ENVIRONMENT" = "production" ]; then
        echo ""
        log_warning "╔═══════════════════════════════════════════════════════════════╗"
        log_warning "║  WARNING: You are about to delete PRODUCTION infrastructure  ║"
        log_warning "╚═══════════════════════════════════════════════════════════════╝"
        echo ""
        echo "This will permanently delete:"
        echo "  - Production database (final snapshot will be created)"
        echo "  - All network infrastructure"
        echo "  - Load balancers and ECS services"
        echo "  - ECR repository (images will be preserved)"
        echo ""
    fi
    
    # Confirm deletion
    log_warning "This will delete ALL infrastructure resources for $ENVIRONMENT environment"
    echo ""
    read -p "Type 'delete $ENVIRONMENT' to confirm: " confirm
    
    if [ "$confirm" != "delete $ENVIRONMENT" ]; then
        log_info "Operation cancelled"
        exit 0
    fi
    
    log_info "Deleting stack..."
    
    aws cloudformation delete-stack \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION"
    
    log_success "Stack deletion initiated"
    
    if wait_for_stack_operation delete; then
        log_success "Stack deleted successfully!"
    else
        exit 1
    fi
}

show_status() {
    log_info "Checking stack status: $STACK_NAME"
    
    local status=$(get_stack_status)
    
    if [ -z "$status" ]; then
        log_warning "Stack does not exist"
        exit 0
    fi
    
    echo ""
    echo -n "Stack Status: "
    
    if [[ "$status" == *"COMPLETE"* && "$status" != *"ROLLBACK"* ]]; then
        echo -e "${GREEN}$status${NC}"
    elif [[ "$status" == *"FAILED"* || "$status" == *"ROLLBACK"* ]]; then
        echo -e "${RED}$status${NC}"
    else
        echo -e "${YELLOW}$status${NC}"
    fi
    
    echo ""
    show_stack_events 10
    echo ""
    
    if [[ "$status" == *"COMPLETE"* && "$status" != *"ROLLBACK"* ]]; then
        show_stack_outputs
    fi
}

preview_changes() {
    log_info "Previewing infrastructure changes for: $STACK_NAME"
    
    # Validate template first
    if ! validate_template; then
        exit 1
    fi
    
    # Check if stack exists
    local status=$(get_stack_status)
    
    if [ -z "$status" ]; then
        log_info "Stack does not exist. This would create a new stack with:"
        echo ""
        echo "Template: $TEMPLATE_FILE"
        echo "Parameters: $PARAMETERS_FILE"
        echo ""
        log_info "Run with 'create' action to create the stack"
        exit 0
    fi
    
    log_info "Creating change set for preview..."
    
    local change_set_name="preview-$(date +%Y%m%d-%H%M%S)"
    
    aws cloudformation create-change-set \
        --stack-name "$STACK_NAME" \
        --change-set-name "$change_set_name" \
        --template-body "file://$TEMPLATE_FILE" \
        --parameters "file://$PARAMETERS_FILE" \
        --capabilities CAPABILITY_NAMED_IAM \
        --region "$AWS_REGION"
    
    log_info "Waiting for change set to be created..."
    sleep 5
    
    log_info "Proposed changes:"
    echo ""
    
    aws cloudformation describe-change-set \
        --stack-name "$STACK_NAME" \
        --change-set-name "$change_set_name" \
        --region "$AWS_REGION" \
        --query 'Changes[*].[Type,ResourceChange.Action,ResourceChange.LogicalResourceId,ResourceChange.ResourceType,ResourceChange.Replacement]' \
        --output table
    
    # Clean up change set
    log_info "Cleaning up preview change set..."
    aws cloudformation delete-change-set \
        --stack-name "$STACK_NAME" \
        --change-set-name "$change_set_name" \
        --region "$AWS_REGION"
    
    log_info "Preview complete. No changes were applied."
}

# ========================================
# Main Execution
# ========================================

# Check arguments
if [ -z "$ENVIRONMENT" ] || [ -z "$ACTION" ]; then
    show_usage
fi

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|production)$ ]]; then
    log_error "Invalid environment: $ENVIRONMENT"
    show_usage
fi

# Validate action
if [[ ! "$ACTION" =~ ^(create|update|delete|status|preview|validate)$ ]]; then
    log_error "Invalid action: $ACTION"
    show_usage
fi

echo "========================================================================"
echo " Task Activity - CloudFormation Infrastructure Deployment"
echo "========================================================================"
echo ""
echo "Environment: $ENVIRONMENT"
echo "Action: $ACTION"
echo "Stack Name: $STACK_NAME"
echo "Region: $AWS_REGION"
echo ""

# Check prerequisites
check_prerequisites

# Execute action
case $ACTION in
    create)
        create_stack
        ;;
    update)
        update_stack
        ;;
    delete)
        delete_stack
        ;;
    status)
        show_status
        ;;
    preview)
        preview_changes
        ;;
    validate)
        if validate_template; then
            log_success "Template is valid"
        else
            exit 1
        fi
        ;;
esac

echo ""
echo "========================================================================"
log_success "Operation completed"
echo "========================================================================"
