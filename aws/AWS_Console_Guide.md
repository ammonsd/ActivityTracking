# AWS Console Verification Guide

**Date:** October 25, 2025  
**Account ID:** 378010131175  
**Region:** us-east-1

---

## 🔍 What to Check in AWS Console

Here's everything you can verify in the AWS Console before and after deployment:

---

## 1. 🗄️ RDS (Database)

**Navigate to:** Services → RDS → Databases

### What to Check:

**Your Database:**

-   **DB Identifier:** taskactivity-db
-   **Status:** Should show **Available** (green)
-   **Engine:** PostgreSQL 15.14
-   **Endpoint:** taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com
-   **Port:** 5432

### Things to Look At:

1. **Monitoring Tab**

    - CPU Utilization
    - Database Connections
    - Free Storage Space
    - Read/Write IOPS

2. **Configuration Tab**

    - Instance class: db.t3.micro
    - Storage: 20 GB gp3
    - Multi-AZ: No
    - Publicly accessible: No ✓ (secure!)

3. **Connectivity & Security Tab**

    - VPC: vpc-0532ba98b7d5e4e52
    - Security group: sg-08f4bdf0f4619d2e0 (taskactivity-db-sg)
    - Subnet group: default

4. **Backups Tab**

    - Automated backups: Enabled
    - Backup retention: 7 days
    - Latest restore time

5. **Logs & Events Tab**
    - Recent events
    - Error logs (if any)

### Quick Actions:

-   ▶️ Start/Stop database to save costs
-   📸 Take manual snapshot
-   🔧 Modify instance (upgrade/downgrade)

---

## 2. 📦 ECR (Container Registry)

**Navigate to:** Services → Elastic Container Registry → Repositories

### What to Check:

**Your Repository:**

-   **Name:** taskactivity
-   **URI:** 378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity

### Things to Look At:

1. **Images Tab**

    - Image tags (latest, timestamp-based tags)
    - Image size
    - Pushed date/time
    - Vulnerability scan results
    - Should see 5-9 images (lifecycle policy maintains this)

2. **Lifecycle Policy Tab** ✅

    - **Rule 1:** Keep only 5 most recent tagged images
    - **Rule 2:** Delete untagged images older than 1 day
    - Status: Active
    - Evaluates: Daily
    - **Note:** This is different from repository permissions!

3. **Permissions Tab**

    - Repository policies: **Normally empty** ✓
    - You don't need repository policies if using IAM user permissions
    - Only needed for cross-account access or service-specific access
    - **Note:** Don't confuse this with Lifecycle Policy (different tab)

4. **Image Scanning Tab**
    - Scan on push: Enabled ✓
    - Scan results for vulnerabilities

### After First Deployment:

You should see at least 2 images:

-   `latest`
-   `20251013-HHMMSS` (timestamp tag)

---

## 3. 🐳 ECS (Container Service)

**Navigate to:** Services → Elastic Container Service

### Clusters

**Your Cluster:**

-   **Name:** taskactivity-cluster
-   **Status:** ACTIVE

#### What to Check:

1. **Services Tab**

    - Service name: taskactivity-service (after deployment)
    - Desired tasks: 1
    - Running tasks: 1
    - Task definition: taskactivity:X (X is revision number)

2. **Tasks Tab**

    - Task status: RUNNING
    - Task health: HEALTHY
    - Public IP (to access application)
    - Last status changes

3. **Metrics Tab**
    - CPU utilization
    - Memory utilization
    - Network metrics

### Task Definitions

**Navigate to:** ECS → Task Definitions → taskactivity

#### What to Check:

1. **Latest Revision**

    - Status: ACTIVE
    - Launch type: FARGATE
    - CPU: 512 (0.5 vCPU)
    - Memory: 1024 MB (1 GB)

2. **Container Definitions**

    - Container name: taskactivity
    - Image: Your ECR image
    - Port: 8080
    - Environment variables
    - Secrets (from Secrets Manager)

3. **Task Execution IAM Role**

    - ecsTaskExecutionRole

4. **Task IAM Role**
    - ecsTaskRole

### Services (After Deployment)

