#!/bin/bash

# Secrets Backup and Recovery Script for Task Activity Management System
# This script provides backup, restore, and disaster recovery capabilities for Docker secrets

set -e

# Configuration
STACK_NAME="${STACK_NAME:-taskactivity}"
BACKUP_BASE_DIR="${HOME}/.taskactivity/secrets-backup"
BACKUP_DIR="${BACKUP_BASE_DIR}/$(date +%Y%m%d-%H%M%S)"
ENCRYPTION_KEY_FILE="${HOME}/.taskactivity/backup-key"

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
║              Secrets Backup & Recovery                       ║
║        Task Activity Management System                        ║
╚═══════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Show usage information
show_usage() {
    cat << EOF
Usage: $0 <command> [options]

Commands:
  backup                    Create encrypted backup of all secrets
  restore <backup_dir>      Restore secrets from backup directory
  list                      List available backups
  verify <backup_dir>       Verify backup integrity
  cleanup                   Remove old backups (keeps last 10)
  disaster-recovery         Complete disaster recovery procedure

Options:
  --stack-name=NAME         Stack name (default: taskactivity)
  --backup-dir=PATH         Backup base directory
  --encryption-key=PATH     Path to encryption key file
  --help                    Show this help message

Examples:
  $0 backup
  $0 restore ~/.taskactivity/secrets-backup/20241228-143022
  $0 disaster-recovery --stack-name=taskactivity
EOF
}

# Generate or load encryption key
setup_encryption() {
    if [[ ! -f "$ENCRYPTION_KEY_FILE" ]]; then
        log "Generating new encryption key..."
        mkdir -p "$(dirname "$ENCRYPTION_KEY_FILE")"
        openssl rand -base64 32 > "$ENCRYPTION_KEY_FILE"
        chmod 600 "$ENCRYPTION_KEY_FILE"
        success "Encryption key created: $ENCRYPTION_KEY_FILE"
        warning "IMPORTANT: Store this key securely - it's required for backup recovery!"
    else
        log "Using existing encryption key: $ENCRYPTION_KEY_FILE"
    fi
}

# Encrypt data using the encryption key
encrypt_data() {
    local input_file=$1
    local output_file=$2
    
    openssl enc -aes-256-cbc -salt -in "$input_file" -out "$output_file" -pass file:"$ENCRYPTION_KEY_FILE"
}

# Decrypt data using the encryption key
decrypt_data() {
    local input_file=$1
    local output_file=$2
    
    openssl enc -aes-256-cbc -d -salt -in "$input_file" -out "$output_file" -pass file:"$ENCRYPTION_KEY_FILE"
}

