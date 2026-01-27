# AWS EKS Deployment Guide for Task Activity Management System

Complete guide for deploying the Task Activity Management application to Amazon EKS (Elastic Kubernetes Service).

**Author:** Dean Ammons  
**Date:** January 2026  
**Last Updated:** January 27, 2026

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Step 1: Create IAM Role for IRSA](#step-1-create-iam-role-for-irsa)
- [Step 2: Install Required Controllers](#step-2-install-required-controllers)
- [Step 3: Create AWS Resources](#step-3-create-aws-resources)
- [Step 4: Deploy Application](#step-4-deploy-application)
- [Step 5: Configure DNS](#step-5-configure-dns)
- [Verification](#verification)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)
- [Cleanup](#cleanup)

---

## Overview

This deployment uses:

- **AWS EKS** - Managed Kubernetes cluster
- **AWS RDS** - PostgreSQL database
- **AWS S3** - Receipt file storage
- **AWS SES** - Email notifications
- **AWS Secrets Manager** - Secure credential storage
- **AWS ALB** - Application Load Balancer for ingress
- **AWS ACM** - SSL/TLS certificates
- **External Secrets Operator** - Sync secrets from AWS Secrets Manager to Kubernetes
- **IAM Roles for Service Accounts (IRSA)** - Grant AWS permissions to pods

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Internet                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   Route 53 (DNS)     │
              │  taskactivitytracker │
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   ACM Certificate    │
              │     (SSL/TLS)        │
              └──────────┬───────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────┐
│                      AWS ALB                                │
│              (Application Load Balancer)                    │
│  - Health checks: /actuator/health                         │
│  - SSL Termination                                         │
│  - HTTP → HTTPS redirect                                   │
└────────────────────────┬───────────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────┐
│                     EKS Cluster                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Ingress Controller                       │  │
│  │         (AWS Load Balancer Controller)               │  │
│  └─────────────────────┬────────────────────────────────┘  │
│                        │                                    │
│                        ▼                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │             taskactivity-service                      │  │
│  │                (ClusterIP)                           │  │
│  └─────────────────────┬────────────────────────────────┘  │
│                        │                                    │
│        ┌───────────────┴───────────────┐                   │
│        ▼                               ▼                   │
│  ┌──────────┐                    ┌──────────┐             │
│  │  Pod 1   │                    │  Pod 2   │             │
│  │ (Java)   │                    │ (Java)   │             │
│  │  IRSA    │                    │  IRSA    │             │
│  └────┬─────┘                    └────┬─────┘             │
│       │                               │                    │
│       │  ┌────────────────────────────┘                   │
│       │  │                                                 │
│  ┌────▼──▼────────────────────────────────────────────┐   │
│  │       External Secrets Operator                    │   │
│  │   (Syncs from AWS Secrets Manager)                │   │
│  └────────────────────────────────────────────────────┘   │
└─────────┬──────────────┬──────────────┬────────────────────┘
          │              │              │
          ▼              ▼              ▼
┌─────────────┐  ┌──────────────┐  ┌──────────┐
│ AWS RDS     │  │ AWS S3       │  │ AWS SES  │
│ PostgreSQL  │  │ (Receipts)   │  │ (Email)  │
└─────────────┘  └──────────────┘  └──────────┘
          ▲
          │
┌─────────┴──────────┐
│ AWS Secrets Manager│
│  - DB credentials  │
│  - JWT secret      │
│  - Admin password  │
└────────────────────┘
```

---

## Prerequisites

### Required Tools

```bash
# AWS CLI
aws --version  # Should be >= 2.x

# kubectl
kubectl version --client  # Should be >= 1.28

# eksctl (optional, for cluster creation)
eksctl version  # Should be >= 0.150.0

# helm (for installing controllers)
helm version  # Should be >= 3.x
```

### AWS Resources Required

- [x] **EKS Cluster** - Kubernetes 1.28+ running
- [x] **VPC** - With public and private subnets
- [x] **Security Groups** - For EKS, RDS, ALB
- [x] **RDS PostgreSQL** - Database instance
- [x] **S3 Bucket** - `taskactivity-receipts-prod`
- [x] **ECR Repository** - `taskactivity` (already exists: `378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity`)
- [x] **ACM Certificate** - For `taskactivitytracker.com` and `*.taskactivitytracker.com`
- [x] **Secrets Manager** - Secrets created (see Step 3)

### AWS Permissions Required

Your IAM user/role needs:

- EKS cluster admin access
- IAM role creation and policy attachment
- Secrets Manager read/write
- S3 bucket access
- RDS access
- ECR repository access
- Route53 DNS management
- ACM certificate management

---

## Step 1: Create IAM Role for IRSA

IAM Roles for Service Accounts (IRSA) allows Kubernetes pods to assume AWS IAM roles.

### 1.1 Create IAM Policy for Application

Create `taskactivity-eks-pod-policy.json`:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "SecretsManagerAccess",
            "Effect": "Allow",
            "Action": [
                "secretsmanager:GetSecretValue",
                "secretsmanager:DescribeSecret"
            ],
            "Resource": [
                "arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/*"
            ]
        },
        {
            "Sid": "S3ReceiptsAccess",
            "Effect": "Allow",
            "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
            "Resource": ["arn:aws:s3:::taskactivity-receipts-prod/*"]
        },
        {
            "Sid": "S3BucketAccess",
            "Effect": "Allow",
            "Action": ["s3:ListBucket", "s3:GetBucketLocation"],
            "Resource": ["arn:aws:s3:::taskactivity-receipts-prod"]
        },
        {
            "Sid": "SESAccess",
            "Effect": "Allow",
            "Action": ["ses:SendEmail", "ses:SendRawEmail"],
            "Resource": "*"
        }
    ]
}
```

Create the policy:

```bash
aws iam create-policy \
    --policy-name TaskActivityEKSPodPolicy \
    --policy-document file://taskactivity-eks-pod-policy.json \
    --description "Permissions for TaskActivity EKS pods"
```

### 1.2 Create IAM Role with IRSA

Get your EKS cluster OIDC provider:

```bash
# Get OIDC provider URL
aws eks describe-cluster \
    --name taskactivity-cluster \
    --query "cluster.identity.oidc.issuer" \
    --output text

# Example output: https://oidc.eks.us-east-1.amazonaws.com/id/EXAMPLED539D4633E53DE1B71EXAMPLE
```

Create trust policy `taskactivity-eks-trust-policy.json`:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "arn:aws:iam::378010131175:oidc-provider/oidc.eks.us-east-1.amazonaws.com/id/EXAMPLED539D4633E53DE1B71EXAMPLE"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "oidc.eks.us-east-1.amazonaws.com/id/EXAMPLED539D4633E53DE1B71EXAMPLE:sub": "system:serviceaccount:taskactivity:taskactivity-service-account",
                    "oidc.eks.us-east-1.amazonaws.com/id/EXAMPLED539D4633E53DE1B71EXAMPLE:aud": "sts.amazonaws.com"
                }
            }
        }
    ]
}
```

**⚠️ IMPORTANT:** Replace `EXAMPLED539D4633E53DE1B71EXAMPLE` with your actual OIDC provider ID!

Create the role:

```bash
# Create IAM role
aws iam create-role \
    --role-name taskactivity-eks-pod-role \
    --assume-role-policy-document file://taskactivity-eks-trust-policy.json \
    --description "IAM role for TaskActivity EKS pods with IRSA"

# Attach policy to role
aws iam attach-role-policy \
    --role-name taskactivity-eks-pod-role \
    --policy-arn arn:aws:iam::378010131175:policy/TaskActivityEKSPodPolicy

# Verify
aws iam get-role --role-name taskactivity-eks-pod-role
```

---

## Step 2: Install Required Controllers

### 2.1 Install External Secrets Operator

```bash
# Add Helm repository
helm repo add external-secrets https://charts.external-secrets.io
helm repo update

# Create namespace
kubectl create namespace external-secrets-system

# Install External Secrets Operator
helm install external-secrets \
    external-secrets/external-secrets \
    -n external-secrets-system \
    --set installCRDs=true

# Verify installation
kubectl get pods -n external-secrets-system
kubectl get crd | grep external-secrets
```

### 2.2 Install AWS Load Balancer Controller

```bash
# Download IAM policy for ALB controller
curl -o alb-iam-policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.0/docs/install/iam_policy.json

# Create IAM policy
aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://alb-iam-policy.json

# Create service account with IRSA for ALB controller
eksctl create iamserviceaccount \
    --cluster=taskactivity-cluster \
    --namespace=kube-system \
    --name=aws-load-balancer-controller \
    --attach-policy-arn=arn:aws:iam::378010131175:policy/AWSLoadBalancerControllerIAMPolicy \
    --override-existing-serviceaccounts \
    --approve

# Add Helm repository
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install AWS Load Balancer Controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
    -n kube-system \
    --set clusterName=taskactivity-cluster \
    --set serviceAccount.create=false \
    --set serviceAccount.name=aws-load-balancer-controller \
    --set region=us-east-1 \
    --set vpcId=vpc-xxxxxxxxx

# Verify installation
kubectl get deployment -n kube-system aws-load-balancer-controller
kubectl logs -n kube-system deployment/aws-load-balancer-controller
```

---

## Step 3: Create AWS Resources

### 3.1 Create Secrets in AWS Secrets Manager

If not already created:

```bash
# Database credentials
aws secretsmanager create-secret \
    --name taskactivity/database/credentials \
    --description "Database credentials for TaskActivity" \
    --secret-string '{
        "username": "admin",
        "password": "YourSecurePassword123!",
        "jdbcUrl": "jdbc:postgresql://taskactivity-db.xxxxxx.us-east-1.rds.amazonaws.com:5432/AmmoP1DB"
    }' \
    --region us-east-1

# JWT secret
aws secretsmanager create-secret \
    --name taskactivity/jwt/secret \
    --description "JWT secret for TaskActivity" \
    --secret-string '{
        "secret": "your-super-secret-jwt-key-at-least-32-characters-long"
    }' \
    --region us-east-1

