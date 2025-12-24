# Manual IAM Policy Update Instructions

## Issue

Your AWS user account doesn't have permission to modify IAM policies. This is normal security practice - users typically cannot modify their own permissions.

## Solution

An AWS administrator needs to update your IAM policy. Share these instructions with them.

---

## For AWS Administrator

### User Information

-   **AWS Account ID**: 378010131175
-   **User**: Dean
-   **Policy Name**: TaskActivityDeveloperPolicy

### What Needs to be Updated

The user needs S3 permissions added for managing documentation in the `taskactivity-docs` bucket.

### Option 1: Update via AWS Console (Easiest)

1. **Go to IAM Console**: https://console.aws.amazon.com/iam/

2. **Navigate to the User**:

    - Click "Users" in left sidebar
    - Click on user "Dean"
    - Click "Permissions" tab

3. **Find and Edit the Policy**:

    - Look for policy named "TaskActivityDeveloperPolicy"
    - Click the policy name or "Edit" button
    - Click "JSON" tab

4. **Update the JSON**:
    - Copy the entire contents of: `aws/taskactivity-developer-policy.json`
    - Replace the existing JSON in the console
    - Click "Review policy" → "Save changes"

### Option 2: Update via AWS CLI

If the administrator has AWS CLI configured with admin credentials:

```bash
# For inline user policy
aws iam put-user-policy \
  --user-name Dean \
  --policy-name TaskActivityDeveloperPolicy \
  --policy-document file://aws/taskactivity-developer-policy.json
```

Or if using a managed policy:

```bash
# Get the policy ARN first
POLICY_ARN="arn:aws:iam::378010131175:policy/TaskActivityDeveloperPolicy"

# Create new policy version
aws iam create-policy-version \
  --policy-arn $POLICY_ARN \
  --policy-document file://aws/taskactivity-developer-policy.json \
  --set-as-default
```

### What's Being Added

The updated policy adds these S3 permissions:

**For S3 Buckets** (ListBucket access):

-   `taskactivity-receipts-prod`
-   `taskactivity-docs` ← **NEW**
-   `taskactivity-logs-archive`

**For S3 Objects** (GetObject, PutObject, DeleteObject):

-   All objects in the above buckets
-   Additional PutObjectAcl/GetObjectAcl for docs bucket

### Why These Permissions Are Needed

1. **s3:ListBucket** - Required to list files in the docs bucket (current error)
2. **s3:GetObject** - Download documentation files
3. **s3:PutObject** - Upload documentation files
4. **s3:PutObjectAcl** - Set file permissions (for public docs)
5. **s3:DeleteObject** - Remove old documentation

These permissions allow the developer to:

-   Fix incorrect Content-Type metadata on existing Word/Excel files
-   Upload new documentation with proper content types
-   Manage the documentation bucket without admin intervention

### Security Considerations

✅ **Safe to Grant**:

-   Permissions are scoped to specific buckets only
-   No IAM modification permissions
-   No access to other AWS resources
-   Follows principle of least privilege

### Verification

After updating, the user can verify by running:

```powershell
# This should now work without errors
aws s3api list-objects-v2 --bucket taskactivity-docs
```

---

## After Administrator Updates Policy

Once your administrator has updated the policy, you can:

1. **Wait 1-2 minutes** for IAM changes to propagate

2. **Test access**:

    ```powershell
    aws s3api list-objects-v2 --bucket taskactivity-docs --max-items 5
    ```

3. **Run the content-type fix script**:
    ```powershell
    .\aws\fix-s3-content-types.ps1
    ```

## Alternative: Use IAM Administrator Role

If you have been granted an administrator role (via `sts:AssumeRole`), you can temporarily assume that role:

```powershell
# Set temporary credentials (ask your admin for the role ARN)
$roleArn = "arn:aws:iam::378010131175:role/AdminRole"
$credentials = aws sts assume-role --role-arn $roleArn --role-session-name PolicyUpdate | ConvertFrom-Json

# Set environment variables
$env:AWS_ACCESS_KEY_ID = $credentials.Credentials.AccessKeyId
$env:AWS_SECRET_ACCESS_KEY = $credentials.Credentials.SecretAccessKey
$env:AWS_SESSION_TOKEN = $credentials.Credentials.SessionToken

# Now run the update script
.\aws\update-iam-policy.ps1

# Clear temporary credentials when done
Remove-Item Env:\AWS_ACCESS_KEY_ID
Remove-Item Env:\AWS_SECRET_ACCESS_KEY
Remove-Item Env:\AWS_SESSION_TOKEN
```

## Questions?

-   Review the full policy: `aws/taskactivity-developer-policy.json`
-   Compare with current policy to see what's added
-   Check AWS IAM documentation: https://docs.aws.amazon.com/IAM/
