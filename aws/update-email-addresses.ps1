<#
.SYNOPSIS
    Update email addresses in ECS task definition from .env file.

.DESCRIPTION
    Reads email configuration from .env file and updates the ECS task definition JSON file
    with the latest email addresses for:
    • MAIL_FROM - Email sender address
    • ADMIN_EMAIL - Administrator notifications
    • EXPENSE_APPROVERS - Expense approval notifications
    • JENKINS_BUILD_NOTIFICATION_EMAIL - Jenkins build notifications
    • JENKINS_DEPLOY_NOTIFICATION_EMAIL - Jenkins deployment notifications
    
    The script:
    1. Loads environment variables from .env file
    2. Updates the taskactivity-task-definition.json file
    3. Validates the JSON format
    4. Optionally registers the new task definition with AWS ECS

.PARAMETER DeployToAws
    If specified, registers the updated task definition with AWS ECS and updates the service.

.PARAMETER SkipValidation
    Skip JSON validation after update.

.EXAMPLE
    .\update-email-addresses.ps1
    Update the task definition JSON file with email values from .env.

.EXAMPLE
    .\update-email-addresses.ps1 -DeployToAws
    Update the JSON file and deploy the new task definition to AWS ECS.

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

param(
    [Parameter(Mandatory=$false)]
    [switch]$DeployToAws,
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipValidation
)

# Stop on errors
$ErrorActionPreference = "Stop"

# ========================================
# Configuration
# ========================================

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$envFilePath = Join-Path $projectRoot ".env"
$setEnvScript = Join-Path $projectRoot "scripts\set-env-values.ps1"
$taskDefPath = Join-Path $scriptDir "taskactivity-task-definition.json"

$awsRegion = "us-east-1"
$ecsCluster = "taskactivity-cluster"
$ecsService = "taskactivity-service"
$taskDefinitionFamily = "taskactivity"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ECS Task Definition Email Update Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ========================================
# Validate Prerequisites
# ========================================

if (-not (Test-Path $envFilePath)) {
    Write-Error ".env file not found at: $envFilePath"
    exit 1
}

if (-not (Test-Path $setEnvScript)) {
    Write-Error "set-env-values.ps1 not found at: $setEnvScript"
    exit 1
}

if (-not (Test-Path $taskDefPath)) {
    Write-Error "Task definition file not found at: $taskDefPath"
    exit 1
}

# ========================================
# Load Environment Variables from .env
# ========================================

Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan
$envOutput = & $setEnvScript -envFile $envFilePath 2>&1
$envOutput | ForEach-Object { Write-Host $_ -ForegroundColor Gray }
Write-Host ""

# ========================================
# Read Email Configuration from Environment
# ========================================

Write-Host "Reading email configuration..." -ForegroundColor Cyan

$mailFrom = $env:MAIL_FROM
$adminEmail = $env:ADMIN_EMAIL
$expenseApprovers = $env:EXPENSE_APPROVERS
$jenkinsBuildEmail = $env:JENKINS_BUILD_NOTIFICATION_EMAIL
$jenkinsDeployEmail = $env:JENKINS_DEPLOY_NOTIFICATION_EMAIL

# Validate required variables
$missingVars = @()
if ([string]::IsNullOrWhiteSpace($mailFrom)) { $missingVars += "MAIL_FROM" }
if ([string]::IsNullOrWhiteSpace($adminEmail)) { $missingVars += "ADMIN_EMAIL" }
if ([string]::IsNullOrWhiteSpace($expenseApprovers)) { $missingVars += "EXPENSE_APPROVERS" }
if ([string]::IsNullOrWhiteSpace($jenkinsBuildEmail)) { $missingVars += "JENKINS_BUILD_NOTIFICATION_EMAIL" }
if ([string]::IsNullOrWhiteSpace($jenkinsDeployEmail)) { $missingVars += "JENKINS_DEPLOY_NOTIFICATION_EMAIL" }

if ($missingVars.Count -gt 0) {
    Write-Error "Missing required environment variables in .env file: $($missingVars -join ', ')"
    exit 1
}

