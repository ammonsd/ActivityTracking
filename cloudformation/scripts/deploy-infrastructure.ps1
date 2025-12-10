###############################################################################
# CloudFormation Infrastructure Deployment Script for Task Activity Application
#
# This script manages CloudFormation stacks for the Task Activity application.
# It handles stack creation, updates, deletion, and validation.
#
# Prerequisites:
# - AWS CLI installed and configured
# - Appropriate AWS IAM permissions for CloudFormation
# - Parameters configured in cloudformation/parameters/*.json
#
# Usage:
#   .\cloudformation\scripts\deploy-infrastructure.ps1 -Environment <env> -Action <action>
#
# Examples:
#   # Create new infrastructure stack
#   .\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action create
#
#   # Update existing infrastructure
#   .\cloudformation\scripts\deploy-infrastructure.ps1 -Environment prod -Action update
#
#   # Preview changes before applying
#   .\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action preview
#
#   # Delete infrastructure (careful!)
#   .\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action delete
#
#   # Check stack status
#   .\cloudformation\scripts\deploy-infrastructure.ps1 -Environment dev -Action status
#
# Author: Dean Ammons
# Date: October 2025
###############################################################################

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('dev', 'staging', 'production')]
    [string]$Environment,
    
    [Parameter(Mandatory=$true)]
    [ValidateSet('create', 'update', 'delete', 'preview', 'status', 'validate')]
    [string]$Action,

    [Parameter(Mandatory=$false)]
    [switch]$SkipConfirmation
)

# Stop on errors
$ErrorActionPreference = "Stop"

# ========================================
# Configuration Variables
# ========================================

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$PROJECT_ROOT = Split-Path -Parent (Split-Path -Parent $SCRIPT_DIR)
$CF_DIR = Join-Path $PROJECT_ROOT "cloudformation"
$TEMPLATE_FILE = Join-Path $CF_DIR "templates\infrastructure.yaml"
$PARAMETERS_FILE = Join-Path $CF_DIR "parameters\$Environment.json"

$STACK_NAME = "taskactivity-$Environment"
$AWS_REGION = "us-east-1"

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
    
    # Check AWS credentials
    try {
        aws sts get-caller-identity | Out-Null
    } catch {
        Write-Error "AWS credentials not configured. Run 'aws configure' first."
        exit 1
    }
    
    # Check template file exists
    if (-not (Test-Path $TEMPLATE_FILE)) {
        Write-Error "Template file not found: $TEMPLATE_FILE"
        exit 1
    }
    
    # Check parameters file exists
    if (-not (Test-Path $PARAMETERS_FILE)) {
        Write-Error "Parameters file not found: $PARAMETERS_FILE"
        Write-Info "Expected location: $PARAMETERS_FILE"
        exit 1
    }
    
    Write-Success "Prerequisites check passed"
}

function Get-StackStatus {
    try {
        $stackInfo = aws cloudformation describe-stacks `
            --stack-name $STACK_NAME `
            --region $AWS_REGION `
            --query 'Stacks[0]' `
            --output json 2>$null | ConvertFrom-Json
        
        return $stackInfo.StackStatus
    } catch {
        return $null
    }
}

function Wait-ForStackOperation {
    param([string]$Operation)
    
    Write-Info "Waiting for stack operation to complete..."
    
    $waitStates = @{
        "create" = "stack-create-complete"
        "update" = "stack-update-complete"
        "delete" = "stack-delete-complete"
    }
    
    $waitState = $waitStates[$Operation]
    
    try {
        aws cloudformation wait $waitState `
            --stack-name $STACK_NAME `
            --region $AWS_REGION
        
        Write-Success "Stack operation completed successfully"
        return $true
    } catch {
        Write-Error "Stack operation failed or timed out"
        Show-StackEvents -Limit 10
        return $false
    }
}

