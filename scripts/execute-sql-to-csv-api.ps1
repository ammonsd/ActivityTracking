###############################################################################
# Execute SQL and Export to CSV via API
# 
# This script executes SQL from a file by calling the Task Activity API endpoint
# and saves the CSV results to a file.
#
# Prerequisites:
# • Task Activity application running (locally or on AWS)
# • Admin credentials for authentication configured in .env file
#
# Usage:
#   .\execute-sql-to-csv-api.ps1 -SqlFile "query.sql" -OutputCsv "results.csv"
#   .\execute-sql-to-csv-api.ps1 -SqlFile "query.sql" -OutputCsv "results.csv" -EnvFile ".env.local"
#   .\execute-sql-to-csv-api.ps1 -SqlFile "query.sql" -OutputCsv "results.csv" -ApiUrl "http://your-alb-endpoint"
#
# Parameters:
#   -SqlFile      : Path to SQL file to execute (required)
#   -OutputCsv    : Path to output CSV file (required)
#   -EnvFile      : Path to .env file (defaults to .env in workspace root)
#   -ApiUrl       : Base URL of the API (overrides API_URL from .env)
#   -Username     : Admin username (overrides ADMIN_USERNAME from .env)
#   -Password     : Admin password (overrides ADMIN_PASSWORD from .env)
#
###############################################################################

param(
    [Parameter(Mandatory=$true)]
    [string]$SqlFile,
    
    [Parameter(Mandatory=$true)]
    [string]$OutputCsv,
    
    [Parameter(Mandatory=$false)]
    [string]$EnvFile = "",
    
    [Parameter(Mandatory=$false)]
    [string]$ApiUrl = "",
    
    [Parameter(Mandatory=$false)]
    [string]$Username = "",
    
    [Parameter(Mandatory=$false)]
    [string]$Password = ""
)

$ErrorActionPreference = "Stop"

# ========================================
# Load Environment Variables
# ========================================

# Determine workspace root (parent of scripts directory)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Split-Path -Parent $scriptDir

# Set default .env file path if not provided
# Default to .env (standard), fall back to .env.local if .env doesn't exist
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $envDefault = Join-Path $workspaceRoot ".env"
    if (Test-Path $envDefault) {
        $EnvFile = $envDefault
    } else {
        $envLocal = Join-Path $workspaceRoot ".env.local"
        if (Test-Path $envLocal) {
            $EnvFile = $envLocal
        } else {
            $EnvFile = $envDefault  # Use .env even if it doesn't exist (will be caught by set-env-values.ps1)
        }
    }
}

# Load environment variables using set-env-values.ps1
$setEnvScript = Join-Path $scriptDir "set-env-values.ps1"
if (Test-Path $setEnvScript) {
    . $setEnvScript -envFile $EnvFile -overrideExisting $false
}

# Use environment variables as defaults if parameters not provided
if ([string]::IsNullOrWhiteSpace($ApiUrl)) {
    $ApiUrl = $env:API_URL
    if ([string]::IsNullOrWhiteSpace($ApiUrl)) {
        $ApiUrl = "http://localhost:8080"
    }
}

if ([string]::IsNullOrWhiteSpace($Username)) {
    $Username = $env:ADMIN_USERNAME
    if ([string]::IsNullOrWhiteSpace($Username)) {
        $Username = "admin"
    }
}

if ([string]::IsNullOrWhiteSpace($Password)) {
    $Password = $env:ADMIN_PASSWORD
}

# ========================================
# Helper Functions
# ========================================

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# ========================================
# Main
# ========================================

Write-Host ""
Write-Host "=============  Execute SQL via API  =============" -ForegroundColor Cyan
Write-Host "SQL File:    $SqlFile" -ForegroundColor Cyan
Write-Host "Output CSV:  $OutputCsv" -ForegroundColor Cyan
Write-Host "API URL:     $ApiUrl" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host ""

# Validate SQL file exists
if (-not (Test-Path $SqlFile)) {
    Write-Error "SQL file not found: $SqlFile"
    exit 1
}

Write-Success "SQL file found: $SqlFile"

# Read SQL content
Write-Info "Reading SQL file..."
$sqlContent = Get-Content $SqlFile -Raw

if ([string]::IsNullOrWhiteSpace($sqlContent)) {
    Write-Error "SQL file is empty"
    exit 1
}

# Get password if not provided
if ([string]::IsNullOrWhiteSpace($Password)) {
    $securePassword = Read-Host "Enter password for user '$Username'" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword))
}

# Step 1: Authenticate and get token
Write-Info "Authenticating with API..."

$loginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$ApiUrl/api/auth/login" `
        -Method Post `
        -Body $loginBody `
        -ContentType "application/json" `
        -ErrorAction Stop
    
    $token = $loginResponse.accessToken
    
    if ([string]::IsNullOrWhiteSpace($token)) {
        Write-Error "Failed to obtain authentication token"
        exit 1
    }
    
    Write-Success "Authentication successful"
    
} catch {
    Write-Error "Authentication failed: $($_.Exception.Message)"
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
    exit 1
}

# Step 2: Execute query
Write-Info "Executing SQL query..."

# Manually construct JSON to ensure proper string escaping
$escapedSql = $sqlContent -replace '\\', '\\' -replace '"', '\"' -replace "`n", '\n' -replace "`r", '\r' -replace "`t", '\t'
$queryBody = "{`"query`":`"$escapedSql`"}"

Write-Host "Request body: $queryBody" -ForegroundColor DarkGray

try {
    $headers = @{
        Authorization = "Bearer $token"
        'Content-Type' = 'application/json'
    }
    
    $response = Invoke-RestMethod -Uri "$ApiUrl/api/admin/query/execute" `
        -Method Post `
        -Headers $headers `
        -Body $queryBody `
        -ErrorAction Stop
    
    Write-Success "Query executed successfully"
    
} catch {
    Write-Error "Query execution failed: $($_.Exception.Message)"
    if ($_.ErrorDetails) {
        Write-Host "Error details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response body: $responseBody" -ForegroundColor Red
    }
    exit 1
}

# Step 3: Save results to file
Write-Info "Saving results to CSV file..."

try {
    # Create output directory if needed
    $outputDir = Split-Path -Parent $OutputCsv
    if ($outputDir -and -not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }
    
    # Save CSV content
    $response | Out-File -FilePath $OutputCsv -Encoding UTF8 -NoNewline
    
    if (-not (Test-Path $OutputCsv)) {
        Write-Error "Failed to create output file"
        exit 1
    }
    
    $fileInfo = Get-Item $OutputCsv
    $rowCount = (Get-Content $OutputCsv | Measure-Object -Line).Lines
    
    Write-Success "Results saved to: $OutputCsv"
    Write-Info "File size: $($fileInfo.Length) bytes"
    Write-Info "Row count: $rowCount (including header)"
    
} catch {
    Write-Error "Failed to save results: $($_.Exception.Message)"
    exit 1
}

Write-Host ""
Write-Success "Operation completed!"
Write-Host ""
