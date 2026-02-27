# AWS Administrator Guide — TaskActivity Application

**Author:** Dean Ammons  
**Date:** February 2026  
**AWS Account ID:** 378010131175  
**AWS Region:** us-east-1  
**Application:** TaskActivity & Expense Management System

> This is the single reference document for all AWS administration tasks.  
> Keep it updated as the infrastructure evolves.

---

## Table of Contents

1. [Infrastructure Inventory](#1-infrastructure-inventory)
2. [IAM — Users, Roles, and Policies](#2-iam--users-roles-and-policies)
3. [Developer Onboarding / Offboarding](#3-developer-onboarding--offboarding)
4. [RDS — PostgreSQL Database](#4-rds--postgresql-database)
5. [ECS — Container Service](#5-ecs--container-service)
6. [ECR — Container Registry](#6-ecr--container-registry)
7. [S3 — Storage Buckets](#7-s3--storage-buckets)
8. [Secrets Manager](#8-secrets-manager)
9. [SES — Email Service](#9-ses--email-service)
10. [CloudWatch — Monitoring and Logging](#10-cloudwatch--monitoring-and-logging)
11. [VPC and Security Groups](#11-vpc-and-security-groups)
12. [Database Access — pgAdmin 4 and psql](#12-database-access--pgadmin-4-and-psql)
13. [Environment Variable Updates](#13-environment-variable-updates)
14. [Deployment Operations](#14-deployment-operations)
15. [Cost Management](#15-cost-management)
16. [Security Best Practices](#16-security-best-practices)
17. [Troubleshooting](#17-troubleshooting)
18. [Quick Reference Card](#18-quick-reference-card)

---

## 1. Infrastructure Inventory

Complete inventory of all AWS resources for the TaskActivity application.

### Account

| Item           | Value                       |
| -------------- | --------------------------- |
| Account ID     | `378010131175`              |
| Primary Region | `us-east-1`                 |
| Root email     | _(stored securely offline)_ |

### RDS Database

| Item                | Value                                                      |
| ------------------- | ---------------------------------------------------------- |
| Identifier          | `taskactivity-db`                                          |
| Endpoint            | `taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com` |
| Port                | `5432`                                                     |
| Engine              | PostgreSQL 15.14                                           |
| Database name       | `AmmoP1DB`                                                 |
| Instance class      | `db.t3.micro`                                              |
| Storage             | 20 GB gp3                                                  |
| Publicly accessible | **No**                                                     |
| Multi-AZ            | No                                                         |
| Encryption          | ✅ AWS KMS                                                 |
| Automated backups   | ✅ 7-day retention                                         |
| Backup window       | 03:00–04:00 UTC                                            |
| Maintenance window  | Monday 04:00–05:00 UTC                                     |
| Security group      | `sg-08f4bdf0f4619d2e0` (taskactivity-db-sg)                |

### ECS

| Item           | Value                  |
| -------------- | ---------------------- |
| Cluster        | `taskactivity-cluster` |
| Service        | `taskactivity-service` |
| Launch type    | FARGATE                |
| Task CPU       | 512 (0.5 vCPU)         |
| Task memory    | 1024 MB                |
| Container port | 8080                   |
| Container name | `taskactivity`         |
| Log group      | `/ecs/taskactivity`    |

### ECR

| Item             | Value                                                         |
| ---------------- | ------------------------------------------------------------- |
| Repository       | `taskactivity`                                                |
| URI              | `378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity`   |
| Lifecycle policy | Keep 5 most recent tagged images; delete untagged after 1 day |

### S3 Buckets

| Bucket                       | Purpose                                       | Access                                  |
| ---------------------------- | --------------------------------------------- | --------------------------------------- |
| `taskactivity-receipts-prod` | Expense receipt file storage                  | Private — ECS task role only            |
| `taskactivity-docs`          | Public documentation (HTML, PDF, Word, Excel) | Public read                             |
| `taskactivity-logs-archive`  | Long-term CloudWatch log exports              | Private — IAM user + CloudWatch service |

### Secrets Manager

| Secret Name                                  | Purpose                                      |
| -------------------------------------------- | -------------------------------------------- |
| `taskactivity/database/credentials`          | postgres master username, password, JDBC URL |
| `taskactivity/admin/credentials`             | Application admin portal password            |
| `taskactivity/jwt/secret`                    | JWT signing key                              |
| `taskactivity/cloudflare/tunnel-credentials` | Cloudflare tunnel JSON (if active)           |
| `taskactivity/cloudflare/config`             | Cloudflare tunnel YAML (if active)           |

### VPC / Networking

| Item               | Value                                        |
| ------------------ | -------------------------------------------- |
| VPC                | `vpc-0532ba98b7d5e4e52` (Default VPC)        |
| ECS security group | `sg-03812ec4ea45c473c` (taskactivity-ecs-sg) |
| RDS security group | `sg-08f4bdf0f4619d2e0` (taskactivity-db-sg)  |

### IAM Roles

| Role                   | Purpose                                                            |
| ---------------------- | ------------------------------------------------------------------ |
| `ecsTaskExecutionRole` | Pulls ECR images, reads Secrets Manager, writes CloudWatch logs    |
| `ecsTaskRole`          | Application runtime role — S3 receipts access, SES send permission |

---

## 2. IAM — Users, Roles, and Policies

### Developer IAM User

| Item        | Value                                                          |
| ----------- | -------------------------------------------------------------- |
| Username    | `Dean`                                                         |
| Policy      | `TaskActivityDeveloperPolicy` (Customer managed)               |
| Policy ARN  | `arn:aws:iam::378010131175:policy/TaskActivityDeveloperPolicy` |
| Source file | `aws/taskactivity-developer-policy.json`                       |

### TaskActivityDeveloperPolicy — Permission Categories

| Sid                       | Services        | Purpose                                   |
| ------------------------- | --------------- | ----------------------------------------- |
| `ECRAccess`               | ECR             | Push/pull Docker images                   |
| `ECSAccess`               | ECS             | Deploy, manage tasks, ECS Exec            |
| `RDSAccess`               | RDS             | Describe DB instances (status checks)     |
| `SecretsManagerAccess`    | Secrets Manager | Read/write all `taskactivity/*` secrets   |
| `CloudWatchLogsAccess`    | CloudWatch Logs | View and export application logs          |
| `IAMPassRole`             | IAM             | Assign roles to ECS tasks                 |
| `LoadBalancerAccess`      | ELB             | Describe load balancers                   |
| `VPCAccess`               | EC2             | Describe VPC, subnets, security groups    |
| `STSAccess`               | STS             | Get caller identity                       |
| `SSMDeployStateAccess`    | SSM Parameters  | Read/write `taskactivity/*` parameters    |
| `SSMSessionAccess`        | SSM Sessions    | Port-forwarding tunnel and ECS Exec shell |
| `Route53Access`           | Route 53        | DNS management                            |
| `SESAccess`               | SES             | Email configuration and sending           |
| `CloudWatchMetricsAccess` | CloudWatch      | Read/write metrics                        |
| `S3ReceiptsBucketAccess`  | S3              | Access receipts and logs-archive buckets  |
| `S3DocsObjectAccess`      | S3              | Access docs bucket                        |

> **Security note:** `SecretsManagerAccess` uses `Resource: "*"` which allows the `Dean` user to
> retrieve the `postgres` master password from Secrets Manager. This is acceptable while Dean is
> the sole developer. When additional developers are onboarded, create a scoped-down policy that
> excludes `secretsmanager:GetSecretValue` on `taskactivity/database/credentials`.

### ECS Roles and Custom Policies

| Policy                              | Attached To                           | Source File                                  |
| ----------------------------------- | ------------------------------------- | -------------------------------------------- |
| `TaskActivitySecretsManagerPolicy`  | `ecsTaskExecutionRole`, `ecsTaskRole` | Inline in task definition                    |
| `TaskActivityS3ReceiptsPolicy`      | `ecsTaskRole`                         | `aws/taskactivity-s3-receipts-policy.json`   |
| `TaskActivityS3DocsPolicy`          | `ecsTaskRole`                         | `aws/taskactivity-s3-docs-policy.json`       |
| `taskactivity-ecs-task-role-policy` | `ecsTaskRole`                         | `aws/taskactivity-ecs-task-role-policy.json` |

---

## 3. Developer Onboarding / Offboarding

### Onboarding a New Developer

#### Step 1 — Create IAM User in AWS Console

1. **IAM → Users → Create user**
2. Username: _(developer name, e.g., `JaneDoe`)_
3. Select **"Provide user access to the AWS Management Console"** if console access is needed
4. Permissions: **Attach policies directly**
5. Attach `TaskActivityDeveloperPolicy`

> **Important:** New developers should get a **scoped-down version** of this policy that
> **excludes** `secretsmanager:GetSecretValue` on `taskactivity/database/credentials`.
> Only the lead administrator should have access to the postgres master password.

#### Step 2 — Create Access Keys for AWS CLI

1. Click the new user → **Security credentials** tab
2. **Access keys → Create access key**
3. Use case: **CLI**
4. Download the CSV — these are shown only once
5. Send the Access Key ID and Secret Access Key to the developer securely

#### Step 3 — Provide the developer with

- Their IAM Access Key ID and Secret Access Key
- The `taskactivity_readonly` database password (retrieve from your password manager)
- A copy of `docs/pgAdmin4_AWS_Setup_Guide.md`

#### Step 4 — Session Manager plugin (if developer needs tunnel/shell access)

Ensure the developer's policy includes the `SSMSessionAccess` statement (scoped to
`taskactivity-cluster` tasks and the port-forwarding document).

---

### Offboarding a Developer

1. **IAM → Users → Select user → Security credentials**
2. **Deactivate** all access keys (do not delete immediately — gives time to verify)
3. After confirming no impact, **Delete** the access keys
4. **Detach** the developer policy
5. If the developer created any IAM resources, review and clean up

---

## 4. RDS — PostgreSQL Database

### Connection Details

```
Host:     taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com
Port:     5432
Database: AmmoP1DB
```

### Database Users

| Username                | Access                   | Password Location                                        |
| ----------------------- | ------------------------ | -------------------------------------------------------- |
| `postgres`              | Master — full read/write | AWS Secrets Manager: `taskactivity/database/credentials` |
| `taskactivity_readonly` | SELECT only — all tables | Administrator password manager                           |

### Start / Stop the Database (cost saving)

```powershell
# Stop (saves ~60% instance cost when not needed)
aws rds stop-db-instance --db-instance-identifier taskactivity-db --region us-east-1

# Start
aws rds start-db-instance --db-instance-identifier taskactivity-db --region us-east-1

# Check status
aws rds describe-db-instances `
    --db-instance-identifier taskactivity-db `
    --region us-east-1 `
    --query "DBInstances[0].[DBInstanceStatus,Endpoint.Address]" `
    --output table
```

### Manual Snapshot

```powershell
aws rds create-db-snapshot `
    --db-instance-identifier taskactivity-db `
    --db-snapshot-identifier "taskactivity-manual-$(Get-Date -Format 'yyyyMMdd-HHmmss')" `
    --region us-east-1
```

### List Snapshots

```powershell
aws rds describe-db-snapshots `
    --db-instance-identifier taskactivity-db `
    --region us-east-1 `
    --query "DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime,Status]" `
    --output table
```

### Enable Deletion Protection (recommended for production)

```powershell
aws rds modify-db-instance `
    --db-instance-identifier taskactivity-db `
    --deletion-protection `
    --apply-immediately
```

### Rotate the postgres Password

```powershell
# 1. Generate new password
$NewPassword = "TaskActivity$(Get-Date -Format 'yyyy')!$(Get-Random -Minimum 1000 -Maximum 9999)"

# 2. Update RDS
aws rds modify-db-instance `
    --db-instance-identifier taskactivity-db `
    --master-user-password $NewPassword `
    --apply-immediately `
    --region us-east-1

# 3. Update Secrets Manager
aws secretsmanager put-secret-value `
    --secret-id taskactivity/database/credentials `
    --secret-string "{`"username`":`"postgres`",`"password`":`"$NewPassword`",`"jdbcUrl`":`"jdbc:postgresql://taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com:5432/AmmoP1DB`"}" `
    --region us-east-1

# 4. Force ECS service restart to pick up new secret
aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity-service `
    --force-new-deployment `
    --region us-east-1
```

### View Database Logs

```powershell
aws rds describe-db-log-files --db-instance-identifier taskactivity-db --region us-east-1

aws rds download-db-log-file-portion `
    --db-instance-identifier taskactivity-db `
    --log-file-name error/postgresql.log.2026-02-01-00 `
    --region us-east-1 `
    --output text
```

### Check if RDS is Publicly Accessible (should always be false)

```powershell
aws rds describe-db-instances `
    --db-instance-identifier taskactivity-db `
    --region us-east-1 `
    --query "DBInstances[0].PubliclyAccessible" `
    --output text
# Expected: false

# If true, fix immediately:
aws rds modify-db-instance `
    --db-instance-identifier taskactivity-db `
    --no-publicly-accessible `
    --apply-immediately `
    --region us-east-1
```

### Estimated Monthly Cost

| Component            | Cost              |
| -------------------- | ----------------- |
| db.t3.micro instance | ~$13–15/month     |
| 20 GB gp3 storage    | ~$2.30/month      |
| 7-day backup storage | ~$1–2/month       |
| **Estimated total**  | **~$16–20/month** |

---

## 5. ECS — Container Service

### Check Service Status

```powershell
aws ecs describe-services `
    --cluster taskactivity-cluster `
    --services taskactivity-service `
    --region us-east-1 `
    --query "services[0].[status,runningCount,desiredCount,deployments[0].rolloutState]" `
    --output table
```

### List Running Tasks

```powershell
aws ecs list-tasks `
    --cluster taskactivity-cluster `
    --service-name taskactivity-service `
    --desired-status RUNNING `
    --region us-east-1
```

### Force Restart (New Deployment)

```powershell
aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity-service `
    --force-new-deployment `
    --region us-east-1
```

### Scale Service (Change Task Count)

```powershell
aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity-service `
    --desired-count 2 `
    --region us-east-1
```

### Get Task Public IP

```powershell
$TASK_ARN = aws ecs list-tasks `
    --cluster taskactivity-cluster `
    --service-name taskactivity-service `
    --region us-east-1 `
    --query "taskArns[0]" --output text

$ENI = aws ecs describe-tasks `
    --cluster taskactivity-cluster `
    --tasks $TASK_ARN `
    --region us-east-1 `
    --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" `
    --output text

aws ec2 describe-network-interfaces `
    --network-interface-ids $ENI `
    --region us-east-1 `
    --query "NetworkInterfaces[0].Association.PublicIp" `
    --output text
```

### Open Interactive Shell in Running Container

```powershell
.\scripts\connect-to-rds.ps1
```

Or directly:

```powershell
$TASK_ID = aws ecs list-tasks `
    --cluster taskactivity-cluster `
    --service-name taskactivity-service `
    --region us-east-1 --query "taskArns[0]" --output text

aws ecs execute-command `
    --cluster taskactivity-cluster `
    --task $TASK_ID `
    --container taskactivity `
    --interactive `
    --command "/bin/bash" `
    --region us-east-1
```

### Update Task Definition and Deploy

```powershell
# Register updated task definition from local file
$NEW_REV = aws ecs register-task-definition `
    --cli-input-json file://aws/taskactivity-task-definition.json `
    --region us-east-1 `
    --query 'taskDefinition.revision' `
    --output text

Write-Host "New revision: $NEW_REV"

# Update service
aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity-service `
    --task-definition "taskactivity:$NEW_REV" `
    --force-new-deployment `
    --region us-east-1
```

### Estimated Monthly Cost

| Component                      | Cost          |
| ------------------------------ | ------------- |
| Fargate (0.5 vCPU, 1 GB, 24/7) | ~$15–20/month |

---

## 6. ECR — Container Registry

### View Repository Images

```powershell
aws ecr describe-images `
    --repository-name taskactivity `
    --region us-east-1 `
    --query "imageDetails[*].[imageTags[0],imagePushedAt,imageSizeInBytes]" `
    --output table
```

### ECR Login (before docker push)

```powershell
aws ecr get-login-password --region us-east-1 | `
    docker login --username AWS --password-stdin `
    378010131175.dkr.ecr.us-east-1.amazonaws.com
```

### Delete Specific Image

```powershell
aws ecr batch-delete-image `
    --repository-name taskactivity `
    --image-ids imageTag=old-tag-name `
    --region us-east-1
```

### Lifecycle Policy (already applied — keeps 5 most recent tagged images)

```powershell
# View current lifecycle policy
aws ecr get-lifecycle-policy --repository-name taskactivity --region us-east-1

# Re-apply from file if needed
aws ecr put-lifecycle-policy `
    --repository-name taskactivity `
    --lifecycle-policy-text file://aws/ecr-lifecycle-policy.json `
    --region us-east-1
```

---

## 7. S3 — Storage Buckets

### Receipt Storage (`taskactivity-receipts-prod`)

Stores expense receipt files uploaded by users. Organized as `username/YYYY/MM/receipt_id_uuid.ext`.

```powershell
# List contents
aws s3 ls s3://taskactivity-receipts-prod/ --recursive --region us-east-1

# Download all receipts for backup
aws s3 sync s3://taskactivity-receipts-prod/ ./receipts-backup/ --region us-east-1

# Delete a specific orphaned file
aws s3 rm s3://taskactivity-receipts-prod/username/2025/12/receipt_7_abc123.pdf --region us-east-1
```

### Documentation Bucket (`taskactivity-docs`)

Hosts publicly accessible documentation files.

```powershell
# List contents
aws s3 ls s3://taskactivity-docs/ --region us-east-1

# Upload with correct content type
aws s3 cp docs/MyDocument.pdf s3://taskactivity-docs/ `
    --content-type "application/pdf" --region us-east-1

aws s3 cp docs/MyDocument.docx s3://taskactivity-docs/ `
    --content-type "application/vnd.openxmlformats-officedocument.wordprocessingml.document" `
    --region us-east-1

aws s3 cp docs/MySpreadsheet.xlsx s3://taskactivity-docs/ `
    --content-type "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" `
    --region us-east-1
```

### Log Archive Bucket (`taskactivity-logs-archive`)

Long-term CloudWatch log exports, organized by date.

```powershell
# List exported logs
aws s3 ls s3://taskactivity-logs-archive/cloudwatch-exports/ --region us-east-1

# Download specific date
aws s3 sync s3://taskactivity-logs-archive/cloudwatch-exports/2026-02-01/ `
    ./downloaded-logs/2026-02-01/ --region us-east-1
```

### Lifecycle Policies (cost optimization)

Receipt bucket lifecycle:

- Glacier after 180 days
- Expiration after 7 years (regulatory)

Log archive lifecycle:

- Glacier after 90 days
- Deep Archive after 365 days
- Expiration after 7 years

```powershell
# Re-apply receipt lifecycle policy if needed
aws s3api put-bucket-lifecycle-configuration `
    --bucket taskactivity-receipts-prod `
    --lifecycle-configuration file://aws/s3-receipts-lifecycle-policy.json `
    --region us-east-1
```

---

## 8. Secrets Manager

### View All TaskActivity Secrets

```powershell
aws secretsmanager list-secrets `
    --region us-east-1 `
    --query "SecretList[?starts_with(Name,'taskactivity')][Name,ARN]" `
    --output table
```

### Retrieve a Secret Value

```powershell
# Database credentials (postgres password)
aws secretsmanager get-secret-value `
    --secret-id taskactivity/database/credentials `
    --region us-east-1 `
    --query SecretString `
    --output text

# Application admin password
aws secretsmanager get-secret-value `
    --secret-id taskactivity/admin/credentials `
    --region us-east-1 `
    --query SecretString `
    --output text
```

### Update a Secret Value

```powershell
aws secretsmanager put-secret-value `
    --secret-id taskactivity/admin/credentials `
    --region us-east-1 `
    --secret-string '{"password":"NewSecurePassword123!"}'
```

### Secret ARNs

| Secret                              | ARN                                                                                             |
| ----------------------------------- | ----------------------------------------------------------------------------------------------- |
| `taskactivity/database/credentials` | `arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/database/credentials-zH7fA0` |
| `taskactivity/admin/credentials`    | `arn:aws:secretsmanager:us-east-1:378010131175:secret:taskactivity/admin/credentials-4nNh6X`    |

---

## 9. SES — Email Service

### Configuration

| Variable                            | Current Value                                                   |
| ----------------------------------- | --------------------------------------------------------------- |
| `MAIL_FROM`                         | `noreply@taskactivitytracker.com`                               |
| `ADMIN_EMAIL`                       | `deanammons@gmail.com`                                          |
| `EXPENSE_APPROVERS`                 | `deanammons@gmail.com;ammonsd@gmail.com`                        |
| `JENKINS_BUILD_NOTIFICATION_EMAIL`  | `deanammons@gmail.com`                                          |
| `JENKINS_DEPLOY_NOTIFICATION_EMAIL` | `deanammons@gmail.com,deanammons48@gmail.com;ammonsd@gmail.com` |

Email triggers:

- Account lockout alerts → `ADMIN_EMAIL`
- Expense submitted → `EXPENSE_APPROVERS`
- Expense status change → submitting user's profile email
- Password expiration warnings → user's profile email
- Jenkins build/deploy results → notification email variables

### Check SES Sending Status

```powershell
# Check sandbox vs production status and sending limits
aws ses get-send-quota --region us-east-1

# List verified identities
aws ses list-identities --region us-east-1

# Check verification status
aws ses get-identity-verification-attributes `
    --identities taskactivitytracker.com `
    --region us-east-1
```

### Monitor Email Delivery via CloudWatch Logs

```powershell
# Live tail — all email activity
aws logs tail /ecs/taskactivity `
    --filter-pattern "email" `
    --follow `
    --region us-east-1

# Filter for sent emails only
aws logs tail /ecs/taskactivity `
    --filter-pattern "Email sent successfully" `
    --follow `
    --region us-east-1

# Filter for errors
aws logs tail /ecs/taskactivity `
    --filter-pattern "Failed to send" `
    --follow `
    --region us-east-1
```

### SES Aggregate Statistics

```powershell
aws ses get-send-statistics --region us-east-1
```

### Update Email Recipients

Email addresses are ECS task definition environment variables.
See [Section 13 — Environment Variable Updates](#13-environment-variable-updates) for the update procedure.

### SES Cost

$0.10 per 1,000 emails. First 62,000/month free when sending from ECS.

---

## 10. CloudWatch — Monitoring and Logging

### View Application Logs

```powershell
# Live tail
aws logs tail /ecs/taskactivity --follow --region us-east-1

# Last 1 hour
aws logs tail /ecs/taskactivity `
    --since 1h `
    --region us-east-1

# Search for errors
aws logs filter-log-events `
    --log-group-name /ecs/taskactivity `
    --filter-pattern "ERROR" `
    --region us-east-1

# Search for specific text
aws logs filter-log-events `
    --log-group-name /ecs/taskactivity `
    --filter-pattern "OutOfMemory" `
    --region us-east-1
```

### Key CloudWatch Metrics to Monitor

**Via AWS Console → CloudWatch → Metrics:**

| Namespace | Metric                     | Alert threshold |
| --------- | -------------------------- | --------------- |
| ECS       | CPUUtilization             | > 80%           |
| ECS       | MemoryUtilization          | > 85%           |
| RDS       | CPUUtilization             | > 80%           |
| RDS       | DatabaseConnections        | > 15            |
| RDS       | FreeStorageSpace           | < 2 GB          |
| RDS       | ReadLatency / WriteLatency | > 100ms         |

### Export Logs to S3 (Manual)

```powershell
$START = [DateTimeOffset]::UtcNow.AddDays(-7).ToUnixTimeMilliseconds()
$END   = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

aws logs create-export-task `
    --log-group-name /ecs/taskactivity `
    --from $START `
    --to $END `
    --destination taskactivity-logs-archive `
    --destination-prefix "cloudwatch-exports/$(Get-Date -Format 'yyyy-MM-dd')" `
    --region us-east-1
```

### Set Log Retention Policy

```powershell
# Retain logs for 90 days (reduce storage costs)
aws logs put-retention-policy `
    --log-group-name /ecs/taskactivity `
    --retention-in-days 90 `
    --region us-east-1
```

### Create a Cost / Alerting Budget

1. AWS Console → **Billing → Budgets → Create budget**
2. Budget type: Cost
3. Amount: `$60/month` (or your preferred limit)
4. Alerts: 80% ($48) and 100% ($60)
5. Email: `deanammons@gmail.com`

```powershell
# Check current month costs by service
aws ce get-cost-and-usage `
    --time-period Start=$(Get-Date -Format 'yyyy-MM-01'),End=$(Get-Date -Format 'yyyy-MM-dd') `
    --granularity MONTHLY `
    --metrics BlendedCost `
    --group-by Type=DIMENSION,Key=SERVICE `
    --region us-east-1
```

---

## 11. VPC and Security Groups

### Security Group: taskactivity-db-sg (RDS)

**ID:** `sg-08f4bdf0f4619d2e0`

| Direction | Protocol | Port | Source                 | Purpose               |
| --------- | -------- | ---- | ---------------------- | --------------------- |
| Inbound   | TCP      | 5432 | `sg-03812ec4ea45c473c` | Allow ECS tasks → RDS |
| Outbound  | All      | All  | `0.0.0.0/0`            | Default               |

### Security Group: taskactivity-ecs-sg (ECS Tasks)

**ID:** `sg-03812ec4ea45c473c`

| Direction | Protocol | Port | Source      | Purpose                      |
| --------- | -------- | ---- | ----------- | ---------------------------- |
| Inbound   | TCP      | 8080 | `0.0.0.0/0` | Allow internet → application |
| Outbound  | All      | All  | `0.0.0.0/0` | Default                      |

### Verify Security Group Rules

```powershell
aws ec2 describe-security-groups `
    --group-ids sg-08f4bdf0f4619d2e0 sg-03812ec4ea45c473c `
    --region us-east-1 `
    --query "SecurityGroups[*].[GroupName,GroupId,IpPermissions]"
```

### Update IAM Policy via Console

When `taskactivity-developer-policy.json` changes:

1. **IAM → Policies** → search `TaskActivityDeveloperPolicy`
2. Click policy → **Edit** → **JSON** tab
3. Paste the full contents of `aws/taskactivity-developer-policy.json`
4. Click **Next → Save changes**

---

## 12. Database Access — pgAdmin 4 and psql

The RDS instance is not publicly accessible. All database access goes through an SSM
port-forwarding tunnel via the running ECS container.

### pgAdmin 4 (GUI — Recommended for Ad-hoc Queries)

See `docs/pgAdmin4_AWS_Setup_Guide.md` for full developer setup instructions.

**Quick steps:**

```powershell
# Step 1 — Start the tunnel (keep this window open)
.\scripts\Start-RdsTunnel.ps1

# Step 2 — Connect pgAdmin to:
#   Host: 127.0.0.1  Port: 15432  DB: AmmoP1DB
#   User: taskactivity_readonly  (read-only)
#   User: postgres               (admin, full access)

# Close tunnel when done: Ctrl+C
```

Optional flags:

```powershell
.\scripts\Start-RdsTunnel.ps1 -LocalPort 5433    # different port
.\scripts\Start-RdsTunnel.ps1 -AdminUser          # shows postgres credentials
```

### psql Shell via ECS (CLI Access)

```powershell
# Read-only interactive session
.\scripts\connect-to-rds.ps1

# Admin (full read/write) session
.\scripts\connect-to-rds.ps1 -Admin

# Run a specific query
.\scripts\connect-to-rds.ps1 'SELECT COUNT(*) FROM public.users'

# Export a query as CSV
.\scripts\connect-to-rds.ps1 'SELECT id,username,email FROM public.users' csv
```

### Database Users

| User                    | Access          | Password Source                                      |
| ----------------------- | --------------- | ---------------------------------------------------- |
| `taskactivity_readonly` | SELECT only     | Administrator password manager                       |
| `postgres`              | Full read/write | Secrets Manager: `taskactivity/database/credentials` |

### Recreate the Read-Only User (if needed)

```powershell
# Connect as admin and run the setup script
.\scripts\connect-to-rds.ps1 -Admin
# Then inside psql:
# \i /path/to/sql/create-readonly-user.sql
```

---

## 13. Environment Variable Updates

All application configuration lives in ECS task definition environment variables. The `.env` file
is **only** used for local development — it is not read by the AWS deployment.

### Common Variables

| Variable                 | Purpose                           | Example                           |
| ------------------------ | --------------------------------- | --------------------------------- |
| `ADMIN_EMAIL`            | Admin notifications               | `admin@example.com`               |
| `EXPENSE_APPROVERS`      | Expense approval notifications    | `approver@example.com`            |
| `MAIL_FROM`              | Email sender address              | `noreply@taskactivitytracker.com` |
| `APP_BASE_URL`           | Base URL for password reset links | `https://taskactivitytracker.com` |
| `JWT_EXPIRATION`         | Access token lifetime (ms)        | `2592000000` (30 days)            |
| `JWT_REFRESH_EXPIRATION` | Refresh token lifetime (ms)       | `604800000` (7 days)              |
| `CORS_ALLOWED_ORIGINS`   | Allowed frontend origins          | `https://taskactivitytracker.com` |

### Method 1 — PowerShell Script (routine updates from .env)

```powershell
# Update and deploy to AWS
.\aws\update-ecs-variables.ps1 -DeployToAws
```

> **Limitation:** Only updates variables that already exist in the AWS task definition.
> To add a **new** variable, use Method 2.

### Method 2 — CLI (register updated local task definition)

```powershell
# Edit aws/taskactivity-task-definition.json locally first, then:
$NEW_REV = aws ecs register-task-definition `
    --cli-input-json file://aws/taskactivity-task-definition.json `
    --region us-east-1 `
    --query 'taskDefinition.revision' `
    --output text

aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity-service `
    --task-definition "taskactivity:$NEW_REV" `
    --force-new-deployment `
    --region us-east-1
```

### Method 3 — AWS Console

1. **ECS → Task Definitions → taskactivity → latest revision**
2. **Create new revision → Create new revision from JSON**
3. Download current: `aws ecs describe-task-definition --task-definition taskactivity --region us-east-1 --query 'taskDefinition' > current.json`
4. Edit `current.json` — find the `environment` array, add/update variables
5. Remove read-only fields: `taskDefinitionArn`, `revision`, `status`, `requiresAttributes`, `compatibilities`, `registeredAt`, `registeredBy`
6. Paste into AWS Console JSON editor → **Create**
7. **ECS → Services → taskactivity-service → Update service**
8. Select new revision → **Force new deployment** → **Update**

---

## 14. Deployment Operations

### Full Deployment (Build + Push + Deploy)

```powershell
.\aws\deploy-aws.ps1
```

### Check Deployment Status

```powershell
aws ecs describe-services `
    --cluster taskactivity-cluster `
    --services taskactivity-service `
    --region us-east-1 `
    --query "services[0].deployments[*].[status,taskDefinition,runningCount,pendingCount,rolloutState]" `
    --output table
```

### Check Current ECR Image Tag

```powershell
.\aws\check-ECRImageTag.ps1
```

### Check Application Health

```powershell
# Get task public IP first (see Section 5), then:
curl http://<TASK_PUBLIC_IP>:8080/actuator/health

# Or via domain:
curl https://taskactivitytracker.com/actuator/health
```

### Rollback to Previous Task Definition

```powershell
# List recent revisions
aws ecs list-task-definitions `
    --family-prefix taskactivity `
    --region us-east-1 `
    --status ACTIVE `
    --query "taskDefinitionArns[-5:]" `
    --output table

# Roll back to a specific revision (e.g., revision 320)
aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity-service `
    --task-definition taskactivity:320 `
    --force-new-deployment `
    --region us-east-1
```

### Cloudflare Cache Purge After Deployment

If users see stale UI after a deployment:

1. **Cloudflare Dashboard** → your domain → **Caching → Configuration**
2. Enable **Development Mode** for 3 hours (bypasses all cache)
3. Verify new JavaScript files are loading
4. Disable Development Mode when done (or wait 3 hours for auto-expiry)

For targeted purge: **Caching → Purge Cache → Custom Purge** → enter specific JS/CSS URLs.

---

## 15. Cost Management

### Estimated Monthly Costs

| Service                            | Estimated Cost    |
| ---------------------------------- | ----------------- |
| RDS db.t3.micro                    | $13–15            |
| RDS storage (20 GB gp3)            | $2.30             |
| RDS backup storage                 | $1–2              |
| ECS Fargate (0.5 vCPU, 1 GB, 24/7) | $15–20            |
| ECR storage                        | ~$1               |
| Secrets Manager (5 secrets)        | ~$2.50            |
| CloudWatch Logs                    | $1–5              |
| S3 (3 buckets)                     | $1–3              |
| SES email                          | ~$0 (free tier)   |
| Lambda log export                  | ~$0 (free tier)   |
| **Total estimate**                 | **~$37–50/month** |

### Cost Saving Tips

- **Stop RDS when not in use** — saves ~$13/month (stop via console or CLI)
- **Use Fargate Spot** for dev/test — up to 70% compute savings
- **RDS Reserved Instance** — up to 40% savings for 1-year commitment
- **Set log retention** to 30–90 days instead of Never Expire

### View Month-to-Date Costs

```powershell
aws ce get-cost-and-usage `
    --time-period Start=$(Get-Date -Format 'yyyy-MM-01'),End=$(Get-Date -Format 'yyyy-MM-dd') `
    --granularity MONTHLY `
    --metrics BlendedCost `
    --group-by Type=DIMENSION,Key=SERVICE `
    --region us-east-1 `
    --query "ResultsByTime[0].Groups[*].[Keys[0],Metrics.BlendedCost.Amount]" `
    --output table
```

### Check AWS Billing Details

```powershell
.\aws\check-billing.ps1
```

---

## 16. Security Best Practices

### Rules to Follow

1. **Never use the root account** for day-to-day work — create IAM users
2. **Enable MFA** on the root account and all IAM users
3. **Rotate access keys** annually at minimum
4. **RDS must stay private** — `PubliclyAccessible: false` — verify regularly (see Section 4)
5. **No passwords in source code** — all secrets go in Secrets Manager
6. **Scoped IAM policies for new developers** — exclude database credentials secret
7. **Review security groups** quarterly — inbound rules only as permissive as required

### Verify Security Posture (run regularly)

```powershell
# RDS not publicly accessible
aws rds describe-db-instances `
    --db-instance-identifier taskactivity-db `
    --query "DBInstances[0].PubliclyAccessible" --output text
# Expected: false

# Check who has access to the account
aws iam list-users --query "Users[*].[UserName,CreateDate]" --output table

# Check what policies Dean has
aws iam list-attached-user-policies --user-name Dean --output table

# List active access keys
aws iam list-access-keys --user-name Dean `
    --query "AccessKeyMetadata[*].[AccessKeyId,Status,CreateDate]" --output table
```

### Incident Response Checklist

If a credential is suspected compromised:

1. **Immediately deactivate** the affected IAM access key:
   `aws iam update-access-key --user-name Dean --access-key-id AKIAxx --status Inactive`
2. **Rotate** (create new key, delete old)
3. **Rotate** the database master password (see Section 4)
4. **Rotate** the JWT secret in Secrets Manager and force ECS restart
5. **Review** CloudTrail for unauthorized API calls:
   AWS Console → CloudTrail → Event history → Filter by time

---

## 17. Troubleshooting

### Application Not Starting

```powershell
# View startup logs
aws logs tail /ecs/taskactivity --since 30m --region us-east-1

# Look for:
# "Started TaskActivityApplication" — success
# "Failed to configure DataSource" — database connection problem
# "Invalid JWT secret" — JWT_SECRET missing or wrong
```

Common causes:

- Secret missing or changed → verify Secrets Manager values
- Database not started → `aws rds start-db-instance --db-instance-identifier taskactivity-db`
- Security group misconfigured → verify ECS sg can reach RDS sg on port 5432

### Tasks Won't Start / Cycling

```powershell
aws ecs describe-services `
    --cluster taskactivity-cluster `
    --services taskactivity-service `
    --region us-east-1 `
    --query "services[0].events[:5]"
```

Check: IAM roles have permissions, secrets exist, image exists in ECR.

### Cannot Connect to Database (via tunnel)

| Symptom                        | Cause                                     | Fix                                   |
| ------------------------------ | ----------------------------------------- | ------------------------------------- |
| `AccessDenied` starting tunnel | Missing `ssm:StartSession` IAM permission | Add `SSMSessionAccess` to policy      |
| `TargetNotConnected`           | ECS task was restarted; stale target ID   | Close tunnel window, run script again |
| `Port 15432 already in use`    | Another tunnel session is open            | Close it or use `-LocalPort 15433`    |
| pgAdmin "connection refused"   | Tunnel not running                        | Run `.\scripts\Start-RdsTunnel.ps1`   |

### CORS Errors in Browser

```powershell
# Update CORS origins in task definition and redeploy
.\aws\update-cors-origins.ps1
```

Or update `CORS_ALLOWED_ORIGINS` directly (see Section 13).

### Email Not Sending

```powershell
# Check SES logs
aws logs filter-log-events `
    --log-group-name /ecs/taskactivity `
    --filter-pattern "SES" `
    --region us-east-1

# Verify SES identity is verified
aws ses get-identity-verification-attributes `
    --identities taskactivitytracker.com `
    --region us-east-1
```

Check: `MAIL_ENABLED=true` and `MAIL_USE_AWS_SDK=true` in task definition environment.

---

## 18. Quick Reference Card

### AWS Account

| Item         | Value                                                       |
| ------------ | ----------------------------------------------------------- |
| Account ID   | `378010131175`                                              |
| Region       | `us-east-1`                                                 |
| RDS Endpoint | `taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com`  |
| ECR URI      | `378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity` |

### Most-Used Commands

| Task                     | Command                                                                                                                          |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| Check ECS service status | `aws ecs describe-services --cluster taskactivity-cluster --services taskactivity-service --region us-east-1`                    |
| Force ECS restart        | `aws ecs update-service --cluster taskactivity-cluster --service taskactivity-service --force-new-deployment --region us-east-1` |
| Tail application logs    | `aws logs tail /ecs/taskactivity --follow --region us-east-1`                                                                    |
| Start RDS                | `aws rds start-db-instance --db-instance-identifier taskactivity-db --region us-east-1`                                          |
| Stop RDS                 | `aws rds stop-db-instance --db-instance-identifier taskactivity-db --region us-east-1`                                           |
| Open pgAdmin tunnel      | `.\scripts\Start-RdsTunnel.ps1`                                                                                                  |
| Open psql shell          | `.\scripts\connect-to-rds.ps1`                                                                                                   |
| Full deployment          | `.\aws\deploy-aws.ps1`                                                                                                           |
| Check billing            | `.\aws\check-billing.ps1`                                                                                                        |
| Who am I (AWS)           | `aws sts get-caller-identity`                                                                                                    |

### Scripts Reference

| Script                          | Purpose                                      |
| ------------------------------- | -------------------------------------------- |
| `aws/deploy-aws.ps1`            | Build, push, and deploy the application      |
| `aws/update-ecs-variables.ps1`  | Update task definition environment variables |
| `aws/update-cors-origins.ps1`   | Update CORS allowed origins                  |
| `aws/update-jwt-expiration.ps1` | Update JWT token expiration                  |
| `aws/update-iam-policy.ps1`     | Apply updated IAM policy to the Dean user    |
| `aws/check-billing.ps1`         | Show current month AWS costs                 |
| `aws/check-ECRImageTag.ps1`     | Show current deployed image tag              |
| `aws/configure-ses.ps1`         | SES email configuration                      |
| `aws/add-jwt-secret.ps1`        | Rotate JWT secret in Secrets Manager         |
| `scripts/Start-RdsTunnel.ps1`   | pgAdmin port-forwarding tunnel               |
| `scripts/connect-to-rds.ps1`    | Interactive psql shell via ECS               |

### Related Documentation

| Document                    | Location                                  |
| --------------------------- | ----------------------------------------- |
| RDS full setup details      | `aws/RDS_Database_Documentation.md`       |
| AWS Console visual guide    | `aws/AWS_Console_Guide.md`                |
| Full deployment guide       | `aws/AWS_Deployment.md`                   |
| SES email setup             | `aws/AWS_SES_Setup_Guide.md`              |
| IAM permissions reference   | `aws/IAM_Permissions_Reference.md`        |
| ECS environment variables   | `aws/Update_ECS_Environment_Variables.md` |
| Manual IAM update for admin | `aws/Manual_IAM_Update.md`                |
| pgAdmin 4 developer setup   | `docs/pgAdmin4_AWS_Setup_Guide.md`        |
| Administrator User Guide    | `docs/Administrator_User_Guide.md`        |
