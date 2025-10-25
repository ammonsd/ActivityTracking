#!/bin/bash

# Kubernetes Deployment Script for Task Activity Management System
# This script helps deploy the application to Kubernetes with proper secrets management

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}Kubernetes Deployment for Task Activity Management System${NC}"
echo "==========================================================="

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}Error: kubectl is not installed or not in PATH.${NC}"
    echo "Please install kubectl to continue."
    exit 1
fi

# Check if we can connect to the cluster
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Error: Cannot connect to Kubernetes cluster.${NC}"
    echo "Please ensure you have a valid kubeconfig and cluster access."
    exit 1
fi

echo -e "${BLUE}Current Kubernetes context:${NC}"
kubectl config current-context

echo ""
read -p "Continue with deployment to this cluster? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled."
    exit 0
fi

# Function to encode base64
encode_base64() {
    echo -n "$1" | base64 -w 0
}

# Collect database credentials
echo ""
echo -e "${BLUE}Database Configuration${NC}"
read -p "Enter database username: " db_username
if [ -z "$db_username" ]; then
    echo -e "${RED}Error: Database username cannot be empty.${NC}"
    exit 1
fi

echo "Enter database password (input will be hidden):"
read -s db_password
if [ -z "$db_password" ]; then
    echo -e "${RED}Error: Database password cannot be empty.${NC}"
    exit 1
fi

# Encode credentials
encoded_username=$(encode_base64 "$db_username")
encoded_password=$(encode_base64 "$db_password")

echo ""
echo -e "${BLUE}Creating Kubernetes resources...${NC}"

# Create namespace
kubectl apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: taskactivity
EOF

# Create secret with actual credentials
kubectl apply -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: taskactivity-db-credentials
  namespace: taskactivity
type: Opaque
data:
  username: $encoded_username
  password: $encoded_password
EOF

# Deploy the rest of the resources
if [ -f "k8s/taskactivity-deployment.yaml" ]; then
    # Update the deployment file with actual credentials and apply
    sed "s/username: cG9zdGdyZXM=/username: $encoded_username/" k8s/taskactivity-deployment.yaml | \
    sed "s/password: eW91cl9zZWN1cmVfcGFzc3dvcmQ=/password: $encoded_password/" | \
    kubectl apply -f -
else
    echo -e "${RED}Error: k8s/taskactivity-deployment.yaml not found.${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo ""
echo -e "${BLUE}Useful commands:${NC}"
echo "Check pod status:"
echo "  kubectl get pods -n taskactivity"
echo ""
echo "View application logs:"
echo "  kubectl logs -f deployment/taskactivity-app -n taskactivity"
echo ""
echo "Check services:"
echo "  kubectl get services -n taskactivity"
echo ""
echo "Access the application (if using port-forward):"
echo "  kubectl port-forward service/taskactivity-service 8080:8080 -n taskactivity"
echo "  Then visit: http://localhost:8080"
echo ""
echo "To delete the deployment:"
echo "  kubectl delete namespace taskactivity"