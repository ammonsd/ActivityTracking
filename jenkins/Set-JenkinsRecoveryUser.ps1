#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Creates or removes a temporary Jenkins recovery user for local login recovery.

.DESCRIPTION
    This script manages a Groovy init hook in the local WSL Jenkins installation to
    create a temporary administrative recovery user on Jenkins startup. Use the default
    Create action when you need to regain access to Jenkins, then run the Cleanup action
    after you have reset the password for your normal Jenkins user.

.PARAMETER Action
    The recovery action to perform. Use Create to add the temporary recovery hook and
    restart Jenkins. Use Cleanup to remove the hook and restart Jenkins.

.PARAMETER RecoveryUsername
    The temporary Jenkins username to create when Action is Create.

.PARAMETER RecoveryPassword
    The temporary Jenkins password to assign when Action is Create.

.PARAMETER HookPath
    The Groovy init hook path inside the WSL Jenkins home directory.

.PARAMETER TimeoutSeconds
    The number of seconds to wait for Jenkins to respond after a restart.

.EXAMPLE
    .\Set-JenkinsRecoveryUser.ps1
    Creates the default recovery user hook, restarts Jenkins, and prints the login URL.

.EXAMPLE
    .\Set-JenkinsRecoveryUser.ps1 -RecoveryPassword "MyTemporaryPassword123!"
    Creates the recovery user hook with a custom temporary password.

.EXAMPLE
    .\Set-JenkinsRecoveryUser.ps1 -Action Cleanup
    Removes the recovery user hook and restarts Jenkins.

.NOTES
    Author: Dean Ammons
    Date: April 2026
#>

[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'High')]
param(
    [Parameter()]
    [ValidateSet('Create', 'Cleanup')]
    [string]$Action = 'Create',

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$RecoveryUsername = 'recovery-admin',

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$RecoveryPassword = 'ChangeMeNow!123!',

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$HookPath = '/var/lib/jenkins/init.groovy.d/99-recovery-user.groovy',

    [Parameter()]
    [ValidateRange(10, 300)]
    [int]$TimeoutSeconds = 60
)

function Invoke-WslRootCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command
    )

    $result = wsl -u root bash -lc $Command 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "WSL command failed: $result"
    }

    return $result
}

function Get-WslPrimaryIpAddress {
    try {
        return (wsl hostname -I).Trim() -split '\s+' | Select-Object -First 1
    }
    catch {
        return $null
    }
}

function Test-JenkinsLoginReady {
    param(
        [int]$TimeoutInSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutInSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri 'http://localhost:8081/login' -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                return $true
            }
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    return $false
}

function Escape-GroovySingleQuotedValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    return $Value.Replace('\', '\\').Replace("'", "\\'")
}

Write-Host ''
Write-Host '=== Jenkins Recovery User Script ===' -ForegroundColor Cyan
Write-Host "Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray
Write-Host "Action: $Action" -ForegroundColor Gray
Write-Host ''

try {
    $null = Invoke-WslRootCommand -Command 'test -d /var/lib/jenkins'
}
catch {
    Write-Host 'ERROR: Jenkins home directory was not found in WSL at /var/lib/jenkins.' -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Yellow
    Write-Host ''
    exit 1
}

if ($Action -eq 'Create') {
    $writeCommand = "mkdir -p `$(dirname '$HookPath') && cat > '$HookPath'"
    $escapedUsername = Escape-GroovySingleQuotedValue -Value $RecoveryUsername
    $escapedPassword = Escape-GroovySingleQuotedValue -Value $RecoveryPassword
    $groovyContent = @"
import jenkins.model.Jenkins
import hudson.model.User

def instance = Jenkins.get()
def realm = instance.getSecurityRealm()
def username = '${escapedUsername}'
def password = '${escapedPassword}'

if (User.getById(username, false) == null) {
    realm.createAccount(username, password)
    println('Created recovery user: ' + username)
} else {
    println('Recovery user already exists: ' + username)
}

instance.save()
"@

    if ($PSCmdlet.ShouldProcess($HookPath, 'Create Jenkins recovery hook')) {
        Write-Host 'Creating Jenkins recovery hook in WSL...' -ForegroundColor Yellow
        $groovyContent | wsl -u root bash -lc $writeCommand > $null

        if ($LASTEXITCODE -ne 0) {
            throw 'Failed to write the Jenkins recovery Groovy hook.'
        }
    }
}
else {
    if ($PSCmdlet.ShouldProcess($HookPath, 'Remove Jenkins recovery hook')) {
        Write-Host 'Removing Jenkins recovery hook from WSL...' -ForegroundColor Yellow
        Invoke-WslRootCommand -Command "rm -f $HookPath" | Out-Null
    }
}

if ($PSCmdlet.ShouldProcess('jenkins.service', 'Restart Jenkins')) {
    Write-Host 'Restarting Jenkins...' -ForegroundColor Yellow
    Invoke-WslRootCommand -Command 'systemctl restart jenkins'
}

Write-Host 'Waiting for Jenkins login page...' -ForegroundColor Yellow
if (-not (Test-JenkinsLoginReady -TimeoutInSeconds $TimeoutSeconds)) {
    Write-Host 'WARNING: Jenkins did not report ready within the timeout window.' -ForegroundColor Yellow
    Write-Host 'Check service status with: wsl -u root systemctl status jenkins' -ForegroundColor Gray
    Write-Host ''
    exit 1
}

$wslIpAddress = Get-WslPrimaryIpAddress

Write-Host ''
if ($Action -eq 'Create') {
    Write-Host 'Recovery user is ready.' -ForegroundColor Green
    Write-Host "Username: $RecoveryUsername" -ForegroundColor White
    Write-Host "Password: $RecoveryPassword" -ForegroundColor White
    Write-Host 'Sign in with the temporary recovery account, then reset the password for your normal Jenkins user.' -ForegroundColor Gray
}
else {
    Write-Host 'Recovery hook removed.' -ForegroundColor Green
}

Write-Host 'Jenkins Login URL: http://localhost:8081/login' -ForegroundColor White
if ($wslIpAddress) {
    Write-Host "Jenkins WSL URL:   http://${wslIpAddress}:8081/login" -ForegroundColor Gray
}
Write-Host ''