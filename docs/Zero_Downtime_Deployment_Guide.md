# Zero-Downtime Deployment Configuration Guide

**Purpose:** Configure the Task Activity Tracking application for multiple daily deployments (8am, 12pm, 4pm) with minimal to zero impact on users.

**Author:** Dean Ammons  
**Date:** January 2026  
**Status:** Development Phase - Changes Required Before Production

---

## Executive Summary

The application is currently configured for automated deployments at **4pm only**. To support **three deployments per day (8am, 12pm, 4pm)** without user impact, specific infrastructure changes are required.

### Current Status

- ✅ **Jenkins Schedule:** Updated to deploy at 8am, 12pm, and 4pm
- ✅ **ECS Deployment Configuration:** Already configured for rolling updates
    - `MinimumHealthyPercent: 100`
    - `MaximumPercent: 200`
    - Circuit breaker enabled with auto-rollback
- ❌ **Task Count:** Currently 1 task (MUST be increased to 2)
- ✅ **Health Checks:** Configured and functional

---

## Critical Change Required

### 1. Increase ECS Task Count to 2

**Current Configuration:**

- Running with `DesiredCount: 1` task
- Deployment can cause brief interruption during task replacement

**Required Change:**

- Update CloudFormation stack to set `DesiredCount: 2`

**Why This Matters:**

With **1 task:**

- During deployment: Start new task → Wait for health checks → Stop old task
- Risk: Brief latency spike or connection drops during switchover
- Problem: No redundancy if deployment fails

With **2 tasks:**

- During deployment: Start new task (now 3 total) → Pass health checks → Stop 1 old task → Repeat for 2nd task
- Result: Always 2 healthy tasks serving traffic
- Benefit: True zero-downtime deployments

---

## Implementation Steps

### Step 1: Update CloudFormation Stack

You can update the ECS service to 2 tasks using either the AWS CLI or the AWS Console.

#### Option A: Using AWS CLI (PowerShell)

Run the following command to update the ECS service to 2 tasks:

```powershell
# Get the current stack name
$StackName = "taskactivity-production"  # Or your actual stack name

# Update the stack with new desired count
aws cloudformation update-stack `
    --stack-name $StackName `
    --use-previous-template `
    --parameters `
        ParameterKey=DesiredCount,ParameterValue=2 `
        ParameterKey=Environment,UsePreviousValue=true `
        ParameterKey=VpcCIDR,UsePreviousValue=true `
        ParameterKey=DBInstanceClass,UsePreviousValue=true `
        ParameterKey=DBAllocatedStorage,UsePreviousValue=true `
        ParameterKey=DBName,UsePreviousValue=true `
        ParameterKey=DBUsername,UsePreviousValue=true `
        ParameterKey=DBPassword,UsePreviousValue=true `
        ParameterKey=AdminPassword,UsePreviousValue=true `
        ParameterKey=TaskCPU,UsePreviousValue=true `
        ParameterKey=TaskMemory,UsePreviousValue=true `
        ParameterKey=ContainerPort,UsePreviousValue=true `
    --capabilities CAPABILITY_IAM
```

**Wait for stack update to complete:**

```powershell
aws cloudformation wait stack-update-complete --stack-name $StackName
```

**Verify the change:**

```powershell
aws ecs describe-services `
    --cluster taskactivity-cluster `
    --services taskactivity-service `
    --query 'services[0].{DesiredCount:desiredCount,RunningCount:runningCount,DeploymentConfig:deploymentConfiguration}'
