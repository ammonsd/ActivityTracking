<#
.SYNOPSIS
    Opens an SSM port-forwarding tunnel so pgAdmin 4 (or any PostgreSQL client)
    can connect to the private AWS RDS instance from your local machine.

.DESCRIPTION
    The RDS instance is not publicly accessible; it only accepts connections from
    within the VPC (i.e., from the ECS tasks). This script uses AWS Systems Manager
    port-forwarding via a running ECS task as the relay, tunneling RDS port 5432 to
    a local port so that pgAdmin can reach it as if it were localhost.

    Once the tunnel is running, open pgAdmin 4 and create a new server with:
        Host:     127.0.0.1
        Port:     15432  (or whatever -LocalPort you specified)
        Database: AmmoP1DB
        Username: taskactivity_readonly   (read-only access)
        Password: <taskactivity_readonly password>

    Use -AdminUser to connect pgAdmin as the postgres master user instead.

    Press Ctrl+C in this window to close the tunnel when you are done.

.PARAMETER LocalPort
    The local TCP port that pgAdmin will connect to.
    Defaults to 15432 to avoid conflicts with any local PostgreSQL on 5432.

.PARAMETER AdminUser
    When specified, displays credentials for the postgres master user instead of
    the default taskactivity_readonly user in the on-screen instructions.
    (The tunnel itself is the same regardless; only the displayed username changes.)

.EXAMPLE
    .\Start-RdsTunnel.ps1
    Open tunnel on localhost:15432 — connect pgAdmin as taskactivity_readonly.

.EXAMPLE
    .\Start-RdsTunnel.ps1 -LocalPort 5433
    Open tunnel on localhost:5433 (useful if 15432 is already in use).

.EXAMPLE
    .\Start-RdsTunnel.ps1 -AdminUser
    Open tunnel and display postgres master-user connection details.

.NOTES
    Author: Dean Ammons
    Date: February 2026

    Prerequisites:
        - AWS CLI v2 installed and configured
        - Session Manager plugin installed
          https://s3.amazonaws.com/session-manager-downloads/plugin/latest/windows/SessionManagerPluginSetup.exe
        - Sufficient IAM permissions (ssm:StartSession, ecs:ListTasks, ecs:DescribeTasks)
#>

param(
    [Parameter(Mandatory = $false)]
    [ValidateRange(1024, 65535)]
    [int]$LocalPort = 15432,

    [Parameter(Mandatory = $false)]
    [switch]$AdminUser
)

# ── Constants ─────────────────────────────────────────────────────────────────
$CLUSTER      = 'taskactivity-cluster'
$SERVICE      = 'taskactivity-service'
$CONTAINER    = 'taskactivity'
$REGION       = 'us-east-1'
$RDS_HOST     = 'taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com'
$RDS_PORT     = '5432'
$DB_NAME      = 'AmmoP1DB'
$READONLY_USER = 'taskactivity_readonly'
$ADMIN_USER   = 'postgres'

# ── Header ────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RDS Tunnel for pgAdmin 4" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── Verify Session Manager plugin ─────────────────────────────────────────────
Write-Host "Checking Session Manager plugin..." -ForegroundColor Yellow
$ssmPlugin = Get-Command session-manager-plugin -ErrorAction SilentlyContinue
if (-not $ssmPlugin) {
    Write-Host ""
    Write-Host "ERROR: Session Manager plugin not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Install it from:" -ForegroundColor Yellow
    Write-Host "  https://s3.amazonaws.com/session-manager-downloads/plugin/latest/windows/SessionManagerPluginSetup.exe" -ForegroundColor White
    Write-Host ""
    Write-Host "After installation, close and reopen PowerShell, then run this script again." -ForegroundColor Yellow
    exit 1
}
Write-Host "Session Manager plugin found: $($ssmPlugin.Source)" -ForegroundColor Green
Write-Host ""

# ── Check if local port is already in use ─────────────────────────────────────
$portInUse = Get-NetTCPConnection -LocalPort $LocalPort -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Host "WARNING: Local port $LocalPort is already in use." -ForegroundColor Yellow
    Write-Host "         Use -LocalPort to choose a different port, e.g.:" -ForegroundColor Yellow
    Write-Host "         .\Start-RdsTunnel.ps1 -LocalPort 15433" -ForegroundColor White
    Write-Host ""
    exit 1
}

# ── Find running ECS task ──────────────────────────────────────────────────────
Write-Host "Finding running ECS task in cluster '$CLUSTER'..." -ForegroundColor Yellow

