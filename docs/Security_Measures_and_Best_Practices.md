# Security Measures and Best Practices

**Project:** Task Activity Management System  
**Document Version:** 1.0  
**Last Updated:** January 5, 2026  
**Classification:** Internal Documentation

---

## Executive Summary

This document provides a comprehensive overview of all security measures, controls, and best practices implemented in the Task Activity Management System. Security is a critical priority for this application, and this document demonstrates our commitment to protecting user data, preventing unauthorized access, and maintaining system integrity.

This document is maintained as a living reference and will be updated as new security features are implemented or existing controls are enhanced.

---

## Table of Contents

1. [Authentication & Authorization](#authentication--authorization)
2. [Access Control & Permissions](#access-control--permissions)
3. [Password Security](#password-security)
4. [Account Protection](#account-protection)
5. [Session Management](#session-management)
6. [API Security](#api-security)
7. [Data Protection](#data-protection)
8. [Network Security](#network-security)
9. [Application Security Headers](#application-security-headers)
10. [Input Validation & Injection Prevention](#input-validation--injection-prevention)
11. [Security Monitoring & Logging](#security-monitoring--logging)
12. [Deployment Security](#deployment-security)
13. [Security Testing](#security-testing)
14. [Compliance & Standards](#compliance--standards)
15. [Recent Security Enhancements](#recent-security-enhancements)

---

## Authentication & Authorization

### JWT-Based Authentication

**Implementation:**

-   JSON Web Tokens (JWT) for stateless API authentication
-   Separate access tokens (short-lived) and refresh tokens (longer-lived)
-   Token type validation prevents token misuse
-   JJWT library (0.12.6) for secure token generation and validation

**Security Features:**

-   **Token Type Differentiation** (Added: January 2026)
    -   Access tokens marked with `token_type: "access"`
    -   Refresh tokens marked with `token_type: "refresh"`
    -   Refresh endpoint validates token type and rejects access tokens
    -   Prevents unauthorized token reuse across different endpoints
-   **Secret Key Management:**
    -   512-bit secret key minimum (validated at runtime)
    -   Configurable via environment variables
    -   Never hardcoded in source code
-   **Token Claims:**
    -   Username, roles, and permissions embedded in token
    -   Expiration timestamps enforced
    -   Custom claims for additional security metadata

### Database-Driven Role-Based Access Control (RBAC)

**Architecture:**

-   **Dynamic Role Management:** Roles stored in database, not hardcoded
-   **Flexible Permissions:** Fine-grained permissions with resource:action pattern
-   **Custom Role Creation:** Administrators can create new roles via web UI without code deployment
-   **Many-to-Many Relationships:** Roles can have multiple permissions, permissions can belong to multiple roles

**Default Roles:**

-   **ADMIN:** Full system access, user management, permission management
-   **USER:** Standard access to task activities and own data
-   **GUEST:** Limited access to task activities only (no expense features)
-   **EXPENSE_ADMIN:** Specialized role for expense approval and management

**Permission System:**

-   **Resource-Based Permissions:** Format: `RESOURCE:ACTION` (e.g., `TASK_ACTIVITY:READ`, `EXPENSE:APPROVE`)
-   **Method-Level Security:** `@RequirePermission` annotation for fine-grained control
-   **Runtime Permission Checks:** Spring AOP aspect intercepts method calls and validates permissions
-   **Self-Service Permissions:** USER role has `USER_MANAGEMENT:READ` for profile access

### Authentication Mechanisms

**Form-Based Authentication:**

-   Custom login page with CSRF protection
-   Secure credential transmission (HTTPS required in production)
-   Custom authentication success and failure handlers

**Custom Authentication Provider:**

-   Validates credentials against database
-   Enforces password expiration policies
-   Checks account status (enabled, locked, expired)
-   Logs authentication attempts for security monitoring

**Account Status Validation** (Enhanced: January 2026)

-   **Post-Authentication Checks:** JWT filter validates account status after token validation
-   **Four Status Checks:**
    -   `isEnabled()` - Account must be enabled
    -   `isAccountNonLocked()` - Account must not be locked
    -   `isAccountNonExpired()` - Account must not be expired
    -   `isCredentialsNonExpired()` - Credentials must not be expired
-   **Prevents Token-Based Bypass:** Users disabled/locked after token issuance cannot access system
-   **Graceful Rejection:** Returns 401 Unauthorized with clear error messages

---

## Access Control & Permissions

### URL-Based Security (Defense-in-Depth)

**Admin Endpoints Protection** (Enhanced: January 2026)

-   `/api/admin/**` endpoints require authentication
-   Method-level `@RequirePermission` annotations control granular access
-   Two-layer security: URL authentication + permission checks
-   Prevents anonymous access while allowing permission-based control

**Public Endpoints:**

-   `/api/auth/**` - Login and token refresh endpoints
-   `/api/health/**` - Health check endpoints
-   `/docs/**` - Public S3 documentation
-   Static resources (CSS, JS, images)

**Protected Endpoints:**

-   `/api/users/me` - Authenticated users can access own profile
-   `/api/users/**` - ADMIN role required
-   `/api/admin/**` - Authentication required, permissions control access
-   `/api/task-activities/**` - Role-based access
-   `/api/expenses/**` - Role-based access with email requirement

### Custom Permission Framework

**@RequirePermission Annotation:**

```java
@RequirePermission(resource = "TASK_ACTIVITY", action = "READ")
public ResponseEntity<?> getTaskActivities() { ... }
```

**PermissionAspect:**

-   Spring AOP interceptor
-   Validates user permissions before method execution
-   Retrieves user permissions from database
-   Returns 403 Forbidden if permission denied
-   Logs unauthorized access attempts

**Permission Service:**

-   Caches user permissions for performance
-   Queries role_permissions join table
-   Supports complex permission combinations
-   Enables audit trails for permission checks

### Role-Based UI Access Control

**Angular Role Guards:**

-   Route guards prevent unauthorized navigation
-   Conditional UI rendering based on user roles
-   API calls include role validation

**Feature Visibility:**

-   Admin-only features hidden from regular users
-   Role-specific menu items
-   Dynamic form field visibility based on permissions

---

## Password Security

### Password Policies

**Complexity Requirements:**

-   Minimum 8 characters
-   Must contain uppercase and lowercase letters
-   Must contain at least one digit
-   Must contain at least one special character
-   Validated on client-side and server-side

**Password Encoding:**

-   **BCrypt Algorithm:** Industry-standard one-way hashing
-   **Strength Factor:** Configurable work factor (default: 10)
-   **Salting:** Automatic random salt per password
-   **No Plain Text Storage:** Passwords never stored in readable form

### Password Lifecycle Management

**Password Expiration:**

-   **90-Day Expiration Policy:** Passwords expire after 90 days
-   **Advance Warnings:** Users notified 14 days before expiration
-   **Grace Period:** 7 days after expiration before forced change
-   **Automatic Enforcement:** System blocks access after grace period

**Forced Password Updates:**

-   **Administrative Reset:** Admins can force password change on next login
-   **Security Incidents:** Compromised accounts can be secured immediately
-   **Filter Interceptor:** ForcePasswordUpdateFilter checks on every request
-   **Redirect to Change Password:** Users cannot proceed until password changed

**Password Change Process:**

-   **Current Password Verification:** Users must provide current password
-   **New Password Validation:** Enforces complexity requirements
-   **Password History:** (Recommended enhancement) Prevent reuse of recent passwords
-   **Secure Transmission:** HTTPS ensures encrypted transmission

---

## Account Protection

### Account Lockout Policy

**Automatic Lockout:**

-   **Trigger:** 5 consecutive failed login attempts
-   **Scope:** Applies to all user roles (ADMIN, USER, GUEST, EXPENSE_ADMIN)
-   **Immediate Effect:** Account locked on 5th failed attempt
-   **Database State:** `account_locked` flag set to true

**Lockout Tracking:**

-   **Failed Attempt Counter:** Incremented on each failed login
-   **Counter Reset:** Cleared on successful login
-   **IP Address Logging:** Last failed attempt IP recorded
-   **Timestamp Recording:** Lockout time stored for audit

**Administrator Notifications:**

-   **Automatic Email Alerts:** Sent to configured admin email
-   **Email Contents:**
    -   Username of locked account
    -   Number of failed login attempts
    -   IP address of last failed attempt
    -   Timestamp of lockout event
-   **Configuration:** Set via `app.mail.admin-email` property

**Account Unlock Process:**

-   **Manual Unlock:** Only administrators can unlock accounts
-   **Web UI Access:** Through "Manage Users" interface
-   **Counter Reset:** Failed login attempts reset to 0
-   **Password Reset Recommendation:** Admins should consider password reset for suspicious lockouts

**Security Benefits:**

-   Prevents brute-force password attacks
-   Provides early warning of unauthorized access attempts
-   Creates audit trail of security incidents
-   Limits attacker attempts

### Rate Limiting

**Endpoint Protection:**

-   **Bucket4j Implementation:** Token bucket algorithm
-   **Configurable Limits:** Per-endpoint rate limits
-   **User-Based Throttling:** Limits per authenticated user
-   **IP-Based Throttling:** Limits per IP address for anonymous endpoints

**Configuration:**

-   **Login Endpoint:** Limited to prevent credential stuffing
-   **API Endpoints:** Configurable limits per resource
-   **Rate Limit Headers:** X-RateLimit-\* headers in responses
-   **Graceful Degradation:** 429 Too Many Requests status

---

## Session Management

### Session Configuration

**Session Properties:**

-   **Timeout:** 30 minutes of inactivity
-   **Cookie Security:**
    -   HttpOnly flag set (prevents XSS access)
    -   Secure flag set (HTTPS only)
    -   SameSite policy enabled
-   **Session Fixation Protection:** New session ID on authentication
-   **Concurrent Session Control:** Multiple sessions allowed with tracking

**Session Registry:**

-   **Active Session Tracking:** In-memory session registry
-   **Concurrent Session Management:** Monitor active sessions per user
-   **Session Invalidation:** Proper cleanup on logout
-   **Force Logout:** Administrators can terminate user sessions

### CSRF Protection

**Cross-Site Request Forgery Prevention:**

-   **CookieCsrfTokenRepository:** Token stored in cookie
-   **Token Validation:** Required for state-changing operations
-   **Angular Integration:** Automatic token inclusion in requests
-   **Exempt Endpoints:** API authentication endpoints exempt (using JWT)

**Token Management:**

-   **Unique Per Session:** New token generated per session
-   **Automatic Rotation:** Token refreshed on use
-   **Secure Transmission:** HTTPS ensures token security

### JWT Token Revocation

**Server-Side Token Blacklist:**

-   **Database-Backed Blacklist:** revoked_tokens table stores revoked JWT tokens
-   **JTI Tracking:** Each token has unique JWT ID (jti) claim for tracking
-   **Immediate Revocation:** Tokens can be invalidated before natural expiration
-   **Automatic Cleanup:** Expired tokens automatically removed daily at 2 AM

**Token Revocation Triggers:**

-   **User Logout:** Token added to blacklist via `/api/auth/logout` endpoint
-   **Password Change:** All user tokens revoked automatically
-   **Security Incidents:** Manual revocation by administrators
-   **Account Status Change:** Disabled/locked accounts trigger revocation

**Blacklist Storage:**

```sql
CREATE TABLE revoked_tokens (
    id BIGSERIAL PRIMARY KEY,
    jti VARCHAR(255) NOT NULL UNIQUE,  -- JWT ID claim
    username VARCHAR(50) NOT NULL,
    token_type VARCHAR(20) NOT NULL,   -- 'access' or 'refresh'
    expiration_time TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NOT NULL,
    reason VARCHAR(50) NOT NULL        -- 'logout', 'password_change', 'security_incident', 'manual'
);
CREATE INDEX idx_revoked_tokens_jti ON revoked_tokens(jti);
```

**Authentication Flow with Revocation:**

1. Client provides JWT token in Authorization header
2. Server validates token signature and expiration (existing)
3. **NEW:** Server checks if token JTI exists in blacklist
4. If revoked, authentication rejected with warning log
5. If not revoked, proceed with account status checks
6. Grant authentication if all checks pass

**Performance:**

-   **O(1) Lookup:** Indexed JTI column for fast blacklist check
-   **Minimal Overhead:** < 50ms added latency per authentication
-   **Automatic Cleanup:** Daily job prevents table growth

**Security Benefits:**

-   **Immediate Invalidation:** Stolen tokens can be revoked instantly
-   **Password Change Protection:** Old tokens invalid after password reset
-   **Audit Trail:** All revocations logged with username, timestamp, reason
-   **Defense in Depth:** Complements signature + expiration + account status checks

**API Endpoints:**

-   `POST /api/auth/logout`: Revokes current token (requires Bearer token)
-   Future: Admin endpoint for manual revocation by username or JTI

---

## API Security

### REST API Security

**JWT Authentication:**

-   **Bearer Token Header:** Authorization: Bearer <token>
-   **Token Validation:** Every API request validates token
-   **Role Claims:** Token contains user roles
-   **Permission Claims:** Token contains user permissions

**API Endpoint Protection:**

-   **URL Security Matchers:** Spring Security request matchers
-   **Method Security:** @RequirePermission annotations
-   **Response Filtering:** Users only see authorized data

**Error Handling:**

-   **401 Unauthorized:** Authentication required
-   **403 Forbidden:** Insufficient permissions
-   **429 Too Many Requests:** Rate limit exceeded
-   **No Information Disclosure:** Generic error messages prevent enumeration

### CORS Configuration

**Cross-Origin Resource Sharing:**

-   **Allowed Origins:** Explicitly configured (not wildcard \*)
-   **Allowed Methods:** Limited to required HTTP methods
-   **Allowed Headers:** Whitelist approach
-   **Credentials Support:** Controlled credential sharing
-   **Preflight Handling:** Proper OPTIONS request handling

**Production Configuration:**

-   **Strict Origin Validation:** Only trusted domains allowed
-   **No Wildcard Origins:** Prevents CSRF attacks
-   **Environment-Specific:** Different CORS settings per environment

---

## Data Protection

### Database Security

**Connection Security:**

-   **Encrypted Connections:** SSL/TLS for database connections
-   **Connection Pooling:** HikariCP with secure configuration
-   **Credential Management:** Database credentials in environment variables
-   **Least Privilege:** Database user has minimal required permissions

**Query Security:**

-   **Parameterized Queries:** JPA prevents SQL injection
-   **Named Parameters:** No string concatenation in queries
-   **Input Sanitization:** Bean validation on all inputs
-   **ORM Protection:** Hibernate protects against SQL injection

### Sensitive Data Handling

**Password Storage:**

-   **BCrypt Hashing:** One-way cryptographic hash
-   **No Reversible Encryption:** Passwords cannot be decrypted
-   **Salted Hashes:** Unique salt per password

**PII Protection:**

-   **Email Addresses:** Required for expense functionality
-   **Data Minimization:** Only collect necessary information
-   **Access Controls:** Users only access own data (except ADMIN)

### File Upload Security (Receipt Handling)

**Receipt Security** (Enhanced: January 2026)

-   **Content-Disposition Header:** Set to "attachment" (prevents XSS)
-   **No Inline Display:** Files downloaded, not rendered in browser
-   **XSS Prevention:** Malicious files cannot execute in browser context
-   **File Type Validation:** (Recommended enhancement) Validate file types
-   **Size Limits:** Maximum file size enforced
-   **Storage Security:** S3 bucket with restricted access

---

## Network Security

### HTTPS Enforcement

**Production Requirements:**

-   **TLS 1.2+ Required:** Minimum TLS version enforced
-   **Strong Cipher Suites:** Only secure ciphers allowed
-   **Certificate Validation:** Valid SSL certificates required
-   **HSTS Header:** Strict-Transport-Security enforced

### CloudFlare Integration (Added: January 2026)

**Reverse Proxy Security:**

-   **CF-Connecting-IP Header:** Application trusts CloudFlare's client IP header
-   **Rate Limiting:** Uses CF-Connecting-IP for accurate rate limiting (cannot be spoofed)
-   **DDoS Protection:** CloudFlare provides Layer 3/4/7 DDoS mitigation
-   **Web Application Firewall:** CloudFlare WAF rules protect against common attacks
-   **SSL/TLS Termination:** Full encryption end-to-end
-   **Caching & Performance:** Edge caching reduces server load and attack surface

**Security Benefits:**

-   **IP Spoofing Prevention:** CF-Connecting-IP set by CloudFlare, not client
-   **X-Forwarded-For Ignored:** Application does not trust easily-spoofed headers
-   **Fallback Mechanism:** Uses getRemoteAddr() when not behind CloudFlare
-   **Rate Limit Integrity:** Attackers cannot bypass rate limits via header manipulation

### AWS Network Security

**VPC Configuration:**

-   **Private Subnets:** Application servers in private subnets
-   **Security Groups:** Firewall rules limit traffic
-   **Network ACLs:** Additional network layer security

**CloudFlare Reverse Proxy:**

-   **CF-Connecting-IP:** Trusted client IP header (cannot be spoofed)
-   **SSL/TLS Termination:** Full encryption end-to-end
-   **Health Checks:** Only healthy instances receive traffic
-   **DDoS Protection:** Layer 3/4/7 DDoS mitigation
-   **Web Application Firewall:** Protection against common attacks
-   **Rate Limiting Integration:** Application uses CF-Connecting-IP for accurate rate limiting

---

## Application Security Headers

### HTTP Security Headers (Fully Implemented: January 2026)

All security headers are now implemented and configured in `SecurityConfig.java`. These headers provide multiple layers of protection against common web vulnerabilities.

**X-Content-Type-Options:**

-   **Value:** `nosniff`
-   **Purpose:** Prevents MIME type sniffing attacks
-   **Browser Behavior:** Forces browser to respect Content-Type header
-   **XSS Protection:** Prevents drive-by download attacks
-   **Implementation:** Applied globally to all responses

**X-Frame-Options:**

-   **Value:** `DENY`
-   **Purpose:** Prevents clickjacking attacks
-   **Browser Behavior:** Page cannot be embedded in iframe or frame
-   **Protection:** Prevents UI redressing attacks
-   **Implementation:** Configured in SecurityConfig headers configuration
-   **Status:** ✅ Implemented

**X-XSS-Protection:**

-   **Value:** `1; mode=block`
-   **Purpose:** Enables browser XSS filtering
-   **Browser Behavior:** Blocks suspected XSS attacks completely
-   **Legacy Support:** For older browsers that don't support CSP
-   **Implementation:** Configured with ENABLED_MODE_BLOCK
-   **Status:** ✅ Implemented

**Content-Security-Policy (Hardened: January 2026):**

-   **Current Implementation:**
    -   `default-src 'self'` - Only load resources from same origin
    -   `script-src 'self' 'unsafe-inline'` - Scripts from same origin + inline (for Thymeleaf)
    -   `style-src 'self' 'unsafe-inline'` - Styles from same origin + inline
    -   `img-src 'self' data:` - Images from same origin and data URIs
    -   `font-src 'self' data:` - Fonts from same origin and data URIs
    -   `connect-src 'self'` - API calls only to same origin
-   **Security Enhancements:**
    -   **Removed 'unsafe-eval':** Prevents dynamic code execution (eval, Function constructor)
    -   **XSS Prevention:** Restricts script sources to prevent injection attacks
    -   **Data Exfiltration Prevention:** connect-src limits API destinations
-   **Future Improvements:**
    -   Migrate inline scripts to external files
    -   Implement nonce-based CSP for inline scripts
    -   Remove 'unsafe-inline' completely
-   **Benefits:** Prevents code injection and data exfiltration attacks
-   **Status:** ✅ Implemented

**Strict-Transport-Security (HSTS):**

-   **Value:** `max-age=31536000; includeSubDomains`
-   **Purpose:** Forces HTTPS connections for 1 year
-   **Browser Behavior:** Browsers always use HTTPS, even if user types http://
-   **Sub-domains:** includeSubDomains applies policy to all subdomains
-   **Duration:** 31,536,000 seconds (1 year)
-   **Implementation:** Configured in SecurityConfig headers
-   **Status:** ✅ Implemented

**Referrer-Policy:**

-   **Value:** `strict-origin-when-cross-origin`
-   **Purpose:** Controls how much referrer information is sent
-   **Behavior:** 
    -   Same-origin requests: Full URL sent
    -   Cross-origin HTTPS→HTTPS: Only origin sent
    -   Cross-origin HTTPS→HTTP: No referrer sent
-   **Privacy Protection:** Prevents leaking sensitive URL parameters
-   **Implementation:** Configured in SecurityConfig headers
-   **Status:** ✅ Implemented (January 2026)

**Permissions-Policy:**

-   **Value:** `geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=()`
-   **Purpose:** Disables unnecessary browser features
-   **Protected Features:**
    -   Geolocation access blocked
    -   Microphone access blocked
    -   Camera access blocked
    -   Payment API blocked
    -   USB device access blocked
    -   Device sensors blocked
-   **Security Benefits:** Reduces attack surface and prevents unauthorized feature access
-   **Implementation:** Configured in SecurityConfig headers
-   **Status:** ✅ Implemented (January 2026)

**Cache-Control:**

-   **Sensitive Endpoints:** `no-store, no-cache, must-revalidate`
-   **Static Resources:** Appropriate caching policies
-   **Purpose:** Prevent sensitive data caching

---

## Input Validation & Injection Prevention

### Bean Validation

**Validation Framework:**

-   **Jakarta Validation API:** Standard validation annotations
-   **Hibernate Validator:** Implementation provider
-   **Automatic Validation:** @Valid on controller methods

**Validation Annotations:**

-   @NotNull, @NotEmpty, @NotBlank
-   @Size, @Min, @Max
-   @Email, @Pattern (regex)
-   @Past, @Future (dates)
-   Custom validators for business logic

### SQL Injection Prevention

**Primary Defense:**

-   **JPA/Hibernate ORM:** Automatic query parameterization
-   **Named Parameters:** @Param annotations in queries
-   **Criteria API:** Type-safe query construction
-   **No Dynamic SQL:** No string concatenation in queries

**Query Examples:**

```java
// Safe - Parameterized query
@Query("SELECT t FROM TaskActivity t WHERE t.username = :username")
List<TaskActivity> findByUsername(@Param("username") String username);

// Safe - JPA method query
List<TaskActivity> findByUsernameAndClientContaining(String username, String client);
```

### XSS Prevention

**Output Encoding:**

-   **Thymeleaf Auto-Escaping:** All dynamic content escaped by default
-   **Angular Sanitization:** Angular sanitizes untrusted content
-   **Content Security Policy:** (Planned) Restrict inline scripts

**Input Sanitization:**

-   **Bean Validation:** Validates input format
-   **Whitelist Approach:** Only accept expected input patterns
-   **HTML Stripping:** Remove HTML tags from user input (where appropriate)

### Path Traversal Prevention

**File Access Security:**

-   **Absolute Path Validation:** Validate file paths
-   **No User-Controlled Paths:** File paths constructed by application
-   **S3 Key Validation:** Validate S3 object keys
-   **Access Control:** File access requires authorization

### File Upload Security

**Magic Number Validation:**

-   **Content Verification:** Validates actual file content, not just headers
-   **File Signatures:** Checks magic numbers (first bytes of file)
-   **Supported Types:**
    -   JPEG: `FF D8 FF` (first 3 bytes)
    -   PNG: `89 50 4E 47 0D 0A 1A 0A` (first 8 bytes)
    -   PDF: `25 50 44 46` (%PDF, first 4 bytes)

**Attack Prevention:**

-   **Executable Upload:** Rejects .exe files renamed as .jpg
-   **Script Upload:** Rejects .js/.html files with fake Content-Type
-   **Polyglot Files:** Validates signature matches declared type
-   **Corrupted Files:** Rejects files too small for signature

**Upload Process:**

```java
// 1. Validate Content-Type header (whitelist)
if (!ALLOWED_TYPES.contains(file.getContentType())) {
    return "File type not allowed";
}

// 2. Validate magic numbers (actual content)
ValidationResult result = fileTypeValidator.validateFileType(file, file.getContentType());
if (!result.isSuccess()) {
    return result.getErrorMessage();
}

// 3. Store file with Content-Disposition: attachment (XSS prevention)
response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
```

**Security Layers:**

1. **Content-Type Whitelist:** Only JPEG, PNG, PDF allowed
2. **Magic Number Validation:** Verifies actual file content
3. **Size Limit:** Maximum 5MB per file
4. **XSS Headers:** Content-Disposition: attachment prevents inline execution
5. **Authorization:** Users can only upload to their own expenses

**Error Messages:**

-   Clear feedback when validation fails
-   Logs suspicious upload attempts
-   No information leakage about internal validation logic

---

## Security Monitoring & Logging

### Audit Logging

**Security Events Logged:**

-   **Authentication Events:**
    -   Login attempts (success and failure)
    -   Logout events
    -   Token generation and refresh
    -   Account lockouts
-   **Authorization Events:**
    -   Permission denied (403) events
    -   Unauthorized access attempts (401)
    -   Role changes
-   **Account Management:**
    -   User creation, modification, deletion
    -   Password changes and resets
    -   Account enable/disable actions

**Log Format:**

-   **Structured Logging:** Consistent format for parsing
-   **Timestamp:** ISO 8601 format
-   **Username:** Authenticated user (if available)
-   **IP Address:** Request source IP
-   **Action:** Specific action performed
-   **Result:** Success or failure
-   **Details:** Additional context

### Application Logging

**Log Levels:**

-   **ERROR:** Critical security events, exceptions
-   **WARN:** Suspicious activities, near-thresholds
-   **INFO:** Normal security events (logins, logouts)
-   **DEBUG:** Detailed troubleshooting (disabled in production)

**Log Management:**

-   **Logback Configuration:** Configurable appenders
-   **File Rotation:** Daily rotation with size limits
-   **Log Retention:** Configurable retention period
-   **AWS CloudWatch:** Centralized logging in AWS
-   **Log Analysis:** Searchable and analyzable logs

### Monitoring & Alerting

**Health Checks:**

-   **Spring Actuator:** /actuator/health endpoint
-   **Database Connectivity:** Connection pool health
-   **External Services:** S3, SES availability

**Security Monitoring:**

-   **Failed Login Attempts:** Track failed login patterns
-   **Account Lockouts:** Alert on lockout events
-   **Permission Violations:** Monitor 403 errors
-   **Unusual Activity:** Detect abnormal patterns

---

## Deployment Security

### Environment Configuration

**Secrets Management:**

-   **Environment Variables:** Sensitive configuration in env vars
-   **AWS Secrets Manager:** (Recommended) Centralized secret storage
-   **No Hardcoded Secrets:** Secrets never in source code
-   **Secret Rotation:** Regular credential rotation

**Configuration Files:**

-   **Externalized Configuration:** application.properties outside JAR
-   **Profile-Specific:** Separate configs per environment
-   **Git Ignore:** Sensitive files excluded from version control

### Container Security

**Docker Security:**

-   **Minimal Base Image:** Alpine-based images
-   **Non-Root User:** Application runs as non-root
-   **Layer Scanning:** Regular vulnerability scans
-   **Image Signing:** Verify image authenticity

**AWS ECS Security:**

-   **Task Roles:** IAM roles for ECS tasks
-   **Least Privilege:** Minimal required permissions
-   **Private Registry:** ECR for container images
-   **Network Isolation:** VPC with private subnets

### Database Security (AWS RDS)

**RDS Configuration:**

-   **Encryption at Rest:** AES-256 encryption
-   **Encryption in Transit:** SSL/TLS connections
-   **Automated Backups:** Daily backups with retention
-   **Multi-AZ Deployment:** High availability and failover
-   **Security Groups:** Database not publicly accessible

---

## Security Testing

### Automated Security Testing

**Unit Tests:**

-   **Security Configuration Tests:** Verify security rules
-   **Authentication Tests:** Test login flows
-   **Authorization Tests:** Verify permission checks
-   **Password Tests:** Validate password policies

**Integration Tests:**

-   **SecurityFixesIntegrationTest:** 24 comprehensive security tests
    -   Account status enforcement (5 tests)
    -   JWT token type validation (5 tests)
    -   Admin endpoint authorization (8 tests)
    -   Receipt security (4 tests)
    -   Combined security scenarios (2 tests)
-   **RoleCombinationIntegrationTest:** 29 role and permission tests
-   **Account Lockout Tests:** Verify lockout behavior
-   **CSRF Protection Tests:** Validate CSRF tokens

**Test Coverage:**

-   **286 Total Tests** with 0 failures
-   **Security Test Categories:**
    -   Authentication flows
    -   Authorization checks
    -   Permission validation
    -   Account status verification
    -   Token security
    -   Input validation
    -   Error handling

### Security Scanning

**Dependency Scanning:**

-   **Maven Dependency Check:** OWASP dependency checker
-   **CVE Monitoring:** Track known vulnerabilities
-   **Regular Updates:** Keep dependencies current

**Static Code Analysis:**

-   **SonarQube:** Code quality and security analysis
-   **Security Hotspots:** Identify potential vulnerabilities
-   **Code Smells:** Detect problematic patterns

---

## Compliance & Standards

### Security Standards Followed

**OWASP Top 10 (2021):**

-   ✅ **A01: Broken Access Control** - RBAC, permission framework, URL security
-   ✅ **A02: Cryptographic Failures** - BCrypt, TLS, encrypted connections
-   ✅ **A03: Injection** - Parameterized queries, input validation
-   ✅ **A04: Insecure Design** - Security-first architecture
-   ✅ **A05: Security Misconfiguration** - Secure defaults, hardened config
-   ✅ **A06: Vulnerable Components** - Dependency scanning, regular updates
-   ✅ **A07: Authentication Failures** - Strong auth, account lockout, MFA-ready
-   ✅ **A08: Software and Data Integrity** - Signed commits, verified dependencies
-   ✅ **A09: Security Logging Failures** - Comprehensive audit logging
-   ✅ **A10: Server-Side Request Forgery** - URL validation, whitelist approach

### Best Practices Implemented

**Defense in Depth:**

-   Multiple layers of security controls
-   URL-based + method-level authorization
-   Network + application + database security

**Principle of Least Privilege:**

-   Minimal permissions by default
-   Role-based access control
-   Database user restrictions

**Secure by Default:**

-   Security features enabled out-of-box
-   Strong defaults for all configurations
-   Opt-in for less secure options

**Security Testing:**

-   Automated security test suite
-   Regular dependency updates
-   Vulnerability scanning

---

## Recent Security Enhancements

### January 2026 Security Improvements (Phase 2)

**1. HTTP Security Headers - Full Implementation**

-   **X-Frame-Options:** DENY - Prevents clickjacking attacks
-   **X-XSS-Protection:** Enabled with blocking mode
-   **Content-Security-Policy:** Hardened policy restricting resource loading
-   **Strict-Transport-Security (HSTS):** 1 year max-age with includeSubDomains
-   **Referrer-Policy:** strict-origin-when-cross-origin for privacy protection
-   **Permissions-Policy:** Disabled geolocation, microphone, camera, payment, USB, sensors
-   **Implementation:** Configured in SecurityConfig.java headers configuration
-   **Impact:** Comprehensive protection against clickjacking, XSS, and other browser-based attacks

**2. CORS Configuration Consolidation**

-   **Removed Duplicate:** Eliminated duplicate CORS configuration in ServerConfig.java
-   **Single Source:** All CORS configuration now centralized in SecurityConfig.java
-   **Production Validation:** Wildcard origins with credentials fail fast in production
-   **Explicit Origins:** Production requires explicit origin lists for security
-   **Impact:** Prevents CORS misconfiguration and potential security vulnerabilities

**3. Session Cookie Security Enhancement**

-   **Secure Flag:** Added conditional secure flag for session cookies
-   **Configuration:** `server.servlet.session.cookie.secure=${SESSION_COOKIE_SECURE:false}`
-   **Development:** Defaults to false for local development (HTTP)
-   **Production:** Set to true in application-aws.properties for HTTPS
-   **Additional Flags:** HttpOnly and SameSite=Lax already configured
-   **Impact:** Prevents session cookie transmission over insecure connections in production

**4. Admin Query Audit Logging**

-   **What's Logged:** Username, timestamp, and SQL query (first 200 characters)
-   **Log Level:** INFO level with [AUDIT] prefix for easy filtering
-   **Implementation:** AdminQueryController and QueryExecutionService
-   **Format:** `[AUDIT] Admin query executed by user: {username} | Query: {sql}`
-   **Benefits:** 
    -   Track all admin SQL query executions
    -   Security incident investigation
    -   Compliance audit trail
    -   Accountability for privileged operations
-   **Impact:** Enhanced visibility into admin database access and operations

### January 2026 Security Update (Phase 1)

**Nine Critical Security Fixes Implemented:**

1. **Admin Endpoint Access Control**

    - **Issue:** `/api/admin/**` endpoints lacked URL-based security
    - **Fix:** Added authentication requirement with permission-based access control
    - **Impact:** Prevents anonymous access to admin functions
    - **Implementation:** SecurityConfig URL matchers + @RequirePermission

2. **JWT Token Type Differentiation**

    - **Issue:** No distinction between access and refresh tokens
    - **Fix:** Added `token_type` claim to tokens (access/refresh)
    - **Impact:** Prevents token misuse across different endpoints
    - **Implementation:** JwtUtil token generation and validation

3. **Refresh Token Validation**

    - **Issue:** Refresh endpoint accepted any valid JWT token
    - **Fix:** Validate token type in ApiAuthController
    - **Impact:** Prevents access tokens from being used as refresh tokens
    - **Implementation:** Token type check in /api/auth/refresh endpoint

4. **Account Status Enforcement**

    - **Issue:** Disabled/locked accounts could use existing valid tokens
    - **Fix:** Added account status checks in JwtAuthenticationFilter
    - **Impact:** Immediately blocks disabled/locked accounts even with valid tokens
    - **Implementation:** Post-authentication status validation

5. **Receipt XSS Prevention**

    - **Issue:** Receipt downloads used inline Content-Disposition
    - **Fix:** Changed to "attachment" disposition
    - **Impact:** Prevents XSS attacks via malicious receipt files
    - **Implementation:** ReceiptController header modification

6. **MIME Sniffing Protection**

    - **Issue:** X-Content-Type-Options header not configured
    - **Fix:** Enabled nosniff header globally
    - **Impact:** Prevents MIME type confusion attacks
    - **Implementation:** SecurityConfig header configuration

7. **Rate Limiting CloudFlare Integration**

    - **Issue:** Rate limiting trusted X-Forwarded-For header (easily spoofed)
    - **Fix:** Use CloudFlare's CF-Connecting-IP header instead
    - **Impact:** Prevents rate limit bypass via header spoofing
    - **Implementation:** RateLimitFilter now uses CF-Connecting-IP or getRemoteAddr()
    - **Rationale:** CloudFlare sets CF-Connecting-IP with real client IP, cannot be spoofed by clients

8. **CORS Wildcard Production Validation**

    - **Issue:** Wildcard CORS with credentials could be enabled in production
    - **Fix:** Fail-fast validation rejects wildcard CORS + credentials in production profiles
    - **Impact:** Prevents critical CORS misconfiguration that allows any origin to make authenticated requests
    - **Implementation:** SecurityConfig.corsConfigurationSource() validates profile and throws IllegalStateException
    - **Security:** Application fails to start if misconfigured (fail-secure design)

9. **Content Security Policy (CSP) Hardening**
    - **Issue:** CSP allowed 'unsafe-eval' enabling dynamic code execution (eval, Function constructor)
    - **Fix:** Removed 'unsafe-eval' from CSP directive
    - **Impact:** Prevents JavaScript injection attacks via eval() or Function() constructor
    - **Implementation:** SecurityConfig CSP directive updated to: `script-src 'self' 'unsafe-inline'`
    - **Note:** 'unsafe-inline' retained for Thymeleaf inline scripts (future: migrate to nonces)

10. **Password Hash Logging Removed**
    - **Issue:** UserDetailsServiceImpl logged first 20 characters of bcrypt password hashes at DEBUG level
    - **Fix:** Removed password hash from debug logs completely
    - **Impact:** Eliminates any password-related information from logs, even in DEBUG mode
    - **Implementation:** UserDetailsServiceImpl now only logs username: `"Loading user: {username}"`
    - **Security:** Follows principle of least information disclosure

11. **Server-Side Token Revocation System**
    - **Issue:** JWT tokens remained valid until expiration even after logout or password change
    - **Fix:** Implemented complete token revocation (blacklist) system with database persistence
    - **Impact:** 
      - Tokens revoked immediately on logout
      - All tokens invalidated on password change
      - Manual revocation available for security incidents
      - Automatic cleanup of expired tokens (daily at 2 AM)
    - **Implementation:**
      - New `revoked_tokens` database table with JTI, username, token_type, expiration_time, reason
      - New entity: `RevokedToken.java`
      - New repository: `RevokedTokenRepository.java` with blacklist queries
      - New service: `TokenRevocationService.java` with revocation logic and scheduled cleanup
      - Updated `JwtUtil.java`: Added UUID JTI claim to all tokens
      - Updated `JwtAuthenticationFilter.java`: Checks blacklist before granting authentication
      - New API endpoint: `POST /api/auth/logout` revokes token and adds to blacklist
      - Updated `UserService.java`: Revokes all user tokens on password change
    - **Performance:** O(1) blacklist check using indexed JTI column
    - **Security:** Defense in depth with signature + expiration + blacklist + account status checks

12. **File Upload Magic Number Validation**
    - **Issue:** Receipt upload only validated Content-Type header (client-controlled, easily spoofed)
    - **Fix:** Implemented magic number (file signature) validation to verify actual file content
    - **Impact:**
      - Prevents executable upload with fake image extension
      - Prevents script upload with fake Content-Type
      - Validates actual file content, not just headers
    - **Implementation:**
      - New utility: `FileTypeValidator.java` validates magic numbers
      - Supported types: JPEG (`FF D8 FF`), PNG (`89 50 4E 47...`), PDF (`25 50 44 46`)
      - Updated `ReceiptController.java`: Validates magic numbers before file storage
      - Descriptive error messages for rejected files
    - **Security:** Defense in depth with Content-Type + magic number validation + XSS headers

**Testing:**

-   34 new integration tests added (SecurityFixesIntegrationTest + ShortTermSecurityFixesIntegrationTest + RateLimitCloudFlareTest)
-   10 new tests added: TokenRevocationIntegrationTest (10 tests covering logout, password change, cleanup)
-   17 new tests added: FileTypeValidatorTest (17 tests covering valid files, fake files, attacks)
-   All tests passing (290+ total tests)
-   Comprehensive coverage of all security controls

**Documentation:**

-   Updated Administrator User Guide
-   Updated Technical Features Summary
-   Updated Task Activity Management Technology Stack (HTML)
-   Updated Security Measures document (this document)
-   Created Long_Term_Security_Fixes_Summary.md (detailed implementation guide)
-   Added inline code documentation
-   Updated schema.sql with revoked_tokens table

**Security Audit Status:**
- ✅ **All 12 critical/high-severity issues RESOLVED** (6 immediate + 3 short-term + 3 long-term)
- Comprehensive defense-in-depth security posture achieved
- Regular security testing and monitoring in place

---

## Future Security Enhancements (Roadmap)

### High Priority

1. **Multi-Factor Authentication (MFA)**

    - TOTP-based MFA for admin accounts
    - SMS/Email verification codes
    - Backup codes for account recovery

2. **Content Security Policy (CSP)**

    - Restrict script sources
    - Prevent inline script execution
    - Report CSP violations

3. **API Rate Limiting Enhancement**

    - Per-user rate limits
    - Endpoint-specific limits
    - Rate limit headers

4. **Password History**
    - Prevent password reuse
    - Track last 5-10 passwords
    - Configurable history size

### Medium Priority

5. **Advanced Audit Logging**

    - Immutable audit trail
    - Log aggregation
    - Anomaly detection

6. **File Upload Validation**

    - MIME type verification
    - File content scanning
    - Virus scanning integration

7. **Content Security Policy Hardening**
    - Remove 'unsafe-inline' from script-src
    - Implement nonce-based CSP
    - Migrate inline scripts to external files

8. **Session Security Enhancement**
    - Session fingerprinting
    - Device tracking
    - Suspicious activity detection

### Low Priority

9. **OAuth 2.0 / OIDC Support**

    - Social login integration
    - Enterprise SSO
    - OAuth2 authorization server

10. **Certificate Pinning**
    - Pin specific certificates
    - Enhance HTTPS security
    - Prevent MITM attacks

---

## Security Contact & Incident Response

### Reporting Security Issues

**Contact Information:**

-   **Primary Contact:** [Configure administrator email]
-   **Email:** [app.mail.admin-email configuration]
-   **Response Time:** Within 24 hours

**What to Report:**

-   Suspected vulnerabilities
-   Unauthorized access attempts
-   Data breaches or leaks
-   Security misconfigurations
-   Suspicious user behavior

### Incident Response Process

**Steps:**

1. **Identify:** Detect and classify security incident
2. **Contain:** Isolate affected systems/accounts
3. **Eradicate:** Remove threat and close vulnerabilities
4. **Recover:** Restore systems to normal operation
5. **Review:** Analyze incident and improve controls

**Escalation:**

-   Critical incidents: Immediate notification
-   High severity: Notification within 4 hours
-   Medium severity: Notification within 24 hours
-   Low severity: Addressed in next maintenance window

---

## Document Maintenance

### Version History

| Version | Date            | Author | Changes                                      |
| ------- | --------------- | ------ | -------------------------------------------- |
| 1.1     | January 5, 2026 | System | Security enhancements: HTTP headers, CORS consolidation, secure cookies, audit logging |
| 1.0     | January 5, 2026 | System | Initial comprehensive security documentation |

### Review Schedule

**Quarterly Reviews:**

-   Review security controls effectiveness
-   Update with new security features
-   Incorporate lessons learned
-   Address new threats and vulnerabilities

**Annual Reviews:**

-   Comprehensive security audit
-   Penetration testing
-   Compliance verification
-   Security awareness training update

### Document Updates

**When to Update:**

-   New security features implemented
-   Security incidents occur
-   Compliance requirements change
-   Vulnerabilities discovered and fixed
-   Security best practices evolve

**Update Process:**

1. Document changes in version history
2. Review with security team
3. Update related documentation
4. Communicate changes to stakeholders

---

## Conclusion

The Task Activity Management System implements comprehensive security measures across all layers of the application stack. From authentication and authorization to data protection and monitoring, security is a foundational principle, not an afterthought.

This document demonstrates our commitment to:

-   **Proactive Security:** Multiple defensive layers
-   **Industry Standards:** Following OWASP and security best practices
-   **Continuous Improvement:** Regular updates and enhancements
-   **Transparency:** Clear documentation of security controls
-   **Accountability:** Defined processes and responsibilities

Security is an ongoing process, and we continuously monitor, test, and improve our security posture to protect user data and maintain system integrity.

---

**Document Classification:** Internal Documentation  
**Confidentiality:** This document contains security implementation details and should be protected accordingly.