# Admin credentials
aws secretsmanager create-secret \
    --name taskactivity/admin/credentials \
    --description "Admin credentials for TaskActivity" \
    --secret-string '{
        "password": "AdminPassword123!"
    }' \
    --region us-east-1
```

### 3.2 Verify S3 Bucket

```bash
# Check if bucket exists
aws s3 ls s3://taskactivity-receipts-prod

# If not exists, create it
aws s3 mb s3://taskactivity-receipts-prod --region us-east-1

# Enable encryption
aws s3api put-bucket-encryption \
    --bucket taskactivity-receipts-prod \
    --server-side-encryption-configuration '{
        "Rules": [{
            "ApplyServerSideEncryptionByDefault": {
                "SSEAlgorithm": "AES256"
            }
        }]
    }'

# Block public access
aws s3api put-public-access-block \
    --bucket taskactivity-receipts-prod \
    --public-access-block-configuration \
        "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
```

### 3.3 Verify RDS Database

```bash
# Check RDS instance
aws rds describe-db-instances \
    --db-instance-identifier taskactivity-db \
    --query 'DBInstances[0].[DBInstanceIdentifier,Endpoint.Address,DBInstanceStatus]' \
    --output table
```

### 3.4 Get ACM Certificate ARN

```bash
# List certificates
aws acm list-certificates --region us-east-1