$TASK_ARN = aws ecs list-tasks `
    --cluster $CLUSTER `
    --service-name $SERVICE `
    --desired-status RUNNING `
    --region $REGION `
    --query "taskArns[0]" `
    --output text

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrEmpty($TASK_ARN) -or $TASK_ARN -eq 'None') {
    Write-Host "ERROR: No running ECS tasks found in service '$SERVICE'." -ForegroundColor Red
    Write-Host "       Ensure the ECS service is running before starting the tunnel." -ForegroundColor Yellow
    exit 1
}

$TASK_ID = $TASK_ARN -replace ".*task/$CLUSTER/", ""
Write-Host "Found task: $TASK_ID" -ForegroundColor Green
Write-Host ""

# ── Get the container runtime ID (required for SSM port-forward target) ────────
Write-Host "Resolving container runtime ID..." -ForegroundColor Yellow

$RUNTIME_ID = aws ecs describe-tasks `
    --cluster $CLUSTER `
    --tasks $TASK_ID `
    --region $REGION `
    --query "tasks[0].containers[?name=='$CONTAINER'].runtimeId" `
    --output text

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrEmpty($RUNTIME_ID) -or $RUNTIME_ID -eq 'None') {
    Write-Host "ERROR: Could not retrieve runtime ID for container '$CONTAINER'." -ForegroundColor Red
    Write-Host "       The container may still be starting. Wait 30 seconds and try again." -ForegroundColor Yellow
    exit 1
}

# Use the full runtime ID exactly as reported by ECS — truncating it causes TargetNotConnected errors
$SSM_TARGET = "ecs:${CLUSTER}_${TASK_ID}_${RUNTIME_ID}"

Write-Host "Container runtime ID: $RUNTIME_ID" -ForegroundColor Green
Write-Host "SSM target:           $SSM_TARGET" -ForegroundColor Gray
Write-Host ""

# ── Display pgAdmin connection instructions ────────────────────────────────────
$displayUser = if ($AdminUser) { $ADMIN_USER } else { $READONLY_USER }
$accessNote  = if ($AdminUser) { "Full read/write access" } else { "Read-only (SELECT only)" }

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  pgAdmin 4 Connection Settings" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  In pgAdmin 4 -> Servers -> Register / Server:" -ForegroundColor White
Write-Host ""
Write-Host "  [General tab]" -ForegroundColor Yellow
Write-Host "    Name:      AWS RDS TaskActivity (via tunnel)" -ForegroundColor White
Write-Host ""
Write-Host "  [Connection tab]" -ForegroundColor Yellow
Write-Host "    Host:      127.0.0.1" -ForegroundColor Green
Write-Host "    Port:      $LocalPort" -ForegroundColor Green
Write-Host "    Database:  $DB_NAME" -ForegroundColor Green
Write-Host "    Username:  $displayUser" -ForegroundColor Green
Write-Host "    Password:  <enter when prompted by pgAdmin>" -ForegroundColor Green
Write-Host ""
Write-Host "  [SSL tab]" -ForegroundColor Yellow
Write-Host "    SSL mode:  Require" -ForegroundColor Green
Write-Host ""
Write-Host "  Access level: $accessNote" -ForegroundColor $(if ($AdminUser) { 'Red' } else { 'Cyan' })
Write-Host ""
Write-Host "  Keep this window open while using pgAdmin." -ForegroundColor Yellow
Write-Host "  Press Ctrl+C here to close the tunnel when done." -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Starting tunnel: localhost:$LocalPort  ->  ${RDS_HOST}:${RDS_PORT}" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── Start SSM port-forwarding session ─────────────────────────────────────────
# AWS-StartPortForwardingSessionToRemoteHost tunnels a remote host:port
# through the SSM-connected instance (the ECS container acts as the relay).
$parametersJson = "{`"host`":[`"$RDS_HOST`"],`"portNumber`":[`"$RDS_PORT`"],`"localPortNumber`":[`"$LocalPort`"]}"

aws ssm start-session `
    --target $SSM_TARGET `
    --document-name AWS-StartPortForwardingSessionToRemoteHost `
    --parameters $parametersJson `
    --region $REGION

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  Tunnel Failed to Start" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Common causes:" -ForegroundColor Yellow
    Write-Host "  1. SSM agent in the container is still initializing." -ForegroundColor White
    Write-Host "     Wait 30 seconds and run again." -ForegroundColor White
    Write-Host "  2. IAM role is missing ssm:StartSession permission." -ForegroundColor White
    Write-Host "  3. The ECS task was restarted and the task ID changed." -ForegroundColor White
    Write-Host "     Run again to pick up the new task." -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "Tunnel closed." -ForegroundColor Yellow
