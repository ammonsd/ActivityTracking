# IAM Permissions Reference for Task Activity Scripts

## Overview

This document describes the AWS IAM permissions required to execute the deployment and configuration scripts for the Task Activity application.

## Required IAM Policy

The complete IAM policy is defined in `taskactivity-developer-policy.json`. This policy must be attached to your IAM user or role to execute all scripts successfully.

---

## Permission Categories

### 1. **ECR (Elastic Container Registry)** - Docker Image Management

**Used by**: `deploy-aws.ps1`

**Actions**:

-   `ecr:GetAuthorizationToken` - Get ECR login token
-   `ecr:BatchCheckLayerAvailability` - Verify image layers
-   `ecr:GetDownloadUrlForLayer` - Pull images
-   `ecr:BatchGetImage` - Pull images
-   `ecr:PutImage` - Push images
-   `ecr:InitiateLayerUpload` - Push image layers
-   `ecr:UploadLayerPart` - Upload image data
-   `ecr:CompleteLayerUpload` - Finalize image push
-   `ecr:DescribeRepositories` - List repositories
-   `ecr:ListImages` - List images in repository
-   `ecr:DescribeImages` - Get image details

**Why needed**: To build and push Docker images to ECR

---

### 2. **ECS (Elastic Container Service)** - Container Orchestration

**Used by**: `deploy-aws.ps1`, `configure-ses.ps1`

**Actions**:

-   `ecs:DescribeClusters` - Get cluster information
-   `ecs:DescribeServices` - Get service status
-   `ecs:DescribeTasks` - Get running task details
-   `ecs:DescribeTaskDefinition` - Read task definitions
-   `ecs:ListTasks` - List running tasks
-   `ecs:ListServices` - List services
-   `ecs:ListClusters` - List clusters
-   `ecs:RegisterTaskDefinition` - Create new task definitions
-   `ecs:DeregisterTaskDefinition` - Remove old task definitions
-   `ecs:UpdateService` - Deploy new versions
-   `ecs:CreateService` - Create new services
-   `ecs:DeleteService` - Remove services
-   `ecs:RunTask` - Start individual tasks
-   `ecs:StopTask` - Stop tasks
-   `ecs:ExecuteCommand` - Access container shell (ECS Exec)

**Why needed**: To deploy and manage containerized application

---

### 3. **RDS (Relational Database Service)** - Database Information

**Used by**: `deploy-aws.ps1` (status checks)

**Actions**:

-   `rds:DescribeDBInstances` - Get database endpoint information
-   `rds:DescribeDBClusters` - Get cluster information
-   `rds:ListTagsForResource` - Read database tags

**Why needed**: To verify database connectivity and display status

---

### 4. **Secrets Manager** - Secure Credential Storage

**Used by**: `deploy-aws.ps1`, application configuration

**Actions**:

-   `secretsmanager:GetSecretValue` - Read secrets (database passwords, SMTP credentials)
-   `secretsmanager:DescribeSecret` - Get secret metadata
-   `secretsmanager:ListSecrets` - List available secrets
-   `secretsmanager:CreateSecret` - Create new secrets
-   `secretsmanager:UpdateSecret` - Update existing secrets
-   `secretsmanager:PutSecretValue` - Update secret values

**Why needed**: To store and retrieve database passwords, SMTP credentials securely

---

### 5. **CloudWatch Logs** - Application Logging

**Used by**: `deploy-aws.ps1` (monitoring), application runtime

**Actions**:

-   `logs:CreateLogGroup` - Create log groups
-   `logs:CreateLogStream` - Create log streams
-   `logs:PutLogEvents` - Write log entries
-   `logs:DescribeLogGroups` - List log groups
-   `logs:DescribeLogStreams` - List log streams
-   `logs:GetLogEvents` - Read logs
-   `logs:FilterLogEvents` - Search logs
-   `logs:PutRetentionPolicy` - Set log retention

**Why needed**: To monitor application logs and troubleshoot issues

---

### 6. **IAM** - Role Management

**Used by**: `deploy-aws.ps1`

**Actions**:

-   `iam:PassRole` - Allow ECS to assume task roles (restricted to `ecs-tasks.amazonaws.com`)
-   `iam:GetRole` - Read role details
-   `iam:ListRoles` - List available roles

