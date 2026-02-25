<#
.SYNOPSIS
    Connect to RDS database via ECS container.

.DESCRIPTION
    Connects to the ECS container and then to the RDS database.
    Can run interactive psql session or execute specific queries.
    Requires Session Manager plugin to be installed.

    By default, connects as the read-only user (taskactivity_readonly) which
    is restricted to SELECT queries only. Use -Admin to connect as the postgres
    master user with full read/write access.

.PARAMETER SqlQuery
    SQL query to execute (optional). If omitted, starts interactive session.

.PARAMETER OutputFormat
    Output format: csv, txt, or blank for console output (optional).

.PARAMETER Admin
    Connect as the postgres master user (full read/write access).
    Default is the read-only user (SELECT only).

.PARAMETER Password
    Database password. If omitted, the password is prompted interactively regardless
    of access mode. Useful for scripted/automated scenarios.

.EXAMPLE
    .\connect-to-rds.ps1
    Start interactive psql session as read-only user (prompted for password).

.EXAMPLE
    .\connect-to-rds.ps1 -Password 'mypassword'
    Start interactive psql session as read-only user (password supplied, no prompt).

.EXAMPLE
    .\connect-to-rds.ps1 -Admin
    Start interactive psql session as postgres master user (prompted for password).

.EXAMPLE
    .\connect-to-rds.ps1 -Admin -Password 'mypassword'
    Start interactive psql session as postgres master user (password supplied, no prompt).

.EXAMPLE
    .\connect-to-rds.ps1 'SELECT * FROM public.users'
    Run specific SELECT query as read-only user.

.EXAMPLE
    .\connect-to-rds.ps1 'SELECT * FROM public.users' 'csv'
    Run query and output as CSV.

.EXAMPLE
    .\connect-to-rds.ps1 -Admin 'UPDATE public.users SET enabled=true WHERE id=5'
    Run an update as the admin (postgres) user.

.NOTES
    Author: Dean Ammons
    Date: December 2025
    Modified by: Dean Ammons - February 2026
    Change: Added -Admin switch; default session uses read-only credentials
    Reason: Non-admin users should be restricted to SELECT only at the database level
    Modified by: Dean Ammons - February 2026
    Change: No passwords stored in script; both modes prompt or accept -Password
    Reason: Hardcoded credentials in source are a security risk regardless of access level
    Requires: Session Manager plugin
#>

param(
    [Parameter(Mandatory=$false)]
    [string]$SqlQuery = "",

    [Parameter(Mandatory=$false)]
    [string]$OutputFormat = "",

    [Parameter(Mandatory=$false)]
    [switch]$Admin,

    [Parameter(Mandatory=$false)]
    [string]$Password = ""
)


if ($OutputFormat -ne 'csv' -and $OutputFormat -ne 'txt') {
    $OutputFormat = ''
}

# Resolve password — accept via -Password or prompt interactively
if (-not [string]::IsNullOrEmpty($Password)) {
    $DbPassword = $Password
} else {
    Write-Host ""
    $promptUser = if ($Admin) { 'postgres (admin)' } else { 'taskactivity_readonly' }
    Write-Host "Enter password for $promptUser" -ForegroundColor Yellow
    $SecurePassword = Read-Host -Prompt 'Password' -AsSecureString
    $DbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecurePassword)
    )
    if ([string]::IsNullOrEmpty($DbPassword)) {
        Write-Host 'ERROR: Password cannot be empty.' -ForegroundColor Red
        exit 1
    }
}

