###############################################################################
# AWS Deployment Powershell script for Task Activity application
# 
# This script automates deployment of application to AWS using ECS Fargate.
#
# Prerequisites:
# • AWS CLI installed and configured with IAM user credentials (do NOT use the root account)
#	   Note: IAM user must have been assigned the policy "TaskActivityDeveloperPolicy" 
#            See ../aws/IAM_User_Setup.md for setup instructions
# • Appropriate AWS IAM permissions (see ./aws/taskactivity-developer-policy.json)
# • ECR repository created (taskactivity)
# • RDS PostgreSQL database created and initialized with schema
# • Secrets stored in AWS Secrets Manager (taskactivity/database)
#
# Deployment Process:
# The script performs the following steps:
#   1. Build the Spring Boot JAR with Maven (includes Angular production build)
#   2. Build the Angular application with production configuration
#   3. Create the Docker image with multi-stage build
#   4. Push the Docker image to AWS ECR (Elastic Container Registry)
#   5. Update the ECS service with new task definition
#   6. Wait for the new task to become healthy and stable
#
# Usage:
#   .\aws\deploy-aws.ps1 [-Environment <env>] [-Rollback] [-Status] [-NoCache]
#   
# Examples:
#   .\aws\deploy-aws.ps1 -Environment dev
#   .\aws\deploy-aws.ps1 -Environment production
#   .\aws\deploy-aws.ps1 -NoCache
#   .\aws\deploy-aws.ps1 -Status
#   .\aws\deploy-aws.ps1 -Rollback
#
# Author: Dean Ammons
# Date: October 2025
###############################################################################

param(
    [Parameter(Mandatory=$false)]
    [string]$Environment = "dev",
    
    [Parameter(Mandatory=$false)]
    [switch]$Rollback,
    
    [Parameter(Mandatory=$false)]
    [switch]$Status,
    
    [Parameter(Mandatory=$false)]
    [switch]$NoCache
)

# Stop on errors
$ErrorActionPreference = "Stop"

# ========================================
# Start Transcript Logging
# ========================================

# Only create log file if not a status check
if (-not $Status) {
    # Create logs directory if it doesn't exist
    $logDir = "C:\Logs"
    if (-not (Test-Path $logDir)) {
        New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    }

    # Start transcript with timestamp
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $logFile = Join-Path $logDir "AWS-Deployment_$timestamp.log"
    Start-Transcript -Path $logFile -Append

    Write-Host "Logging to: $logFile" -ForegroundColor Cyan
    Write-Host ""
}

# ========================================
# Configuration Variables
# ========================================

$AWS_REGION = "us-east-1"
$AWS_ACCOUNT_ID = (aws sts get-caller-identity --query Account --output text)

$APP_NAME = "taskactivity"
$ECR_REPOSITORY = "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$APP_NAME"
$ECS_CLUSTER = "$APP_NAME-cluster"
$ECS_SERVICE = "$APP_NAME-service"
$TASK_FAMILY = $APP_NAME

$BUILD_VERSION = Get-Date -Format "yyyyMMdd-HHmmss"
$IMAGE_TAG = $BUILD_VERSION

# Track which Docker we're using
$script:useWSLDocker = $false

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