**Navigate to:** ECS → Clusters → taskactivity-cluster → Services

#### What to Check:

1. **Service Overview**

    - Launch type: FARGATE
    - Desired count: 1
    - Running count: 1
    - Pending count: 0

2. **Deployments Tab**

    - Primary deployment status
    - Task definition revision
    - Rollout state

3. **Events Tab**

    - Recent service events
    - Deployment progress
    - Any error messages

4. **Logs Tab**

    - View application logs
    - Filter by time/severity

5. **Tasks Tab**
    - Click on task ID to see:
        - Public IP address
        - Private IP address
        - Task health
        - Network configuration

---

## 4. 🔐 Secrets Manager

**Navigate to:** Services → Secrets Manager → Secrets

### Your Secrets:

**1. taskactivity/database/credentials**

-   **ARN:** arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/database/credentials-zH7fA0
-   **Type:** Other type of secret

#### What to Check:

-   Click on secret → "Retrieve secret value"
-   Should contain:
    -   `username`: postgres
    -   `password`: TaskActivity2025!SecureDB
    -   `jdbcUrl`: jdbc:postgresql://taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com:5432/AmmoP1DB

**2. taskactivity/admin/credentials**

-   **ARN:** arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/admin/credentials-4nNh6X

#### What to Check:

-   Should contain:
    -   `password`: Admin123!

### Security:

-   ✓ Automatic rotation: Not configured (can enable later)
-   ✓ Encryption: AWS KMS
-   ✓ Resource policy: None (uses IAM roles)

---

## 5. 👤 IAM (Identity & Access Management)

**Navigate to:** Services → IAM

### Roles

**Your Roles:**

**1. ecsTaskExecutionRole**

**Navigate to:** IAM → Roles → ecsTaskExecutionRole

#### What to Check:

-   **Trusted entities:** ecs-tasks.amazonaws.com
-   **Permissions policies:**
    -   AmazonECSTaskExecutionRolePolicy (AWS managed)
    -   CloudWatchLogsFullAccess (AWS managed)
    -   TaskActivitySecretsManagerPolicy (Custom)

**2. ecsTaskRole**

**Navigate to:** IAM → Roles → ecsTaskRole

#### What to Check:

-   **Trusted entities:** ecs-tasks.amazonaws.com
-   **Permissions policies:**
    -   TaskActivitySecretsManagerPolicy (Custom)

### Policies

**Navigate to:** IAM → Policies → Customer managed

**Your Custom Policy:**

-   **Name:** TaskActivitySecretsManagerPolicy
-   **ARN:** arn:aws:iam::378010131175:policy/TaskActivitySecretsManagerPolicy

#### What to Check:

-   Allows `secretsmanager:GetSecretValue` and `secretsmanager:DescribeSecret`
-   For resources: `arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/*`

---

## 6. 🔒 VPC & Security Groups

**Navigate to:** Services → VPC

### Security Groups

**Navigate to:** VPC → Security Groups

**Your Security Groups:**

**1. taskactivity-db-sg (sg-08f4bdf0f4619d2e0)**

#### Inbound Rules:

| Type       | Port | Source               | Description                |
| ---------- | ---- | -------------------- | -------------------------- |
| PostgreSQL | 5432 | sg-03812ec4ea45c473c | Allow ECS tasks to connect |

#### Outbound Rules:

| Type        | Port | Destination | Description      |
| ----------- | ---- | ----------- | ---------------- |
| All traffic | All  | 0.0.0.0/0   | Default outbound |

**2. taskactivity-ecs-sg (sg-03812ec4ea45c473c)**

#### Inbound Rules:

| Type       | Port | Source    | Description               |
| ---------- | ---- | --------- | ------------------------- |
| Custom TCP | 8080 | 0.0.0.0/0 | Allow HTTP traffic to app |

#### Outbound Rules:

| Type        | Port | Destination | Description      |
| ----------- | ---- | ----------- | ---------------- |
| All traffic | All  | 0.0.0.0/0   | Default outbound |

### What to Check:

-   Both security groups should be in VPC: vpc-0532ba98b7d5e4e52
-   Rules are correctly configured for ECS → RDS communication

