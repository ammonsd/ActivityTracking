# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## AI Guidance

* ALWAYS read and understand relevant files before proposing code edits. Do not speculate about code you have not inspected. If the user references a specific file/path, you MUST open and inspect it before explaining or proposing fixes. Be rigorous and persistent in searching code for key facts. Thoroughly review the style, conventions, and abstractions of the codebase before implementing new features or abstractions.
* After receiving tool results, carefully reflect on their quality and determine optimal next steps before proceeding. Use your thinking to plan and iterate based on this new information, and then take the best next action.
* After completing a task that involves tool use, provide a quick summary of what you've done.
* Before you finish, please verify your solution
* Do what has been asked; nothing more, nothing less.
* NEVER create files unless they're absolutely necessary for achieving your goal.
* ALWAYS prefer editing an existing file to creating a new one.
* If you create any temporary new files, scripts, or helper files for iteration, clean up these files by removing them at the end of the task.
* When you update or modify core context files, also update markdown documentation and memory banks (see Memory Bank System section below)
* When asked to commit changes, exclude CLAUDE.md and all /ai/ memory bank files from commits unless explicitly updating them as part of documentation work. Never delete these files.
* **When a user request goes against established conventions or best practices (e.g., inverting standard exit codes, breaking naming conventions, skipping security measures), ALWAYS confirm the intent before implementing.** Ask: "Just to clarify - this goes against [standard convention/best practice]. Is that intentional?" Better to ask one clarifying question than to implement something incorrectly and have to redo it.

<investigate_before_answering>
Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
</investigate_before_answering>

<do_not_act_before_instructions>
Do not jump into implementatation or changes files unless clearly instructed to make changes. When the user's intent is ambiguous, default to providing information, doing research, and providing recommendations rather than taking action. Only proceed with edits, modifications, or implementations when the user explicitly requests them.
</do_not_act_before_instructions>

<use_parallel_tool_calls>
If you intend to call multiple tools and there are no dependencies between the tool calls, make all of the independent tool calls in parallel. Prioritize calling tools simultaneously whenever the actions can be done in parallel rather than sequentially. For example, when reading 3 files, run 3 tool calls in parallel to read all 3 files into context at the same time. Maximize use of parallel tool calls where possible to increase speed and efficiency. However, if some tool calls depend on previous calls to inform dependent values like the parameters, do NOT call these tools in parallel and instead call them sequentially. Never use placeholders or guess missing parameters in tool calls.
</use_parallel_tool_calls>

## Memory Bank System

This project uses a structured memory bank system with specialized context files. **Always check these files for relevant information before starting work.**

### Memory Bank Files Location

Memory banks are stored in the `/ai/` directory:

- **[ai/project-overview.md](ai/project-overview.md)** - High-level project context, tech stack, architecture
- **[ai/java-conventions.md](ai/java-conventions.md)** - Java/Spring Boot coding standards (CRITICAL: explicit access modifiers)
- **[ai/devops-practices.md](ai/devops-practices.md)** - Infrastructure, deployment, AWS configuration
- **[ai/architecture-patterns.md](ai/architecture-patterns.md)** - Design patterns, domain model, security architecture
- **[ai/common-patterns.md](ai/common-patterns.md)** - Code templates and quick reference for common tasks
- **[ai/memory-bank-maintenance.md](ai/memory-bank-maintenance.md)** - Process for updating memory banks

### When to Reference Memory Banks

**Before generating code:**
- Check `java-conventions.md` for coding standards
- Check `common-patterns.md` for templates
- Check `architecture-patterns.md` for design patterns

**Before suggesting changes:**
- Check `project-overview.md` for project context
- Check `devops-practices.md` for deployment/infrastructure

**After making significant changes:**
- Update relevant memory bank files following `memory-bank-maintenance.md`
- Update this CLAUDE.md file if needed

**Important**: When asked to commit changes, **exclude CLAUDE.md and all files in /ai/ from commits** unless explicitly updating them as part of documentation work.

## Agent Skills

This project includes GitHub Copilot Agent Skills for common workflows. Skills provide step-by-step guidance, templates, and troubleshooting for specific tasks.

### Available Skills

Skills are located in the `/skills/` directory:

