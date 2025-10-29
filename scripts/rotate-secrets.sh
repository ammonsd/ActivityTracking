#!/bin/bash

# Secrets Rotation Script for Task Activity Management System
# This script safely rotates Docker secrets with zero-downtime deployment

set -e

# Configuration
STACK_NAME="${STACK_NAME:-taskactivity}"
BACKUP_DIR="${HOME}/.taskactivity/secrets-backup"
DATE=$(date +%Y%m%d-%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

log() { echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"; }
success() { echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✓ $1${NC}"; }
warning() { echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠ $1${NC}"; }
error() { echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ✗ $1${NC}"; }

echo -e "${BOLD}${GREEN}"
cat << "EOF"
╔═══════════════════════════════════════════════════════════════╗
║              Secrets Rotation Manager                         ║
║        Task Activity Management System                        ║
╚═══════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Pre-flight checks
check_prerequisites() {
    log "Running pre-flight checks..."
    
    # Check Docker Swarm
    if ! docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
        error "Docker Swarm is not active. Secrets rotation requires Swarm mode."
        exit 1
    fi
    
    # Check if stack exists
    if ! docker stack ls | grep -q "$STACK_NAME"; then
        error "Stack '$STACK_NAME' not found. Please deploy the stack first."
        exit 1
    fi
    
    # Create backup directory
    mkdir -p "$BACKUP_DIR"
    
    success "Pre-flight checks passed"
}

# Function to backup current secret metadata
backup_secret_metadata() {
    local secret_name=$1
    
    log "Backing up metadata for secret: $secret_name"
    
    if docker secret ls --format "{{.Name}}" | grep -q "^${secret_name}$"; then
        docker secret inspect "$secret_name" > "$BACKUP_DIR/${secret_name}-${DATE}.json"
        success "Metadata backed up: $BACKUP_DIR/${secret_name}-${DATE}.json"
    else
        warning "Secret '$secret_name' does not exist, skipping backup"
    fi
}

# Function to create new secret with validation
create_new_secret() {
    local old_secret_name=$1
    local new_secret_name=$2
    local prompt_text=$3
    local min_length=${4:-8}
    
    log "Creating new secret: $new_secret_name"
    
    while true; do
        echo -n "${prompt_text}: "
        read -s secret_value
        echo
        
        if [[ ${#secret_value} -lt $min_length ]]; then
            error "Value must be at least ${min_length} characters long"
            continue
        fi
        
        if [[ "$new_secret_name" == *"password"* ]]; then
            # Validate password strength
            if [[ ! "$secret_value" =~ [A-Z] ]]; then
                error "Password must contain at least one uppercase letter"
                continue
            fi
            if [[ ! "$secret_value" =~ [0-9] ]]; then
                error "Password must contain at least one number"
                continue
            fi
            if [[ ! "$secret_value" =~ [^a-zA-Z0-9] ]]; then
                error "Password must contain at least one special character"
                continue
            fi
        fi
        
        # Confirm password
        echo -n "Confirm ${prompt_text}: "
        read -s secret_confirm
        echo
        
        if [[ "$secret_value" != "$secret_confirm" ]]; then
            error "Values do not match. Please try again."
            continue
        fi
        
        break
    done
    
    echo "$secret_value" | docker secret create "$new_secret_name" -
    success "Created new secret: $new_secret_name"
}

# Function to update stack with new secrets
update_stack_secrets() {
    local old_username_secret=$1
    local new_username_secret=$2
    local old_password_secret=$3
    local new_password_secret=$4
    
    log "Updating stack configuration with new secrets..."
    
    # Create temporary docker-compose file with new secrets
    local temp_compose="/tmp/docker-compose-rotation-${DATE}.yml"
    
    cat > "$temp_compose" << EOF
version: '3.8'

services:
  app-production:
    profiles: ["production"]
    image: ${STACK_NAME}:latest
    ports:
      - "80:8080"
      - "443:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_USERNAME_FILE=/run/secrets/db_username
      - DB_PASSWORD_FILE=/run/secrets/db_password
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-production:5432/AmmoP1DB
      - JAVA_OPTS=-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
    secrets:
      - source: $new_username_secret
        target: db_username
      - source: $new_password_secret
        target: db_password
    deploy:
      replicas: 2
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
        window: 120s
      update_config:
        parallelism: 1
        delay: 10s
        failure_action: rollback
        order: start-first
      rollback_config:
        parallelism: 1
        delay: 5s
        failure_action: pause
        order: stop-first
      resources:
        limits:
          cpus: '1.0'
          memory: 1024M
        reservations:
          cpus: '0.5'
          memory: 512M
      placement:
        constraints:
          - node.role == worker
    networks:
      - ${STACK_NAME}-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  postgres-production:
    profiles: ["production"]
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=AmmoP1DB
      - POSTGRES_USER_FILE=/run/secrets/db_username
      - POSTGRES_PASSWORD_FILE=/run/secrets/db_password
    secrets:
      - source: $new_username_secret
        target: db_username
      - source: $new_password_secret
        target: db_password
    volumes:
      - postgres_production_data:/var/lib/postgresql/data
    deploy:
      replicas: 1
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          cpus: '0.25'
          memory: 256M
      placement:
        constraints:
          - node.role == manager
    networks:
      - ${STACK_NAME}-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U \\\$\\\$POSTGRES_USER -d \\\$\\\$POSTGRES_DB"]
      interval: 30s
      timeout: 10s
      retries: 3

networks:
  ${STACK_NAME}-network:
    external: true

volumes:
  postgres_production_data:
    driver: local

secrets:
  $new_username_secret:
    external: true
  $new_password_secret:
    external: true
EOF

    # Update the stack
    docker stack deploy \
        --compose-file "$temp_compose" \
        --with-registry-auth \
        "$STACK_NAME"
    
    success "Stack updated with new secrets"
    
    # Clean up temporary file
    rm -f "$temp_compose"
}

# Function to wait for services to be healthy
wait_for_services() {
    log "Waiting for services to be healthy after rotation..."
    
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        log "Health check attempt $attempt/$max_attempts"
        
        # Check application health
        if curl -f -s http://localhost/actuator/health > /dev/null 2>&1; then
            success "Application is healthy"
            break
        fi
        
        if [[ $attempt -eq $max_attempts ]]; then
            error "Services failed to become healthy after rotation"
            return 1
        fi
        
        sleep 10
        ((attempt++))
    done
    
    # Additional verification - check service status
    log "Verifying service status..."
    docker stack services "$STACK_NAME"
    
    success "All services are healthy"
}

# Function to cleanup old secrets
cleanup_old_secrets() {
    local old_username_secret=$1
    local old_password_secret=$2
    
    log "Scheduling cleanup of old secrets..."
    
    # Wait a bit more to ensure all containers have picked up new secrets
    warning "Waiting 60 seconds before cleaning up old secrets..."
    sleep 60
    
    # Remove old secrets
    if docker secret ls --format "{{.Name}}" | grep -q "^${old_username_secret}$"; then
        docker secret rm "$old_username_secret"
        success "Removed old secret: $old_username_secret"
    fi
    
    if docker secret ls --format "{{.Name}}" | grep -q "^${old_password_secret}$"; then
        docker secret rm "$old_password_secret"
        success "Removed old secret: $old_password_secret"
    fi
}

# Function to rotate database credentials
rotate_db_credentials() {
    log "Starting database credentials rotation..."
    
    local old_username_secret="${STACK_NAME}_db_username"
    local old_password_secret="${STACK_NAME}_db_password"
    local new_username_secret="${STACK_NAME}_db_username_${DATE}"
    local new_password_secret="${STACK_NAME}_db_password_${DATE}"
    
    # Backup current secret metadata
    backup_secret_metadata "$old_username_secret"
    backup_secret_metadata "$old_password_secret"
    
    # Create new secrets
    create_new_secret "$old_username_secret" "$new_username_secret" "Enter new database username" 3
    create_new_secret "$old_password_secret" "$new_password_secret" "Enter new database password (min 12 chars)" 12
    
    # Update stack with new secrets
    update_stack_secrets "$old_username_secret" "$new_username_secret" "$old_password_secret" "$new_password_secret"
    
    # Wait for services to be healthy
    if wait_for_services; then
        # Cleanup old secrets
        cleanup_old_secrets "$old_username_secret" "$old_password_secret"
        
        # Update the main secret names (create aliases)
        echo "Creating new primary secret references..."
        docker secret inspect "$new_username_secret" | jq -r '.[0].Spec.Data' | base64 -d | docker secret create "${STACK_NAME}_db_username" -
        docker secret inspect "$new_password_secret" | jq -r '.[0].Spec.Data' | base64 -d | docker secret create "${STACK_NAME}_db_password" -
        
        # Clean up temporary secrets
        docker secret rm "$new_username_secret" "$new_password_secret"
        
        success "Database credentials rotation completed successfully"
    else
        error "Rotation failed - services are not healthy"
        warning "Consider rolling back using: docker stack rollback $STACK_NAME"
        return 1
    fi
}

# Function to create rotation report
create_rotation_report() {
    local report_file="$BACKUP_DIR/rotation-report-${DATE}.txt"
    
    cat > "$report_file" << EOF
Secrets Rotation Report
======================
Date: $(date)
Stack: $STACK_NAME
Operator: $(whoami)
Host: $(hostname)

Rotated Secrets:
- ${STACK_NAME}_db_username
- ${STACK_NAME}_db_password

Backup Location: $BACKUP_DIR

Post-Rotation Status:
$(docker stack services "$STACK_NAME" 2>/dev/null || echo "Error retrieving service status")

EOF

    success "Rotation report created: $report_file"
}

# Main rotation function
main() {
    echo -e "${BOLD}Starting secrets rotation for stack: $STACK_NAME${NC}"
    echo ""
    
    warning "This will rotate database credentials and restart services."
    warning "Ensure you have database access to update user credentials if needed."
    echo ""
    read -p "Continue with rotation? (y/n): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log "Rotation cancelled by user"
        exit 0
    fi
    
    check_prerequisites
    rotate_db_credentials
    create_rotation_report
    
    echo ""
    success "🔄 Secrets rotation completed successfully!"
    echo ""
    echo -e "${BOLD}Important Notes:${NC}"
    echo "• Update any external database user passwords to match new credentials"
    echo "• Verify application functionality after rotation"
    echo "• Keep rotation report for audit purposes: $BACKUP_DIR/rotation-report-${DATE}.txt"
    echo "• Next rotation recommended in 90 days"
}

# Handle interrupts gracefully
trap 'error "Rotation interrupted - stack may be in inconsistent state"; exit 1' INT TERM

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --stack-name=*)
            STACK_NAME="${1#*=}"
            shift
            ;;
        --backup-dir=*)
            BACKUP_DIR="${1#*=}"
            shift
            ;;
        --help)
            echo "Usage: $0 [--stack-name=taskactivity] [--backup-dir=\$HOME/.taskactivity/secrets-backup]"
            echo ""
            echo "This script rotates Docker secrets for the Task Activity Management System"
            echo "with zero-downtime deployment using rolling updates."
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Run main function
main