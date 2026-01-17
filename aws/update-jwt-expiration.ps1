#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Updates JWT_EXPIRATION environment variable in AWS ECS task definition.

.DESCRIPTION
    This script simplifies the process of extending JWT token lifetime in the
    ECS task definition. It downloads the current task definition, updates the
    JWT_EXPIRATION value, and deploys the new revision.
    
    Default changes JWT_EXPIRATION from 24 hours (86400000) to 30 days (2592000000).

.PARAMETER ExpirationDays
    Number of days for JWT token lifetime. Default: 30 days.
    Common values: 1 (24h), 7 (1 week), 30 (1 month)

.PARAMETER DryRun
    If specified, shows what would be changed without making actual AWS updates.

.EXAMPLE
    .\update-jwt-expiration.ps1
    Updates JWT_EXPIRATION to 30 days (default).

.EXAMPLE
    .\update-jwt-expiration.ps1 -ExpirationDays 7
    Updates JWT_EXPIRATION to 7 days.

.EXAMPLE
    .\update-jwt-expiration.ps1 -DryRun
    Shows what would be changed without making actual updates.

.NOTES
    Author: Dean Ammons
    Date: January 2026
    
    Prerequisites:
    - AWS CLI installed and configured
    - AWS credentials with ECS permissions
    - Task definition: taskactivity
    - Cluster: taskactivity-cluster
    - Service: taskactivity
#>

[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [Parameter()]
    [ValidateRange(1, 365)]
    [int]$ExpirationDays = 30,
    
    [Parameter()]
    [switch]$DryRun
)

# Configuration
$TaskDefinition = "taskactivity"
$Cluster = "taskactivity-cluster"
$Service = "taskactivity-service"
$Region = "us-east-1"
$TempFile = "task-def-temp.json"

Write-Host ""
Write-Host "=== JWT Expiration Update Script ===" -ForegroundColor Cyan
Write-Host "Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray
Write-Host ""

# Calculate expiration in milliseconds
$ExpirationMs = $ExpirationDays * 24 * 60 * 60 * 1000

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Expiration Days:   $ExpirationDays days" -ForegroundColor White
Write-Host "  Expiration Ms:     $ExpirationMs ms" -ForegroundColor Gray
Write-Host "  Task Definition:   $TaskDefinition" -ForegroundColor Gray
Write-Host "  Cluster:           $Cluster" -ForegroundColor Gray
Write-Host "  Service:           $Service" -ForegroundColor Gray
Write-Host "  Region:            $Region" -ForegroundColor Gray
Write-Host ""

if ($DryRun) {
    Write-Host "⚠️  DRY RUN MODE - No changes will be made" -ForegroundColor Yellow
    Write-Host ""
}

# Step 1: Download current task definition
Write-Host "[1/5] Downloading current task definition..." -ForegroundColor Cyan