Write-Host "Email Configuration from .env file:" -ForegroundColor White
Write-Host "  MAIL_FROM: $mailFrom" -ForegroundColor Green
Write-Host "  ADMIN_EMAIL: $adminEmail" -ForegroundColor Green
Write-Host "  EXPENSE_APPROVERS: $expenseApprovers" -ForegroundColor Green
Write-Host "  JENKINS_BUILD_NOTIFICATION_EMAIL: $jenkinsBuildEmail" -ForegroundColor Green
Write-Host "  JENKINS_DEPLOY_NOTIFICATION_EMAIL: $jenkinsDeployEmail" -ForegroundColor Green
Write-Host ""

# ========================================
# Backup Current Task Definition
# ========================================

$backupPath = "$taskDefPath.backup"
Write-Host "Creating backup of task definition..." -ForegroundColor Cyan
Copy-Item -Path $taskDefPath -Destination $backupPath -Force
Write-Host "  Backup saved to: $backupPath" -ForegroundColor Gray
Write-Host ""

# ========================================
# Update Task Definition JSON
# ========================================

Write-Host "Updating task definition JSON file..." -ForegroundColor Cyan

try {
    # Read and parse JSON
    $taskDefContent = Get-Content -Path $taskDefPath -Raw
    $taskDef = $taskDefContent | ConvertFrom-Json
    
    # Find and update environment variables in the container definition
    $envVars = $taskDef.containerDefinitions[0].environment
    
    # Track what was updated
    $updatedVars = @()
    $checkedVars = @()
    
    foreach ($envVar in $envVars) {
        $oldValue = $envVar.value
        $updated = $false
        $checked = $false
        
        switch ($envVar.name) {
            "MAIL_FROM" {
                $checked = $true
                if ($envVar.value -ne $mailFrom) {
                    $envVar.value = $mailFrom
                    $updated = $true
                } else {
                    $checkedVars += @{
                        Name = $envVar.name
                        Value = $envVar.value
                        Status = "unchanged"
                    }
                }
            }
            "ADMIN_EMAIL" {
                $checked = $true
                if ($envVar.value -ne $adminEmail) {
                    $envVar.value = $adminEmail
                    $updated = $true
                } else {
                    $checkedVars += @{
                        Name = $envVar.name
                        Value = $envVar.value
                        Status = "unchanged"
                    }
                }
            }
            "EXPENSE_APPROVERS" {
                $checked = $true
                if ($envVar.value -ne $expenseApprovers) {
                    $envVar.value = $expenseApprovers
                    $updated = $true
                } else {
                    $checkedVars += @{
                        Name = $envVar.name
                        Value = $envVar.value
                        Status = "unchanged"
                    }
                }
            }
            "JENKINS_BUILD_NOTIFICATION_EMAIL" {
                $checked = $true
                if ($envVar.value -ne $jenkinsBuildEmail) {
                    $envVar.value = $jenkinsBuildEmail
                    $updated = $true
                } else {
                    $checkedVars += @{
                        Name = $envVar.name
                        Value = $envVar.value
                        Status = "unchanged"
                    }
                }
            }
            "JENKINS_DEPLOY_NOTIFICATION_EMAIL" {
                $checked = $true
                if ($envVar.value -ne $jenkinsDeployEmail) {
                    $envVar.value = $jenkinsDeployEmail
                    $updated = $true
                } else {
                    $checkedVars += @{
                        Name = $envVar.name
                        Value = $envVar.value
                        Status = "unchanged"
                    }
                }
            }
        }
        
        if ($updated) {
            $updatedVars += @{
                Name = $envVar.name
                OldValue = $oldValue
                NewValue = $envVar.value
            }
        }
    }
    
    # Display comparison results
    Write-Host "Comparison Results:" -ForegroundColor White
    Write-Host ""
    
    if ($checkedVars.Count -gt 0) {
        Write-Host "  Unchanged (already current):" -ForegroundColor Gray
        foreach ($var in $checkedVars) {
            Write-Host "    ✓ $($var.Name): $($var.Value)" -ForegroundColor DarkGray
        }
        Write-Host ""
    }
    
    if ($updatedVars.Count -eq 0) {
        Write-Host "  No changes needed - all email addresses are up to date" -ForegroundColor Yellow
    } else {
        Write-Host "  Updated $($updatedVars.Count) email configuration(s):" -ForegroundColor Green
        foreach ($var in $updatedVars) {
            Write-Host "    $($var.Name):" -ForegroundColor White
            Write-Host "      Old: $($var.OldValue)" -ForegroundColor Red
            Write-Host "      New: $($var.NewValue)" -ForegroundColor Green
        }
    }
    Write-Host ""
    
    # Convert back to JSON with proper formatting
    $updatedJson = $taskDef | ConvertTo-Json -Depth 100 -Compress:$false
    
    # Save updated JSON
    $updatedJson | Set-Content -Path $taskDefPath -NoNewline -Encoding UTF8
    
    Write-Host ""
    Write-Host "Task definition JSON file updated successfully" -ForegroundColor Green
    Write-Host "  File: $taskDefPath" -ForegroundColor Gray
    Write-Host ""
    
} catch {
    Write-Error "Failed to update task definition JSON: $_"
    Write-Host "Restoring backup..." -ForegroundColor Yellow
    Copy-Item -Path $backupPath -Destination $taskDefPath -Force
    exit 1
}

