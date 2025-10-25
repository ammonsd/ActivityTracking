#!/bin/bash
set -e

echo "=========================================="
echo "Task Activity Application Startup"
echo "=========================================="

# Check if Cloudflare credentials are available
if [ -n "$CLOUDFLARE_TUNNEL_CREDENTIALS" ] && [ -n "$CLOUDFLARE_TUNNEL_CONFIG" ]; then
    echo "[INFO] Cloudflare tunnel configuration detected"
    
    # Create cloudflared credentials file
    echo "[INFO] Setting up Cloudflare tunnel credentials..."
    echo "$CLOUDFLARE_TUNNEL_CREDENTIALS" > /etc/cloudflared/credentials.json
    
    # Create cloudflared config file
    echo "[INFO] Setting up Cloudflare tunnel configuration..."
    echo "$CLOUDFLARE_TUNNEL_CONFIG" > /etc/cloudflared/config.yml
    
    # Start cloudflared tunnel in background
    echo "[INFO] Starting Cloudflare tunnel..."
    cloudflared tunnel --config /etc/cloudflared/config.yml run &
    CLOUDFLARED_PID=$!
    
    echo "[SUCCESS] Cloudflare tunnel started (PID: $CLOUDFLARED_PID)"
    
    # Give cloudflared a moment to establish connection
    sleep 3
else
    echo "[INFO] No Cloudflare tunnel configuration found - running without tunnel"
fi

# Start the Spring Boot application
echo "[INFO] Starting Spring Boot application..."
echo "=========================================="

# Execute the main application (replaces this script's process)
exec java -jar /opt/app.jar