# Set credentials based on access mode
if ($Admin) {
    $DbUser      = 'postgres'
    $AccessMode  = 'ADMIN (full read/write)'
    $AccessColor = 'Red'
} else {
    $DbUser      = 'taskactivity_readonly'
    $AccessMode  = 'READ-ONLY (SELECT only)'
    $AccessColor = 'Green'
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Connect to RDS via ECS Container" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Session Manager plugin is installed
Write-Host "Checking Session Manager plugin..." -ForegroundColor Yellow
$sessionManagerPath = Get-Command session-manager-plugin -ErrorAction SilentlyContinue
if (-not $sessionManagerPath) {
    Write-Host ""
    Write-Host "ERROR: Session Manager plugin not found in PATH!" -ForegroundColor Red
    Write-Host ""
    Write-Host "This means one of:" -ForegroundColor Yellow
    Write-Host "  1. Session Manager plugin is not installed" -ForegroundColor White
    Write-Host "  2. You need to close and reopen PowerShell" -ForegroundColor White
    Write-Host ""
    Write-Host "To install:" -ForegroundColor Yellow
    Write-Host "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/windows/SessionManagerPluginSetup.exe" -ForegroundColor White
    Write-Host ""
    Write-Host "After installation or if already installed:" -ForegroundColor Yellow
    Write-Host "  1. Close this PowerShell window" -ForegroundColor White
    Write-Host "  2. Open a NEW PowerShell window" -ForegroundColor White
    Write-Host "  3. Run this script again" -ForegroundColor White
    Write-Host ""
    exit 1
}
Write-Host "Session Manager plugin found at: $($sessionManagerPath.Source)" -ForegroundColor Green

Write-Host ""

# Get the current running task
Write-Host "Finding running ECS task..." -ForegroundColor Yellow
$TASK_ARN = aws ecs list-tasks `
    --cluster taskactivity-cluster `
    --service-name taskactivity-service `
    --desired-status RUNNING `
    --region us-east-1 `
    --query "taskArns[0]" `
    --output text

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrEmpty($TASK_ARN)) {
    Write-Host "ERROR: Could not find running task" -ForegroundColor Red
    exit 1
}

# Extract just the task ID (32 character hex string)
$TASK_ID = $TASK_ARN -replace ".*task/taskactivity-cluster/", ""
Write-Host "Found task ID: $TASK_ID" -ForegroundColor Green
Write-Host "Full ARN: $TASK_ARN" -ForegroundColor Gray
Write-Host ""

# Display connection information
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Database Connection Info:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Host:     taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com" -ForegroundColor White
Write-Host "Port:     5432" -ForegroundColor White
Write-Host "Database: AmmoP1DB" -ForegroundColor White
Write-Host "Username: $DbUser" -ForegroundColor White
Write-Host "Access:   $AccessMode" -ForegroundColor $AccessColor
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Connecting to ECS Container..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if SSM agent is ready
Write-Host "Checking if SSM agent is ready..." -ForegroundColor Yellow
$agentStatus = aws ecs describe-tasks `
    --cluster taskactivity-cluster `
    --tasks $TASK_ID `
    --region us-east-1 `
    --query "tasks[0].containers[0].managedAgents[?name=='ExecuteCommandAgent'].lastStatus" `
    --output text

if ($agentStatus -ne "RUNNING") {
    Write-Host "SSM agent is not ready yet (status: $agentStatus)" -ForegroundColor Yellow
    Write-Host "Waiting 15 seconds for agent to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 15
    
    # Check again
    $agentStatus = aws ecs describe-tasks `
        --cluster taskactivity-cluster `
        --tasks $TASK_ID `
        --region us-east-1 `
        --query "tasks[0].containers[0].managedAgents[?name=='ExecuteCommandAgent'].lastStatus" `
        --output text
}

if ($agentStatus -eq "RUNNING") {
    Write-Host "SSM agent is ready!" -ForegroundColor Green
} else {
    Write-Host "Warning: SSM agent status is: $agentStatus" -ForegroundColor Yellow
    Write-Host "Connection may fail, but attempting anyway..." -ForegroundColor Yellow
}

Write-Host ""

