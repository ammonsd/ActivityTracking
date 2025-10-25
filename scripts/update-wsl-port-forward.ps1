# Update WSL2 Port Forwarding when WSL IP changes
# This script updates the port forwarding rule with the current WSL IP address
# Run this script as Administrator whenever WSL restarts or the IP changes

param(
    [int]$Port = 8080
)

Write-Host "Updating WSL2 port forwarding for port $Port..." -ForegroundColor Cyan

# Get current WSL IP address
$wslIp = (wsl bash -c "hostname -I").Split(" ")[0].Trim()

if ([string]::IsNullOrWhiteSpace($wslIp)) {
    Write-Host "ERROR: Could not get WSL IP address. Make sure WSL is running." -ForegroundColor Red
    exit 1
}

Write-Host "Current WSL IP Address: $wslIp" -ForegroundColor Green

# Check current port forwarding rule
$currentRule = netsh interface portproxy show v4tov4 | Select-String "0.0.0.0.*$Port"

if ($currentRule) {
    Write-Host "`nCurrent rule: $currentRule" -ForegroundColor Yellow
    
    # Delete old rule
    netsh interface portproxy delete v4tov4 listenaddress=0.0.0.0 listenport=$Port | Out-Null
    Write-Host "Old rule removed" -ForegroundColor Green
}

# Add new rule
netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=$Port connectaddress=$wslIp connectport=$Port | Out-Null
Write-Host "New rule added: 0.0.0.0:$Port -> $wslIp`:$Port" -ForegroundColor Green

# Verify
Write-Host "`nUpdated port forwarding rules:" -ForegroundColor Cyan
netsh interface portproxy show v4tov4

Write-Host "`nUpdate complete!" -ForegroundColor Green
