/**
 * Description: AWS SES Email Tracking Setup Script - configures AWS SES to track email events and publish them to CloudWatch for monitoring
 *
 * Author: Dean Ammons
 * Date: December 2025
 */

###############################################################################
# AWS SES Email Tracking Setup Script
# 
# This script configures AWS SES to track email events (sends, deliveries, 
# bounces, complaints) and publish them to CloudWatch for monitoring.
#
# What this provides:
# - Tracks all email sends with delivery status
# - CloudWatch metrics for email health monitoring
# - Alternative to Gmail's "Sent" folder for AWS SES
#
# Prerequisites:
# - AWS CLI installed and configured
# - AWS SES already configured and verified
# - Appropriate IAM permissions for SES and CloudWatch
#
# Usage:
#   .\setup-ses-tracking.ps1
#
# Author: Dean Ammons
# Date: December 2025
#
###############################################################################

param(
    [Parameter(Mandatory=$false)]
    [string]$ConfigSetName = "taskactivity-emails",
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

Write-Host ""
Write-Host "==================  AWS SES Email Tracking Setup  ==================" -ForegroundColor Cyan
Write-Host "Configuration Set: $ConfigSetName" -ForegroundColor Cyan
Write-Host "AWS Region:        $Region" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host ""

# Check if configuration set already exists
Write-Info "Checking if configuration set already exists..."
$ErrorActionPreference = "SilentlyContinue"
$existingConfigSet = aws ses describe-configuration-set `
    --configuration-set-name $ConfigSetName `
    --region $Region `
    --output json 2>&1
$configSetExists = ($LASTEXITCODE -eq 0)
$ErrorActionPreference = "Stop"

if ($configSetExists) {
    Write-Warning "Configuration set '$ConfigSetName' already exists. Skipping creation."
} else {
    # Create configuration set
    Write-Info "Creating SES configuration set: $ConfigSetName"
    
    # Create JSON for configuration set (UTF-8 without BOM)
    $configSetJson = @"
{
  "Name": "$ConfigSetName"
}
"@
    
    $tempConfigFile = Join-Path $env:TEMP "ses-config-set.json"
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($tempConfigFile, $configSetJson, $utf8NoBom)
    
    aws ses create-configuration-set `
        --configuration-set "file://$tempConfigFile" `
        --region $Region
    
    Remove-Item $tempConfigFile -ErrorAction SilentlyContinue
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to create configuration set"
        exit 1
    }
    
    Write-Success "Configuration set created"
}

# Add CloudWatch event destination
Write-Info "Configuring CloudWatch event destination..."

$eventDestination = @"
{
  "Name": "CloudWatchDestination",
  "Enabled": true,
  "MatchingEventTypes": ["send", "delivery", "bounce", "complaint", "reject"],
  "CloudWatchDestination": {
    "DimensionConfigurations": [
      {
        "DimensionName": "ses:configuration-set",
        "DimensionValueSource": "emailHeader",
        "DefaultDimensionValue": "$ConfigSetName"
      },
      {
        "DimensionName": "ses:from-domain",
        "DimensionValueSource": "emailHeader",
        "DefaultDimensionValue": "taskactivitytracker.com"
      }
    ]
  }
}
"@

# Check if event destination already exists
$ErrorActionPreference = "SilentlyContinue"
$existingDestination = aws ses describe-configuration-set `
    --configuration-set-name $ConfigSetName `
    --region $Region `
    --query "EventDestinations[?Name=='CloudWatchDestination']" `
    --output json 2>&1
$destinationExists = ($LASTEXITCODE -eq 0 -and $existingDestination -ne "[]" -and $existingDestination -notmatch "error")
$ErrorActionPreference = "Stop"

if ($destinationExists) {
    Write-Warning "CloudWatch event destination already exists. Skipping creation."
} else {
    # Save to temp file
    $tempFile = Join-Path $env:TEMP "ses-event-destination.json"
    $eventDestination | Out-File -FilePath $tempFile -Encoding UTF8

    aws ses put-configuration-set-event-destination `
        --configuration-set-name $ConfigSetName `
        --event-destination (Get-Content $tempFile -Raw) `
        --region $Region

    Remove-Item $tempFile -ErrorAction SilentlyContinue

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to create CloudWatch event destination"
        exit 1
    }

    Write-Success "CloudWatch event destination configured"
}

Write-Host ""
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Success "AWS SES Email Tracking Setup Complete!"
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Update application-aws.properties:" -ForegroundColor Yellow
Write-Host "   Uncomment: spring.mail.properties.mail.ses.configuration-set=$ConfigSetName" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Rebuild and redeploy your application:" -ForegroundColor Yellow
Write-Host "   .\aws\deploy-aws.ps1 -EnableEmail -UseAwsSdk ``" -ForegroundColor Gray
Write-Host "     -MailFrom 'noreply@taskactivitytracker.com' ``" -ForegroundColor Gray
Write-Host "     -AdminEmail 'your-email@example.com'" -ForegroundColor Gray
Write-Host ""
Write-Host "3. View email metrics in CloudWatch:" -ForegroundColor Yellow
Write-Host "   - Console: https://console.aws.amazon.com/cloudwatch" -ForegroundColor Gray
Write-Host "   - Metrics → SES → Configuration Set" -ForegroundColor Gray
Write-Host "   - Or run: aws cloudwatch list-metrics --namespace AWS/SES --region $Region" -ForegroundColor Gray
Write-Host ""
Write-Host "4. View email logs:" -ForegroundColor Yellow
Write-Host "   aws logs tail /ecs/taskactivity --filter-pattern `"email`" --follow --region $Region" -ForegroundColor Gray
Write-Host ""
Write-Host "=====================================================================" -ForegroundColor Cyan