- **[api-endpoint](skills/api-endpoint/SKILL.md)** - REST API endpoint creation with validation and documentation
- **[aws-deployment](skills/aws-deployment/SKILL.md)** - AWS ECS deployment workflows and monitoring
- **[csv-bulk-import](skills/csv-bulk-import/SKILL.md)** - CSV import automation with templates and validation
- **[database-migration](skills/database-migration/SKILL.md)** - PostgreSQL schema migration patterns
- **[docker-operations](skills/docker-operations/SKILL.md)** - Docker build strategies, troubleshooting, optimization
- **[explaining-code](skills/explaining-code/SKILL.md)** - Code explanation patterns and documentation
- **[security-audit](skills/security-audit/SKILL.md)** - Comprehensive security checklist and scanning
- **[spring-boot-entity](skills/spring-boot-entity/SKILL.md)** - Complete entity creation (entity, repository, service, controller, tests)

### When to Use Skills

- Creating new entities → `spring-boot-entity`
- Creating REST APIs → `api-endpoint`
- Importing CSV data → `csv-bulk-import`
- Building/troubleshooting Docker → `docker-operations`
- Deploying to AWS → `aws-deployment`
- Database changes → `database-migration`
- Security reviews → `security-audit`
- Code explanation → `explaining-code`

Each skill includes templates, checklists, and troubleshooting guides.

## Quick Setup Reference

**For complete setup instructions, see `README.md`**

### Essential Prerequisites

- Java 21 (OpenJDK or Oracle)
- Node.js v20.11.1+ (CRITICAL: v20.11.0 incompatible)
- PostgreSQL 15+
- Docker (optional)

### Required Environment Variables

```bash
# Generate JWT secret (256-bit minimum)
JWT_SECRET=$(openssl rand -base64 32)

# Admin password (min 12 chars, mixed case, numbers, special)
APP_ADMIN_INITIAL_PASSWORD="SecurePassword123!"

# Database credentials
DB_USERNAME="postgres"
DB_PASSWORD="your-db-password"
```

### Quick Start

```bash
# Clone and setup database
git clone https://github.com/ammonsd/ActivityTracking.git
cd ActivityTracking
psql -U postgres -c "CREATE DATABASE AmmoP1DB;"
psql -U postgres -d AmmoP1DB -f src/main/resources/schema.sql

# Build and run
./mvnw.cmd clean package
./mvnw.cmd spring-boot:run
```

**Access**: http://localhost:8080 (login: admin / your-password)

## Build Commands Quick Reference

**Backend (Maven)**:
```bash
./mvnw.cmd clean package                       # Full build with frontend (~120s)
./mvnw.cmd clean package -Dskip.frontend.build=true  # Skip frontend
./mvnw.cmd test                                # Run tests
./mvnw.cmd spring-boot:run                     # Run locally
```

**Frontend (Angular)**:
```bash
cd frontend
npm install           # Install dependencies
npm start             # Dev server (localhost:4200)
npm run build:prod    # Production build
npm run test:once     # Tests with coverage
```

**Docker**:
```bash
docker-compose --profile host-db up -d          # Standard build
docker-compose --profile local-fast up -d       # Fast (requires pre-built JAR)
docker-compose --profile containerized-db up -d # Full stack with PostgreSQL
```

## Architecture Overview

### Technology Stack

- **Backend**: Spring Boot 3.5.7 (Java 21), Spring Security 6, Spring Data JPA, JWT authentication (JJWT 0.12.6)
- **Frontend**: Angular 19.2 standalone components, Angular Material, Chart.js, RxJS
- **Database**: PostgreSQL 15+
- **Storage**: Dual-mode receipt storage (AWS S3 for production, local filesystem for development)
- **Email**: AWS SES (production) or Spring Boot Mail (development)
- **Testing**: JUnit 5 (290+ tests), Testcontainers, Karma/Jasmine

### Core Architecture Patterns

#### 1. Dual REST + Web MVC Architecture

The application serves both:

- **REST API** (`/api/*`) - JWT-authenticated Angular frontend and external integrations
- **Traditional Web UI** (`/app/*`, `/profile/*`, `/login`, etc.) - Thymeleaf templates for fallback/admin views

#### 2. JWT Authentication Flow

- `JwtAuthenticationFilter` intercepts `/api/*` requests and validates JWT tokens
- `JwtUtil` handles token generation, validation, and parsing
- `TokenRevocationService` maintains server-side token blacklist for logout/password changes
- **Access tokens**: 24 hours (configurable via `JWT_EXPIRATION`)
- **Refresh tokens**: 7 days (configurable via `JWT_REFRESH_EXPIRATION`)
- Rate limiting: 5 requests/minute per IP on auth endpoints (Bucket4j)

