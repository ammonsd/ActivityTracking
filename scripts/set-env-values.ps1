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

.PARAMETER EncryptionKey
    Optional encryption key for decrypting field values. If provided along with ENCRYPTED_FIELDS
    in the .env file, those fields will be decrypted using Encrypt-Decrypt.ps1.

.EXAMPLE
    .\set-env-values.ps1
    Loads variables from .env in current directory.

.EXAMPLE
    .\set-env-values.ps1 -envFile "path/to/.env"
    Loads variables from specified .env file.

.EXAMPLE
    .\set-env-values.ps1 -overrideExisting $true
    Loads variables and overrides existing ones.

.EXAMPLE
    .\set-env-values.ps1 -envFile ".env.local" -EncryptionKey "MySecretKey123"
    Loads variables and decrypts fields listed in ENCRYPTED_FIELDS.

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
    [bool]$overrideExisting = $false,

    # Optional encryption key for decrypting encrypted field values
    [string]$EncryptionKey = ""
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

# Decrypt encrypted fields if both ENCRYPTED_FIELDS and EncryptionKey are provided
$encryptedFields = [System.Environment]::GetEnvironmentVariable('ENCRYPTED_FIELDS', 'Process')
if (-not [string]::IsNullOrWhiteSpace($encryptedFields) -and -not [string]::IsNullOrWhiteSpace($EncryptionKey)) {
    Write-Host ""
    Write-Host "Decrypting encrypted fields..." -ForegroundColor Cyan
    
    # Get the path to Encrypt-Decrypt.ps1
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $encryptDecryptScript = Join-Path $scriptDir "Encrypt-Decrypt.ps1"
    
    if (-not (Test-Path $encryptDecryptScript)) {
        Write-Warning "Encrypt-Decrypt.ps1 not found at: $encryptDecryptScript"
        Write-Warning "Skipping field decryption."
    } else {
        # Split ENCRYPTED_FIELDS by space to get individual field names
        $fieldsToDecrypt = $encryptedFields -split '\s+' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
        
        foreach ($fieldName in $fieldsToDecrypt) {
            $encryptedValue = [System.Environment]::GetEnvironmentVariable($fieldName, 'Process')
            
            if (-not [string]::IsNullOrWhiteSpace($encryptedValue)) {
                try {
                    # Call Encrypt-Decrypt.ps1 to decrypt the value
                    $decryptedValue = & $encryptDecryptScript -InputString $encryptedValue -Action Decrypt -Key $EncryptionKey
                    
                    if (-not [string]::IsNullOrWhiteSpace($decryptedValue)) {
                        # Update the environment variable with decrypted value
                        [System.Environment]::SetEnvironmentVariable($fieldName, $decryptedValue, 'Process')
                        Write-Host "  Decrypted $fieldName" -ForegroundColor Green
                    } else {
                        Write-Warning "  Failed to decrypt $fieldName - keeping encrypted value"
                    }
                } catch {
                    Write-Warning "  Error decrypting $fieldName - keeping encrypted value: $($_.Exception.Message)"
                }
            } else {
                Write-Warning "  Field $fieldName is empty or not found - skipping"
            }
        }
    }
}