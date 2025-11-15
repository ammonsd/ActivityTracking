# AWS Deployment Files

This directory contains all the configuration files and scripts needed to deploy the Task Activity Management application to AWS.

> **💡 Infrastructure Automation Available:** This guide covers manual AWS setup. For automated infrastructure provisioning using AWS CloudFormation (deployment-ready), see the [CloudFormation Guide](../cloudformation/README.md). CloudFormation can automate VPC, RDS, ECS, ECR, ALB, Secrets Manager, IAM roles, and Security Groups setup.

## Files Overview

### Configuration Files

-   **`taskactivity-task-definition.json`** - ECS Fargate task definition with:
    -   Container configuration
    -   Environment variables
    -   AWS Secrets Manager integration
    -   Health checks
    -   CloudWatch logging
-   **`taskactivity-developer-policy.json`** - IAM policy for deployment users with minimal required permissions
-   **`ecr-lifecycle-policy.json`** - ECR lifecycle policy for automatic image cleanup:
    -   Keeps only the 5 most recent tagged images
    -   Deletes untagged images after 1 day
    -   Reduces storage costs automatically

### Deployment Scripts

-   **`deploy-aws.ps1`** - PowerShell deployment script for Windows
-   **`deploy-aws.sh`** - Bash deployment script for Linux/Mac

## Prerequisites

Before deploying to AWS, ensure you have:

1. **AWS IAM User** with appropriate permissions for:

    - ECS (Fargate)
    - ECR (Elastic Container Registry)
    - RDS (PostgreSQL)
    - Secrets Manager
    - Application Load Balancer
    - CloudWatch Logs
    - Route 53 (if using custom domain)

    **Important:** Do not use the AWS root account for deployments. Create an IAM user with the `TaskActivityDeveloperPolicy` or equivalent permissions. See [`../localdocs/IAM_USER_SETUP.md`](../localdocs/IAM_USER_SETUP.md) for setup instructions.

2. **AWS CLI** installed and configured with IAM user credentials:

    ```powershell
    # Configure AWS CLI with your IAM user credentials (not root account)
    aws configure
    
    # Verify you're using an IAM user (not root)
    aws sts get-caller-identity
    # Should show: "arn:aws:iam::ACCOUNT_ID:user/USERNAME"
    ```

3. **Docker** installed and running

4. **(Recommended) AWS ECR Credential Helper** for secure credential storage:

    ```powershell
    # Install in WSL2 (as root)
    wsl -u root -e bash -c "apt-get update && apt-get install -y amazon-ecr-credential-helper"
    
    # Configure Docker to use it (as regular user)
    wsl -e bash -c 'cat > ~/.docker/config.json << "EOF"
    {
      "credHelpers": {
        "378010131175.dkr.ecr.us-east-1.amazonaws.com": "ecr-login",
        "public.ecr.aws": "ecr-login"
      }
    }
    EOF'
    ```
    
    This eliminates the "credentials stored unencrypted" warning and automatically handles ECR authentication.

4. **AWS Resources Created**:
    - ECR repository: `taskactivity`
    - RDS PostgreSQL instance
    - Secrets in AWS Secrets Manager:
        - `taskactivity/database/credentials` (username, password, jdbcUrl)
        - `taskactivity/admin/credentials` (admin password)
        - `taskactivity/cloudflare/tunnel-credentials` (Cloudflare tunnel credentials JSON)
        - `taskactivity/cloudflare/config` (Cloudflare tunnel configuration YAML)
    - ECS Cluster: `taskactivity-cluster`
    - Application Load Balancer (optional but recommended)

## Quick Start

### Step 1: Update Task Definition

Edit `taskactivity-task-definition.json` and replace:

-   `ACCOUNT_ID` - Your AWS account ID
-   `REGION` - Your AWS region (e.g., `us-east-1`)
-   Update CORS_ALLOWED_ORIGINS with your domain

### Step 2: Create AWS Secrets

**Required Secrets:**