# Create comprehensive backup
create_backup() {
    log "Creating comprehensive secrets backup..."
    
    # Check Docker Swarm
    if ! docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
        error "Docker Swarm is not active. Cannot backup secrets."
        exit 1
    fi
    
    # Create backup directory
    mkdir -p "$BACKUP_DIR"
    
    setup_encryption
    
    # Create backup manifest
    local manifest_file="$BACKUP_DIR/backup-manifest.json"
    cat > "$manifest_file" << EOF
{
  "backup_date": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "stack_name": "$STACK_NAME",
  "docker_version": "$(docker version --format '{{.Server.Version}}')",
  "swarm_node_id": "$(docker info --format '{{.Swarm.NodeID}}')",
  "hostname": "$(hostname)",
  "operator": "$(whoami)",
  "secrets": []
}
EOF

    # Backup all stack-related secrets
    local secrets_found=false
    
    for secret_name in $(docker secret ls --format "{{.Name}}" | grep "^${STACK_NAME}_"); do
        secrets_found=true
        log "Backing up secret: $secret_name"
        
        # Get secret metadata
        docker secret inspect "$secret_name" > "$BACKUP_DIR/${secret_name}-metadata.json"
        
        # Note: We cannot backup the actual secret data from Docker Swarm
        # This is a security feature. We can only backup metadata and structure.
        
        # Add to manifest
        jq --arg name "$secret_name" '.secrets += [$name]' "$manifest_file" > "$manifest_file.tmp" && mv "$manifest_file.tmp" "$manifest_file"
        
        success "Backed up metadata for: $secret_name"
    done
    
    if [[ "$secrets_found" == false ]]; then
        warning "No secrets found for stack: $STACK_NAME"
    fi
    
    # Backup stack configuration
    if docker stack ls | grep -q "$STACK_NAME"; then
        log "Backing up stack configuration..."
        docker stack config "$STACK_NAME" > "$BACKUP_DIR/stack-config.yml" 2>/dev/null || {
            warning "Could not export stack config - saving services info instead"
            docker stack services "$STACK_NAME" --format "table {{.Name}}\t{{.Mode}}\t{{.Replicas}}\t{{.Image}}" > "$BACKUP_DIR/stack-services.txt"
        }
    fi
    
    # Backup network configuration
    if docker network ls | grep -q "${STACK_NAME}-network"; then
        docker network inspect "${STACK_NAME}-network" > "$BACKUP_DIR/network-config.json"
        success "Backed up network configuration"
    fi
    
    # Create encrypted archive
    log "Creating encrypted backup archive..."
    cd "$(dirname "$BACKUP_DIR")"
    tar -czf "$(basename "$BACKUP_DIR").tar.gz" "$(basename "$BACKUP_DIR")"
    encrypt_data "$(basename "$BACKUP_DIR").tar.gz" "$(basename "$BACKUP_DIR").tar.gz.enc"
    rm -f "$(basename "$BACKUP_DIR").tar.gz"
    
    # Create checksum
    sha256sum "$(basename "$BACKUP_DIR").tar.gz.enc" > "$(basename "$BACKUP_DIR").sha256"
    
    success "Backup completed: $BACKUP_DIR"
    echo ""
    echo -e "${BOLD}Backup Summary:${NC}"
    echo "• Location: $BACKUP_DIR"
    echo "• Encrypted archive: $(basename "$BACKUP_DIR").tar.gz.enc"
    echo "• Checksum: $(basename "$BACKUP_DIR").sha256"
    echo "• Encryption key: $ENCRYPTION_KEY_FILE"
    echo ""
    warning "IMPORTANT: Secret values cannot be backed up from Docker Swarm for security reasons."
    warning "This backup contains metadata and configuration only."
    warning "For disaster recovery, you will need to re-enter secret values."
}

