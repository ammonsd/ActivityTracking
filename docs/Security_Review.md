# Security Review — Task Activity Management Application

**Review Date:** February 16, 2026
**Reviewer:** Dean Ammons (via GitHub Copilot)
**Application:** Task Activity Management API (Spring Boot 3.5.7 / Java 21)
**Scope:** Security review of AWS-deployed application covering backend, frontend (Angular + React), infrastructure, and configuration. Local development-only concerns are excluded.

---

## Table of Contents

- [Executive Summary](#executive-summary)
- [Positive Security Features](#positive-security-features)
- [Potential Security Issues](#potential-security-issues)
- [Risk Summary Matrix](#risk-summary-matrix)

---

## Executive Summary

The Task Activity Management application demonstrates a **mature security posture** with many industry best practices already in place. The application uses Spring Security with both session-based and JWT authentication, enforces strong password policies with history tracking, implements rate limiting, applies comprehensive HTTP security headers, and follows secure Docker deployment practices.

However, several areas need attention, ranging from a **critical credential exposure** in the Angular frontend to moderate issues around the admin SQL query endpoint and in-memory token revocation. This review identifies **3 critical/high**, **8 medium**, and **5 low** severity findings.

---

## Positive Security Features

### 1. Authentication and Authorization

| Feature                             | Details                                                                                                                     |
| ----------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| **BCrypt Password Hashing**         | Uses `BCryptPasswordEncoder` — industry-standard adaptive hashing algorithm                                                 |
| **JWT Token Security**              | Startup validation rejects missing, default, or weak JWT secrets (`JwtUtil.validateJwtSecret()`)                            |
| **Token Type Separation**           | Access and refresh tokens include `token_type` claim to prevent token confusion attacks                                     |
| **Token Revocation**                | `TokenRevocationService` supports individual token blacklisting (JTI-based) and bulk revocation on password change          |
| **Refresh Token Validation**        | `ApiAuthController.refreshToken()` explicitly validates `isRefreshToken()` before issuing new access tokens                 |
| **Account Lockout**                 | Configurable failed login attempt threshold (default: 5) with automatic account locking                                     |
| **Password Strength Policy**        | Minimum 10 characters, uppercase, digit, special character, no 3+ consecutive identical characters, cannot contain username |
| **Password History**                | Configurable password reuse prevention (default: 5 previous passwords checked)                                              |
| **Force Password Update**           | New accounts and admin-reset accounts require password change on first login                                                |
| **Guest Password Expiration**       | `CustomAuthenticationProvider` blocks expired GUEST accounts from authenticating                                            |
| **Role-Based Access Control**       | Multi-role system (ADMIN, USER, GUEST, EXPENSE_ADMIN, JENKINS_SERVICE) with URL-pattern and method-level security           |
| **Permission-Based Access Control** | Fine-grained `@RequirePermission` annotation with `PermissionAspect` AOP enforcement and database-driven permissions        |
| **Method-Level Security**           | `@EnableMethodSecurity(prePostEnabled = true)` with custom `PermissionEvaluator`                                            |
| **Session Management**              | Configurable maximum concurrent sessions (5), session invalidation on logout with cookie cleanup                            |

### 2. HTTP Security Headers

| Header                      | Configuration                                                                                                    |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| **Content-Security-Policy** | Restrictive policy with `default-src 'self'`, `frame-ancestors 'none'`, explicit external sources for Cloudflare |
| **X-Frame-Options**         | `DENY` — prevents all framing (clickjacking protection)                                                          |
| **HSTS**                    | 1-year max-age with `includeSubDomains` — forces HTTPS                                                           |
| **Referrer-Policy**         | `strict-origin-when-cross-origin` — limits referrer information leakage                                          |
| **Permissions-Policy**      | Disables camera, microphone, geolocation, payment APIs                                                           |
| **X-Content-Type-Options**  | `nosniff` — prevents MIME type sniffing                                                                          |

### 3. CSRF Protection

| Feature                      | Details                                                                                       |
| ---------------------------- | --------------------------------------------------------------------------------------------- |
| **CSRF Token Repository**    | `CookieCsrfTokenRepository` for session-based web requests                                    |
| **API CSRF Exemption**       | CSRF disabled only for stateless JWT API endpoints (`/api/**`) — correct for token-based auth |
| **Angular CSRF Integration** | Angular interceptor reads `XSRF-TOKEN` cookie and sends `X-XSRF-TOKEN` header                 |

### 4. CORS Configuration

| Feature                             | Details                                                                                                    |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| **Explicit Origins**                | Production uses `setAllowedOrigins()` with explicit domain list (not wildcards)                            |
| **Production Wildcard Prevention**  | `corsConfigurationSource()` throws `IllegalStateException` if wildcard CORS detected in production profile |
| **Environment-Based Configuration** | Origins configurable via `CORS_ALLOWED_ORIGINS` environment variable                                       |

### 5. Input Validation

| Feature                       | Details                                                                                                |
| ----------------------------- | ------------------------------------------------------------------------------------------------------ |
| **Bean Validation**           | `spring-boot-starter-validation` with `@Valid` on request bodies                                       |
| **Custom Password Validator** | `@ValidPassword` annotation with `PasswordValidator` implementation                                    |
| **File Upload Validation**    | Content-type whitelist (JPEG, PNG, PDF), file size limits, ownership verification                      |
| **Magic Number Validation**   | `FileTypeValidator` validates actual file bytes against known signatures — prevents extension spoofing |
| **CSV Import Validation**     | Extension check (`.csv` only), empty file check, role-restricted to ADMIN/MANAGER                      |

### 6. Error Handling (Production)

| Setting                               | Value   |
| ------------------------------------- | ------- |
| `server.error.include-message`        | `never` |
| `server.error.include-stacktrace`     | `never` |
| `server.error.include-exception`      | `false` |
| `server.error.include-binding-errors` | `never` |

### 7. Infrastructure Security

| Feature                            | Details                                                                                                |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------ |
| **Non-Root Docker Container**      | Both Dockerfile and Dockerfile.local create and run as `appuser`                                       |
| **Multi-Stage Docker Build**       | Build artifacts separated from runtime image, reducing attack surface                                  |
| **Docker Secrets (Production)**    | Production docker-compose uses Docker secrets via `_FILE` suffix pattern for JWT, database credentials |
| **Hibernate DDL Validation**       | `ddl-auto=validate` in production — prevents accidental schema modifications                           |
| **Open-in-View Disabled**          | `spring.jpa.open-in-view=false` — prevents lazy-loading issues and connection leaks                    |
| **Actuator Lockdown**              | Only `health` endpoint exposed; `show-details=never`, `show-components=never`                          |
| **Swagger Disabled in Production** | `springdoc.swagger-ui.enabled=false` and `springdoc.api-docs.enabled=false`                            |
| **HikariCP Leak Detection**        | `leak-detection-threshold=60000` — detects unclosed database connections                               |
| **Connection Pool Limits**         | Properly configured pool size, idle timeout, and max lifetime                                          |
| **Jenkins Credentials**            | Uses Jenkins credential store (`credentials('jenkins-api-token')`) — not hardcoded                     |

### 8. Logging and Audit

| Feature                         | Details                                                                                                |
| ------------------------------- | ------------------------------------------------------------------------------------------------------ |
| **Login Audit Trail**           | Failed/successful login attempts logged with IP address, GeoIP location                                |
| **Admin Query Audit**           | SQL queries executed via admin endpoint are logged with username                                       |
| **Security Event Logging**      | Token revocation, account lockout, and password changes are logged                                     |
| **Password Excluded from Logs** | `User.toString()` explicitly excludes password; `@JsonProperty(access = WRITE_ONLY)` on password field |
| **Rate Limit Logging**          | Rate limit violations logged with client IP                                                            |

### 9. Secrets Management

| Feature                                  | Details                                                                                |
| ---------------------------------------- | -------------------------------------------------------------------------------------- |
| **Environment Variable Externalization** | All secrets (JWT_SECRET, DB_PASSWORD, MAIL_PASSWORD) loaded from environment variables |
| **Docker Secrets Support**               | `SecretsEnvironmentPostProcessor` reads credentials from file-based Docker secrets     |
| **No Hardcoded Production Secrets**      | No API keys, passwords, or tokens found hardcoded in production source files           |

### 10. Rate Limiting

| Feature                     | Details                                                                                                    |
| --------------------------- | ---------------------------------------------------------------------------------------------------------- |
| **Bucket4j Token Bucket**   | Per-IP rate limiting on authentication endpoints                                                           |
| **Configurable Limits**     | Capacity and refill rate configurable via properties                                                       |
| **CloudFlare IP Detection** | Uses `CF-Connecting-IP` header (cannot be spoofed by clients); explicitly does NOT trust `X-Forwarded-For` |

---

## Potential Security Issues

### CRITICAL / HIGH Severity

---

#### Issue 1: Base64 Credentials Stored in Angular sessionStorage

**Severity:** 🔴 CRITICAL
**Location:** `frontend/src/app/services/auth.service.ts` (login method)
**Category:** Credential Exposure

**Description:**
The Angular frontend stores Base64-encoded `username:password` credentials in `sessionStorage` after login:

```typescript
sessionStorage.setItem("auth", btoa(username + ":" + password));
```

Base64 is encoding, not encryption — it is trivially reversible. Any XSS vulnerability anywhere in the application would allow an attacker to read `sessionStorage` and obtain the user's plaintext password.

**Why This Matters:**

- XSS attacks can read `sessionStorage` contents
- Base64 decoding reveals actual credentials: `atob(sessionStorage.getItem('auth'))`
- Violates OWASP guidance: credentials should never be stored client-side
- Session cookies (`JSESSIONID`) already handle authentication — this storage is unnecessary

**Suggested Fix:**
Remove the credential storage entirely. The application already uses session-based auth with `JSESSIONID` cookies, making client-side credential storage redundant:

```typescript
// REMOVE these lines:
// sessionStorage.setItem('auth', credentials);

// Keep only non-sensitive session data:
sessionStorage.setItem("username", username);
sessionStorage.setItem("userRole", role);
```

The React frontend already demonstrates the correct pattern — it uses only a Zustand in-memory store with no credential persistence.

---

#### Issue 2: Admin SQL Query Endpoint — SQL Injection Risk

**Severity:** 🔴 HIGH
**Location:** `src/main/java/com/ammons/taskactivity/service/QueryExecutionService.java` (line 54)
**Category:** SQL Injection

**Description:**
The `QueryExecutionService` executes raw SQL directly via `jdbcTemplate.queryForList(sql)` where `sql` is user-provided input from ADMIN users. While the service validates that queries start with `SELECT` and blocks dangerous keywords, this approach has known bypass risks:

- The `removeComments()` regex may not handle all SQL comment variants (e.g., MySQL's `/*!...*/` conditional comments)
- Keyword filtering can potentially be bypassed via encoding, Unicode characters, or database-specific syntax
- `SELECT` statements can still be weaponized: `SELECT * FROM pg_shadow` exposes password hashes, `SELECT pg_read_file('/etc/passwd')` reads files, `SELECT ... INTO OUTFILE` writes files

**Why This Matters:**
Even with ADMIN-only access, a compromised admin account or insider threat could:

- Read sensitive data from system tables (`pg_user`, `pg_shadow`, `information_schema`)
- Access database server filesystem via PostgreSQL functions
- Exfiltrate data through crafted SELECT queries

**Suggested Fix:**

1. **Use a read-only database user** for this endpoint:

```java
@Bean
@Qualifier("readOnlyDataSource")
public DataSource readOnlyDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(readOnlyUrl);
    config.setUsername(readOnlyUser); // DB user with SELECT-only grants
    config.setPassword(readOnlyPassword);
    config.setReadOnly(true);
    return new HikariDataSource(config);
}
```

2. **Add `@Transactional(readOnly = true)`** to enforce read-only at the transaction level:

```java
@Transactional(readOnly = true)
public String executeQueryAsCsv(String sql, String username) { ... }
```

3. **Restrict accessible tables** by blocking queries against system catalogs:

```java
private static final String[] BLOCKED_TABLES = {
    "pg_shadow", "pg_user", "pg_authid", "information_schema.columns",
    "pg_stat_activity", "pg_read_file", "pg_ls_dir"
};
```

4. **Consider a query allowlist** rather than blocklist approach for highest security.

---

#### Issue 3: Console Logging of Sensitive Data in Angular Frontend

**Severity:** 🟡 HIGH
**Location:** `frontend/src/app/services/auth.service.ts` (lines 67-96)
**Category:** Information Disclosure

**Description:**
The Angular auth service contains `console.log` statements that output user authentication details (username, role, authentication status) to the browser console. The admin guard (`admin.guard.ts`) also logs role information.

**Why This Matters:**

- Browser console output is accessible to anyone with physical access to the machine
- Browser extensions and injected scripts can intercept console output
- Development logging should never be present in production builds

**Suggested Fix:**
Remove all `console.log` statements from production code, or implement an environment-aware logging service:

```typescript
// Use Angular's isDevMode() or environment.production flag
if (!environment.production) {
    console.log("Debug info:", data);
}
```

Better yet, use a structured logging library that can be disabled in production builds.

---

### MEDIUM Severity

---

#### Issue 4: In-Memory Token Revocation Lost on Restart

**Severity:** 🟠 MEDIUM
**Location:** `src/main/java/com/ammons/taskactivity/service/TokenRevocationService.java`
**Category:** Authentication Bypass

**Description:**
The `TokenRevocationService` stores revoked tokens in a `ConcurrentHashMap` (in-memory). Application restarts clear all revocation entries, meaning previously revoked tokens become valid again until they naturally expire.

Similarly, `PasswordResetService` stores reset tokens in-memory — active reset tokens are lost on restart.

**Why This Matters:**

- After deploying updates or restarting the server, a user who was logged out (token revoked) could use their old token
- In a multi-instance deployment (e.g., ECS with 2+ tasks), revocations are not shared across instances

**Suggested Fix:**
For a single-instance deployment, this is a moderate risk (tokens have expiration). For higher security:

1. **Database-backed revocation:** Store revoked JTIs in a database table with TTL matching token expiration
2. **Redis-backed revocation:** Use Redis with TTL keys for distributed, high-performance token blacklisting
3. **Short-lived access tokens:** Reduce `jwt.expiration` from 24 hours to 15-30 minutes; use refresh tokens for renewal

---

#### Issue 5: Password Hash Generator Utility in Production Source

**Severity:** 🟠 MEDIUM
**Location:** `src/main/java/com/ammons/taskactivity/util/PasswordHashGenerator.java`
**Category:** Code Hygiene / Information Disclosure

**Description:**
A utility class containing hardcoded test password `"admin123"` and its BCrypt hash exists in `src/main/java`. This class ships with the production JAR.

**Why This Matters:**

- Hardcoded passwords in source code are a common audit finding
- The utility could inadvertently reveal password hash format and cost factor
- Should reside in `src/test/java` or be removed entirely

**Suggested Fix:**
Move the file to `src/test/java/com/ammons/taskactivity/util/` or delete it. If needed for operational use, make it a standalone CLI tool that is not bundled with the application.

---

#### Issue 6: Admin Default Password Comment in AWS Properties

**Severity:** 🟠 MEDIUM
**Location:** `src/main/resources/application-aws.properties` (line 83)
**Category:** Information Disclosure

**Description:**
The AWS properties file contains a comment revealing the default admin password pattern:

```properties
# - ADMIN_PASSWORD (default: ChangeMe123!)
```

While the base `application.properties` correctly requires `APP_ADMIN_INITIAL_PASSWORD` without a default, this comment could mislead operators into using a weak password.

**Why This Matters:**
Operators who read the properties file may use `ChangeMe123!` as the actual password. Comments revealing default credentials are a common attack vector.

**Suggested Fix:**
Remove the default password from the comment:

```properties
# - ADMIN_PASSWORD (REQUIRED - no default, must be set via environment variable)
```

---

#### Issue 7: GET-Based Logout Accepted

**Severity:** 🟠 MEDIUM
**Location:** `src/main/java/com/ammons/taskactivity/config/SecurityConfig.java` (line 381)
**Category:** CSRF on Logout

**Description:**
The logout endpoint accepts both GET and POST requests:

```java
.logoutRequestMatcher(request -> LOGOUT_URL.equals(request.getRequestURI())
    && ("POST".equalsIgnoreCase(request.getMethod())
        || "GET".equalsIgnoreCase(request.getMethod())))
```

GET-based logout is vulnerable to CSRF via image tags or link prefetching:

```html
<img src="https://target.com/logout" />
```

**Why This Matters:**
An attacker could force a user to log out by embedding the logout URL in any webpage the user visits.

**Suggested Fix:**
Restrict logout to POST-only for the web interface. For the Angular frontend, switch to POST-based logout with CSRF token. Keep the JWT `/api/auth/logout` (POST-only) for API clients:

```java
.logoutRequestMatcher(request -> LOGOUT_URL.equals(request.getRequestURI())
    && "POST".equalsIgnoreCase(request.getMethod()))
```

Update the Angular logout to use a form POST instead of a GET redirect.

---

#### Issue 8: CORS `allowedHeaders` Set to Wildcard

**Severity:** 🟠 MEDIUM
**Location:** `src/main/java/com/ammons/taskactivity/config/SecurityConfig.java` (line 556)
**Category:** CORS Misconfiguration

**Description:**
The CORS configuration allows all headers:

```java
configuration.setAllowedHeaders(List.of("*"));
```

**Why This Matters:**
While less severe than wildcard origins, allowing all headers means an attacker from an allowed origin could inject arbitrary headers, potentially exploiting header-based vulnerabilities in downstream proxies or load balancers.

**Suggested Fix:**
Restrict to the specific headers your application needs:

```java
configuration.setAllowedHeaders(List.of(
    "Authorization", "Content-Type", "X-XSRF-TOKEN",
    "Accept", "Origin", "X-Requested-With"
));
```

---

#### Issue 9: Rate Limit Bucket Cache Unbounded Growth

**Severity:** 🟠 MEDIUM
**Location:** `src/main/java/com/ammons/taskactivity/security/RateLimitFilter.java`
**Category:** Denial of Service

**Description:**
The `ConcurrentHashMap<String, Bucket> cache` stores one bucket per IP address and never evicts entries. Over time, or during a distributed attack from many IPs, this map grows unbounded.

**Why This Matters:**
An attacker could exhaust server memory by sending requests from many different IP addresses (or spoofed `CF-Connecting-IP` headers if not behind Cloudflare).

**Suggested Fix:**
Use a size-bounded cache with TTL eviction:

```java
// Option 1: Caffeine cache with size limit and TTL
private final Cache<String, Bucket> cache = Caffeine.newBuilder()
    .maximumSize(100_000)
    .expireAfterAccess(Duration.ofMinutes(10))
    .build();

// Option 2: Scheduled cleanup (simpler)
@Scheduled(fixedRate = 600000) // Every 10 minutes
public void cleanupBuckets() {
    cache.clear(); // Or implement smarter eviction
}
```

---

#### Issue 10: CSP Still Uses `unsafe-inline` for Scripts and Styles

**Severity:** 🟠 MEDIUM
**Location:** `src/main/java/com/ammons/taskactivity/config/SecurityConfig.java` (CSP directives)
**Category:** Cross-Site Scripting

**Description:**
The Content-Security-Policy includes `'unsafe-inline'` for both `script-src` and `style-src`:

```
script-src 'self' 'unsafe-inline' https://static.cloudflareinsights.com;
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
```

The codebase already has a TODO comment acknowledging this and references `CSP_Hardening_Implementation_Plan.md`.

**Why This Matters:**
`'unsafe-inline'` significantly weakens CSP protection against XSS attacks by allowing inline scripts to execute. This makes Issue 1 (credentials in sessionStorage) more exploitable.

**Suggested Fix:**
Implement nonce-based CSP as outlined in the existing hardening plan:

```
script-src 'self' 'nonce-{random}' https://static.cloudflareinsights.com;
style-src 'self' 'nonce-{random}' https://fonts.googleapis.com;
```

This requires generating a unique nonce per request and injecting it into allowed `<script>` and `<style>` tags.

---

#### Issue 11: React Axios Client Missing CSRF Token Handling

**Severity:** 🟠 MEDIUM
**Location:** `frontend-react/src/api/axios.client.ts`
**Category:** CSRF Protection Gap

**Description:**
The React Axios client sets `withCredentials: true` but does not include CSRF tokens in state-changing requests (POST, PUT, DELETE). The Angular frontend correctly reads the `XSRF-TOKEN` cookie and adds the `X-XSRF-TOKEN` header.

Since CSRF is disabled for `/api/**` endpoints (which use JWT), this only affects non-API endpoints that the React frontend might call. However, if the React dashboard ever makes session-authenticated requests, CSRF protection would be absent.

**Suggested Fix:**
Add CSRF token handling to the Axios request interceptor:

```typescript
apiClient.interceptors.request.use((config) => {
    if (["post", "put", "delete", "patch"].includes(config.method ?? "")) {
        const csrfToken = document.cookie
            .split("; ")
            .find((row) => row.startsWith("XSRF-TOKEN="))
            ?.split("=")[1];
        if (csrfToken) {
            config.headers["X-XSRF-TOKEN"] = decodeURIComponent(csrfToken);
        }
    }
    return config;
});
```

---

### LOW Severity

---

#### Issue 12: `@CrossOrigin(origins = "*")` on VisitorCounterController

**Severity:** 🟢 LOW
**Location:** `src/main/java/com/ammons/taskactivity/controller/VisitorCounterController.java` (line 22)
**Category:** CORS

**Description:**
The visitor counter controller uses `@CrossOrigin(origins = "*")`, allowing any origin to call these endpoints. While the endpoints are intentionally public and contain no sensitive data, the annotation overrides the application-level CORS configuration.

**Suggested Fix:**
Remove the `@CrossOrigin` annotation since the endpoints are already permitted via `requestMatchers("/api/public/**").permitAll()` and the application-level CORS config applies. If wildcard CORS is desired for these endpoints, document the rationale in a code comment.

---

#### Issue 13: JWT Expiration of 24 Hours

**Severity:** 🟢 LOW
**Location:** `src/main/resources/application.properties` (line 64)
**Category:** Token Lifetime

**Description:**
The default JWT access token expiration is 24 hours (`86400000` ms) and refresh tokens last 7 days (`604800000` ms). Industry best practice recommends shorter access token lifetimes (15-30 minutes) with refresh token rotation.

**Suggested Fix:**
Consider reducing the access token lifetime:

```properties
jwt.expiration=900000       # 15 minutes
jwt.refresh.expiration=86400000  # 24 hours
```

This reduces the window of exposure if a token is compromised.

---

#### Issue 14: `DataInitializer` Default Password Fallback

**Severity:** 🟢 LOW
**Location:** `src/main/java/com/ammons/taskactivity/config/DataInitializer.java` (line 48)
**Category:** Weak Default

**Description:**
The `DataInitializer` has a default password fallback of `admin123`:

```java
@Value("${app.admin.initial-password:admin123}")
private String adminPassword;
```

While the main `application.properties` requires `APP_ADMIN_INITIAL_PASSWORD` (no default), the `@Value` annotation includes a fallback that could be used if the profile-specific properties don't override it.

**Suggested Fix:**
Remove the fallback value to force explicit configuration:

```java
@Value("${app.admin.initial-password}")
private String adminPassword;
```

---

#### Issue 15: `img-src` CSP Directive Allows `https:`

**Severity:** 🟢 LOW
**Location:** `src/main/java/com/ammons/taskactivity/config/SecurityConfig.java` (second CSP block)
**Category:** Content Security Policy

**Description:**
One of the two CSP directives (the one inside the second `.headers()` block) uses `img-src 'self' data: https:` — this allows images from any HTTPS source, which could be used for tracking pixels or data exfiltration via image URLs.

**Suggested Fix:**
Restrict `img-src` to only the specific external domains needed:

```
img-src 'self' data:;
```

---

#### Issue 16: Duplicate Security Headers Configuration

**Severity:** 🟢 LOW
**Location:** `src/main/java/com/ammons/taskactivity/config/SecurityConfig.java`
**Category:** Code Maintenance

**Description:**
The `SecurityConfig` configures security headers in two separate places within the filter chain — once around line 155 and again around line 455. The second configuration overrides the first, and they have slightly different CSP directives (the second one includes `frame-ancestors 'none'` and broader `img-src`). This creates confusion about which policy is actually enforced.

**Suggested Fix:**
Consolidate into a single `.headers()` configuration block and remove the duplicate. Verify which CSP policy should be authoritative.

---

## Risk Summary Matrix

| #   | Issue                                              | Severity    | Category               | OWASP Top 10                                          |
| --- | -------------------------------------------------- | ----------- | ---------------------- | ----------------------------------------------------- |
| 1   | Base64 credentials in sessionStorage               | 🔴 CRITICAL | Credential Exposure    | A07:2021 — Identification and Authentication Failures |
| 2   | Admin SQL query — injection risk                   | 🔴 HIGH     | SQL Injection          | A03:2021 — Injection                                  |
| 3   | Console logging of auth data                       | 🟡 HIGH     | Information Disclosure | A09:2021 — Security Logging and Monitoring Failures   |
| 4   | In-memory token revocation                         | 🟠 MEDIUM   | Auth Bypass            | A07:2021 — Identification and Authentication Failures |
| 5   | PasswordHashGenerator in prod source               | 🟠 MEDIUM   | Code Hygiene           | A05:2021 — Security Misconfiguration                  |
| 6   | Default password in AWS comments                   | 🟠 MEDIUM   | Information Disclosure | A05:2021 — Security Misconfiguration                  |
| 7   | GET-based logout (CSRF risk)                       | 🟠 MEDIUM   | CSRF                   | A01:2021 — Broken Access Control                      |
| 8   | CORS wildcard allowedHeaders                       | 🟠 MEDIUM   | CORS Misconfiguration  | A05:2021 — Security Misconfiguration                  |
| 9   | Rate limit cache unbounded growth                  | 🟠 MEDIUM   | Denial of Service      | A05:2021 — Security Misconfiguration                  |
| 10  | CSP `unsafe-inline` for scripts                    | 🟠 MEDIUM   | XSS                    | A03:2021 — Injection                                  |
| 11  | React missing CSRF tokens                          | 🟠 MEDIUM   | CSRF                   | A01:2021 — Broken Access Control                      |
| 12  | `@CrossOrigin(origins = "*")` on public controller | 🟢 LOW      | CORS                   | A05:2021 — Security Misconfiguration                  |
| 13  | JWT 24-hour expiration                             | 🟢 LOW      | Token Lifetime         | A07:2021 — Identification and Authentication Failures |
| 14  | DataInitializer default password fallback          | 🟢 LOW      | Weak Default           | A07:2021 — Identification and Authentication Failures |
| 15  | `img-src https:` in CSP                            | 🟢 LOW      | CSP                    | A05:2021 — Security Misconfiguration                  |
| 16  | Duplicate security headers config                  | 🟢 LOW      | Code Maintenance       | A05:2021 — Security Misconfiguration                  |

---

## Recommendations Priority

### Immediate (Sprint 1)

1. **Remove credential storage from Angular sessionStorage** (Issue 1)
2. **Implement read-only database user for admin query endpoint** (Issue 2)
3. **Remove console.log statements from Angular production code** (Issue 3)

### Short-Term (Sprint 2-3)

4. Implement persistent token revocation (database or Redis) (Issue 4)
5. Move PasswordHashGenerator to test sources (Issue 5)
6. Remove default password from AWS properties comments (Issue 6)
7. Restrict logout to POST-only (Issue 7)
8. Restrict CORS allowed headers (Issue 8)
9. Implement rate limit cache eviction (Issue 9)

### Medium-Term (Sprint 4+)

10. Implement nonce-based CSP (Issue 10)
11. Add CSRF handling to React Axios client (Issue 11)
12. Reduce JWT access token lifetime (Issue 13)
13. Consolidate duplicate header configuration (Issue 16)

---

_This review was conducted as a static analysis of the codebase. Dynamic penetration testing is recommended to validate findings and uncover runtime-specific vulnerabilities._
