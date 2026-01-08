#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Keeps WSL running in the background by pinging it periodically.
.DESCRIPTION
    This script is designed to run as a Windows scheduled task to prevent
    WSL from auto-shutting down. It runs a simple command every 15 seconds.
#>

# Log file for debugging
$logFile = "$env:TEMP\keep-wsl-alive.log"

while ($true) {
    try {
        # Run a simple WSL command to keep it alive
        $result = wsl -e true 2>&1
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        # Only log errors to keep log size manageable
        if ($LASTEXITCODE -ne 0) {
            Add-Content -Path $logFile -Value "$timestamp - Error: $result"
        }
    }
    catch {
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        Add-Content -Path $logFile -Value "$timestamp - Exception: $_"
    }
    Start-Sleep -Seconds 15
}
