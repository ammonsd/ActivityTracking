---
name: security-audit
description: "Performs security audits checking for exposed secrets, weak authentication, SQL injection, XSS vulnerabilities, and validates security best practices"
---

# Security Audit Skill

This skill performs security audits of the ActivityTracking application.

## When to Use

- Before production deployment
- After major security-related changes
- Regular security reviews (quarterly recommended)
- Investigating potential security issues

## Security Audit Checklist

### 1. Authentication & Authorization

#### JWT Token Security

- [ ] JWT_SECRET is 256-bit minimum
- [ ] JWT_SECRET stored in environment variable, not code
- [ ] Access token expiration set (24 hours recommended)
- [ ] Refresh token expiration set (7 days recommended)
- [ ] Token revocation implemented for logout/password changes
- [ ] Rate limiting on auth endpoints (5 req/min configured)

```powershell
# Check for hardcoded secrets
git grep -i "jwt.secret\s*=" src/
git grep -i "Bearer\s+[A-Za-z0-9-_=]+\.[A-Za-z0-9-_=]+\.[A-Za-z0-9-_=]+" src/
```

#### Password Security

- [ ] Passwords hashed with BCrypt (strength 10+)
- [ ] Password expiration policy enforced (90 days)
- [ ] Password history prevents reuse
- [ ] Account lockout after failed attempts (5 attempts)
- [ ] Password complexity requirements enforced (12+ chars, mixed case)

```java
// Verify BCrypt strength
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // Strength 12 recommended
}
```

#### Role-Based Access Control

- [ ] @PreAuthorize annotations on sensitive methods
- [ ] Principal validation in controllers
- [ ] User can only access their own data
- [ ] Admin-only endpoints properly protected

```bash
# Find methods without @PreAuthorize that should have it
grep -r "public.*delete\|public.*update" src/main/java/com/ammons/taskactivity/service/ | grep -v "@PreAuthorize"
```

### 2. Input Validation

#### SQL Injection Prevention

- [ ] All queries use parameterized queries
- [ ] No string concatenation in SQL
- [ ] @Query annotations use :param syntax

```bash
# Find potential SQL injection vulnerabilities
grep -r "\"SELECT.*+\|\"INSERT.*+\|\"UPDATE.*+" src/
```

#### XSS Prevention

- [ ] Thymeleaf templates use th:text (not th:utext)
- [ ] User input sanitized before display
- [ ] Content-Security-Policy header configured

```bash
# Check for unsafe Thymeleaf usage
grep -r "th:utext" src/main/resources/templates/
```

#### File Upload Security

- [ ] Magic number validation for file types
- [ ] File size limits enforced (max 10MB)
- [ ] Filename sanitization implemented
- [ ] Files stored outside web root

```java
// Verify magic number validation exists
private boolean isValidFileType(byte[] fileBytes) {
    // JPEG: FF D8 FF
    // PNG: 89 50 4E 47
    // PDF: 25 50 44 46
}
```

### 3. Sensitive Data Exposure

#### Environment Variables

- [ ] All secrets in environment variables
- [ ] No secrets in application.properties
- [ ] .env file in .gitignore

```powershell
# Check for exposed secrets in git history
git log -p | grep -i "password\|secret\|key" | grep -v "\.md"

# Check for secrets in committed files
git grep -i "password.*=\|secret.*=\|key.*=" | grep -v "\.example\|\.md"
```

#### Logging

- [ ] Passwords never logged
- [ ] JWT tokens never logged
- [ ] Credit card numbers never logged
- [ ] PII properly redacted in logs

```bash
# Check for potential secret logging
grep -r "log.*password\|log.*token\|log.*secret" src/ --include="*.java"
```

### 4. Security Headers

Check SecurityConfig for proper headers:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            .frameOptions().deny()  // ✅ X-Frame-Options
            .contentSecurityPolicy("default-src 'self'")  // ✅ CSP
            .xssProtection().block(true)  // ✅ XSS Protection
            .contentTypeOptions().disable()  // ❌ Should be enabled
        );
}
```

**Required Headers:**

- [ ] X-Frame-Options: DENY
- [ ] Content-Security-Policy: default-src 'self'
- [ ] X-Content-Type-Options: nosniff
- [ ] Referrer-Policy: no-referrer
- [ ] Permissions-Policy: geolocation=(), microphone=(), camera=()
- [ ] Strict-Transport-Security (HSTS in production)

### 5. CORS Configuration

```properties
# Production - Strict
cors.allowed-origins=https://yourdomain.com

# Development - Permissive (OK)
cors.allowed-origins=http://localhost:4200,http://localhost:8080
```

- [ ] CORS origins restricted in production
- [ ] Credentials allowed only for same-origin
- [ ] Methods limited to required set

### 6. Dependency Vulnerabilities

```powershell
# Check for known vulnerabilities
./mvnw dependency:tree
./mvnw dependency-check:check

