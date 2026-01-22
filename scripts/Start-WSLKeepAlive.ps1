<#
.SYNOPSIS
    Creates a scheduled task to keep WSL session alive indefinitely.

.DESCRIPTION
    This script creates a Windows Task Scheduler task that starts at user login
    and runs a hidden WSL process (tail -f /dev/null) to prevent WSL from auto-shutting down.
    This ensures Jenkins and other WSL services remain active.

.EXAMPLE
    .\Start-WSLKeepAlive.ps1
    Creates the scheduled task to keep WSL alive.

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

[CmdletBinding()]
param()

$TaskName = "Keep WSL Alive"
$ScriptDir = Split-Path -Parent $PSCommandPath
$WrapperPath = Join-Path $ScriptDir "wsl-keep-alive-wrapper.ps1"

Write-Host "Creating scheduled task: $TaskName" -ForegroundColor Cyan

try {
    # Create PowerShell wrapper script
    $WrapperContent = @'
Start-Process -FilePath "wsl" -ArgumentList "-u", "root", "journalctl", "-u", "jenkins", "-f" -WindowStyle Hidden
'@
    
    Write-Host "Creating PowerShell wrapper: $WrapperPath" -ForegroundColor Gray
    $WrapperContent | Out-File -FilePath $WrapperPath -Encoding UTF8 -Force

    # Check if task already exists
    $ExistingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    if ($ExistingTask) {
        Write-Host "Task already exists. Removing old task..." -ForegroundColor Yellow
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
    }

    # Define the action - run PowerShell wrapper hidden
    $Action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-WindowStyle Hidden -ExecutionPolicy Bypass -File `"$WrapperPath`""

    # Define the trigger - run at user login
    $Trigger = New-ScheduledTaskTrigger -AtLogOn

    # Define settings
    $Settings = New-ScheduledTaskSettingsSet `
        -Hidden `
        -AllowStartIfOnBatteries `
        -DontStopIfGoingOnBatteries `
        -ExecutionTimeLimit (New-TimeSpan -Days 365) `
        -RestartCount 3 `
        -RestartInterval (New-TimeSpan -Minutes 1)

    # Define principal (current user)
    $Principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive

    # Register the task
    Register-ScheduledTask `
        -TaskName $TaskName `
        -Action $Action `
        -Trigger $Trigger `
        -Settings $Settings `
        -Principal $Principal `
        -Description "Keeps WSL session alive to prevent auto-shutdown and maintain Jenkins service" `
        -Force | Out-Null

    Write-Host "✓ Task '$TaskName' created successfully!" -ForegroundColor Green
    Write-Host "✓ PowerShell wrapper created: $WrapperPath" -ForegroundColor Green
    Write-Host ""
    Write-Host "The task will:" -ForegroundColor White
    Write-Host "  • Start automatically at login" -ForegroundColor Gray
    Write-Host "  • Run WSL completely hidden (no window)" -ForegroundColor Gray
    Write-Host "  • Monitor Jenkins logs to keep session alive" -ForegroundColor Gray
    Write-Host "  • Keep WSL and Jenkins running indefinitely" -ForegroundColor Gray
    Write-Host "  • Pure PowerShell solution (no VBScript needed)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "To start it now (without logging out):" -ForegroundColor Yellow
    Write-Host "  Start-ScheduledTask -TaskName '$TaskName'" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To verify it's working:" -ForegroundColor Yellow
    Write-Host "  wsl --list --running" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To stop it:" -ForegroundColor Yellow
    Write-Host "  .\Stop-WSLKeepAlive.ps1" -ForegroundColor Cyan

} catch {
    Write-Host "✗ Error creating task: $_" -ForegroundColor Red
    exit 1
}