```powershell
# Database credentials (REQUIRED)
aws secretsmanager create-secret `
    --name taskactivity/database/credentials `
    --description "TaskActivity Database Credentials" `
    --secret-string '{\"username\":\"admin\",\"password\":\"YourSecurePassword\",\"jdbcUrl\":\"jdbc:postgresql://your-rds-endpoint:5432/AmmoP1DB\"}'

# Admin credentials (REQUIRED)
aws secretsmanager create-secret `
    --name taskactivity/admin/credentials `
    --description "TaskActivity Admin Credentials" `
    --secret-string '{\"password\":\"YourAdminPassword\"}'
```

**Optional Secrets (for Cloudflare Tunnel):**

If you're using Cloudflare Tunnel for secure public access without exposing your server:

```powershell
# Cloudflare tunnel credentials (OPTIONAL - only if using Cloudflare tunnel)
aws secretsmanager create-secret `
    --name taskactivity/cloudflare/tunnel-credentials `
    --description "TaskActivity Cloudflare Tunnel Credentials" `
    --secret-string file://path/to/your/cloudflare-credentials.json

# Cloudflare tunnel configuration (OPTIONAL - only if using Cloudflare tunnel)
aws secretsmanager create-secret `
    --name taskactivity/cloudflare/config `
    --description "TaskActivity Cloudflare Tunnel Configuration" `
    --secret-string file://path/to/your/cloudflare-config.yaml
```

**Optional Secrets (for Email Notifications):**

If you want to receive email alerts when user accounts are locked due to failed login attempts:

```powershell
# Email SMTP credentials (OPTIONAL - only if enabling email notifications)
aws secretsmanager create-secret `
    --name taskactivity/email/credentials `
    --description "SMTP credentials for email notifications" `
    --secret-string '{\"username\":\"your-email@gmail.com\",\"password\":\"your-app-password\"}' `
    --region us-east-1
```

For Gmail setup, you'll need to:
1. Enable 2-Factor Authentication on your Google account
2. Generate an App Password at https://myaccount.google.com/security
3. Use the 16-character app password in the secret above

For detailed email configuration, see [Email Notification Configuration](../localdocs/Email_Notification_Configuration.md).

**Notes:**
- Replace `YourSecurePassword` and `YourAdminPassword` with strong passwords
- Replace `your-rds-endpoint` with your actual RDS endpoint (e.g., `taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com`)
- For Cloudflare tunnel setup details, see [CLOUDFLARE_ON_AWS.md](CLOUDFLARE_ON_AWS.md)
- The application will work without Cloudflare secrets if you're using ALB or direct access
- Email notifications are configured in the task definition but require the email credentials secret to function

### Step 3: Create ECR Repository

```powershell
aws ecr create-repository --repository-name taskactivity --region us-east-1
```

### Step 3.1: Configure ECR Lifecycle Policy (Recommended)

**Apply automated image cleanup to reduce storage costs:**

```powershell
# From project root
aws ecr put-lifecycle-policy --repository-name taskactivity --lifecycle-policy-text file://aws/ecr-lifecycle-policy.json
```

This lifecycle policy will:
- Keep only the 5 most recent tagged images
- Delete untagged images after 1 day
- Evaluate daily to maintain clean repository
- Save ~85% on ECR storage costs

**Verify the policy:**
```powershell
aws ecr get-lifecycle-policy --repository-name taskactivity
```

### Step 4: Deploy

**Important:** Run the deployment script from the project root directory (not from the aws folder).

**Windows (PowerShell):**

```powershell
# From project root: C:\Users\deana\GitHub\ActivityTracking
.\aws\deploy-aws.ps1 -Environment production
```

**Linux/Mac (Bash):**

```bash
# From project root
chmod +x aws/deploy-aws.sh
./aws/deploy-aws.sh production
```

## Deployment Script Features

### Deploy New Version

```powershell
# From project root
.\aws\deploy-aws.ps1 -Environment production
```

This will:

1. Build the Spring Boot application
2. Create Docker image
3. Push to Amazon ECR
4. Register new ECS task definition
5. Update ECS service with new task
6. Wait for deployment to complete
7. Display deployment status

### Deploy with No Cache