cd frontend
npm audit
npm audit fix
```

- [ ] All dependencies up-to-date
- [ ] No critical vulnerabilities
- [ ] No high vulnerabilities in production

### 7. API Security

#### Rate Limiting

- [ ] Rate limiting on auth endpoints
- [ ] Bucket4j configuration verified
- [ ] DDoS protection considered

```java
// Verify rate limiting exists
@Configuration
public class RateLimitConfig {
    @Bean
    public Bucket createNewBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
            .build();
    }
}
```

#### API Endpoint Security

- [ ] All /api/\* endpoints require JWT
- [ ] Proper HTTP methods used (POST for create, PUT for update)
- [ ] 401 Unauthorized returned for invalid tokens
- [ ] 403 Forbidden returned for insufficient permissions

```bash
# Find API endpoints without security
grep -r "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" src/main/java/com/ammons/taskactivity/controller/ -A 3 | grep -v "@PreAuthorize"
```

### 8. Database Security

- [ ] Database user has minimal permissions
- [ ] Database connection encrypted (SSL in production)
- [ ] Database credentials in Secrets Manager (AWS)
- [ ] Connection pooling configured properly

```properties
# Check database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/taskactivity?ssl=true&sslmode=require
```

### 9. AWS Security (Production)

- [ ] IAM roles follow least privilege
- [ ] S3 bucket not publicly accessible
- [ ] S3 encryption at rest enabled
- [ ] RDS encryption enabled
- [ ] Security groups properly configured
- [ ] VPC with private subnets for ECS/RDS

```powershell
# Check S3 bucket public access
aws s3api get-bucket-policy-status --bucket taskactivity-receipts-prod

# Check RDS encryption
aws rds describe-db-instances --db-instance-identifier taskactivity-db | jq '.DBInstances[0].StorageEncrypted'
```

### 10. Session Management

- [ ] JWT tokens have expiration
- [ ] Refresh tokens have expiration
- [ ] Token revocation on logout
- [ ] Token revocation on password change
- [ ] No session fixation vulnerabilities

## Security Audit Report Template

```markdown
# Security Audit Report

**Date:** January 19, 2026
**Auditor:** Dean Ammons
**Application:** ActivityTracking
**Version:** 0.0.1-SNAPSHOT

## Executive Summary

Overall Security Rating: [High/Medium/Low]

## Findings

### Critical Issues (Fix Immediately)

1. [Issue description]
    - **Risk:** [Description of risk]
    - **Remediation:** [How to fix]
    - **Priority:** Critical

### High Priority Issues

1. [Issue description]
    - **Risk:** [Description of risk]
    - **Remediation:** [How to fix]
    - **Priority:** High

### Medium Priority Issues

1. [Issue description]

### Low Priority Issues

1. [Issue description]

### Best Practices Recommendations

1. [Recommendation]

## Compliance Checklist

- [ ] OWASP Top 10 addressed
- [ ] PCI DSS compliance (if handling payments)
- [ ] GDPR compliance (if EU users)
- [ ] SOC 2 requirements (if applicable)

## Action Items

| Issue              | Priority | Owner | Due Date   | Status |
| ------------------ | -------- | ----- | ---------- | ------ |
| Fix exposed secret | Critical | Dean  | 2026-01-20 | Open   |

## Next Audit

Recommended: [3 months from now]
```

## Quick Security Scan Commands

```powershell
# Full security scan
.\scripts\security-scan.ps1

# Check for exposed secrets
git grep -i "password.*=\|secret.*=\|api.key.*=" | grep -v "\.md\|\.example"

# Check dependencies
./mvnw dependency-check:check
cd frontend && npm audit

# Check for SQL injection
grep -r "\"SELECT.*+\|\"INSERT.*+" src/

# Check for hardcoded IPs or URLs
grep -r "\d\{1,3\}\.\d\{1,3\}\.\d\{1,3\}\.\d\{1,3\}" src/ --include="*.java"
```

## Remediation Priority

1. **Critical** - Fix within 24 hours
    - Exposed credentials
    - SQL injection vulnerabilities
    - Authentication bypass

2. **High** - Fix within 1 week
    - Missing authorization checks
    - XSS vulnerabilities
    - Weak encryption

3. **Medium** - Fix within 1 month
    - Missing security headers
    - Outdated dependencies
    - Weak password policy

4. **Low** - Fix in next sprint
    - Code style security issues
    - Documentation gaps
    - Logging improvements

## Memory Bank References

- Check `ai/project-overview.md` for security features
- Check `ai/architecture-patterns.md` for security architecture
- Check `docs/Security_Measures_and_Best_Practices.md` for detailed security docs