function Show-StackEvents {
    param([int]$Limit = 20)
    
    Write-Info "Recent stack events:"
    Write-Host ""
    
    try {
        $events = aws cloudformation describe-stack-events `
            --stack-name $STACK_NAME `
            --region $AWS_REGION `
            --max-items $Limit `
            --query 'StackEvents[*].[Timestamp,ResourceStatus,ResourceType,LogicalResourceId,ResourceStatusReason]' `
            --output table
        
        Write-Host $events
    } catch {
        Write-Warning "Unable to retrieve stack events"
    }
}

function Show-StackOutputs {
    Write-Info "Stack outputs:"
    Write-Host ""
    
    try {
        $outputs = aws cloudformation describe-stacks `
            --stack-name $STACK_NAME `
            --region $AWS_REGION `
            --query 'Stacks[0].Outputs[*].[OutputKey,OutputValue,Description]' `
            --output table
        
        Write-Host $outputs
    } catch {
        Write-Warning "Unable to retrieve stack outputs"
    }
}

function Validate-Template {
    Write-Info "Validating CloudFormation template..."
    
    try {
        aws cloudformation validate-template `
            --template-body file://$TEMPLATE_FILE `
            --region $AWS_REGION | Out-Null
        
        Write-Success "Template validation passed"
        return $true
    } catch {
        Write-Error "Template validation failed: $_"
        return $false
    }
}

function Create-Stack {
    Write-Info "Creating CloudFormation stack: $STACK_NAME"
    
    # Validate template first
    if (-not (Validate-Template)) {
        exit 1
    }
    
    # Check if stack already exists
    $status = Get-StackStatus
    if ($status) {
        Write-Error "Stack already exists with status: $status"
        Write-Info "Use -Action update to update the stack, or -Action delete to remove it first"
        exit 1
    }
    
    # Confirm action
    if (-not $SkipConfirmation) {
        Write-Warning "This will create a new CloudFormation stack with the following resources:"
        Write-Host "  - VPC with subnets, NAT gateway, and Internet gateway"
        Write-Host "  - RDS PostgreSQL database"
        Write-Host "  - ECS Fargate cluster and service"
        Write-Host "  - Application Load Balancer"
        Write-Host "  - ECR repository"
        Write-Host "  - Secrets Manager secrets"
        Write-Host "  - IAM roles and security groups"
        Write-Host ""
        Write-Warning "This operation may take 10-15 minutes and will incur AWS costs."
        Write-Host ""
        
        $confirm = Read-Host "Do you want to continue? (yes/no)"
        if ($confirm -ne "yes") {
            Write-Info "Operation cancelled"
            exit 0
        }
    }
    
    Write-Info "Creating stack..."
    
    try {
        aws cloudformation create-stack `
            --stack-name $STACK_NAME `
            --template-body file://$TEMPLATE_FILE `
            --parameters file://$PARAMETERS_FILE `
            --capabilities CAPABILITY_NAMED_IAM `
            --region $AWS_REGION `
            --tags Key=Environment,Value=$Environment Key=ManagedBy,Value=CloudFormation
        
        Write-Success "Stack creation initiated"
        
        if (Wait-ForStackOperation -Operation "create") {
            Write-Success "Stack created successfully!"
            Show-StackOutputs
        } else {
            exit 1
        }
    } catch {
        Write-Error "Failed to create stack: $_"
        exit 1
    }
}

