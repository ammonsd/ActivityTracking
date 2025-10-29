#!/usr/bin/env bash

################################################################################
# Cleanup Old Docker Images from ECR
#
# Usage: ./cleanup-old-images.sh [--dry-run] [--keep N]
#
# This script removes old Docker images from ECR to save storage costs.
# By default, keeps the latest 10 images and removes older ones.
#
# Options:
#   --dry-run    Show what would be deleted without actually deleting
#   --keep N     Number of recent images to keep (default: 10)
################################################################################

set -e

# Configuration
AWS_REGION=${AWS_REGION:-us-east-1}
ECR_REPOSITORY="taskactivity"
KEEP_COUNT=10
DRY_RUN=false

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --keep)
            KEEP_COUNT="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--dry-run] [--keep N]"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ECR Image Cleanup${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Repository: ${ECR_REPOSITORY}"
echo -e "Region: ${AWS_REGION}"
echo -e "Keep latest: ${KEEP_COUNT} images"
if [ "${DRY_RUN}" = true ]; then
    echo -e "Mode: ${YELLOW}DRY RUN (no actual deletion)${NC}"
else
    echo -e "Mode: ${RED}LIVE (will delete images)${NC}"
fi
echo -e "${BLUE}========================================${NC}"
echo ""

# Get all images sorted by push date
echo -e "${YELLOW}Fetching image list...${NC}"
IMAGES=$(aws ecr describe-images \
    --repository-name "${ECR_REPOSITORY}" \
    --region "${AWS_REGION}" \
    --query 'sort_by(imageDetails,& imagePushedAt)[*].[imageDigest,imageTags[0],imagePushedAt]' \
    --output json)

TOTAL_IMAGES=$(echo "${IMAGES}" | jq 'length')
echo -e "${GREEN}✓ Found ${TOTAL_IMAGES} images${NC}"
echo ""

if [ "${TOTAL_IMAGES}" -le "${KEEP_COUNT}" ]; then
    echo -e "${GREEN}No cleanup needed. Total images (${TOTAL_IMAGES}) <= keep count (${KEEP_COUNT})${NC}"
    exit 0
fi

# Calculate how many to delete
DELETE_COUNT=$((TOTAL_IMAGES - KEEP_COUNT))
echo -e "${YELLOW}Images to delete: ${DELETE_COUNT}${NC}"
echo ""

# Get images to delete (oldest ones)
TO_DELETE=$(echo "${IMAGES}" | jq -r ".[0:${DELETE_COUNT}][] | @json")

# Display images to be deleted
echo -e "${BLUE}Images to be deleted:${NC}"
echo "${TO_DELETE}" | jq -r '. | 
    "  Digest: \(.[0] | split(":")[1] | .[0:12])...\n" +
    "  Tag: \(.[1] // "untagged")\n" +
    "  Pushed: \(.[2])\n" +
    "---"'

# Confirm deletion if not dry run
if [ "${DRY_RUN}" = false ]; then
    echo ""
    read -p "Delete these ${DELETE_COUNT} images? (yes/no): " CONFIRM
    if [ "${CONFIRM}" != "yes" ]; then
        echo -e "${YELLOW}Deletion cancelled${NC}"
        exit 0
    fi
    
    echo ""
    echo -e "${YELLOW}Deleting images...${NC}"
    
    # Delete images one by one
    DELETED=0
    while IFS= read -r IMAGE_JSON; do
        DIGEST=$(echo "${IMAGE_JSON}" | jq -r '.[0]')
        TAG=$(echo "${IMAGE_JSON}" | jq -r '.[1] // "untagged"')
        
        echo -e "  Deleting: ${TAG} (${DIGEST:7:12}...)"
        
        if aws ecr batch-delete-image \
            --repository-name "${ECR_REPOSITORY}" \
            --region "${AWS_REGION}" \
            --image-ids imageDigest="${DIGEST}" \
            --output json > /dev/null 2>&1; then
            DELETED=$((DELETED + 1))
        else
            echo -e "  ${RED}Failed to delete${NC}"
        fi
    done <<< "${TO_DELETE}"
    
    echo ""
    echo -e "${GREEN}✓ Deleted ${DELETED} images${NC}"
else
    echo ""
    echo -e "${YELLOW}DRY RUN: No images were actually deleted${NC}"
    echo -e "Remove --dry-run flag to perform actual deletion"
fi

# Show storage savings estimate
DELETED_SIZE=$(echo "${TO_DELETE}" | jq -s 'map(.[3] // 0) | add')
echo ""
echo -e "${BLUE}Estimated storage freed: ${DELETED_SIZE} bytes${NC}"
echo ""

echo -e "${GREEN}✓ Cleanup complete${NC}"
