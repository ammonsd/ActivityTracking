<!--
/**
 * Description: Consolidated evaluation of current security controls, verification evidence,
 *              and outstanding risks for the Activity Tracking platform.
 *
 * Author: Dean Ammons
 * Date: February 2026
 */
-->

# Security Evaluation – February 27, 2026

## Overview

This document summarizes the security posture of the Activity Tracking platform following the
latest round of remediation work (refresh-token hardening, login-audit lockdown, and
task-deletion enforcement). It captures the controls currently in effect, highlights the evidence
collected during this evaluation, and enumerates open issues that require follow-up while manual
testing continues.

## Evaluation Scope & Inputs

- **Source Review:** `ApiAuthController`, `UserRestController`, `TaskActivityService`,
  security guidance in `docs/Security_Measures_and_Best_Practices.md`, and supporting services
  (permission, revocation, dropdown access).
- **Automated Evidence:** `mvnw.cmd clean test -DskipFrontend` (405 tests, 0 failures, 0 errors,
  23 skipped) executed on 2026-02-27 15:18:18 -0500.
- **Recent Changes:**
    - Refresh endpoint now validates token intent, revocation status, and password-change cutoffs.
    - Logout flow optionally revokes refresh tokens via `X-Refresh-Token` header.
    - Login audit endpoint restricted to admins or the requesting user, with page-size enforcement.
    - Task deletions centralized in the service layer with ownership checks and unit coverage.

## Control Summary

| Domain                     | Key Controls                                                                                                           | Status              | Verification Evidence                                                                        |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------- | ------------------- | -------------------------------------------------------------------------------------------- |
| Authentication & Session   | JWT access/refresh tokens, server-side revocation, password expiry & history, account lockout, CSRF for stateful flows | **Working**         | Unit/service tests, refresh controller inspection, `Security_Measures_and_Best_Practices.md` |
| Authorization & RBAC       | Database-driven roles, `@RequirePermission` annotation, login-audit guardrails, task-deletion ownership checks         | **Working**         | PermissionService tests, controller review, recent enforcement unit tests                    |
| API & Application Security | Rate limiting (Bucket4j), security headers (CSP, HSTS), input validation, file-type magic-number checks                | **Working**         | `Security_Measures_and_Best_Practices.md`, FileTypeValidator tests                           |
| Data Protection            | BCrypt password hashing, password history table, S3/local receipt storage abstraction, HTTPS/TLS requirements          | **Working**         | Service review, documentation, configuration guidelines                                      |
| Monitoring & Logging       | Login audit trail, permission-check logging, token revocation logging, user-service audit logs                         | **Working**         | Controller/service source review, Maven test output                                          |
| Pending / In Progress      | Manual verification of new token/logout behaviors, regression of login-audit UI, periodic secret rotation automation   | **Needs Follow-up** | Manual tests not yet run; see Recommendations                                                |

## Detailed Findings

### 1. Authentication & Session Management

- **JWT Flows:** Refresh tokens now undergo explicit token-type validation, JTI blacklist checks,
  and password-change cutoff comparison before new access tokens are issued. Logout supports
  optional refresh-token revocation, closing a previous reuse window.
- **Password Hygiene:** 90-day expiration, password history (5 entries), forced update flag, and
  BCrypt hashing remain in force. Account lockout triggers after five failed attempts and notifies
  administrators.
- **Session Controls:** HttpOnly/Secure cookies, SameSite enforcement, CSRF tokens for form-based
  flows, and session fixation protection remain aligned with documented standards.
- **Residual Risk:** Manual regression is still required to confirm the new refresh/logout flows on
  deployed environments and to ensure operational dashboards capture revocation failures.

### 2. Authorization & Access Control

- **RBAC & Permissions:** Runtime permission checks via `@RequirePermission` and the permission
  aspect prevent unauthorized API usage. Role definitions and permission assignments are stored in
  the database, enabling dynamic updates without redeployments.
- **Ownership Enforcement:** The `TaskActivityService.deleteTaskActivity` method now enforces
  ownership checks for non-admin users, and both REST/MVC controllers call this centralized logic.
  Corresponding unit tests cover admin deletion, owner deletion, cross-user denial, and not-found
  scenarios.
