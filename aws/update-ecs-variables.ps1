<#
.SYNOPSIS
    Update email addresses and base URL in ECS task definition from .env file.

.DESCRIPTION
    Reads email configuration from .env file and updates the ECS task definition with the latest
    email addresses for:
    • MAIL_FROM - Email sender address
    • ADMIN_EMAIL - Administrator notifications
    • EXPENSE_APPROVERS - Expense approval notifications
    • JENKINS_BUILD_NOTIFICATION_EMAIL - Jenkins build notifications
    • JENKINS_DEPLOY_NOTIFICATION_EMAIL - Jenkins deployment notifications
    • JENKINS_DEPLOY_SKIPPED_CHECK - Enable/disable skipped deployment notifications
    • APP_BASE_URL - Base URL for password reset links and application URLs
    
    SAFETY: The script fetches the CURRENT task definition from AWS ECS (if AWS CLI is available)
    rather than using the local file. This ensures that any changes made directly in ECS are
    preserved and not overwritten. If AWS CLI is unavailable or ECS fetch fails, it falls back
    to using the local taskactivity-task-definition.json file.
    
    The script:
    1. Loads environment variables from .env file
    2. Fetches current task definition from AWS ECS (or uses local file as fallback)
    3. Updates only the email configuration environment variables
    4. Saves updated definition to local taskactivity-task-definition.json file
    5. Validates the JSON format
    6. Optionally registers the new task definition with AWS ECS and updates the service

.PARAMETER DeployToAws
    If specified, registers the updated task definition with AWS ECS and updates the service.

.PARAMETER SkipValidation
    Skip JSON validation after update.

.PARAMETER EncryptionKey
    Encryption key for sensitive data. Passed to set-env-values.ps1 for decryption.

.EXAMPLE
    .\update-ecs-variables.ps1
    Fetch current task definition from ECS, update with .env values, save to local file.

.EXAMPLE
    .\update-ecs-variables.ps1 -DeployToAws
    Fetch from ECS, update with .env values, save locally, and deploy the new task definition.

.NOTES
    Author: Dean Ammons
    Date: January 2026
    
    IMPORTANT: This script prioritizes the current ECS task definition over the local file
    to prevent accidentally overwriting changes made directly in AWS ECS.
#>