# Get specific certificate for taskactivitytracker.com
aws acm list-certificates \
    --region us-east-1 \
    --query 'CertificateSummaryList[?DomainName==`taskactivitytracker.com`].CertificateArn' \
    --output text
```

**Update the Ingress file** with your certificate ARN in `taskactivity-ingress-aws.yaml`:

```yaml
alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:378010131175:certificate/YOUR-CERT-ID
```

---

## Step 4: Deploy Application

### 4.1 Configure kubectl Context

```bash
# Update kubeconfig for your EKS cluster
aws eks update-kubeconfig \
    --name taskactivity-cluster \
    --region us-east-1

# Verify connection
kubectl get nodes
kubectl get namespaces
```

### 4.2 Update Configuration Files

Before deploying, update these values in the YAML files:

**In `taskactivity-rbac.yaml`:**

- Verify IAM role ARN is correct: `arn:aws:iam::378010131175:role/taskactivity-eks-pod-role`

**In `taskactivity-ingress-aws.yaml`:**

- Update `alb.ingress.kubernetes.io/subnets` with your public subnet IDs
- Update `alb.ingress.kubernetes.io/certificate-arn` with your ACM certificate ARN
- Update `alb.ingress.kubernetes.io/security-groups` with your security group ID

**In `taskactivity-deployment-aws.yaml`:**

- Verify image: `378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity:latest`
- Verify ConfigMap values match your requirements

### 4.3 Deploy to EKS

```bash
# Navigate to k8s directory
cd k8s

