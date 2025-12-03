#!/usr/bin/env pwsh
###############################################################################
# Configure AWS SES for Task Activity Email Notifications
#
# This script helps configure AWS Simple Email Service (SES) for the 
# Task Activity application. It can verify email addresses, configure
# identities, and update ECS task definitions with email settings.
#
# Prerequisites:
# • AWS CLI installed and configured
# • Appropriate AWS IAM permissions for SES and ECS
# • Email address or domain to verify
#
# Usage:
#   .\configure-ses.ps1 -Email <address> [-Domain <domain>] [-UpdateECS]
#
# Examples:
#   # Verify single email address
#   .\configure-ses.ps1 -Email noreply@taskactivitytracker.com
#
#   # Verify domain and update ECS
#   .\configure-ses.ps1 -Domain taskactivitytracker.com -AdminEmail admin@company.com -UpdateECS
#
#   # Request production access
#   .\configure-ses.ps1 -RequestProductionAccess
#
# Parameters:
#   -Email                 : Email address to verify (sender address)
#   -Domain               : Domain to verify (recommended for production)
#   -AdminEmail           : Admin email to receive notifications
#   -UpdateECS            : Update ECS task definition with email config
#   -RequestProductionAccess : Show instructions for requesting production access
#   -Region               : AWS region (default: us-east-1)
#
# Author: Dean Ammons
# Date: December 2025
###############################################################################

param(
    [Parameter(Mandatory=$false)]
    [string]$Email,
    
    [Parameter(Mandatory=$false)]
    [string]$Domain,
    
    [Parameter(Mandatory=$false)]
    [string]$AdminEmail,
    
    [Parameter(Mandatory=$false)]
    [switch]$UpdateECS,
    
    [Parameter(Mandatory=$false)]
    [switch]$RequestProductionAccess,
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"

# ========================================
# Configuration
# ========================================

$ECS_CLUSTER = "taskactivity-cluster"
$ECS_SERVICE = "taskactivity-service"
$TASK_FAMILY = "taskactivity"

# ========================================
# Helper Functions
# ========================================

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

function Show-ProductionAccessInstructions {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "AWS SES Production Access Request" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Info "By default, AWS SES accounts are in SANDBOX mode with limitations:"
    Write-Host "  • Can only send to verified email addresses" -ForegroundColor Yellow
    Write-Host "  • Limited to 200 emails per 24 hours" -ForegroundColor Yellow
    Write-Host "  • Maximum 1 email per second" -ForegroundColor Yellow
    Write-Host ""
    Write-Info "To request production access:"
    Write-Host "  1. Go to AWS Console → SES → Account dashboard"
    Write-Host "  2. Look for banner: 'Your account is in the Amazon SES sandbox'"
    Write-Host "  3. Click 'Request production access'"
    Write-Host "  4. Fill out the form:"
    Write-Host "     - Mail type: Transactional"
    Write-Host "     - Website URL: https://taskactivitytracker.com"
    Write-Host "     - Use case: Security notifications and system alerts"
    Write-Host "     - Expected volume: 500-1000 emails/month"
    Write-Host "  5. Submit and wait for approval (typically 24-48 hours)"
    Write-Host ""
    Write-Info "After approval, you can send to any email address."
    Write-Host ""
}

function Verify-EmailIdentity {
    param([string]$EmailAddress)
    
    Write-Info "Verifying email identity: $EmailAddress"
    
    # Check if already verified
    $status = aws ses get-identity-verification-attributes `
        --identities $EmailAddress `
        --region $Region `
        --query "VerificationAttributes.""$EmailAddress"".VerificationStatus" `
        --output text 2>$null
    
    if ($status -eq "Success") {
        Write-Success "Email is already verified: $EmailAddress"
        return $true
    }
    
    # Send verification email
    Write-Info "Sending verification email to: $EmailAddress"
    aws ses verify-email-identity `
        --email-address $EmailAddress `
        --region $Region | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Verification email sent to: $EmailAddress"
        Write-Warning "Please check your inbox and click the verification link"
        Write-Info "Run this script again after verifying to check status"
        return $false
    } else {
        Write-Error "Failed to send verification email"
        return $false
    }
}