param(
    [Parameter(Mandatory=$false)]
    [switch]$DeployToAws,
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipValidation,
    
    [Parameter(Mandatory=$false)]
    [string]$EncryptionKey = ""
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

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "ECS Task Definition Configuration Update Script" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
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
if (-not [string]::IsNullOrWhiteSpace($EncryptionKey)) {
    $envOutput = & $setEnvScript -envFile $envFilePath -EncryptionKey $EncryptionKey 2>&1
} else {
    $envOutput = & $setEnvScript -envFile $envFilePath 2>&1
}
$envOutput | ForEach-Object { Write-Host $_ -ForegroundColor Gray }
Write-Host ""

# ========================================
# Read Configuration from Environment
# ========================================

Write-Host "Reading Environment configuration..." -ForegroundColor Cyan

$mailFrom = $env:MAIL_FROM
$adminEmail = $env:ADMIN_EMAIL
$expenseApprovers = $env:EXPENSE_APPROVERS
$jenkinsBuildEmail = $env:JENKINS_BUILD_NOTIFICATION_EMAIL
$jenkinsDeployEmail = $env:JENKINS_DEPLOY_NOTIFICATION_EMAIL
$jenkinsDeploySkippedCheck = $env:JENKINS_DEPLOY_SKIPPED_CHECK
$appBaseUrl = $env:APP_BASE_URL

# Validate required variables
$missingVars = @()
if ([string]::IsNullOrWhiteSpace($mailFrom)) { $missingVars += "MAIL_FROM" }
if ([string]::IsNullOrWhiteSpace($adminEmail)) { $missingVars += "ADMIN_EMAIL" }
if ([string]::IsNullOrWhiteSpace($expenseApprovers)) { $missingVars += "EXPENSE_APPROVERS" }
if ([string]::IsNullOrWhiteSpace($jenkinsBuildEmail)) { $missingVars += "JENKINS_BUILD_NOTIFICATION_EMAIL" }
if ([string]::IsNullOrWhiteSpace($jenkinsDeployEmail)) { $missingVars += "JENKINS_DEPLOY_NOTIFICATION_EMAIL" }
if ([string]::IsNullOrWhiteSpace($jenkinsDeploySkippedCheck)) { $missingVars += "JENKINS_DEPLOY_SKIPPED_CHECK" }
if ([string]::IsNullOrWhiteSpace($appBaseUrl)) { $missingVars += "APP_BASE_URL" }

if ($missingVars.Count -gt 0) {
    Write-Error "Missing required environment variables in .env file: $($missingVars -join ', ')"
    exit 1
}

Write-Host "Configurations from .env file:" -ForegroundColor White
Write-Host "  MAIL_FROM: $mailFrom" -ForegroundColor Green
Write-Host "  ADMIN_EMAIL: $adminEmail" -ForegroundColor Green
Write-Host "  EXPENSE_APPROVERS: $expenseApprovers" -ForegroundColor Green
Write-Host "  JENKINS_BUILD_NOTIFICATION_EMAIL: $jenkinsBuildEmail" -ForegroundColor Green
Write-Host "  JENKINS_DEPLOY_NOTIFICATION_EMAIL: $jenkinsDeployEmail" -ForegroundColor Green
Write-Host "  JENKINS_DEPLOY_SKIPPED_CHECK: $jenkinsDeploySkippedCheck" -ForegroundColor Green
Write-Host "  APP_BASE_URL: $appBaseUrl" -ForegroundColor Green
Write-Host ""

# ========================================
# Fetch Current Task Definition from ECS
# ========================================

Write-Host "Fetching current task definition from AWS ECS..." -ForegroundColor Cyan

try {
    # Check AWS CLI
    $awsVersion = aws --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "AWS CLI not found. Falling back to local file."
        $useLocalFile = $true
    } else {
        Write-Host "  AWS CLI Version: $awsVersion" -ForegroundColor Gray
        
        # Describe the current task definition to get the latest revision
        $describeOutput = aws ecs describe-task-definition `
            --task-definition $taskDefinitionFamily `
            --region $awsRegion 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            $currentTaskDef = ($describeOutput | ConvertFrom-Json).taskDefinition
            $currentRevision = $currentTaskDef.revision
            Write-Host "  Current ECS revision: ${taskDefinitionFamily}:${currentRevision}" -ForegroundColor Green
            
            # Remove fields that cannot be used when registering new task definition
            $currentTaskDef.PSObject.Properties.Remove('taskDefinitionArn')
            $currentTaskDef.PSObject.Properties.Remove('revision')
            $currentTaskDef.PSObject.Properties.Remove('status')
            $currentTaskDef.PSObject.Properties.Remove('requiresAttributes')
            $currentTaskDef.PSObject.Properties.Remove('compatibilities')
            $currentTaskDef.PSObject.Properties.Remove('registeredAt')
            $currentTaskDef.PSObject.Properties.Remove('registeredBy')
            
            $taskDef = $currentTaskDef
            $useLocalFile = $false
            Write-Host "  Using current ECS task definition as base" -ForegroundColor Green
        } else {
            Write-Warning "Failed to fetch from ECS: $describeOutput"
            Write-Warning "Falling back to local file."
            $useLocalFile = $true
        }
    }
} catch {
    Write-Warning "Error fetching from ECS: $_"
    Write-Warning "Falling back to local file."
    $useLocalFile = $true
}

# ========================================
# Backup Local Task Definition File
# ========================================

$backupPath = "$taskDefPath.backup"
Write-Host "Creating backup of local task definition file..." -ForegroundColor Cyan
Copy-Item -Path $taskDefPath -Destination $backupPath -Force
Write-Host "  Backup saved to: $backupPath" -ForegroundColor Gray
Write-Host ""

# ========================================
# Load Task Definition
# ========================================

