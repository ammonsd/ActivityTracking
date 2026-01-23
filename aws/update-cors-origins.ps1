<#
.SYNOPSIS
    Update CORS allowed origins for the Task Activity application.

.DESCRIPTION
    Updates the CORS_ALLOWED_ORIGINS environment variable in the ECS task definition
    to allow cross-origin requests from S3-hosted static files.

.PARAMETER ClusterName
    ECS cluster name (default: taskactivity-cluster)

.PARAMETER ServiceName
    ECS service name (default: taskactivity-service)

.PARAMETER Origins
    Comma-separated list of allowed origins

.EXAMPLE
    .\update-cors-origins.ps1 -Origins "https://taskactivitytracker.com,https://taskactivity-docs.s3.us-east-1.amazonaws.com"

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

param(
    [string]$ClusterName = "taskactivity-cluster",
    [string]$ServiceName = "taskactivity-service",
    [Parameter(Mandatory=$true)]
    [string]$Origins
)

Write-Host "Updating CORS allowed origins for ECS service..." -ForegroundColor Cyan

# Get the current task definition
Write-Host "Fetching current task definition..." -ForegroundColor Yellow
$service = aws ecs describe-services `
    --cluster $ClusterName `
    --services $ServiceName `
    --query 'services[0].taskDefinition' `
    --output text

if (-not $service) {
    Write-Error "Failed to find service: $ServiceName"
    exit 1
}

Write-Host "Current task definition: $service" -ForegroundColor Green

# Get the task definition JSON
$taskDefJson = aws ecs describe-task-definition `
    --task-definition $service `
    --query 'taskDefinition' `
    --output json | ConvertFrom-Json

# Update the CORS_ALLOWED_ORIGINS environment variable
$containerDef = $taskDefJson.containerDefinitions[0]
$corsEnvVar = $containerDef.environment | Where-Object { $_.name -eq "CORS_ALLOWED_ORIGINS" }

if ($corsEnvVar) {
    Write-Host "Updating existing CORS_ALLOWED_ORIGINS..." -ForegroundColor Yellow
    $corsEnvVar.value = $Origins
} else {
    Write-Host "Adding new CORS_ALLOWED_ORIGINS..." -ForegroundColor Yellow
    $containerDef.environment += @{
        name = "CORS_ALLOWED_ORIGINS"
        value = $Origins
    }
}

# Remove fields that cannot be included in registration
$taskDefJson.PSObject.Properties.Remove('taskDefinitionArn')
$taskDefJson.PSObject.Properties.Remove('revision')
$taskDefJson.PSObject.Properties.Remove('status')
$taskDefJson.PSObject.Properties.Remove('requiresAttributes')
$taskDefJson.PSObject.Properties.Remove('compatibilities')
$taskDefJson.PSObject.Properties.Remove('registeredAt')
$taskDefJson.PSObject.Properties.Remove('registeredBy')

# Save to temporary file
$tempFile = [System.IO.Path]::GetTempFileName()
$taskDefJson | ConvertTo-Json -Depth 10 | Out-File -FilePath $tempFile -Encoding utf8

Write-Host "Registering new task definition..." -ForegroundColor Yellow
$newTaskDef = aws ecs register-task-definition `
    --cli-input-json file://$tempFile `
    --query 'taskDefinition.taskDefinitionArn' `
    --output text

Remove-Item $tempFile

if (-not $newTaskDef) {
    Write-Error "Failed to register new task definition"
    exit 1
}

Write-Host "New task definition: $newTaskDef" -ForegroundColor Green

# Update the service to use the new task definition
Write-Host "Updating ECS service with new task definition..." -ForegroundColor Yellow
aws ecs update-service `
    --cluster $ClusterName `
    --service $ServiceName `
    --task-definition $newTaskDef `
    --force-new-deployment

Write-Host "`nCORS origins updated successfully!" -ForegroundColor Green
Write-Host "Origins: $Origins" -ForegroundColor Cyan
Write-Host "`nThe service is being redeployed. This may take a few minutes." -ForegroundColor Yellow
Write-Host "Monitor deployment: aws ecs describe-services --cluster $ClusterName --services $ServiceName --query 'services[0].deployments'" -ForegroundColor Gray