# Deploy in order:
# 1. RBAC (ServiceAccount with IRSA)
kubectl apply -f taskactivity-rbac.yaml

# 2. Application deployment (includes namespace, configmap, external secrets, deployment, service, HPA, PDB)
kubectl apply -f taskactivity-deployment-aws.yaml

# 3. Ingress (ALB)
kubectl apply -f taskactivity-ingress-aws.yaml

# Verify deployments
kubectl get all -n taskactivity
kubectl get externalsecrets -n taskactivity
kubectl get secretstore -n taskactivity
```

### 4.4 Wait for Resources to be Ready

```bash
# Watch pods come up
kubectl get pods -n taskactivity -w

# Check External Secrets sync status
kubectl get externalsecrets -n taskactivity

# Expected output:
# NAME                            STORE                 REFRESH INTERVAL   STATUS         READY
# taskactivity-admin-credentials  aws-secrets-manager   1h                 SecretSynced   True
# taskactivity-db-credentials     aws-secrets-manager   1h                 SecretSynced   True
# taskactivity-jwt-secret         aws-secrets-manager   1h                 SecretSynced   True

# Check if secrets were created
kubectl get secrets -n taskactivity

# Check ALB creation
kubectl get ingress -n taskactivity
kubectl describe ingress taskactivity-ingress -n taskactivity

# Get ALB DNS name (will take 2-3 minutes to provision)
kubectl get ingress taskactivity-ingress -n taskactivity -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

---

## Step 5: Configure DNS

### 5.1 Get ALB DNS Name

```bash
# Get ALB DNS name
ALB_DNS=$(kubectl get ingress taskactivity-ingress -n taskactivity -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo $ALB_DNS
# Example: k8s-taskacti-taskacti-xxxxxxxxxx-yyyyyyyyyy.us-east-1.elb.amazonaws.com
```

### 5.2 Create Route53 Records

```bash
# Get hosted zone ID
HOSTED_ZONE_ID=$(aws route53 list-hosted-zones-by-name \
    --dns-name taskactivitytracker.com \
    --query 'HostedZones[0].Id' \
    --output text)

echo $HOSTED_ZONE_ID

# Create/update A record for root domain
aws route53 change-resource-record-sets \
    --hosted-zone-id $HOSTED_ZONE_ID \
    --change-batch '{
        "Changes": [{
            "Action": "UPSERT",
            "ResourceRecordSet": {
                "Name": "taskactivitytracker.com",
                "Type": "A",
                "AliasTarget": {
                    "HostedZoneId": "Z35SXDOTRQ7X7K",
                    "DNSName": "'"$ALB_DNS"'",
                    "EvaluateTargetHealth": true
                }
            }
        }]
    }'

# Create/update A record for www subdomain
aws route53 change-resource-record-sets \
    --hosted-zone-id $HOSTED_ZONE_ID \
    --change-batch '{
        "Changes": [{
            "Action": "UPSERT",
            "ResourceRecordSet": {
                "Name": "www.taskactivitytracker.com",
                "Type": "A",
                "AliasTarget": {
                    "HostedZoneId": "Z35SXDOTRQ7X7K",
                    "DNSName": "'"$ALB_DNS"'",
                    "EvaluateTargetHealth": true
                }
            }
        }]
    }'
```

**Note:** `Z35SXDOTRQ7X7K` is the hosted zone ID for ALBs in us-east-1. Adjust if deploying to different region.

### 5.3 Wait for DNS Propagation

```bash
# Test DNS resolution (may take 1-5 minutes)
nslookup taskactivitytracker.com
nslookup www.taskactivitytracker.com

# Or use dig
dig taskactivitytracker.com +short
```

