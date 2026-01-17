#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Generates a JWT token for Jenkins using Maven exec plugin.

.DESCRIPTION
    Simpler approach using Maven to handle classpath automatically.

.EXAMPLE
    .\generate-jenkins-token-simple.ps1

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

[CmdletBinding()]
param()

Write-Host ""
Write-Host "=== Jenkins JWT Token Generator ===" -ForegroundColor Cyan
Write-Host ""

# Check for JWT_SECRET
if (-not $env:JWT_SECRET) {
    Write-Host "Getting JWT_SECRET from .env.local..." -ForegroundColor Yellow
    
    if (Test-Path ".env.local") {
        $secretLine = Select-String -Path .env.local -Pattern "^JWT_SECRET=" | Select-Object -First 1
        if ($secretLine) {
            $env:JWT_SECRET = ($secretLine.Line -split "=",  2)[1]
            Write-Host "✓ JWT_SECRET loaded from .env.local" -ForegroundColor Green
        } else {
            Write-Host "ERROR: JWT_SECRET not found in .env.local" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "ERROR: .env.local not found and JWT_SECRET not set!" -ForegroundColor Red
        Write-Host "Set JWT_SECRET environment variable or create .env.local file" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "Using JWT_SECRET: $($env:JWT_SECRET.Substring(0, 10))... (truncated)" -ForegroundColor Gray
Write-Host ""

# Generate token using Java directly with Maven dependencies
Write-Host "Generating JWT token..." -ForegroundColor Yellow

$javaCode = @"
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenGen {
    public static void main(String[] args) {
        String secret = System.getenv("JWT_SECRET");
        long expMs = 30L * 24 * 60 * 60 * 1000;
        Date now = new Date();
        Date exp = new Date(now.getTime() + expMs);
        
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("permissions", "JENKINS:NOTIFY");
        claims.put("type", "SERVICE_ACCOUNT");
        
        String token = Jwts.builder()
            .subject("JENKINS_SERVICE")
            .issuedAt(now)
            .expiration(exp)
            .id(UUID.randomUUID().toString())
            .claims(claims)
            .signWith(key)
            .compact();
        
        System.out.println("======================================================================");
        System.out.println("Jenkins API Token");
        System.out.println("======================================================================");
        System.out.println();
        System.out.println("Token (copy entire line below):");
        System.out.println("----------------------------------------------------------------------");
        System.out.println(token);
        System.out.println("----------------------------------------------------------------------");
        System.out.println();
        System.out.println("Valid for: 30 days");
        System.out.println("Expires: " + exp);
        System.out.println();
        System.out.println("Next Steps:");
        System.out.println("  1. Copy the token above");
        System.out.println("  2. Jenkins: http://172.27.85.228:8081");
        System.out.println("  3. Manage Jenkins → Credentials → jenkins-api-token → Update");
        System.out.println("  4. Paste token in Secret field → Save");
        System.out.println("======================================================================");
    }
}
"@

# Save temporary Java file
$tempJava = "TokenGen.java"
$javaCode | Out-File -FilePath $tempJava -Encoding ASCII

# Run with Maven
Write-Host "Executing..." -ForegroundColor Gray
.\mvnw.cmd -q exec:java -Dexec.mainClass="TokenGen" -Dexec.classpathScope=compile -Dexec.cleanupDaemonThreads=false

# Cleanup
Remove-Item $tempJava -ErrorAction SilentlyContinue

Write-Host ""
