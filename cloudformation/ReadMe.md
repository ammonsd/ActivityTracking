# CloudFormation Infrastructure Guide

**Task Activity Tracking Application**  
**Date:** October 28, 2025  
**Status:** Deployment-Ready

---

## Overview

This directory contains complete AWS CloudFormation infrastructure-as-code templates and deployment scripts for the Task Activity Tracking application. CloudFormation automates the provisioning and management of all AWS resources needed to run the application.

## What's Included

### 📁 Directory Structure

```
cloudformation/
├── templates/
│   └── infrastructure.yaml      # Main CloudFormation template (complete stack)
├── parameters/
│   ├── dev.json                 # Development environment parameters
│   ├── staging.json             # Staging environment parameters
│   └── production.json          # Production environment parameters
├── scripts/
│   ├── deploy-infrastructure.ps1  # PowerShell deployment script (Windows)
│   └── deploy-infrastructure.sh   # Bash deployment script (Linux/Mac)
└── README.md                      # This file
```

### 🏗️ Infrastructure Resources

The CloudFormation template creates:

#### Networking

-   **VPC** - Isolated virtual network (10.0.0.0/16)
-   **Public Subnets** (2) - For load balancers and NAT gateway
-   **Private Subnets** (2) - For ECS tasks and RDS database
-   **Internet Gateway** - Public internet access
-   **NAT Gateway** - Private subnet internet access
-   **Route Tables** - Network routing configuration

#### Database

-   **RDS PostgreSQL** - Managed database (version 15.14)
-   **DB Subnet Group** - Multi-AZ subnet placement
-   **Automated Backups** - 7-day retention (production), 1-day (dev)
-   **Encryption** - AWS KMS encryption at rest
-   **CloudWatch Logs** - PostgreSQL log exports

#### Container Infrastructure

-   **ECR Repository** - Docker image registry
-   **Lifecycle Policy** - Automatic image cleanup
-   **ECS Fargate Cluster** - Serverless container orchestration
-   **ECS Service** - Application service with auto-scaling
-   **Task Definition** - Container configuration

#### Load Balancing

-   **Application Load Balancer** - HTTP/HTTPS traffic distribution
-   **Target Group** - Health checks and routing
-   **Listener** - HTTP listener (port 80)

#### Security

-   **Security Groups** - Firewall rules (ALB, ECS, RDS)
-   **Secrets Manager** - Encrypted credential storage
    -   Database credentials
    -   Admin password
    -   Cloudflare tunnel credentials (optional)
-   **IAM Roles** - Task execution and task roles
-   **IAM Policies** - Least-privilege permissions

#### Monitoring

-   **CloudWatch Log Groups** - Application and database logs
-   **Container Insights** - ECS monitoring (production)
-   **Log Retention** - 30 days (production), 7 days (dev)

---

## Prerequisites

### Required

-   **AWS Account** with appropriate permissions
-   **AWS CLI** installed and configured
-   **IAM User** with CloudFormation permissions (not root account)
-   **Git** repository cloned locally

### AWS Permissions Required

Your IAM user needs permissions for:

-   CloudFormation (create/update/delete stacks)
-   VPC (create/manage networking)
-   RDS (create/manage databases)
-   ECS (create/manage clusters/services)
-   ECR (create repositories)
-   Secrets Manager (create/read secrets)
-   IAM (create roles and policies)
-   CloudWatch Logs (create log groups)
-   EC2 (for security groups, NAT gateway)
-   Elastic Load Balancing (create ALBs)

### Configure AWS CLI

```powershell
# Configure credentials (use IAM user, not root)
aws configure

# Verify configuration
aws sts get-caller-identity
# Should show: "arn:aws:iam::ACCOUNT_ID:user/USERNAME"
```

---

## Quick Start

### 1. Update Parameters

Edit the appropriate parameters file for your environment:

```powershell
# Edit parameters
notepad cloudformation\parameters\dev.json
```

**Required Updates:**

