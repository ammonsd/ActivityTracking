<#
.SYNOPSIS
    Execute SQL and Export Results via API.

.DESCRIPTION
    Executes SQL from a file by calling the Task Activity API endpoint
    and saves the results to a file. Supports CSV (default) and aligned
    text table output formats.
    
    Prerequisites:
    • Task Activity application running (locally or on AWS)
    • Admin credentials for authentication configured in .env file

.PARAMETER SqlFile
    Path to SQL file to execute (required).

.PARAMETER OutputCsv
    Path to output file (required). Use any extension; content will
    match the selected Format. Alias: -OutputFile

.PARAMETER Format
    Output format for the results. Accepted values: csv (default), text.
    'text' renders an aligned table identical to psql \x off output,
    with a separator line and row-count footer.

.PARAMETER EnvFile
    Path to .env file (defaults to .env in workspace root).

.PARAMETER ApiUrl
    Base URL of the API (overrides API_URL from .env).

.PARAMETER Username
    Admin username (overrides ADMIN_USERNAME from .env).

.PARAMETER Password
    Admin password (overrides ADMIN_PASSWORD from .env).

.PARAMETER OverrideExisting
    Override existing environment variables. Defaults to $false.

.PARAMETER EncryptionKey
    Encryption key for sensitive data. Passed to set-env-values.ps1 for decryption.

.EXAMPLE
    .\execute-sql-to-csv-api.ps1 -SqlFile "query.sql" -OutputCsv "results.csv"
    Execute SQL from query.sql and save results to results.csv.

.EXAMPLE
    .\execute-sql-to-csv-api.ps1 -SqlFile "query.sql" -OutputCsv "results.csv" -EnvFile ".env.local"
    Execute SQL using credentials from .env.local file.

.EXAMPLE
    .\execute-sql-to-csv-api.ps1 -SqlFile "query.sql" -OutputCsv "results.csv" -ApiUrl "http://your-alb-endpoint"
    Execute SQL against custom API endpoint.

.EXAMPLE
    .\execute-sql-to-csv-api.ps1 -SqlFile "query.sql" -OutputCsv "results.csv" -EncryptionKey "????????????" -OverrideExisting:$true
    Execute SQL with encryption key and override existing environment variables.

.EXAMPLE
    .\execute-sql-to-csv-api.ps1 -SqlFile "query.sql" -OutputCsv "results.txt" -Format text
    Execute SQL and save results as an aligned text table (psql-style).

.EXAMPLE
    .\execute-sql-to-csv-api.ps1 -SqlFile "query.sql" -OutputFile "results.txt" -Format text
    Same as above using the -OutputFile alias.

.NOTES
    Author: Dean Ammons
    Date: December 2025

    Modified by: Dean Ammons - February 2026
    Change: Added -Format parameter supporting 'csv' (default) and 'text' output
    Reason: Allow results to be viewed as an aligned text table in addition to CSV
#>
###############################################################################

param(
    [Parameter(Mandatory=$true)]
    [string]$SqlFile,
    
    [Parameter(Mandatory=$true)]
    [Alias("OutputFile")]
    [string]$OutputCsv,
    
    [Parameter(Mandatory=$false)]
    [ValidateSet("csv", "text", IgnoreCase=$true)]
    [string]$Format = "csv",
    
    [Parameter(Mandatory=$false)]
    [string]$EnvFile = "",
    
    [Parameter(Mandatory=$false)]
    [string]$ApiUrl = "",
    
    [Parameter(Mandatory=$false)]
    [string]$Username = "",
    
    [Parameter(Mandatory=$false)]
    [string]$Password = "",
    
    [Parameter(Mandatory=$false)]
    [bool]$OverrideExisting = $false,
    
    [Parameter(Mandatory=$false)]
    [string]$EncryptionKey = ""
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
    if (-not [string]::IsNullOrWhiteSpace($EncryptionKey)) {
        . $setEnvScript -envFile $EnvFile -overrideExisting $OverrideExisting -EncryptionKey $EncryptionKey
    } else {
        . $setEnvScript -envFile $EnvFile -overrideExisting $OverrideExisting
    }
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

function ConvertTo-TextTable {
    <#
    .SYNOPSIS
        Converts CSV text into a psql-style aligned text table.
    #>
    param([string]$CsvContent)

    $trimmed = $CsvContent.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed)) { return "" }

    # Parse CSV into objects
    $data = @($trimmed | ConvertFrom-Csv)
    if ($data.Count -eq 0) { return $trimmed }

    # Extract ordered header names from the first CSV line
    $firstLine = ($trimmed -split "`n")[0].Trim()
    $headers   = $firstLine -split "," | ForEach-Object { $_.Trim().Trim('"') }

    # Calculate column widths: max of header length and widest data value
    $widths = @{}
    foreach ($h in $headers) { $widths[$h] = $h.Length }
    foreach ($row in $data) {
        foreach ($h in $headers) {
            $val = if ($null -ne $row.$h) { "$($row.$h)" } else { "" }
            if ($val.Length -gt $widths[$h]) { $widths[$h] = $val.Length }
        }
    }

    # Header row:  " colName   | colName2 | ..."
    $headerParts = $headers | ForEach-Object { " $($_.PadRight($widths[$_])) " }
    $headerLine  = $headerParts -join "|"

    # Separator:   "-----------+----------+..."
    $sepParts = $headers | ForEach-Object { "-" * ($widths[$_] + 2) }
    $sepLine  = $sepParts -join "+"

    # Data rows
    $dataRows = $data | ForEach-Object {
        $row   = $_
        $parts = $headers | ForEach-Object { " $("$($row.$_)".PadRight($widths[$_])) " }
        $parts -join "|"
    }

    $lines = @($headerLine, $sepLine) + $dataRows + @("($($data.Count) rows)")
    return $lines -join "`n"
}

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
Write-Host "Output File: $OutputCsv" -ForegroundColor Cyan
Write-Host "Format:      $Format" -ForegroundColor Cyan
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

# Step 3: Format and save results
$formatLabel = if ($Format -ieq "text") { "text table" } else { "CSV" }
Write-Info "Formatting results as $formatLabel and saving to file..."

# Convert to text table when requested
$outputContent = if ($Format -ieq "text") {
    ConvertTo-TextTable -CsvContent $response
} else {
    $response
}

try {
    # Create output directory if needed
    $outputDir = Split-Path -Parent $OutputCsv
    if ($outputDir -and -not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }
    
    # Save content (UTF8 without BOM)
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($OutputCsv, $outputContent, $utf8NoBom)
    
    if (-not (Test-Path $OutputCsv)) {
        Write-Error "Failed to create output file"
        exit 1
    }
    
    $fileInfo = Get-Item $OutputCsv
    
    if ($Format -ieq "text") {
        # Count data rows: total lines minus header, separator, and footer
        $totalLines = ($outputContent -split "`n").Count
        $dataRows   = [Math]::Max(0, $totalLines - 3)   # header + separator + footer
        Write-Success "Results saved to: $OutputCsv"
        Write-Info "File size: $($fileInfo.Length) bytes"
        Write-Info "Row count: $dataRows data rows"
    } else {
        $rowCount = (Get-Content $OutputCsv | Measure-Object -Line).Lines
        Write-Success "Results saved to: $OutputCsv"
        Write-Info "File size: $($fileInfo.Length) bytes"
        Write-Info "Row count: $rowCount (including header)"
    }
    
} catch {
    Write-Error "Failed to save results: $($_.Exception.Message)"
    exit 1
}

Write-Host ""
Write-Success "Operation completed!"
Write-Host ""
