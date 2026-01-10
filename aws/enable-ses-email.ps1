<#
 * Description: Configure AWS SES for TaskActivity - updates the ECS task IAM role and deploys with email notifications enabled
 *
 * Author: Dean Ammons
 * Date: December 2025
 #>

# Configure AWS SES for TaskActivity
# This script updates the ECS task IAM role and deploys with email notifications enabled

param(
    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1",
    
    [Parameter(Mandatory=$false)]
    [string]$TaskRoleName = "ecsTaskRole",
    
    [Parameter(Mandatory=$false)]
    [string]$PolicyName = "TaskActivitySESPolicy"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TaskActivity - Enable AWS SES Email" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Update ECS Task IAM Role with SES permissions
Write-Host "Step 1: Updating ECS Task IAM Role with SES permissions..." -ForegroundColor Yellow

$policyDocument = Get-Content -Path ".\aws\taskactivity-ecs-task-role-policy.json" -Raw

try {
    Write-Host "  - Attaching inline policy to role: $TaskRoleName" -ForegroundColor Gray
    
    aws iam put-role-policy `
        --role-name $TaskRoleName `
        --policy-name $PolicyName `
        --policy-document $policyDocument `
        --region $Region
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✓ IAM role updated successfully" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Failed to update IAM role" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "  ✗ Error updating IAM role: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 2: Verify SES domain status
Write-Host "Step 2: Verifying SES domain status..." -ForegroundColor Yellow

try {
    $sesStatus = aws sesv2 get-email-identity `
        --email-identity taskactivitytracker.com `
        --region $Region `
        --output json 2>$null | ConvertFrom-Json
    
    if ($sesStatus.VerifiedForSendingStatus) {
        Write-Host "  ✓ Domain verified: taskactivitytracker.com" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Domain not verified yet" -ForegroundColor Yellow
        Write-Host "  Please complete DNS verification in AWS SES Console" -ForegroundColor Yellow
    }
    
    # Check sandbox status
    $account = aws sesv2 get-account --region $Region --output json | ConvertFrom-Json
    
    if ($account.ProductionAccessEnabled) {
        Write-Host "  ✓ Production access enabled" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Still in sandbox mode" -ForegroundColor Yellow
        Write-Host "  Can only send to verified email addresses until production access approved" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ⚠ Could not verify SES status (this is OK if you just set it up)" -ForegroundColor Yellow
}

Write-Host ""

# Step 3: Register new task definition
Write-Host "Step 3: Registering new ECS task definition..." -ForegroundColor Yellow

try {
    Write-Host "  - Reading task definition from file..." -ForegroundColor Gray
    
    $taskDefOutput = aws ecs register-task-definition `
        --cli-input-json file://aws/taskactivity-task-definition.json `
        --region $Region `
        --output json
    
    if ($LASTEXITCODE -eq 0) {
        $taskDef = $taskDefOutput | ConvertFrom-Json
        $revision = $taskDef.taskDefinition.revision
        Write-Host "  ✓ Registered task definition revision: $revision" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Failed to register task definition" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "  ✗ Error registering task definition: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 4: Update ECS service
Write-Host "Step 4: Updating ECS service..." -ForegroundColor Yellow

try {
    Write-Host "  - Finding ECS cluster and service..." -ForegroundColor Gray
    
    $services = aws ecs list-services --cluster taskactivity --region $Region --output json | ConvertFrom-Json
    
    if ($services.serviceArns.Count -eq 0) {
        Write-Host "  ⚠ No services found in cluster 'taskactivity'" -ForegroundColor Yellow
        Write-Host "  You may need to create the service first" -ForegroundColor Yellow
    } else {
        $serviceArn = $services.serviceArns[0]
        $serviceName = $serviceArn.Split('/')[-1]
        
        Write-Host "  - Updating service: $serviceName" -ForegroundColor Gray
        
        aws ecs update-service `
            --cluster taskactivity `
            --service $serviceName `
            --task-definition taskactivity:$revision `
            --force-new-deployment `
            --region $Region `
            --output json | Out-Null
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✓ Service updated successfully" -ForegroundColor Green
            Write-Host "  ✓ New deployment initiated" -ForegroundColor Green
        } else {
            Write-Host "  ✗ Failed to update service" -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "  ✗ Error updating service: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Configuration Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Email Configuration:" -ForegroundColor White
Write-Host "  • Method: AWS SES SDK (IAM Role-based)" -ForegroundColor Gray
Write-Host "  • From Address: noreply@taskactivitytracker.com" -ForegroundColor Gray
Write-Host "  • Admin Email: deanammons@gmail.com" -ForegroundColor Gray
Write-Host "  • Region: $Region" -ForegroundColor Gray
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor White
Write-Host "  1. Wait for ECS deployment to complete (2-3 minutes)" -ForegroundColor Gray
Write-Host "  2. Test email by triggering account lockout (5 failed logins)" -ForegroundColor Gray
Write-Host "  3. Check admin email inbox for notification" -ForegroundColor Gray
Write-Host "  4. Monitor logs: aws logs tail /ecs/taskactivity --follow" -ForegroundColor Gray
Write-Host ""
Write-Host "Note: While in sandbox mode, emails can only be sent to deanammons@gmail.com" -ForegroundColor Yellow
Write-Host "      Once production access is approved, any email address will work" -ForegroundColor Yellow
Write-Host ""