---

## 7. 📊 CloudWatch (Monitoring & Logs)

**Navigate to:** Services → CloudWatch

### Log Groups

**Navigate to:** CloudWatch → Logs → Log groups

**Your Log Group:**

-   **Name:** /ecs/taskactivity
-   **Retention:** Never expire (can change this to save costs)

#### What to Check:

1. Click on log group
2. See log streams (one per task)
3. Click on latest stream to view application logs
4. Look for:
    - ✓ Application startup messages
    - ✓ "Started TaskActivityApplication" message
    - ✓ Database connection success
    - ⚠️ Any errors or warnings

### Metrics

**Navigate to:** CloudWatch → Metrics → All metrics

#### What to Check:

**ECS Metrics:**

-   Navigate to: ECS → ClusterName → taskactivity-cluster
-   Metrics available:
    -   CPUUtilization
    -   MemoryUtilization

**RDS Metrics:**

-   Navigate to: RDS → Per-Database Metrics → taskactivity-db
-   Metrics available:
    -   CPUUtilization
    -   DatabaseConnections
    -   FreeStorageSpace
    -   ReadIOPS / WriteIOPS
    -   ReadLatency / WriteLatency

### Alarms (Optional)

You can create alarms for:

-   High CPU usage
-   High memory usage
-   Database connection failures
-   Low disk space

---

## 8. �️ S3 (Log Archival Storage)

**Navigate to:** Services → S3 → Buckets

### What to Check:

**Your Bucket:**

-   **Name:** taskactivity-logs-archive
-   **Region:** us-east-1
-   **Purpose:** Long-term CloudWatch log storage

### Things to Look At:

1. **Objects Tab**

    - Folder: `cloudwatch-exports/`
    - Sub-folders organized by date: `YYYY-MM-DD/`
    - Exported log files (`.gz` compressed)

2. **Properties Tab**

    - Bucket Versioning: Disabled
    - Server-side encryption: Enabled
    - Default encryption: Amazon S3 managed keys (SSE-S3)

3. **Management Tab**

    - **Lifecycle rules:** Check for cost optimization policies
        - Rule: Archive old logs
        - Transition to Glacier: After 90 days
        - Transition to Deep Archive: After 365 days
        - Expiration: After 7 years (2555 days)

4. **Permissions Tab**
    - Bucket policy allows CloudWatch Logs service
    - IAM user Dean has read/write access

### Quick Actions:

-   📥 Download log files for local analysis
-   🔍 Search for specific date ranges
-   💰 Review storage costs in different tiers

### Expected Contents:

After Lambda automation is running, you should see:

-   Daily folders: `cloudwatch-exports/2025-10-25/`, `2025-10-26/`, etc.
-   Each folder contains task export folders with gzipped log files
-   Test file: `aws-logs-write-test` (used for bucket permission validation)

---

## 9. ⚡ Lambda (Log Export Automation)

**Navigate to:** Services → Lambda → Functions

### What to Check:

**Your Function:**

-   **Name:** TaskActivityLogExporter
-   **Runtime:** Python 3.12
-   **Status:** Active
-   **Last modified:** Recently (after setup)

### Things to Look At:

1. **Code Tab**

    - Source code should match `aws/lambda-export-logs.py`
    - Handler: `lambda-export-logs.lambda_handler`

2. **Configuration Tab**

    **General configuration:**

    - Memory: 256 MB
    - Timeout: 1 min 0 sec
    - Execution role: TaskActivityLogExportRole

    **Environment variables:**

    - `LOG_GROUP_NAME`: /ecs/taskactivity
    - `S3_BUCKET`: taskactivity-logs-archive
    - `EXPORT_DAYS`: 1

    **Permissions:**

    - Execution role: TaskActivityLogExportRole
    - Resource-based policy: EventBridge can invoke

3. **Monitor Tab**

    - Recent invocations
    - Success/failure metrics
    - Duration metrics
    - Error count

4. **Test Tab**
    - Test event: Can manually trigger export
    - Should see "Succeeded" status
    - Response shows task ID and S3 destination

### Quick Actions:

