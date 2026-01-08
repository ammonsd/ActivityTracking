#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Creates a Windows scheduled task to keep WSL running.
.DESCRIPTION
    This script creates a scheduled task that runs on user logon and keeps
    WSL alive in the background, preventing auto-shutdown.
#>

# Requires admin privileges
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "This script requires administrator privileges." -ForegroundColor Red
    Write-Host "Please run PowerShell as Administrator and try again." -ForegroundColor Yellow
    exit 1
}

$taskName = "KeepWSLAlive"
$scriptPath = Join-Path $PSScriptRoot "keep-wsl-alive.ps1"

# Check if task already exists
$existingTask = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($existingTask) {
    Write-Host "Task '$taskName' already exists. Removing..." -ForegroundColor Yellow
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
}

# Create the scheduled task with better settings
$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-WindowStyle Hidden -NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`""
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -RestartCount 999 `
    -RestartInterval (New-TimeSpan -Minutes 1) `
    -ExecutionTimeLimit (New-TimeSpan -Days 0) `
    -DontStopOnIdleEnd
    
$principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Highest

Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Description "Keeps WSL running in the background to prevent auto-shutdown" | Out-Null

Write-Host "`nScheduled task '$taskName' created successfully!" -ForegroundColor Green
Write-Host "The task will start automatically on logon." -ForegroundColor Gray
Write-Host "`nTo start the task now:" -ForegroundColor Cyan
Write-Host "  Start-ScheduledTask -TaskName '$taskName'" -ForegroundColor White
Write-Host "`nTo stop the task:" -ForegroundColor Cyan
Write-Host "  Stop-ScheduledTask -TaskName '$taskName'" -ForegroundColor White
Write-Host "`nTo remove the task:" -ForegroundColor Cyan
Write-Host "  Unregister-ScheduledTask -TaskName '$taskName' -Confirm:`$false" -ForegroundColor White
Write-Host ""

# Ask if user wants to start the task now
$response = Read-Host "Start the task now? (Y/N)"
if ($response -eq 'Y' -or $response -eq 'y') {
    Start-ScheduledTask -TaskName $taskName
    Start-Sleep -Seconds 2
    Write-Host "`nTask started! WSL should now stay running." -ForegroundColor Green
}
