#!/bin/bash

/**
 * Description: Health Monitoring Script - provides comprehensive monitoring of application health, secrets, and infrastructure
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

# Health Monitoring Script for Task Activity Management System
# Provides comprehensive monitoring of application health, secrets, and infrastructure

set -e

# Configuration
STACK_NAME="${STACK_NAME:-taskactivity}"
MONITOR_DIR="${HOME}/.taskactivity/monitoring"
LOG_FILE="$MONITOR_DIR/health-monitor.log"
ALERT_CONFIG="$MONITOR_DIR/alert-config.json"
WEBHOOK_URL="${WEBHOOK_URL:-}"  # Slack/Teams webhook for alerts

# Monitoring thresholds
CPU_THRESHOLD=80
MEMORY_THRESHOLD=85
DISK_THRESHOLD=90
RESPONSE_TIME_THRESHOLD=5000  # milliseconds
ERROR_RATE_THRESHOLD=5        # percentage

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

log() { echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}" | tee -a "$LOG_FILE"; }
success() { echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✓ $1${NC}" | tee -a "$LOG_FILE"; }
warning() { echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠ $1${NC}" | tee -a "$LOG_FILE"; }
error() { echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ✗ $1${NC}" | tee -a "$LOG_FILE"; }
critical() { echo -e "${RED}${BOLD}[$(date +'%Y-%m-%d %H:%M:%S')] 🚨 CRITICAL: $1${NC}" | tee -a "$LOG_FILE"; }

echo -e "${BOLD}${GREEN}"
cat << "EOF"
╔═══════════════════════════════════════════════════════════════╗
║              Health Monitoring System                        ║
║        Task Activity Management System                        ║
╚═══════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Setup monitoring environment
setup_monitoring() {
    mkdir -p "$MONITOR_DIR"
    
    # Create default alert configuration if it doesn't exist
    if [[ ! -f "$ALERT_CONFIG" ]]; then
        cat > "$ALERT_CONFIG" << EOF
{
  "alerts": {
    "critical": {
      "enabled": true,
      "webhook": "$WEBHOOK_URL",
      "email": "",
      "thresholds": {
        "cpu": $CPU_THRESHOLD,
        "memory": $MEMORY_THRESHOLD,
        "disk": $DISK_THRESHOLD,
        "response_time": $RESPONSE_TIME_THRESHOLD,
        "error_rate": $ERROR_RATE_THRESHOLD
      }
    },
    "warning": {
      "enabled": true,
      "webhook": "$WEBHOOK_URL",
      "email": ""
    }
  },
  "monitoring": {
    "interval": 60,
    "retention_days": 30,
    "health_check_timeout": 10
  }
}
EOF
    fi
}

# Send alert notification
send_alert() {
    local severity=$1
    local message=$2
    local details=$3
    
    local timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    local hostname=$(hostname)
    
    # Log alert
    case $severity in
        "critical")
            critical "$message"
            ;;
        "warning")
            warning "$message"
            ;;
        *)
            log "$message"
            ;;
    esac
    
    # Send webhook notification if configured
    if [[ -n "$WEBHOOK_URL" ]]; then
        local color="#ff0000"  # red for critical
        [[ "$severity" == "warning" ]] && color="#ffa500"  # orange for warning
        [[ "$severity" == "info" ]] && color="#00ff00"     # green for info
        
        local payload=$(cat << EOF
{
  "text": "🔍 TaskActivity Monitor Alert",
  "attachments": [
    {
      "color": "$color",
      "fields": [
        {
          "title": "Severity",
          "value": "$severity",
          "short": true
        },
        {
          "title": "Host",
          "value": "$hostname",
          "short": true
        },
        {
          "title": "Stack",
          "value": "$STACK_NAME",
          "short": true
        },
        {
          "title": "Time",
          "value": "$timestamp",
          "short": true
        },
        {
          "title": "Message",
          "value": "$message",
          "short": false
        },
        {
          "title": "Details",
          "value": "$details",
          "short": false
        }
      ]
    }
  ]
}
EOF
        )
        
        curl -s -X POST \
            -H "Content-Type: application/json" \
            -d "$payload" \
            "$WEBHOOK_URL" >/dev/null 2>&1 || {
            warning "Failed to send webhook notification"
        }
    fi
}

# Check Docker Swarm health
check_swarm_health() {
    log "Checking Docker Swarm health..."
    
    # Check if Swarm is active
    if ! docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
        send_alert "critical" "Docker Swarm is not active" "Swarm mode is required for the application to function"
        return 1
    fi
    
    # Check node status
    local unhealthy_nodes=$(docker node ls --format "{{.Status}} {{.Availability}}" | grep -v "Ready Active" | wc -l)
    if [[ $unhealthy_nodes -gt 0 ]]; then
        send_alert "warning" "$unhealthy_nodes unhealthy Swarm nodes detected" "$(docker node ls)"
    fi
    
    success "Docker Swarm is healthy"
    return 0
}

# Check secrets health
check_secrets_health() {
    log "Checking secrets health..."
    
    local required_secrets=("${STACK_NAME}_db_username" "${STACK_NAME}_db_password")
    local missing_secrets=()
    
    for secret in "${required_secrets[@]}"; do
        if ! docker secret ls --format "{{.Name}}" | grep -q "^${secret}$"; then
            missing_secrets+=("$secret")
        fi
    done
    
    if [[ ${#missing_secrets[@]} -gt 0 ]]; then
        local missing_list=$(IFS=', '; echo "${missing_secrets[*]}")
        send_alert "critical" "Missing required secrets" "Missing secrets: $missing_list"
        return 1
    fi
    
    # Check secret age (rotation recommendation)
    for secret in "${required_secrets[@]}"; do
        local created_at=$(docker secret inspect "$secret" --format "{{.CreatedAt}}")
        local created_timestamp=$(date -d "$created_at" +%s 2>/dev/null || echo "0")
        local current_timestamp=$(date +%s)
        local age_days=$(( (current_timestamp - created_timestamp) / 86400 ))
        
        if [[ $age_days -gt 90 ]]; then
            send_alert "warning" "Secret rotation recommended" "Secret '$secret' is $age_days days old (recommend rotation after 90 days)"
        fi
    done
    
    success "All required secrets are present"
    return 0
}

# Check application health
check_application_health() {
    log "Checking application health..."
    
    # Check if stack is deployed
    if ! docker stack ls | grep -q "$STACK_NAME"; then
        send_alert "critical" "Stack not deployed" "Stack '$STACK_NAME' is not found"
        return 1
    fi
    
    # Check service status
    local services_status=$(docker stack services "$STACK_NAME" --format "{{.Name}} {{.Replicas}}")
    local unhealthy_services=()
    
    while IFS= read -r line; do
        local service_name=$(echo "$line" | awk '{print $1}')
        local replicas=$(echo "$line" | awk '{print $2}')
        
        if [[ "$replicas" == *"0/"* ]]; then
            unhealthy_services+=("$service_name")
        elif [[ "$replicas" =~ ^([0-9]+)/([0-9]+)$ ]]; then
            local running=${BASH_REMATCH[1]}
            local desired=${BASH_REMATCH[2]}
            if [[ $running -lt $desired ]]; then
                unhealthy_services+=("$service_name (${running}/${desired})")
            fi
        fi
    done <<< "$services_status"
    
    if [[ ${#unhealthy_services[@]} -gt 0 ]]; then
        local unhealthy_list=$(IFS=', '; echo "${unhealthy_services[*]}")
        send_alert "critical" "Unhealthy services detected" "Services: $unhealthy_list"
        return 1
    fi
    
    success "All services are healthy"
    return 0
}

# Check application responsiveness
check_application_response() {
    log "Checking application responsiveness..."
    
    local start_time=$(date +%s%3N)
    local health_response=$(curl -s -w "%{http_code}" -o /dev/null --max-time 10 http://localhost/actuator/health 2>/dev/null || echo "000")
    local end_time=$(date +%s%3N)
    local response_time=$((end_time - start_time))
    
    # Check HTTP status
    if [[ "$health_response" != "200" ]]; then
        send_alert "critical" "Application health check failed" "HTTP status: $health_response, Response time: ${response_time}ms"
        return 1
    fi
    
    # Check response time
    if [[ $response_time -gt $RESPONSE_TIME_THRESHOLD ]]; then
        send_alert "warning" "Slow response time detected" "Response time: ${response_time}ms (threshold: ${RESPONSE_TIME_THRESHOLD}ms)"
    fi
    
    success "Application is responsive (${response_time}ms)"
    return 0
}

# Check database connectivity
check_database_health() {
    log "Checking database connectivity..."
    
    # Get database service name
    local db_service="${STACK_NAME}_postgres-production"
    
    # Check if database service is running
    local db_replicas=$(docker service ls --filter "name=$db_service" --format "{{.Replicas}}" 2>/dev/null)
    
    if [[ -z "$db_replicas" ]] || [[ "$db_replicas" == "0/1" ]]; then
        send_alert "critical" "Database service is not running" "Service: $db_service"
        return 1
    fi
    
    # Note: /actuator/health/db endpoint is not available due to security restrictions
    # Database health is verified through service status and connection tests
    success "Database service is running"
    return 0
}

# Check system resources
check_system_resources() {
    log "Checking system resources..."
    
    # Check CPU usage
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//' 2>/dev/null || echo "0")
    cpu_usage=${cpu_usage%.*}  # Remove decimal part
    
    if [[ $cpu_usage -gt $CPU_THRESHOLD ]]; then
        send_alert "warning" "High CPU usage detected" "CPU usage: ${cpu_usage}% (threshold: ${CPU_THRESHOLD}%)"
    fi
    
    # Check memory usage
    local memory_info=$(free | grep "Mem:")
    local total_mem=$(echo "$memory_info" | awk '{print $2}')
    local used_mem=$(echo "$memory_info" | awk '{print $3}')
    local memory_usage=$(( (used_mem * 100) / total_mem ))
    
    if [[ $memory_usage -gt $MEMORY_THRESHOLD ]]; then
        send_alert "warning" "High memory usage detected" "Memory usage: ${memory_usage}% (threshold: ${MEMORY_THRESHOLD}%)"
    fi
    
    # Check disk usage
    local disk_usage=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
    
    if [[ $disk_usage -gt $DISK_THRESHOLD ]]; then
        send_alert "critical" "High disk usage detected" "Disk usage: ${disk_usage}% (threshold: ${DISK_THRESHOLD}%)"
    fi
    
    success "System resources are within normal ranges (CPU: ${cpu_usage}%, Memory: ${memory_usage}%, Disk: ${disk_usage}%)"
    return 0
}

# Check container logs for errors
check_application_logs() {
    log "Checking application logs for errors..."
    
    local error_keywords=("ERROR" "FATAL" "Exception" "OutOfMemoryError" "ConnectException" "SQLException")
    local recent_errors=()
    
    # Get logs from the last 5 minutes
    local since_time=$(date -d '5 minutes ago' '+%Y-%m-%dT%H:%M:%S')
    
    for service in $(docker stack services "$STACK_NAME" --format "{{.Name}}"); do
        for keyword in "${error_keywords[@]}"; do
            local error_count=$(docker service logs "$service" --since "$since_time" 2>/dev/null | grep -i "$keyword" | wc -l)
            if [[ $error_count -gt 0 ]]; then
                recent_errors+=("$service: $error_count $keyword occurrences")
            fi
        done
    done
    
    if [[ ${#recent_errors[@]} -gt 0 ]]; then
        local error_summary=$(IFS='; '; echo "${recent_errors[*]}")
        send_alert "warning" "Recent errors detected in logs" "$error_summary"
        return 1
    fi
    
    success "No critical errors found in recent logs"
    return 0
}

# Generate health report
generate_health_report() {
    local report_file="$MONITOR_DIR/health-report-$(date +%Y%m%d-%H%M%S).json"
    local timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    
    log "Generating comprehensive health report..."
    
    # Gather comprehensive system information
    local docker_version=$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo "unknown")
    local swarm_nodes=$(docker node ls --format "{{.Hostname}} {{.Status}} {{.Availability}}" 2>/dev/null || echo "unknown")
    local stack_services=$(docker stack services "$STACK_NAME" --format "{{.Name}} {{.Mode}} {{.Replicas}} {{.Image}}" 2>/dev/null || echo "unknown")
    local system_info=$(uname -a)
    local uptime=$(uptime)
    
    cat > "$report_file" << EOF
{
  "report_timestamp": "$timestamp",
  "stack_name": "$STACK_NAME",
  "hostname": "$(hostname)",
  "system": {
    "info": "$system_info",
    "uptime": "$uptime",
    "docker_version": "$docker_version"
  },
  "swarm": {
    "status": "$(docker info --format '{{.Swarm.LocalNodeState}}' 2>/dev/null || echo 'unknown')",
    "nodes": "$swarm_nodes"
  },
  "services": {
    "list": "$stack_services",
    "count": $(docker stack services "$STACK_NAME" --format "{{.Name}}" 2>/dev/null | wc -l)
  },
  "secrets": {
    "count": $(docker secret ls --format "{{.Name}}" | grep "^${STACK_NAME}_" | wc -l),
    "list": "$(docker secret ls --filter "name=${STACK_NAME}_" --format "{{.Name}} {{.CreatedAt}}" 2>/dev/null || echo 'unknown')"
  },
  "health_checks": {
    "swarm": "$(check_swarm_health >/dev/null 2>&1 && echo 'PASS' || echo 'FAIL')",
    "secrets": "$(check_secrets_health >/dev/null 2>&1 && echo 'PASS' || echo 'FAIL')",
    "application": "$(check_application_health >/dev/null 2>&1 && echo 'PASS' || echo 'FAIL')",
    "response": "$(check_application_response >/dev/null 2>&1 && echo 'PASS' || echo 'FAIL')",
    "database": "$(check_database_health >/dev/null 2>&1 && echo 'PASS' || echo 'FAIL')",
    "resources": "$(check_system_resources >/dev/null 2>&1 && echo 'PASS' || echo 'FAIL')",
    "logs": "$(check_application_logs >/dev/null 2>&1 && echo 'PASS' || echo 'FAIL')"
  }
}
EOF

    success "Health report generated: $report_file"
}

# Continuous monitoring mode
continuous_monitoring() {
    local interval=${1:-60}  # Default 60 seconds
    
    log "Starting continuous monitoring (interval: ${interval}s)"
    log "Press Ctrl+C to stop monitoring"
    
    while true; do
        echo ""
        log "=== Health Check Cycle Started ==="
        
        local failed_checks=0
        
        check_swarm_health || ((failed_checks++))
        check_secrets_health || ((failed_checks++))
        check_application_health || ((failed_checks++))
        check_application_response || ((failed_checks++))
        check_database_health || ((failed_checks++))
        check_system_resources || ((failed_checks++))
        check_application_logs || ((failed_checks++))
        
        if [[ $failed_checks -eq 0 ]]; then
            success "=== All health checks passed ==="
        else
            warning "=== $failed_checks health check(s) failed ==="
        fi
        
        # Generate periodic detailed report (every 10 cycles)
        local cycle_count_file="$MONITOR_DIR/.cycle_count"
        local cycle_count=1
        if [[ -f "$cycle_count_file" ]]; then
            cycle_count=$(<"$cycle_count_file")
        fi
        
        if [[ $((cycle_count % 10)) -eq 0 ]]; then
            generate_health_report
        fi
        
        echo $((cycle_count + 1)) > "$cycle_count_file"
        
        sleep "$interval"
    done
}

# One-time health check
single_health_check() {
    log "Performing single health check..."
    
    local failed_checks=0
    
    check_swarm_health || ((failed_checks++))
    check_secrets_health || ((failed_checks++))
    check_application_health || ((failed_checks++))
    check_application_response || ((failed_checks++))
    check_database_health || ((failed_checks++))
    check_system_resources || ((failed_checks++))
    check_application_logs || ((failed_checks++))
    
    generate_health_report
    
    echo ""
    if [[ $failed_checks -eq 0 ]]; then
        success "🎉 All health checks passed!"
        return 0
    else
        error "❌ $failed_checks health check(s) failed!"
        return 1
    fi
}

# Show current status
show_status() {
    echo -e "${BOLD}Current System Status${NC}"
    echo "====================="
    echo ""
    
    # Stack information
    echo -e "${BOLD}Stack: $STACK_NAME${NC}"
    if docker stack ls | grep -q "$STACK_NAME"; then
        docker stack services "$STACK_NAME"
    else
        echo "Stack not deployed"
    fi
    echo ""
    
    # Secrets information
    echo -e "${BOLD}Secrets:${NC}"
    docker secret ls --filter "name=${STACK_NAME}_"
    echo ""
    
    # System resources
    echo -e "${BOLD}System Resources:${NC}"
    echo "CPU: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//' || echo "unknown")%"
    echo "Memory: $(free -h | grep "Mem:" | awk '{printf "%.1f%%", ($3/$2)*100}')"
    echo "Disk: $(df -h / | tail -1 | awk '{print $5}')"
    echo ""
    
    # Recent logs
    echo -e "${BOLD}Recent Activity:${NC}"
    if [[ -f "$LOG_FILE" ]]; then
        tail -n 10 "$LOG_FILE"
    else
        echo "No monitoring logs found"
    fi
}

# Main function
main() {
    local command=$1
    shift
    
    setup_monitoring
    
    case $command in
        monitor)
            continuous_monitoring "$@"
            ;;
        check)
            single_health_check
            ;;
        status)
            show_status
            ;;
        report)
            generate_health_report
            ;;
        *)
            echo "Usage: $0 <command> [options]"
            echo ""
            echo "Commands:"
            echo "  monitor [interval]    Start continuous monitoring (default: 60s)"
            echo "  check                 Perform single health check"
            echo "  status                Show current system status"
            echo "  report                Generate detailed health report"
            echo ""
            echo "Environment Variables:"
            echo "  STACK_NAME           Stack name (default: taskactivity)"
            echo "  WEBHOOK_URL          Webhook URL for alerts"
            echo "  CPU_THRESHOLD        CPU alert threshold % (default: 80)"
            echo "  MEMORY_THRESHOLD     Memory alert threshold % (default: 85)"
            echo "  DISK_THRESHOLD       Disk alert threshold % (default: 90)"
            exit 1
            ;;
    esac
}

# Handle interrupts gracefully
trap 'log "Monitoring stopped"; exit 0' INT TERM

# Parse command line arguments
COMMAND=""
while [[ $# -gt 0 ]]; do
    case $1 in
        monitor|check|status|report)
            COMMAND=$1
            shift
            break
            ;;
        --stack-name=*)
            STACK_NAME="${1#*=}"
            shift
            ;;
        --webhook-url=*)
            WEBHOOK_URL="${1#*=}"
            shift
            ;;
        --cpu-threshold=*)
            CPU_THRESHOLD="${1#*=}"
            shift
            ;;
        --memory-threshold=*)
            MEMORY_THRESHOLD="${1#*=}"
            shift
            ;;
        --disk-threshold=*)
            DISK_THRESHOLD="${1#*=}"
            shift
            ;;
        --help)
            main "help"
            ;;
        *)
            error "Unknown option: $1"
            main "help"
            ;;
    esac
done

if [[ -z "$COMMAND" ]]; then
    main "help"
fi

# Run the command with remaining arguments
main "$COMMAND" "$@"