# Update IAM Policy for S3 Docs Bucket Access
# This script updates the IAM user/role policy to include permissions for the taskactivity-docs bucket
# Run this script after updating the taskactivity-developer-policy.json file

param(
    [Parameter(Mandatory=$false)]
    [string]$UserName = "Dean",
    
    [Parameter(Mandatory=$false)]
    [string]$PolicyName = "TaskActivityDeveloperPolicy"
)

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "IAM Policy Update for S3 Docs Bucket Access" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Continue"

# Check if AWS CLI is configured
Write-Host "Checking AWS CLI configuration..." -ForegroundColor Yellow
try {
    $identityJson = aws sts get-caller-identity 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "AWS CLI not configured"
    }
    $identity = $identityJson | ConvertFrom-Json
    Write-Host "✓ AWS CLI configured" -ForegroundColor Green
    Write-Host "  Account: $($identity.Account)" -ForegroundColor Gray
    Write-Host "  User: $($identity.Arn)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "✗ AWS CLI not configured or not authenticated" -ForegroundColor Red
    Write-Host "  Run: aws configure" -ForegroundColor Yellow
    exit 1
}

# Check if policy file exists
$policyFile = Join-Path $PSScriptRoot "taskactivity-developer-policy.json"
if (-not (Test-Path $policyFile)) {
    Write-Host "✗ Policy file not found: $policyFile" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Policy file found: $policyFile" -ForegroundColor Green
Write-Host ""

# Validate JSON
Write-Host "Validating policy JSON..." -ForegroundColor Yellow
try {
    $policyContent = Get-Content $policyFile -Raw
    $null = $policyContent | ConvertFrom-Json
    Write-Host "✓ Policy JSON is valid" -ForegroundColor Green
} catch {
    Write-Host "✗ Invalid JSON in policy file: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Policy will be updated for:" -ForegroundColor Yellow
Write-Host "  User Name: $UserName" -ForegroundColor Cyan
Write-Host "  Policy Name: $PolicyName" -ForegroundColor Cyan
Write-Host ""

# Get AWS account ID
$accountId = $identity.Account

# Try to update inline user policy
Write-Host "================================================" -ForegroundColor Yellow
Write-Host "Attempting to Update IAM Policy" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "Trying to update inline policy for user $UserName..." -ForegroundColor Yellow

$result = aws iam put-user-policy `
    --user-name $UserName `
    --policy-name $PolicyName `
    --policy-document "file://$policyFile" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Successfully updated inline policy!" -ForegroundColor Green
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Green
    Write-Host "Policy Update Complete" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "The updated policy includes permissions for:" -ForegroundColor Yellow
    Write-Host "  ✓ S3 receipts bucket (taskactivity-receipts-prod)" -ForegroundColor Green
    Write-Host "  ✓ S3 docs bucket (taskactivity-docs)" -ForegroundColor Green
    Write-Host "  ✓ S3 logs bucket (taskactivity-logs-archive)" -ForegroundColor Green
    Write-Host ""
    Write-Host "You can now run the fix-s3-content-types.ps1 script" -ForegroundColor Cyan
    Write-Host ""
    exit 0
}

# Access denied - provide manual instructions
if ($result -match "AccessDenied|not authorized") {
    Write-Host "✗ ACCESS DENIED" -ForegroundColor Red
    Write-Host ""
    Write-Host "You don't have permission to modify IAM policies." -ForegroundColor Yellow
    Write-Host "This is normal - IAM users typically cannot modify their own permissions." -ForegroundColor Gray
    Write-Host ""
    Write-Host "See MANUAL_IAM_UPDATE.md for complete instructions" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "QUICK INSTRUCTIONS FOR YOUR AWS ADMINISTRATOR" -ForegroundColor Cyan
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Update IAM Policy via AWS Console:" -ForegroundColor White
    Write-Host ""
    Write-Host "1. Go to: https://console.aws.amazon.com/iam/" -ForegroundColor Gray
    Write-Host "2. Click 'Users' → '$UserName'" -ForegroundColor Gray
    Write-Host "3. Click 'Permissions' tab" -ForegroundColor Gray
    Write-Host "4. Find policy '$PolicyName' and click 'Edit'" -ForegroundColor Gray
    Write-Host "5. Click 'JSON' tab" -ForegroundColor Gray
    Write-Host "6. Replace JSON with content from:" -ForegroundColor Gray
    Write-Host "   $policyFile" -ForegroundColor Cyan
    Write-Host "7. Click 'Review policy' → 'Save changes'" -ForegroundColor Gray
    Write-Host ""
    Write-Host "OR via AWS CLI (with admin credentials):" -ForegroundColor White
    Write-Host ""
    Write-Host "  aws iam put-user-policy ``" -ForegroundColor Gray
    Write-Host "    --user-name $UserName ``" -ForegroundColor Gray
    Write-Host "    --policy-name $PolicyName ``" -ForegroundColor Gray
    Write-Host "    --policy-document file://$policyFile" -ForegroundColor Gray
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "What's Being Added" -ForegroundColor Cyan
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "S3 permissions for these buckets:" -ForegroundColor White
    Write-Host "  • taskactivity-receipts-prod" -ForegroundColor Gray
    Write-Host "  • taskactivity-docs ← NEW" -ForegroundColor Green
    Write-Host "  • taskactivity-logs-archive" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Permissions:" -ForegroundColor White
    Write-Host "  • s3:ListBucket (list files)" -ForegroundColor Gray
    Write-Host "  • s3:GetObject (download)" -ForegroundColor Gray
    Write-Host "  • s3:PutObject (upload)" -ForegroundColor Gray
    Write-Host "  • s3:DeleteObject (delete)" -ForegroundColor Gray
    Write-Host "  • s3:PutObjectAcl/GetObjectAcl (manage permissions)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "These permissions are needed to:" -ForegroundColor White
    Write-Host "  ✓ Fix Content-Type metadata on Word/Excel files" -ForegroundColor Green
    Write-Host "  ✓ Upload documentation with proper content types" -ForegroundColor Green
    Write-Host "  ✓ Manage documentation files" -ForegroundColor Green
    Write-Host ""
    
    exit 1
}

# Other error
Write-Host "✗ Failed to update policy" -ForegroundColor Red
Write-Host ""
Write-Host "Error details:" -ForegroundColor Yellow
Write-Host $result -ForegroundColor Red
Write-Host ""
exit 1
