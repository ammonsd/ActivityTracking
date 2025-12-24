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
    - S3 buckets:
        - `taskactivity-receipts-prod` (expense receipt storage - private)
        - `taskactivity-docs` (public documentation - HTML, PDF files)
        - `taskactivity-logs-archive` (CloudWatch log exports)
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
-   Update `CORS_ALLOWED_ORIGINS` with your domain:
    -   **Wildcard subdomains**: Use `https://*.yourdomain.com` to allow all subdomains
    -   **Specific domains**: Use `https://yourdomain.com,https://www.yourdomain.com`
    -   **Example**: `https://*.taskactivitytracker.com,https://taskactivitytracker.com`
    -   **Note**: The application automatically detects wildcard patterns (`*`) and uses `setAllowedOriginPatterns()` instead of `setAllowedOrigins()`

### Step 2: Create AWS Secrets

> **🔒 CRITICAL SECURITY REQUIREMENTS:**
> 
> The application enforces strict security requirements and **will fail to start** if the following are not properly configured:
> 
> 1. **JWT_SECRET** - Must be set and cannot use the default value. Generate a secure 256-bit key:
>    ```powershell
>    # Generate a secure JWT secret (PowerShell)
>    $jwtSecret = [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
>    Write-Host "JWT_SECRET=$jwtSecret"
>    
>    # Or use OpenSSL (WSL/Linux/Mac)
>    openssl rand -base64 32
>    ```
> 
> 2. **APP_ADMIN_INITIAL_PASSWORD** - Must be set via AWS Secrets Manager. Minimum requirements:
>    - At least 12 characters
>    - Include uppercase, lowercase, number, and special character
>    - Will be forced to change on first login
> 
> 3. **Swagger/OpenAPI** - Disabled in production (AWS profile) for security

**Required Secrets:**

```powershell
# JWT Secret (REQUIRED - CRITICAL)
# Generate a secure 256-bit key using the command above
aws secretsmanager create-secret `
    --name taskactivity/jwt/secret `
    --description "TaskActivity JWT Secret Key" `
    --secret-string '{\"secret\":\"YOUR-GENERATED-BASE64-KEY-HERE\"}'

# Database credentials (REQUIRED)
aws secretsmanager create-secret `
    --name taskactivity/database/credentials `
    --description "TaskActivity Database Credentials" `
    --secret-string '{\"username\":\"admin\",\"password\":\"YourSecurePassword\",\"jdbcUrl\":\"jdbc:postgresql://your-rds-endpoint:5432/AmmoP1DB\"}'

# Admin credentials (REQUIRED)
# Password must be: 12+ chars, uppercase, lowercase, number, special character
aws secretsmanager create-secret `
    --name taskactivity/admin/credentials `
    --description "TaskActivity Admin Credentials" `
    --secret-string '{\"password\":\"YourSecureAdminPassword123!\"}'
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

The application supports email notifications for security events (account lockouts), expense submissions, and expense status changes. Email can be configured using either SMTP (development) or AWS SES (production).

**Option 1: AWS SES (Recommended for Production)**

AWS SES provides reliable, scalable email delivery without requiring SMTP credentials. Configuration is done via environment variables in the task definition:

```json
"environment": [
    {"name": "MAIL_ENABLED", "value": "true"},
    {"name": "MAIL_USE_AWS_SDK", "value": "true"},
    {"name": "MAIL_FROM", "value": "noreply@taskactivitytracker.com"},
    {"name": "ADMIN_EMAIL", "value": "admin@yourdomain.com"},
    {"name": "EXPENSE_APPROVERS", "value": "approver1@yourdomain.com,approver2@yourdomain.com"}
]
```

**AWS SES Requirements:**
1. Verify your sending domain in AWS SES console
2. Add DNS records (DKIM, SPF, DMARC) to your DNS provider (e.g., Cloudflare)
3. Request production access (sandbox mode only allows verified recipients)
4. Add SES permissions to ECS task role:

```json
{
    "Effect": "Allow",
    "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
    ],
    "Resource": "*"
}
```

**Deployment with AWS SES:**

Configure email settings in your `.env` file (recommended) or pass as CLI parameters:

```powershell
# Method 1: Using .env file (recommended)
# Create .env file in project root with:
# MAIL_ENABLED=true
# MAIL_USE_AWS_SDK=true
# MAIL_FROM=noreply@taskactivitytracker.com
# ADMIN_EMAIL=admin@yourdomain.com

.\aws\deploy-aws.ps1

# Method 2: Using CLI parameters (overrides .env)
.\aws\deploy-aws.ps1 -EnableEmail -UseAwsSdk `
  -MailFrom "noreply@taskactivitytracker.com" `
  -AdminEmail "admin@yourdomain.com"
```

**Option 2: SMTP (Development/Testing)**

For development or if you prefer SMTP:

```powershell
# Email SMTP credentials (OPTIONAL - only if using SMTP instead of AWS SES)
aws secretsmanager create-secret `
    --name taskactivity/email/credentials `
    --description "SMTP credentials for email notifications" `
    --secret-string '{\"username\":\"your-email@gmail.com\",\"password\":\"your-app-password\"}' `
    --region us-east-1
```

For Gmail setup:
1. Enable 2-Factor Authentication on your Google account
2. Generate an App Password at https://myaccount.google.com/security
3. Use the 16-character app password in the secret above

**Email Documentation:**
- **[AWS SES Setup Guide](AWS_SES_Setup_Guide.md)** - Complete AWS SES configuration walkthrough
- **[Email Notification Configuration](../localdocs/Email_Notification_Configuration.md)** - General email setup
- **[IAM Permissions Reference](IAM_Permissions_Reference.md)** - Required IAM policies for SES

**Testing Email:**

```powershell
# Monitor email activity in CloudWatch logs
aws logs tail /ecs/taskactivity --filter-pattern "AWS SES" --follow --region us-east-1

# Expected logs:
# - "AWS SES client initialized for region: us-east-1"
# - "Email sent successfully via AWS SES. MessageId: [id]"
```

**Tracking Sent Emails:**

AWS SES doesn't have a "Sent" folder like Gmail, but you can track emails using:

1. **CloudWatch Logs** (already configured) - View all sent emails:
   ```powershell
   aws logs tail /ecs/taskactivity --filter-pattern "email" --follow --region us-east-1
   ```

2. **SES Configuration Set** (optional) - Track delivery status, bounces, complaints:
   ```powershell
   # Run the setup script to enable detailed tracking
   .\aws\setup-ses-tracking.ps1
   
   # Then uncomment in application-aws.properties:
   # spring.mail.properties.mail.ses.configuration-set=taskactivity-emails
   
   # View metrics in CloudWatch Console or via CLI:
   aws cloudwatch list-metrics --namespace AWS/SES --region us-east-1
   ```

3. **SES Sending Statistics** - View aggregate metrics:
   ```powershell
   aws ses get-send-statistics --region us-east-1
   ```

For detailed email tracking setup, see the `setup-ses-tracking.ps1` script in the `aws/` folder.

**Cost:** AWS SES is $0.10 per 1,000 emails (first 62,000/month free with EC2/ECS).

**Notes:**
- Replace `YourSecurePassword` and `YourAdminPassword` with strong passwords
- Replace `your-rds-endpoint` with your actual RDS endpoint (e.g., `taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com`)
- For Cloudflare tunnel setup details, see [CLOUDFLARE_ON_AWS.md](CLOUDFLARE_ON_AWS.md)
- The application will work without Cloudflare secrets if you're using ALB or direct access
- Email notifications are configured in the task definition but require the email credentials secret to function

### Step 3: Create S3 Buckets

**Receipt Storage Bucket:**