**Why needed**: To assign IAM roles to ECS tasks for S3 and SES access

**Security Note**: `PassRole` is restricted by condition to only allow passing roles to ECS tasks, preventing privilege escalation.

---

### 7. **Elastic Load Balancing** - Load Balancer Information

**Used by**: `deploy-aws.ps1` (status checks)

**Actions**:

-   `elasticloadbalancing:DescribeLoadBalancers` - Get ALB DNS name
-   `elasticloadbalancing:DescribeTargetGroups` - Get target group info
-   `elasticloadbalancing:DescribeTargetHealth` - Check backend health
-   `elasticloadbalancing:DescribeListeners` - Get listener configuration
-   `elasticloadbalancing:DescribeRules` - Get routing rules

**Why needed**: To display application URL after deployment

---

### 8. **VPC/EC2** - Network Information

**Used by**: `deploy-aws.ps1` (status checks)

**Actions**:

-   `ec2:DescribeVpcs` - Get VPC details
-   `ec2:DescribeSubnets` - Get subnet information
-   `ec2:DescribeSecurityGroups` - Get security group rules
-   `ec2:DescribeNetworkInterfaces` - Get task public IP addresses

**Why needed**: To determine task public IPs and network configuration

---

### 9. **STS (Security Token Service)** - Identity Verification

**Used by**: `deploy-aws.ps1`

**Actions**:

-   `sts:GetCallerIdentity` - Get AWS account ID and user information

**Why needed**: To determine AWS account ID for ECR repository URLs

---

### 10. **SSM Sessions** - Port Forwarding to RDS via ECS

**Used by**: `Start-RdsTunnel.ps1`, `connect-to-rds.ps1`

**Actions**:

-   `ssm:StartSession` - Open an SSM session (required for port-forwarding and ECS Exec)
-   `ssm:TerminateSession` - Close an active session
-   `ssm:ResumeSession` - Resume an interrupted session
-   `ssm:DescribeSessions` - List active sessions (debugging)
-   `ssm:GetConnectionStatus` - Check whether the SSM agent in the container is ready

**Resources scoped to**:
-   `arn:aws:ecs:us-east-1:378010131175:task/taskactivity-cluster/*` - ECS tasks used as the relay
-   `arn:aws:ssm:*::document/AWS-StartPortForwardingSessionToRemoteHost` - AWS-managed port-forward document
-   `arn:aws:ssm:us-east-1:378010131175:session/*` - Sessions created by this user

**Why needed**: `ssm:StartSession` is required for both `aws ssm start-session` (port forwarding
used by `Start-RdsTunnel.ps1`) and `aws ecs execute-command` (shell access used by
`connect-to-rds.ps1`). Without it, both scripts return `AccessDenied`.

---

### 11. **Route53** - DNS Management

**Used by**: Future CloudFront/custom domain setup

**Actions**:

-   `route53:ListHostedZones` - List DNS zones
-   `route53:GetHostedZone` - Get zone details
-   `route53:ListResourceRecordSets` - List DNS records
-   `route53:ChangeResourceRecordSets` - Update DNS records

**Why needed**: For custom domain configuration

---

### 12. **SES (Simple Email Service)** - Email Notifications

**Used by**: `configure-ses.ps1`

**Actions**:

-   `ses:GetSendQuota` - Check sending limits and usage
-   `ses:GetIdentityVerificationAttributes` - Check email/domain verification status
-   `ses:VerifyEmailIdentity` - Initiate email verification
-   `ses:VerifyDomainIdentity` - Initiate domain verification
-   `ses:VerifyDomainDkim` - Get DKIM tokens for domain
-   `ses:ListIdentities` - List verified emails/domains
-   `ses:DeleteIdentity` - Remove verified identities
-   `ses:GetIdentityDkimAttributes` - Get DKIM status
-   `ses:SetIdentityDkimEnabled` - Enable/disable DKIM
-   `ses:SendEmail` - Send emails (for testing)
-   `ses:SendRawEmail` - Send raw MIME emails
-   `sesv2:GetAccount` - Check sandbox/production status
-   `sesv2:PutAccountDetails` - Request production access

**Why needed**: To configure email notifications for account lockouts and security alerts

