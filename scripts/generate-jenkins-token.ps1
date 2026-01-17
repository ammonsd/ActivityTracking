#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Generates a JWT token for Jenkins API authentication.

.DESCRIPTION
    This script generates a JWT token that Jenkins can use to authenticate
    webhook requests to the production application. The token is valid for 30 days.
    
    Prerequisites:
    - JWT_SECRET environment variable must be set (same as production)
    - Java must be available on the system PATH

.PARAMETER JwtSecret
    Optional JWT secret. If not provided, reads from JWT_SECRET environment variable.

.EXAMPLE
    .\generate-jenkins-token.ps1
    Generates token using JWT_SECRET from environment variable.

.EXAMPLE
    .\generate-jenkins-token.ps1 -JwtSecret "your-secret-key"
    Generates token using provided secret.

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

[CmdletBinding()]
param(
    [Parameter()]
    [string]$JwtSecret
)

Write-Host ""
Write-Host "=== Jenkins JWT Token Generator ===" -ForegroundColor Cyan
Write-Host "Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray
Write-Host ""

# Check for JWT_SECRET
if (-not $JwtSecret) {
    $JwtSecret = $env:JWT_SECRET
    
    if (-not $JwtSecret) {
        Write-Host "ERROR: JWT_SECRET not found!" -ForegroundColor Red
        Write-Host ""
        Write-Host "You need to set the JWT_SECRET environment variable." -ForegroundColor Yellow
        Write-Host "This must be the SAME secret used in your ECS production environment." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "To set it temporarily (current session):" -ForegroundColor White
        Write-Host '  $env:JWT_SECRET = "your-secret-key-here"' -ForegroundColor Gray
        Write-Host ""
        Write-Host "Or pass it as a parameter:" -ForegroundColor White
        Write-Host '  .\generate-jenkins-token.ps1 -JwtSecret "your-secret-key-here"' -ForegroundColor Gray
        Write-Host ""
        Write-Host "To get your production JWT_SECRET from AWS:" -ForegroundColor White
        Write-Host "  aws ecs describe-task-definition --task-definition taskactivity --region us-east-1 --query 'taskDefinition.containerDefinitions[0].environment[?name==``JWT_SECRET``].value' --output text" -ForegroundColor Gray
        Write-Host ""
        exit 1
    }
}

# Validate JWT_SECRET length (minimum 32 bytes)
if ($JwtSecret.Length -lt 32) {
    Write-Host "ERROR: JWT_SECRET is too short!" -ForegroundColor Red
    Write-Host "Minimum length: 32 characters (256 bits)" -ForegroundColor Yellow
    Write-Host "Current length: $($JwtSecret.Length) characters" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host "Using JWT_SECRET: $($JwtSecret.Substring(0, 10))... (truncated for security)" -ForegroundColor Gray
Write-Host ""

# Check if Java is available
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "Java: $javaVersion" -ForegroundColor Gray
} catch {
    Write-Host "ERROR: Java not found!" -ForegroundColor Red
    Write-Host "Please ensure Java is installed and available in PATH." -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# Compile the Java class
Write-Host ""
Write-Host "Compiling token generator..." -ForegroundColor Yellow

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaFile = Join-Path $scriptDir "GenerateJenkinsToken.java"
$projectRoot = Split-Path -Parent $scriptDir
$targetDir = Join-Path $projectRoot "target"
$classesDir = Join-Path $targetDir "classes"

if (-not (Test-Path $targetDir)) {
    Write-Host "ERROR: target directory not found!" -ForegroundColor Red
    Write-Host "Please run 'mvnw clean compile' first to build the project." -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# Set classpath - include all JARs recursively
$classpath = @(
    $classesDir
    $scriptDir
)

# Add all JAR files from target directory
Get-ChildItem -Path $targetDir -Filter "*.jar" -Recurse | ForEach-Object {
    $classpath += $_.FullName
}

$classpathString = $classpath -join ";"

# Compile
$compileResult = javac -cp $classpathString $javaFile 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Compilation failed!" -ForegroundColor Red
    Write-Host $compileResult
    Write-Host ""
    exit 1
}

Write-Host "✓ Compiled successfully" -ForegroundColor Green
Write-Host ""

# Run the generator
Write-Host "Generating JWT token..." -ForegroundColor Yellow
Write-Host ""

$env:JWT_SECRET = $JwtSecret
java -cp $classpathString GenerateJenkinsToken

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Token generation failed!" -ForegroundColor Red
    Write-Host ""
    exit 1
}

# Cleanup
Write-Host ""
Write-Host "Cleaning up..." -ForegroundColor Gray
Remove-Item (Join-Path $scriptDir "GenerateJenkinsToken.class") -ErrorAction SilentlyContinue

Write-Host "✓ Done!" -ForegroundColor Green
Write-Host ""
