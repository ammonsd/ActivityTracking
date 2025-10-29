#!/bin/bash

# Production Docker Swarm Setup for Task Activity Management System
# This script sets up a production-ready Docker Swarm environment with secrets

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
STACK_NAME="taskactivity"
ENVIRONMENT="${ENVIRONMENT:-production}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✓ $1${NC}"
}

warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠ $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ✗ $1${NC}"
}

echo -e "${BOLD}${GREEN}"
cat << "EOF"
╔═══════════════════════════════════════════════════════════════╗
║        Task Activity Management System                        ║
║              Production Docker Swarm Setup                    ║
╚═══════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Pre-flight checks
log "Running pre-flight checks..."

# Check if running as root or with sudo (recommended for production)
if [[ $EUID -eq 0 ]]; then
    warning "Running as root. This is acceptable for production setup."
elif sudo -n true 2>/dev/null; then
    log "Sudo access available for system configuration."
else
    warning "No sudo access. Some system optimizations may be skipped."
fi

# Check Docker availability
if ! command -v docker &> /dev/null; then
    error "Docker is not installed. Please install Docker first."
    exit 1
fi

# Check Docker daemon
if ! docker info &> /dev/null; then
    error "Docker daemon is not running. Please start Docker first."
    exit 1
fi

success "Docker is available and running"

# Check available resources
TOTAL_MEMORY=$(docker system info --format '{{.MemTotal}}' 2>/dev/null || echo "0")
if [[ $TOTAL_MEMORY -lt 2147483648 ]]; then  # 2GB in bytes
    warning "Less than 2GB RAM available. Production deployment may be limited."
fi

# Check disk space
AVAILABLE_SPACE=$(df "$PROJECT_ROOT" | tail -1 | awk '{print $4}')
if [[ $AVAILABLE_SPACE -lt 5242880 ]]; then  # 5GB in KB
    warning "Less than 5GB disk space available. Monitor storage usage."
fi

# Check if JAR file exists
if [[ ! -f "$PROJECT_ROOT/target/taskactivity-0.0.1-SNAPSHOT.jar" ]]; then
    error "Application JAR not found. Please run 'mvn clean package' first."
    exit 1
fi

success "Application JAR file found"

# Function to initialize Docker Swarm
init_swarm() {
    log "Initializing Docker Swarm..."
    
    # Check if already in swarm mode
    if docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
        success "Docker Swarm already initialized"
        return 0
    fi
    
    # Get the primary IP address
    PRIMARY_IP=$(ip route get 8.8.8.8 | grep -oP 'src \K\S+' 2>/dev/null || hostname -I | awk '{print $1}')
    
    if [[ -z "$PRIMARY_IP" ]]; then
        log "Could not detect primary IP. Using default swarm init..."
        docker swarm init
    else
        log "Initializing Docker Swarm with advertise address: $PRIMARY_IP"
        docker swarm init --advertise-addr "$PRIMARY_IP"
    fi
    
    success "Docker Swarm initialized successfully"
    
    # Display join tokens for reference
    echo ""
    echo -e "${BOLD}Manager Join Token:${NC}"
    docker swarm join-token manager -q
    echo ""
    echo -e "${BOLD}Worker Join Token:${NC}"
    docker swarm join-token worker -q
    echo ""
}

# Function to create Docker networks
setup_networks() {
    log "Setting up Docker networks..."
    
    # Create overlay network for the application
    if ! docker network ls | grep -q "${STACK_NAME}-network"; then
        docker network create \
            --driver overlay \
            --attachable \
            --opt encrypted \
            "${STACK_NAME}-network"
        success "Created encrypted overlay network: ${STACK_NAME}-network"
    else
        success "Network ${STACK_NAME}-network already exists"
    fi
}

