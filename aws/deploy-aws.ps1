<#
.SYNOPSIS
    AWS ECS Fargate Deployment Script.

.DESCRIPTION
    Automates deployment of containerized applications to AWS using ECS Fargate.
    
    Prerequisites:
    • AWS CLI installed and configured with IAM user credentials (do NOT use root account)
      Note: IAM user must have appropriate ECS, ECR, and related service permissions
            Consult your project's IAM setup documentation for details
    • Appropriate AWS IAM permissions for ECS, ECR, and related services
    • ECR repository created for your application
    • RDS database created and initialized (if applicable)
    • Application secrets stored in AWS Secrets Manager
    
    Deployment Process:
    The script performs the following steps:
    1. Clean old Angular and React builds from source tree (prevents duplicate files in JAR)
    2. Kill any stuck Node.js processes
    3. Clean Angular and React build caches
    4. Prune Docker build cache (WSL2 only)
    5. Build the Spring Boot JAR with Maven (includes Angular and React production builds)
    6. Build the Docker image with multi-stage build
    7. Push the Docker image to AWS ECR (Elastic Container Registry)
    8. Update the ECS service with new task definition
    9. Wait for the new task to become healthy and stable

.PARAMETER Environment
    Target environment (default: dev).

.PARAMETER RunTests
    Run all Maven tests before building (default: skips tests).

.PARAMETER NoCache
    Build Docker image without cache.

.PARAMETER Status
    Check current deployment status.

.PARAMETER Rollback
    Rollback to previous task definition.

.PARAMETER EnableEmail
    Enable email notifications (requires -MailFrom and -AdminEmail).

.PARAMETER UseAwsSdk
    Use AWS SES SDK instead of SMTP (only applies when -EnableEmail is set).

.PARAMETER MailFrom
    Email address to send from (defaults to MAIL_FROM env var).

.PARAMETER AdminEmail
    Administrator email address (defaults to ADMIN_EMAIL env var).

.PARAMETER SkipEnvFile
    Skip loading environment variables from .env file.

.PARAMETER EnvFile
    Path to environment file. Defaults to ../.env if not specified.

.PARAMETER OverrideExisting
    Override existing environment variables. Defaults to $false.

.PARAMETER EncryptionKey
    Encryption key for sensitive data. Passed to set-env-values.ps1 for decryption.

.PARAMETER UpdateJenkinsMarker
    Update the S3 deployment marker to prevent Jenkins from auto-deploying.
    Use this when manually deploying during the day to avoid duplicate deployments.

.EXAMPLE
    .\deploy-aws.ps1 -Environment dev
    Deploy to development environment.

.EXAMPLE
    .\deploy-aws.ps1 -Environment production -RunTests
    Deploy to production with tests.

.EXAMPLE
    .\deploy-aws.ps1 -NoCache
    Build without Docker cache.

.EXAMPLE
    .\deploy-aws.ps1 -Status
    Check deployment status.

.EXAMPLE
    .\deploy-aws.ps1 -Rollback
    Rollback to previous version.

.EXAMPLE
    .\deploy-aws.ps1 -EnableEmail -MailFrom "noreply@example.com" -AdminEmail "admin@example.com" -SkipEnvFile
    Deploy with email notifications enabled.

.EXAMPLE
    .\deploy-aws.ps1 -EncryptionKey "N1ghrd+1968" -OverrideExisting:$true
    Deploy with encryption key and override existing environment variables.

.EXAMPLE
    .\deploy-aws.ps1 -UpdateJenkinsMarker
    Deploy and update S3 marker to prevent Jenkins from deploying again (prevents duplicate deploys).

.NOTES
    Author: Dean Ammons
    Date: December 2025
#>