if ([string]::IsNullOrEmpty($SqlQuery)) {
    # No SQL query provided - show interactive instructions
    Write-Host "Once connected, run this command to access the database:" -ForegroundColor Green
    Write-Host ""
    Write-Host "PGPASSWORD='$DbPassword' psql -h taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com -p 5432 -U $DbUser -d AmmoP1DB" -ForegroundColor White
    Write-Host ""
    if (-not $Admin) {
        Write-Host "Note: Connected as read-only user. Only SELECT queries are permitted." -ForegroundColor Yellow
        Write-Host "      Use -Admin switch for write access." -ForegroundColor Yellow
        Write-Host ""
    }
    Write-Host "Common PostgreSQL commands:" -ForegroundColor Yellow
    Write-Host "  \dt          - List all tables" -ForegroundColor Gray
    Write-Host "  \d tablename - Describe a table" -ForegroundColor Gray
    Write-Host "  SELECT * FROM public.users LIMIT 10;  - Query data" -ForegroundColor Gray
    Write-Host "  \q           - Exit psql" -ForegroundColor Gray
} else {
    # SQL query provided - show command with query
    # Escape double quotes for bash
    $escapedQuery = $SqlQuery -replace '"', '\"'

    # Guard: warn if a non-admin session is attempting a write operation
    if (-not $Admin) {
        $writeKeywords = @('INSERT', 'UPDATE', 'DELETE', 'DROP', 'CREATE', 'ALTER', 'TRUNCATE', 'GRANT', 'REVOKE')
        $upperQuery = $SqlQuery.ToUpper().Trim()
        foreach ($kw in $writeKeywords) {
            if ($upperQuery.StartsWith($kw)) {
                Write-Host "" 
                Write-Host "WARNING: Query appears to be a write operation ($kw) but you are connected" -ForegroundColor Red
                Write-Host "         as the read-only user. PostgreSQL will reject it." -ForegroundColor Red
                Write-Host "         Re-run with -Admin if you intended a write operation." -ForegroundColor Red
                Write-Host ""
                break
            }
        }
    }

    Write-Host "Once connected, run this command to execute your query:" -ForegroundColor Green
    Write-Host ""
    Write-Host "Copy/Paste to opt# prompt" -ForegroundColor Yellow
    if ($OutputFormat -eq '') {
        Write-Host "PGPASSWORD='$DbPassword' psql -h taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com -p 5432 -U $DbUser -d AmmoP1DB -c `"$escapedQuery`"" -ForegroundColor Cyan
    }
    if ($OutputFormat -eq 'txt') {
        Write-Host "PGPASSWORD='$DbPassword' psql -h taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com -p 5432 -U $DbUser -d AmmoP1DB -c `"$escapedQuery`" > /tmp/results.txt"'; cat /tmp/results.txt' -ForegroundColor White
    }
    if ($OutputFormat -eq 'csv') {
        Write-Host "PGPASSWORD='$DbPassword' psql -h taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com -p 5432 -U $DbUser -d AmmoP1DB -c `"\copy ($escapedQuery) TO '/tmp/results.csv' CSV HEADER`""'; cat /tmp/results.csv' -ForegroundColor Cyan
    }
    Write-Host ""
}
# Write-Host ""
# Write-Host "Press Enter to connect..." -ForegroundColor Yellow
# Read-Host

# Connect to the container
# Use a startup command that ensures psql is available before dropping into bash
$startupCommand = "command -v psql > /dev/null 2>&1 || (apt-get update -qq && apt-get install -y -qq postgresql-client); exec /bin/bash"

Write-Host "Initiating connection (will auto-install psql if missing)..." -ForegroundColor Yellow
aws ecs execute-command `
    --cluster taskactivity-cluster `
    --task $TASK_ID `
    --container taskactivity `
    --interactive `
    --command "/bin/bash -c '$startupCommand'" `
    --region us-east-1

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "Connection Failed!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Possible reasons:" -ForegroundColor Yellow
    Write-Host "1. SSM agent is still initializing - wait 30 seconds and try again" -ForegroundColor White
    Write-Host "2. Task just restarted - ECS may be starting a new task" -ForegroundColor White
    Write-Host "3. Network or AWS service issue" -ForegroundColor White
    Write-Host ""
    Write-Host "Try running the script again in 30-60 seconds." -ForegroundColor Green
    Write-Host ""
    Write-Host "Press Enter to exit..."
    Read-Host
    exit 1
}
