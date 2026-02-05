#!/bin/bash
<#
.SYNOPSIS
    Fix Jenkins to run on port 8081 instead of 8080.

.DESCRIPTION
    This script configures Jenkins to use port 8081 by creating a systemd override.
    Run this if Jenkins reverts to port 8080 after a restart or reinstall.
    
    Prerequisites:
    • Must be run as root (or with sudo)
    • Jenkins must be installed
    
.EXAMPLE
    sudo bash fix-jenkins-port.sh
    
    Fix Jenkins port configuration and restart service.

.NOTES
    Author: Dean Ammons
    Date: February 2026
#>

echo "=========================================="
echo "Jenkins Port 8081 Configuration Script"
echo "=========================================="
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo "ERROR: This script must be run as root"
    echo "Usage: sudo bash fix-jenkins-port.sh"
    exit 1
fi

echo "Step 1: Creating systemd override directory..."
mkdir -p /etc/systemd/system/jenkins.service.d

echo "Step 2: Creating override configuration..."
cat > /etc/systemd/system/jenkins.service.d/override.conf << 'EOF'
[Service]
Restart=no
Environment="JENKINS_PORT=8081"
EOF

if [ $? -eq 0 ]; then
    echo "✓ Override configuration created"
else
    echo "✗ Failed to create override configuration"
    exit 1
fi

echo ""
echo "Step 3: Reloading systemd daemon..."
systemctl daemon-reload

echo "Step 4: Restarting Jenkins..."
systemctl restart jenkins

echo ""
echo "Waiting 15 seconds for Jenkins to start..."
sleep 15

echo ""
echo "Step 5: Verifying configuration..."

# Check if Jenkins is running
if systemctl is-active --quiet jenkins; then
    echo "✓ Jenkins service is active"
else
    echo "✗ Jenkins service is not running"
    echo "  Check status with: systemctl status jenkins"
    exit 1
fi

# Check if Jenkins is listening on port 8081
if netstat -tlnp 2>/dev/null | grep -q ':8081.*java' || ss -tlnp 2>/dev/null | grep -q ':8081.*java'; then
    echo "✓ Jenkins is listening on port 8081"
    echo ""
    echo "=========================================="
    echo "SUCCESS: Jenkins is now configured for port 8081"
    echo "Access Jenkins at: http://localhost:8081"
    echo "=========================================="
else
    echo "✗ Jenkins may not be listening on port 8081 yet"
    echo ""
    echo "Check with one of these commands:"
    echo "  netstat -tlnp | grep java"
    echo "  ss -tlnp | grep java"
    echo "  ps aux | grep jenkins"
fi