try {
    $taskDefJson = aws ecs describe-task-definition `
        --task-definition $TaskDefinition `
        --region $Region `
        --query 'taskDefinition' 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to download task definition: $taskDefJson"
    }
    
    $taskDef = $taskDefJson | ConvertFrom-Json
    Write-Host "  ✓ Downloaded revision: $($taskDef.revision)" -ForegroundColor Green
} catch {
    Write-Error "Failed to download task definition: $_"
    exit 1
}

# Step 2: Find and update JWT_EXPIRATION
Write-Host ""
Write-Host "[2/5] Updating JWT_EXPIRATION value..." -ForegroundColor Cyan

$container = $taskDef.containerDefinitions[0]
$envVars = $container.environment
$jwtExpirationFound = $false

foreach ($env in $envVars) {
    if ($env.name -eq "JWT_EXPIRATION") {
        $oldValue = $env.value
        $oldDays = [math]::Round([int]$oldValue / (24 * 60 * 60 * 1000), 1)
        
        Write-Host "  Current value: $oldValue ms ($oldDays days)" -ForegroundColor Yellow
        Write-Host "  New value:     $ExpirationMs ms ($ExpirationDays days)" -ForegroundColor Green
        
        $env.value = $ExpirationMs.ToString()
        $jwtExpirationFound = $true
        break
    }
}

if (-not $jwtExpirationFound) {
    Write-Host "  ⚠️  JWT_EXPIRATION not found, adding new variable" -ForegroundColor Yellow
    $newEnv = [PSCustomObject]@{
        name = "JWT_EXPIRATION"
        value = $ExpirationMs.ToString()
    }
    $envVars += $newEnv
    $container.environment = $envVars
}

# Step 3: Remove read-only fields
Write-Host ""
Write-Host "[3/5] Cleaning task definition JSON..." -ForegroundColor Cyan

$readOnlyFields = @(
    'taskDefinitionArn', 'revision', 'status', 
    'requiresAttributes', 'compatibilities', 
    'registeredAt', 'registeredBy'
)

foreach ($field in $readOnlyFields) {
    if ($taskDef.PSObject.Properties.Name -contains $field) {
        $taskDef.PSObject.Properties.Remove($field)
        Write-Host "  Removed: $field" -ForegroundColor Gray
    }
}

Write-Host "  ✓ Cleaned task definition" -ForegroundColor Green

# Save to temp file
$taskDef | ConvertTo-Json -Depth 10 | Set-Content -Path $TempFile -Encoding UTF8
Write-Host "  ✓ Saved to: $TempFile" -ForegroundColor Gray

if ($DryRun) {
    Write-Host ""
    Write-Host "=== DRY RUN: Changes that would be made ===" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "JWT_EXPIRATION would be updated to: $ExpirationMs ms ($ExpirationDays days)" -ForegroundColor White
    Write-Host ""
    Write-Host "To apply these changes, run without -DryRun flag" -ForegroundColor Gray
    Write-Host ""
    
    # Cleanup
    Remove-Item $TempFile -ErrorAction SilentlyContinue
    exit 0
}

# Step 4: Register new task definition
Write-Host ""
Write-Host "[4/5] Registering new task definition..." -ForegroundColor Cyan

try {
    $registerResult = aws ecs register-task-definition `
        --cli-input-json "file://$TempFile" `
        --region $Region 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to register task definition: $registerResult"
    }
    
    $newTaskDef = $registerResult | ConvertFrom-Json
    $newRevision = $newTaskDef.taskDefinition.revision
    Write-Host "  ✓ Registered new revision: $newRevision" -ForegroundColor Green
} catch {
    Write-Error "Failed to register new task definition: $_"
    Remove-Item $TempFile -ErrorAction SilentlyContinue
    exit 1
}

# Step 5: Update service with new task definition
Write-Host ""
Write-Host "[5/5] Updating ECS service (force new deployment)..." -ForegroundColor Cyan

try {
    $updateResult = aws ecs update-service `
        --cluster $Cluster `
        --service $Service `
        --task-definition $TaskDefinition `
        --force-new-deployment `
        --region $Region 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to update service: $updateResult"
    }
    
    Write-Host "  ✓ Service update initiated" -ForegroundColor Green
} catch {
    Write-Error "Failed to update service: $_"
    Remove-Item $TempFile -ErrorAction SilentlyContinue
    exit 1
}

# Cleanup
Remove-Item $TempFile -ErrorAction SilentlyContinue

# Success
Write-Host ""
Write-Host "=== Update Complete! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Wait 5-10 minutes for ECS deployment to complete" -ForegroundColor White
Write-Host "  2. Monitor deployment status:" -ForegroundColor White
Write-Host "     aws ecs describe-services --cluster $Cluster --services $Service --region $Region --query 'services[0].deployments'" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. After deployment completes, generate new Jenkins API token:" -ForegroundColor White
Write-Host "     See: jenkins/Jenkins_Token_Maintenance_Guide.md" -ForegroundColor Gray
Write-Host ""
Write-Host "  4. Update Jenkins credential 'jenkins-api-token' with new token" -ForegroundColor White
Write-Host "     Jenkins → Manage Jenkins → Credentials → jenkins-api-token → Update" -ForegroundColor Gray
Write-Host ""
Write-Host "  5. Test with a Jenkins build and verify email notifications work" -ForegroundColor White
Write-Host ""

Write-Host "JWT tokens will now be valid for $ExpirationDays days" -ForegroundColor Cyan
Write-Host ""