---

## Verification

### Check Application Health

```bash
# 1. Check pods are running
kubectl get pods -n taskactivity

# 2. Check logs
kubectl logs -n taskactivity -l app=taskactivity-app --tail=50

# 3. Check application logs for startup
kubectl logs -n taskactivity deployment/taskactivity-app -f

# Look for:
# - "Started TaskactivityApplication"
# - "S3 storage initialized for bucket: taskactivity-receipts-prod"
# - No error messages about secrets or database connections

# 4. Test health endpoint via port-forward
kubectl port-forward -n taskactivity service/taskactivity-service 8080:8080

# In another terminal:
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# 5. Test via ALB (after DNS propagation)
curl https://taskactivitytracker.com/actuator/health
# Expected: {"status":"UP"}

# 6. Open in browser
open https://taskactivitytracker.com
```

### Verify AWS Integration

```bash
# 1. Check S3 access (upload a test receipt via UI)
aws s3 ls s3://taskactivity-receipts-prod/ --recursive

# 2. Check CloudWatch logs (if configured)
aws logs tail /aws/eks/taskactivity-cluster/cluster --follow

# 3. Check SES sent emails
aws ses get-send-statistics --region us-east-1

# 4. Verify External Secrets are syncing
kubectl describe externalsecret -n taskactivity

# 5. Check IAM role is being used by pods
kubectl describe pod -n taskactivity <pod-name> | grep AWS
```

### Load Testing

```bash
# Test autoscaling with load
kubectl run -it --rm load-generator --image=busybox --restart=Never -- /bin/sh -c \
    "while true; do wget -q -O- https://taskactivitytracker.com; done"

# Watch HPA scale up
kubectl get hpa -n taskactivity -w

# Watch pods scale
kubectl get pods -n taskactivity -w
```

---

## Monitoring

### Prometheus & Grafana (Optional)

```bash
# Install Prometheus stack
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install prometheus prometheus-community/kube-prometheus-stack \
    --namespace monitoring \
    --create-namespace

# Access Grafana
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80

# Default credentials: admin/prom-operator
```

### CloudWatch Container Insights

```bash
# Install CloudWatch agent
curl https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/quickstart/cwagent-fluentd-quickstart.yaml | \
    sed "s/{{cluster_name}}/taskactivity-cluster/;s/{{region_name}}/us-east-1/" | \
    kubectl apply -f -

# Verify
kubectl get pods -n amazon-cloudwatch
```

---

## Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl get pods -n taskactivity

# Describe pod for events
kubectl describe pod -n taskactivity <pod-name>

# Check logs
kubectl logs -n taskactivity <pod-name>

# Common issues:
# - ImagePullBackOff: Check ECR permissions and image exists
# - CrashLoopBackOff: Check application logs for errors
# - Secrets not found: Check External Secrets sync status
```

### External Secrets Not Syncing

```bash
# Check SecretStore
kubectl get secretstore -n taskactivity
kubectl describe secretstore aws-secrets-manager -n taskactivity

# Check ExternalSecret status
kubectl get externalsecrets -n taskactivity
kubectl describe externalsecret taskactivity-db-credentials -n taskactivity

# Check External Secrets Operator logs
kubectl logs -n external-secrets-system deployment/external-secrets

# Common issues:
# - IAM permissions: Verify IRSA role has SecretsManager access
# - Secret not found in AWS: Check secret name matches exactly
# - Region mismatch: Verify region in SecretStore matches where secrets are stored
```

### ALB Not Created

```bash
# Check Ingress status
kubectl describe ingress taskactivity-ingress -n taskactivity

# Check AWS Load Balancer Controller logs
kubectl logs -n kube-system deployment/aws-load-balancer-controller

# Verify controller is running
kubectl get deployment -n kube-system aws-load-balancer-controller

# Common issues:
# - Subnet tags missing: Subnets need kubernetes.io/role/elb=1 tag
# - Security group issues: Check ALB security group allows inbound 80/443
# - IAM permissions: Verify ALB controller has required permissions
```

### Database Connection Issues

```bash
# Test database connectivity from pod
kubectl exec -it -n taskactivity <pod-name> -- /bin/sh

