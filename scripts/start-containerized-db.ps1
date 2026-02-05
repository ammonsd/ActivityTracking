<#
.SYNOPSIS
    Start Containerized Database Environment.

.DESCRIPTION
    Launches WSL2 as root and runs the containerized-db startup script.
    PowerShell wrapper for the bash script.

.PARAMETER NoCache
    If specified, builds Docker images without cache.

.PARAMETER EnvFile
    Path to environment file (.env or .env.local). Defaults to .env.local if exists, otherwise .env.

.PARAMETER OverrideExisting
    Override existing environment variables. Defaults to $false.

.PARAMETER EncryptionKey
    Encryption key for sensitive data. Passed to set-env-values.ps1 for decryption.

.EXAMPLE
    .\start-containerized-db.ps1
    Start containerized database.

.EXAMPLE
    .\start-containerized-db.ps1 -NoCache
    Start with no Docker cache.

.EXAMPLE
    .\start-containerized-db.ps1 -EncryptionKey "your-key-here"
    Start with encryption key for decrypting sensitive values.

.EXAMPLE
    .\start-containerized-db.ps1 -EncryptionKey "N1ghrd+1968" -OverrideExisting:$true
    Start with encryption key and override existing environment variables.

.NOTES
    Author: Dean Ammons
    Date: December 2025
#>

param(
    [switch]$NoCache,
    
    [string]$EnvFile = "",
    
    [bool]$OverrideExisting = $false,
    
    [string]$EncryptionKey = ""
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Starting Containerized Database Setup" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Determine script directory and workspace root
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent $scriptDir

# Determine which .env file to use
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $envLocal = Join-Path $workspaceRoot ".env.local"
    if (Test-Path $envLocal) {
        $EnvFile = $envLocal
        Write-Host "Using .env.local file" -ForegroundColor Yellow
    } else {
        $envDefault = Join-Path $workspaceRoot ".env"
        $EnvFile = $envDefault
        Write-Host "Using .env file" -ForegroundColor Yellow
    }
} else {
    Write-Host "Using specified env file: $EnvFile" -ForegroundColor Yellow
}
Write-Host ""

# Load environment variables using set-env-values.ps1
$setEnvScript = Join-Path $scriptDir "set-env-values.ps1"
if (Test-Path $setEnvScript) {
    if (-not [string]::IsNullOrWhiteSpace($EncryptionKey)) {
        . $setEnvScript -envFile $EnvFile -overrideExisting $OverrideExisting -EncryptionKey $EncryptionKey
    } else {
        . $setEnvScript -envFile $EnvFile -overrideExisting $OverrideExisting
    }
    Write-Host ""
} else {
    Write-Warning "set-env-values.ps1 not found at: $setEnvScript"
    Write-Warning "Environment variables will not be loaded."
    Write-Host ""
}

# Check if WSL2 is available
try {
    $wslCheck = wsl --list --verbose 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: WSL2 is not available or not running" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERROR: WSL2 is not available" -ForegroundColor Red
    exit 1
}

# Build the command with environment variables
$scriptPath = "/mnt/c/Users/deana/GitHub/ActivityTracking/start-containerized-db.sh"

# Export PowerShell environment variables to WSL
$envVars = @()
if ($env:DB_USERNAME) { $envVars += "DB_USERNAME='$($env:DB_USERNAME)'" }
if ($env:DB_PASSWORD) { $envVars += "DB_PASSWORD='$($env:DB_PASSWORD)'" }
if ($env:JWT_SECRET) { $envVars += "JWT_SECRET='$($env:JWT_SECRET)'" }
if ($env:ENABLE_FILE_LOGGING) { $envVars += "ENABLE_FILE_LOGGING='$($env:ENABLE_FILE_LOGGING)'" }
if ($env:APP_ADMIN_INITIAL_PASSWORD) { $envVars += "APP_ADMIN_INITIAL_PASSWORD='$($env:APP_ADMIN_INITIAL_PASSWORD)'" }

$envPrefix = ""
if ($envVars.Count -gt 0) {
    $envPrefix = "export " + ($envVars -join " && export ") + " && "
}

$command = "${envPrefix}bash $scriptPath"

if ($NoCache) {
    $command = "${envPrefix}bash $scriptPath --no-cache"
    Write-Host "Running with --no-cache (complete rebuild)..." -ForegroundColor Yellow
    Write-Host ""
}

# Execute in WSL2 as root
Write-Host "Launching WSL2 as root..." -ForegroundColor Green
Write-Host ""

wsl -u root -- bash -c $command

# Check exit code
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Green
    Write-Host "Setup completed successfully!" -ForegroundColor Green
    Write-Host "=========================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Red
    Write-Host "Setup failed with exit code: $LASTEXITCODE" -ForegroundColor Red
    Write-Host "=========================================" -ForegroundColor Red
    exit $LASTEXITCODE
}