# Function to create secrets securely
create_secrets() {
    log "Setting up production secrets..."
    
    # Function to create a secret with validation
    create_secret_secure() {
        local secret_name=$1
        local prompt_text=$2
        local min_length=${3:-8}
        
        # Check if secret already exists
        if docker secret ls --format "{{.Name}}" | grep -q "^${secret_name}$"; then
            warning "Secret '${secret_name}' already exists"
            read -p "Remove and recreate? (y/n): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                docker secret rm "${secret_name}"
                success "Removed existing secret: ${secret_name}"
            else
                return 0
            fi
        fi
        
        while true; do
            echo -n "${prompt_text}: "
            read -s secret_value
            echo
            
            if [[ ${#secret_value} -lt $min_length ]]; then
                error "Value must be at least ${min_length} characters long"
                continue
            fi
            
            if [[ "$secret_name" == *"password"* ]]; then
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
            
            break
        done
        
        echo "$secret_value" | docker secret create "${secret_name}" -
        success "Created secret: ${secret_name}"
    }
    
    # Create database credentials
    create_secret_secure "${STACK_NAME}_db_username" "Enter database username" 3
    create_secret_secure "${STACK_NAME}_db_password" "Enter database password (min 12 chars, uppercase, number, special char)" 12
    
    echo ""
    success "All secrets created successfully"
}

# Function to build Docker image
build_image() {
    log "Building Docker image..."
    
    cd "$PROJECT_ROOT"
    
    # Build with cache optimization
    docker build \
        --tag "${STACK_NAME}:latest" \
        --tag "${STACK_NAME}:$(date +%Y%m%d-%H%M%S)" \
        --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
        --build-arg VCS_REF="$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')" \
        .
    
    success "Docker image built successfully"
}

# Function to configure system for production
optimize_system() {
    log "Applying production optimizations..."
    
    if command -v sysctl &> /dev/null && [[ $EUID -eq 0 || $(sudo -n true 2>/dev/null; echo $?) -eq 0 ]]; then
        # Increase file descriptor limits
        echo "* soft nofile 65536" | sudo tee -a /etc/security/limits.conf >/dev/null 2>&1 || true
        echo "* hard nofile 65536" | sudo tee -a /etc/security/limits.conf >/dev/null 2>&1 || true
        
        # Configure kernel parameters for better performance
        sudo sysctl -w vm.max_map_count=262144 >/dev/null 2>&1 || true
        sudo sysctl -w net.core.somaxconn=65535 >/dev/null 2>&1 || true
        
        success "System optimizations applied"
    else
        warning "Skipping system optimizations (no sudo access or sysctl unavailable)"
    fi
}

# Function to create production docker-compose override
create_production_override() {
    log "Creating production configuration..."
    
    cat > "$PROJECT_ROOT/docker-compose.prod.yml" << EOF
version: '3.8'

services:
  app-production:
    profiles: ["production"]
    image: ${STACK_NAME}:latest
    ports:
      - "80:8080"
      - "443:8080"  # If using SSL termination at load balancer
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_USERNAME_FILE=/run/secrets/db_username
      - DB_PASSWORD_FILE=/run/secrets/db_password
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-production:5432/AmmoP1DB
      - JAVA_OPTS=-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
    secrets:
      - source: ${STACK_NAME}_db_username
        target: db_username
      - source: ${STACK_NAME}_db_password
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
      - source: ${STACK_NAME}_db_username
        target: db_username
      - source: ${STACK_NAME}_db_password
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
  ${STACK_NAME}_db_username:
    external: true
  ${STACK_NAME}_db_password:
    external: true
EOF

    success "Production configuration created: docker-compose.prod.yml"
}

# Function to deploy the stack
deploy_stack() {
    log "Deploying production stack..."
    
    cd "$PROJECT_ROOT"
    
    # Deploy using docker stack
    docker stack deploy \
        --compose-file docker-compose.yml \
        --compose-file docker-compose.prod.yml \
        --with-registry-auth \
        "$STACK_NAME"
    
    success "Stack deployed successfully"
    
    # Wait for services to be ready
    log "Waiting for services to be ready..."
    sleep 10
    
    # Check service status
    echo ""
    echo -e "${BOLD}Service Status:${NC}"
    docker stack services "$STACK_NAME"
    
    echo ""
    success "Production deployment completed!"
}

# Function to display post-deployment information
show_deployment_info() {
    echo ""
    echo -e "${BOLD}${GREEN}🎉 Production Deployment Successful!${NC}"
    echo ""
    echo -e "${BOLD}Deployment Information:${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "Stack Name: ${BOLD}$STACK_NAME${NC}"
    echo -e "Environment: ${BOLD}$ENVIRONMENT${NC}"
    echo -e "Application URL: ${BOLD}http://$(hostname -I | awk '{print $1}')${NC}"
    echo ""
    echo -e "${BOLD}Useful Commands:${NC}"
    echo "• View services:       docker stack services $STACK_NAME"
    echo "• View service logs:   docker service logs ${STACK_NAME}_app-production"
    echo "• Scale application:   docker service scale ${STACK_NAME}_app-production=3"
    echo "• Update stack:        docker stack deploy -c docker-compose.yml -c docker-compose.prod.yml $STACK_NAME"
    echo "• Remove stack:        docker stack rm $STACK_NAME"
    echo ""
    echo -e "${BOLD}Monitoring:${NC}"
    echo "• Health check:        curl http://localhost/actuator/health"
    echo "• Application logs:    docker service logs -f ${STACK_NAME}_app-production"
    echo "• Database logs:       docker service logs -f ${STACK_NAME}_postgres-production"
    echo ""
    echo -e "${BOLD}Security:${NC}"
    echo "• Secrets are stored encrypted in Docker Swarm"
    echo "• Network traffic is encrypted using overlay network"
    echo "• Regular secret rotation is recommended"
    echo ""
    warning "Remember to:"
    echo "• Set up regular backups for the postgres_production_data volume"
    echo "• Configure SSL/TLS termination at your load balancer"
    echo "• Set up monitoring and alerting"
    echo "• Plan for secret rotation procedures"
}

# Main execution flow
main() {
    init_swarm
    setup_networks
    optimize_system
    create_secrets
    build_image
    create_production_override
    deploy_stack
    show_deployment_info
}

# Handle interrupts gracefully
trap 'error "Script interrupted"; exit 1' INT TERM

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --environment=*)
            ENVIRONMENT="${1#*=}"
            shift
            ;;
        --stack-name=*)
            STACK_NAME="${1#*=}"
            shift
            ;;
        --help)
            echo "Usage: $0 [--environment=production] [--stack-name=taskactivity]"
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

echo ""
echo -e "${GREEN}Production setup completed successfully! 🚀${NC}"