Use the `-NoCache` flag to force a complete rebuild without using Docker cache. This is useful when:
- Configuration files have changed (e.g., application-aws.properties)
- You need to ensure all layers are rebuilt from scratch
- Troubleshooting deployment issues

```powershell
.\aws\deploy-aws.ps1 -NoCache
```

**Note:** Building without cache takes longer but ensures all changes are picked up.

### Check Status

```powershell
.\deploy-aws.ps1 -Status
```

Shows current deployment information including:

-   Running task count
-   Task definition version
-   Application URL
-   Health check endpoint

### Rollback

```powershell
.\deploy-aws.ps1 -Rollback
```

Rolls back to the previous task definition version.

## Environment Variables

The following environment variables are configured in the task definition:

### Application Configuration

-   `SPRING_PROFILES_ACTIVE=aws` - Activates AWS profile
-   `AWS_REGION` - AWS region for Secrets Manager
-   `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed CORS origins
-   `LOG_LEVEL` - Application logging level (INFO, DEBUG, etc.)
-   `SECURITY_LOG_LEVEL` - Security logging level

### Database Configuration (from Secrets Manager)

-   `DB_USERNAME` - Database username
-   `DB_PASSWORD` - Database password
-   `DATABASE_URL` - JDBC connection URL

### Admin Configuration (from Secrets Manager)

-   `ADMIN_PASSWORD` - Admin user password

### Cloudflare Tunnel Configuration (from Secrets Manager)

-   `CLOUDFLARE_TUNNEL_CREDENTIALS` - Cloudflare tunnel credentials JSON
-   `CLOUDFLARE_TUNNEL_CONFIG` - Cloudflare tunnel configuration YAML

**Note:** The Cloudflare tunnel runs in the same container as the application, providing secure HTTPS access without exposing ports. See [CLOUDFLARE_ON_AWS.md](CLOUDFLARE_ON_AWS.md) for complete setup guide.

## Application Configuration

The application uses `application-aws.properties` which includes:

-   PostgreSQL RDS configuration
-   AWS Secrets Manager integration
-   CloudWatch logging
-   Health check endpoints for ALB
-   Optimized connection pooling
-   Production security settings

## Monitoring

### CloudWatch Logs

**Log Group**: `/ecs/taskactivity`  
**Retention**: 30 days  
**Region**: `us-east-1`

View application logs in real-time:

```powershell
# Tail live logs
aws logs tail /ecs/taskactivity --follow --region us-east-1

# View logs from last hour
aws logs tail /ecs/taskactivity --since 1h --region us-east-1

# Filter by error messages
aws logs tail /ecs/taskactivity --filter-pattern "ERROR" --follow --region us-east-1

# View logs from specific time range
aws logs tail /ecs/taskactivity --since "2025-10-16T10:00:00" --until "2025-10-16T11:00:00" --region us-east-1
```

**Useful Log Queries:**

```powershell
# Find all errors
aws logs filter-log-events --log-group-name /ecs/taskactivity --filter-pattern "ERROR" --region us-east-1

# Track user logins
aws logs filter-log-events --log-group-name /ecs/taskactivity --filter-pattern "UserService" --region us-east-1

# Monitor application startup
aws logs filter-log-events --log-group-name /ecs/taskactivity --filter-pattern "Started TaskactivityApplication" --region us-east-1

# Verify Cloudflare tunnel is running (if enabled)
aws logs filter-log-events --log-group-name /ecs/taskactivity --filter-pattern "cloudflared" --region us-east-1
```

**Cloudflare Tunnel Verification:**

If Cloudflare tunnel is enabled, check the logs for successful tunnel startup:

```powershell
# Check tunnel connection status
aws logs tail /ecs/taskactivity --filter-pattern "Connection.*registered" --follow --region us-east-1

