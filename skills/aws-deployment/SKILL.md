---
name: aws-deployment
description: "Guides AWS ECS deployment workflows including CloudFormation stack management, ECS service updates, RDS configuration, S3 setup, and SES email configuration"
---

# AWS Deployment Skill

This skill helps with AWS ECS deployment and infrastructure management for the ActivityTracking application.

## When to Use

- Deploying to AWS ECS for the first time
- Updating existing ECS deployment
- Troubleshooting AWS infrastructure issues
- Configuring AWS services (RDS, S3, SES)

## Architecture Overview

```
┌─────────────────────────────────────────────┐
│ Internet Gateway                            │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│ Application Load Balancer (ALB)             │
│ - Health checks on /actuator/health         │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│ ECS Fargate Service                         │
│ - Task Definition: 512 CPU / 1024 MB        │
│ - Auto-scaling: 1-3 tasks                   │
└─────────────────┬───────────────────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
┌───────▼─────┐    ┌────────▼────────┐
│ RDS         │    │ S3 Bucket       │
│ PostgreSQL  │    │ (Receipts)      │
│ Multi-AZ    │    │ + Lifecycle     │
└─────────────┘    └─────────────────┘
```

## Deployment Steps

### 1. Pre-Deployment Checklist

- [ ] Docker image built and tested locally
- [ ] Environment variables defined (JWT_SECRET, DB_PASSWORD, etc.)
- [ ] CloudFormation templates reviewed
- [ ] AWS CLI configured with appropriate credentials
- [ ] ECR repository created for Docker images

### 2. Initial Infrastructure Deployment

```powershell
# Using CloudFormation
cd cloudformation
.\scripts\deploy-stack.sh infrastructure-stack templates/infrastructure.yaml parameters/production.json

# Or using PowerShell script
.\aws\deploy-aws.ps1 -Environment production -StackName taskactivity-prod
```

### 3. Database Configuration

```powershell
# Configure RDS endpoint in environment variables
$env:DATABASE_URL = "jdbc:postgresql://rds-endpoint:5432/taskactivity"

# Run schema initialization (first time only)
psql -h rds-endpoint -U dbuser -d taskactivity -f src/main/resources/schema.sql
```

### 4. S3 Receipt Storage Setup

```powershell
# Create S3 bucket with lifecycle policy
aws s3 mb s3://taskactivity-receipts-prod

# Apply lifecycle policy
aws s3api put-bucket-lifecycle-configuration `
    --bucket taskactivity-receipts-prod `
    --lifecycle-configuration file://aws/s3-receipts-lifecycle-policy.json

# Enable encryption
aws s3api put-bucket-encryption `
    --bucket taskactivity-receipts-prod `
    --server-side-encryption-configuration '{
        "Rules": [{
            "ApplyServerSideEncryptionByDefault": {
                "SSEAlgorithm": "AES256"
            }
        }]
    }'
```

### 5. SES Email Configuration

```powershell
# Configure SES
.\aws\configure-ses.ps1 -Region us-east-1 -FromEmail noreply@yourdomain.com

# Verify email addresses (if in sandbox)
.\aws\enable-ses-email.ps1 -EmailAddress user@example.com
```

### 6. Deploy Application

```powershell
# Build and push Docker image to ECR
docker build -t taskactivity:latest .
aws ecr get-login-password | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag taskactivity:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/taskactivity:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/taskactivity:latest

# Update ECS service
aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity-service `
    --force-new-deployment
```

## Environment Variables for AWS

```bash
# Application
JWT_SECRET=<from-secrets-manager>
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Database
DATABASE_URL=jdbc:postgresql://<rds-endpoint>:5432/taskactivity
DB_USERNAME=<from-secrets-manager>
DB_PASSWORD=<from-secrets-manager>

# Storage
storage.type=s3
storage.s3.bucket-name=taskactivity-receipts-prod
storage.s3.region=us-east-1

# Email
email.provider=ses
email.ses.region=us-east-1
email.from=noreply@yourdomain.com

# Admin
APP_ADMIN_INITIAL_PASSWORD=<from-secrets-manager>

# Profile
SPRING_PROFILES_ACTIVE=aws
```

## Common AWS Issues

### Issue: Task keeps restarting

**Causes:**

- Health check failing
- Missing environment variables
- Database connection issues

**Solutions:**

1. Check CloudWatch logs: `/ecs/taskactivity-service`
2. Verify security group rules allow outbound to RDS
3. Test database connection from task
4. Verify all required env vars are set

### Issue: 502 Bad Gateway from ALB

**Causes:**

- Application not responding on correct port
- Health check endpoint failing
- Security group blocking ALB to ECS

**Solutions:**

1. Verify `server.port=8080` in application properties
2. Test `/actuator/health` endpoint
3. Check target group health in ALB console
4. Verify security group rules

### Issue: S3 upload fails

**Causes:**

- Incorrect IAM permissions
- Bucket doesn't exist
- Incorrect region configuration

**Solutions:**

1. Verify ECS task role has S3 permissions
2. Check bucket name in environment variables
3. Verify S3 region matches configuration

## Cost Management

```powershell
# Check current costs
.\aws\check-billing.ps1 -StartDate (Get-Date).AddDays(-30) -EndDate (Get-Date)
```

**Cost Optimization Tips:**

- Use Fargate Spot for non-production
- Right-size RDS instance based on CloudWatch metrics
- Enable S3 lifecycle policies (IA, Glacier)
- Use Reserved Instances for steady-state workloads

## Monitoring

**Key CloudWatch Metrics:**

- ECS: CPUUtilization, MemoryUtilization
- ALB: TargetResponseTime, HealthyHostCount
- RDS: DatabaseConnections, CPUUtilization
- Application: Custom metrics from Actuator

**Alarms to Set:**

- ECS tasks < 1 healthy instance
- RDS CPU > 80%
- ALB 5xx errors > threshold
- Database connections > 80% of max

## Memory Bank References

- Check `ai/devops-practices.md` for AWS infrastructure details
- Check `ai/project-overview.md` for deployment models
- Check `aws/AWS_Deployment.md` for comprehensive deployment guide

## Rollback Procedure

```powershell
# Rollback to previous task definition
aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity-service `
    --task-definition taskactivity-service:<previous-revision>

# Or use CloudFormation rollback
aws cloudformation rollback-stack --stack-name taskactivity-prod
```
