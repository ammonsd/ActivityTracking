#!/bin/bash

###############################################################################
# Setup ECR Credential Helper for Jenkins
#
# Description: Installs and configures AWS ECR credential helper to eliminate
#              unencrypted Docker credential storage warnings in Jenkins.
#
# Author: Dean Ammons
# Date: January 2026
#
# Prerequisites:
# - Jenkins server with Docker installed
# - AWS CLI configured with valid credentials
# - Sudo access for package installation
#
# Usage:
#   sudo ./setup-ecr-credential-helper.sh <AWS_ACCOUNT_ID> <AWS_REGION>
#
# Example:
#   sudo ./setup-ecr-credential-helper.sh 378010131175 us-east-1
#
###############################################################################

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check arguments
if [ $# -ne 2 ]; then
    log_error "Usage: $0 <AWS_ACCOUNT_ID> <AWS_REGION>"
    log_info "Example: $0 378010131175 us-east-1"
    exit 1
fi

AWS_ACCOUNT_ID=$1
AWS_REGION=$2
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

log_info "=========================================="
log_info "ECR Credential Helper Setup"
log_info "=========================================="
log_info "AWS Account ID: ${AWS_ACCOUNT_ID}"
log_info "AWS Region: ${AWS_REGION}"
log_info "ECR Registry: ${ECR_REGISTRY}"
log_info "=========================================="

# Step 1: Check if running as root or with sudo
if [ "$EUID" -ne 0 ]; then
    log_error "Please run this script with sudo"
    exit 1
fi

log_success "Running with appropriate privileges"

# Step 2: Detect Jenkins user and home directory
JENKINS_USER="jenkins"
JENKINS_HOME="/var/lib/jenkins"

if id "${JENKINS_USER}" &>/dev/null; then
    log_success "Jenkins user found: ${JENKINS_USER}"
    JENKINS_HOME=$(eval echo ~${JENKINS_USER})
    log_info "Jenkins home: ${JENKINS_HOME}"
else
    log_warning "Jenkins user not found, using current user"
    JENKINS_USER=$(whoami)
    JENKINS_HOME=$HOME
fi

# Step 3: Install Amazon ECR credential helper
log_info "Installing Amazon ECR credential helper..."

if command -v docker-credential-ecr-login &>/dev/null; then
    log_warning "ECR credential helper already installed"
    docker-credential-ecr-login version
else
    # Update package list
    apt-get update -qq
    
    # Install ECR credential helper and AWS CLI
    apt-get install -y amazon-ecr-credential-helper awscli
    
    log_success "ECR credential helper installed"
    docker-credential-ecr-login version
fi

# Step 4: Verify AWS CLI is installed
log_info "Verifying AWS CLI installation..."

if command -v aws &>/dev/null; then
    log_success "AWS CLI is installed: $(aws --version)"
else
    log_error "AWS CLI is not installed. Please install AWS CLI first."
    exit 1
fi

# Step 5: Create Docker config directory
log_info "Creating Docker config directory..."

DOCKER_CONFIG_DIR="${JENKINS_HOME}/.docker"
DOCKER_CONFIG_FILE="${DOCKER_CONFIG_DIR}/config.json"

mkdir -p "${DOCKER_CONFIG_DIR}"
log_success "Docker config directory created: ${DOCKER_CONFIG_DIR}"

# Step 6: Backup existing config if it exists
if [ -f "${DOCKER_CONFIG_FILE}" ]; then
    BACKUP_FILE="${DOCKER_CONFIG_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
    cp "${DOCKER_CONFIG_FILE}" "${BACKUP_FILE}"
    log_warning "Existing config backed up to: ${BACKUP_FILE}"
fi

# Step 7: Create Docker config with ECR credential helper
log_info "Creating Docker config with ECR credential helper..."

cat > "${DOCKER_CONFIG_FILE}" <<EOF
{
  "credHelpers": {
    "${ECR_REGISTRY}": "ecr-login",
    "public.ecr.aws": "ecr-login"
  }
}
EOF

log_success "Docker config created with ECR credential helper"

# Step 8: Set proper permissions
chown -R ${JENKINS_USER}:${JENKINS_USER} "${DOCKER_CONFIG_DIR}"
chmod 700 "${DOCKER_CONFIG_DIR}"
chmod 600 "${DOCKER_CONFIG_FILE}"

log_success "Permissions set correctly"

# Step 9: Display the configuration
log_info "Docker configuration:"
cat "${DOCKER_CONFIG_FILE}"

# Step 10: Test AWS credentials (as Jenkins user)
log_info "Testing AWS credentials..."

if sudo -u ${JENKINS_USER} aws sts get-caller-identity --region ${AWS_REGION} &>/dev/null; then
    IDENTITY=$(sudo -u ${JENKINS_USER} aws sts get-caller-identity --region ${AWS_REGION})
    log_success "AWS credentials are valid:"
    echo "${IDENTITY}" | jq '.'
else
    log_warning "Could not verify AWS credentials for Jenkins user"
    log_info "Make sure AWS credentials are configured in Jenkins"
fi

# Step 11: Test credential helper
log_info "Testing ECR credential helper..."

if echo "https://${ECR_REGISTRY}" | sudo -u ${JENKINS_USER} docker-credential-ecr-login get &>/dev/null; then
    log_success "ECR credential helper is working correctly"
else
    log_error "ECR credential helper test failed"
    log_info "Please ensure AWS credentials are available to Jenkins user"
fi

# Step 12: Summary and next steps
echo ""
log_success "=========================================="
log_success "ECR Credential Helper Setup Complete!"
log_success "=========================================="
echo ""
log_info "Next steps:"
echo "  1. Update your Jenkinsfile to remove 'docker login' commands"
echo "  2. Docker will now authenticate automatically to ECR"
echo "  3. Test with a Jenkins build"
echo "  4. Verify no credential warnings appear"
echo ""
log_info "Configuration file: ${DOCKER_CONFIG_FILE}"
log_info "For more details, see: jenkins/Docker_Credential_Security_Guide.md"
echo ""
log_warning "Important: Restart Jenkins to ensure changes take effect:"
echo "  sudo systemctl restart jenkins"
echo ""

exit 0