# Verify tunnel configuration was loaded
aws logs tail /ecs/taskactivity --filter-pattern "cloudflared" --since 10m --region us-east-1
```

You should see messages indicating:
- Cloudflare credentials loaded successfully
- Tunnel configuration applied
- 4 connections registered to Cloudflare edge servers

### S3 Log Archival

For long-term log retention and compliance, logs are automatically exported to S3.

**Bucket**: `taskactivity-logs-archive`  
**Lifecycle Policy**: 
- 0-90 days: S3 Standard
- 90-365 days: Glacier Flexible Retrieval (~84% cost savings)
- 365+ days: Glacier Deep Archive (~96% cost savings)

**Automated Export (Recommended):**

AWS Lambda function automatically exports logs daily at 2:00 AM UTC:
- **Function**: `TaskActivityLogExporter`
- **Schedule**: EventBridge Scheduler (daily)
- **No PC dependency**: Runs entirely in AWS
- **Setup Guide**: See `aws/LAMBDA_CONSOLE_SETUP_GUIDE.md`

**Manual trigger:**
```powershell
aws lambda invoke --function-name TaskActivityLogExporter --region us-east-1 output.json
```

**Manual Export Scripts:**

```powershell
# From project root - export last 7 days
.\scripts\export-logs-to-s3.ps1 -Days 7

# Export specific date range
.\scripts\export-logs-to-s3.ps1 -StartDate "2025-10-01" -EndDate "2025-10-15"
```

**View Exported Logs:**

```powershell
# List all exports
aws s3 ls s3://taskactivity-logs-archive/cloudwatch-exports/ --recursive --region us-east-1

# Download specific date
aws s3 sync s3://taskactivity-logs-archive/cloudwatch-exports/2025-10-25/ ./downloaded-logs/2025-10-25/ --region us-east-1
```

**Helper Scripts:**
- `scripts/export-logs-to-s3.ps1` - Manual CloudWatch log export
- `aws/lambda-export-logs.py` - Lambda function for automated exports
- `aws/setup-lambda-export.ps1` - Lambda automation setup script

**Documentation:**
- **[Lambda Console Setup Guide](../aws/LAMBDA_CONSOLE_SETUP_GUIDE.md)** - Step-by-step Lambda automation setup
- **[Helper Scripts README](HELPER_SCRIPTS_README.md)** - Script usage and examples
- **[CloudWatch Logging Guide](../localdocs/CLOUDWATCH_LOGGING_GUIDE.md)** - Complete CloudWatch setup and usage

### Health Check

Check application health:

```bash
curl https://your-alb-dns/actuator/health
```

### ECS Service Status

```powershell
aws ecs describe-services `
    --cluster taskactivity-cluster `
    --services taskactivity-service
```

## Troubleshooting

### Tasks Won't Start

1. Check CloudWatch logs: `aws logs tail /ecs/taskactivity --follow`
2. Verify secrets exist in Secrets Manager
3. Check IAM roles have correct permissions
4. Verify security groups allow traffic

### Cannot Connect to Database

1. Verify RDS security group allows connections from ECS security group
2. Check DATABASE_URL format in secrets
3. Verify database is running and accessible

### Health Checks Failing

1. Verify `/actuator/health` endpoint is accessible
2. Check application logs for startup errors
3. Increase health check grace period in service configuration

### CORS Errors

1. Update `CORS_ALLOWED_ORIGINS` environment variable
2. Redeploy service to apply changes
3. Verify domain matches exactly (including protocol)

## Security Best Practices

1. **Never commit secrets** - All sensitive data in AWS Secrets Manager
2. **Use HTTPS** - Configure ACM certificate on ALB
3. **Restrict CORS** - Only allow specific production domains
4. **Enable CloudWatch** - Monitor all application logs
5. **Use IAM users with limited permissions** - Never use root account for deployments
6. **Enable MFA** - Require multi-factor authentication for IAM users
7. **Rotate credentials** - Regularly update access keys and database passwords
8. **Review security groups** - Principle of least privilege
9. **Use IAM roles for ECS tasks** - Don't use access keys in containers
10. **RDS Security** - Ensure database is NOT publicly accessible:
    - Set "Publicly accessible" to **No** in RDS settings
    - Database should only be accessible from ECS tasks within VPC
    - Security groups should only allow inbound traffic from ECS security group

### RDS Database Security

**Verify your RDS instance is properly secured:**

```powershell
# Check if database is publicly accessible (should be false)
aws rds describe-db-instances --db-instance-identifier taskactivity-db \
  --query "DBInstances[0].PubliclyAccessible" --output text
