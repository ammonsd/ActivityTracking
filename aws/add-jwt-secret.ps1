#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Adds JWT_SECRET to ECS task definition

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== Add JWT_SECRET to ECS Task Definition ===" -ForegroundColor Cyan
Write-Host ""

# Configuration
$Region = "us-east-1"
$Cluster = "taskactivity-cluster"
$Service = "taskactivity-service"
$TaskFamily = "taskactivity"
$TempFile = "task-def-jwt-secret.json"

# Get JWT_SECRET
if (-not $env:JWT_SECRET) {
    if (Test-Path ".env.local") {
        $secretLine = Select-String -Path .env.local -Pattern "^JWT_SECRET=" | Select-Object -First 1
        if ($secretLine) {
            $env:JWT_SECRET = ($secretLine.Line -split "=", 2)[1]
        }
    }
}

if (-not $env:JWT_SECRET) {
    Write-Host "ERROR: JWT_SECRET not found!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ JWT_SECRET loaded" -ForegroundColor Green
Write-Host ""

# Get current task definition
Write-Host "Retrieving current task definition..." -ForegroundColor Cyan
aws ecs describe-task-definition `
    --task-definition $TaskFamily `
    --region $Region `
    --query 'taskDefinition' `
    --output json > $TempFile

$taskDef = Get-Content $TempFile | ConvertFrom-Json
$currentRevision = $taskDef.revision
Write-Host "  Current revision: $currentRevision" -ForegroundColor Gray

# Update environment variables
$containerDef = $taskDef.containerDefinitions[0]

# Check if JWT_SECRET already exists
$existingSecret = $containerDef.environment | Where-Object { $_.name -eq "JWT_SECRET" }
if ($existingSecret) {
    Write-Host "  JWT_SECRET already exists, updating value..." -ForegroundColor Yellow
    $existingSecret.value = $env:JWT_SECRET
} else {
    Write-Host "  Adding JWT_SECRET..." -ForegroundColor Yellow
    $containerDef.environment += @{
        name = "JWT_SECRET"
        value = $env:JWT_SECRET
    }
}

# Remove fields that shouldn't be in registration
$taskDef.PSObject.Properties.Remove('taskDefinitionArn')
$taskDef.PSObject.Properties.Remove('revision')
$taskDef.PSObject.Properties.Remove('status')
$taskDef.PSObject.Properties.Remove('requiresAttributes')
$taskDef.PSObject.Properties.Remove('compatibilities')
$taskDef.PSObject.Properties.Remove('registeredAt')
$taskDef.PSObject.Properties.Remove('registeredBy')

# Save updated definition
$taskDef | ConvertTo-Json -Depth 10 | Set-Content -Path $TempFile

Write-Host ""
Write-Host "Registering new task definition..." -ForegroundColor Cyan
$newTaskDefArn = aws ecs register-task-definition `
    --cli-input-json "file://$TempFile" `
    --region $Region `
    --query 'taskDefinition.taskDefinitionArn' `
    --output text

$newRevision = $newTaskDefArn -replace '.*:(\d+)$', '$1'
Write-Host "✓ Registered revision: $newRevision" -ForegroundColor Green

# Clean up
Remove-Item $TempFile -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Updating ECS service..." -ForegroundColor Cyan
aws ecs update-service `
    --cluster $Cluster `
    --service $Service `
    --task-definition "${TaskFamily}:${newRevision}" `
    --force-new-deployment `
    --region $Region `
    --no-cli-pager | Out-Null

Write-Host "✓ Service update initiated" -ForegroundColor Green
Write-Host ""
Write-Host "Deployment in progress..." -ForegroundColor Yellow
Write-Host "  Cluster: $Cluster"
Write-Host "  Service: $Service"
Write-Host "  New Revision: $newRevision"
Write-Host ""
Write-Host "Monitor deployment:" -ForegroundColor Cyan
Write-Host "  aws ecs describe-services --cluster $Cluster --services $Service --region $Region --query 'services[0].deployments' --output table"
Write-Host ""