if ($useLocalFile) {
    Write-Host "Loading task definition from local file..." -ForegroundColor Yellow
    $taskDefContent = Get-Content -Path $taskDefPath -Raw
    $taskDef = $taskDefContent | ConvertFrom-Json
} else {
    Write-Host "Using task definition fetched from ECS..." -ForegroundColor Cyan
}

# ========================================
# Update Task Definition JSON
# ========================================

Write-Host "Updating email configuration in task definition..." -ForegroundColor Cyan

try {
    
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
            "JENKINS_DEPLOY_SKIPPED_CHECK" {
                $checked = $true
                if ($envVar.value -ne $jenkinsDeploySkippedCheck) {
                    $envVar.value = $jenkinsDeploySkippedCheck
                    $updated = $true
                } else {
                    $checkedVars += @{
                        Name = $envVar.name
                        Value = $envVar.value
                        Status = "unchanged"
                    }
                }
            }
            "APP_BASE_URL" {
                $checked = $true
                if ($envVar.value -ne $appBaseUrl) {
                    $envVar.value = $appBaseUrl
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
        Write-Host "  No changes needed - all configurations are up to date" -ForegroundColor Yellow
    } else {
        Write-Host "  Updated $($updatedVars.Count) configuration(s):" -ForegroundColor Green
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
    if ($useLocalFile) {
        Write-Host "  ⚠ Note: Used local file as base (AWS CLI not available or ECS fetch failed)" -ForegroundColor Yellow
        Write-Host "  ⚠ Local file may not match current ECS configuration" -ForegroundColor Yellow
    } else {
        Write-Host "  ✓ Based on current ECS task definition revision ${currentRevision}" -ForegroundColor Green
    }
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
    # Check if any changes were made
    if ($updatedVars.Count -eq 0) {
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "Deployment Skipped" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "✓ No configuration changes detected" -ForegroundColor Green
        Write-Host "✓ ECS task definition is already up-to-date" -ForegroundColor Green
        Write-Host ""
        Write-Host "Skipping ECS deployment to avoid unnecessary service restart." -ForegroundColor Yellow
        Write-Host ""
    } else {
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "Deploying to AWS ECS" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host ""
        
        # Verify AWS CLI is available
        if ($useLocalFile) {
            Write-Error "Cannot deploy to AWS: AWS CLI not available or ECS fetch failed."
            Write-Error "Please install AWS CLI and ensure credentials are configured."
            exit 1
        }
        
        Write-Host "Configuration changes detected: $($updatedVars.Count)" -ForegroundColor Yellow
        foreach ($var in $updatedVars) {
            Write-Host "  • $($var.Name)" -ForegroundColor Gray
        }
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
}

# ========================================
# Summary
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($updatedVars.Count -gt 0) {
    Write-Host "✓ Task definition updated with latest configuration values" -ForegroundColor Green
    Write-Host "✓ Backup saved to: $backupPath" -ForegroundColor Green
    
    if ($DeployToAws) {
        Write-Host "✓ New task definition registered with AWS ECS" -ForegroundColor Green
        Write-Host "✓ ECS service updated (deployment in progress)" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "To deploy the updated task definition to AWS, run:" -ForegroundColor Yellow
        Write-Host "  .\update-ecs-variables.ps1 -DeployToAws" -ForegroundColor Cyan
    }
} else {
    Write-Host "✓ No changes needed - all configurations are already current" -ForegroundColor Green
    
    if ($DeployToAws) {
        Write-Host "✓ ECS deployment skipped (no changes detected)" -ForegroundColor Green
    }
    Write-Host ""
    Write-Host "Restoring task definition JSON from backup..." -ForegroundColor Green
    Copy-Item -Path $backupPath -Destination $taskDefPath -Force
    Remove-Item $backupPath -Force
}

Write-Host ""
Write-Host "Environment configuration complete!" -ForegroundColor Green
Write-Host ""
# Exit with appropriate code
if ($updatedVars.Count -gt 0) {
    # Changes were made (update successful)
    exit 0
} else {
    # No changes needed (no update required)
    exit 1
}