function Verify-DomainIdentity {
    param([string]$DomainName)
    
    Write-Info "Verifying domain identity: $DomainName"
    
    # Check if already verified
    $status = aws ses get-identity-verification-attributes `
        --identities $DomainName `
        --region $Region `
        --query "VerificationAttributes.""$DomainName"".VerificationStatus" `
        --output text 2>$null
    
    if ($status -eq "Success") {
        Write-Success "Domain is already verified: $DomainName"
        return $true
    }
    
    # Verify domain
    Write-Info "Initiating domain verification..."
    $token = aws ses verify-domain-identity `
        --domain $DomainName `
        --region $Region `
        --query 'VerificationToken' `
        --output text
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Domain verification initiated"
        Write-Host ""
        Write-Warning "Add this TXT record to your DNS:"
        Write-Host "  Name:  _amazonses.$DomainName" -ForegroundColor Yellow
        Write-Host "  Type:  TXT" -ForegroundColor Yellow
        Write-Host "  Value: $token" -ForegroundColor Yellow
        Write-Host ""
        Write-Info "Also add DKIM records for better deliverability:"
        
        # Get DKIM tokens
        $dkimTokens = aws ses verify-domain-dkim `
            --domain $DomainName `
            --region $Region `
            --query 'DkimTokens' `
            --output json | ConvertFrom-Json
        
        Write-Host ""
        foreach ($dkimToken in $dkimTokens) {
            Write-Host "  Name:  ${dkimToken}._domainkey.$DomainName" -ForegroundColor Yellow
            Write-Host "  Type:  CNAME" -ForegroundColor Yellow
            Write-Host "  Value: ${dkimToken}.dkim.amazonses.com" -ForegroundColor Yellow
            Write-Host ""
        }
        
        Write-Info "Wait for DNS propagation (up to 72 hours, usually faster)"
        Write-Info "Run this script again to check verification status"
        return $false
    } else {
        Write-Error "Failed to initiate domain verification"
        return $false
    }
}

function Get-SESStatus {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "AWS SES Configuration Status" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Check account status
    $sendQuota = aws ses get-send-quota --region $Region --output json | ConvertFrom-Json
    
    Write-Info "Sending Quota:"
    Write-Host "  • Max 24-hour send: $($sendQuota.Max24HourSend)" -ForegroundColor Cyan
    Write-Host "  • Max send rate:    $($sendQuota.MaxSendRate) emails/second" -ForegroundColor Cyan
    Write-Host "  • Sent last 24h:    $($sendQuota.SentLast24Hours)" -ForegroundColor Cyan
    Write-Host ""
    
    # Check if in sandbox
    $accountStatus = aws sesv2 get-account --region $Region --output json 2>$null | ConvertFrom-Json
    if ($accountStatus.ProductionAccessEnabled) {
        Write-Success "Account Status: Production Access Enabled"
    } else {
        Write-Warning "Account Status: SANDBOX MODE (limited to verified addresses)"
        Write-Info "Use -RequestProductionAccess to see how to request production access"
    }
    Write-Host ""
    
    # List verified identities
    Write-Info "Verified Identities:"
    $identities = aws ses list-identities --region $Region --output json | ConvertFrom-Json
    
    if ($identities.Identities.Count -eq 0) {
        Write-Warning "  No verified identities found"
    } else {
        foreach ($identity in $identities.Identities) {
            $status = aws ses get-identity-verification-attributes `
                --identities $identity `
                --region $Region `
                --query "VerificationAttributes.""$identity"".VerificationStatus" `
                --output text
            
            if ($status -eq "Success") {
                Write-Host "  ✓ $identity" -ForegroundColor Green
            } else {
                Write-Host "  ⏳ $identity (Pending)" -ForegroundColor Yellow
            }
        }
    }
    Write-Host ""
}

