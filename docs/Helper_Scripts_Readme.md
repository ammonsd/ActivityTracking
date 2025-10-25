# CloudWatch Logs Export Helper Scripts

This directory contains scripts to manage CloudWatch Logs exports to S3.

## Automated Log Exports (Recommended)

**✅ AWS Lambda Automation** - Logs are now automatically exported to S3 daily at 2:00 AM UTC using an AWS Lambda function triggered by EventBridge Scheduler.

**Setup:** See `aws/LAMBDA_CONSOLE_SETUP_GUIDE.md` for complete setup instructions.

**What it does:**
- Runs entirely in AWS (no PC dependency)
- Exports the previous day's logs to S3 automatically
- Organizes logs by date: `s3://taskactivity-logs-archive/cloudwatch-exports/YYYY-MM-DD/`
- Can be triggered manually anytime via AWS Console or CLI

**Manual trigger:**
```powershell
aws lambda invoke --function-name TaskActivityLogExporter --region us-east-1 output.json
```

---

## Manual Export Scripts (Alternative)

If you need to export logs manually or haven't set up Lambda automation yet, use these scripts.

**Important:** Run all scripts from the project root directory (`C:\Users\deana\GitHub\ActivityTracking`), not from the aws folder.

### 1. `export-logs-to-s3.ps1` - Export CloudWatch Logs to S3

Exports CloudWatch logs from `/ecs/taskactivity` to the S3 bucket `taskactivity-logs-archive`.

**Usage Examples:**

```powershell
# Export last 24 hours (default)
.\aws\export-logs-to-s3.ps1

# Export last 7 days
.\aws\export-logs-to-s3.ps1 -Days 7

# Export last 30 days
.\aws\export-logs-to-s3.ps1 -Days 30

# Export specific date range
.\aws\export-logs-to-s3.ps1 -StartDate "2025-10-01" -EndDate "2025-10-15"

# Export with custom S3 prefix
.\aws\export-logs-to-s3.ps1 -Days 7 -Prefix "weekly-backup/2025-10"
```

**What it does:**
- Creates a CloudWatch Logs export task
- Exports logs to S3 with automatic organization
- Provides task ID for tracking
- Shows commands to check status and download logs

**Time to complete:** 5-15 minutes depending on log volume

---

### 2. Quick Commands (No Script Needed)

**List all archived logs:**
```powershell
aws s3 ls s3://taskactivity-logs-archive/cloudwatch-exports/ --recursive --region us-east-1 --human-readable --summarize
```

**Download all logs:**
```powershell
aws s3 cp s3://taskactivity-logs-archive/cloudwatch-exports/ ./downloaded-logs --recursive --region us-east-1
```

**Download specific date:**
```powershell
aws s3 cp s3://taskactivity-logs-archive/cloudwatch-exports/2025-10-16/ ./logs-2025-10-16 --recursive --region us-east-1
```

**View a downloaded log (decompress first):**
```powershell
# Decompress
gunzip downloaded-logs/*/000000.gz

# View
Get-Content downloaded-logs/*/000000
```

---

## Common Export Scenarios

### Weekly Export (Recommended)

Run this every Monday to export the previous week:

```powershell
.\export-logs-to-s3.ps1 -Days 7 -Prefix "weekly/$(Get-Date -Format 'yyyy-MM-dd')"
```

### Monthly Archive

Run this at the start of each month:

```powershell
$lastMonth = (Get-Date).AddMonths(-1)
$startDate = Get-Date $lastMonth -Day 1 -Format "yyyy-MM-dd"
$endDate = Get-Date $lastMonth -Day ([DateTime]::DaysInMonth($lastMonth.Year, $lastMonth.Month)) -Format "yyyy-MM-dd"

.\export-logs-to-s3.ps1 -StartDate $startDate -EndDate $endDate -Prefix "monthly/$($lastMonth.ToString('yyyy-MM'))"
```

### Emergency Export (Last Hour)

Quick export for immediate analysis:

```powershell
$toTime = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$fromTime = [DateTimeOffset]::UtcNow.AddHours(-1).ToUnixTimeMilliseconds()

aws logs create-export-task `
    --log-group-name "/ecs/taskactivity" `
    --from $fromTime `
    --to $toTime `
    --destination "taskactivity-logs-archive" `
    --destination-prefix "emergency/$(Get-Date -Format 'yyyy-MM-dd-HHmm')" `
    --region us-east-1
```

---

## Checking Export Status

### Check specific export task:
```powershell
aws logs describe-export-tasks --task-id <task-id> --region us-east-1
```

### List all recent export tasks:
```powershell
aws logs describe-export-tasks --region us-east-1
```

### Status codes:
- **PENDING**: Task is queued
- **RUNNING**: Export in progress
- **COMPLETED**: Export finished successfully  
- **FAILED**: Export failed (check error message)

---

## Working with Exported Logs

### 1. Download Logs

```powershell
# Download all exports
aws s3 cp s3://taskactivity-logs-archive/cloudwatch-exports/ ./logs --recursive --region us-east-1

# Download specific export task
aws s3 cp s3://taskactivity-logs-archive/cloudwatch-exports/<task-id>/ ./logs --recursive --region us-east-1
```

### 2. Decompress Logs

```powershell
# Windows (requires gzip/gunzip from Git Bash, WSL, or Cygwin)
Get-ChildItem -Path ./logs -Filter *.gz -Recurse | ForEach-Object {
    gunzip $_.FullName
}

# Or use 7-Zip if installed
Get-ChildItem -Path ./logs -Filter *.gz -Recurse | ForEach-Object {
    & "C:\Program Files\7-Zip\7z.exe" x $_.FullName -o$_.Directory
}
```