- **Audit Visibility:** `/api/users/login-audit` now limits data exposure to admins or the
  requesting user and clamps page sizes (1–200). Unauthorized attempts are logged.
- **Residual Risk:** Confirm that React/Angular clients gracefully handle the new 403 responses and
  that UI-level caching does not expose stale audit data.

### 3. Data Protection & Storage

- **Secrets Management:** JWT secrets and administrator bootstrap passwords are sourced from
  environment variables; instructions mandate 256-bit randomness. Database credentials are likewise
  externalized.
- **Receipts & File Uploads:** Both S3 and local storage flows rely on the `ReceiptStorageService`
  abstraction. Uploads undergo magic-number validation (JPEG, PNG, PDF) to thwart extension
  spoofing; unit tests validate negative cases (e.g., malware signatures, truncated files).
- **Encryption:** TLS is required in production (AWS ALB / CloudFront) with HSTS enabled; S3 buckets
  rely on server-side encryption per AWS policy documents.
- **Residual Risk:** For local deployments, ensure file-system permissions restrict access to the
  receipts directory and audit for orphaned files when records are removed.

### 4. Application & API Hardening

- **Rate Limiting:** Bucket4j token buckets shield authentication endpoints from brute force and
  throttle API misuse. Responses include rate-limit headers, and configuration can be tuned per
  environment.
- **Security Headers:** CSP (`default-src 'self'`), X-Frame-Options (`DENY`), X-Content-Type-Options
  (`nosniff`), Referrer-Policy (`no-referrer`), and Permissions-Policy (disables sensors/camera) are
  applied globally.
- **Input Validation:** DTOs leverage Bean Validation annotations, and controllers centralize error
  responses. SQL queries rely on Spring Data JPA, limiting injection risk.
- **Residual Risk:** The CSP policy should be re-evaluated when introducing third-party analytics or
  charting libraries to avoid over-broad allowances.

### 5. Monitoring, Logging, and Incident Response

- **Login Auditing:** User login events (success/failure) are persisted and now shielded via the
  tightened endpoint. Review dashboards should ensure only admins can visualize aggregate data.
- **Token Revocation Logging:** ApiAuthController logs rejected refresh attempts (invalid type,
  revoked JTI, password-cutoff) to aid intrusion detection.
- **User Service Logging:** Password changes, account creations, and lockouts generate INFO/WARN
  logs for SIEM ingestion.
- **Residual Risk:** Consider forwarding token/logging events to a centralized monitoring stack (e.g.,
  CloudWatch, ELK) for alerting; current setup assumes local logs are inspected manually.

## Pending Issues & Recommendations

1. **Manual Regression:** Execute Postman or automated API flows to confirm the new refresh/logout
   logic behaves correctly in deployed environments and that the optional `X-Refresh-Token` header
   is handled by clients.
2. **UI Validation:** Verify both Angular and Thymeleaf login-audit views properly scope results and
   surface 403 errors with actionable messaging.
3. **Secrets Rotation:** Schedule routine rotation for `JWT_SECRET`, database credentials, and SES
   SMTP credentials; document the operational playbook (CloudFormation / ECS updates).
4. **Centralized Monitoring:** Integrate token revocation and permission-denial logs with a
   monitoring platform to enable alerting on anomalous spikes (potential attacks).
5. **Receipt Retention Review:** Audit receipts stored on local disk/S3 to ensure retention policies
   match compliance requirements and that deletes cascade to storage objects.

## Verification Evidence

- `mvnw.cmd clean test -DskipFrontend` – 405 tests run, 0 failures, 0 errors, 23 skipped
  (completed 2026-02-27 15:18:18 -0500) covering JWT utilities, permission checks, task activity
  workflows, file validators, and user service flows.
- Updated unit coverage specifically for task deletions (admin vs. owner vs. unauthorized) ensures
  the new authorization logic is exercised.
- Manual verification pending: refresh/logout API exercises, login-audit UI workflows, and secret
  rotation drills (see recommendations above).

## Conclusion

The Activity Tracking platform currently maintains strong layered defenses across authentication,
authorization, data protection, and monitoring. The latest fixes close previously identified gaps
(refresh token reuse, login-audit overexposure, and task deletion without ownership checks). The
remaining action items primarily concern manual verification, operational hardening (secret
rotation, centralized monitoring), and periodic policy reviews.