param(
    [Parameter(Mandatory=$false)]
    [string]$Environment = "dev",
    
    [Parameter(Mandatory=$false)]
    [switch]$Rollback,
    
    [Parameter(Mandatory=$false)]
    [switch]$Status,
    
    [Parameter(Mandatory=$false)]
    [switch]$NoCache,
    
    [Parameter(Mandatory=$false)]
    [switch]$RunTests,
    
    [Parameter(Mandatory=$false)]
    [switch]$EnableEmail,
    
    [Parameter(Mandatory=$false)]
    [switch]$UseAwsSdk,
    
    [Parameter(Mandatory=$false)]
    [string]$MailFrom = "",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminEmail = "",
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipEnvFile,
    
    [Parameter(Mandatory=$false)]
    [string]$EnvFile = "",
    
    [Parameter(Mandatory=$false)]
    [bool]$OverrideExisting = $false,
    
    [Parameter(Mandatory=$false)]
    [string]$EncryptionKey = "",
    
    [Parameter(Mandatory=$false)]
    [switch]$UpdateJenkinsMarker
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
# Load Environment Variables from .env
# ========================================

if (-not $SkipEnvFile) {
    # Determine which .env file to use
    if ([string]::IsNullOrWhiteSpace($EnvFile)) {
        $envFilePath = Join-Path $PSScriptRoot "..\.env"
    } else {
        $envFilePath = $EnvFile
    }
    
    $setEnvScript = Join-Path $PSScriptRoot "..\scripts\set-env-values.ps1"
    
    if (Test-Path $setEnvScript) {
        Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan
        # Capture output and write to both console and transcript
        if (-not [string]::IsNullOrWhiteSpace($EncryptionKey)) {
            $envOutput = & $setEnvScript -envFile $envFilePath -overrideExisting $OverrideExisting -EncryptionKey $EncryptionKey 2>&1
        } else {
            $envOutput = & $setEnvScript -envFile $envFilePath -overrideExisting $OverrideExisting 2>&1
        }
        $envOutput | ForEach-Object { Write-Host $_ }
        Write-Host ""
    } else {
        Write-Warning "set-env-values.ps1 not found. Environment variables will not be loaded."
    }
}

# ========================================
# Apply Environment Variable Defaults
# ========================================

# Email parameters: Use CLI params if provided, otherwise fall back to environment variables
if ([string]::IsNullOrWhiteSpace($MailFrom)) {
    $MailFrom = $env:MAIL_FROM
    if (-not [string]::IsNullOrWhiteSpace($MailFrom)) {
        Write-Host "Using MAIL_FROM from environment: $MailFrom" -ForegroundColor Gray
    }
}

if ([string]::IsNullOrWhiteSpace($AdminEmail)) {
    $AdminEmail = $env:ADMIN_EMAIL
    if (-not [string]::IsNullOrWhiteSpace($AdminEmail)) {
        Write-Host "Using ADMIN_EMAIL from environment: $AdminEmail" -ForegroundColor Gray
    }
}

# EnableEmail switch: Check if MAIL_ENABLED environment variable is set to "true"
# Note: We check $PSBoundParameters to see if the switch was explicitly provided by the user
if (-not $PSBoundParameters.ContainsKey('EnableEmail') -and $env:MAIL_ENABLED -eq "true") {
    $EnableEmail = $true
    Write-Host "Email enabled via MAIL_ENABLED environment variable" -ForegroundColor Gray
}

# UseAwsSdk switch: Check if MAIL_USE_AWS_SDK environment variable is set to "true"
# Note: We check $PSBoundParameters to see if the switch was explicitly provided by the user
if (-not $PSBoundParameters.ContainsKey('UseAwsSdk') -and $env:MAIL_USE_AWS_SDK -eq "true") {
    $UseAwsSdk = $true
    Write-Host "Using AWS SES SDK via MAIL_USE_AWS_SDK environment variable" -ForegroundColor Gray
}

# Display Jenkins environment variables if loaded (helps verify decryption worked)
if (-not [string]::IsNullOrWhiteSpace($env:JENKINS_BUILD_NOTIFICATION_EMAIL)) {
    Write-Host "Using JENKINS_BUILD_NOTIFICATION_EMAIL from environment: $env:JENKINS_BUILD_NOTIFICATION_EMAIL" -ForegroundColor Gray
}

if (-not [string]::IsNullOrWhiteSpace($env:JENKINS_DEPLOY_NOTIFICATION_EMAIL)) {
    Write-Host "Using JENKINS_DEPLOY_NOTIFICATION_EMAIL from environment: $env:JENKINS_DEPLOY_NOTIFICATION_EMAIL" -ForegroundColor Gray
}

if (-not [string]::IsNullOrWhiteSpace($env:JENKINS_DEPLOY_SKIPPED_CHECK)) {
    Write-Host "Using JENKINS_DEPLOY_SKIPPED_CHECK from environment: $env:JENKINS_DEPLOY_SKIPPED_CHECK" -ForegroundColor Gray
}

Write-Host ""

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
    
    # Remove old Angular builds from source tree to prevent duplicate files in JAR
    Write-Info "Cleaning old Angular builds from source tree..."
    $sourceStaticAppPath = Join-Path $PWD "src\main\resources\static\app"
    if (Test-Path $sourceStaticAppPath) {
        try {
            Remove-Item -Recurse -Force $sourceStaticAppPath -ErrorAction Stop
            Write-Info "Removed old Angular files from src/main/resources/static/app"
        } catch {
            Write-Error "Failed to delete old Angular builds from $sourceStaticAppPath. Error: $_"
            Write-Error "These files should not be committed to source control."
            exit 1
        }
    }
    
    # Remove old React builds from source tree to prevent duplicate files in JAR
    Write-Info "Cleaning old React builds from source tree..."
    $sourceStaticDashboardPath = Join-Path $PWD "src\main\resources\static\dashboard"
    if (Test-Path $sourceStaticDashboardPath) {
        try {
            Remove-Item -Recurse -Force $sourceStaticDashboardPath -ErrorAction Stop
            Write-Info "Removed old React files from src/main/resources/static/dashboard"
        } catch {
            Write-Error "Failed to delete old React builds from $sourceStaticDashboardPath. Error: $_"
            Write-Error "These files should not be committed to source control."
            exit 1
        }
    }
    
    # Kill any stuck Node.js processes that might interfere with the build
    Write-Info "Checking for stuck Node.js processes..."
    try {
        taskkill /F /IM node.exe >$null 2>&1
    } catch {
        Write-Info "No Node.js processes found...."
    }
    
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
    
    # Clean React build cache
    Write-Info "Cleaning React build cache..."
    $reactCachePath = Join-Path $PWD "frontend-react\.cache"
    $reactNodeModulesCachePath = Join-Path $PWD "frontend-react\node_modules\.cache"
    
    if (Test-Path $reactCachePath) {
        Remove-Item -Recurse -Force $reactCachePath -ErrorAction SilentlyContinue
        Write-Info "React cache cleaned"
    }
    
    if (Test-Path $reactNodeModulesCachePath) {
        Remove-Item -Recurse -Force $reactNodeModulesCachePath -ErrorAction SilentlyContinue
        Write-Info "React node modules cache cleaned"
    }
    
    # Clean Docker build cache if using WSL2 Docker (prevents disk space issues)
    if ($script:useWSLDocker) {
        Write-Info "Pruning Docker build cache in WSL2..."
        wsl -u root bash -c "docker builder prune -af" 2>$null | Out-Null
        Write-Info "Docker build cache pruned"
    }
    
    # Build the application
    Write-Info "Building Spring Boot application with Maven..."
    
    if ($RunTests) {
        Write-Info "Running all tests before build (this may take several minutes)..."
        & .\mvnw.cmd clean test
        
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Tests failed - aborting deployment"
            exit 1
        }
        
        Write-Success "All tests passed"
        Write-Info "Building package..."
        & .\mvnw.cmd package -DskipTests
    } else {
        Write-Info "Skipping tests (use -RunTests to enable)"
        & .\mvnw.cmd clean package -DskipTests
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed"
        exit 1
    }
    
    # Build Docker image
    Write-Info "Building Docker image: ${APP_NAME}:${IMAGE_TAG}"
    Write-Info "Using Dockerfile.local (JAR already built by Maven)"
    
    $noCacheFlag = if ($NoCache) { "--no-cache" } else { "" }
    
    if ($script:useWSLDocker) {
        Write-Info "Using WSL2 Docker..."
        # Convert Windows path to WSL path for context
        $wslPath = wsl wslpath -a $PWD.Path
        wsl -e bash -c "cd '$wslPath' && docker build $noCacheFlag -f Dockerfile.local -t ${APP_NAME}:${IMAGE_TAG} -t ${APP_NAME}:latest ."
    } else {
        if ($NoCache) {
            docker build --no-cache -f Dockerfile.local -t "${APP_NAME}:${IMAGE_TAG}" -t "${APP_NAME}:latest" .
        } else {
            docker build -f Dockerfile.local -t "${APP_NAME}:${IMAGE_TAG}" -t "${APP_NAME}:latest" .
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
    if ($script:useWSLDocker) {
        wsl -e docker tag "${APP_NAME}:${IMAGE_TAG}" "${ECR_REPOSITORY}:${IMAGE_TAG}"
        wsl -e docker tag "${APP_NAME}:latest" "${ECR_REPOSITORY}:latest"
    } else {
        docker tag "${APP_NAME}:${IMAGE_TAG}" "${ECR_REPOSITORY}:${IMAGE_TAG}"
        docker tag "${APP_NAME}:latest" "${ECR_REPOSITORY}:latest"
    }
    
    # Push to ECR
    Write-Info "Pushing images to Amazon ECR..."
    if ($script:useWSLDocker) {
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
    
    # Task definition file should match your application name
    $TASK_DEF_FILE = "aws\taskactivity-task-definition.json"
    
    if (-not (Test-Path $TASK_DEF_FILE)) {
        Write-Error "Task definition file not found: $TASK_DEF_FILE"
        exit 1
    }
    
    # Read task definition
    $taskDef = Get-Content $TASK_DEF_FILE -Raw | ConvertFrom-Json
    
    # Replace the image tag
    $taskDef.containerDefinitions[0].image = "${ECR_REPOSITORY}:${IMAGE_TAG}"
    
    # Add/Update email environment variables if enabled
    if ($EnableEmail) {
        # Validate required email parameters
        if ([string]::IsNullOrWhiteSpace($MailFrom)) {
            Write-Error "When -EnableEmail is specified, -MailFrom parameter is required (e.g., -MailFrom 'noreply@yourdomain.com')"
            exit 1
        }
        if ([string]::IsNullOrWhiteSpace($AdminEmail)) {
            Write-Error "When -EnableEmail is specified, -AdminEmail parameter is required (e.g., -AdminEmail 'admin@yourdomain.com')"
            exit 1
        }
        
        $envVars = $taskDef.containerDefinitions[0].environment
        
        # Helper function to update or add environment variable
        function Set-EnvVar($name, $value) {
            $existing = $envVars | Where-Object { $_.name -eq $name }
            if ($existing) {
                $existing.value = $value
            } else {
                $envVars += @{ name = $name; value = $value }
            }
        }
        
        Set-EnvVar "MAIL_ENABLED" "true"
        Set-EnvVar "MAIL_USE_AWS_SDK" $(if ($UseAwsSdk) { "true" } else { "false" })
        Set-EnvVar "MAIL_FROM" $MailFrom
        Set-EnvVar "ADMIN_EMAIL" $AdminEmail
        
        $taskDef.containerDefinitions[0].environment = $envVars
        
        Write-Info "Email configuration added to task definition"
    }
    
    # Convert back to JSON and save
    $taskDefContent = $taskDef | ConvertTo-Json -Depth 10
    
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

function Update-JenkinsDeploymentMarker {
    Write-Info "Updating Jenkins deployment marker in S3..."
    
    try {
        $s3Bucket = "taskactivity-logs-archive"
        $s3KeyDeploy = "jenkins-build-history/TaskActivity-Pipeline-deploy-last-build.txt"
        $s3KeyBuildOnly = "jenkins-build-history/TaskActivity-Pipeline-build-only-last-build.txt"
        
        # Get the latest successful Jenkins build number from S3
        $lastBuildNumber = aws s3 cp "s3://${s3Bucket}/${s3KeyBuildOnly}" - 2>$null
        
        if ([string]::IsNullOrWhiteSpace($lastBuildNumber)) {
            Write-Warning "Could not find last Jenkins build number in S3. Using timestamp as marker."
            $buildMarker = Get-Date -Format "yyyyMMddHHmmss"
        } else {
            $buildMarker = $lastBuildNumber.Trim()
            Write-Info "Using Jenkins build number: $buildMarker"
        }
        
        # Create timestamp for metadata
        $timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ" -AsUTC
        
        # Update the deployment marker (write build number to S3)
        $buildMarker | aws s3 cp - "s3://${s3Bucket}/${s3KeyDeploy}" `
            --content-type "text/plain" `
            --metadata "buildNumber=${buildMarker},actionType=deploy,timestamp=${timestamp},source=manual-powershell" 2>&1 | Out-Null
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Jenkins deployment marker updated successfully"
            Write-Info "Jenkins will skip next scheduled deployment (no new builds since this deploy)"
        } else {
            Write-Warning "Failed to update Jenkins deployment marker - Jenkins may still auto-deploy"
        }
        
    } catch {
        Write-Warning "Error updating Jenkins deployment marker: $_"
        Write-Warning "This is non-critical, but Jenkins may perform a duplicate deployment"
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
    Write-Host "Running Tasks:   $runningCount/$desiredCount"
    Write-Host ""
    
    # Get running task details
    $taskArns = aws ecs list-tasks `
        --cluster $ECS_CLUSTER `
        --service-name $ECS_SERVICE `
        --desired-status RUNNING `
        --query 'taskArns[0]' `
        --output text
    
    if ($taskArns -and $taskArns -ne "None") {
        $taskDetails = aws ecs describe-tasks `
            --cluster $ECS_CLUSTER `
            --tasks $taskArns `
            --output json | ConvertFrom-Json
        
        # Get the container image being used
        $containerImage = $taskDetails.tasks[0].containers[0].image
        Write-Host "Running Image:   $containerImage"
        
        # Check email configuration from running task definition
        $currentTaskDefArn = $taskDetails.tasks[0].taskDefinitionArn
        $taskDefDetails = aws ecs describe-task-definition `
            --task-definition $currentTaskDefArn `
            --output json | ConvertFrom-Json
        
        $envVars = $taskDefDetails.taskDefinition.containerDefinitions[0].environment
        $mailEnabled = ($envVars | Where-Object { $_.name -eq "MAIL_ENABLED" }).value
        $useAwsSdk = ($envVars | Where-Object { $_.name -eq "MAIL_USE_AWS_SDK" }).value
        $mailFrom = ($envVars | Where-Object { $_.name -eq "MAIL_FROM" }).value
        
        if ($mailEnabled -eq "true") {
            $emailMethod = if ($useAwsSdk -eq "true") { "AWS SES SDK (IAM Role)" } else { "SMTP" }
            Write-Host "Email Status:    Enabled ($emailMethod)" -ForegroundColor Green
            if ($mailFrom) {
                Write-Host "  From Address:  $mailFrom" -ForegroundColor Gray
            }
        } else {
            Write-Host "Email Status:    Disabled" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    
    # Try to get ALB endpoint first
    $albDns = $null
    try {
        $ErrorActionPreference = "SilentlyContinue"
        $albDns = aws elbv2 describe-load-balancers `
            --names "$APP_NAME-alb" `
            --query 'LoadBalancers[0].DNSName' `
            --output text 2>&1
        $ErrorActionPreference = "Stop"
        
        # Check if result is valid (not an error message)
        if ($albDns -match "error|exception|not found" -or [string]::IsNullOrWhiteSpace($albDns) -or $albDns -eq "None") {
            $albDns = $null
        }
    }
    catch {
        $albDns = $null
    }
    
    if ($albDns) {
        Write-Success "Application URL:    http://$albDns"
        Write-Info "Health Check:       http://$albDns/actuator/health"
    }
    else {
        # No ALB, try to get task public IP
        if ($taskArns -and $taskArns -ne "None") {
            # Get ENI attachment
            $eniId = $taskDetails.tasks[0].attachments | 
                Where-Object { $_.type -eq "ElasticNetworkInterface" } | 
                Select-Object -First 1 -ExpandProperty details | 
                Where-Object { $_.name -eq "networkInterfaceId" } | 
                Select-Object -First 1 -ExpandProperty value
            
            if ($eniId) {
                try {
                    $ErrorActionPreference = "SilentlyContinue"
                    $publicIp = aws ec2 describe-network-interfaces `
                        --network-interface-ids $eniId `
                        --query 'NetworkInterfaces[0].Association.PublicIp' `
                        --output text 2>&1
                    $ErrorActionPreference = "Stop"
                    
                    # Check if result is valid
                    if ($publicIp -match "error|exception" -or [string]::IsNullOrWhiteSpace($publicIp) -or $publicIp -eq "None") {
                        $publicIp = $null
                    }
                }
                catch {
                    $publicIp = $null
                }
                
                if ($publicIp) {
                    Write-Host "Application URL:    http://${publicIp}:8080"
                    Write-Host "Health Check:       http://${publicIp}:8080/actuator/health"
                }
                else {
                    Write-Warning "No public IP found for task. Check security group and task configuration."
                }
            }
            else {
                Write-Warning "No network interface found for task."
            }
        }
        else {
            Write-Warning "No running tasks found. Cannot determine application URL."
        }
    }
    
    Write-Host "===========================================================================================" -ForegroundColor Cyan
    Write-Host ""
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

# Handle switches
if ($Status) {
    Get-DeploymentStatus
    exit 0
}

Write-Host ""
Write-Host "===================  AWS Deployment  ===================" -ForegroundColor Cyan
Write-Host "Application: $APP_NAME" -ForegroundColor Cyan
Write-Host "Environment: $Environment" -ForegroundColor Cyan
Write-Host "AWS Region:  $AWS_REGION" -ForegroundColor Cyan
Write-Host "AWS Account: $AWS_ACCOUNT_ID" -ForegroundColor Cyan
if ($EnableEmail) {
    $emailMethod = if ($UseAwsSdk) { "AWS SES SDK (IAM Role)" } else { "SMTP" }
    Write-Host "Email:       Enabled ($emailMethod)" -ForegroundColor Green
} else {
    Write-Host "Email:       Disabled" -ForegroundColor Yellow
}
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

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

# Update Jenkins marker if requested
if ($UpdateJenkinsMarker) {
    Update-JenkinsDeploymentMarker
}

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
