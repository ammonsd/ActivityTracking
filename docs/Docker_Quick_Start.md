# Quick Start Guide - Docker in WSL2

## First Time Setup - Admin User

When you first start the application, an admin user is automatically created:
- **Username**: `admin`
- **Password**: `Admin123!` (all environments)
- **Note**: You'll be prompted to change the password on first login

For more details, see [Admin User Setup Guide](ADMIN_USER_SETUP.md).

---

## Daily Startup (Most Common)

```powershell
# In PowerShell
wsl -u root
```

```bash
# In WSL2
cd /mnt/c/Users/deana/GitHub/ActivityTracking
./start-docker.sh
```

**Then open browser to:** 
- **Application**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html

---

## Daily Shutdown

```bash
# In WSL2
docker compose --profile host-db down
```

---

## Common Tasks

### View Logs

```bash
docker compose logs -f app
```

### Rebuild After Code Changes

```bash
wsl -u root
./rebuild-and-start.sh 
./rebuild-and-start.sh --no-cache
```

### Check if Running

```bash
docker compose ps
```

### Restart Application

```bash
docker compose --profile host-db restart
```

---

## Troubleshooting

### Database Connection Issues

```bash
./test-db-connection.sh
```

Expected: `✓ Port 5432 is accessible`

### Application Issues

```bash
./check-app-status.sh
```

### If Docker Stopped

```bash
service docker start
```

### If WSL2 Seems Broken

```powershell
# In PowerShell
wsl --shutdown
# Wait 10 seconds
wsl -u root
```

---

## One-Time Setup Checklist

- [x] Installed Docker Engine in WSL2
- [x] Added Windows Firewall rule for port 5432
- [x] Updated pg_hba.conf with WSL2 IP range (172.27.0.0/16)
- [x] Restarted PostgreSQL service
- [x] Updated Dockerfile to use eclipse-temurin base
- [x] Updated docker-compose.yml with WSL2 paths (/mnt/c/Logs)

---

## Key Files

| File                    | What It Does                       |
| ----------------------- | ---------------------------------- |
| `start-docker.sh`         | Main startup script - **USE THIS** |
| `test-db-connection.sh` | Test PostgreSQL connectivity       |
| `check-app-status.sh`   | Diagnose application issues        |
| `WSL2_DOCKER_GUIDE.md`  | Full documentation                 |

---

## Remember

✅ **Always run as root in WSL2:** `wsl -u root`  
✅ **Database password:** N1ghrd01-1948  
✅ **Application URL:** http://localhost:8080  
✅ **Docker is in WSL2, not Docker Desktop**  
✅ **PostgreSQL is on Windows**

---

## Emergency Commands

### Complete Restart

```powershell
# PowerShell
wsl --shutdown
```

```powershell
# Wait 10 seconds, then:
wsl -u root
```

```bash
# WSL2
service docker start
cd /mnt/c/Users/deana/GitHub/ActivityTracking
./start-docker.sh
```

### Check Everything

```bash
# Is Docker running?
docker info

# Is PostgreSQL accessible?
./test-db-connection.sh

# Is app running?
docker compose ps

# What errors?
docker compose logs app | grep -i error
```

---

## Support Checklist

Before asking for help, check:

1. [ ] Docker is running: `docker info`
2. [ ] PostgreSQL is running (Windows): `Get-Service postgresql-x64-17`
3. [ ] Port 5432 accessible: `./test-db-connection.sh`
4. [ ] Firewall rule exists: `Get-NetFirewallRule -DisplayName "PostgreSQL WSL2"`
5. [ ] pg_hba.conf has 172.27.0.0/16
6. [ ] Checked logs: `docker compose logs app`

---

**For full details, see:** `WSL2_DOCKER_GUIDE.md`