### 3. Search Logs

```powershell
# Search for error messages
Get-ChildItem -Path ./logs -Recurse | Select-String -Pattern "ERROR"

# Search for specific text
Get-ChildItem -Path ./logs -Recurse | Select-String -Pattern "UserService"

# Count errors
(Get-ChildItem -Path ./logs -Recurse | Select-String -Pattern "ERROR").Count
```

### 4. Analyze Logs

```powershell
# View first 100 lines
Get-Content ./logs/ecs-taskactivity-*/000000 | Select-Object -First 100

# View last 100 lines
Get-Content ./logs/ecs-taskactivity-*/000000 | Select-Object -Last 100

# Filter by date/time
Get-Content ./logs/ecs-taskactivity-*/000000 | Where-Object { $_ -match "2025-10-16" }
```

---

## Cost Optimization

### Current Setup

- **CloudWatch**: 30-day retention at $0.03/GB
- **S3 Standard**: 0-90 days at $0.023/GB  
- **Glacier Flexible**: 90-365 days at $0.0036/GB (84% savings)
- **Glacier Deep Archive**: 365+ days at $0.00099/GB (96% savings)

### Cost Estimates

**Scenario 1: Small Application (1 GB/month)**
- CloudWatch: $0.03/month
- S3 Year 1: $0.28/month average
- **Total: ~$0.31/month or $3.72/year**

**Scenario 2: Medium Application (10 GB/month)**
- CloudWatch: $0.30/month
- S3 Year 1: $2.76/month average
- **Total: ~$3.06/month or $36.72/year**

**Scenario 3: Large Application (100 GB/month)**
- CloudWatch: $3.00/month
- S3 Year 1: $27.60/month average
- **Total: ~$30.60/month or $367.20/year**

### Tips to Reduce Costs

1. **Export selectively**: Only export what you need (weekly vs daily)
2. **Clean up old CloudWatch logs**: 30-day retention is often sufficient
3. **Use S3 lifecycle policies**: Automatic transitions save 84-96%
4. **Delete unnecessary exports**: Remove test exports
5. **Compress before export**: Already done automatically by AWS

---

## Automation

### Option 1: Windows Task Scheduler

Create a scheduled task to run exports weekly:

```powershell
# Create a scheduled task (run as Administrator)
$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-File C:\Users\deana\GitHub\ActivityTracking\aws\export-logs-to-s3.ps1 -Days 7"
$trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek Monday -At 2am
$principal = New-ScheduledTaskPrincipal -UserID "NT AUTHORITY\SYSTEM" -LogonType ServiceAccount -RunLevel Highest
Register-ScheduledTask -Action $action -Trigger $trigger -Principal $principal -TaskName "Export CloudWatch Logs Weekly" -Description "Weekly export of ECS application logs to S3"
```

### Option 2: AWS EventBridge + Lambda

For fully automated cloud-based exports:

1. Create Lambda function with export logic
2. Set up EventBridge rule to trigger weekly
3. Lambda calls `logs:CreateExportTask`
4. No local machine required

**Cost**: ~$0.20/month for Lambda execution

---

## Troubleshooting

### Export task fails with "AccessDenied"

**Problem**: IAM permissions missing

**Solution**: Ensure IAM user has `CloudWatchLogsExportPolicy`:
```powershell
# Check current permissions
aws iam list-attached-user-policies --user-name Dean --region us-east-1
aws iam list-user-policies --user-name Dean --region us-east-1
```

### Export task succeeds but no files in S3

**Problem**: S3 bucket policy not configured

**Solution**: Verify bucket policy allows CloudWatch Logs service:
```powershell
aws s3api get-bucket-policy --bucket taskactivity-logs-archive --region us-east-1
```

### Cannot download files

**Problem**: Missing S3 read permissions

**Solution**: Add `s3:GetObject` permission to IAM user

### Lifecycle policy not working

**Problem**: Policy misconfigured or not enough time passed

**Solution**: 
- Check lifecycle rules in S3 Console
- Wait 24-48 hours for first transition
- Verify objects are at least as old as transition days

---

## Files in This Directory

- **export-logs-to-s3.ps1** - Main export script
- **cloudwatch-export-policy.json** - IAM policy for log exports
- **s3-bucket-policy.json** - S3 bucket policy for CloudWatch access
- **HELPER_SCRIPTS_README.md** - This file

## Related Documentation

- **localdocs/S3_LOG_ARCHIVAL_GUIDE.md** - Complete S3 setup guide
- **localdocs/CLOUDWATCH_LOGGING_GUIDE.md** - CloudWatch configuration
- **localdocs/IAM_CLOUDWATCH_EXPORT_PERMISSIONS.md** - IAM setup guide

---

## Quick Reference

**Export last week:**
```powershell
.\export-logs-to-s3.ps1 -Days 7
```

**List archived logs:**
```powershell
aws s3 ls s3://taskactivity-logs-archive/cloudwatch-exports/ --recursive --human-readable --summarize
```

**Download everything:**
```powershell
aws s3 cp s3://taskactivity-logs-archive/cloudwatch-exports/ ./logs --recursive
```

**Search logs:**
```powershell
Get-ChildItem -Path ./logs -Recurse | Select-String -Pattern "ERROR"
```

---

✅ **Your logging infrastructure is ready!**

Questions? Check the related documentation or review AWS CloudWatch Logs documentation.
