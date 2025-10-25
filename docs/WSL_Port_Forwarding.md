# WSL2 Port Forwarding Scripts

These PowerShell scripts manage network access to the Spring Boot application running in Docker on WSL2.

## Problem

By default, WSL2 only forwards ports to `127.0.0.1` (localhost), making the application inaccessible from other devices on your network or via your Windows machine's network IP address (e.g., `192.168.12.179:8080`).

## Solution

These scripts configure Windows to forward port 8080 from all network interfaces to the WSL2 Docker container.

---

## Scripts

### 1. `setup-wsl-port-forward.ps1` (Initial Setup)

**Purpose:** One-time setup to configure port forwarding and firewall rules.

**Usage:**

```powershell
# Run PowerShell as Administrator
.\setup-wsl-port-forward.ps1
```

**What it does:**

-   Gets the current WSL IP address
-   Adds a Windows Firewall rule to allow inbound traffic on port 8080
-   Configures port forwarding from `0.0.0.0:8080` to `<WSL_IP>:8080`
-   Displays all available IP addresses where you can access the application

**When to run:**

-   First time setting up WSL2 Docker
-   After reinstalling WSL or Docker

---

### 2. `update-wsl-port-forward.ps1` (After WSL Restart)

**Purpose:** Updates the port forwarding rule when WSL's IP address changes.

**Usage:**

```powershell
# Run PowerShell as Administrator
.\update-wsl-port-forward.ps1

# Optional: Specify a different port
.\update-wsl-port-forward.ps1 -Port 8081
```

**What it does:**

-   Gets the current WSL IP address
-   Removes the old port forwarding rule
-   Adds a new rule with the updated WSL IP

**When to run:**

-   After WSL restarts
-   After Windows restarts
-   When the application becomes inaccessible via network IP
-   You see "Connection refused" errors from network access

---

### 3. `remove-wsl-port-forward.ps1` (Cleanup)

**Purpose:** Removes all port forwarding and firewall rules.

**Usage:**

```powershell
# Run PowerShell as Administrator
.\remove-wsl-port-forward.ps1

# Optional: Specify a different port
.\remove-wsl-port-forward.ps1 -Port 8081
```

**What it does:**

-   Removes the port forwarding rule for port 8080
-   Removes the Windows Firewall rule
-   Shows remaining port forwarding rules

**When to run:**

-   When you no longer need network access to the application
-   Before uninstalling WSL or Docker
-   To troubleshoot networking issues

---

## Accessing the Application

After running the setup script, you can access the application at:

### From the Windows machine:

-   `http://localhost:8080`
-   `http://192.168.12.179:8080` (or your actual Windows IP)

### From other devices on the same network:

-   `http://192.168.12.179:8080` (or your actual Windows IP)

### To find your Windows IP address:

```powershell
ipconfig
```

Look for the IPv4 address of your active network adapter (Wi-Fi or Ethernet).

---

## Troubleshooting

### Application not accessible via network IP

**Symptoms:**

-   Works on `http://localhost:8080`
-   Doesn't work on `http://192.168.12.179:8080`

**Solution:**

```powershell
# Run PowerShell as Administrator
.\update-wsl-port-forward.ps1
```

### WSL IP address changes frequently

The WSL IP address can change when:

-   WSL restarts
-   Windows restarts
-   Network configuration changes

**Solution:** Run `update-wsl-port-forward.ps1` after restarts.

### Check current port forwarding rules

```powershell
netsh interface portproxy show v4tov4
```

### Check current WSL IP address

```powershell
wsl bash -c "hostname -I"
```

### Check if firewall rule exists

```powershell
Get-NetFirewallRule -DisplayName "WSL2 App Port 8080"
```

### Check if port 8080 is listening

```powershell
netstat -ano | findstr :8080
```

---

## Important Notes

1. **Administrator privileges required:** All scripts must be run in PowerShell as Administrator
2. **WSL must be running:** Make sure WSL and Docker are running before executing scripts
3. **IP address persistence:** WSL IP addresses are not persistent across restarts
4. **Security:** Opening port 8080 to your network means anyone on your network can access the application
5. **Production:** For production deployments, use proper networking, load balancers, and security groups

---

## Alternative: Manual Commands

If you prefer to run commands manually instead of using scripts:

### Get WSL IP

```powershell
wsl bash -c "hostname -I"
```

### Add firewall rule

```powershell
New-NetFirewallRule -DisplayName "WSL2 App Port 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

### Add port forwarding

```powershell
netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=8080 connectaddress=<WSL_IP> connectport=8080
```

### Remove port forwarding

```powershell
netsh interface portproxy delete v4tov4 listenaddress=0.0.0.0 listenport=8080
```

### Remove firewall rule

```powershell
Remove-NetFirewallRule -DisplayName "WSL2 App Port 8080"
```

---

## Related Documentation

-   [WSL2 Docker Guide](docs/WSL2_DOCKER_GUIDE.md) - Complete WSL2 and Docker setup
-   [Quick Start Guide](docs/QUICK_START.md) - Getting started with the application
-   [Developer Guide](docs/DEVELOPER_GUIDE.md) - Development environment setup
