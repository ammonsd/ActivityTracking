#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Generates JWT token using PowerShell and .NET

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

[CmdletBinding()]
param()

Write-Host ""
Write-Host "=== Jenkins JWT Token Generator (PowerShell) ===" -ForegroundColor Cyan
Write-Host ""

# Get JWT_SECRET
if (-not $env:JWT_SECRET) {
    if (Test-Path ".env.local") {
        $secretLine = Select-String -Path .env.local -Pattern "^JWT_SECRET=" | Select-Object -First 1
        if ($secretLine) {
            $env:JWT_SECRET = ($secretLine.Line -split "=", 2)[1]
            Write-Host "✓ JWT_SECRET loaded from .env.local" -ForegroundColor Green
        }
    }
}

if (-not $env:JWT_SECRET) {
    Write-Host "ERROR: JWT_SECRET not set!" -ForegroundColor Red
    exit 1
}

Write-Host "Using JWT_SECRET: $($env:JWT_SECRET.Substring(0, 10))..." -ForegroundColor Gray
Write-Host ""

# Create JWT manually
$secret = $env:JWT_SECRET
$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$exp = [DateTimeOffset]::UtcNow.AddDays(30).ToUnixTimeSeconds()
$jti = [guid]::NewGuid().ToString()

# Header
$header = @{
    alg = "HS256"
    typ = "JWT"
} | ConvertTo-Json -Compress

# Payload
$payload = @{
    sub = "jenkins-service"
    iat = $now
    exp = $exp
    jti = $jti
    permissions = "JENKINS:NOTIFY"
    type = "SERVICE_ACCOUNT"
} | ConvertTo-Json -Compress

# Base64Url encode
function ConvertTo-Base64Url {
    param([string]$text)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
    $base64 = [Convert]::ToBase64String($bytes)
    return $base64.TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$headerEncoded = ConvertTo-Base64Url $header
$payloadEncoded = ConvertTo-Base64Url $payload
$message = "$headerEncoded.$payloadEncoded"

# Sign with HMACSHA256
$hmac = [System.Security.Cryptography.HMACSHA256]::new([System.Text.Encoding]::UTF8.GetBytes($secret))
$signatureBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($message))
$signatureEncoded = [Convert]::ToBase64String($signatureBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')

$token = "$message.$signatureEncoded"

# Display
Write-Host "=" * 70
Write-Host "Jenkins API Token"
Write-Host "=" * 70
Write-Host ""
Write-Host "Token Details:" -ForegroundColor Cyan
Write-Host "  Subject: jenkins-service"
Write-Host "  Permission: JENKINS:NOTIFY"
Write-Host "  Expires: $([DateTimeOffset]::FromUnixTimeSeconds($exp).LocalDateTime)"
Write-Host "  Lifetime: 30 days"
Write-Host ""
Write-Host "JWT Token (copy entire line below):" -ForegroundColor Cyan
Write-Host ("-" * 70)
Write-Host $token -ForegroundColor Yellow
Write-Host ("-" * 70)
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Copy the token above (entire yellow line)"
Write-Host "  2. Go to Jenkins: http://172.27.85.228:8081"
Write-Host "  3. Manage Jenkins → Credentials → System → Global"
Write-Host "  4. Find 'jenkins-api-token' → Click → Update"
Write-Host "  5. Paste token in Secret field → Save"
Write-Host "  6. Test with a Jenkins build"
Write-Host ""
Write-Host "Token expires on: $([DateTimeOffset]::FromUnixTimeSeconds($exp).LocalDateTime)" -ForegroundColor Yellow
Write-Host "Set reminder to regenerate: $([DateTimeOffset]::FromUnixTimeSeconds($exp).AddDays(-1).ToString('MMMM d, yyyy'))" -ForegroundColor Yellow
Write-Host "=" * 70
Write-Host ""