```powershell
# Create S3 bucket for expense receipts
aws s3api create-bucket --bucket taskactivity-receipts-prod --region us-east-1

# Enable server-side encryption
aws s3api put-bucket-encryption --bucket taskactivity-receipts-prod --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

# Block all public access (security best practice)
aws s3api put-public-access-block --bucket taskactivity-receipts-prod --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# Optional: Add lifecycle policy for cost optimization (archive receipts after 180 days)
aws s3api put-bucket-lifecycle-configuration --bucket taskactivity-receipts-prod --lifecycle-configuration file://aws/s3-receipts-lifecycle-policy.json
```

**Public Documentation Bucket:**

```powershell
# Create S3 bucket for public documentation
aws s3api create-bucket --bucket taskactivity-docs --region us-east-1

# Enable server-side encryption
aws s3api put-bucket-encryption --bucket taskactivity-docs --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

# Allow public read access via bucket policy (keep ACL blocks enabled)
aws s3api put-public-access-block --bucket taskactivity-docs --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=false,RestrictPublicBuckets=false"

# Add bucket policy for public read access
aws s3api put-bucket-policy --bucket taskactivity-docs --policy '{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "PublicReadGetObject",
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::taskactivity-docs/*"
  }]
}'
```

**Upload documentation files:**

```powershell
# Upload HTML documentation to S3
aws s3 cp docs/User_Guide.html s3://taskactivity-docs/ --content-type "text/html"
aws s3 cp docs/Task_Activity_Mangement_Technology_Stack.html s3://taskactivity-docs/ --content-type "text/html"
aws s3 cp docs/activitytracking.html s3://taskactivity-docs/ --content-type "text/html"

# Upload Word documents with proper content type
aws s3 cp docs/MyDocument.docx s3://taskactivity-docs/ --content-type "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
aws s3 cp docs/MyDocument.doc s3://taskactivity-docs/ --content-type "application/msword"

# Upload Excel documents with proper content type
aws s3 cp docs/MySpreadsheet.xlsx s3://taskactivity-docs/ --content-type "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
aws s3 cp docs/MySpreadsheet.xls s3://taskactivity-docs/ --content-type "application/vnd.ms-excel"

# Upload PDF documents
aws s3 cp docs/MyDocument.pdf s3://taskactivity-docs/ --content-type "application/pdf"
```

**Verify buckets created:**

```powershell
aws s3 ls | findstr taskactivity
```

You should see:
- `taskactivity-logs-archive`
- `taskactivity-receipts-prod`
- `taskactivity-docs`

### Step 4: Create ECR Repository

```powershell
aws ecr create-repository --repository-name taskactivity --region us-east-1
```

### Step 4.1: Configure ECR Lifecycle Policy (Recommended)

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

### Step 5: Deploy

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

### Environment Variables Configuration (.env)

**Recommended:** Store deployment configuration in a `.env` file in the project root. This keeps sensitive information out of version control and command history.

**Create `.env` file:**

```bash
# Email Configuration
MAIL_ENABLED=true
MAIL_USE_AWS_SDK=true
MAIL_FROM=noreply@taskactivitytracker.com
ADMIN_EMAIL=admin@yourdomain.com
```

The deployment script automatically loads these variables at startup using `scripts/set-env-values.ps1`.

**Benefits:**
- ✅ Secure: `.env` is in `.gitignore` (never committed)
- ✅ Convenient: No need to type email parameters on every deployment
- ✅ Flexible: CLI parameters can still override `.env` values

**Usage examples:**

```powershell
# Uses values from .env file
.\aws\deploy-aws.ps1

# Override specific values from command line
.\aws\deploy-aws.ps1 -MailFrom "different@email.com"

# Skip .env file entirely
.\aws\deploy-aws.ps1 -SkipEnvFile -EnableEmail -MailFrom "..." -AdminEmail "..."
```

**Supported environment variables:**
- `MAIL_ENABLED` - Set to `true` to enable email notifications
- `MAIL_USE_AWS_SDK` - Set to `true` to use AWS SES SDK (recommended)
- `MAIL_FROM` - Email sender address (must be verified in AWS SES)
- `ADMIN_EMAIL` - Admin email address for notifications