---

## How to Apply the Policy

### Method 1: AWS Console

1. Log into AWS Console as administrator
2. Go to **IAM** → **Policies**
3. Click **Create policy**
4. Click **JSON** tab
5. Paste contents of `taskactivity-developer-policy.json`
6. Click **Next**
7. Name: `TaskActivityDeveloperPolicy`
8. Description: "Deployment permissions for Task Activity application"
9. Click **Create policy**
10. Go to **IAM** → **Users** → Select your user
11. Click **Add permissions** → **Attach policies directly**
12. Search for `TaskActivityDeveloperPolicy`
13. Select it and click **Add permissions**

### Method 2: AWS CLI

```powershell
# Create the policy
aws iam create-policy `
  --policy-name TaskActivityDeveloperPolicy `
  --policy-document file://aws/taskactivity-developer-policy.json `
  --description "Deployment permissions for Task Activity application"

# Get your IAM username
$username = aws sts get-caller-identity --query "Arn" --output text | Split-Path -Leaf

# Get the policy ARN (replace with your account ID)
$accountId = aws sts get-caller-identity --query "Account" --output text
$policyArn = "arn:aws:iam::${accountId}:policy/TaskActivityDeveloperPolicy"

# Attach policy to user
aws iam attach-user-policy `
  --user-name $username `
  --policy-arn $policyArn
```

---

## Security Best Practices

### ✅ What This Policy Does Well

1. **Scoped PassRole**: `iam:PassRole` is restricted to only `ecs-tasks.amazonaws.com` service
2. **Read-Only Where Possible**: Many permissions are describe/list only
3. **No Resource Wildcards for Secrets**: Uses `*` but in practice you should scope to specific secret ARNs
4. **No Admin Access**: Does not grant full admin permissions

### ⚠️ Optional Improvements for Production

For tighter security in production, consider:

1. **Restrict ECR to specific repositories**:

    ```json
    "Resource": "arn:aws:ecr:us-east-1:123456789012:repository/taskactivity"
    ```

2. **Restrict ECS to specific cluster/service**:

    ```json
    "Resource": [
      "arn:aws:ecs:us-east-1:123456789012:cluster/taskactivity-cluster",
      "arn:aws:ecs:us-east-1:123456789012:service/taskactivity-cluster/taskactivity-service"
    ]
    ```

3. **Restrict Secrets Manager to specific secrets**:

    ```json
    "Resource": [
      "arn:aws:secretsmanager:us-east-1:123456789012:secret:taskactivity/*"
    ]
    ```

4. **Restrict SES to verified identities only** (after verification):
    ```json
    "Resource": [
      "arn:aws:ses:us-east-1:123456789012:identity/taskactivitytracker.com"
    ]
    ```

---

## Testing Permissions

After applying the policy, test each script:

```powershell
# Test basic deployment (requires ECR, ECS, STS)
.\aws\deploy-aws.ps1 -Status

# Test SES configuration (requires SES)
.\aws\configure-ses.ps1

# Test full deployment (requires all permissions)
.\aws\deploy-aws.ps1
```

If you encounter permission errors, check the error message for the specific action needed and verify it's in the policy.

---

## Troubleshooting

### Error: "AccessDenied" or "UnauthorizedOperation"

**Solution**: Check which AWS API action is being denied in the error message and verify it's included in your policy.

### Error: "PassRole is not authorized"

**Solution**: Verify the `iam:PassRole` permission is present and the condition allows `ecs-tasks.amazonaws.com`.

### Error: "SES sending limits exceeded"

**Solution**: This is not a permissions issue. You're in SES sandbox mode or have hit rate limits. Use `configure-ses.ps1 -RequestProductionAccess` for instructions.

---

## Summary

**Total Permissions**: 12 AWS service categories, 75+ individual actions

**Critical for deployment**:

-   ECR (push images)
-   ECS (deploy containers)
-   STS (get account ID)

**Critical for email**:

-   SES (verify identities, send emails)

**Optional but recommended**:

-   Secrets Manager (secure credentials)
-   CloudWatch Logs (monitoring)
-   ELB (get application URL)
-   EC2 (get task IPs)

All these permissions are now included in the updated `taskactivity-developer-policy.json` file.
