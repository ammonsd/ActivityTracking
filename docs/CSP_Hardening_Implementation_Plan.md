# CSP Hardening Implementation Plan

**Project:** ActivityTracking Application  
**Feature:** Remove 'unsafe-inline' and Implement Nonce-Based Content Security Policy  
**Estimated Effort:** 5-7 days  
**Priority:** Medium (Security Enhancement)  
**Status:** Phase 1 Complete (unsafe-eval removed 2026-02-11)  

---

## Executive Summary

This document outlines the complete implementation plan for hardening the Content Security Policy (CSP) by removing `'unsafe-inline'` directives and implementing nonce-based CSP. This enhancement will significantly improve XSS protection by preventing inline script execution while maintaining full application functionality.

**Current State:**
```
script-src 'self' 'unsafe-inline' https://static.cloudflareinsights.com
```

**Target State:**
```
script-src 'self' 'nonce-{RANDOM}' https://static.cloudflareinsights.com
```

---

## Table of Contents

1. [Background](#background)
2. [Security Benefits](#security-benefits)
3. [Current Analysis](#current-analysis)
4. [Implementation Phases](#implementation-phases)
5. [Technical Approach](#technical-approach)
6. [File-by-File Migration Guide](#file-by-file-migration-guide)
7. [Testing Strategy](#testing-strategy)
8. [Rollback Plan](#rollback-plan)
9. [Post-Implementation](#post-implementation)

---

## Background

### What is 'unsafe-inline'?

The `'unsafe-inline'` CSP directive allows:
- Inline `<script>` tags with JavaScript code
- Inline event handlers (`onclick`, `onchange`, etc.)
- `javascript:` URLs

This significantly weakens XSS protection because any injected script, even malicious ones, will execute.

### Why Remove It?

**Current Risk:** If an attacker finds an injection point (e.g., reflected XSS), they can execute arbitrary JavaScript despite having CSP enabled.

**After Hardening:** Only scripts with valid nonces (cryptographic tokens) can execute, blocking nearly all XSS attacks.

---

## Security Benefits

✅ **XSS Protection:** Prevents execution of injected inline scripts  
✅ **Defense in Depth:** Adds layer even if input validation fails  
✅ **Compliance:** Meets strict security standards (PCI-DSS, SOC2)  
✅ **Audit Trail:** CSP violation reports help detect attacks  
✅ **Zero Trust:** Only explicitly allowed scripts execute  

**Risk Reduction:** High → Negligible for inline script injection attacks

---

## Current Analysis

### Phase 1: unsafe-eval Removal ✅ COMPLETE

**Status:** Completed 2026-02-11  
**Result:** `'unsafe-eval'` removed from both security configurations  
**Impact:** Zero - No eval(), Function(), or string-based setTimeout/setInterval found

### Phase 2: unsafe-inline Removal (This Plan)

**Affected Resources:**

#### Inline Script Blocks: 59 instances across 26 templates
- **Main Templates (11 files):**
  - task-list.html (3 blocks)
  - task-detail.html (3 blocks)
  - task-activity-form.html (3 blocks)
  - expense-list.html (4 blocks)
  - expense-detail.html (3 blocks)
  - expense-sheet.html (3 blocks)
  - expense-form.html (2 blocks)
  - weekly-timesheet.html (3 blocks)
  - change-password.html (2 blocks)
  - login.html (1 block)
  - reset-password.html (1 block)

- **Admin Templates (13 files):**
  - user-management.html (2 blocks)
  - user-add.html (2 blocks)
  - user-edit.html (1 block)
  - user-change-password.html (2 blocks)
  - role-management.html (1 block)
  - role-add.html (1 block)
  - role-edit.html (1 block)
  - client-management.html (3 blocks)
  - project-management.html (3 blocks)
  - phase-management.html (3 blocks)
  - dropdown-management.html (3 blocks)
  - dropdown-edit.html (1 block)
  - guest-activity.html (2 blocks)

- **Other Templates (2 files):**
  - dropdown-management-simple.html (3 blocks)
  - dropdown-category-management.html (2 blocks)
  - access-denied.html (1 block)

#### Inline Event Handlers: ~100+ instances
- `onclick=` - ~50 instances (buttons, links, pagination)
- `onchange=` - ~20 instances (select dropdowns, checkboxes)
- `onsubmit=` - ~10 instances (forms)
- `onload=` - ~5 instances (page initialization)
- Thymeleaf-generated `th:onclick=` - ~20 instances (GUEST role restrictions)

#### Existing External JS Files: 5
- `/js/date-utils.js` - Date formatting utilities
- `/js/form-utils.js` - Form validation helpers
- `/js/modal-utils.js` - Modal dialog functions
- `/js/password-toggle.js` - Password visibility toggle
- `/js/utc-time-converter.js` - UTC/local time conversion

---

## Implementation Phases

### Phase 1: Infrastructure (Day 1) - 8 hours

**Goal:** Create nonce generation and injection infrastructure

#### 1.1 Create CSP Nonce Filter
**File:** `src/main/java/com/ammons/taskactivity/security/CspNonceFilter.java`

```java
package com.ammons.taskactivity.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates and injects CSP nonce for each request.
 * Nonce is stored in request attribute for Thymeleaf access.
 */
@Component
public class CspNonceFilter implements Filter {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final String CSP_NONCE_ATTRIBUTE = "cspNonce";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        
        // Generate 128-bit cryptographic nonce
        byte[] nonceBytes = new byte[16];
        RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getEncoder().encodeToString(nonceBytes);
        
        // Store in request for Thymeleaf access
        request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
        
        // Add to CSP header (handled by SecurityConfig)
        ((HttpServletRequest) request).setAttribute("cspNonce", nonce);
        
        chain.doFilter(request, response);
    }
}
```

#### 1.2 Update SecurityConfig.java
**File:** `src/main/java/com/ammons/taskactivity/config/SecurityConfig.java`

Update CSP configuration to use nonce:

```java
.contentSecurityPolicy(csp -> csp.policyDirectives(
    "default-src 'self'; " +
    "script-src 'self' 'nonce-" + request.getAttribute("cspNonce") + "' https://static.cloudflareinsights.com; " +
    // ... rest of CSP
))
```

**Challenge:** SecurityConfig is static, need dynamic header injection.

**Solution:** Use custom CSP header writer:

```java
@Bean
public HeaderWriter cspHeaderWriter() {
    return (request, response) -> {
        String nonce = (String) request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
        if (nonce != null) {
            String cspPolicy = String.format(
                "default-src 'self'; " +
                "script-src 'self' 'nonce-%s' https://static.cloudflareinsights.com; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data: https://fonts.gstatic.com; " +
                "connect-src 'self' https://cloudflareinsights.com; " +
                "frame-ancestors 'none'",
                nonce
            );
            response.setHeader("Content-Security-Policy", cspPolicy);
        }
    };
}
```

#### 1.3 Create Thymeleaf Dialect for Nonce Injection
**File:** `src/main/java/com/ammons/taskactivity/config/CspNonceDialect.java`

```java
package com.ammons.taskactivity.config;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;
import org.springframework.stereotype.Component;
import java.util.Set;

/**
 * Thymeleaf dialect to automatically add nonce to script tags.
 */
@Component
public class CspNonceDialect extends AbstractProcessorDialect {
    
    public CspNonceDialect() {
        super("CSP Nonce Dialect", "csp", StandardDialect.PROCESSOR_PRECEDENCE);
    }
    
    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new ScriptNonceProcessor(dialectPrefix));
    }
}
```

#### 1.4 Testing Infrastructure
- Unit tests for nonce generation (uniqueness, length, encoding)
- Integration tests for nonce injection in requests
- Verify CSP header contains correct nonce value

**Deliverables:**
- ✅ CspNonceFilter.java
- ✅ Custom CSP header writer
- ✅ CspNonceDialect.java (optional, for auto-injection)
- ✅ Unit tests

---

### Phase 2: High-Priority Templates (Days 2-3) - 16 hours

**Goal:** Migrate 8 most-used templates

#### Priority Order (by user traffic/criticality):

1. **login.html** (Entry point)
2. **task-list.html** (Primary interface)
3. **task-detail.html** (High usage)
4. **task-activity-form.html** (Data entry)
5. **expense-list.html** (Finance tracking)
6. **expense-detail.html** (Finance review)
7. **user-management.html** (Admin critical)
8. **weekly-timesheet.html** (Reporting)

#### Migration Pattern for Each Template:

**Step 1: Extract Inline Scripts**
```html
<!-- BEFORE: Inline script in template -->
<script>
    const userId = /*[[${user.id}]]*/ 0;
    function deleteUser() {
        if (confirm('Delete user ' + userId + '?')) {
            document.getElementById('deleteForm').submit();
        }
    }
</script>

<!-- AFTER: External script with nonce -->
<script th:attr="nonce=${cspNonce}" src="/js/user-management.js"></script>
```

**Step 2: Pass Data via data-* Attributes**
```html
<!-- Store dynamic values in HTML -->
<form id="deleteForm" 
      data-user-id="[[${user.id}]]" 
      data-user-name="[[${user.username}]]"
      th:action="@{/admin/users/__${user.id}__/delete}" 
      method="post">
</form>
```

**Step 3: Read Data in External JS**
```javascript
// user-management.js
document.addEventListener('DOMContentLoaded', () => {
    const deleteForm = document.getElementById('deleteForm');
    if (deleteForm) {
        const userId = deleteForm.dataset.userId;
        const userName = deleteForm.dataset.userName;
        
        // Rest of logic...
    }
});
```

**Step 4: Convert Inline Event Handlers**
```html
<!-- BEFORE -->
<button onclick="deleteUser()">Delete</button>

<!-- AFTER -->
<button class="delete-user-btn" data-user-id="[[${user.id}]]">Delete</button>
```

```javascript
// JS file
document.querySelectorAll('.delete-user-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
        const userId = e.target.dataset.userId;
        deleteUser(userId);
    });
});
```

#### New JS Files to Create (Phase 2):

1. `/js/login.js` - Login page functionality
2. `/js/task-list.js` - Task list interactions (sidebar, filters, pagination, CSV)
3. `/js/task-detail.js` - Task detail page (edit, delete modal, hours stepper)
4. `/js/task-form.js` - Task form validation and dropdowns
5. `/js/expense-list.js` - Expense list interactions
6. `/js/expense-detail.js` - Expense detail with receipt viewer
7. `/js/user-management.js` - Admin user management
8. `/js/timesheet.js` - Weekly timesheet functionality

**Deliverables (Phase 2):**
- ✅ 8 migrated templates
- ✅ 8 new external JS files
- ✅ Removal of ~25 inline script blocks
- ✅ Conversion of ~40 inline event handlers
- ✅ Functional testing of all 8 pages

---

### Phase 3: Remaining Templates (Day 4) - 8 hours

**Goal:** Migrate remaining 18 templates

#### Batch 1: Admin Templates (8 files)
- user-add.html, user-edit.html, user-change-password.html
- role-management.html, role-add.html, role-edit.html
- client-management.html, project-management.html

#### Batch 2: Management Templates (6 files)
- phase-management.html
- dropdown-management.html, dropdown-edit.html
- dropdown-management-simple.html, dropdown-category-management.html
- guest-activity.html

#### Batch 3: Auxiliary Pages (4 files)
- expense-sheet.html, expense-form.html
- change-password.html, reset-password.html
- access-denied.html

#### New JS Files (Phase 3):

9. `/js/user-form.js` - User add/edit forms
10. `/js/role-management.js` - Role management
11. `/js/dropdown-management.js` - Dropdown value management
12. `/js/expense-form.js` - Expense form validation
13. `/js/password-change.js` - Password change functionality (replace inline)
14. `/js/guest-activity.js` - Guest activity tracking

**Deliverables (Phase 3):**
- ✅ 18 migrated templates
- ✅ 6 new external JS files
- ✅ Removal of ~34 inline script blocks
- ✅ Conversion of ~60 inline event handlers

---

### Phase 4: Testing & Validation (Day 5) - 8 hours

**Goal:** Comprehensive testing and bug fixes

#### 4.1 Browser Testing
- Chrome/Edge (latest)
- Firefox (latest)
- Safari (if applicable)

#### 4.2 Functional Testing Checklist

**Core Functionality:**
- [ ] Login/logout
- [ ] Task CRUD operations
- [ ] Expense CRUD operations
- [ ] User management (create, edit, delete)
- [ ] Role management
- [ ] Dropdown management
- [ ] Weekly timesheet generation
- [ ] CSV export/import
- [ ] Modal dialogs (delete confirmations)
- [ ] Form validation
- [ ] Pagination
- [ ] Sidebar navigation
- [ ] Password change/reset

**JavaScript Features:**
- [ ] Event listeners working (no onclick errors)
- [ ] Form submissions
- [ ] AJAX calls (if any)
- [ ] Date pickers and dropdowns
- [ ] Hours stepper (+0.25/-0.25)
- [ ] CSV download modal
- [ ] Guest role restrictions
- [ ] Password visibility toggle

#### 4.3 CSP Violation Monitoring

**Enable CSP Reporting:**
```java
.contentSecurityPolicy(csp -> csp.policyDirectives(
    "default-src 'self'; " +
    "script-src 'self' 'nonce-{NONCE}' https://static.cloudflareinsights.com; " +
    "... " +
    "report-uri /csp-violation-report"
))
```

Create endpoint to log violations:
```java
@PostMapping("/csp-violation-report")
@ResponseBody
public ResponseEntity<Void> handleCspViolation(@RequestBody String report) {
    logger.warn("CSP Violation: {}", report);
    return ResponseEntity.ok().build();
}
```

**DevTools Monitoring:**
- Open Chrome DevTools → Console
- Look for CSP violation warnings
- Fix any remaining inline scripts/handlers

#### 4.4 Performance Testing
- Measure page load times before/after
- Verify no regression in performance
- External JS files should be cached (304 responses)

**Deliverables (Phase 4):**
- ✅ All tests passing
- ✅ Zero CSP violations in DevTools
- ✅ Bug fixes for any issues found
- ✅ Performance validation

---

### Phase 5: Deployment & Monitoring (Days 6-7) - 8 hours

#### 5.1 Staged Rollout

**Stage 1: Dev Environment**
- Deploy with nonce-based CSP
- Test all functionality
- Monitor logs for errors

**Stage 2: Test/QA Environment**
- Deploy and soak test for 24 hours
- QA team full regression testing
- Monitor CSP violation reports

**Stage 3: Production Deployment**
- Deploy during low-traffic window
- Monitor application logs
- Watch for increased error rates
- Have rollback plan ready

#### 5.2 Monitoring Plan

**Metrics to Track:**
- CSP violation count (should be zero after fixes)
- JavaScript error rate
- Page load time (should be unchanged)
- User-reported issues

**Logging:**
```java
logger.info("CSP nonce generated for request: {}", nonce);
logger.warn("CSP violation detected: {}", violationDetails);
```

#### 5.3 Documentation Updates

Files to update:
- **Developer_Guide.md** - Add section on CSP nonces
- **Security_Measures_and_Best_Practices.md** - Update CSP section
- **Security_Audit_Report_2026-02-11.md** - Mark as IMPLEMENTED
- **README.md** - Note CSP hardening completion

**Deliverables (Phase 5):**
- ✅ Production deployment
- ✅ 48-hour monitoring period
- ✅ Documentation updated
- ✅ Team training on nonce usage

---

## Technical Approach

### Handling Thymeleaf Variables

**Challenge:** Inline scripts often use Thymeleaf expressions:
```javascript
const userId = /*[[${user.id}]]*/ 0;
const isGuest = /*[[${#authorization.expression('hasRole(''GUEST'')')}]]*/ false;
```

**Solution 1: Data Attributes (Recommended)**
```html
<div id="userContext" 
     data-user-id="[[${user.id}]]"
     data-is-guest="[[${#authorization.expression('hasRole(''GUEST'')')}]]">
</div>
```

```javascript
const userContext = document.getElementById('userContext');
const userId = parseInt(userContext.dataset.userId);
const isGuest = userContext.dataset.isGuest === 'true';
```

**Solution 2: JSON Configuration Object**
```html
<script th:attr="nonce=${cspNonce}" type="application/json" id="pageConfig">
{
    "userId": [[${user.id}]],
    "isGuest": [[${#authorization.expression('hasRole(''GUEST'')')}]],
    "csrfToken": "[[${_csrf.token}]]"
}
</script>
```

```javascript
const config = JSON.parse(document.getElementById('pageConfig').textContent);
const userId = config.userId;
```

### Event Handler Patterns

**Pattern 1: Class-Based Selectors**
```javascript
// Single element
document.querySelector('.delete-btn')?.addEventListener('click', handleDelete);

// Multiple elements
document.querySelectorAll('.delete-btn').forEach(btn => {
    btn.addEventListener('click', handleDelete);
});
```

**Pattern 2: Event Delegation** (for dynamic content)
```javascript
document.addEventListener('click', (e) => {
    if (e.target.matches('.delete-btn')) {
        handleDelete(e);
    }
});
```

**Pattern 3: Form-Based**
```javascript
document.getElementById('myForm')?.addEventListener('submit', (e) => {
    e.preventDefault();
    // validation logic
});
```

### Nonce Injection in Templates

**Automatic (Recommended):**
```html
<script th:attr="nonce=${cspNonce}" src="/js/my-script.js"></script>
```

**Manual (if needed):**
```html
<script th:nonce="${cspNonce}" src="/js/my-script.js"></script>
```

### CSRF Token Handling

External JS files need CSRF tokens for POST requests:

**Option 1: Meta Tag**
```html
<meta name="csrf-token" th:content="${_csrf.token}">
<meta name="csrf-header" th:content="${_csrf.headerName}">
```

```javascript
const token = document.querySelector('meta[name="csrf-token"]').content;
const header = document.querySelector('meta[name="csrf-header"]').content;
```

**Option 2: Data Attribute on Form**
```html
<form data-csrf-token="[[${_csrf.token}]]" data-csrf-header="[[${_csrf.headerName}]]">
```

---

## File-by-File Migration Guide

### Example: task-list.html

#### Current Inline Scripts (3 blocks):
1. **Block 1:** Sidebar toggle, CSV modal, pagination
2. **Block 2:** GUEST role message
3. **Block 3:** Cloudflare Insights initialization

#### Migration Steps:

**Step 1: Create /js/task-list.js**
```javascript
/**
 * Task List Page Functionality
 * 
 * Author: Dean Ammons
 * Date: February 2026
 */

// Sidebar Toggle
function toggleSidebar(event) {
    event?.preventDefault();
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('overlay');
    
    if (sidebar && overlay) {
        sidebar.classList.toggle('active');
        overlay.classList.toggle('active');
    }
    return false;
}

// CSV Export Modal
function showCsvModal() {
    const modal = document.getElementById('csvModal');
    if (modal) {
        modal.style.display = 'block';
    }
}

function closeCsvModal() {
    const modal = document.getElementById('csvModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

// Initialize page
document.addEventListener('DOMContentLoaded', () => {
    // Sidebar toggle buttons
    document.getElementById('sidebarToggle')?.addEventListener('click', toggleSidebar);
    document.querySelector('.sidebar-close')?.addEventListener('click', toggleSidebar);
    document.getElementById('overlay')?.addEventListener('click', toggleSidebar);
    
    // CSV modal buttons
    document.getElementById('downloadCsvBtn')?.addEventListener('click', showCsvModal);
    document.getElementById('closeCsvBtn')?.addEventListener('click', closeCsvModal);
    
    // GUEST role message
    const isGuest = document.body.dataset.isGuest === 'true';
    if (isGuest) {
        console.info('Running in GUEST mode - some features restricted');
    }
});
```

**Step 2: Update task-list.html Template**

Remove inline `<script>` blocks, add data attributes, reference external JS:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" 
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
      th:attr="data-is-guest=${#authorization.expression('hasRole(''GUEST'')')}">
<head>
    <title>Task Activity List</title>
    <!-- ... other head content ... -->
</head>
<body>
    <!-- Remove inline scripts, keep only external JS with nonce -->
    <script th:attr="nonce=${cspNonce}" src="/js/task-list.js"></script>
    <script th:attr="nonce=${cspNonce}" 
            src="https://static.cloudflareinsights.com/..." 
            defer></script>
</body>
</html>
```

**Step 3: Convert onclick Handlers**

```html
<!-- BEFORE -->
<button class="sidebar-toggle" 
        onclick="return toggleSidebar(event);" 
        id="sidebarToggle">Menu</button>

<!-- AFTER -->
<button class="sidebar-toggle" id="sidebarToggle">Menu</button>
```

Event listener added in task-list.js (see above).

**Step 4: Test**
- Verify sidebar toggle works
- Verify CSV modal opens/closes
- Verify pagination works
- Check DevTools console for CSP violations (should be zero)

---

### Example: user-management.html

#### Current Inline Scripts (2 blocks):
1. User deletion modal and confirmation
2. Page initialization with Thymeleaf variables

#### Migration Steps:

**Step 1: Create /js/user-management.js**
```javascript
/**
 * User Management Page
 */

let deleteUserId = null;
let deleteUsername = null;

function showDeleteModal(userId, username) {
    deleteUserId = userId;
    deleteUsername = username;
    
    document.getElementById('deleteUserName').textContent = username;
    document.getElementById('deleteModal').style.display = 'block';
}

function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
    deleteUserId = null;
    deleteUsername = null;
}

function confirmDelete() {
    if (deleteUserId) {
        const form = document.getElementById('deleteForm');
        form.action = `/admin/users/${deleteUserId}/delete`;
        form.submit();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    // Delete button click handlers
    document.querySelectorAll('.delete-user-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const userId = e.target.dataset.userId;
            const username = e.target.dataset.username;
            showDeleteModal(userId, username);
        });
    });
    
    // Modal close buttons
    document.querySelector('.close-modal')?.addEventListener('click', closeDeleteModal);
    document.querySelector('.cancel-delete')?.addEventListener('click', closeDeleteModal);
    
    // Confirm delete button
    document.querySelector('.confirm-delete')?.addEventListener('click', confirmDelete);
});
```

**Step 2: Update user-management.html**

```html
<!-- User table row -->
<tr th:each="user : ${users}">
    <td th:text="${user.username}"></td>
    <td>
        <button class="delete-user-btn btn-danger"
                th:attr="data-user-id=${user.id},data-username=${user.username}">
            Delete
        </button>
    </td>
</tr>

<!-- External JS with nonce -->
<script th:attr="nonce=${cspNonce}" src="/js/user-management.js"></script>
```

---

## Testing Strategy

### Unit Testing

**Test: Nonce Generation**
```java
@Test
void testNonceGeneration() {
    Set<String> nonces = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
        String nonce = generateNonce();
        assertNotNull(nonce);
        assertEquals(24, nonce.length()); // Base64 of 16 bytes
        assertTrue(nonces.add(nonce)); // Ensure uniqueness
    }
}
```

**Test: Nonce in Request Attribute**
```java
@Test
void testNonceFilterAddsAttribute() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    
    nonceFilter.doFilter(request, response, chain);
    
    String nonce = (String) request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
    assertNotNull(nonce);
}
```

### Integration Testing

**Test: CSP Header Contains Nonce**
```java
@Test
void testCspHeaderContainsNonce() throws Exception {
    mockMvc.perform(get("/task-activity/list"))
        .andExpect(status().isOk())
        .andExpect(header().exists("Content-Security-Policy"))
        .andExpect(header().string("Content-Security-Policy", 
            containsString("'nonce-")));
}
```

**Test: Script Tag Has Nonce Attribute**
```java
@Test
void testScriptTagHasNonce() throws Exception {
    MvcResult result = mockMvc.perform(get("/task-activity/list"))
        .andExpect(status().isOk())
        .andReturn();
    
    String content = result.getResponse().getContentAsString();
    assertTrue(content.contains("<script nonce="));
}
```

### Browser Testing

**Manual Test Cases:**

1. **Functional Testing:**
   - [ ] All pages load without errors
   - [ ] All JavaScript features work
   - [ ] Forms submit correctly
   - [ ] Modals open and close
   - [ ] AJAX calls succeed
   - [ ] Event handlers fire

2. **CSP Validation:**
   - [ ] Open Chrome DevTools → Console
   - [ ] No CSP violation warnings
   - [ ] Check Network tab → Response headers contain correct CSP
   - [ ] Verify nonce value matches in header and script tags

3. **Regression Testing:**
   - [ ] Compare behavior before/after migration
   - [ ] No broken functionality
   - [ ] No visual regressions

### Load Testing

Before and after metrics:
- Page load time
- JavaScript execution time
- Number of HTTP requests
- Total page size

**Expected Results:**
- Slightly faster (fewer inline scripts to parse)
- Cacheable external JS files (304 responses)
- No performance regression

---

## Rollback Plan

### If Issues Discovered in Testing

**Quick Rollback:**
1. Revert SecurityConfig.java CSP changes
2. Re-enable `'unsafe-inline'`
3. Templates still work (external JS is additive)

### If Issues Discovered in Production

**Emergency Rollback Steps:**

1. **Immediate:** Re-enable 'unsafe-inline' in CSP header
   ```java
   script-src 'self' 'unsafe-inline' 'nonce-{NONCE}' https://...
   ```
   This allows both old and new patterns to work.

2. **Within 1 hour:** Revert code changes via git
   ```bash
   git revert <commit-hash>
   git push origin main
   ```

3. **Redeploy:** Use CI/CD pipeline or manual deployment

4. **Monitor:** Verify application functionality restored

### Gradual Rollout (Recommended)

Instead of all-or-nothing, use feature flag:

```java
@Value("${security.csp.nonce-enabled:false}")
private boolean nonceEnabled;

String scriptSrc = nonceEnabled 
    ? "'self' 'nonce-' + nonce 
    : "'self' 'unsafe-inline'";
```

Deploy with flag OFF, test in production, then enable flag.

---

## Post-Implementation

### Documentation to Update

1. **Developer_Guide.md**
   - Add "CSP Nonce Usage" section
   - Document how to add new JS files
   - Explain data attribute pattern

2. **Security_Measures_and_Best_Practices.md**
   - Update CSP section with new configuration
   - Document nonce-based approach
   - Add CSP violation monitoring

3. **Security_Audit_Report_2026-02-11.md**
   - Mark "CSP Hardening" as IMPLEMENTED
   - Update security score
   - Remove from enhancement opportunities

4. **README.md**
   - Update security features section
   - Note CSP hardening completion

### Team Training

**Developer Training Session (1 hour):**
- Why nonce-based CSP matters
- How to add new JavaScript (with nonce)
- Data attribute pattern for Thymeleaf variables
- Debugging CSP violations

**Operations Training:**
- Monitoring CSP violation reports
- What CSP violations mean
- When to alert developers

### Ongoing Maintenance

**When Adding New Pages:**
1. Create external JS file (no inline scripts)
2. Add nonce attribute to script tags
3. Use data attributes for dynamic values
4. Test for CSP violations before committing

**Code Review Checklist:**
- [ ] No inline `<script>` blocks
- [ ] No inline event handlers (onclick, etc.)
- [ ] All scripts have nonce attribute
- [ ] Thymeleaf variables use data attributes

---

## Success Criteria

### Definition of Done

- [ ] All 26 templates migrated
- [ ] All 59 inline scripts moved to external files
- [ ] All 100+ inline event handlers converted
- [ ] Zero CSP violations in browser console
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Team trained
- [ ] Production deployment successful
- [ ] 48-hour monitoring period complete with no issues

### Metrics

**Before Implementation:**
- CSP: `script-src 'self' 'unsafe-inline' 'unsafe-eval' ...`
- Inline scripts: 59
- Inline event handlers: ~100
- XSS protection: Moderate

**After Implementation:**
- CSP: `script-src 'self' 'nonce-{RANDOM}' ...`
- Inline scripts: 0
- Inline event handlers: 0
- XSS protection: Strong

**Security Impact:**
- Blocks inline script injection attacks
- Provides audit trail for attempted attacks
- Meets compliance requirements (PCI-DSS Level 1, SOC2)

---

## Appendix A: Complete File List

### Templates to Migrate (26 files)

**Main Templates (11):**
1. src/main/resources/templates/task-list.html
2. src/main/resources/templates/task-detail.html
3. src/main/resources/templates/task-activity-form.html
4. src/main/resources/templates/expense-list.html
5. src/main/resources/templates/expense-detail.html
6. src/main/resources/templates/expense-sheet.html
7. src/main/resources/templates/expense-form.html
8. src/main/resources/templates/weekly-timesheet.html
9. src/main/resources/templates/change-password.html
10. src/main/resources/templates/login.html
11. src/main/resources/templates/reset-password.html

**Admin Templates (13):**
12. src/main/resources/templates/admin/user-management.html
13. src/main/resources/templates/admin/user-add.html
14. src/main/resources/templates/admin/user-edit.html
15. src/main/resources/templates/admin/user-change-password.html
16. src/main/resources/templates/admin/role-management.html
17. src/main/resources/templates/admin/role-add.html
18. src/main/resources/templates/admin/role-edit.html
19. src/main/resources/templates/admin/client-management.html
20. src/main/resources/templates/admin/project-management.html
21. src/main/resources/templates/admin/phase-management.html
22. src/main/resources/templates/admin/dropdown-management.html
23. src/main/resources/templates/admin/dropdown-edit.html
24. src/main/resources/templates/admin/guest-activity.html

**Other (2):**
25. src/main/resources/templates/dropdown-management-simple.html
26. src/main/resources/templates/dropdown-category-management.html
27. src/main/resources/templates/access-denied.html

### New JavaScript Files to Create (14)

**Existing (keep):**
1. src/main/resources/static/js/date-utils.js
2. src/main/resources/static/js/form-utils.js
3. src/main/resources/static/js/modal-utils.js
4. src/main/resources/static/js/password-toggle.js
5. src/main/resources/static/js/utc-time-converter.js

**New (create):**
6. src/main/resources/static/js/login.js
7. src/main/resources/static/js/task-list.js
8. src/main/resources/static/js/task-detail.js
9. src/main/resources/static/js/task-form.js
10. src/main/resources/static/js/expense-list.js
11. src/main/resources/static/js/expense-detail.js
12. src/main/resources/static/js/expense-form.js
13. src/main/resources/static/js/user-management.js
14. src/main/resources/static/js/user-form.js
15. src/main/resources/static/js/role-management.js
16. src/main/resources/static/js/dropdown-management.js
17. src/main/resources/static/js/timesheet.js
18. src/main/resources/static/js/password-change.js
19. src/main/resources/static/js/guest-activity.js

### Java Files to Create/Modify (3)

**New:**
1. src/main/java/com/ammons/taskactivity/security/CspNonceFilter.java
2. src/main/java/com/ammons/taskactivity/config/CspNonceDialect.java (optional)
3. src/main/java/com/ammons/taskactivity/controller/CspViolationReportController.java

**Modify:**
4. src/main/java/com/ammons/taskactivity/config/SecurityConfig.java

---

## Appendix B: Resources

### CSP References
- [MDN Web Docs - Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [CSP Evaluator (Google)](https://csp-evaluator.withgoogle.com/)
- [Content Security Policy Cheat Sheet (OWASP)](https://cheatseries.owasp.org/cheatsheets/Content_Security_Policy_Cheat_Sheet.html)

### Nonce Best Practices
- [CSP Nonces (Google Web Fundamentals)](https://web.dev/strict-csp/)
- [Spring Security CSP Support](https://docs.spring.io/spring-security/reference/features/exploits/headers.html#headers-csp)

### Testing Tools
- Chrome DevTools → Security Tab
- [CSP Header Validator](https://cspvalidator.org/)
- Browser extensions: CSP Auditor, CSP Generator

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-11 | Dean Ammons | Initial plan creation after Phase 1 (unsafe-eval removal) |

---

**Next Steps:**
1. Review and approve this implementation plan
2. Schedule 1-week sprint for implementation
3. Allocate developer resources
4. Set up staging environment for testing
5. Create JIRA tickets for each phase

**Questions? Contact:** Dean Ammons
