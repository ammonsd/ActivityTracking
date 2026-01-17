# Jenkins API Token Maintenance Guide

## Quick Reference

**Token Renewal in 3 Steps (30 days from last generation):**

```powershell
# 1. Get production secret from AWS Secrets Manager
$secretJson = aws secretsmanager get-secret-value --secret-id "taskactivity/jwt/secret" --region us-east-1 --query 'SecretString' --output text | ConvertFrom-Json
$env:JWT_SECRET = $secretJson.secret

# 2. Generate new token
.\scripts\generate-token.ps1

# 3. Update Jenkins credential at http://172.27.85.228:8081
# Manage Jenkins → Credentials → System → Global → jenkins-api-token → Update → Paste token → Save
```

---

## Overview

The Jenkins build notifications use JWT (JSON Web Token) authentication to securely communicate with the production application. These tokens have a **30-day expiration** for security purposes and must be renewed regularly.

**⚠️ IMPORTANT**: Set a reminder to regenerate the Jenkins API token **every 30 days** to prevent notification failures.

**📍 Token Lifetime Expires**: February 16, 2026 (30 days from January 17, 2026)

---

## Token Expiration Settings

- **Current Token Lifetime**: 30 days (2,592,000,000 milliseconds)
- **Configured In**: AWS ECS environment variable `JWT_EXPIRATION=2592000000`
- **Renewal Frequency**: Every 30 days
- **Impact if Expired**: Jenkins build/deploy email notifications will fail with HTTP 401 errors

---

## How to Know When Token Needs Renewal

### Signs of Expired Token

1. **Jenkins Console Output** shows:

    ```
    ⚠ Notification failed with HTTP 401
    Response: {"error":"Unauthorized","message":"Invalid or expired JWT token."}
    ```

2. **No email notifications** received for successful builds or deployments

3. **30 days have passed** since last token generation (check your calendar reminder)

---

## Step-by-Step Token Renewal Process

### Step 1: Generate New JWT Token Using Script

Since the application doesn't currently have a token generation UI, use the provided script:

1. **Retrieve production JWT_SECRET from AWS Secrets Manager**:

    ```powershell
    # Get the secret from AWS Secrets Manager (stored as JSON)
    $secretJson = aws secretsmanager get-secret-value `
        --secret-id "taskactivity/jwt/secret" `
        --region us-east-1 `
        --query 'SecretString' `
        --output text | ConvertFrom-Json

    # Extract the actual secret value
    $env:JWT_SECRET = $secretJson.secret

    # Verify (shows first 10 characters only for security)
    Write-Host "JWT_SECRET: $($env:JWT_SECRET.Substring(0, 10))..."
    ```

2. **Run the token generator script**:

    ```powershell
    # From project root
    .\scripts\generate-token.ps1
    ```