```

**Expected output:** `false`

**If true, make it private:**
```powershell
aws rds modify-db-instance --db-instance-identifier taskactivity-db \
  --no-publicly-accessible --apply-immediately
```

### IAM Best Practices

- **For Deployments:** Use an IAM user with the `TaskActivityDeveloperPolicy` (see `taskactivity-developer-policy.json`)
- **For ECS Tasks:** Use IAM roles attached to task definitions (already configured)
- **Never use:** Root account credentials or long-lived access keys in application code
- **Documentation:** See [`../localdocs/IAM_USER_SETUP.md`](../localdocs/IAM_USER_SETUP.md) for complete IAM setup guide

#### Additional IAM Permissions for Operations

In addition to the base `TaskActivityDeveloperPolicy`, you may want to add these permissions for operational tasks:

**RDS Maintenance Operations:**
```json
{
    "Sid": "RDSMaintenanceActions",
    "Effect": "Allow",
    "Action": [
        "rds:ApplyPendingMaintenanceAction",
        "rds:DescribePendingMaintenanceActions",
        "rds:DescribeDBInstances",
        "rds:ModifyDBInstance"
    ],
    "Resource": "arn:aws:rds:REGION:ACCOUNT_ID:db:taskactivity-db"
}
```

**ECR Image Management:**
```json
{
    "Sid": "ECRImageManagement",
    "Effect": "Allow",
    "Action": [
        "ecr:BatchDeleteImage",
        "ecr:ListImages",
        "ecr:DescribeImages",
        "ecr:PutLifecyclePolicy",
        "ecr:GetLifecyclePolicy"
    ],
    "Resource": "arn:aws:ecr:REGION:ACCOUNT_ID:repository/taskactivity"
}
```

These permissions allow:
- Applying RDS OS updates and maintenance actions via CLI
- Managing ECR images and lifecycle policies
- Cleaning up old Docker images to reduce costs

**Example Combined Policy:**

See `localdocs/Production_Operations_Runbook.md` for the complete IAM policy including all operational permissions.

## Cost Optimization

### Compute
-   Use **Fargate Spot** for non-production environments (up to 70% savings)
-   Set appropriate **task CPU/memory** (current: 512 CPU, 1024 MB)
-   Configure **auto-scaling** based on CloudWatch metrics
-   Use **RDS Reserved Instances** for production databases (up to 60% savings)

### Logging & Storage
-   **CloudWatch Logs**: 30-day retention (~$0.03/GB ingested + $0.03/GB stored)
-   **S3 Log Archival**: Export logs for long-term retention
    - S3 Standard (0-90 days): $0.023/GB/month
    - Glacier Flexible (90-365 days): $0.0036/GB/month (~84% savings)
    - Glacier Deep Archive (365+ days): $0.00099/GB/month (~96% savings)
-   **Export selectively**: Export logs weekly or monthly instead of daily
-   **Lifecycle policies**: Automatic transitions to cheaper storage (already configured)

### Example Cost Breakdown (10 GB logs/month)
-   **CloudWatch**: $0.30/month (30-day retention)
-   **S3 Archive**: $2.76/month average (Year 1 with lifecycle policies)
-   **Total Logging**: ~$3.06/month
-   **Without S3 archival**: $0.30/month (logs deleted after 30 days)

**Recommendation**: Enable S3 archival for compliance and troubleshooting, cost is minimal compared to value.

## Additional Resources

-   [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
-   [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
-   [Spring Boot on AWS](https://spring.io/guides/gs/spring-boot-aws/)
-   [Task Activity AWS Deployment Analysis](../AWS_DEPLOYMENT_ANALYSIS.md)

## Support

For deployment issues:

1. Check CloudWatch logs
2. Review AWS_DEPLOYMENT_ANALYSIS.md
3. Verify all prerequisites are met
4. Check AWS service health dashboard