function Test-Prerequisites {
    Write-Info "Checking prerequisites..."
    
    # Check AWS CLI
    if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
        Write-Error "AWS CLI is not installed. Please install it first."
        exit 1
    }
    
    # Check Docker (prefer WSL2 Docker for better compatibility with ECR)
    $dockerAvailable = $false
    
    # Try WSL2 Docker first (better for ECR login)
    try {
        wsl -e docker ps | Out-Null
        $dockerAvailable = $true
        $script:useWSLDocker = $true
        Write-Info "Using Docker from WSL2"
        # Set up Docker command wrapper for WSL2
        Set-Alias -Name docker-native -Value docker -Scope Script -ErrorAction SilentlyContinue
        function global:docker { wsl -e docker @args }
    } catch {
        Write-Warning "Docker not running in WSL2, trying Windows Docker..."
    }
    
    # If WSL2 Docker not available, try Windows Docker
    if (-not $dockerAvailable) {
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            try {
                docker ps | Out-Null
                $dockerAvailable = $true
                $script:useWSLDocker = $false
                Write-Info "Using Docker from Windows"
            } catch {
                Write-Warning "Docker command found but not running in Windows"
            }
        }
    }
    
    if (-not $dockerAvailable) {
        Write-Error "Docker is not running. Please start Docker in Windows or WSL2."
        exit 1
    }
    
    # Check AWS credentials
    try {
        aws sts get-caller-identity | Out-Null
    } catch {
        Write-Error "AWS credentials not configured. Run 'aws configure' first."
        exit 1
    }
    
    Write-Success "Prerequisites check passed"
}

function Build-AndPushImage {
    Write-Info "Building Docker image..."
    
    # Clean Angular build cache to prevent LMDB "Not enough space" errors
    Write-Info "Cleaning Angular build cache..."
    $angularCachePath = Join-Path $PWD "frontend\.angular"
    $nodeModulesCachePath = Join-Path $PWD "frontend\node_modules\.cache"
    
    if (Test-Path $angularCachePath) {
        Remove-Item -Recurse -Force $angularCachePath -ErrorAction SilentlyContinue
        Write-Info "Angular cache cleaned"
    }
    
    if (Test-Path $nodeModulesCachePath) {
        Remove-Item -Recurse -Force $nodeModulesCachePath -ErrorAction SilentlyContinue
        Write-Info "Node modules cache cleaned"
    }
    
    # Build the application
    Write-Info "Building Spring Boot application with Maven..."
    & .\mvnw.cmd clean package -DskipTests
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed"
        exit 1
    }
    
    # Build Docker image
    Write-Info "Building Docker image: ${APP_NAME}:${IMAGE_TAG}"
    
    $noCacheFlag = if ($NoCache) { "--no-cache" } else { "" }
    
    if ($script:useWSLDocker) {
        Write-Info "Using WSL2 Docker..."
        # Convert Windows path to WSL path for context
        $wslPath = wsl wslpath -a $PWD.Path
        wsl -e bash -c "cd '$wslPath' && docker build $noCacheFlag -t ${APP_NAME}:${IMAGE_TAG} -t ${APP_NAME}:latest ."
    } else {
        if ($NoCache) {
            docker build --no-cache -t "${APP_NAME}:${IMAGE_TAG}" -t "${APP_NAME}:latest" .
        } else {
            docker build -t "${APP_NAME}:${IMAGE_TAG}" -t "${APP_NAME}:latest" .
        }
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Docker build failed"
        exit 1
    }
    
    Write-Success "Docker image built successfully"
    
    # Check if ECR credential helper is configured
    $credHelperConfigured = $false
    if ($script:useWSLDocker) {
        $dockerConfig = wsl -e bash -c "cat ~/.docker/config.json 2>/dev/null || echo '{}'"
        if ($dockerConfig -match 'ecr-login') {
            $credHelperConfigured = $true
            Write-Info "ECR credential helper detected - skipping manual login"
        }
    }
    
    # Login to ECR only if credential helper is not configured
    if (-not $credHelperConfigured) {
        Write-Info "Logging into Amazon ECR..."
        
        if ($script:useWSLDocker) {
            # For WSL2: Get password from Windows AWS CLI, pipe directly to WSL Docker
            Write-Info "Using WSL2 Docker login method..."
            aws ecr get-login-password --region $AWS_REGION | wsl docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
        } else {
            $ecrPassword = aws ecr get-login-password --region $AWS_REGION
            $ecrPassword | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
        }
        
        if ($LASTEXITCODE -ne 0) {
            Write-Error "ECR login failed"
            exit 1
        }
    }
    
    # Tag images for ECR
    Write-Info "Tagging images for ECR..."
    if ($useWSL) {
        wsl -e docker tag "${APP_NAME}:${IMAGE_TAG}" "${ECR_REPOSITORY}:${IMAGE_TAG}"
        wsl -e docker tag "${APP_NAME}:latest" "${ECR_REPOSITORY}:latest"
    } else {
        docker tag "${APP_NAME}:${IMAGE_TAG}" "${ECR_REPOSITORY}:${IMAGE_TAG}"
        docker tag "${APP_NAME}:latest" "${ECR_REPOSITORY}:latest"
    }
    
    # Push to ECR
    Write-Info "Pushing images to Amazon ECR..."
    if ($useWSL) {
        wsl -e docker push "${ECR_REPOSITORY}:${IMAGE_TAG}"
        wsl -e docker push "${ECR_REPOSITORY}:latest"
    } else {
        docker push "${ECR_REPOSITORY}:${IMAGE_TAG}"
        docker push "${ECR_REPOSITORY}:latest"
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Docker push failed"
        exit 1
    }
    
    Write-Success "Images pushed to ECR successfully"
    Write-Info "Image: ${ECR_REPOSITORY}:${IMAGE_TAG}"
}

