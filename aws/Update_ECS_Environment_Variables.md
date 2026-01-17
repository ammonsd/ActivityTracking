# How to Update ECS Task Definition Environment Variables

This guide explains how to update environment variables in your ECS task definition, particularly for email configuration that may change over time.

## Overview

The TaskActivity application running on AWS ECS reads configuration from **environment variables** defined in the ECS task definition. These variables are **NOT** read from the `.env` file at runtime - that file is only used for local Docker development.

Common variables that may need updating:
- `ADMIN_EMAIL` - Administrator email address
- `EXPENSE_APPROVERS` - Expense approval notification recipients
- `JENKINS_BUILD_NOTIFICATION_EMAIL` - Build notification recipients
- `JENKINS_DEPLOY_NOTIFICATION_EMAIL` - Deployment notification recipients

## Method 1: AWS Console (Recommended for Simple Updates)

### Step 1: Navigate to Task Definition

1. Go to **AWS Console** → **ECS** → **Task Definitions**
2. Click on **taskactivity**
3. Select the latest revision (highest number)

### Step 2: Create New Revision with Updates

1. Click **Create new revision** → **Create new revision from JSON**
2. Download current task definition JSON:
   ```powershell
   aws ecs describe-task-definition --task-definition taskactivity --region us-east-1 --query 'taskDefinition' > current-task-def.json
   ```

3. Edit the JSON file:
   - Find the `"environment"` array inside `"containerDefinitions"`
   - Update existing variables or add new ones:
     ```json
     {
         "name": "JENKINS_BUILD_NOTIFICATION_EMAIL",
         "value": "new-email@company.com"
     }
     ```

4. Remove read-only fields (if present):
   - `taskDefinitionArn`
   - `revision`
   - `status`
   - `requiresAttributes`
   - `compatibilities`
   - `registeredAt`
   - `registeredBy`

5. Copy the entire JSON content
6. Paste into AWS Console JSON editor
7. Click **Create**

### Step 3: Update Service to Use New Revision

1. Go to **ECS** → **Clusters** → **taskactivity-cluster**
2. Click on the **taskactivity** service
3. Click **Update service**
4. Under **Task Definition**, select the new revision
5. Check **Force new deployment**
6. Click **Update**

The service will restart with the new environment variables.

## Method 2: AWS CLI (Automated)

### Prerequisites

Ensure you have AWS CLI configured with appropriate permissions:
```powershell
aws configure list
```

### Script to Update Environment Variables

```powershell
# Step 1: Get current task definition
aws ecs describe-task-definition `
    --task-definition taskactivity `
    --region us-east-1 `
    --query 'taskDefinition' > current-task-def.json

# Step 2: Edit current-task-def.json manually to update environment variables

# Step 3: Register new task definition
aws ecs register-task-definition `
    --cli-input-json file://current-task-def.json `
    --region us-east-1

# Step 4: Update service to use new revision
aws ecs update-service `
    --cluster taskactivity-cluster `
    --service taskactivity `
    --task-definition taskactivity `
    --force-new-deployment `
    --region us-east-1
```

## Email Configuration Variables

### Format Guidelines

**Single Email:**
```json
"value": "email@example.com"
```

**Multiple Emails (separate emails):**
```json
"value": "email1@example.com,email2@example.com"
```

**Grouped Emails (one email with multiple recipients):**
```json
"value": "email1@example.com;email2@example.com"
```

**Mixed (separate + grouped):**
```json
"value": "email1@example.com,email2@example.com;email3@example.com"
```
- `email1@example.com` receives one email
- `email2@example.com` AND `email3@example.com` receive one shared email

### Current Email Configuration

| Variable | Purpose | Current Value |
|----------|---------|---------------|
| `MAIL_FROM` | Sender address | `noreply@taskactivitytracker.com` |
| `ADMIN_EMAIL` | Admin notifications | `deanammons@gmail.com` |
| `EXPENSE_APPROVERS` | Expense approvals | `deanammons@gmail.com;ammonsd@gmail.com` |
| `JENKINS_BUILD_NOTIFICATION_EMAIL` | Build notifications | `deanammons@gmail.com` |
| `JENKINS_DEPLOY_NOTIFICATION_EMAIL` | Deploy notifications | `deanammons@gmail.com,deanammons48@gmail.com;ammonsd@gmail.com` |

