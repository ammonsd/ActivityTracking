# Jenkins AWS Deployment Setup Guide

This guide walks through configuring your local Jenkins instance to deploy the Task Activity Management application to AWS.

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [AWS Credentials Setup](#aws-credentials-setup)
4. [AWS Resources Verification](#aws-resources-verification)
5. [Jenkins Configuration](#jenkins-configuration)
6. [First Deployment](#first-deployment)
7. [Troubleshooting](#troubleshooting)

## Overview

Your local Jenkins will:

-   Build the application (Maven + Angular)
-   Create Docker images
-   Push images to AWS ECR (Elastic Container Registry)
-   Deploy to AWS ECS Fargate (serverless containers)
-   Use RDS PostgreSQL for the database
-   Store receipts and documents in S3

**Cost Benefits:** Running Jenkins locally saves $30-50/month compared to hosting Jenkins on EC2, while still providing full CI/CD capabilities.

## Prerequisites

Before starting, ensure you have:

✅ Local Jenkins running (use `.\scripts\Start-Jenkins.ps1`)
✅ AWS Account with billing configured
✅ AWS CLI installed on Windows
✅ Basic understanding of AWS services (ECR, ECS, RDS)

## AWS Credentials Setup

### Step 1: Create IAM User for Jenkins

**Important:** Do NOT use your AWS root account credentials. Create a dedicated IAM user.

1. Open AWS Console → IAM → Users → Create user
2. User name: `jenkins-deployer`
3. **Access type:** Select "Programmatic access" (for AWS CLI/API)
4. Click "Next: Permissions"

### Step 2: Attach IAM Policy

You have two options:

**Option A: Use existing policy file (Recommended)**

1. In IAM, select "Attach policies directly"
2. Click "Create policy"
3. Choose JSON tab
4. Copy contents from `aws/taskactivity-developer-policy.json`
5. Click "Next: Tags" → "Next: Review"
6. Name: `TaskActivityDeveloperPolicy`
7. Click "Create policy"
8. Return to user creation and attach this policy

**Option B: Use AWS Managed Policies (Quick start, broader permissions)**

1. Attach these managed policies:
    - `AmazonEC2ContainerRegistryFullAccess`
    - `AmazonECS_FullAccess`
    - `AmazonRDSFullAccess`
    - `SecretsManagerReadWrite`
    - `CloudWatchLogsFullAccess`

### Step 3: Save Access Keys

1. Click "Create user"
2. **IMPORTANT:** Save the Access Key ID and Secret Access Key
    - You'll only see the secret key once
    - Store them securely (password manager recommended)
    - Format:
        ```
        Access Key ID: AKIAIOSFODNN7EXAMPLE
        Secret Access Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
        ```

### Step 4: Configure AWS CLI

Open PowerShell and configure AWS CLI with your IAM user credentials:

```powershell
# Configure AWS CLI
aws configure

# Enter when prompted:
AWS Access Key ID: [Your Access Key ID]
AWS Secret Access Key: [Your Secret Access Key]
Default region name: us-east-1
Default output format: json
```

Verify configuration:

```powershell
# Should show your IAM user ARN (not root)
aws sts get-caller-identity
```

Expected output:

```json
{
    "UserId": "AIDAIOSFODNN7EXAMPLE",
    "Account": "378010131175",
    "Arn": "arn:aws:iam::378010131175:user/jenkins-deployer"
}
```

### Step 5: Add Credentials to Jenkins

1. Open Jenkins: http://172.27.85.228:8081
2. Navigate to: **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
3. Click **Add Credentials**
4. Configure:
    - **Kind:** AWS Credentials
    - **ID:** `aws-credentials` (exactly this - required by Jenkinsfile)
    - **Description:** AWS Deployment Credentials for Jenkins
    - **Access Key ID:** [Your Access Key ID]
    - **Secret Access Key:** [Your Secret Access Key]
5. Click **Create**

**Verification:** The credential should appear in the list as "AWS Credentials"

## AWS Resources Verification

Your application requires several AWS resources. You can either:

-   **Use CloudFormation** (automated - recommended, see `cloudformation/ReadMe.md`)
-   **Create manually** (follow `aws/AWS_Deployment.md`)

### Required AWS Resources Checklist

Check if these exist in your AWS account (us-east-1 region):

-   [ ] **ECR Repository:** `taskactivity`
-   [ ] **ECS Cluster:** `taskactivity-cluster-dev` (or staging/production)
-   [ ] **RDS Instance:** PostgreSQL database
-   [ ] **VPC:** With public and private subnets
-   [ ] **Security Groups:** For ECS tasks and RDS
-   [ ] **Application Load Balancer:** (Optional, but recommended)
-   [ ] **Secrets Manager:** For database credentials and JWT secret

### Quick Check Script

Run this PowerShell script to verify AWS resources:

```powershell
# Check ECR Repository
Write-Host "Checking ECR Repository..." -ForegroundColor Cyan
aws ecr describe-repositories --repository-names taskactivity --region us-east-1 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ ECR Repository exists" -ForegroundColor Green
} else {
    Write-Host "✗ ECR Repository NOT found" -ForegroundColor Red
    Write-Host "  Create with: aws ecr create-repository --repository-name taskactivity --region us-east-1" -ForegroundColor Yellow
}

# Check ECS Cluster
Write-Host "`nChecking ECS Cluster..." -ForegroundColor Cyan
aws ecs describe-clusters --clusters taskactivity-cluster-dev --region us-east-1 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ ECS Cluster exists" -ForegroundColor Green
} else {
    Write-Host "✗ ECS Cluster NOT found" -ForegroundColor Red
    Write-Host "  Create with: aws ecs create-cluster --cluster-name taskactivity-cluster-dev --region us-east-1" -ForegroundColor Yellow
}

# Check RDS Instances
Write-Host "`nChecking RDS Instances..." -ForegroundColor Cyan
$rdsInstances = aws rds describe-db-instances --region us-east-1 --query 'DBInstances[?Engine==`postgres`].DBInstanceIdentifier' --output text
if ($rdsInstances) {
    Write-Host "✓ PostgreSQL RDS Instance(s) found: $rdsInstances" -ForegroundColor Green
} else {
    Write-Host "✗ No PostgreSQL RDS Instances found" -ForegroundColor Red
}

Write-Host "`nFor complete setup, see: cloudformation/ReadMe.md or aws/AWS_Deployment.md" -ForegroundColor Yellow
```

### Create ECR Repository (If Missing)

If ECR repository doesn't exist, create it:

```powershell
# Create ECR repository
aws ecr create-repository --repository-name taskactivity --region us-east-1

# Set lifecycle policy to auto-delete old images
aws ecr put-lifecycle-policy --repository-name taskactivity --region us-east-1 --lifecycle-policy-text file://aws/ecr-lifecycle-policy.json
```

## Jenkins Configuration

### Verify Jenkins Plugins

Ensure these plugins are installed:

1. Go to: **Manage Jenkins** → **Plugins** → **Installed plugins**
2. Verify these are installed:
    - ✅ Pipeline
    - ✅ Git
    - ✅ Docker Pipeline
    - ✅ Maven Integration
    - ✅ Pipeline Utility Steps

**Missing plugins?** Go to **Available plugins**, search, and install them.

### Verify Tool Configurations

1. Go to: **Manage Jenkins** → **Tools**
2. Verify these exist:

**JDK:**

-   Name: `JDK-21`
-   JAVA_HOME: Auto-installed JDK 21

**Maven:**

-   Name: `Maven-3.9`
-   Version: 3.9.12 or higher (auto-installed)

If missing, add them using the "Add JDK" or "Add Maven" buttons.

### Pipeline Parameter Review

The TaskActivity-Pipeline job has these parameters:

| Parameter             | Options                      | Description                       |
| --------------------- | ---------------------------- | --------------------------------- |
| ENVIRONMENT           | dev, staging, production     | Target AWS environment            |
| DEPLOY_ACTION         | deploy, build-only, rollback | What to do                        |
| SKIP_TESTS            | true/false                   | Skip unit tests (not recommended) |
| NO_CACHE              | true/false                   | Force Docker rebuild              |
| DEPLOY_INFRASTRUCTURE | true/false                   | Run CloudFormation first          |
| INFRASTRUCTURE_ACTION | update, create, preview      | CloudFormation operation          |

**For first deployment:**

-   ENVIRONMENT: `dev`
-   DEPLOY_ACTION: `build-only` (test build first)
-   All others: default values

## First Deployment

### Understanding Build Modes

Jenkins provides a production-grade build pipeline, but you have multiple options for local development and testing:

#### Approach 1: Traditional Development (Rapid Iteration)

**Best for: Daily coding and quick testing**

Use your existing Spring Boot and Docker scripts **without Jenkins**:

-   Run `mvnw spring-boot:run` directly from command line or IDE
-   Use your IDE's launch configurations
-   Fast iteration during active development
-   Direct access to application logs
-   **No containerization** - runs directly on your machine

**When to use:**

-   Actively developing features
-   Quick testing and debugging
-   Don't need to test containerized behavior
-   Want immediate feedback

#### Approach 2: Jenkins "build-only" Mode (Pre-deployment Validation)

**Best for: Testing production-like builds before AWS deployment**

Build through Jenkins but don't deploy to AWS:

-   Produces the **exact same Docker image** that will go to AWS
-   Tests the full build pipeline (Maven, tests, Docker)
-   Verifies production-like configuration and dependencies
-   Image preserved locally for testing
-   **No AWS costs** incurred

**When to use:**

-   Verify the image builds correctly with all dependencies
-   Test the containerized app locally before AWS
-   Validate the full CI/CD pipeline works
-   Ensure your changes will work in production environment

#### Approach 3: Jenkins "deploy" Mode (AWS Deployment)

**Best for: Deploying to AWS environments**

Complete CI/CD pipeline with AWS deployment:

-   Builds Docker image
-   Pushes image to AWS ECR
-   Deploys to AWS ECS Fargate
-   Updates running containers
-   **AWS costs** apply

**When to use:**

-   Ready to deploy to development/staging/production
-   Want to test in actual AWS environment
-   Need to share changes with team or stakeholders

---

### Phase 1: Test Build (Local Only)

1. Open Jenkins: http://172.27.85.228:8081
2. Click **TaskActivity-Pipeline**
3. Click **Build with Parameters**
4. Configure:
    - ENVIRONMENT: `dev`
    - DEPLOY_ACTION: `build-only`
    - SKIP_TESTS: `false`
    - NO_CACHE: `false`
5. Click **Build**

**Expected Results:**

-   ✅ Maven build succeeds
-   ✅ All tests pass (315 tests)
-   ✅ Docker image created locally
-   ✅ Image tagged: `378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity:<build_number>`
-   ✅ Image tagged: `taskactivity:latest`
-   ✅ Images preserved (cleanup skipped in build-only mode)

**If build fails:** Check the console output for errors. Common issues:

-   Node.js version mismatch
-   Maven dependencies
-   Test failures

### Phase 2: Push to ECR

Once build-only succeeds:

1. Click **Build with Parameters**
2. Configure:
    - ENVIRONMENT: `dev`
    - DEPLOY_ACTION: `deploy` (this pushes to ECR but doesn't deploy to ECS yet)
    - SKIP_TESTS: `false`
3. Click **Build**

**Expected Results:**

-   ✅ Build succeeds
-   ✅ Docker image pushed to ECR
-   ⚠️ ECS deployment may fail (expected if ECS resources don't exist yet)

**Verify in AWS Console:**

1. Open AWS Console → ECR → Repositories → taskactivity
2. You should see your image with build number tag
3. Note the image URI: `378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity:<build_number>`

### Phase 3: Complete AWS Infrastructure

Before full deployment works, ensure all AWS resources exist:

**Option A: CloudFormation (Recommended)**

```powershell
# Deploy complete infrastructure
cd cloudformation
# Follow instructions in cloudformation/ReadMe.md
```

**Option B: Manual Setup**
Follow the detailed guide in `aws/AWS_Deployment.md`

### Phase 4: Full Deployment

Once infrastructure exists:

1. Click **Build with Parameters**
2. Configure:
    - ENVIRONMENT: `dev`
    - DEPLOY_ACTION: `deploy`
    - SKIP_TESTS: `false`
3. Click **Build**

**Expected Results:**

-   ✅ Build succeeds
-   ✅ Docker image pushed to ECR
-   ✅ ECS task definition updated
-   ✅ ECS service updated
-   ✅ New containers running in Fargate
-   ✅ Application accessible via Load Balancer URL

**Find your application URL:**

```powershell
# Get the load balancer DNS name
aws elbv2 describe-load-balancers --region us-east-1 --query 'LoadBalancers[?contains(LoadBalancerName, `taskactivity`)].DNSName' --output text
```

Access your application at: `http://<load-balancer-dns>/task-activity`

## Deployment Workflow

### Daily Development Flow

1. Make code changes locally
2. Commit and push to GitHub
3. Run Jenkins build:
    - ENVIRONMENT: `dev`
    - DEPLOY_ACTION: `deploy`
4. Jenkins automatically:
    - Builds application
    - Runs tests
    - Creates Docker image
    - Pushes to ECR
    - Deploys to ECS Fargate

### Multi-Environment Strategy

-   **dev:** Continuous deployment, latest code
-   **staging:** Pre-production testing, requires approval
-   **production:** Stable releases, manual trigger only

Deploy to staging/production:

1. Ensure dev environment is stable
2. Use same build number for consistency
3. Run deployment with ENVIRONMENT: `staging` or `production`

## Monitoring and Logs

### View Application Logs

**Via Jenkins:**

-   Check "ECS Deployment Status" stage output

**Via AWS Console:**

1. AWS Console → CloudWatch → Log Groups
2. Find: `/ecs/taskactivity-dev` (or staging/production)
3. View container logs in real-time

**Via AWS CLI:**

```powershell
# Tail logs
aws logs tail /ecs/taskactivity-dev --follow --region us-east-1
```

### Check Deployment Status

```powershell
# Check ECS service status
aws ecs describe-services --cluster taskactivity-cluster-dev --services taskactivity-service-dev --region us-east-1

# List running tasks
aws ecs list-tasks --cluster taskactivity-cluster-dev --region us-east-1

# Check task definition
aws ecs describe-task-definition --task-definition taskactivity-task-dev --region us-east-1
```

## Cost Management

### Estimated Monthly Costs

**With Local Jenkins (your setup):**

-   ECS Fargate (0.25 vCPU, 0.5GB): ~$12-15/month
-   RDS PostgreSQL (db.t3.micro): ~$15-20/month
-   Application Load Balancer: ~$18/month
-   ECR Storage (5GB): ~$0.50/month
-   Data Transfer: ~$1-5/month

**Total: ~$46-58/month** (dev environment)

**Savings vs Jenkins on EC2:** ~$30-50/month saved

### Cost Optimization Tips

1. **Stop dev environment when not in use:**

    ```powershell
    # Stop ECS service (scales to 0 tasks)
    aws ecs update-service --cluster taskactivity-cluster-dev --service taskactivity-service-dev --desired-count 0 --region us-east-1

    # Stop RDS instance
    aws rds stop-db-instance --db-instance-identifier taskactivity-db-dev --region us-east-1
    ```

2. **Start dev environment:**

    ```powershell
    # Start RDS instance
    aws rds start-db-instance --db-instance-identifier taskactivity-db-dev --region us-east-1

    # Start ECS service
    aws ecs update-service --cluster taskactivity-cluster-dev --service taskactivity-service-dev --desired-count 1 --region us-east-1
    ```

3. **Use ECR lifecycle policies:** Already configured to delete old images

4. **Set up billing alerts:**
    - See: `aws/Cost_Explorer_Setup_Guide.md`
    - Recommended: Alert at $50/month threshold

## Troubleshooting

### Issue: AWS Credentials Error

**Symptom:** Jenkins build fails with "Unable to locate credentials"

**Solution:**

1. Verify credential ID is exactly `aws-credentials` in Jenkins
2. Check AWS CLI works: `aws sts get-caller-identity`
3. Re-enter credentials in Jenkins if needed

### Issue: ECR Push Failed - Authentication Error

**Symptom:** "no basic auth credentials"

**Solution:**

```powershell
# Login to ECR manually to verify credentials
aws ecr get-login-password --region us-east-1 | wsl docker login --username AWS --password-stdin 378010131175.dkr.ecr.us-east-1.amazonaws.com
```

If this fails, check IAM user has `AmazonEC2ContainerRegistryFullAccess` or equivalent.

### Issue: ECS Deployment Timeout

**Symptom:** ECS deployment stage hangs or times out

**Causes:**

1. **Task can't pull image:** Check ECR permissions
2. **Task fails health checks:** Check application logs
3. **No available capacity:** Check service desired count vs cluster capacity

**Solution:**

```powershell
# Check service events
aws ecs describe-services --cluster taskactivity-cluster-dev --services taskactivity-service-dev --region us-east-1 --query 'services[0].events[:5]'

# Check task status
aws ecs describe-tasks --cluster taskactivity-cluster-dev --tasks $(aws ecs list-tasks --cluster taskactivity-cluster-dev --region us-east-1 --query 'taskArns[0]' --output text) --region us-east-1
```

### Issue: Application Not Accessible

**Symptom:** Load balancer URL returns 503 or times out

**Checks:**

1. **Security Groups:** Ensure ECS tasks allow inbound from ALB
2. **Health Checks:** Application must respond at `/actuator/health`
3. **Target Group:** Verify targets are healthy in AWS Console
4. **Database Connection:** Check RDS security group allows ECS tasks

**Debug:**

```powershell
# Check target health
aws elbv2 describe-target-health --target-group-arn <target-group-arn> --region us-east-1

# View application logs
aws logs tail /ecs/taskactivity-dev --follow --region us-east-1
```

### Issue: Database Connection Failed

**Symptom:** Application logs show "Connection refused" or authentication errors

**Solution:**

1. **Security Groups:** RDS security group must allow inbound from ECS security group
2. **Secrets Manager:** Verify DB credentials are correct in Secrets Manager
3. **Database URL:** Check environment variables in task definition
4. **RDS Status:** Ensure RDS instance is running

```powershell
# Check RDS status
aws rds describe-db-instances --db-instance-identifier taskactivity-db-dev --region us-east-1 --query 'DBInstances[0].DBInstanceStatus'

# Check Secrets Manager
aws secretsmanager get-secret-value --secret-id taskactivity/db/dev --region us-east-1
```

### Issue: Build Fails in Jenkins

**Common causes and solutions:**

1. **Maven Build Failed:**

    - Check Java version (must be 21)
    - Check Node.js version (must be v20.11.1)
    - Review console output for specific errors

2. **Tests Failed:**

    - Review test output
    - Check database connectivity (local PostgreSQL)
    - Ensure JWT_SECRET is configured

3. **Docker Build Failed:**
    - Check Docker is running in WSL: `wsl docker ps`
    - Verify jenkins user in docker group

## Next Steps

Once AWS deployment is working:

1. **Set up staging environment:** Repeat setup with ENVIRONMENT=staging
2. **Configure production:** Follow same process for production
3. **Add approval gates:** Require manual approval for production deployments
4. **Set up monitoring:** CloudWatch alarms for critical metrics
5. **Implement blue/green deployments:** Zero-downtime deployments
6. **Add automated rollback:** Configure automatic rollback on errors

## Additional Resources

-   **CloudFormation Automation:** `cloudformation/ReadMe.md`
-   **Manual AWS Setup:** `aws/AWS_Deployment.md`
-   **IAM User Setup:** `aws/IAM_Permissions_Reference.md`
-   **Cost Management:** `aws/Cost_Explorer_Setup_Guide.md`
-   **Local Jenkins Setup:** `jenkins/Local_Jenkins_Setup_Guide.md`

## Support

For issues or questions:

1. Check Jenkins console output for detailed error messages
2. Review CloudWatch logs for application errors
3. Check AWS service status: https://status.aws.amazon.com/
4. Review this guide's troubleshooting section

---

**Last Updated:** January 7, 2026
**Jenkins Version:** 2.x
**AWS Region:** us-east-1 (configurable in Jenkinsfile)
