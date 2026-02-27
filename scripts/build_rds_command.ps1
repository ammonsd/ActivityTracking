<#
.SYNOPSIS
    Builds a formatted RDS command string from a SQL file and authentication parameters.

.DESCRIPTION
    Reads a SQL file, processes its contents by removing comments, normalizing whitespace,
    and escaping single quotes, then assembles and returns a command string suitable for
    passing to an RDS CLI or wrapper tool. Returns a single space and writes an error if
    the SQL contains write operations (DML/DDL) but -Admin was not specified. The output
    format defaults to CSV but is suppressed when the SQL contains data-modification or
    DDL statements (INSERT, UPDATE, DELETE, DROP, CREATE, ALTER, TRUNCATE, GRANT, REVOKE).

.PARAMETER SqlFile
    Required. Path to the .sql file to process.

.PARAMETER Password
    Required. The password credential to embed in the returned command string.

.PARAMETER Admin
    Optional switch. When present, prepends -Admin to the credential portion of the
    returned command string.

.PARAMETER DBUser
    Optional. The database username to embed in the returned command string.
    When populated, prepends -DBUser <value> before -Password in the output.
    Not applicable when -Admin is specified.

.PARAMETER OutputFormat
    Optional. Desired output format for the RDS command. Pass "Text" to use plain-text
    output. Any other value (or omitting the parameter) defaults to "csv". Automatically
    set to empty when DML or DDL keywords are detected in the SQL file.

.EXAMPLE
    .\build_rds_command.ps1 -SqlFile .\query.sql -DBUser 'jsmith' -Password "MyPass"
    Returns: -DBUser jsmith -Password MyPass "SELECT ..." csv

.EXAMPLE
    .\build_rds_command.ps1 -SqlFile .\query.sql -Password "MyPass"
    Returns: -Password MyPass "SELECT ..." csv

.EXAMPLE
    .\build_rds_command.ps1 -SqlFile .\update.sql -Password "DBUserPassword" -Admin
    Returns: -Admin -Password DBUserPassword "UPDATE users SET username = '''' WHERE username IS NULL"

.EXAMPLE
    .\build_rds_command.ps1 -SqlFile .\update.sql -DBUser 'jsmith' -Password "MyPass"
    Returns a single space and writes an error because the SQL contains a write operation but -Admin was not specified.

.NOTES
    Author: Dean Ammons
    Date: February 2026
#>
[CmdletBinding()]
param (
    [Parameter(Mandatory = $true)]
    [string]$SqlFile,

    [Parameter(Mandatory = $true)]
    [string]$Password,

    [Parameter(Mandatory = $false)]
    [switch]$Admin,

    [Parameter(Mandatory = $false)]
    [string]$DBUser = "",

    [Parameter(Mandatory = $false)]
    [string]$OutputFormat
)

# Build the password portion of the command string
if ($Admin) {
    $PasswordString = "-Admin -Password $Password"
} elseif (-not [string]::IsNullOrEmpty($DBUser)) {
    $PasswordString = "-DBUser $DBUser -Password $Password"
} else {
    $PasswordString = "-Password $Password"
}

# Read raw SQL file content for DML/DDL keyword detection
$rawSql = Get-Content -Path $SqlFile -Raw

# Determine OutputFormat
# If the SQL file contains DML/DDL keywords, suppress output format (set to empty)
$dmlDdlPattern = '\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|GRANT|REVOKE)\b'
if ($rawSql -match $dmlDdlPattern) {
    $OutputFormatString = ''
} elseif ($OutputFormat -eq 'Text') {
    $OutputFormatString = 'Text'
} else {
    $OutputFormatString = 'csv'
}

# Process SQL file content
# Step 1: Remove block comments /* ... */
$content = [regex]::Replace($rawSql, '/\*.*?\*/', '', [System.Text.RegularExpressions.RegexOptions]::Singleline)

# Step 2: Split into lines and process each line
$processedLines = foreach ($line in ($content -split '\r?\n')) {
    # Remove line comments (-- to end of line)
    $line = [regex]::Replace($line, '--.*$', '')

    # Replace tabs with a single space
    $line = $line -replace '\t', ' '

    # Replace single quote with two single quotes
    $line = $line -replace "'", "''"

    # Trim surrounding whitespace; skip empty lines
    $line = $line.Trim()
    if ($line -ne '') { $line }
}

# Step 3: Concatenate all lines with a single space
$SqlCommands = $processedLines -join ' '

# Step 4: Remove trailing semicolon (and any trailing whitespace)
$SqlCommands = $SqlCommands.TrimEnd().TrimEnd(';').TrimEnd()

# Step 5: Wrap the SQL in double quotes
$SqlCommands = "`"$SqlCommands`""

# Build the final command string
$parts = @($PasswordString, $SqlCommands)
if ($OutputFormatString -ne '') {
    $parts += $OutputFormatString
}

# Error if a non-admin session is attempting a write operation; return a space to signal failure
if (-not $Admin -and $rawSql -match $dmlDdlPattern) {
    $matchedKeyword = $Matches[1]
    Write-Error "Query contains a write operation ($matchedKeyword) but -Admin was not specified. Re-run with -Admin if you intended a write operation."
    return ' '
}

return ($parts -join ' ')