-   `DBPassword` - Change from `CHANGE_ME_DEV_PASSWORD`
-   `AdminPassword` - Change from `CHANGE_ME_ADMIN_PASSWORD`

**Optional Updates:**

-   `DBInstanceClass` - Adjust database instance size
-   `TaskCPU` / `TaskMemory` - Adjust container resources
-   `DesiredCount` - Number of ECS tasks to run

### 2. Validate Template

```powershell
# Windows PowerShell
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action validate

# Linux/Mac Bash
./cloudformation/scripts/deploy-infrastructure.sh dev validate
```

### 3. Create Infrastructure

```powershell
# Windows PowerShell
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action create

# Linux/Mac Bash
./cloudformation/scripts/deploy-infrastructure.sh dev create
```

**⏱️ Duration:** 10-15 minutes for complete stack creation

### 4. Check Status

```powershell
# Windows PowerShell
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action status

# Linux/Mac Bash
./cloudformation/scripts/deploy-infrastructure.sh dev status
```

---

## Usage Guide

### Manual Deployment (Standalone)

#### Create New Infrastructure

```powershell
# Development environment
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action create

# Production environment (requires confirmation)
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment production -Action create
```

#### Update Existing Infrastructure

```powershell
# Preview changes first (recommended)
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action preview

# Apply updates
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action update
```

#### Delete Infrastructure

```powershell
# Delete development infrastructure
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action delete

# Production requires typing "delete production" to confirm
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment production -Action delete
```

### Jenkins Integration

The Jenkins pipeline includes optional infrastructure deployment:

#### Pipeline Parameters

When running a Jenkins build:

1. Set `DEPLOY_INFRASTRUCTURE` = `true`
2. Choose `INFRASTRUCTURE_ACTION`:
    - `create` - Create new stack
    - `update` - Update existing stack
    - `preview` - Preview changes only

#### Example Jenkins Build

```groovy
// Jenkins pipeline will:
1. Deploy/update infrastructure (if DEPLOY_INFRASTRUCTURE=true)
2. Build application
3. Build Docker image
4. Push to ECR
5. Deploy to ECS
```

### Direct AWS CLI (Advanced)

```bash
# Create stack
aws cloudformation create-stack \
  --stack-name taskactivity-dev \
  --template-body file://cloudformation/templates/infrastructure.yaml \
  --parameters file://cloudformation/parameters/dev.json \
  --capabilities CAPABILITY_NAMED_IAM \
  --region us-east-1

# Update stack
aws cloudformation update-stack \
  --stack-name taskactivity-dev \
  --template-body file://cloudformation/templates/infrastructure.yaml \
  --parameters file://cloudformation/parameters/dev.json \
  --capabilities CAPABILITY_NAMED_IAM

# Delete stack
aws cloudformation delete-stack \
  --stack-name taskactivity-dev \
  --region us-east-1
```

---

## Environment-Specific Configurations

### Development Environment

**Purpose:** Day-to-day development and testing  
**Stack Name:** `taskactivity-dev`  
**VPC CIDR:** `10.0.0.0/16`

**Characteristics:**

-   Minimal resource allocation (cost-optimized)
-   Single-AZ RDS deployment
-   1-day backup retention
-   Small instance sizes (db.t3.micro, 256 CPU, 512 MB)
-   Can be torn down/recreated frequently

**Use Cases:**

-   Feature development
-   Integration testing
-   Learning and experimentation

### Staging Environment

**Purpose:** Pre-production testing and validation  
**Stack Name:** `taskactivity-staging`  
**VPC CIDR:** `10.1.0.0/16`

**Characteristics:**

-   Production-like configuration
-   Single-AZ RDS (for cost)
-   7-day backup retention
-   Medium instance sizes (db.t3.small, 512 CPU, 1024 MB)
-   Stable but can be recreated

**Use Cases:**

-   UAT (User Acceptance Testing)
-   Performance testing
-   Client demos
-   Production deployment rehearsal

### Production Environment

