<#
.SYNOPSIS
    Environment Variable Loader - loads environment variables from a .env file.

.DESCRIPTION
    Loads environment variables from a .env file into the current PowerShell session.
    
    Features:
    • Supports comments (#) and empty lines
    • Handles quoted values (VARIABLE="value" or VARIABLE='value')
    • Optional override mode for existing environment variables
    • Error handling for missing files
    • Graceful warnings instead of failures

.PARAMETER envFile
    Optional path to the .env file. Defaults to ".env" in the current directory if not provided.

.PARAMETER overrideExisting
    If true, overwrites existing environment variables. Defaults to false.

.EXAMPLE
    .\set-env-values.ps1
    Loads variables from .env in current directory.

.EXAMPLE
    .\set-env-values.ps1 -envFile "path/to/.env"
    Loads variables from specified .env file.

.EXAMPLE
    .\set-env-values.ps1 -overrideExisting $true
    Loads variables and overrides existing ones.

.NOTES
    Author: Dean Ammons
    Date: December 2025
#>

param(
    # Optional input argument for the .env file path
    # Defaults to ".env" in the current directory if not provided
    [string]$envFile = ".env",

    # Control whether existing environment variables should be overridden
    # Defaults to "false"
    [bool]$overrideExisting = $false
)

# Check if .env file exists
if (-not (Test-Path $envFile)) {
    Write-Warning ".env file not found: $envFile"
    Write-Warning "Skipping environment variable loading. Use command-line parameters instead."
    return
}

# Read each line from the specified .env file
Get-Content -LiteralPath $envFile | ForEach-Object {
    # Trim whitespace from the line
    $line = $_.Trim()

    # Skip empty lines or lines starting with "#"
    if (-not [string]::IsNullOrWhiteSpace($line) -and -not $line.StartsWith("#")) {
        # Split the line into key and value at the first '='
        $parts = $line -split '=', 2

        # Only proceed if we have both key and value
        if ($parts.Count -eq 2) {
            $key = $parts[0].Trim()   # Environment variable name
            $value = $parts[1].Trim() # Environment variable value
            
            # Remove surrounding quotes if present
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or 
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            # Read existing value from the current process environment
            $oldValue = [System.Environment]::GetEnvironmentVariable($key, 'Process')

            if ($null -ne $oldValue) {
                if ($overrideExisting) {
                    if ($oldValue -ne $value) {
                        [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
                        Write-Host "Changed $key from '$oldValue' to '$value'"
                    }
                    # If old and new values are the same, do nothing
                }
                else {
                    if ($oldValue -ne $value) {
                        Write-Host "Did not change $key (existing value: '$oldValue', new value: '$value')"
                    }
                    # If old and new values are the same, do nothing
                }
            }
            else {
                # Variable does not exist yet, set it
                [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
                Write-Host "Exported $key=$value"
            }
        }
    }
}