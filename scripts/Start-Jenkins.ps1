#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Manages Jenkins service in WSL and displays connection information.

.DESCRIPTION
    This script provides a one-step solution to start Jenkins in WSL, including:
    • Stopping Jenkins if already running
    • Starting Jenkins service
    • Waiting for Jenkins to be ready
    • Displaying WSL IP address and Jenkins URL
    • Showing the most recent Docker image for running the app

.PARAMETER Restart
    If specified, restarts Jenkins even if it's already running.

.PARAMETER Status
    If specified, only displays Jenkins status without starting/stopping.

.EXAMPLE
    .\start-jenkins.ps1
    Starts Jenkins if not running, or restarts if already running.

.EXAMPLE
    .\start-jenkins.ps1 -Status
    Displays current Jenkins status and connection info.

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

[CmdletBinding()]
param(
    [switch]$Restart,
    [switch]$Status
)

# Ensure proper console output for batch execution
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Force console buffer width to prevent wrapping issues
try {
    $host.UI.RawUI.BufferSize = New-Object System.Management.Automation.Host.Size(200, $host.UI.RawUI.BufferSize.Height)
} catch {
    # Ignore if we can't set buffer size (e.g., in ISE or other hosts)
}

Write-Host ""
Write-Host "=== Jenkins Management Script ===" -ForegroundColor Cyan
Write-Host "Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray
Write-Host ""

# Function to get WSL IP address
function Get-WslIpAddress {
    try {
        $wslIp = (wsl hostname -I).Trim() -split '\s+' | Select-Object -First 1
        return $wslIp
    }
    catch {
        Write-Warning "Failed to get WSL IP address: $_"
        return $null
    }
}

# Function to check if Jenkins is running
function Test-JenkinsRunning {
    try {
        $status = wsl -u root systemctl is-active jenkins 2>&1
        return $status -eq "active"
    }
    catch {
        return $false
    }
}

# Function to wait for Jenkins to be ready
function Wait-JenkinsReady {
    param(
        [string]$IpAddress,
        [int]$TimeoutSeconds = 60
    )
    
    Write-Host "Waiting for Jenkins to be ready..." -ForegroundColor Yellow
    $startTime = Get-Date
    $ready = $false
    
    while (((Get-Date) - $startTime).TotalSeconds -lt $TimeoutSeconds) {
        try {
            $response = Invoke-WebRequest -Uri "http://${IpAddress}:8081/login" -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                $ready = $true
                break
            }
        }
        catch {
            # Jenkins not ready yet
        }
        
        Write-Host "." -NoNewline -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
    
    Write-Host ""
    return $ready
}

# Function to get most recent Docker image
function Get-LatestDockerImage {
    try {
        $latestImage = wsl docker images --format "{{.Repository}}:{{.Tag}}`t{{.ID}}`t{{.CreatedAt}}" | Select-String "taskactivity" | Select-Object -First 1
        return $latestImage
    }
    catch {
        return $null
    }
}

# Check current Jenkins status
$isRunning = Test-JenkinsRunning
Write-Host "Current Jenkins Status: " -NoNewline
if ($isRunning) {
    Write-Host "RUNNING" -ForegroundColor Green
} else {
    Write-Host "STOPPED" -ForegroundColor Red
}

# If Status flag is set, just display info and exit
if ($Status) {
    Write-Host "`nJenkins Connection Information:" -ForegroundColor Cyan
    $wslIp = Get-WslIpAddress
    if ($wslIp) {
        Write-Host "  Jenkins URL: http://${wslIp}:8081" -ForegroundColor White
        Write-Host "  WSL IP Address: $wslIp" -ForegroundColor Gray
    }
    
    Write-Host "`nDocker Information:" -ForegroundColor Cyan
    $latestImage = Get-LatestDockerImage
    if ($latestImage) {
        Write-Host "  Most Recent Image: $latestImage" -ForegroundColor White
    }
    
    exit 0
}

