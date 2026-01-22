<#
.SYNOPSIS
    Stops and removes the WSL keep-alive scheduled task.

.DESCRIPTION
    This script stops the running WSL keep-alive process and removes the scheduled task
    from Task Scheduler. This will allow WSL to auto-shutdown after inactivity.

.EXAMPLE
    .\Stop-WSLKeepAlive.ps1
    Stops the scheduled task and removes it.

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

[CmdletBinding()]
param()

$TaskName = "Keep WSL Alive"
$ScriptDir = Split-Path -Parent $PSCommandPath
$VbsPath = Join-Path $ScriptDir "wsl-keep-alive.vbs"
$WrapperPath = Join-Path $ScriptDir "wsl-keep-alive-wrapper.ps1"

Write-Host "Stopping and removing task: $TaskName" -ForegroundColor Cyan

try {
    # Check if task exists
    $ExistingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    
    if (-not $ExistingTask) {
        Write-Host "Task '$TaskName' not found." -ForegroundColor Yellow
    } else {
        # Stop the task if running
        Stop-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
        Write-Host "✓ Task stopped" -ForegroundColor Green

        # Unregister the task
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
        Write-Host "✓ Task removed" -ForegroundColor Green
    }

    # Remove wrapper files if they exist
    if (Test-Path $VbsPath) {
        Remove-Item $VbsPath -Force
        Write-Host "✓ VBScript wrapper removed" -ForegroundColor Green
    }
    if (Test-Path $WrapperPath) {
        Remove-Item $WrapperPath -Force -ErrorAction SilentlyContinue
        Write-Host "✓ Old PowerShell wrapper removed" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "WSL will now auto-shutdown after several minutes of inactivity." -ForegroundColor Gray
    Write-Host ""
    Write-Host "To manually shutdown WSL now:" -ForegroundColor Yellow
    Write-Host "  wsl --shutdown" -ForegroundColor Cyan

} catch {
    Write-Host "✗ Error removing task: $_" -ForegroundColor Red
    exit 1
}