### Deploy New Version

```powershell
# From project root
.\aws\deploy-aws.ps1 -Environment production
```

This will:

1. Build the Spring Boot application (skipping tests by default)
2. Create Docker image
3. Push to Amazon ECR
4. Register new ECS task definition
5. Update ECS service with new task
6. Wait for deployment to complete
7. Display deployment status

### Deploy with Tests (QA/Production)

Use the `-RunTests` flag to run all tests before building and deploying. This is recommended for QA and production deployments to ensure code quality:

```powershell
# Deploy to production with full test validation
.\aws\deploy-aws.ps1 -Environment production -RunTests
```

**Behavior:**
- Runs all 131 unit tests before building
- Aborts deployment if any tests fail
- Increases deployment time by ~2-3 minutes
- Recommended for QA and production environments

**Without `-RunTests` flag:**
- Tests are skipped for faster deployment
- Suitable for development and quick iterations

### Deploy with No Cache

Use the `-NoCache` flag to force a complete rebuild without using Docker cache. This is useful when:
- Configuration files have changed (e.g., application-aws.properties)
- You need to ensure all layers are rebuilt from scratch
- Troubleshooting deployment issues

```powershell
.\aws\deploy-aws.ps1 -NoCache

# Can be combined with -RunTests
.\aws\deploy-aws.ps1 -Environment production -RunTests -NoCache
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
-   `AWS_REGION` - AWS region for Secrets Manager and S3
-   `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed CORS origins
-   `LOG_LEVEL` - Application logging level (INFO, DEBUG, etc.)
-   `SECURITY_LOG_LEVEL` - Security logging level

### Storage Configuration

-   `STORAGE_TYPE=s3` - Use S3 for receipt storage (AWS profile default)
-   `STORAGE_S3_BUCKET=taskactivity-receipts-prod` - S3 bucket for receipts
-   `STORAGE_S3_REGION=us-east-1` - S3 bucket region

### Security Configuration (from Secrets Manager)

-   `JWT_SECRET` - **REQUIRED** - JWT signing key (minimum 256 bits). Application will fail to start if not set or using default value.

### Database Configuration (from Secrets Manager)

-   `DB_USERNAME` - Database username
-   `DB_PASSWORD` - Database password
-   `DATABASE_URL` - JDBC connection URL

### Admin Configuration (from Secrets Manager)

-   `APP_ADMIN_INITIAL_PASSWORD` - **REQUIRED** - Initial admin password (minimum 12 chars, must include uppercase, lowercase, number, and special character)

### Cloudflare Tunnel Configuration (from Secrets Manager)

-   `CLOUDFLARE_TUNNEL_CREDENTIALS` - Cloudflare tunnel credentials JSON
-   `CLOUDFLARE_TUNNEL_CONFIG` - Cloudflare tunnel configuration YAML

**Note:** The Cloudflare tunnel runs in the same container as the application, providing secure HTTPS access without exposing ports. See [CLOUDFLARE_ON_AWS.md](CLOUDFLARE_ON_AWS.md) for complete setup guide.

## Application Configuration

The application uses `application-aws.properties` which includes:

-   PostgreSQL RDS configuration
-   AWS Secrets Manager integration
-   S3 storage for expense receipts
-   CloudWatch logging
-   Health check endpoints for ALB
-   Optimized connection pooling
-   Production security settings

### Receipt Storage Architecture

The application uses a storage service abstraction:

-   **Development:** `LocalFileStorageService` - Stores files locally
-   **Production (AWS):** `S3StorageService` - Stores files in S3
-   **Configuration:** Automatically switches based on `SPRING_PROFILES_ACTIVE`
-   **File Organization:** `username/YYYY/MM/receipt_id_uuid.ext`
-   **Database:** Stores relative paths for portability
-   **IAM:** Uses ECS task role (no access keys needed)

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