# List available backups
list_backups() {
    log "Available backups:"
    echo ""
    
    if [[ ! -d "$BACKUP_BASE_DIR" ]]; then
        warning "No backup directory found: $BACKUP_BASE_DIR"
        return
    fi
    
    local count=0
    for backup_dir in "$BACKUP_BASE_DIR"/*/; do
        if [[ -d "$backup_dir" ]]; then
            local backup_name=$(basename "$backup_dir")
            local manifest_file="$backup_dir/backup-manifest.json"
            
            if [[ -f "$manifest_file" ]]; then
                local backup_date=$(jq -r '.backup_date' "$manifest_file" 2>/dev/null || echo "Unknown")
                local stack_name=$(jq -r '.stack_name' "$manifest_file" 2>/dev/null || echo "Unknown")
                local secret_count=$(jq -r '.secrets | length' "$manifest_file" 2>/dev/null || echo "0")
                
                echo -e "${GREEN}$backup_name${NC}"
                echo "  Date: $backup_date"
                echo "  Stack: $stack_name"
                echo "  Secrets: $secret_count"
                echo ""
                ((count++))
            fi
        fi
    done
    
    if [[ $count -eq 0 ]]; then
        warning "No valid backups found"
    else
        success "Found $count backup(s)"
    fi
}

# Verify backup integrity
verify_backup() {
    local backup_path=$1
    
    if [[ -z "$backup_path" ]]; then
        error "Backup path required for verification"
        return 1
    fi
    
    if [[ ! -d "$backup_path" ]]; then
        error "Backup directory not found: $backup_path"
        return 1
    fi
    
    log "Verifying backup integrity: $backup_path"
    
    # Check manifest
    local manifest_file="$backup_path/backup-manifest.json"
    if [[ ! -f "$manifest_file" ]]; then
        error "Backup manifest not found"
        return 1
    fi
    
    # Validate JSON
    if ! jq empty "$manifest_file" 2>/dev/null; then
        error "Invalid backup manifest JSON"
        return 1
    fi
    
    # Check encrypted archive if it exists
    local backup_name=$(basename "$backup_path")
    local encrypted_file="${backup_path}/../${backup_name}.tar.gz.enc"
    local checksum_file="${backup_path}/../${backup_name}.sha256"
    
    if [[ -f "$encrypted_file" && -f "$checksum_file" ]]; then
        log "Verifying encrypted archive checksum..."
        cd "$(dirname "$encrypted_file")"
        if sha256sum -c "$(basename "$checksum_file")" >/dev/null 2>&1; then
            success "Checksum verification passed"
        else
            error "Checksum verification failed"
            return 1
        fi
    fi
    
    # Check secret metadata files
    local secrets=$(jq -r '.secrets[]' "$manifest_file" 2>/dev/null)
    for secret_name in $secrets; do
        local metadata_file="$backup_path/${secret_name}-metadata.json"
        if [[ ! -f "$metadata_file" ]]; then
            error "Missing metadata file: $metadata_file"
            return 1
        fi
        
        if ! jq empty "$metadata_file" 2>/dev/null; then
            error "Invalid metadata JSON: $metadata_file"
            return 1
        fi
    done
    
    success "Backup verification completed successfully"
    return 0
}

# Restore secrets from backup
restore_backup() {
    local backup_path=$1
    
    if [[ -z "$backup_path" ]]; then
        error "Backup path required for restore"
        return 1
    fi
    
    if ! verify_backup "$backup_path"; then
        error "Backup verification failed - cannot restore"
        return 1
    fi
    
    warning "This will restore secrets and may affect running services."
    read -p "Continue with restore? (y/n): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log "Restore cancelled by user"
        return 0
    fi
    
    log "Restoring secrets from backup: $backup_path"
    
    # Check Docker Swarm
    if ! docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
        error "Docker Swarm is not active. Cannot restore secrets."
        exit 1
    fi
    
    setup_encryption
    
    local manifest_file="$backup_path/backup-manifest.json"
    local secrets=$(jq -r '.secrets[]' "$manifest_file" 2>/dev/null)
    
    for secret_name in $secrets; do
        log "Restoring secret: $secret_name"
        
        # Check if secret already exists
        if docker secret ls --format "{{.Name}}" | grep -q "^${secret_name}$"; then
            warning "Secret '$secret_name' already exists. Skipping..."
            continue
        fi
        
        # We cannot restore the actual secret value, so we need to prompt for it
        echo -n "Enter value for secret '$secret_name': "
        read -s secret_value
        echo
        
        # Create the secret
        echo "$secret_value" | docker secret create "$secret_name" -
        success "Restored secret: $secret_name"
    done
    
    success "Secret restore completed"
    warning "Remember to restart services to pick up restored secrets"
}

# Cleanup old backups
cleanup_backups() {
    local keep_count=${1:-10}
    
    log "Cleaning up old backups (keeping last $keep_count)..."
    
    if [[ ! -d "$BACKUP_BASE_DIR" ]]; then
        warning "No backup directory found: $BACKUP_BASE_DIR"
        return
    fi
    
    # Get sorted list of backup directories (newest first)
    local backup_dirs=($(ls -dt "$BACKUP_BASE_DIR"/*/ 2>/dev/null | head -n 100))
    local total_backups=${#backup_dirs[@]}
    
    if [[ $total_backups -le $keep_count ]]; then
        success "No cleanup needed. Found $total_backups backups (keeping $keep_count)"
        return
    fi
    
    local to_remove=$((total_backups - keep_count))
    log "Removing $to_remove old backup(s)..."
    
    for ((i=$keep_count; i<$total_backups; i++)); do
        local backup_dir="${backup_dirs[$i]}"
        local backup_name=$(basename "$backup_dir")
        
        log "Removing old backup: $backup_name"
        rm -rf "$backup_dir"
        
        # Remove associated encrypted files
        local encrypted_file="${BACKUP_BASE_DIR}/${backup_name}.tar.gz.enc"
        local checksum_file="${BACKUP_BASE_DIR}/${backup_name}.sha256"
        
        [[ -f "$encrypted_file" ]] && rm -f "$encrypted_file"
        [[ -f "$checksum_file" ]] && rm -f "$checksum_file"
        
        success "Removed: $backup_name"
    done
    
    success "Cleanup completed. Kept $keep_count most recent backups"
}