function Update-TaskDefinition {
    Write-Info "Updating ECS task definition..."
    
    $TASK_DEF_FILE = "aws\taskactivity-task-definition.json"
    
    if (-not (Test-Path $TASK_DEF_FILE)) {
        Write-Error "Task definition file not found: $TASK_DEF_FILE"
        exit 1
    }
    
    # Read and replace the image tag
    $taskDefContent = Get-Content $TASK_DEF_FILE -Raw
    # Replace the full ECR image reference (not just "taskactivity:latest")
    $taskDefContent = $taskDefContent -replace "${ECR_REPOSITORY}:latest", "${ECR_REPOSITORY}:${IMAGE_TAG}"
    
    # Save to temp file in current directory (use absolute path)
    $tempFile = Join-Path $PSScriptRoot "temp-task-definition-$IMAGE_TAG.json"
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($tempFile, $taskDefContent, $utf8NoBom)
    
    # Register new task definition
    Write-Info "Registering new task definition..."
    $taskRevision = aws ecs register-task-definition `
        --cli-input-json "file://$tempFile" `
        --query 'taskDefinition.revision' `
        --output text 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to register task definition. Error: $taskRevision"
        Remove-Item $tempFile -ErrorAction SilentlyContinue
        exit 1
    }
    
    Remove-Item $tempFile -ErrorAction SilentlyContinue
    
    Write-Success "Task definition registered: ${TASK_FAMILY}:${taskRevision}"
    
    return $taskRevision
}

function Deploy-ToECS {
    param([string]$TaskRevision)
    
    Write-Info "Deploying to ECS cluster: $ECS_CLUSTER"
    
    # Check if service exists
    $serviceStatus = aws ecs describe-services `
        --cluster $ECS_CLUSTER `
        --services $ECS_SERVICE `
        --query 'services[0].status' `
        --output text 2>$null
    
    if ($serviceStatus -eq "ACTIVE") {
        # Update existing service
        Write-Info "Updating existing ECS service..."
        $updateResult = aws ecs update-service `
            --cluster $ECS_CLUSTER `
            --service $ECS_SERVICE `
            --task-definition "${TASK_FAMILY}:${TaskRevision}" `
            --force-new-deployment `
            --output json 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to update ECS service. Error: $updateResult"
            exit 1
        }
        
        Write-Success "ECS service update initiated"
        
        # Wait for deployment to complete
        Write-Info "Waiting for deployment to complete (this may take several minutes)..."
        aws ecs wait services-stable `
            --cluster $ECS_CLUSTER `
            --services $ECS_SERVICE
        
        Write-Success "Deployment completed successfully!"
    } else {
        Write-Warning "Service $ECS_SERVICE does not exist or is not ACTIVE"
        Write-Info "Please create the service manually or use the AWS Console"
        Write-Info "Task Definition: ${TASK_FAMILY}:${TaskRevision}"
    }
}