## Example: Updating Jenkins Email Addresses

### Scenario
- Build notifications should go to: `dev-team@company.com`
- Deploy notifications should go to: `dev-team@company.com` and `ba-team@company.com` (separate emails)

### Steps

1. **Download current task definition:**
   ```powershell
   aws ecs describe-task-definition --task-definition taskactivity --region us-east-1 --query 'taskDefinition' > current-task-def.json
   ```

2. **Edit current-task-def.json:**
   Find the environment variables section and update:
   ```json
   {
       "name": "JENKINS_BUILD_NOTIFICATION_EMAIL",
       "value": "dev-team@company.com"
   },
   {
       "name": "JENKINS_DEPLOY_NOTIFICATION_EMAIL",
       "value": "dev-team@company.com,ba-team@company.com"
   }
   ```

3. **Remove read-only fields:**
   Delete these properties if present:
   - `taskDefinitionArn`
   - `revision`
   - `status`
   - `requiresAttributes`
   - `compatibilities`
   - `registeredAt`
   - `registeredBy`

4. **Upload via AWS Console:**
   - ECS → Task Definitions → taskactivity
   - Create new revision → Create new revision from JSON
   - Paste updated JSON
   - Click Create

5. **Update service:**
   - ECS → Clusters → taskactivity-cluster → taskactivity service
   - Update service → Select new revision → Force new deployment
   - Click Update

## Verification

After updating and redeploying:

1. **Check running task:**
   ```powershell
   aws ecs describe-tasks `
       --cluster taskactivity-cluster `
       --tasks $(aws ecs list-tasks --cluster taskactivity-cluster --service taskactivity --query 'taskArns[0]' --output text) `
       --query 'tasks[0].taskDefinitionArn' `
       --region us-east-1
   ```
   Verify it shows the new revision number.

2. **Check environment variables:**
   ```powershell
   aws ecs describe-task-definition `
       --task-definition taskactivity `
       --region us-east-1 `
       --query 'taskDefinition.containerDefinitions[0].environment[?name==`JENKINS_BUILD_NOTIFICATION_EMAIL` || name==`JENKINS_DEPLOY_NOTIFICATION_EMAIL`]'
   ```

3. **Test functionality:**
   - Trigger a Jenkins build
   - Verify emails are sent to new addresses

## Troubleshooting

### Changes Not Taking Effect

**Problem:** Updated environment variables but application still uses old values.

**Solution:**
1. Verify the service is using the new task definition revision:
   - ECS Console → Clusters → Service → Deployments tab
   - Check "Task definition" shows latest revision
2. If not, force new deployment:
   - Update service → Force new deployment

### Email Not Sending

**Problem:** No emails received after updating addresses.

**Checklist:**
1. ✅ Verify sender email (`MAIL_FROM`) is verified in AWS SES
2. ✅ Check recipient emails are verified (if SES is in sandbox mode)
3. ✅ Verify ECS task role has SES permissions
4. ✅ Check CloudWatch logs for email sending errors:
   ```
   /ecs/taskactivity
   ```

### Invalid JSON Error

**Problem:** AWS Console rejects the JSON file.

**Solution:**
1. Validate JSON syntax: https://jsonlint.com/
2. Remove any read-only fields listed above
3. Ensure no trailing commas in arrays/objects

## Notes

- **Environment variables are separate from .env file** - The `.env` file in the repository is for Docker/local development only
- **Changes require service restart** - New environment variables only take effect after ECS service redeploys
- **No downtime** - ECS performs rolling updates when you force new deployment
- **Version history** - Each task definition revision is preserved, so you can roll back if needed

## Related Documentation

- [Jenkins Build Notifications](../jenkins/Jenkins_Build_Notifications.md) - Setup guide for Jenkins email notifications
- [AWS Deployment Guide](./AWS_Deployment.md) - Full AWS deployment process
- [AWS SES Setup Guide](./AWS_SES_Setup_Guide.md) - Email service configuration
