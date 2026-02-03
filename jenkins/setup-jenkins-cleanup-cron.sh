#!/bin/bash

##############################################################################
# Setup Jenkins Workspace Cleanup Cron Job
#
# Description: Configures a daily cron job to clean up Jenkins workspace
#              directories, preventing accumulation of corrupt directories
#              after WSL2 restarts.
#
# Author: Dean Ammons
# Date: February 2026
##############################################################################

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLEANUP_SCRIPT="${SCRIPT_DIR}/cleanup-jenkins-workspace.sh"
CRON_TIME="0 2 * * *"  # Daily at 2 AM

echo "========================================="
echo "Setup Jenkins Workspace Cleanup Cron"
echo "========================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "ERROR: This script must be run as root"
    echo "Usage: sudo $0"
    exit 1
fi

# Check if cleanup script exists
if [ ! -f "${CLEANUP_SCRIPT}" ]; then
    echo "ERROR: Cleanup script not found at ${CLEANUP_SCRIPT}"
    exit 1
fi

# Make cleanup script executable
chmod +x "${CLEANUP_SCRIPT}"
echo "✓ Made cleanup script executable"

# Create log directory
LOG_DIR="/var/log/jenkins-cleanup"
mkdir -p "${LOG_DIR}"
chown jenkins:jenkins "${LOG_DIR}"
echo "✓ Created log directory: ${LOG_DIR}"

# Create cron job entry
CRON_JOB="${CRON_TIME} /bin/bash ${CLEANUP_SCRIPT} >> ${LOG_DIR}/cleanup-\$(date +\\%Y\\%m\\%d).log 2>&1"

# Check if cron job already exists
if crontab -u root -l 2>/dev/null | grep -q "cleanup-jenkins-workspace.sh"; then
    echo "⚠ Cron job already exists. Removing old entry..."
    crontab -u root -l 2>/dev/null | grep -v "cleanup-jenkins-workspace.sh" | crontab -u root -
fi

# Add new cron job
(crontab -u root -l 2>/dev/null; echo "# Jenkins workspace cleanup - runs daily at 2 AM"; echo "${CRON_JOB}") | crontab -u root -

echo "✓ Added cron job: ${CRON_TIME}"
echo ""

# Show current crontab
echo "Current cron jobs:"
crontab -u root -l | grep -A 1 "Jenkins workspace cleanup" || echo "(none)"

echo ""
echo "========================================="
echo "Setup Complete"
echo "========================================="
echo ""
echo "The Jenkins workspace will be cleaned daily at 2 AM"
echo "Logs will be stored in: ${LOG_DIR}"
echo ""
echo "To test the cleanup manually, run:"
echo "  sudo ${CLEANUP_SCRIPT}"
echo ""
echo "To view cron logs:"
echo "  ls -lh ${LOG_DIR}"
