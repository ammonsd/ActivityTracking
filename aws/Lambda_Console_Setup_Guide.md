# AWS Console Setup: Automated CloudWatch Log Exports using Lambda

This guide walks you through setting up automated daily CloudWatch log exports using the AWS Console.

## Prerequisites

-   AWS Console access with administrator privileges
-   The Lambda function code is in: `aws/lambda-export-logs.py`

---

## Step 1: Create IAM Role for Lambda

1. Go to **IAM Console**: https://console.aws.amazon.com/iam/
2. Click **Roles** in the left sidebar
3. Click **Create role**
4. Select **AWS service** → **Lambda** → Click **Next**
5. Search for and select: **AWSLambdaBasicExecutionRole**
6. Click **Next**
7. Role name: `TaskActivityLogExportRole`
8. Description: `Role for Lambda to export CloudWatch logs to S3`
9. Click **Create role**

### Add Custom Permissions to the Role

1. Find the role you just created: `TaskActivityLogExportRole`
2. Click on it
3. Click **Add permissions** → **Create inline policy**
4. Click the **JSON** tab
5. Paste this policy:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateExportTask",
                "logs:DescribeExportTasks",
                "logs:DescribeLogGroups",
                "logs:DescribeLogStreams"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": ["s3:PutObject", "s3:GetBucketLocation"],
            "Resource": [
                "arn:aws:s3:::taskactivity-logs-archive",
                "arn:aws:s3:::taskactivity-logs-archive/*"
            ]
        }
    ]
}
```

6. Click **Review policy**
7. Name: `CloudWatchLogExportPolicy`
8. Click **Create policy**

---

## Step 2: Create Lambda Function

1. Go to **Lambda Console**: https://console.aws.amazon.com/lambda/
2. Click **Create function**
3. Choose **Author from scratch**
4. Configure:
    - **Function name**: `TaskActivityLogExporter`
    - **Runtime**: **Python 3.12**
    - **Architecture**: `x86_64`
    - **Execution role**: Choose "Use an existing role"
    - **Existing role**: Select `TaskActivityLogExportRole`
5. Click **Create function**

### Add the Function Code

1. In the **Code** tab, delete the default code
2. Copy the entire contents from `c:\Users\deana\GitHub\ActivityTracking\aws\lambda-export-logs.py`
3. Paste it into the Lambda code editor
4. Click **Deploy**

### Configure Environment Variables

1. Click the **Configuration** tab
2. Click **Environment variables** in the left menu
3. Click **Edit**
4. Click **Add environment variable** three times and add:
    - Key: `LOG_GROUP_NAME`, Value: `/ecs/taskactivity`
    - Key: `S3_BUCKET`, Value: `taskactivity-logs-archive`
    - Key: `EXPORT_DAYS`, Value: `1`
5. Click **Save**

### Adjust Function Settings

1. Still in **Configuration** tab
2. Click **General configuration** in the left menu
3. Click **Edit**
4. Set **Timeout** to `1 min 0 sec`
5. Set **Memory** to `256 MB`
6. Click **Save**

---

## Step 3: Test the Lambda Function

1. Go back to the **Code** tab
2. Click **Test**
3. Create a test event:
    - **Event name**: `TestExport`
    - **Event JSON**: `{}` (just empty braces)
4. Click **Save**
5. Click **Test** again to run it
6. Check the execution results - you should see:
    - Status: **Succeeded**
    - Response showing task ID and S3 destination

---

## Step 4: Create EventBridge Schedule

1. Go to **EventBridge Console**: https://console.aws.amazon.com/events/
2. Click **Rules** in the left sidebar
3. Click **Create rule**
4. Configure the rule:
    - **Name**: `TaskActivityDailyLogExport`
    - **Description**: `Daily CloudWatch log export to S3 at 2:00 AM UTC`
    - **Event bus**: `default`
    - **Rule type**: **Schedule**
5. Click **Next**

### Define Schedule Pattern

1. Choose **A schedule that runs at a regular rate, such as every 10 minutes**
2. Select **Cron-based schedule**
3. Enter: `0 2 * * ? *`
    - This runs daily at 2:00 AM UTC
4. Click **Next**

### Select Target

1. **Target types**: Select **AWS service**
2. **Select a target**: Choose **Lambda function**
3. **Function**: Select `TaskActivityLogExporter`
4. Click **Next**
5. Click **Next** again (skip tags)
6. Click **Create rule**

---

## Step 5: Verify Setup

### Check the Schedule is Active

1. In EventBridge Rules, find `TaskActivityDailyLogExport`
2. Status should be **Enabled**

### Manually Trigger to Test

1. Click on the rule name
2. Click **Test schedule**
3. Or use the AWS CLI:

```powershell
aws lambda invoke --function-name TaskActivityLogExporter --region us-east-1 output.json
cat output.json
```

### Monitor Execution

1. Go to **CloudWatch Console**: https://console.aws.amazon.com/cloudwatch/
2. Click **Log groups**
3. Find `/aws/lambda/TaskActivityLogExporter`
4. Click on the latest log stream to see execution details

### Verify Exports in S3

Wait 5-10 minutes after triggering, then:

```powershell
aws s3 ls s3://taskactivity-logs-archive/cloudwatch-exports/ --recursive --region us-east-1 | Select-Object -Last 20
```

---

## Management Commands

### Disable the Schedule

```powershell
aws events disable-rule --name TaskActivityDailyLogExport --region us-east-1
```

### Enable the Schedule

```powershell
aws events enable-rule --name TaskActivityDailyLogExport --region us-east-1
```

### Test Lambda Manually

```powershell
aws lambda invoke --function-name TaskActivityLogExporter --region us-east-1 output.json
```

### View Lambda Logs

```powershell
aws logs tail /aws/lambda/TaskActivityLogExporter --follow --region us-east-1
```

### Change Export Frequency

To change from daily to a different schedule:

1. Go to EventBridge → Rules → `TaskActivityDailyLogExport`
2. Click **Edit**
3. Change the cron expression:
    - **Every 6 hours**: `0 */6 * * ? *`
    - **Twice daily (2am and 2pm)**: `0 2,14 * * ? *`
    - **Weekly on Sunday at 2am**: `0 2 ? * SUN *`
4. Click **Save**

---

## Cleanup (If Needed)

To remove everything:

1. **EventBridge**: Delete rule `TaskActivityDailyLogExport`
2. **Lambda**: Delete function `TaskActivityLogExporter`
3. **IAM**: Delete role `TaskActivityLogExportRole`

---

## Summary

✅ **Lambda Function**: `TaskActivityLogExporter`  
✅ **Schedule**: Daily at 2:00 AM UTC  
✅ **Exports**: Last 1 day of logs from `/ecs/taskactivity`  
✅ **Destination**: `s3://taskactivity-logs-archive/cloudwatch-exports/YYYY-MM-DD/`  
✅ **No PC dependency**: Runs entirely in AWS

After setup, you can **remove the Windows scheduled task** from your PC - it's no longer needed!
