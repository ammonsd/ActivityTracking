<#
.SYNOPSIS
    Encrypts or decrypts a string using AES encryption.

.DESCRIPTION
    This script provides functionality to encrypt or decrypt strings using AES-256 encryption.
    You can use a password-based key or let the script generate one for you.

.PARAMETER InputString
    The string to encrypt or decrypt.

.PARAMETER Action
    Specify 'Encrypt' or 'Decrypt' operation.

.PARAMETER Key
    The encryption key (password). Must be provided for both encryption and decryption.
    The same key must be used for both operations.

.PARAMETER UseBase64
    When encrypting, output as Base64 string (default: true).
    When decrypting, expect Base64 input (default: true).

.EXAMPLE
    .\Encrypt-Decrypt.ps1 -InputString "Hello World" -Action Encrypt -Key "MySecretPassword123"
    Encrypts the string "Hello World"

.EXAMPLE
    .\Encrypt-Decrypt.ps1 -InputString "encrypted_string_here" -Action Decrypt -Key "MySecretPassword123"
    Decrypts the encrypted string

.EXAMPLE
    # Encrypt with pipeline
    "Sensitive data" | .\Encrypt-Decrypt.ps1 -Action Encrypt -Key "MyPassword"

.EXAMPLE
    # From CMD batch file - capture encrypted value into environment variable
    for /f "delims=" %%i in ('powershell -File "Encrypt-Decrypt.ps1" -InputString "MySensitiveData" -Action Encrypt -Key "MyPassword"') do set ENCRYPTED=%%i
    
.EXAMPLE
    # From CMD batch file - decrypt and use the value
    for /f "delims=" %%i in ('powershell -File "Encrypt-Decrypt.ps1" -InputString "%ENCRYPTED%" -Action Decrypt -Key "MyPassword"') do set DECRYPTED=%%i

.NOTES
    Keep your encryption key safe! Without it, you cannot decrypt the data.
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, ValueFromPipeline = $true, Position = 0)]
    [string]$InputString,

    [Parameter(Mandatory = $true, Position = 1)]
    [ValidateSet('Encrypt', 'Decrypt')]
    [string]$Action,

    [Parameter(Mandatory = $true, Position = 2)]
    [string]$Key,

    [Parameter(Mandatory = $false)]
    [switch]$UseBase64 = $true
)

function Get-AESKey {
    param([string]$Password)
    
    # Create a 256-bit key from the password
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $keyBytes = $sha256.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($Password))
    $sha256.Dispose()
    
    return $keyBytes
}

function Encrypt-String {
    param(
        [string]$PlainText,
        [byte[]]$KeyBytes
    )
    
    try {
        # Create AES encryption object
        $aes = [System.Security.Cryptography.Aes]::Create()
        $aes.KeySize = 256
        $aes.Key = $KeyBytes
        $aes.Mode = [System.Security.Cryptography.CipherMode]::CBC
        $aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
        $aes.GenerateIV()
        
        # Create encryptor
        $encryptor = $aes.CreateEncryptor()
        
        # Convert plain text to bytes
        $plainBytes = [System.Text.Encoding]::UTF8.GetBytes($PlainText)
        
        # Encrypt the data
        $encryptedBytes = $encryptor.TransformFinalBlock($plainBytes, 0, $plainBytes.Length)
        
        # Combine IV and encrypted data
        $result = $aes.IV + $encryptedBytes
        
        # Clean up
        $encryptor.Dispose()
        $aes.Dispose()
        
        if ($UseBase64) {
            return [Convert]::ToBase64String($result)
        } else {
            return [BitConverter]::ToString($result).Replace('-', '')
        }
    }
    catch {
        return $null
    }
}

function Decrypt-String {
    param(
        [string]$EncryptedText,
        [byte[]]$KeyBytes
    )
    
    try {
        # Convert encrypted text to bytes
        if ($UseBase64) {
            $encryptedBytes = [Convert]::FromBase64String($EncryptedText)
        } else {
            $encryptedBytes = [byte[]]::new($EncryptedText.Length / 2)
            for ($i = 0; $i -lt $encryptedBytes.Length; $i++) {
                $encryptedBytes[$i] = [Convert]::ToByte($EncryptedText.Substring($i * 2, 2), 16)
            }
        }
        
        # Create AES decryption object
        $aes = [System.Security.Cryptography.Aes]::Create()
        $aes.KeySize = 256
        $aes.Key = $KeyBytes
        $aes.Mode = [System.Security.Cryptography.CipherMode]::CBC
        $aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
        
        # Extract IV (first 16 bytes) and encrypted data
        $iv = $encryptedBytes[0..15]
        $encryptedData = $encryptedBytes[16..($encryptedBytes.Length - 1)]
        $aes.IV = $iv
        
        # Create decryptor
        $decryptor = $aes.CreateDecryptor()
        
        # Decrypt the data
        $decryptedBytes = $decryptor.TransformFinalBlock($encryptedData, 0, $encryptedData.Length)
        
        # Convert to string
        $result = [System.Text.Encoding]::UTF8.GetString($decryptedBytes)
        
        # Clean up
        $decryptor.Dispose()
        $aes.Dispose()
        
        return $result
    }
    catch {
        return $null
    }
}

# Main execution
try {
    # Generate key from password
    $keyBytes = Get-AESKey -Password $Key
    
    if ($Action -eq 'Encrypt') {
        $result = Encrypt-String -PlainText $InputString -KeyBytes $keyBytes
        if ($result) {
            Write-Output $result
        }
    }
    elseif ($Action -eq 'Decrypt') {
        $result = Decrypt-String -EncryptedText $InputString -KeyBytes $keyBytes
        if ($result) {
            Write-Output $result
        }
    }
}
finally {
    # Clear sensitive data from memory
    if ($keyBytes) {
        [Array]::Clear($keyBytes, 0, $keyBytes.Length)
    }
}