**Purpose:** Live application serving end users  
**Stack Name:** `taskactivity-production`  
**VPC CIDR:** `10.2.0.0/16`

**Characteristics:**

-   High availability configuration
-   **Multi-AZ RDS deployment** (automatic failover)
-   7-day backup retention
-   Larger instance sizes (db.t3.small+, 512+ CPU, 1024+ MB)
-   2+ ECS tasks for redundancy
-   Deletion protection enabled
-   Container Insights enabled
-   30-day log retention

**Use Cases:**

-   Live production workloads
-   Serving actual users
-   24/7 availability requirement

---

## Stack Outputs

After successful deployment, the stack provides these outputs:

| Output             | Description             | Example                                                        |
| ------------------ | ----------------------- | -------------------------------------------------------------- |
| `VPCId`            | VPC identifier          | vpc-0abc123...                                                 |
| `DBEndpoint`       | RDS database endpoint   | taskactivity-dev-db.xxx.rds.amazonaws.com                      |
| `DBPort`           | Database port           | 5432                                                           |
| `ECRRepositoryURI` | Docker image repository | 123456789.dkr.ecr.us-east-1.amazonaws.com/taskactivity-dev     |
| `LoadBalancerDNS`  | ALB DNS name            | taskactivity-dev-alb-123456.us-east-1.elb.amazonaws.com        |
| `LoadBalancerURL`  | Application URL         | http://taskactivity-dev-alb-123456.us-east-1.elb.amazonaws.com |
| `ECSClusterName`   | ECS cluster name        | taskactivity-dev-cluster                                       |
| `ECSServiceName`   | ECS service name        | taskactivity-dev-service                                       |

**Access Application:**

```powershell
# Get Load Balancer URL
aws cloudformation describe-stacks \
  --stack-name taskactivity-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' \
  --output text
```

---

## Cost Estimation

### Development Environment (Minimal)

-   **RDS db.t3.micro:** ~$15/month
-   **NAT Gateway:** ~$33/month
-   **ECS Fargate (256/512):** ~$10/month
-   **ALB:** ~$16/month
-   **Data Transfer:** ~$5/month
-   **CloudWatch Logs:** ~$2/month
-   **Total:** ~$81/month

### Production Environment (HA)

-   **RDS db.t3.small (Multi-AZ):** ~$60/month
-   **NAT Gateway:** ~$33/month
-   **ECS Fargate (512/1024 x2):** ~$40/month
-   **ALB:** ~$16/month
-   **Data Transfer:** ~$15/month
-   **CloudWatch Logs:** ~$5/month
-   **Total:** ~$169/month

**Cost Savings Tips:**

-   Delete dev/staging when not in use
-   Use scheduled Lambda to stop RDS at night
-   Enable RDS autoscaling for storage
-   Use S3 for log archival

---

## Common Operations

### Update Database Password

```powershell
# 1. Update Secrets Manager
aws secretsmanager update-secret \
  --secret-id taskactivity/dev/database/credentials \
  --secret-string '{"username":"postgres","password":"NewPassword123!","jdbcUrl":"..."}'

# 2. Force new ECS deployment to pick up new secret
aws ecs update-service \
  --cluster taskactivity-dev-cluster \
  --service taskactivity-dev-service \
  --force-new-deployment
```

### Scale ECS Tasks

```powershell
# Update parameters file
# Change DesiredCount from 1 to 2

# Update stack
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action update
```

### Upgrade RDS Instance

```powershell
# Update parameters file
# Change DBInstanceClass from db.t3.micro to db.t3.small

# Preview changes
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action preview

# Apply update (will cause brief downtime for RDS)
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action update
```

### View Logs

```powershell
# View ECS logs
aws logs tail /ecs/taskactivity-dev --follow

# View RDS logs
aws logs tail /aws/rds/instance/taskactivity-dev-db/postgresql --follow
```

---

## Troubleshooting

### Stack Creation Failed