# ========================================
# Validate JSON Format
# ========================================

if (-not $SkipValidation) {
    Write-Host "Validating JSON format..." -ForegroundColor Cyan
    try {
        $null = Get-Content -Path $taskDefPath -Raw | ConvertFrom-Json
        Write-Host "  JSON validation passed ✓" -ForegroundColor Green
        Write-Host ""
    } catch {
        Write-Error "JSON validation failed: $_"
        Write-Host "Restoring backup..." -ForegroundColor Yellow
        Copy-Item -Path $backupPath -Destination $taskDefPath -Force
        exit 1
    }
}

# ========================================
# Deploy to AWS (Optional)
# ========================================

if ($DeployToAws) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Deploying to AWS ECS" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Check AWS CLI
    $awsVersion = aws --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error "AWS CLI not found. Please install AWS CLI first."
        exit 1
    }
    Write-Host "AWS CLI Version: $awsVersion" -ForegroundColor Gray
    Write-Host ""
    
    # Register new task definition
    Write-Host "Registering new task definition with AWS ECS..." -ForegroundColor Cyan
    
    $registerOutput = aws ecs register-task-definition `
        --cli-input-json "file://$taskDefPath" `
        --region $awsRegion 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to register task definition:`n$registerOutput"
        exit 1
    }
    
    $taskDefResponse = $registerOutput | ConvertFrom-Json
    $newRevision = $taskDefResponse.taskDefinition.revision
    
    Write-Host "  New task definition registered: ${taskDefinitionFamily}:${newRevision}" -ForegroundColor Green
    Write-Host ""
    
    # Update ECS service
    Write-Host "Updating ECS service to use new task definition..." -ForegroundColor Cyan
    
    $updateOutput = aws ecs update-service `
        --cluster $ecsCluster `
        --service $ecsService `
        --task-definition "${taskDefinitionFamily}:${newRevision}" `
        --region $awsRegion 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to update ECS service:`n$updateOutput"
        exit 1
    }
    
    Write-Host "  ECS service updated successfully ✓" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "Deployment initiated. The new task will be deployed gradually." -ForegroundColor Yellow
    Write-Host "Monitor deployment status in AWS ECS Console:" -ForegroundColor Yellow
    Write-Host "  https://console.aws.amazon.com/ecs/v2/clusters/$ecsCluster/services/$ecsService" -ForegroundColor Cyan
    Write-Host ""
}

# ========================================
# Summary
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($updatedVars.Count -gt 0) {
    Write-Host "✓ Task definition updated with latest email addresses" -ForegroundColor Green
    Write-Host "✓ Backup saved to: $backupPath" -ForegroundColor Green
    
    if ($DeployToAws) {
        Write-Host "✓ New task definition registered with AWS ECS" -ForegroundColor Green
        Write-Host "✓ ECS service updated (deployment in progress)" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "To deploy the updated task definition to AWS, run:" -ForegroundColor Yellow
        Write-Host "  .\update-email-addresses.ps1 -DeployToAws" -ForegroundColor Cyan
    }
} else {
    Write-Host "✓ No changes needed - all email addresses are already current" -ForegroundColor Green
}

Write-Host ""
Write-Host "Email configuration complete!" -ForegroundColor Green
Write-Host ""
# Exit with appropriate code
if ($updatedVars.Count -gt 0) {
    # Changes were made (update successful)
    exit 0
} else {
    # No changes needed (no update required)
    exit 1
}