function Update-Stack {
    Write-Info "Updating CloudFormation stack: $STACK_NAME"
    
    # Validate template first
    if (-not (Validate-Template)) {
        exit 1
    }
    
    # Check if stack exists
    $status = Get-StackStatus
    if (-not $status) {
        Write-Error "Stack does not exist. Use -Action create to create it first."
        exit 1
    }
    
    if ($status -like "*PROGRESS*") {
        Write-Error "Stack is currently in progress: $status"
        Write-Info "Wait for the current operation to complete before updating"
        exit 1
    }
    
    Write-Info "Creating change set..."
    
    $changeSetName = "changeset-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    
    try {
        aws cloudformation create-change-set `
            --stack-name $STACK_NAME `
            --change-set-name $changeSetName `
            --template-body file://$TEMPLATE_FILE `
            --parameters file://$PARAMETERS_FILE `
            --capabilities CAPABILITY_NAMED_IAM `
            --region $AWS_REGION
        
        Write-Info "Waiting for change set to be created..."
        Start-Sleep -Seconds 5
        
        # Describe changes
        Write-Info "Proposed changes:"
        Write-Host ""
        
        $changes = aws cloudformation describe-change-set `
            --stack-name $STACK_NAME `
            --change-set-name $changeSetName `
            --region $AWS_REGION `
            --query 'Changes[*].[Type,ResourceChange.Action,ResourceChange.LogicalResourceId,ResourceChange.ResourceType,ResourceChange.Replacement]' `
            --output table
        
        Write-Host $changes
        
        # Confirm execution
        if (-not $SkipConfirmation) {
            Write-Host ""
            $confirm = Read-Host "Execute these changes? (yes/no)"
            if ($confirm -ne "yes") {
                Write-Info "Deleting change set..."
                aws cloudformation delete-change-set `
                    --stack-name $STACK_NAME `
                    --change-set-name $changeSetName `
                    --region $AWS_REGION
                Write-Info "Operation cancelled"
                exit 0
            }
        }
        
        Write-Info "Executing change set..."
        aws cloudformation execute-change-set `
            --stack-name $STACK_NAME `
            --change-set-name $changeSetName `
            --region $AWS_REGION
        
        Write-Success "Change set execution initiated"
        
        if (Wait-ForStackOperation -Operation "update") {
            Write-Success "Stack updated successfully!"
            Show-StackOutputs
        } else {
            exit 1
        }
    } catch {
        if ($_ -match "No updates are to be performed") {
            Write-Info "No changes detected - stack is already up to date"
            # Clean up change set
            try {
                aws cloudformation delete-change-set `
                    --stack-name $STACK_NAME `
                    --change-set-name $changeSetName `
                    --region $AWS_REGION 2>$null
            } catch {}
        } else {
            Write-Error "Failed to update stack: $_"
            exit 1
        }
    }
}

function Remove-Stack {
    Write-Info "Deleting CloudFormation stack: $STACK_NAME"
    
    # Check if stack exists
    $status = Get-StackStatus
    if (-not $status) {
        Write-Warning "Stack does not exist"
        exit 0
    }
    
    # Strong warning for production
    if ($Environment -eq "production") {
        Write-Host ""
        Write-Warning "╔═══════════════════════════════════════════════════════════════╗"
        Write-Warning "║  WARNING: You are about to delete PRODUCTION infrastructure  ║"
        Write-Warning "╚═══════════════════════════════════════════════════════════════╝"
        Write-Host ""
        Write-Host "This will permanently delete:"
        Write-Host "  - Production database (final snapshot will be created)"
        Write-Host "  - All network infrastructure"
        Write-Host "  - Load balancers and ECS services"
        Write-Host "  - ECR repository (images will be preserved)"
        Write-Host ""
    }
    
    # Confirm deletion
    if (-not $SkipConfirmation) {
        Write-Warning "This will delete ALL infrastructure resources for $Environment environment"
        Write-Host ""
        Write-Host "Type 'delete $Environment' to confirm: " -NoNewline
        $confirm = Read-Host
        
        if ($confirm -ne "delete $Environment") {
            Write-Info "Operation cancelled"
            exit 0
        }
    }
    
    Write-Info "Deleting stack..."
    
    try {
        aws cloudformation delete-stack `
            --stack-name $STACK_NAME `
            --region $AWS_REGION
        
        Write-Success "Stack deletion initiated"
        
        if (Wait-ForStackOperation -Operation "delete") {
            Write-Success "Stack deleted successfully!"
        } else {
            exit 1
        }
    } catch {
        Write-Error "Failed to delete stack: $_"
        exit 1
    }
}

function Show-StackStatus {
    Write-Info "Checking stack status: $STACK_NAME"
    
    $status = Get-StackStatus
    
    if (-not $status) {
        Write-Warning "Stack does not exist"
        exit 0
    }
    
    Write-Host ""
    Write-Host "Stack Status: " -NoNewline
    
    if ($status -like "*COMPLETE*" -and $status -notlike "*ROLLBACK*") {
        Write-Host $status -ForegroundColor Green
    } elseif ($status -like "*FAILED*" -or $status -like "*ROLLBACK*") {
        Write-Host $status -ForegroundColor Red
    } else {
        Write-Host $status -ForegroundColor Yellow
    }
    
    Write-Host ""
    Show-StackEvents -Limit 10
    Write-Host ""
    
    if ($status -like "*COMPLETE*" -and $status -notlike "*ROLLBACK*") {
        Show-StackOutputs
    }
}

function Preview-Changes {
    Write-Info "Previewing infrastructure changes for: $STACK_NAME"
    
    # Validate template first
    if (-not (Validate-Template)) {
        exit 1
    }
    
    # Check if stack exists
    $status = Get-StackStatus
    
    if (-not $status) {
        Write-Info "Stack does not exist. This would create a new stack with:"
        Write-Host ""
        Write-Host "Template: $TEMPLATE_FILE"
        Write-Host "Parameters: $PARAMETERS_FILE"
        Write-Host ""
        Write-Info "Run with -Action create to create the stack"
        exit 0
    }
    
    Write-Info "Creating change set for preview..."
    
    $changeSetName = "preview-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    
    try {
        aws cloudformation create-change-set `
            --stack-name $STACK_NAME `
            --change-set-name $changeSetName `
            --template-body file://$TEMPLATE_FILE `
            --parameters file://$PARAMETERS_FILE `
            --capabilities CAPABILITY_NAMED_IAM `
            --region $AWS_REGION
        
        Write-Info "Waiting for change set to be created..."
        Start-Sleep -Seconds 5
        
        Write-Info "Proposed changes:"
        Write-Host ""
        
        $changes = aws cloudformation describe-change-set `
            --stack-name $STACK_NAME `
            --change-set-name $changeSetName `
            --region $AWS_REGION `
            --query 'Changes[*].[Type,ResourceChange.Action,ResourceChange.LogicalResourceId,ResourceChange.ResourceType,ResourceChange.Replacement]' `
            --output table
        
        Write-Host $changes
        
        # Clean up change set
        Write-Info "Cleaning up preview change set..."
        aws cloudformation delete-change-set `
            --stack-name $STACK_NAME `
            --change-set-name $changeSetName `
            --region $AWS_REGION
        
        Write-Info "Preview complete. No changes were applied."
    } catch {
        if ($_ -match "No updates are to be performed") {
            Write-Info "No changes detected - stack is already up to date"
            # Clean up change set
            try {
                aws cloudformation delete-change-set `
                    --stack-name $STACK_NAME `
                    --change-set-name $changeSetName `
                    --region $AWS_REGION 2>$null
            } catch {}
        } else {
            Write-Error "Failed to create preview: $_"
            exit 1
        }
    }
}

# ========================================
# Main Execution
# ========================================

Write-Host "========================================================================"
Write-Host " Task Activity - CloudFormation Infrastructure Deployment"
Write-Host "========================================================================"
Write-Host ""
Write-Host "Environment: $Environment"
Write-Host "Action: $Action"
Write-Host "Stack Name: $STACK_NAME"
Write-Host "Region: $AWS_REGION"
Write-Host ""

# Check prerequisites
Test-Prerequisites

# Execute action
switch ($Action) {
    "create" {
        Create-Stack
    }
    "update" {
        Update-Stack
    }
    "delete" {
        Remove-Stack
    }
    "status" {
        Show-StackStatus
    }
    "preview" {
        Preview-Changes
    }
    "validate" {
        if (Validate-Template) {
            Write-Success "Template is valid"
        } else {
            exit 1
        }
    }
}

Write-Host ""
Write-Host "========================================================================"
Write-Success "Operation completed"
Write-Host "========================================================================"
