# AWS Cost Explorer Setup Guide

## Overview

This guide helps you set up AWS Cost Explorer access to check billing via CLI.

## Files Created

-   **check-billing.ps1** - PowerShell script to check AWS billing
-   **cost-explorer-policy.json** - IAM policy with required permissions

---

## Step 1: Enable Cost Explorer

1. Log into AWS Console
2. Navigate to **Billing and Cost Management**
3. Click **Cost Explorer** in left menu
4. Click **Enable Cost Explorer** button
5. Wait 24 hours for data to populate (one-time delay)

---

## Step 2: Add IAM Permissions (Manual via Portal)

### Option A: Attach Policy Directly to Your IAM User

1. Go to **IAM Console** → **Users**
2. Click on your username
3. Go to **Permissions** tab
4. Click **Add permissions** → **Create inline policy**
5. Click **JSON** tab
6. Copy and paste contents from `cost-explorer-policy.json`
7. Click **Review policy**
8. Name it: `CostExplorerAccess`
9. Click **Create policy**

### Option B: Create Managed Policy and Attach

1. Go to **IAM Console** → **Policies**
2. Click **Create policy**
3. Click **JSON** tab
4. Copy and paste contents from `cost-explorer-policy.json`
5. Click **Next: Tags** (optional)
6. Click **Next: Review**
7. Name: `CostExplorerReadOnly`
8. Description: `Allows read access to Cost Explorer and billing data`
9. Click **Create policy**

Then attach to your user:

1. Go to **IAM Console** → **Users**
2. Click on your username
3. Click **Add permissions** → **Attach existing policies directly**
4. Search for `CostExplorerReadOnly`
5. Check the box and click **Next**
6. Click **Add permissions**

---

## Step 3: Verify Setup

Run the following to test:

```powershell
# Test AWS credentials
aws sts get-caller-identity

# Test Cost Explorer access
cd aws
.\check-billing.ps1
```

---

## Usage Examples

```powershell
# Basic usage - current month total
.\check-billing.ps1

# Detailed breakdown by service
.\check-billing.ps1 -Detailed

# Include forecast for rest of month
.\check-billing.ps1 -Forecast

# Both detailed and forecast
.\check-billing.ps1 -Detailed -Forecast

# Last month's costs
.\check-billing.ps1 -LastMonth

# Last month detailed
.\check-billing.ps1 -LastMonth -Detailed
```

---

## Troubleshooting

### "AccessDenied" or "not authorized" errors

-   Verify Cost Explorer is enabled (Step 1)
-   Verify IAM permissions are attached (Step 2)
-   Wait a few minutes for IAM changes to propagate

### "Cost Explorer is not enabled"

-   Go to Billing Console → Cost Explorer → Enable
-   Wait 24 hours for initial data population

### "AWS CLI is not installed"

```powershell
# Install via Chocolatey
choco install awscli

# Or download from:
# https://aws.amazon.com/cli/
```

### "Unable to verify AWS credentials"

```powershell
# Configure AWS CLI
aws configure

# Enter your:
# - AWS Access Key ID
# - AWS Secret Access Key
# - Default region (e.g., us-east-1)
# - Default output format (json)
```

---

## What the Script Shows

-   **Month-to-Date Total**: Total costs from start of current month to today
-   **Costs by Service** (with `-Detailed`): Breakdown showing which AWS services cost what
-   **Forecast** (with `-Forecast`): Projected costs for rest of month and month total
-   **Last Month** (with `-LastMonth`): Previous month's final costs

---

## Security Note

The `cost-explorer-policy.json` provides **read-only** access to:

-   Cost and usage data
-   Billing information
-   Cost forecasts

It does **NOT** grant permissions to:

-   Modify billing settings
-   Make purchases
-   Change payment methods
-   Modify AWS resources

---

## Schedule Regular Checks (Optional)

Create a scheduled task to run daily:

```powershell
# Create scheduled task to run daily at 9 AM
$action = New-ScheduledTaskAction -Execute "powershell.exe" `
    -Argument "-File C:\Users\deana\GitHub\ActivityTracking\aws\check-billing.ps1 -Detailed"

$trigger = New-ScheduledTaskTrigger -Daily -At 9am

Register-ScheduledTask -Action $action -Trigger $trigger `
    -TaskName "AWS Billing Check" `
    -Description "Daily AWS billing check"
```

---

## Additional Resources

-   [AWS Cost Explorer Documentation](https://docs.aws.amazon.com/cost-management/latest/userguide/ce-what-is.html)
-   [AWS CLI Cost Explorer Commands](https://docs.aws.amazon.com/cli/latest/reference/ce/)
-   [IAM Policies Documentation](https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies.html)
