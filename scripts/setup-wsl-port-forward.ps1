# Setup WSL2 Port Forwarding for Spring Boot Application
# This script configures Windows to forward port 8080 from all network interfaces to the WSL2 container
# Run this script as Administrator after starting the Docker container

Write-Host "Setting up WSL2 port forwarding for Spring Boot application..." -ForegroundColor Cyan

# Get WSL IP address
Write-Host "
Getting WSL IP address..." -ForegroundColor Yellow
$wslIp = (wsl bash -c "hostname -I").Split(" ")[0].Trim()

if ([string]::IsNullOrWhiteSpace($wslIp)) {
    Write-Host "ERROR: Could not get WSL IP address. Make sure WSL is running." -ForegroundColor Red
    exit 1
}

Write-Host "WSL IP Address: $wslIp" -ForegroundColor Green

# Port to forward
$port = 8080

# Step 1: Add Windows Firewall rule
Write-Host "
Step 1: Configuring Windows Firewall..." -ForegroundColor Yellow

# Check if firewall rule already exists
$existingRule = Get-NetFirewallRule -DisplayName "WSL2 App Port 8080" -ErrorAction SilentlyContinue

if ($existingRule) {
    Write-Host "Firewall rule already exists. Skipping..." -ForegroundColor Gray
} else {
    try {
        New-NetFirewallRule -DisplayName "WSL2 App Port 8080" -Direction Inbound -LocalPort $port -Protocol TCP -Action Allow | Out-Null
        Write-Host "Firewall rule added successfully" -ForegroundColor Green
    } catch {
        Write-Host "ERROR: Failed to add firewall rule: $_" -ForegroundColor Red
        Write-Host "Make sure you are running PowerShell as Administrator" -ForegroundColor Yellow
        exit 1
    }
}

# Step 2: Remove old port forwarding rule (if exists)
Write-Host "
Step 2: Removing old port forwarding rules..." -ForegroundColor Yellow
$existingProxy = netsh interface portproxy show v4tov4 | Select-String "0.0.0.0.*$port"

if ($existingProxy) {
    netsh interface portproxy delete v4tov4 listenaddress=0.0.0.0 listenport=$port | Out-Null
    Write-Host "Old port forwarding rule removed" -ForegroundColor Green
} else {
    Write-Host "No existing port forwarding rules found" -ForegroundColor Gray
}

# Step 3: Add new port forwarding rule
Write-Host "
Step 3: Adding port forwarding rule..." -ForegroundColor Yellow
try {
    netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=$port connectaddress=$wslIp connectport=$port | Out-Null
    Write-Host "Port forwarding rule added: 0.0.0.0:$port -> $wslIp`:$port" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to add port forwarding rule: $_" -ForegroundColor Red
    exit 1
}

# Step 4: Verify configuration
Write-Host "
Step 4: Verifying configuration..." -ForegroundColor Yellow
Write-Host "
Active port forwarding rules:" -ForegroundColor Cyan
netsh interface portproxy show v4tov4

# Get Windows IP address
Write-Host "
Your Windows IP addresses:" -ForegroundColor Cyan
Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.*" } | Select-Object IPAddress, InterfaceAlias | Format-Table -AutoSize

Write-Host "
Setup complete!" -ForegroundColor Green
Write-Host "
You can now access your application at:" -ForegroundColor Cyan
$windowsIps = Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.*" } | Select-Object -ExpandProperty IPAddress
foreach ($ip in $windowsIps) {
    Write-Host "  http://$ip`:$port" -ForegroundColor White
}
Write-Host "  http://localhost:$port" -ForegroundColor White

Write-Host "
Note: If WSL restarts you will need to run this script again to update the IP address." -ForegroundColor Yellow