#### 3. Multi-Stage Expense Approval Workflow

Expense status progression:

```
Draft → Submitted → Approved/Rejected → Reimbursed
         ↓ (can resubmit if rejected)
     Resubmitted
```

**Role-based access**:

- `USER/GUEST`: Create, edit (Draft only), submit
- `EXPENSE_ADMIN`: Approve, reject, mark reimbursed
- `ADMIN`: Full control

**Email notifications** sent automatically on each status change (AWS SES in production).

#### 4. Dual Storage Backend for Receipts

`ReceiptStorageService` interface with two implementations:

- `LocalReceiptStorageService` - File system storage (`./receipts` by default)
- `S3ReceiptStorageService` - AWS S3 storage

Switched via `storage.type=local|s3` in application properties.

**File security**: Magic number validation enforces JPEG/PNG/PDF (prevents malicious uploads via extension spoofing).

#### 5. Password Security and Expiration

- **90-day expiration policy** enforced in `UserService`
- **Daily scheduled job** (`PasswordExpirationNotificationService`) sends email warnings 1-7 days before expiration
- **Account lockout** after 5 failed login attempts (`CustomAuthenticationProvider`)
- Password changes invalidate all existing JWT tokens via `TokenRevocationService`

### Key Service Layer Responsibilities

| Service                                 | Purpose                                                         |
| --------------------------------------- | --------------------------------------------------------------- |
| `TaskActivityService`                   | Time tracking CRUD, filtering, CSV export/import                |
| `ExpenseService`                        | Expense workflow management, status transitions, approval logic |
| `ReceiptStorageService`                 | Abstract storage interface (S3 or local)                        |
| `EmailService`                          | AWS SES integration for transactional emails                    |
| `UserService`                           | User management, authentication, password policies              |
| `TokenRevocationService`                | Server-side JWT blacklist for logout/security events            |
| `PasswordExpirationNotificationService` | Scheduled job for password expiration warnings                  |
| `CsvImportService`                      | Bulk import of TaskActivity and Expense records                 |

### Security Architecture

#### Required Environment Variables (Application Fails Without These)

```bash
JWT_SECRET                    # 256-bit key (openssl rand -base64 32)
APP_ADMIN_INITIAL_PASSWORD    # Min 12 chars, mixed case, numbers, special
DB_USERNAME
DB_PASSWORD
```

#### Security Headers (applied in SecurityConfig)

- `X-Frame-Options: DENY`
- `Content-Security-Policy: default-src 'self'`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: no-referrer`
- `Permissions-Policy: geolocation=(), microphone=(), camera=()`
- HSTS enabled in production

#### Rate Limiting

- Authentication endpoints: 5 requests/minute per IP
- Controlled by `security.rate-limit.enabled` (disable for local dev if needed)

#### File Upload Security

- Magic number validation in `ReceiptController` (JPEG: `FF D8 FF`, PNG: `89 50 4E 47`, PDF: `25 50 44 46`)
- Prevents extension spoofing attacks

#### CORS Configuration

Cross-Origin Resource Sharing configured for API security:

```properties
# Production (strict)
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:https://taskactivitytracker.com}
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true