3. **Copy the generated token** (entire line shown in yellow output)
    - The script will display:
        - Token details (subject, permissions, expiration date)
        - The full JWT token string
        - Next steps
    - Copy the full JWT token (it's very long - about 300+ characters)
    - Keep it secure - you'll need it in Step 2

**Note**: The JWT_SECRET is stored in AWS Secrets Manager (not as an ECS environment variable). This provides better security and centralized secret management.

---

### Step 2: Update Jenkins Credential

1. **Access Jenkins**:

    ```
    http://172.27.85.228:8081
    ```

2. **Navigate to Credentials**:
    - Click **Manage Jenkins** (left sidebar)
    - Click **Credentials**
    - Click **System**
    - Click **Global credentials (unrestricted)**

3. **Update the jenkins-api-token credential**:
    - Find credential with ID: `jenkins-api-token`
    - Click the credential name
    - Click **Update** (left sidebar)
    - In the **Secret** field, paste the new JWT token
    - Click **Save**

---

### Step 3: Test the New Token

1. **Trigger a test build**:

    ```bash
    # From project root
    .\jenkins\trigger-build.sh

    # Or from Windows PowerShell
    wsl bash -c "cd /mnt/c/Users/deana/GitHub/ActivityTracking && ./jenkins/trigger-build.sh"
    ```

2. **Check the build console output**:
    - Go to Jenkins → TaskActivity → Latest build → Console Output
    - Look for: `✓ Build success notification sent successfully`
    - Should NOT see: `⚠ Notification failed with HTTP 401`

3. **Verify email received**:
    - Check your email for Jenkins build notification
    - Subject should be: "✅ Jenkins Build Successful - Build #XXX"

---

## Automation Options

### Option 1: Calendar Reminder (Recommended)

Set a recurring reminder every 30 days:

- **Calendar**: Google Calendar, Outlook, etc.
- **Reminder Title**: "Renew Jenkins API Token"
- **Recurrence**: Every 30 days
- **Include**: Link to this document

### Option 2: Token Refresh Script (Advanced)

Create an automated token refresh script (requires admin API access):

```powershell
# Script: scripts/renew-jenkins-token.ps1

<#
.SYNOPSIS
    Automatically renews Jenkins API token and updates Jenkins credential.

.DESCRIPTION
    Generates a new JWT token from production API and updates the Jenkins credential.
    Requires admin credentials for production app and Jenkins.
#>

[CmdletBinding()]
param()

# Configuration
$ProductionApiUrl = "https://taskactivitytracker.com/task-activity/api/admin/tokens"
$JenkinsUrl = "http://172.27.85.228:8081"
$JenkinsCredentialId = "jenkins-api-token"

# TODO: Implement token generation and Jenkins credential update
# This requires Jenkins API access and admin token for production

Write-Host "⚠ This script is a template - implementation required" -ForegroundColor Yellow
```

### Option 3: Monitoring Alert

Set up monitoring to alert when token is nearing expiration (requires custom implementation).

---

## Troubleshooting

### Problem: "Invalid or expired JWT token" Error

**Cause**: Token has exceeded 30-day lifetime

**Solution**: Follow renewal process above

---

### Problem: New Token Still Returns 401

**Possible Causes**:

1. **Token not properly copied** (extra spaces, truncated)
    - Solution: Copy the entire token line from the yellow output
    - The token is ~300 characters - make sure you got all of it

2. **Token generated with wrong JWT_SECRET**
    - Solution: Verify you retrieved the secret from AWS Secrets Manager (not `.env.local`)
    - Local and production secrets are different!

3. **Wrong permissions in token**
    - Solution: Token must have `permissions: "JENKINS:NOTIFY"`
    - The script automatically sets this correctly

4. **JWT_SECRET changed in production** (rare)
    - Check last changed date:
        ```powershell
        aws secretsmanager describe-secret `
            --secret-id "taskactivity/jwt/secret" `
            --region us-east-1 `
            --query 'LastChangedDate'
        ```

**Solutions**:

1. Regenerate token carefully, ensuring full copy
2. Verify token has `JENKINS:NOTIFY` permission
3. Check production app JWT_SECRET hasn't changed

---

### Problem: Can't Access Production Admin Interface

**Solution**:

- Use database direct access to verify token generation
- Check with team for admin credentials
- Review admin access logs for authentication issues

---

### Problem: Jenkins Build Succeeds but No Email

**Check List**:

1. ✓ Token is valid (< 30 days old)
2. ✓ Jenkins credential updated
3. ✓ Console shows notification success (not 401)
4. ✓ Email addresses configured correctly
5. ✓ AWS SES email addresses verified
6. ✓ Network connectivity to production app

---

## Token Security Best Practices

### DO:

- ✓ Store tokens only in Jenkins credentials (never commit to Git)
- ✓ Use minimum required permissions (`JENKINS:NOTIFY` only)
- ✓ Regenerate immediately if token suspected compromised
- ✓ Keep this document updated with process changes

### DON'T:

- ✗ Share tokens via email or chat
- ✗ Store tokens in plain text files
- ✗ Use personal admin tokens for Jenkins (use service account)
- ✗ Extend token lifetime beyond 30 days without security review

---

## Quick Reference Commands

### Check Current Token Age

```bash
# View last Jenkins build console output for token usage
wsl -u root journalctl -u jenkins -n 100 | grep -i "notification"
```

### Get Jenkins Credential Info

```bash
# List all Jenkins credentials (requires Jenkins CLI)
java -jar jenkins-cli.jar -s http://172.27.85.228:8081/ \
  -auth admin:YOUR_PASSWORD \
  list-credentials system::system::jenkins
```

### Test Production API Connectivity

```bash
# Test if production app is responding
curl -I https://taskactivitytracker.com/task-activity/actuator/health
```

---

## Related Documentation

- **[generate-token.ps1](../scripts/generate-token.ps1)** - PowerShell script that generates JWT tokens (no Java compilation needed!)
- [Update ECS Environment Variables](../aws/Update_ECS_Environment_Variables.md) - How to update JWT_EXPIRATION
- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/) - Managing secrets in AWS

---

## Script Details: generate-token.ps1

The token generation script (`scripts/generate-token.ps1`) is a pure PowerShell solution that:

- ✅ **No Java compilation required** - Uses .NET cryptography libraries
- ✅ **Automatic JWT_SECRET loading** - Reads from `.env.local` if not set in environment
- ✅ **Proper JWT structure** - Creates HS256-signed tokens with correct claims
- ✅ **30-day expiration** - Matches the JWT_EXPIRATION setting in ECS
- ✅ **Clear output** - Shows token details, expiration date, and next steps

**What it generates:**

- Subject: `JENKINS_SERVICE`
- Permission: `JENKINS:NOTIFY`
- Type: `SERVICE_ACCOUNT`
- Algorithm: HS256 (HMACSHA256)
- Lifetime: 30 days from generation

**Example output:**

```
=== Jenkins JWT Token Generator (PowerShell) ===

JWT Token (copy entire line below):
----------------------------------------------------------------------
eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoiU0VSVklDRV9BQ0NPVU5U...
----------------------------------------------------------------------

Token expires on: 02/16/2026 14:29:14
```

- [AWS SES Setup Guide](../aws/AWS_SES_Setup_Guide.md) - Email configuration
- [Jenkinsfile Documentation](../Jenkinsfile) - Lines 635-770 (notification code)

---

## Maintenance History

| Date       | Action                        | Notes                              |
| ---------- | ----------------------------- | ---------------------------------- |
| 2026-01-17 | Initial token renewal process | Extended token lifetime to 30 days |
| 2026-01-17 | Updated JWT_EXPIRATION to 30d | Changed from 24h to 2592000000 ms  |

---

## Next Renewal Due

**📅 Next Token Renewal**: **{{ DATE + 30 DAYS }}**

_Update this section each time you renew the token_

---

**Document Author**: Dean Ammons  
**Last Updated**: January 2026  
**Version**: 1.0