# Inside pod:
nc -zv <rds-endpoint> 5432

# Check database credentials secret
kubectl get secret taskactivity-db-credentials -n taskactivity -o yaml

# Decode and verify credentials
kubectl get secret taskactivity-db-credentials -n taskactivity -o jsonpath='{.data.username}' | base64 -d

# Common issues:
# - Security group: Verify RDS security group allows inbound from EKS pods
# - Secret format: Verify jdbcUrl format is correct
# - Network: Verify RDS is in accessible subnet from EKS
```

### S3 Access Issues

```bash
# Check IAM role is attached to ServiceAccount
kubectl describe serviceaccount taskactivity-service-account -n taskactivity

# Verify pod is using the role
kubectl describe pod -n taskactivity <pod-name> | grep -A 5 AWS_ROLE_ARN

# Check application logs for S3 errors
kubectl logs -n taskactivity <pod-name> | grep -i s3

# Test S3 access from pod
kubectl exec -it -n taskactivity <pod-name> -- /bin/sh
# Install AWS CLI in pod (if needed) and test
```

---

## Cleanup

### Remove Application

```bash
# Delete application resources
kubectl delete -f taskactivity-ingress-aws.yaml
kubectl delete -f taskactivity-deployment-aws.yaml
kubectl delete -f taskactivity-rbac.yaml

# Or delete entire namespace
kubectl delete namespace taskactivity

# This will automatically delete:
# - Deployment
# - Pods
# - Service
# - Ingress (and ALB)
# - ConfigMap
# - Secrets
# - ExternalSecrets
# - SecretStore
# - HPA
# - PDB
```

### Remove Controllers (Optional)

```bash
# Remove AWS Load Balancer Controller
helm uninstall aws-load-balancer-controller -n kube-system

# Remove External Secrets Operator
helm uninstall external-secrets -n external-secrets-system
kubectl delete namespace external-secrets-system
```

### Clean Up AWS Resources

**⚠️ WARNING:** This will delete data!

```bash
# Delete IAM role
aws iam detach-role-policy \
    --role-name taskactivity-eks-pod-role \
    --policy-arn arn:aws:iam::378010131175:policy/TaskActivityEKSPodPolicy

aws iam delete-role --role-name taskactivity-eks-pod-role

# Delete IAM policy
aws iam delete-policy \
    --policy-arn arn:aws:iam::378010131175:policy/TaskActivityEKSPodPolicy

# Note: Do NOT delete RDS, S3, or Secrets Manager unless you're sure!
# These contain production data
```

---

## Cost Optimization

### Right-Size Resources

```bash
# Monitor actual resource usage
kubectl top pods -n taskactivity

# Adjust resource requests/limits in deployment based on actual usage
```

### Use Spot Instances

```bash
# Create node group with spot instances for non-critical workloads
eksctl create nodegroup \
    --cluster=taskactivity-cluster \
    --name=taskactivity-spot \
    --instance-types=t3.medium,t3a.medium \
    --spot \
    --nodes-min=1 \
    --nodes-max=5
```

### Enable Cluster Autoscaler

```bash
# Install cluster autoscaler
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml

# Configure for your cluster
kubectl -n kube-system edit deployment.apps/cluster-autoscaler
# Add: --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/taskactivity-cluster
```

---

## Additional Resources

### AWS Documentation

- [Amazon EKS User Guide](https://docs.aws.amazon.com/eks/latest/userguide/)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)
- [External Secrets Operator](https://external-secrets.io/latest/)
- [IAM Roles for Service Accounts](https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html)

### Project Documentation

- [Main README](../ReadMe.md)
- [Developer Guide](../docs/Developer_Guide.md)
- [AWS Deployment Guide](../aws/AWS_Deployment.md)
- [CloudFormation Guide](../cloudformation/README.md)

---

## Support

For issues or questions:

1. Check application logs: `kubectl logs -n taskactivity -l app=taskactivity-app`
2. Check AWS CloudWatch Logs
3. Review this troubleshooting guide
4. Contact: deanammons@gmail.com

---

**Last Updated:** January 27, 2026
