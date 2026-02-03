#!/bin/bash

##############################################################################
# Jenkins Workspace Cleanup Script
#
# Description: Cleans up corrupt or stale Jenkins workspace directories that
#              can accumulate after WSL2 restarts or failed builds.
#
# Author: Dean Ammons
# Date: February 2026
##############################################################################

set -e

JENKINS_WORKSPACE="/var/lib/jenkins/workspace"
DRY_RUN=${1:-false}

echo "========================================="
echo "Jenkins Workspace Cleanup"
echo "========================================="
echo "Workspace: ${JENKINS_WORKSPACE}"
echo "Dry Run: ${DRY_RUN}"
echo ""

# Check if Jenkins workspace exists
if [ ! -d "${JENKINS_WORKSPACE}" ]; then
    echo "ERROR: Jenkins workspace not found at ${JENKINS_WORKSPACE}"
    exit 1
fi

# Function to clean empty directories
clean_empty_dirs() {
    local dir=$1
    local count=0
    
    echo "Checking for empty directories in ${dir}..."
    
    while IFS= read -r -d '' emptydir; do
        if [ "${DRY_RUN}" = "true" ]; then
            echo "[DRY RUN] Would remove: ${emptydir}"
        else
            echo "Removing empty directory: ${emptydir}"
            rm -rf "${emptydir}"
        fi
        ((count++))
    done < <(find "${dir}" -mindepth 1 -type d -empty -print0 2>/dev/null)
    
    echo "Found ${count} empty directories"
    return 0
}

# Function to clean old @script directories
clean_script_dirs() {
    local workspace=$1
    local days=${2:-7}
    local count=0
    
    echo "Checking for @script directories older than ${days} days..."
    
    while IFS= read -r -d '' scriptdir; do
        if [ "${DRY_RUN}" = "true" ]; then
            echo "[DRY RUN] Would remove: ${scriptdir}"
        else
            echo "Removing old @script directory: ${scriptdir}"
            rm -rf "${scriptdir}"
        fi
        ((count++))
    done < <(find "${workspace}" -maxdepth 1 -type d -name "*@script" -mtime +${days} -print0 2>/dev/null)
    
    echo "Found ${count} old @script directories"
    return 0
}

# Function to clean old @tmp directories
clean_tmp_dirs() {
    local workspace=$1
    local days=${2:-7}
    local count=0
    
    echo "Checking for @tmp directories older than ${days} days..."
    
    while IFS= read -r -d '' tmpdir; do
        if [ "${DRY_RUN}" = "true" ]; then
            echo "[DRY RUN] Would remove: ${tmpdir}"
        else
            echo "Removing old @tmp directory: ${tmpdir}"
            rm -rf "${tmpdir}"
        fi
        ((count++))
    done < <(find "${workspace}" -maxdepth 1 -type d -name "*@tmp" -mtime +${days} -print0 2>/dev/null)
    
    echo "Found ${count} old @tmp directories"
    return 0
}

# Function to show workspace statistics
show_stats() {
    local workspace=$1
    
    echo ""
    echo "========================================="
    echo "Workspace Statistics"
    echo "========================================="
    
    echo "Total size: $(du -sh ${workspace} 2>/dev/null | cut -f1)"
    
    local job_count=$(find "${workspace}" -maxdepth 1 -type d ! -name "workspace" | wc -l)
    echo "Job directories: ${job_count}"
    
    local script_count=$(find "${workspace}" -maxdepth 1 -type d -name "*@script" 2>/dev/null | wc -l)
    echo "@script directories: ${script_count}"
    
    local tmp_count=$(find "${workspace}" -maxdepth 1 -type d -name "*@tmp" 2>/dev/null | wc -l)
    echo "@tmp directories: ${tmp_count}"
    
    echo ""
}

# Main execution
echo "Starting cleanup..."
echo ""

# Show before stats
show_stats "${JENKINS_WORKSPACE}"

# Clean empty directories in @script paths
if [ -d "${JENKINS_WORKSPACE}/TaskActivity-Pipeline@script" ]; then
    clean_empty_dirs "${JENKINS_WORKSPACE}/TaskActivity-Pipeline@script"
fi

# Clean old @script directories (older than 7 days)
clean_script_dirs "${JENKINS_WORKSPACE}" 7

# Clean old @tmp directories (older than 7 days)
clean_tmp_dirs "${JENKINS_WORKSPACE}" 7

# Show after stats
echo ""
show_stats "${JENKINS_WORKSPACE}"

echo "========================================="
echo "Cleanup Complete"
echo "========================================="

if [ "${DRY_RUN}" = "true" ]; then
    echo ""
    echo "This was a dry run. To actually perform cleanup, run:"
    echo "  sudo $(basename $0)"
fi