function Update-ECSTaskDefinition {
    param(
        [string]$FromEmail,
        [string]$ToEmail
    )
    
    Write-Info "Updating ECS task definition with email configuration..."
    
    # Get current task definition
    $taskDef = aws ecs describe-task-definition `
        --task-definition $TASK_FAMILY `
        --region $Region `
        --output json | ConvertFrom-Json
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to get task definition"
        return
    }
    
    # Extract container definition
    $container = $taskDef.taskDefinition.containerDefinitions[0]
    
    # Add/update email environment variables
    $emailEnvVars = @(
        @{ name = "MAIL_ENABLED"; value = "true" },
        @{ name = "MAIL_USE_AWS_SDK"; value = "true" },
        @{ name = "AWS_REGION"; value = $Region },
        @{ name = "MAIL_FROM"; value = $FromEmail },
        @{ name = "ADMIN_EMAIL"; value = $ToEmail }
    )
    
    # Merge with existing environment variables
    $existingEnv = $container.environment | Where-Object { 
        $_.name -notin @("MAIL_ENABLED", "MAIL_USE_AWS_SDK", "AWS_REGION", "MAIL_FROM", "ADMIN_EMAIL")
    }
    
    $container.environment = $existingEnv + $emailEnvVars
    
    # Create new task definition JSON
    $newTaskDef = @{
        family = $taskDef.taskDefinition.family
        taskRoleArn = $taskDef.taskDefinition.taskRoleArn
        executionRoleArn = $taskDef.taskDefinition.executionRoleArn
        networkMode = $taskDef.taskDefinition.networkMode
        containerDefinitions = @($container)
        requiresCompatibilities = $taskDef.taskDefinition.requiresCompatibilities
        cpu = $taskDef.taskDefinition.cpu
        memory = $taskDef.taskDefinition.memory
    }
    
    # Save to temp file
    $tempFile = [System.IO.Path]::GetTempFileName() + ".json"
    $newTaskDef | ConvertTo-Json -Depth 10 | Out-File -FilePath $tempFile -Encoding utf8
    
    # Register new task definition
    Write-Info "Registering new task definition..."
    $newRevision = aws ecs register-task-definition `
        --cli-input-json "file://$tempFile" `
        --region $Region `
        --query 'taskDefinition.revision' `
        --output text
    
    Remove-Item $tempFile -ErrorAction SilentlyContinue
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "New task definition registered: ${TASK_FAMILY}:${newRevision}"
        
        # Update service
        Write-Info "Updating ECS service..."
        aws ecs update-service `
            --cluster $ECS_CLUSTER `
            --service $ECS_SERVICE `
            --task-definition "${TASK_FAMILY}:${newRevision}" `
            --force-new-deployment `
            --region $Region | Out-Null
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "ECS service updated successfully"
            Write-Info "Email notifications are now enabled"
            Write-Host ""
            Write-Host "Configuration:" -ForegroundColor Cyan
            Write-Host "  • From:        $FromEmail" -ForegroundColor Cyan
            Write-Host "  • Admin Email: $ToEmail" -ForegroundColor Cyan
            Write-Host "  • Method:      AWS SES SDK (IAM Role)" -ForegroundColor Cyan
            Write-Host "  • Region:      $Region" -ForegroundColor Cyan
        } else {
            Write-Error "Failed to update ECS service"
        }
    } else {
        Write-Error "Failed to register new task definition"
    }
}

# ========================================
# Main Execution
# ========================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AWS SES Configuration Tool" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Show production access instructions if requested
if ($RequestProductionAccess) {
    Show-ProductionAccessInstructions
    exit 0
}

# If no parameters, show status
if (-not $Email -and -not $Domain) {
    Get-SESStatus
    Write-Info "Use -Email or -Domain to verify identities"
    Write-Info "Use -RequestProductionAccess to see how to exit sandbox mode"
    exit 0
}

# Verify email identity
if ($Email) {
    $verified = Verify-EmailIdentity -EmailAddress $Email
    
    if (-not $verified) {
        Write-Warning "Email verification pending. Cannot proceed with ECS update."
        exit 0
    }
}

# Verify domain identity
if ($Domain) {
    $verified = Verify-DomainIdentity -DomainName $Domain
    
    if (-not $verified) {
        Write-Warning "Domain verification pending. Cannot proceed with ECS update."
        exit 0
    }
}

# Update ECS if requested and email is verified
if ($UpdateECS) {
    if (-not $AdminEmail) {
        Write-Error "AdminEmail is required when using -UpdateECS"
        Write-Info "Usage: .\configure-ses.ps1 -Email <from> -AdminEmail <to> -UpdateECS"
        exit 1
    }
    
    $fromEmail = if ($Email) { $Email } else { "noreply@$Domain" }
    Update-ECSTaskDefinition -FromEmail $fromEmail -ToEmail $AdminEmail
}

# Show final status
Get-SESStatus

Write-Success "SES configuration complete!"
Write-Host ""