function Get-DeploymentStatus {
    Write-Info "Fetching deployment status..."
    
    $serviceInfo = aws ecs describe-services `
        --cluster $ECS_CLUSTER `
        --services $ECS_SERVICE `
        --output json | ConvertFrom-Json
    
    $runningCount = $serviceInfo.services[0].runningCount
    $desiredCount = $serviceInfo.services[0].desiredCount
    $taskDef = $serviceInfo.services[0].taskDefinition
    
    Write-Host ""
    Write-Host "===================================  Deployment Status  ===================================" -ForegroundColor Cyan
    Write-Host "Cluster:         $ECS_CLUSTER"
    Write-Host "Service:         $ECS_SERVICE"
    Write-Host "Task Definition: $taskDef"
    Write-Host "Image:           ${ECR_REPOSITORY}:${IMAGE_TAG}"
    Write-Host "Running Tasks:   $runningCount/$desiredCount"
    Write-Host "===========================================================================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Get ALB endpoint
    Write-Info "Getting Application Load Balancer endpoint..."
    try {
        $albDns = aws elbv2 describe-load-balancers `
            --names "$APP_NAME-alb" `
            --query 'LoadBalancers[0].DNSName' `
            --output text 2>$null
        
        if ($albDns -and $albDns -ne "None") {
            Write-Success "Application URL: https://$albDns"
            Write-Info "Health Check: https://$albDns/actuator/health"
        }
    } catch {
        Write-Info "ALB not configured or not found"
    }
}

function Invoke-Rollback {
    Write-Warning "Initiating rollback to previous task definition..."
    
    $previousTaskDef = aws ecs describe-services `
        --cluster $ECS_CLUSTER `
        --services $ECS_SERVICE `
        --query 'services[0].deployments[1].taskDefinition' `
        --output text
    
    if (-not $previousTaskDef -or $previousTaskDef -eq "None") {
        Write-Error "No previous task definition found for rollback"
        exit 1
    }
    
    Write-Info "Rolling back to: $previousTaskDef"
    
    aws ecs update-service `
        --cluster $ECS_CLUSTER `
        --service $ECS_SERVICE `
        --task-definition $previousTaskDef `
        --force-new-deployment `
        --output text | Out-Null
    
    Write-Success "Rollback initiated"
    Get-DeploymentStatus
}

# ========================================
# Main Execution
# ========================================

Write-Host ""
Write-Host "===================  AWS Deployment  ===================" -ForegroundColor Cyan
Write-Host "Application: Task Activity Management" -ForegroundColor Cyan
Write-Host "Environment: $Environment" -ForegroundColor Cyan
Write-Host "AWS Region:  $AWS_REGION" -ForegroundColor Cyan
Write-Host "AWS Account: $AWS_ACCOUNT_ID" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

# Handle switches
if ($Status) {
    Get-DeploymentStatus
    exit 0
}

if ($Rollback) {
    Invoke-Rollback
    # Stop transcript before exit
    if (-not $Status) {
        Stop-Transcript
    }
    exit 0
}

# Check prerequisites
Test-Prerequisites

# Build and push Docker image
Build-AndPushImage

# Update task definition
$taskRevision = Update-TaskDefinition

# Deploy to ECS
Deploy-ToECS -TaskRevision $taskRevision

# Show deployment status
Get-DeploymentStatus

Write-Success "Deployment process completed!"
Write-Info "Monitor the deployment in the AWS Console: https://console.aws.amazon.com/ecs"

# ========================================
# Stop Transcript Logging
# ========================================
if (-not $Status) {
    Stop-Transcript
}