-   🧪 Test the function manually
-   📊 View CloudWatch Logs for function execution
-   🔧 Modify environment variables (export more/fewer days)

### Expected Behavior:

-   Runs automatically daily at 2:00 AM UTC
-   Exports previous day's logs to S3
-   Completes in 5-15 minutes
-   CloudWatch logs show successful export task creation

---

## 10. ⏰ EventBridge (Scheduler)

**Navigate to:** Services → Amazon EventBridge → Scheduler → Schedules

### What to Check:

**Your Schedule:**

-   **Name:** TaskActivityDailyLogExport
-   **Status:** Enabled
-   **Schedule pattern:** cron(0 2 \* _ ? _)
-   **Target:** Lambda function - TaskActivityLogExporter
-   **Flexible time window:** Off

### Things to Look At:

1. **Schedule Details**

    - Schedule pattern: Daily at 2:00 AM UTC
    - Timezone: UTC
    - State: Enabled ✓

2. **Target Details**

    - Target API: Lambda Invoke
    - Lambda function: TaskActivityLogExporter
    - Retry policy: Default (2 retries)

3. **History** (if available)
    - Last execution time
    - Next scheduled execution
    - Execution status

### Alternative View (EventBridge Rules):

**Navigate to:** EventBridge → Rules (under Events, not Scheduler)

Note: If you created the schedule using the new Scheduler interface, it will appear under "Schedules" not "Rules". Both work the same way.

### Quick Actions:

-   ▶️ Enable/Disable schedule
-   🔧 Modify schedule (change time or frequency)
-   📝 Edit target configuration

### Expected Behavior:

-   Triggers Lambda function daily at 2:00 AM UTC
-   No manual intervention required
-   Runs even when your PC is off

---

## 11. �💰 Billing & Cost Explorer

**Navigate to:** Account menu (top right) → Billing Dashboard

### What to Check:

1. **Current Month Costs**

    - View by service
    - Estimated charges

2. **Cost by Service**

    - RDS: ~$16-20/month
    - ECS/Fargate: ~$15-20/month
    - ECR: ~$1/month
    - Secrets Manager: ~$1/month
    - CloudWatch: ~$1-5/month
    - Lambda: Minimal (~$0-1/month, likely free tier)
    - EventBridge: Minimal (~$0, free tier for standard schedules)
    - S3: ~$1-3/month (depends on log volume and retention)

3. **Free Tier Usage**

    - Check if any services are still in free tier
    - Lambda: 1M free requests/month + 400,000 GB-seconds compute
    - EventBridge: All standard events free
    - S3: 5GB standard storage free for 12 months

4. **Billing Alerts** (Recommended!)
    - Set up budget alerts
    - Get notified when costs exceed threshold

### Create a Budget:

1. Go to: AWS Cost Management → Budgets
2. Create budget
3. Set amount: $50/month (or your preference)
4. Set alerts at 80%, 100%
5. Enter your email

---

## 12. 🎯 Quick Health Check Checklist

### Before Deployment:

-   [ ] RDS database status: **Available**
-   [ ] ECS cluster status: **Active**
-   [ ] ECR repository exists: **taskactivity**
-   [ ] Secrets exist and have values
-   [ ] IAM roles exist with correct permissions
-   [ ] Security groups configured properly
-   [ ] S3 bucket exists: **taskactivity-logs-archive**
-   [ ] Lambda function deployed: **TaskActivityLogExporter**
-   [ ] EventBridge schedule configured: **TaskActivityDailyLogExport**

### After Deployment:

-   [ ] ECR has Docker images
-   [ ] ECS service is **Active**
-   [ ] ECS task is **Running**
-   [ ] Task health check: **Healthy**
-   [ ] CloudWatch logs show application startup
-   [ ] Application accessible at task's public IP
-   [ ] Health endpoint responds: `http://<task-ip>:8080/actuator/health`
-   [ ] Lambda function tested successfully
-   [ ] EventBridge schedule is **Enabled**
-   [ ] Recent log exports visible in S3 bucket

---

## 🔗 Quick Access URLs

Here are direct links you can bookmark (replace us-east-1 if different):

### RDS:

```
https://console.aws.amazon.com/rds/home?region=us-east-1#database:id=taskactivity-db
```

### ECS Cluster:

```
https://console.aws.amazon.com/ecs/v2/clusters/taskactivity-cluster?region=us-east-1
```

### ECR Repository:

```
https://console.aws.amazon.com/ecr/repositories/private/378010131175/taskactivity?region=us-east-1
```

### Secrets Manager:

```
https://console.aws.amazon.com/secretsmanager/listsecrets?region=us-east-1
```

### CloudWatch Logs:

```
https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/$252Fecs$252Ftaskactivity
```

### Lambda Function:

```
https://console.aws.amazon.com/lambda/home?region=us-east-1#/functions/TaskActivityLogExporter
```

### EventBridge Scheduler:

```
https://console.aws.amazon.com/scheduler/home?region=us-east-1#schedules/default/TaskActivityDailyLogExport
```

### S3 Bucket (Log Archives):

```
https://s3.console.aws.amazon.com/s3/buckets/taskactivity-logs-archive?region=us-east-1&tab=objects
```

---

## 💡 Common Things to Monitor

### Daily:

-   ECS task running status
-   Application logs for errors
-   RDS database status
-   Lambda execution success (check EventBridge history or Lambda metrics)

### Weekly:

-   CloudWatch metrics trends
-   Billing dashboard
-   Security group rules (ensure no unauthorized changes)
-   S3 bucket log exports (verify daily folders being created)

### Monthly:

-   Cost analysis
-   Database backup verification
-   Review CloudWatch logs for unusual patterns
-   S3 storage costs and lifecycle transitions

---

## 🆘 What to Look for if Something's Wrong

### Application Won't Start:

1. Check **ECS Task Events** for error messages
2. Check **CloudWatch Logs** for startup errors
3. Verify **Secrets Manager** values are correct
4. Check **Security Groups** allow traffic

### Can't Access Application:

1. Get **Public IP** from ECS task
2. Verify **Security Group** sg-03812ec4ea45c473c allows port 8080
3. Check **Task Status** is "Running"
4. Check **Health Check** status

### Database Connection Errors:

1. Check **RDS Status** is "Available"
2. Verify **Security Group** sg-08f4bdf0f4619d2e0 allows port 5432 from ECS
3. Check **Secrets** have correct database endpoint
4. Verify **ECS tasks** are using security group sg-03812ec4ea45c473c

### Logs Not Exporting to S3:

1. Check **Lambda Function** status and recent invocations
2. Verify **EventBridge Schedule** is Enabled
3. Check **CloudWatch Logs** for Lambda function errors: `/aws/lambda/TaskActivityLogExporter`
4. Verify **IAM Role** TaskActivityLogExportRole has correct permissions
5. Check **S3 Bucket** permissions allow CloudWatch Logs service
6. View export task status: `aws logs describe-export-tasks --region us-east-1`

### High Costs:

1. Check **Billing Dashboard** for breakdown
2. Verify only 1 ECS task is running
3. Check if RDS database can be stopped when not in use
4. Review **CloudWatch Logs** retention settings
5. Review **S3 storage** costs - check lifecycle policies are working

---

## 📱 AWS Console Mobile App

You can also monitor your AWS resources on your phone:

1. Download "AWS Console" app (iOS/Android)
2. Sign in with your credentials
3. View resources on the go
4. Get push notifications for alarms

---

## 🎯 Summary

**What You Should See Right Now:**

-   ✅ RDS: 1 database (Available)
-   ✅ ECS: 1 cluster (Active), 0 services (until you deploy)
-   ✅ ECR: 1 repository (empty until you deploy)
-   ✅ Secrets Manager: 2 secrets
-   ✅ IAM: 2 roles, 1 custom policy
-   ✅ VPC: 2 security groups

**After Deployment:**

-   ✅ ECR: Images appear
-   ✅ ECS: 1 service, 1 running task
-   ✅ CloudWatch: Application logs
-   ✅ Application accessible via task IP

---

**Pro Tip:** Bookmark the RDS, ECS, and CloudWatch Logs pages for quick access! 📌
