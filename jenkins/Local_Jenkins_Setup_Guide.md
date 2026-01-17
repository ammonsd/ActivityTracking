# Local Jenkins Setup Guide for TaskActivity

This guide documents the complete process of setting up Jenkins locally in WSL for testing the TaskActivity CI/CD pipeline before deploying to AWS.

**Date:** January 2026  
**Author:** Development Team  
**Purpose:** Test Jenkins pipeline locally before AWS deployment

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Jenkins Installation](#jenkins-installation)
3. [Initial Configuration](#initial-configuration)
4. [Plugin Installation](#plugin-installation)
5. [Tool Configuration](#tool-configuration)
6. [Pipeline Job Setup](#pipeline-job-setup)
7. [Troubleshooting Build Failures](#troubleshooting-build-failures)
8. [Docker Configuration](#docker-configuration)
9. [Verification](#verification)
10. [Running the Jenkins-Built Application](#running-the-jenkins-built-application)
11. [Daily Jenkins Startup](#daily-jenkins-startup)
12. [Keeping WSL Active for Jenkins](#keeping-wsl-active-for-jenkins)
13. [Next Steps](#next-steps)

---

## Prerequisites

### System Requirements

-   **Windows with WSL 2** (Ubuntu recommended)
-   **Java:** Java 11+ (Java 24 used for Jenkins server, Java 21 for builds)
-   **Node.js:** v20.11.1+ (for Angular 19.2 compatibility)
-   **npm:** 10.2.4+
-   **Docker:** WSL Docker installation (no Desktop version needed)
-   **Git:** 2.43.0+
-   **Maven:** 3.9.12+ (auto-installed by Jenkins)

### Verify Prerequisites in WSL

```bash
# Check Java version
java -version

# Check Node.js version
node -v

# Check npm version
npm -v

# Check Git version
git --version

# Check Docker (if installed)
docker --version
```

---

## Jenkins Installation

### Step 1: Install Jenkins in WSL

```bash
# Update package list
sudo apt update

# Install Java if not already installed
sudo apt install openjdk-11-jdk -y

# Add Jenkins repository key
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null

# Add Jenkins repository
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

# Update package list again
sudo apt update

# Install Jenkins
sudo apt install jenkins -y
```

### Step 2: Start Jenkins Service

```bash
# Start Jenkins
sudo systemctl start jenkins

# Enable Jenkins to start on boot
sudo systemctl enable jenkins

# Check Jenkins status
sudo systemctl status jenkins
```

### Step 3: Access Jenkins

1. Open browser and navigate to: `http://localhost:8081`
2. Retrieve initial admin password:

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

3. Copy the password and paste it into the Jenkins UI

---

## Initial Configuration

### Step 1: Setup Wizard

1. **Unlock Jenkins:** Paste the initial admin password
2. **Install Plugins:** Select "Install suggested plugins"
3. **Create Admin User:** Set up your admin credentials
    - Username: `admin` (or your preference)
    - Password: Create a secure password
    - Full name: Your name
    - Email: Your email
4. **Jenkins URL:** Accept default `http://localhost:8081/`

### Step 2: Verify Installation

After setup completes, you should see the Jenkins dashboard.

---

## Plugin Installation

### Required Plugins

Navigate to **Manage Jenkins → Plugins → Available plugins** and install:

1. **Pipeline** (usually pre-installed)
2. **Git Plugin** (usually pre-installed)
3. **Docker Pipeline**
4. **Maven Integration**
5. **Pipeline Utility Steps** ⚠️ **CRITICAL** - Required for `readMavenPom()`

### Installation Steps

```
1. Go to: Manage Jenkins → Plugins → Available plugins
2. Search for each plugin by name
3. Check the box next to the plugin
4. Click "Install"
5. After all plugins are installed, click "Restart Jenkins when installation is complete"
```

### ⚠️ Important: Full Jenkins Restart

After installing plugins, **always perform a full Jenkins restart**:

```bash
# In WSL terminal
sudo systemctl restart jenkins
```

**Wait 1-2 minutes** for Jenkins to fully restart before proceeding.

---

## Tool Configuration

### Configure JDK

1. Navigate to **Manage Jenkins → Tools**
2. Scroll to **JDK installations**
3. Click **Add JDK**
4. Configure:
    - **Name:** `JDK-21`
    - **Install automatically:** ✅ Checked
    - **Version:** Select `OpenJDK 21` (or latest)
5. Click **Save**

### Configure Maven

1. In the same **Tools** page, scroll to **Maven installations**
2. Click **Add Maven**
3. Configure:
    - **Name:** `Maven-3.9`
    - **Install automatically:** ✅ Checked
    - **Version:** Select `3.9.12` (or latest 3.9.x)
4. Click **Save**

---

## Pipeline Job Setup

### Step 1: Create Pipeline Job

1. From Jenkins dashboard, click **New Item**
2. Enter name: `TaskActivity-Pipeline`
3. Select **Pipeline**
4. Click **OK**

### Step 2: Configure Pipeline Parameters

In the job configuration, check **"This project is parameterized"** and add:

#### Parameter 1: ENVIRONMENT

-   **Type:** Choice Parameter
-   **Name:** `ENVIRONMENT`
-   **Choices:** (one per line)
    ```
    dev
    staging
    prod
    ```
-   **Description:** `Target deployment environment`

#### Parameter 2: DEPLOY_ACTION

-   **Type:** Choice Parameter
-   **Name:** `DEPLOY_ACTION`
-   **Choices:**
    ```
    build-only
    deploy
    rollback
    ```
-   **Description:** `Action to perform`

#### Parameter 3: SKIP_TESTS

-   **Type:** Boolean Parameter
-   **Name:** `SKIP_TESTS`
-   **Default:** `false`
-   **Description:** `Skip running unit tests`

#### Parameter 4: NO_CACHE

-   **Type:** Boolean Parameter
-   **Name:** `NO_CACHE`
-   **Default:** `false`
-   **Description:** `Build Docker image without cache`

#### Parameter 5: DEPLOY_INFRASTRUCTURE

-   **Type:** Boolean Parameter
-   **Name:** `DEPLOY_INFRASTRUCTURE`
-   **Default:** `false`
-   **Description:** `Deploy CloudFormation infrastructure`

#### Parameter 6: INFRASTRUCTURE_ACTION

-   **Type:** Choice Parameter
-   **Name:** `INFRASTRUCTURE_ACTION`
-   **Choices:**
    ```
    create
    update
    delete
    ```
-   **Description:** `Infrastructure deployment action`

### Step 3: Configure Pipeline Source

1. Scroll to **Pipeline** section
2. **Definition:** Select `Pipeline script from SCM`
3. **SCM:** Select `Git`
4. **Repository URL:** `https://github.com/ammonsd/ActivityTracking.git`
5. **Branch Specifier:** `*/main`
6. **Script Path:** `Jenkinsfile`
7. Click **Save**

---

## Troubleshooting Build Failures

During initial testing, we encountered and resolved several issues:

### Issue 1: Pipeline Utility Steps Missing (Builds #1-2)

**Error:**

```
No such DSL method 'readMavenPom' found
```

**Solution:**

1. Install **Pipeline Utility Steps** plugin
2. Perform **full Jenkins restart**:
    ```bash
    sudo systemctl restart jenkins
    ```
3. Wait 1-2 minutes for Jenkins to fully restart

### Issue 2: mvnw Permission Denied (Build #3)

**Error:**

```
./mvnw: Permission denied (exit code 126)
```

**Solution:**

```bash
# In PowerShell (Windows side)
cd C:\Users\deana\GitHub\ActivityTracking

# Make mvnw executable
chmod +x mvnw

# Commit via Git
git add mvnw
git update-index --chmod=+x mvnw
git commit -m "fix: make mvnw executable"
git push
```

### Issue 3: MaxPermSize JVM Option Error (Build #4)

**Error:**

```
Unrecognized VM option 'MaxPermSize=256m'
Error: Could not create the Java Virtual Machine.
```

**Cause:** `-XX:MaxPermSize` was deprecated in Java 8 and removed in Java 9+

**Solution:**
Edit [Jenkinsfile](../Jenkinsfile) line 79:

```groovy
// BEFORE (line 79):
environment {
    MAVEN_OPTS = '-Xmx1024m -XX:MaxPermSize=256m'
}

// AFTER:
environment {
    MAVEN_OPTS = '-Xmx1024m'
}
```

### Issue 4: npm Timeout Error (Build #5)

**Error:**

```
npm error code ETIMEDOUT
npm error network request to https://registry.npmjs.org/... failed
```

**Cause:** Transient network connectivity issue

**Solution:**

-   Verified npm connectivity: `npm ping` (613ms response)
-   Re-ran build - issue resolved (network was temporarily unstable)

### Issue 5: Angular Compiler Module Not Found (Build #6)

**Error:**

```
Error: Cannot find module '@angular/compiler/fesm2022/compiler.mjs'
```

**Cause:** Node.js v20.11.0 incompatible with Angular 19.2.15

**Solution:**
Edit [pom.xml](../pom.xml) line 262:

```xml
<!-- BEFORE (line 262): -->
<nodeVersion>v20.11.0</nodeVersion>

<!-- AFTER: -->
<nodeVersion>v20.11.1</nodeVersion>
```

### Issue 6: Cached node_modules Corruption (Build #7)

**Error:**

```
Cannot find module '@angular/compiler/fesm2022/compiler.mjs'
npm showed: "up to date, audited 946 packages in 14s"
```

**Cause:**

-   Jenkins workspace cleanup runs AFTER build failures
-   Old node_modules from Node v20.11.0 remained in workspace
-   npm skipped reinstallation with Node v20.11.1
-   @angular/compiler module structure incompatible between Node versions

**Solution:**
Edit [pom.xml](../pom.xml) lines 227-230, add node_modules cleanup to Maven clean phase:

```xml
<!-- Added to maven-clean-plugin filesets -->
<fileset>
    <directory>frontend/node_modules</directory>
    <followSymlinks>false</followSymlinks>
</fileset>
```

**Result:** Build #8 successfully cleaned node_modules and installed fresh dependencies compatible with Node v20.11.1

---

## Docker Configuration

### Issue: Docker Permission Denied (Build #8)

**Error:**

```
ERROR: permission denied while trying to connect to the Docker daemon socket
```

**Cause:** Jenkins user lacks permission to access Docker socket

**Solution:**

```bash
# In WSL terminal

# Add Jenkins user to docker group
sudo usermod -aG docker jenkins

# Restart Jenkins service to pick up new group membership
sudo systemctl restart jenkins

# Wait 1-2 minutes for Jenkins to restart

# Verify Jenkins can access Docker
sudo -u jenkins docker ps
```

**Expected Output:**

```
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
```

### ✅ Build #9 - Docker Configuration Verified

**Status:** SUCCESS ✅

After applying the Docker permissions fix:

1. Jenkins user successfully added to docker group
2. Jenkins service restarted
3. Build #9 executed successfully
4. Docker image built: `378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity:9`
5. All pipeline stages completed in `build-only` mode

**Complete Pipeline Success:**

-   ✅ Maven Clean Phase
-   ✅ Node.js/npm Installation
-   ✅ Fresh npm Install (950 packages)
-   ✅ Angular Build
-   ✅ Java Compilation
-   ✅ Test Execution (315 tests passed)
-   ✅ JAR Packaging
-   ✅ **Docker Image Build** (NEW - previously failed)
-   ✅ Code Quality Analysis
-   ⏭️ Security Scan (skipped - build-only mode)
-   ⏭️ Push to ECR (skipped - build-only mode)
-   ⏭️ Deploy to ECS (skipped - build-only mode)

**Result:** First complete successful Jenkins build with Docker integration!

---

## Verification

### Build #8 Success Criteria (Before Docker Step)

✅ **Maven Clean Phase:**

-   Deleted `frontend/dist`
-   Deleted `frontend/node_modules` (new cleanup)
-   Deleted `target/classes/static/app`

✅ **Node.js Installation:**

-   Node v20.11.1 installed
-   npm 10.2.4 installed

✅ **npm Install:**

-   Fresh installation: 950 packages installed
-   Duration: 18 seconds

✅ **Angular Build:**

-   Compilation successful
-   Output: `frontend/dist/app`
-   Bundle size: 1.48 MB (with budget warning)

✅ **Maven Compile:**

-   Java compilation successful
-   No compilation errors

✅ **Test Execution:**

-   Tests run: **315**
-   Failures: **0**
-   Errors: **0**
-   Skipped: **8**
-   Duration: 72 seconds

✅ **JAR Packaging:**

-   JAR created: `taskactivity-0.0.1-SNAPSHOT.jar`
-   Spring Boot repackaging successful

---

## Running the Jenkins-Built Application

After a successful Jenkins build, you can run the application using the Docker image that was created.

### Prerequisites

-   Jenkins Build #9 (or later) completed successfully
-   Docker running in WSL
-   PostgreSQL database running on Windows
-   `.env.local` file with required configuration

### Step 1: Locate the Built Docker Image

```bash
# List Docker images to find the latest build
docker images | grep taskactivity

# You should see images like:
# taskactivity    latest    32e3bd23c88d    17 hours ago    716MB
```

### Step 2: Prepare Environment Configuration

Create or update `.env.local` file in your project root:

```bash
cd /mnt/c/Users/deana/GitHub/ActivityTracking

# Create .env.local with required variables
cat > .env.local << 'EOF'
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=AmmoP1DB
DB_USERNAME=postgres
DB_PASSWORD=your_password_here

# JWT Secret - REQUIRED (minimum 32 bytes/256 bits)
# Generate with: openssl rand -base64 32
JWT_SECRET=your_generated_jwt_secret_here

# Other environment variables from your .env file
EOF
```

**Important:** Use `.env.local` (not `.env`) for Docker compatibility.

### Step 3: Find Your Windows Host IP

The Docker container needs to connect to PostgreSQL running on Windows:

```bash
# Get Windows host IP as seen from WSL
ip route show | grep default | awk '{print $3}'

# Example output: 172.27.80.1
```

Save this IP - you'll use it in the next step.

### Step 4: Run the Docker Container

```bash
# Navigate to project directory
cd /mnt/c/Users/deana/GitHub/ActivityTracking

# Run container with environment file and database override
docker run -p 8081:8081 \
  --env-file .env.local \
  -e DATABASE_URL="jdbc:postgresql://172.27.80.1:5432/AmmoP1DB" \
  taskactivity:latest
```

**Key points:**

-   `-p 8081:8081` - Maps container port 8080 to host port 8081 (Jenkins uses 8080)
-   `--env-file .env.local` - Loads all environment variables from file
-   `-e DATABASE_URL=...` - Overrides database URL to use Windows host IP (replace `172.27.80.1` with your actual IP)
-   `taskactivity:latest` - Uses the most recent Jenkins-built image

### Step 5: Verify Application Started

Watch the console output for:

```
Started TaskactivityApplication in X.XXX seconds
```

### Step 6: Access the Application

**Find WSL IP address:**

```bash
# In WSL
hostname -I | awk '{print $1}'

# Example output: 172.27.85.228
```

**Access from Windows browser:**

-   **Main Application:** http://172.27.85.228:8081/task-activity
-   **Angular Frontend:** http://172.27.85.228:8081/app/
-   **Swagger API Docs:** http://172.27.85.228:8081/swagger-ui.html
-   **Health Check:** http://172.27.85.228:8081/actuator/health

**Important:** Replace `172.27.85.228` with your actual WSL IP address.

### Common Issues

#### Issue 1: Port Already in Use

**Error:** `address already in use`

**Solution:**

```bash
# Find what's using port 8081
lsof -i :8081

# Or use a different port
docker run -p 8082:8081 \
  --env-file .env.local \
  -e DATABASE_URL="jdbc:postgresql://172.27.80.1:5432/AmmoP1DB" \
  taskactivity:latest
```

#### Issue 2: Database Connection Failed

**Error:** `Connection refused` or `Unknown host`

**Solution:**

-   Verify PostgreSQL is running on Windows
-   Confirm Windows host IP: `ip route show | grep default | awk '{print $3}'`
-   Update `DATABASE_URL` with correct IP
-   Check PostgreSQL is accepting TCP/IP connections

#### Issue 3: JWT Secret Validation Failed

**Error:** `JWT secret validation` error

**Solution:**

```bash
# Generate a new JWT secret
openssl rand -base64 32

# Update .env.local with the generated value
JWT_SECRET=<output_from_openssl_command>
```

#### Issue 4: Browser Can't Connect

**Error:** `localhost:8081` not working in browser

**Solution:**

-   WSL 2 uses separate network from Windows
-   Must use WSL IP address, not `localhost`
-   Get WSL IP: `hostname -I | awk '{print $1}'`
-   Use: `http://<WSL_IP>:8081/task-activity`

### Stop the Container

```bash
# Press Ctrl+C in the terminal where Docker is running

# Or in another terminal:
docker ps  # Find the container ID
docker stop <container_id>
```

### Quick Start Script

Create `run-jenkins-build.sh` for easy startup:

```bash
#!/bin/bash

cd /mnt/c/Users/deana/GitHub/ActivityTracking

# Get Windows host IP
WIN_HOST_IP=$(ip route show | grep default | awk '{print $3}')
echo "Windows Host IP: $WIN_HOST_IP"

# Get WSL IP for browser access
WSL_IP=$(hostname -I | awk '{print $1}')
echo "WSL IP: $WSL_IP"
echo "Access app at: http://$WSL_IP:8081/task-activity"
echo ""

# Run container
docker run -p 8081:8081 \
  --env-file .env.local \
  -e DATABASE_URL="jdbc:postgresql://$WIN_HOST_IP:5432/AmmoP1DB" \
  taskactivity:latest
```

Make executable and run:

```bash
chmod +x run-jenkins-build.sh
./run-jenkins-build.sh
```

---

## Daily Jenkins Startup

When you restart your PC, Jenkins won't be running automatically. Here's how to start it:

### Quick Start (Recommended)

```bash
# Open WSL terminal and run:
sudo systemctl start jenkins

# Verify it's running:
sudo systemctl status jenkins

# Access Jenkins in browser:
# http://localhost:8081
```

### Automatic Startup (Optional)

Jenkins is already configured to start automatically, but WSL itself doesn't auto-start. To have Jenkins available immediately:

#### Option 1: Auto-start WSL and Jenkins on Windows Boot

Create a Windows Task Scheduler task:

1. Open **Task Scheduler** (search in Windows Start)
2. Click **Create Task** (not Basic Task)
3. **General tab:**
    - Name: `Start WSL Jenkins`
    - Run whether user is logged on or not: ✅ Checked
    - Run with highest privileges: ✅ Checked
4. **Triggers tab:**
    - New → Begin the task: **At startup**
    - Delay task for: **30 seconds** (give Windows time to start)
5. **Actions tab:**
    - New → Action: **Start a program**
    - Program: `wsl.exe`
    - Arguments: `sudo systemctl start jenkins`
6. Click **OK** and enter your Windows password

#### Option 2: Create Desktop Shortcut

1. Right-click Desktop → New → Shortcut
2. Location: `wsl.exe sudo systemctl start jenkins`
3. Name: `Start Jenkins`
4. Double-click shortcut when you need Jenkins

#### Option 3: PowerShell Profile

Add to PowerShell profile for easy command:

```powershell
# Open PowerShell profile
notepad $PROFILE

# Add this function:
function Start-Jenkins {
    wsl sudo systemctl start jenkins
    Write-Host "Jenkins started. Access at http://localhost:8081"
    Write-Host "Waiting 30 seconds for Jenkins to fully start..."
    Start-Sleep -Seconds 30
    Start-Process "http://localhost:8081"
}

# Save and close. Then run:
Start-Jenkins
```

### Daily Workflow

**Morning routine:**

```bash
# 1. Open WSL terminal
wsl

# 2. Start Jenkins
sudo systemctl start jenkins

# 3. Wait 30-60 seconds for Jenkins to fully start

# 4. Open browser to Jenkins
# http://localhost:8081

# 5. Trigger a build or let it run automatically
```

**End of day:**

```bash
# Optional: Stop Jenkins to save resources
sudo systemctl stop jenkins

# Or leave it running (uses minimal resources when idle)
```

### Verify Jenkins is Running

```bash
# Check service status
sudo systemctl status jenkins

# Should show: "active (running)"

# Check if Jenkins port is listening
netstat -tulpn | grep 8080

# Or test with curl
curl -I http://localhost:8081
```

### Troubleshooting Startup Issues

#### Issue: Jenkins fails to start

```bash
# Check logs for errors
sudo journalctl -u jenkins -n 50

# Common fix: Remove stale PID file
sudo rm /var/run/jenkins.pid
sudo systemctl start jenkins
```

#### Issue: Port 8080 already in use

```bash
# Find what's using port 8080
lsof -i :8081

# If it's a zombie Jenkins process:
sudo killall java
sudo systemctl start jenkins
```

#### Issue: WSL won't start

```powershell
# From PowerShell as Administrator:
wsl --shutdown
wsl

# Then start Jenkins again
```

### Jenkins Startup Time

-   **First startup:** 30-60 seconds
-   **Subsequent startups:** 20-30 seconds
-   **Build ready:** Additional 10-20 seconds for tools to initialize

You'll know Jenkins is ready when:

-   `http://localhost:8081` loads the dashboard
-   No "Jenkins is getting ready" message
-   Your jobs are visible in the UI

---

## Code Changes Made

### 1. Jenkinsfile (Line 79)

**File:** `Jenkinsfile`  
**Change:** Removed deprecated MaxPermSize JVM option

```groovy
environment {
    MAVEN_OPTS = '-Xmx1024m'  // Removed: -XX:MaxPermSize=256m
}
```

### 2. pom.xml (Line 262)

**File:** `pom.xml`  
**Change:** Updated Node.js version for Angular 19.2 compatibility

```xml
<nodeVersion>v20.11.1</nodeVersion>  <!-- Was: v20.11.0 -->
```

### 3. pom.xml (Lines 227-230)

**File:** `pom.xml`  
**Change:** Added node_modules cleanup to Maven clean phase

```xml
<fileset>
    <directory>frontend/node_modules</directory>
    <followSymlinks>false</followSymlinks>
</fileset>
```

### 4. Git Permissions

**File:** `mvnw`  
**Change:** Made Maven wrapper executable

```bash
git update-index --chmod=+x mvnw
```

---

## Next Steps

### ✅ Local Jenkins Testing - COMPLETE!

**Status:** All local testing successfully completed with Build #9

-   ✅ Jenkins installation and configuration
-   ✅ Plugin installation (Pipeline Utility Steps, Docker, Maven)
-   ✅ Tool configuration (JDK-21, Maven-3.9)
-   ✅ Pipeline job setup with parameters
-   ✅ Maven build with all tests passing
-   ✅ Docker image build successful
-   ✅ Complete pipeline execution verified

### AWS Deployment (Ready to Begin)

**Local Jenkins testing is now complete - ready for AWS deployment when desired**

1. **Configure AWS Credentials in Jenkins:**

    - Go to Manage Jenkins → Credentials
    - Add AWS Access Key ID and Secret Access Key
    - Configure for ECR and ECS access

2. **Set up ECR Repository Access:**

    - Verify ECR repository exists: `taskactivity`
    - Test ECR login from Jenkins workspace
    - Configure Jenkins AWS CLI access

3. **Configure ECS Deployment:**

    - Verify ECS cluster configuration
    - Review task definition settings
    - Test deployment with `DEPLOY_ACTION=deploy`

4. **Test Full CI/CD Pipeline to AWS:**
    - Start with dev environment
    - Monitor deployment logs
    - Verify application accessibility
    - Test rollback functionality if needed

### Optional Enhancements

1. **Configure SonarQube** for code quality analysis
2. **Set up webhook** for automatic builds on Git push
3. **Configure email notifications** for build failures
4. **Add Slack integration** for build status updates

---

## Common Commands Reference

### Jenkins Service Management

```bash
# Start Jenkins
sudo systemctl start jenkins

# Stop Jenkins
sudo systemctl stop jenkins

# Restart Jenkins (use after plugin installation)
sudo systemctl restart jenkins

# Check Jenkins status
sudo systemctl status jenkins

# View Jenkins logs
sudo journalctl -u jenkins -f
```

### Jenkins Workspace

```bash
# Jenkins home directory
/var/lib/jenkins

# Pipeline workspace
/var/lib/jenkins/workspace/TaskActivity-Pipeline

# Jenkins logs
/var/log/jenkins/jenkins.log
```

### Build Verification Commands

```bash
# Check Git executable permissions
ls -la mvnw

# Verify Node.js version in project
cat pom.xml | grep nodeVersion

# Check Maven clean configuration
cat pom.xml | grep -A 5 "maven-clean-plugin"

# View Jenkins user groups
groups jenkins

# Test Docker access as Jenkins user
sudo -u jenkins docker ps
```

---

## Lessons Learned

### 1. Plugin Installation Requires Full Restart

-   Installing plugins via UI requires **full Jenkins restart**
-   Use `sudo systemctl restart jenkins` instead of web UI restart
-   Wait 1-2 minutes after restart before running builds

### 2. Git File Permissions Must Be Committed

-   Simply running `chmod +x` is not enough
-   Must use `git update-index --chmod=+x` to persist permissions
-   Commit and push changes to repository

### 3. Deprecated JVM Options Cause Hard Failures

-   Modern Java versions (9+) reject deprecated options
-   Remove `-XX:MaxPermSize` for Java 21+
-   Use `-Xmx` for heap size configuration

### 4. Node.js Minor Version Differences Matter

-   Angular 19.2 requires Node v20.11.1+
-   Minor version differences (v20.11.0 vs v20.11.1) cause compilation failures
-   Module structure changes between Node minor versions

### 5. Jenkins Workspace Cleanup Timing

-   `cleanWs()` runs in post-actions AFTER build failures
-   Corrupted dependencies persist between builds
-   Add critical cleanups to build tool (Maven clean phase)

### 6. npm Caches node_modules

-   npm skips reinstallation if node_modules exists
-   Use Maven clean phase filesets for reliable cleanup
-   Ensures fresh dependency installation on every build

### 7. Docker Group Membership Requires Restart

-   Adding user to docker group requires service restart
-   Changes don't take effect until service reloads user context
-   Verify with `sudo -u jenkins docker ps`

---

## Success Metrics

### Build #9 - Complete Success! 🎉

-   ✅ **9 build attempts** total to achieve full success
-   ✅ **7 distinct problems** identified and resolved
-   ✅ **315 tests passing** (100% success rate)
-   ✅ **Complete Maven build** with all tests
-   ✅ **JAR packaging** successful
-   ✅ **Docker image build** successful
-   ✅ **Full pipeline execution** in build-only mode
-   ✅ **Zero build errors** in final successful build

### Build #8 Achievement (Pre-Docker)

-   ✅ **7 build attempts** to resolve Maven/npm issues
-   ✅ **6 distinct problems** identified and fixed
-   ✅ **315 tests passing** (100% success rate)
-   ✅ **72-second test execution** (acceptable performance)
-   ✅ **Zero code defects** in test results
-   ✅ **Complete Maven build** successful
-   ✅ **JAR packaging** successful
-   ❌ Docker permission error (resolved in Build #9)

### Issues Resolved Progression

1. Build #1-2: Pipeline Utility Steps plugin missing
2. Build #3: mvnw permission denied
3. Build #4: MaxPermSize JVM option error
4. Build #5: npm timeout (network issue)
5. Build #6: Node.js version incompatibility
6. Build #7: Cached node_modules corruption
7. Build #8: Maven build success → Docker permission error
8. **Build #9: Complete pipeline success! ✅**

---

## Support and Resources

### Project Documentation

-   [Jenkins Quick Reference](./Jenkins_Quick_Reference.md)
-   [Jenkins Setup Guide](./Jenkins_Setup.md)
-   [Main README](../ReadMe.md)
-   [Developer Guide](../docs/Developer_Guide.md)

### External Resources

-   [Jenkins Documentation](https://www.jenkins.io/doc/)
-   [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
-   [Angular CLI](https://angular.io/cli)
-   [Maven Documentation](https://maven.apache.org/guides/)

---

## Keeping WSL Active for Jenkins

### The Problem

By default, WSL2 automatically shuts down approximately 60 seconds after the last terminal connection closes. This causes Jenkins (running in WSL) to stop, making it inaccessible even though the systemd service shows as \"running\".

**Symptoms:**
- Jenkins accessible after starting `start-jenkins.ps1`
- Closing all WSL/PowerShell terminals causes Jenkins to become inaccessible
- Running `wsl --list --running` shows no distributions running
- Need to restart Jenkins every time you want to access it

### The Solution

Use a Windows scheduled task that periodically pings WSL to keep it alive in the background.

### Setup Instructions

#### Step 1: Run the Setup Script (One-Time)

**Open PowerShell as Administrator** and run:

```powershell
cd c:\\Users\\[YOUR_USERNAME]\\GitHub\\ActivityTracking\\scripts
.\\setup-wsl-keepalive-task.ps1
```

When prompted, choose **Y** to start the task immediately.

**What this does:**
- Creates a Windows scheduled task named \"KeepWSLAlive\"
- Task runs automatically on user login
- Executes `keep-wsl-alive.ps1` in hidden background mode
- Pings WSL every 15 seconds to prevent auto-shutdown
- Auto-restarts if it crashes (up to 999 times)

#### Step 2: Verify Setup

```powershell
# Check if task exists and is running
Get-ScheduledTask -TaskName \"KeepWSLAlive\" | Select-Object TaskName, State

# Should show:
# TaskName       State
# --------       -----
# KeepWSLAlive   Running
```

#### Step 3: Test WSL Stays Alive

1. Start Jenkins with `start-jenkins.ps1`
2. Close all terminal windows
3. Wait 2-3 minutes
4. Open new PowerShell and run:
   ```powershell
   wsl --list --running
   ```
5. Should show: `Ubuntu (Default)` is running
6. Access Jenkins at `http://[WSL-IP]:8081` - should be accessible

### Automatic Startup

The `start-jenkins.ps1` script now automatically ensures the KeepWSLAlive task is running:

```powershell
# Just run your normal Jenkins start script
.\\scripts\\start-jenkins.ps1

# Script will:
# 1. Start Jenkins on port 8081
# 2. Check if KeepWSLAlive task is running
# 3. Start the task if not already running
# 4. Display connection information
```

### Managing the Keep-Alive Task

**Check Status:**
```powershell
Get-ScheduledTask -TaskName \"KeepWSLAlive\" | Select-Object TaskName, State, LastRunTime
```

**Start Task Manually:**
```powershell
Start-ScheduledTask -TaskName \"KeepWSLAlive\"
```

**Stop Task:**
```powershell
Stop-ScheduledTask -TaskName \"KeepWSLAlive\"
```

**Remove Task (if you want to disable this feature):**
```powershell
Unregister-ScheduledTask -TaskName \"KeepWSLAlive\" -Confirm:$false
```

**Recreate Task (if settings changed):**
```powershell
# Run setup script again as Administrator
.\\scripts\\setup-wsl-keepalive-task.ps1
```

### Stopping Everything

When you want to shut down Jenkins and WSL:

```powershell
# Option 1: Stop just the keep-alive task (WSL will shutdown after 60 seconds)
Stop-ScheduledTask -TaskName \"KeepWSLAlive\"

# Option 2: Force immediate WSL shutdown
wsl --shutdown

# Jenkins will stop when WSL shuts down
```

### Troubleshooting

#### WSL Still Shutting Down After Setup

**Check if task is actually running:**
```powershell
Get-ScheduledTask -TaskName \"KeepWSLAlive\" | Format-List *
```

**Check task execution history:**
```powershell
Get-ScheduledTaskInfo -TaskName \"KeepWSLAlive\" | Select-Object LastRunTime, LastTaskResult, NumberOfMissedRuns
```

**View keep-alive log (if errors occur):**
```powershell
# Log file location
Get-Content \"$env:TEMP\\keep-wsl-alive.log\" -Tail 20
```

**Manually test the keep-alive script:**
```powershell
# Run the script directly to see if it works
.\\scripts\\keep-wsl-alive.ps1
# Press Ctrl+C to stop after a few iterations
```

#### Task Shows \"Running\" but WSL Stops

**Solution:** Recreate the task with elevated privileges:

```powershell
# As Administrator
Unregister-ScheduledTask -TaskName \"KeepWSLAlive\" -Confirm:$false
.\\scripts\\setup-wsl-keepalive-task.ps1
```

#### Task Not Starting on Login

**Check trigger configuration:**
```powershell
$task = Get-ScheduledTask -TaskName \"KeepWSLAlive\"
$task.Triggers | Format-List *
```

Should show trigger type: `LOGON`

**Manually start for current session:**
```powershell
Start-ScheduledTask -TaskName \"KeepWSLAlive\"
```

#### High CPU Usage from Keep-Alive Task

The task pings WSL every 15 seconds with minimal CPU impact. If you see high usage:

1. Check if multiple instances are running:
   ```powershell
   Get-Process | Where-Object {$_.Name -like \"*powershell*\" -and $_.CommandLine -like \"*keep-wsl-alive*\"}
   ```

2. Stop and restart the task:
   ```powershell
   Stop-ScheduledTask -TaskName \"KeepWSLAlive\"
   Start-Sleep -Seconds 5
   Start-ScheduledTask -TaskName \"KeepWSLAlive\"
   ```

### Alternative: .wslconfig Method (Not Recommended)

You may find recommendations to configure WSL auto-shutdown in `%USERPROFILE%\\.wslconfig`. However, **this does not prevent WSL shutdown** when no connections are active. The scheduled task approach is more reliable.

### Related Files

- **setup-wsl-keepalive-task.ps1** - One-time setup script (requires Admin)
- **keep-wsl-alive.ps1** - Background script that keeps WSL running
- **start-jenkins.ps1** - Jenkins startup script (auto-starts keep-alive task)

---

## Next Steps

Once Jenkins is successfully building and running locally:

1. **Test Pipeline Stages** - Verify each stage works correctly
2. **Configure AWS Credentials** - Add credentials for ECR/ECS deployment
3. **Test ECR Push** - Verify Docker images can be pushed to ECR
4. **Test ECS Deployment** - Deploy to dev environment from Jenkins
5. **Set Up Monitoring** - Configure build notifications and monitoring
6. **Document Customizations** - Keep track of any environment-specific changes

Refer to the main Jenkinsfile for production deployment configuration.

---

### Troubleshooting

If you encounter issues not covered in this guide:

1. Check Jenkins console output for detailed error messages
2. Review Jenkins system logs: `sudo journalctl -u jenkins -f`
3. Verify all prerequisites are installed and correct versions
4. Ensure all configuration steps were completed in order
5. Check that all code changes were committed and pushed

---

**Document Version:** 1.1  
**Last Updated:** January 6, 2026  
**Status:** Complete - Local Jenkins fully operational with successful Build #9  
**Achievement:** First complete CI/CD pipeline execution with Docker integration ✅
