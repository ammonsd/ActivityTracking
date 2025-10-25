# Setup Automated CloudWatch Log Exports to S3
# This script creates a Windows Scheduled Task to run daily log exports

$ErrorActionPreference = "Stop"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Setup Automated Log Export Schedule" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Configuration
$scriptPath = "c:\Users\deana\GitHub\ActivityTracking\scripts\export-logs-to-s3.ps1"
$taskName = "Export CloudWatch Logs Daily"
$description = "Daily export of ECS application logs from CloudWatch to S3"

# Check if script exists
if (-not (Test-Path $scriptPath)) {
    Write-Host "Error: Export script not found at: $scriptPath" -ForegroundColor Red
    exit 1
}

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Task Name:     $taskName"
Write-Host "  Script:        $scriptPath"
Write-Host "  Schedule:      Daily at 2:00 AM"
Write-Host "  Export Range:  Last 1 day`n"

# Check if task already exists
$existingTask = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($existingTask) {
    Write-Host "Task already exists. Removing old task..." -ForegroundColor Yellow
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
    Write-Host "  Old task removed`n" -ForegroundColor Green
}

# Create the action (what to run)
$action = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-ExecutionPolicy Bypass -NoProfile -WindowStyle Hidden -File `"$scriptPath`" -Days 1"

# Create the trigger (when to run) - Daily at 2:00 AM
$trigger = New-ScheduledTaskTrigger -Daily -At "2:00AM"

# Create the principal (run as current user, highest privileges)
$principal = New-ScheduledTaskPrincipal `
    -UserId "$env:USERDOMAIN\$env:USERNAME" `
    -LogonType S4U `
    -RunLevel Highest

# Create settings
$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable `
    -ExecutionTimeLimit (New-TimeSpan -Hours 1)

# Register the scheduled task
Write-Host "Creating scheduled task..." -ForegroundColor Yellow
try {
    Register-ScheduledTask `
        -TaskName $taskName `
        -Action $action `
        -Trigger $trigger `
        -Principal $principal `
        -Settings $settings `
        -Description $description | Out-Null
    
    Write-Host "  Task created successfully!`n" -ForegroundColor Green
} catch {
    Write-Host "  Failed to create task" -ForegroundColor Red
    Write-Host "  Error: $_`n" -ForegroundColor Yellow
    exit 1
}

# Verify the task was created
$task = Get-ScheduledTask -TaskName $taskName
if ($task) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Setup Complete!" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
    
    Write-Host "Task Details:" -ForegroundColor Yellow
    Write-Host "  Name:          $($task.TaskName)"
    Write-Host "  State:         $($task.State)"
    Write-Host "  Next Run:      $($task.Triggers[0].StartBoundary)`n"
    
    Write-Host "Useful Commands:" -ForegroundColor Yellow
    Write-Host "  # View task status"
    Write-Host "  Get-ScheduledTask -TaskName '$taskName' | Format-List`n" -ForegroundColor Cyan
    
    Write-Host "  # Run task manually now"
    Write-Host "  Start-ScheduledTask -TaskName '$taskName'`n" -ForegroundColor Cyan
    
    Write-Host "  # View task history"
    Write-Host "  Get-ScheduledTask -TaskName '$taskName' | Get-ScheduledTaskInfo`n" -ForegroundColor Cyan
    
    Write-Host "  # Disable task"
    Write-Host "  Disable-ScheduledTask -TaskName '$taskName'`n" -ForegroundColor Cyan
    
    Write-Host "  # Remove task"
    Write-Host "  Unregister-ScheduledTask -TaskName '$taskName' -Confirm:`$false`n" -ForegroundColor Cyan
    
    Write-Host "The task will run automatically every day at 2:00 AM" -ForegroundColor Green
    Write-Host "You can test it now by running: Start-ScheduledTask -TaskName '$taskName'`n" -ForegroundColor Green
} else {
    Write-Host "Error: Task was not created successfully`n" -ForegroundColor Red
    exit 1
}