# Local development (permissive)
cors.allowed-origins=http://localhost:4200,http://localhost:8080
```

**AWS/Production**: Set `CORS_ALLOWED_ORIGINS` environment variable with comma-separated list of allowed domains.

### CSV Import Features

**Bulk data import** for TaskActivity and Expense records via `CsvImportService`.

#### Access Control

- **Required roles**: ADMIN or MANAGER
- **Endpoint**: `POST /api/csv/import`
- **File formats**: CSV with specific column headers

#### Import Capabilities

1. **TaskActivity Import**
    - Template: `docs/taskactivity-import-template.csv`
    - Validates: date format, hours, client/project/phase references
    - Creates missing DropdownValues automatically (if enabled)

2. **Expense Import**
    - Template: `docs/expense-import-template.csv`
    - Validates: amount, date, expense type, payment method
    - Supports receipt file references
    - Sets initial status to Draft

#### PowerShell Utility

`scripts/Import-CsvData.ps1` - Automated bulk import with:

- File validation
- JWT authentication
- Progress reporting
- Error handling and rollback

**Documentation**: `docs/CSV_Import_User_Guide.md`, `docs/CSV_Import_Quick_Reference.md`

### Profile Management

**Dual UI approach** for user profile editing:

#### 1. Angular Material UI (Modern)

- **Route**: `/app/profile`
- **Features**: Real-time validation, Material Design, responsive
- **Access**: All authenticated users (non-admin self-service)
- **Fields**: First name, last name, email, company

#### 2. Thymeleaf Backend UI (Fallback)

- **Route**: `/profile/edit`
- **Features**: Server-side validation, success/error notifications
- **Access**: All authenticated users
- **Use case**: JavaScript disabled or legacy browser support

**Security**: Users can only edit their own profile. Admins use separate user management UI for editing other users.

### Frontend Architecture

#### Standalone Components (Angular 19)

All components use standalone architecture (no NgModule):

- `AppComponent` - Root component with Material toolbar/sidenav
- Feature components: `DashboardComponent`, `TaskListComponent`, `ExpenseListComponent`, `ReportsComponent`, etc.
- Dialog components: Task/Expense edit, Receipt upload, User management

#### Services

- `AuthService` - JWT token management, login/logout, auto-logout on 401
- `TaskActivityService` / `ExpenseService` - HTTP clients for backend APIs
- `JwtInterceptor` - Automatically attaches `Authorization: Bearer <token>` to `/api/*` requests

#### Route Guards

- `AuthGuard` - Protects routes requiring authentication
- Admin-only routes protected in `app.routes.ts`

### Database Schema

**7 Core Entities**:

1. `User` - Authentication, profile, password expiration tracking
2. `TaskActivity` - Time tracking records (date, hours, client, project, phase)
3. `Expense` - Expense records with approval workflow
4. `DropdownValue` - Dynamic configuration (clients, projects, phases, expense types)
5. `Roles` - User roles (USER, GUEST, ADMIN, EXPENSE_ADMIN)
6. `Permission` - Fine-grained permissions
7. `RevokedToken` - JWT blacklist for server-side revocation

**Connection pooling** (HikariCP): 20 max connections, 5 min idle, 30s timeout.

### Configuration Profiles

| Profile      | Use Case                    | Database                         | Swagger  | DevTools |
| ------------ | --------------------------- | -------------------------------- | -------- | -------- |
| `local`      | Local development           | Host PostgreSQL                  | Enabled  | Enabled  |
| `docker`     | Docker development          | Host or containerized PostgreSQL | Disabled | Disabled |
| `aws`        | AWS ECS/Fargate             | RDS PostgreSQL                   | Disabled | Disabled |
| `production` | Production (Kubernetes/ECS) | External PostgreSQL              | Disabled | Disabled |

Activate via `SPRING_PROFILES_ACTIVE` environment variable or `--spring.profiles.active=local` JVM arg.

### Database Schema Management

**NO migration tools** (Flyway/Liquibase) - schema managed manually via `src/main/resources/schema.sql`.

#### Schema Initialization Modes by Profile

| Profile      | `spring.sql.init.mode` | Behavior                                                               |
| ------------ | ---------------------- | ---------------------------------------------------------------------- |
| `local`      | `never`                | **SAFE** - Schema NOT auto-created. Run manually for first-time setup. |
| `docker`     | `always`               | **AUTO-RUN** - Executes schema.sql on container start (fresh DB only). |
| `aws`        | `never`                | **SAFE** - Schema managed separately (CloudFormation/migrations).      |
| `production` | `never`                | **SAFE** - Schema managed separately.                                  |

#### Schema File Structure

```
src/main/resources/schema.sql   # Main schema definition (275 lines)
  - Creates tables in dependency order
  - Includes indexes and constraints
  - Inserts default roles and permissions
  - Creates initial admin user (password from env var)
```

#### Manual Schema Updates

When schema changes are needed:

1. Update `schema.sql` with DDL changes
2. For existing databases, create migration SQL script in `sql/` directory
3. Execute migration manually or via deployment automation
4. Document changes in commit message

**WARNING**: Running `schema.sql` on existing database will fail due to `CREATE TABLE IF NOT EXISTS`. Use targeted ALTER statements instead.

### API Documentation

Swagger UI is **disabled by default** for security. Enable in development:

```properties
# In application-local.properties
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
```

Access at `http://localhost:8080/swagger-ui.html`

## Development Scenarios Quick Reference

**JWT Authentication Testing**:
1. `POST /api/auth/login` → get `{accessToken, refreshToken}`
2. Add `Authorization: Bearer <token>` to API requests
3. `POST /api/auth/refresh` → get new tokens
4. `POST /api/auth/logout` → blacklist token

**Adding DropdownValues**: Insert via SQL or `DropdownRestController` API (categories: `client`, `project`, `phase`, `expense_type`, `payment_method`)

**Debugging Expenses**: Check logs for `EmailService`, permission issues (`@RequirePermission`), status transitions (`ExpenseService`)

**PostgreSQL Tests**: Integration tests auto-start Testcontainers PostgreSQL

**For complete development scenarios**, see `docs/Developer_Guide.md`.

### Deployment

#### Docker Build Strategies

- **Multi-stage Dockerfile**: Self-contained (builds Node.js + Maven), slow (~120s)
- **Dockerfile.local**: Fast (~10s), requires pre-built JAR
- **Production**: Uses Docker secrets for all sensitive environment variables

#### AWS Deployment

- CloudFormation templates: `cloudformation/templates/infrastructure.yaml`
- ECS Fargate tasks with RDS PostgreSQL
- S3 for receipt storage (bucket name in `storage.s3.bucket-name`)
- SES for email notifications (region in `mail.region`)

#### Kubernetes

- Manifests in `k8s/` directory
- Secrets: `taskactivity-secrets` (JWT, DB credentials, admin password)
- RBAC: `taskactivity-rbac.yaml`
- Liveness/Readiness probes configured

### Jenkins CI/CD

- Pipeline: `jenkins/Jenkinsfile`
- Multi-branch pipeline support
- Automatic build → Docker push → ECS deployment
- Email notifications on build completion

## Scripts and Utilities

**Deployment**: `scripts/docker-*.sh`, `scripts/setup-*.sh`

**Data Management**: `scripts/Import-CsvData.ps1`, `scripts/execute-sql-to-csv-api.ps1`

**Development**: `scripts/set-env-values.ps1`, `scripts/generate-token.ps1`, `scripts/start-jenkins.ps1`

**Monitoring**: `scripts/monitor-health.sh`, `scripts/rotate-secrets.sh`

**Database Optimization**: `sql/production_optimization.sql` - Run after deployment for performance tuning

**For detailed usage**, see script comments or `docs/` directory.

## Coding Standards and Best Practices

For comprehensive coding standards, refer to the **`.github/instructions/`** directory. This project uses GitHub Copilot custom instructions to enforce consistent quality across all code.

### Language-Specific Standards

| Instruction File                      | Applies To            | Purpose                                                            |
| ------------------------------------- | --------------------- | ------------------------------------------------------------------ |
| **java.instructions.md**              | `**/*.java`           | Java coding standards, Spring Boot patterns, JavaDoc requirements  |
| **python.instructions.md**            | `**/*.py`             | PEP 8 compliance, type hints, docstring conventions                |
| **powershell.instructions.md**        | `**/*.ps1, **/*.psm1` | PowerShell cmdlet standards, approved verbs, comment-based help    |
| **sql-sp-generation.instructions.md** | `**/*.sql`            | SQL formatting, stored procedure conventions, performance patterns |
| **markdown.instructions.md**          | `**/*.md`             | Documentation structure, style guide, formatting rules             |

### Universal Standards

| Instruction File                                     | Applies To | Purpose                                                             |
| ---------------------------------------------------- | ---------- | ------------------------------------------------------------------- |
| **code-comment.instructions.md**                     | `**`       | Attribution requirements with author/date for all new/modified code |
| **self-explanatory-code-commenting.instructions.md** | `**`       | Write code that speaks for itself, comment only when necessary      |
| **code-review-generic.instructions.md**              | `**`       | Comprehensive code review checklist (CRITICAL/IMPORTANT/SUGGESTION) |

### Process and Quality

| Instruction File                                                | Purpose                                                                                    |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| **devops-core-principles.instructions.md**                      | CALMS framework, DORA metrics (Deployment Frequency, Lead Time, Change Failure Rate, MTTR) |
| **copilot-thought-logging.instructions.md**                     | Track multi-step work with Copilot-Processing.md for complex tasks                         |
| **update-docs-on-code-change.instructions.md**                  | Auto-update README.md when application code changes                                        |
| **ai-prompt-engineering-safety-best-practices.instructions.md** | AI safety frameworks, bias mitigation, responsible AI usage                                |
| **instructions.instructions.md**                                | Guidelines for creating new custom instruction files                                       |

### Key Principles

1. **Code Attribution**: All new/modified code requires author and date comments (see code-comment.instructions.md)
2. **Self-Documenting Code**: Write clear code first, comment only WHY not WHAT (see self-explanatory-code-commenting.instructions.md)
3. **DevOps Culture**: Follow CALMS principles and optimize for DORA metrics (see devops-core-principles.instructions.md)
4. **Code Review Priority**: CRITICAL (block merge) > IMPORTANT (requires discussion) > SUGGESTION (non-blocking)

## Code Conventions (Project-Specific)

### Backend

- **Controllers**: `@RestController` for `/api/*`, `@Controller` for web views
- **Permissions**: Use `@RequirePermission("PERMISSION_NAME")` for fine-grained access control
- **Service layer**: All business logic in service classes, controllers are thin
- **DTOs**: Always use DTOs for API responses (never expose entities directly)
- **Exception handling**: `@ControllerAdvice` classes for global error handling

### Frontend

- **HTTP errors**: Interceptor auto-redirects to `/login` on 401
- **Date formatting**: Use `DatePipe` with 'yyyy-MM-dd' format for backend compatibility
- **Material Design**: Use Angular Material components for consistency
- **Charts**: Chart.js with ng2-charts wrapper, configuration in `ChartConfigService`

## Testing Strategy

### Backend Testing

- **Unit tests**: Mock dependencies, test business logic in isolation
- **Integration tests**: Use `@SpringBootTest` with Testcontainers for database
- **Security tests**: `@WithMockUser` for testing with different roles/permissions
- **Coverage**: 290+ tests covering core services, controllers, security

### Frontend Testing

- **Component tests**: Karma/Jasmine with mock services
- **Service tests**: Mock HttpClient responses
- **Run once with coverage**: `npm run test:once`

## Critical Configuration Notes

**Required Environment Variables** (application will NOT start without these):
- `JWT_SECRET` - 256-bit minimum (generate: `openssl rand -base64 32`)
- `APP_ADMIN_INITIAL_PASSWORD` - Min 12 chars, mixed case, numbers, special chars
- `DB_USERNAME` and `DB_PASSWORD`

**Key Constraints**:
- Node.js v20.11.1+ required (v20.11.0 incompatible with Angular 19.2)
- Docker Compose requires explicit profile: `--profile host-db|local-fast|containerized-db`
- Schema management: Manual migrations only (no Flyway/Liquibase)
- Password expiration: 90 days for all users including admin
- Rate limiting: 5 requests/minute on auth endpoints
- File uploads: Magic number validation (checks content, not just extension)

**Common Issues**:
- Swagger UI disabled by default (enable in application-local.properties)
- Schema not auto-created in local/production profiles (run schema.sql manually)
- JWT tokens invalidated on password change or server restart
- Frontend build takes ~120s (skip with `-Dskip.frontend.build=true`)

**For detailed troubleshooting**, see:
- `docs/Developer_Guide.md` - Complete technical reference
- `docs/Docker_Build_Guide.md` - Docker-specific issues
- Application logs: `/var/log/app/application.log` (Docker) or console output

## Tech Stack Summary

**Backend**: Spring Boot 3.5.7 (Java 21), Spring Security 6, Spring Data JPA, JWT (JJWT 0.12.6), PostgreSQL 15+

**Frontend**: Angular 19.2 standalone components, Angular Material, Chart.js, Node.js v20.11.1+ (CRITICAL)

**Infrastructure**: Docker, Docker Compose, AWS ECS/Fargate, RDS PostgreSQL, S3, SES

**For complete version details**, see `pom.xml` and `frontend/package.json`

## Key Documentation Reference

**In `docs/` directory**:
- `Developer_Guide.md` - Most comprehensive technical reference
- `Administrator_User_Guide.md` - Admin features and operations
- `Security_Measures_and_Best_Practices.md` - Complete security documentation
- `CSV_Import_User_Guide.md` - Bulk data import procedures
- `Docker_Build_Guide.md` - Docker containerization guide
- `Concurrency_and_Scaling_Guide.md` - Horizontal scaling strategies

**AWS and Kubernetes guides** in `aws/`, `cloudformation/`, and `k8s/` directories.

**Coding standards** in `.github/instructions/` (see Coding Standards section above).