```

**Expected output:**

```json
{
    "DesiredCount": 2,
    "RunningCount": 2,
    "DeploymentConfig": {
        "MinimumHealthyPercent": 100,
        "MaximumPercent": 200,
        "DeploymentCircuitBreaker": {
            "Enable": true,
            "Rollback": true
        }
    }
}
```

#### Option B: Using AWS Console

If you don't have CLI permissions or prefer the console:

1. **Open CloudFormation Console**
   - Navigate to: https://console.aws.amazon.com/cloudformation
   - Select your region: **US East (N. Virginia) us-east-1**

2. **Locate Your Stack**
   - Find stack named: `taskactivity-production` (or your stack name)
   - Click on the stack name to open it

3. **Update Stack**
   - Click **Update** button (top right)
   - Select **Use current template**
   - Click **Next**

4. **Update Parameters**
   - Scroll to find **DesiredCount** parameter
   - Change value from `1` to `2`
   - Leave all other parameters unchanged
   - Click **Next**

5. **Configure Stack Options**
   - Leave all options as-is (don't change anything)
   - Click **Next**

6. **Review and Update**
   - Review the changes (should show DesiredCount: 1 → 2)
   - Check the box: **I acknowledge that AWS CloudFormation might create IAM resources**
   - Click **Submit** (or **Update stack**)

7. **Monitor Stack Update**
   - Watch the **Events** tab for progress
   - Wait for status to change from `UPDATE_IN_PROGRESS` to `UPDATE_COMPLETE`
   - This typically takes 3-5 minutes

8. **Verify ECS Service**
   - Navigate to: https://console.aws.amazon.com/ecs
   - Click **Clusters** → `taskactivity-cluster`
   - Click **Services** → `taskactivity-service`
   - Under **Deployments and events**, verify:
     - **Desired tasks**: 2
     - **Running tasks**: 2
   - Under **Deployment configuration**, verify:
     - **Minimum healthy percent**: 100
     - **Maximum percent**: 200
     - **Deployment circuit breaker**: Enabled

### Step 2: Verify Health Checks

Ensure Spring Boot Actuator health endpoints are working correctly:

```powershell
# Test liveness endpoint
curl https://taskactivitytracker.com/actuator/health/liveness

# Test readiness endpoint
curl https://taskactivitytracker.com/actuator/health/readiness

# Test general health endpoint
curl https://taskactivitytracker.com/actuator/health
```

All should return `{"status":"UP"}` or similar positive response.

### Step 3: Test a Deployment

After increasing to 2 tasks, test the deployment process during business hours:

1. **Trigger a manual deployment:**

    ```bash
    # In Jenkins, trigger a manual build with:
    # - DEPLOY_ACTION: deploy
    # - Environment: production
    ```

2. **Monitor the deployment:**

    ```powershell
    # Watch task count during deployment
    while ($true) {
        $service = aws ecs describe-services `
            --cluster taskactivity-cluster `
            --services taskactivity-service `
            --query 'services[0].{Running:runningCount,Desired:desiredCount,Deployments:deployments[*].{Status:status,TaskDef:taskDefinition}}' `
            --output json | ConvertFrom-Json

        Write-Host "$(Get-Date -Format 'HH:mm:ss') - Running: $($service.Running) | Desired: $($service.Desired)"
        Start-Sleep -Seconds 5
    }
    ```

3. **Verify zero downtime:**
    - Monitor application logs for any errors
    - Check that users can access the application throughout deployment
    - Verify ALB target health remains healthy

### Step 4: Monitor After Implementation

After enabling multiple deployments per day, monitor these metrics:

**CloudWatch Metrics to Watch:**

- ECS Service: `CPUUtilization` and `MemoryUtilization`
- ALB: `TargetResponseTime`, `UnHealthyHostCount`, `HTTPCode_Target_5XX_Count`
- RDS: `DatabaseConnections`, `CPUUtilization`

**Set up CloudWatch Alarms:**

```powershell
# Example: Alert on unhealthy targets
aws cloudwatch put-metric-alarm `
    --alarm-name taskactivity-unhealthy-targets `
    --alarm-description "Alert when ALB targets are unhealthy" `
    --metric-name UnHealthyHostCount `
    --namespace AWS/ApplicationELB `
    --statistic Average `
    --period 60 `
    --evaluation-periods 2 `
    --threshold 1 `
    --comparison-operator GreaterThanThreshold `
    --dimensions Name=LoadBalancer,Value=<your-alb-arn-suffix>