# Disaster recovery procedure
disaster_recovery() {
    echo -e "${BOLD}${RED}🚨 DISASTER RECOVERY PROCEDURE 🚨${NC}"
    echo ""
    warning "This procedure will help recover from complete system failure"
    echo ""
    
    # Check if backup exists
    if [[ ! -d "$BACKUP_BASE_DIR" ]] || [[ -z "$(ls -A "$BACKUP_BASE_DIR" 2>/dev/null)" ]]; then
        error "No backups found for disaster recovery"
        echo ""
        echo -e "${BOLD}Manual Recovery Steps:${NC}"
        echo "1. Ensure Docker Swarm is initialized"
        echo "2. Create required networks"
        echo "3. Manually recreate secrets using original values"
        echo "4. Deploy stack using docker-compose files"
        return 1
    fi
    
    # List available backups
    echo -e "${BOLD}Available backups for recovery:${NC}"
    list_backups
    
    echo ""
    read -p "Enter backup directory name for recovery: " backup_name
    
    local backup_path="$BACKUP_BASE_DIR/$backup_name"
    
    if [[ ! -d "$backup_path" ]]; then
        error "Backup not found: $backup_path"
        return 1
    fi
    
    # Verify Docker Swarm
    if ! docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
        warning "Docker Swarm is not active. Initializing..."
        docker swarm init
        success "Docker Swarm initialized"
    fi
    
    # Recreate network if needed
    if ! docker network ls | grep -q "${STACK_NAME}-network"; then
        log "Recreating network: ${STACK_NAME}-network"
        docker network create \
            --driver overlay \
            --encrypted \
            --attachable \
            "${STACK_NAME}-network"
        success "Network recreated"
    fi
    
    # Restore secrets
    restore_backup "$backup_path"
    
    # Restore stack if config exists
    local stack_config="$backup_path/stack-config.yml"
    if [[ -f "$stack_config" ]]; then
        log "Restoring stack configuration..."
        docker stack deploy --compose-file "$stack_config" "$STACK_NAME"
        success "Stack restored"
    else
        warning "No stack configuration found in backup"
        echo "You will need to manually deploy the stack using docker-compose files"
    fi
    
    echo ""
    success "🔧 Disaster recovery procedure completed!"
    echo ""
    echo -e "${BOLD}Next Steps:${NC}"
    echo "• Verify all services are running: docker stack services $STACK_NAME"
    echo "• Check application health: curl http://localhost/actuator/health"
    echo "• Verify data integrity and functionality"
    echo "• Consider creating a fresh backup after recovery"
}

# Main function
main() {
    local command=$1
    shift
    
    case $command in
        backup)
            create_backup
            ;;
        restore)
            restore_backup "$1"
            ;;
        list)
            list_backups
            ;;
        verify)
            verify_backup "$1"
            ;;
        cleanup)
            cleanup_backups "$1"
            ;;
        disaster-recovery)
            disaster_recovery
            ;;
        *)
            show_usage
            exit 1
            ;;
    esac
}

# Parse command line arguments
COMMAND=""
while [[ $# -gt 0 ]]; do
    case $1 in
        backup|restore|list|verify|cleanup|disaster-recovery)
            COMMAND=$1
            shift
            break
            ;;
        --stack-name=*)
            STACK_NAME="${1#*=}"
            shift
            ;;
        --backup-dir=*)
            BACKUP_BASE_DIR="${1#*=}"
            shift
            ;;
        --encryption-key=*)
            ENCRYPTION_KEY_FILE="${1#*=}"
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

if [[ -z "$COMMAND" ]]; then
    show_usage
    exit 1
fi

# Handle interrupts gracefully
trap 'error "Operation interrupted"; exit 1' INT TERM

# Run the command with remaining arguments
main "$COMMAND" "$@"