```powershell
# 1. Check stack events
aws cloudformation describe-stack-events \
  --stack-name taskactivity-dev \
  --max-items 20

# 2. Common issues:
# - Insufficient IAM permissions → Add required policies
# - Resource limits exceeded → Request limit increase
# - Invalid parameters → Check parameters file

# 3. Delete failed stack and retry
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action delete
.\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action create
```

### Stack Update Stuck

```powershell
# Check current status
aws cloudformation describe-stacks \
  --stack-name taskactivity-dev \
  --query 'Stacks[0].StackStatus'

# If stuck, may need to cancel update
aws cloudformation cancel-update-stack --stack-name taskactivity-dev
```

### Template Validation Errors

```powershell
# Validate template
aws cloudformation validate-template \
  --template-body file://cloudformation/templates/infrastructure.yaml

# Common issues:
# - YAML syntax errors → Check indentation
# - Invalid resource properties → Check AWS documentation
# - Missing required parameters → Add to parameters file
```

### Database Connection Issues

```powershell
# 1. Check security groups
aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=taskactivity-dev-rds-sg"

# 2. Verify ECS tasks can reach RDS
# - Both should be in same VPC
# - RDS security group should allow port 5432 from ECS security group

# 3. Check RDS endpoint
aws rds describe-db-instances \
  --db-instance-identifier taskactivity-dev-db \
  --query 'DBInstances[0].Endpoint'
```

---

## Best Practices

### Version Control

-   ✅ Commit all template and parameter changes to Git
-   ✅ Use pull requests for production infrastructure changes
-   ✅ Tag releases with infrastructure versions

### Security

-   ✅ Never commit passwords in parameter files
-   ✅ Use Secrets Manager for all sensitive data
-   ✅ Enable MFA for production stack modifications
-   ✅ Review security group rules regularly
-   ✅ Enable RDS encryption (done automatically)

### Operations

-   ✅ Always preview changes before applying
-   ✅ Test infrastructure changes in dev first
-   ✅ Schedule production updates during maintenance windows
-   ✅ Create RDS snapshots before major changes
-   ✅ Document custom modifications

### Cost Management

-   ✅ Delete unused dev/staging stacks
-   ✅ Right-size instances based on actual usage
-   ✅ Enable AWS Cost Explorer
-   ✅ Set up billing alerts
-   ✅ Use AWS Compute Optimizer recommendations

---

## Migration from Manual Setup

If you manually created AWS resources and want to migrate to CloudFormation:

### Option 1: Import Existing Resources (Recommended)

CloudFormation supports importing existing resources. See AWS documentation for details.

### Option 2: Create Parallel Environment

1. Create new CloudFormation stack (dev environment)
2. Test thoroughly
3. Migrate data from manual RDS to CloudFormation RDS
4. Update DNS/load balancer to point to new infrastructure
5. Delete manual resources after verification

### Option 3: Start Fresh

1. Export data from existing database
2. Delete manual infrastructure
3. Create CloudFormation stack
4. Import data to new database
5. Deploy application

---

## Support and Resources

### Internal Documentation

-   [AWS Deployment Guide](../docs/AWS_Deployment.md)
-   [Developer Guide](../docs/Developer_Guide.md)
-   [Jenkins CI/CD Guide](../jenkins/README.md)

### AWS CloudFormation Documentation

-   [Template Reference](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-reference.html)
-   [Best Practices](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/best-practices.html)
-   [Resource Types](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html)

### AWS Service Documentation

-   [Amazon ECS](https://docs.aws.amazon.com/ecs/)
-   [Amazon RDS](https://docs.aws.amazon.com/rds/)
-   [Amazon VPC](https://docs.aws.amazon.com/vpc/)
-   [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)

---

## Changelog

### Version 1.0.0 (October 28, 2025)

-   Initial CloudFormation infrastructure template
-   Support for dev/staging/production environments
-   PowerShell and Bash deployment scripts
-   Jenkins pipeline integration
-   Complete documentation

---

**Author:** Dean Ammons  
**Project:** Task Activity Tracking  
**Status:** Production-Ready