# Monitor email activity (AWS SES)
aws logs filter-log-events --log-group-name /ecs/taskactivity --filter-pattern "AWS SES" --region us-east-1

# Check for email failures
aws logs filter-log-events --log-group-name /ecs/taskactivity --filter-pattern "Error sending email" --region us-east-1
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

### Changes Not Appearing After Deployment

**Problem:** After deploying new code, users still see the old version of the application. This is often caused by Cloudflare CDN caching static files (JavaScript, CSS) even after successful deployment.

**Solution: Enable Cloudflare Development Mode**

1. Login to [Cloudflare Dashboard](https://dash.cloudflare.com)
2. Select your domain (e.g., `taskactivitytracker.com`)
3. Navigate to: **Caching → Configuration**
4. Find the "Development Mode" section
5. Toggle the switch to **ON**
6. Wait 30 seconds for the changes to propagate globally

**Testing the deployment:**
1. Open a **new incognito/private browser window** (Ctrl+Shift+N)
2. Navigate to your application
3. Open Browser DevTools (F12) → Network tab
4. Verify new JavaScript files are loading (check filename hashes)
5. Test the functionality that was changed

**⚠️ IMPORTANT: Disable Development Mode After Testing**

Once you've verified your deployment works correctly:
1. Return to Cloudflare Dashboard → Caching → Configuration
2. Toggle Development Mode back to **OFF**

**Why this is important:**
- Development Mode bypasses ALL Cloudflare caching
- This increases origin server load and bandwidth usage
- Page load times will be slower for users
- CDN costs may increase with cache bypass

Development Mode automatically expires after 3 hours, but it's best practice to disable it manually as soon as testing is complete.

**Alternative: Purge Specific Files (For Production)**

If you need to clear cache for production deployments without affecting all users:

1. Navigate to: Caching → Configuration → Purge Cache
2. Select **Custom Purge**
3. Enter the specific URLs that changed:
   - `https://yourdomain.com/app/main-*.js`
   - `https://yourdomain.com/app/styles-*.css`
4. Click **Purge**

This method is more efficient than Development Mode and only clears specific cached files.

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

## Managing S3 Document Content Types

### Issue: Word/Excel files download as .txt files

When documents are uploaded to S3 without proper Content-Type metadata, they may download with incorrect file extensions. The application now includes fallback logic to detect content types by file extension, but you can also fix the S3 metadata directly.

### Solution 1: Update IAM Permissions (Required)

Your IAM user needs permissions to access the `taskactivity-docs` bucket:

```powershell
# Run the IAM policy update script
.\update-iam-policy.ps1
```

This will update your IAM policy to include:
- `s3:ListBucket` on `taskactivity-docs`
- `s3:GetObject` and `s3:PutObject` on `taskactivity-docs/*`

### Solution 2: Fix Existing Files in S3

After updating IAM permissions, fix content types for existing files:

```powershell
# Fix content types for all documents in S3
.\fix-s3-content-types.ps1
```

This script will:
- Scan all files in the `taskactivity-docs` bucket
- Update Word, Excel, PDF files that have incorrect content types
- Preserve files that already have correct metadata

### Solution 3: Upload New Files with Correct Content Types

When uploading new documents, always specify the content type:

```powershell
# Word documents
aws s3 cp MyDocument.docx s3://taskactivity-docs/ --content-type "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
aws s3 cp MyDocument.doc s3://taskactivity-docs/ --content-type "application/msword"

# Excel documents
aws s3 cp MySpreadsheet.xlsx s3://taskactivity-docs/ --content-type "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
aws s3 cp MySpreadsheet.xls s3://taskactivity-docs/ --content-type "application/vnd.ms-excel"

# PDF documents
aws s3 cp MyDocument.pdf s3://taskactivity-docs/ --content-type "application/pdf"
```

### Application-Level Protection

The `DocumentService` class now includes fallback logic that detects content types by file extension when S3 returns generic types like `text/plain` or `application/octet-stream`. This provides automatic protection even if S3 metadata is incorrect.

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