```

---

## Cost Analysis

### Current Cost (1 Task)

- CPU: 512 units (0.5 vCPU) = $0.04048/hour
- Memory: 1024 MB (1 GB) = $0.004445/hour
- **Total:** ~$0.0449/hour × 720 hours/month = **~$32.33/month**

### New Cost (2 Tasks)

- **Total:** ~$0.0449/hour × 2 tasks × 720 hours/month = **~$64.66/month**

### Additional Monthly Cost

- **Increase:** ~$32.33/month (~$388/year)
- **Cost per deployment:** ~$1.08/day for reliable, zero-downtime updates

### Cost Optimization Option (If Needed)

If cost is a concern, consider reducing task size when scaling to 2:

**Option: 256 CPU / 512 MB per task**

- Per task: ~$0.0225/hour
- 2 tasks: ~$32.40/month (vs $64.66 for current size)
- **Savings:** 50% on compute costs
- **Trade-off:** Slightly reduced performance headroom per task, but 2 tasks provide redundancy

To implement:

```powershell
# Update stack with smaller task size
--parameters `
    ParameterKey=DesiredCount,ParameterValue=2 `
    ParameterKey=TaskCPU,ParameterValue=256 `
    ParameterKey=TaskMemory,ParameterValue=512 `
    # ... other parameters
```

---

## Deployment Flow Visualization

### With 1 Task (Current - Risk of Downtime)

```
Time    Task 1    Task 2    User Impact
----    ------    ------    -----------
T0      Running   None      ✓ Service available
T1      Running   Starting  ✓ Service available
T2      Running   Healthy   ✓ Service available (ALB adds Task 2)
T3      Draining  Healthy   ⚠ Possible connection drops
T4      Stopped   Healthy   ✓ Service available (only Task 2)
```

### With 2 Tasks (Recommended - Zero Downtime)

```
Time    Task 1    Task 2    Task 3    User Impact
----    ------    ------    ------    -----------
T0      Running   Running   None      ✓ Service available (2 tasks)
T1      Running   Running   Starting  ✓ Service available (2 tasks)
T2      Running   Running   Healthy   ✓ Service available (3 tasks, ALB adds Task 3)
T3      Draining  Running   Healthy   ✓ Service available (2 healthy tasks)
T4      Stopped   Running   Healthy   ✓ Service available (2 tasks)
T5      Running   Starting  Healthy   ✓ Service available (2 tasks)
T6      Running   Healthy   Healthy   ✓ Service available (3 tasks, ALB adds new Task 2)
T7      Running   Healthy   Draining  ✓ Service available (2 healthy tasks)
T8      Running   Healthy   Stopped   ✓ Service available (2 tasks)
```

**Key Difference:** With 2 tasks, there are ALWAYS at least 2 healthy tasks serving traffic during the entire deployment.

---

## Rollback Plan

If issues occur after implementation, you can quickly rollback:

### Rollback to Single Task

```powershell
aws cloudformation update-stack `
    --stack-name $StackName `
    --use-previous-template `
    --parameters `
        ParameterKey=DesiredCount,ParameterValue=1 `
        # ... other parameters use previous values
```

### Rollback Jenkins Schedule

Edit `Jenkinsfile` and revert the cron schedule:

```groovy
cron('H 16 * * *')  // Back to 4pm only
```

### Emergency Rollback via Jenkins

Use Jenkins "Rollback" action to deploy previous task definition:

1. Go to Jenkins job
2. Select "Build with Parameters"
3. Set `DEPLOY_ACTION` to `rollback`
4. Click "Build"

---

## Required IAM Permissions

If you encounter permission errors when using the AWS CLI or Console, ensure your IAM user/role has the following permissions.