# Stop Jenkins if running (for clean restart)
if ($isRunning) {
    Write-Host "`nStopping Jenkins..." -ForegroundColor Yellow
    wsl -u root bash -c "systemctl stop jenkins && sleep 2"
    
    if (Test-JenkinsRunning) {
        Write-Host "Failed to stop Jenkins!" -ForegroundColor Red
        exit 1
    } else {
        Write-Host "Jenkins stopped successfully." -ForegroundColor Green
    }
}

# Start Jenkins
Write-Host "`nStarting Jenkins..." -ForegroundColor Yellow
wsl -u root bash -c "systemctl start jenkins && sleep 3"

# Verify Jenkins started
if (Test-JenkinsRunning) {
    Write-Host "Jenkins started successfully." -ForegroundColor Green
} else {
    Write-Host "Failed to start Jenkins!" -ForegroundColor Red
    Write-Host "`nChecking Jenkins service status:" -ForegroundColor Yellow
    wsl -u root systemctl status jenkins --no-pager -l
    exit 1
}

# Get WSL IP address
$wslIp = Get-WslIpAddress
if (-not $wslIp) {
    Write-Warning "Could not determine WSL IP address"
    $wslIp = "localhost"
}

# Wait for Jenkins to be ready
$jenkinsReady = Wait-JenkinsReady -IpAddress $wslIp

if ($jenkinsReady) {
    Write-Host "`n=== Jenkins is Ready! ===" -ForegroundColor Green
} else {
    Write-Warning "Jenkins started but may still be initializing..."
}

# Display connection information
Write-Host ""
Write-Host "=== Jenkins Connection Information ===" -ForegroundColor Cyan
Write-Host "Jenkins URL:     http://${wslIp}:8081" -ForegroundColor White
Write-Host "WSL IP Address:  $wslIp" -ForegroundColor Gray
Write-Host "App Access URL:  http://${wslIp}:8080/task-activity" -ForegroundColor White
Write-Host "                 (when container is running)" -ForegroundColor Gray

# Display Docker information
Write-Host ""
Write-Host "=== Docker Information ===" -ForegroundColor Cyan
$latestImage = Get-LatestDockerImage
if ($latestImage) {
    # Parse the docker image output more reliably
    $imageString = $latestImage.ToString()
    $parts = $imageString -split '\s+', 3
    
    Write-Host "Most Recent Build Image:" -ForegroundColor White
    if ($parts.Count -ge 1) {
        Write-Host "  Image: $($parts[0])" -ForegroundColor Yellow
    }
    if ($parts.Count -ge 2) {
        Write-Host "  ID:    $($parts[1])" -ForegroundColor Yellow
    }
    if ($parts.Count -ge 3) {
        Write-Host "  Date:  $($parts[2])" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "To run the latest build:" -ForegroundColor White
    Write-Host "  wsl docker run -p 8080:8080 --env-file .env.local -e DATABASE_URL=`"jdbc:postgresql://172.27.80.1:5432/AmmoP1DB`" taskactivity:latest" -ForegroundColor Gray
}

# Display helpful commands
Write-Host ""
Write-Host "=== Helpful Commands ===" -ForegroundColor Cyan
Write-Host "Check Jenkins status:  wsl -u root systemctl status jenkins" -ForegroundColor Gray
Write-Host "View Jenkins logs:     wsl -u root journalctl -u jenkins -f" -ForegroundColor Gray
Write-Host "List Docker images:    wsl docker images | Select-String taskactivity" -ForegroundColor Gray
Write-Host "Stop Jenkins:          wsl -u root systemctl stop jenkins" -ForegroundColor Gray
Write-Host "                       wsl sudo systemctl stop jenkins" -ForegroundColor Gray

Write-Host ""
Write-Host "=== Ready to Build! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Tailing Jenkins logs (Ctrl+C to exit)..." -ForegroundColor Yellow
Write-Host ""

# Tail Jenkins logs
wsl -u root journalctl -u jenkins -f