### Minimum Required Permissions

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "CloudFormationStackUpdate",
            "Effect": "Allow",
            "Action": [
                "cloudformation:UpdateStack",
                "cloudformation:DescribeStacks",
                "cloudformation:DescribeStackEvents",
                "cloudformation:GetTemplate"
            ],
            "Resource": "arn:aws:cloudformation:us-east-1:378010131175:stack/taskactivity-*/*"
        },
        {
            "Sid": "ECSServiceUpdate",
            "Effect": "Allow",
            "Action": [
                "ecs:DescribeServices",
                "ecs:UpdateService",
                "ecs:DescribeClusters",
                "ecs:DescribeTasks"
            ],
            "Resource": [
                "arn:aws:ecs:us-east-1:378010131175:cluster/taskactivity-cluster",
                "arn:aws:ecs:us-east-1:378010131175:service/taskactivity-cluster/taskactivity-service"
            ]
        },
        {
            "Sid": "PassRoleForECS",
            "Effect": "Allow",
            "Action": "iam:PassRole",
            "Resource": [
                "arn:aws:iam::378010131175:role/ecsTaskRole",
                "arn:aws:iam::378010131175:role/ecsTaskExecutionRole"
            ]
        }
    ]
}
```

### How to Add Permissions via AWS Console

If you need to grant these permissions to a user or role:

#### For IAM Users:

1. **Navigate to IAM**
   - Go to: https://console.aws.amazon.com/iam
   - Click **Users** in the left sidebar

2. **Select User**
   - Find and click on the username that needs permissions
   - Click **Add permissions** → **Create inline policy**

3. **Create Policy**
   - Click **JSON** tab
   - Paste the permission JSON above
   - Update the AWS account ID (`378010131175`) if different
   - Click **Review policy**

4. **Name and Create**
   - Policy name: `TaskActivityDeploymentPermissions`
   - Description: `Permissions to update TaskActivity CloudFormation stack and ECS service`
   - Click **Create policy**

#### For IAM Roles:

1. **Navigate to IAM Roles**
   - Go to: https://console.aws.amazon.com/iam
   - Click **Roles** in the left sidebar

2. **Select Role**
   - Find and click on the role name
   - Click **Add permissions** → **Create inline policy**

3. **Create Policy** (same as above)
   - Click **JSON** tab
   - Paste the permission JSON
   - Click **Review policy**
   - Name: `TaskActivityDeploymentPermissions`
   - Click **Create policy**

### Alternative: Use Existing AWS Managed Policies

If inline policies are too restrictive, you can use AWS managed policies (less secure, broader permissions):

- **CloudFormation:** `AWSCloudFormationFullAccess` or `PowerUserAccess`
- **ECS:** `AmazonECS_FullAccess`

**To attach managed policies:**

1. Go to IAM → Users (or Roles)
2. Select the user/role
3. Click **Add permissions** → **Attach policies directly**
4. Search for and select the policy
5. Click **Add permissions**

### Verify Your Permissions

After granting permissions, test them:

**CLI Test:**
```powershell
# Test CloudFormation access
aws cloudformation describe-stacks --stack-name taskactivity-production

# Test ECS access
aws ecs describe-services `
    --cluster taskactivity-cluster `
    --services taskactivity-service
```

**Console Test:**
- Try accessing CloudFormation console and viewing your stack
- Try accessing ECS console and viewing your service

If you can view these resources, you have the minimum read permissions. Try updating the stack to verify write permissions.

---

## Testing Checklist

Before enabling multiple daily deployments in production:

- [ ] CloudFormation stack updated to `DesiredCount: 2`
- [ ] Verify 2 tasks are running: `aws ecs describe-services ...`
- [ ] Health check endpoints return healthy status
- [ ] Test deployment during business hours with 2 tasks
- [ ] Monitor CloudWatch metrics during test deployment
- [ ] Verify no 5xx errors during deployment
- [ ] Verify ALB always has 2 healthy targets during deployment
- [ ] Check database connection pool handles 2 tasks
- [ ] Review application logs for any issues
- [ ] Confirm deployment time is acceptable (~3-5 minutes)

---

## Monitoring and Alerts

### Key Metrics to Watch

1. **ECS Service Metrics**
    - Running task count (should always be 2)
    - Deployment status
    - Task CPU/Memory utilization

2. **Application Load Balancer**
    - Healthy target count (should always be 2)
    - Response time (should remain consistent)
    - 5xx error rate (should remain near zero)

3. **Application Metrics**
    - Spring Boot Actuator health status
    - Database connection pool usage
    - API response times

### Recommended CloudWatch Dashboard

Create a deployment monitoring dashboard with:

```powershell
# Export current ECS service metrics
aws cloudwatch get-dashboard --dashboard-name TaskActivityDeployment
```

Include widgets for:

- ECS running task count (time series)
- ALB healthy target count (time series)
- ALB target response time (time series)
- ECS CPU utilization (time series)
- ECS memory utilization (time series)
- RDS connections (time series)

---

## Frequently Asked Questions

### Q: Why do we need 2 tasks for zero downtime?

**A:** With only 1 task, during deployment the old task must be stopped before or shortly after the new task starts. This creates a brief window where only 1 task is handling all traffic, potentially causing connection issues. With 2 tasks, ECS can start a new task, verify it's healthy, then stop an old task while still maintaining 2 healthy tasks serving traffic.

### Q: What happens if a deployment fails?

**A:** The ECS Circuit Breaker is enabled, which automatically detects failed deployments and rolls back to the previous task definition. This prevents bad deployments from affecting production.

### Q: Will 2 tasks handle our current load?

**A:** Yes. Each task has 512 CPU units (0.5 vCPU) and 1GB memory. With 2 tasks, you have 1 full vCPU and 2GB memory total capacity, with built-in redundancy.

### Q: Can we deploy more than 3 times per day?

**A:** Yes. Once you have 2 tasks, you can deploy as frequently as needed without user impact. However, consider the overhead of deployment time and Jenkins resource usage.

### Q: What if we want to scale beyond 2 tasks?

**A:** The CloudFormation template supports up to 10 tasks via the parameter. You can increase `DesiredCount` at any time. Consider implementing auto-scaling based on CPU/memory utilization for production.

---

## Summary of Changes

### ✅ Completed

1. **Jenkins Schedule Updated** - Now deploys at 8am, 12pm, and 4pm

### 🔧 Required Before Production Use

1. **CloudFormation Update** - Set `DesiredCount: 2` in stack parameters
2. **Testing** - Verify zero-downtime deployment with 2 tasks during business hours
3. **Monitoring** - Set up CloudWatch alarms for deployment health

### 📊 Expected Outcome

- **User Impact:** Zero downtime during deployments
- **Deployment Frequency:** 3 times per day (8am, 12pm, 4pm)
- **Deployment Duration:** ~3-5 minutes
- **Cost Impact:** ~$32/month additional (~$1/day)
- **Reliability:** Auto-rollback on failures, always 2 healthy tasks

---

## Next Steps

1. **Immediate (Before Next Deployment):**
    - Update CloudFormation stack to 2 tasks
    - Verify 2 tasks are running and healthy

2. **Testing Phase (1-2 weeks):**
    - Monitor deployments at 8am, 12pm, 4pm
    - Track CloudWatch metrics
    - Review application logs for issues

3. **Production Ready:**
    - Document any issues encountered
    - Consider implementing auto-scaling
    - Review and optimize task resource allocation

---

## References

- **CloudFormation Template:** `/cloudformation/templates/infrastructure.yaml`
- **Jenkins Pipeline:** `/Jenkinsfile`
- **ECS Task Definition:** `/aws/taskactivity-task-definition.json`
- **AWS ECS Documentation:** https://docs.aws.amazon.com/AmazonECS/latest/developerguide/
- **Spring Boot Actuator:** https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

---

**Document Version:** 1.0  
**Last Updated:** January 27, 2026  
**Review Status:** Pending Implementation
