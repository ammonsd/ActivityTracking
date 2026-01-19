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
* When asked to commit changes, exclude CLAUDE.md, all /ai/ memory bank files, and /memories/ conversation memory files from commits unless explicitly updating them as part of documentation work. Never delete these files.

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

### Conversation Memory

In addition to memory banks, conversation-specific preferences are stored in `/memories/`:
- `/memories/dean-preferences.md` - Dean's coding preferences and working style
- `/memories/recent-work.md` - Recent work, decisions, and to-do items

**Important**: When asked to commit changes, **exclude CLAUDE.md and all files in /ai/ and /memories/ from commits** unless explicitly updating them as part of documentation work.

## Agent Skills

This project includes GitHub Copilot Agent Skills for common workflows. Skills provide step-by-step guidance, templates, and troubleshooting for specific tasks.

### Available Skills

Skills are located in the `/skills/` directory:

- **[csv-bulk-import](skills/csv-bulk-import/SKILL.md)** - CSV import automation with templates and validation
- **[aws-deployment](skills/aws-deployment/SKILL.md)** - AWS ECS deployment workflows and monitoring
- **[database-migration](skills/database-migration/SKILL.md)** - PostgreSQL schema migration patterns
- **[spring-boot-entity](skills/spring-boot-entity/SKILL.md)** - Complete entity creation (entity, repository, service, controller, tests)
- **[security-audit](skills/security-audit/SKILL.md)** - Comprehensive security checklist and scanning
- **[docker-operations](skills/docker-operations/SKILL.md)** - Docker build strategies, troubleshooting, optimization
- **[api-endpoint](skills/api-endpoint/SKILL.md)** - REST API endpoint creation with validation and documentation

### When to Use Skills

**Common workflow tasks:**
- Creating new entities → Use `spring-boot-entity` skill
- Building/troubleshooting Docker → Use `docker-operations` skill
- Creating REST APIs → Use `api-endpoint` skill
- Importing CSV data → Use `csv-bulk-import` skill

**Deployment and operations:**
- Deploying to AWS → Use `aws-deployment` skill
- Database changes → Use `database-migration` skill
- Security reviews → Use `security-audit` skill

Each skill includes templates, checklists, troubleshooting guides, and references to relevant memory bank files.

## Prerequisites

### Required Software

- **Java**: OpenJDK or Oracle JDK 21
- **Node.js**: v20.11.1+ (CRITICAL - v20.11.0 is incompatible with Angular 19.2)
- **npm**: 10.2.4+
- **PostgreSQL**: 15+ (local installation or Docker)
- **Git**: Latest version
- **Docker & Docker Compose**: Optional, for containerized development

### Verify Installations

```bash
# Check Java version
java -version  # Should show version 21

# Check Node.js version (must be v20.11.1+)
node --version

# Check npm version
npm --version

# Check PostgreSQL
psql --version

# Check Docker (optional)
docker --version
docker-compose --version
```

## First-Time Setup

### 1. Clone the Repository

```bash
git clone https://github.com/ammonsd/ActivityTracking.git
cd ActivityTracking
```

### 2. Set Required Environment Variables

**CRITICAL**: These environment variables are REQUIRED. The application will fail to start without them.

```bash
# Generate a secure JWT secret (256-bit minimum)
openssl rand -base64 32

# Set environment variables (Windows PowerShell)
$env:JWT_SECRET = "your-generated-secret-from-above"
$env:APP_ADMIN_INITIAL_PASSWORD = "SecurePassword123!"  # Min 12 chars, mixed case, numbers, special
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-db-password"

# Set environment variables (Linux/Mac)
export JWT_SECRET="your-generated-secret-from-above"
export APP_ADMIN_INITIAL_PASSWORD="SecurePassword123!"
export DB_USERNAME="postgres"
export DB_PASSWORD="your-db-password"
```

### 3. Start PostgreSQL Database

**Option A: Local PostgreSQL**

```bash
# Create database
psql -U postgres
CREATE DATABASE AmmoP1DB;
\q
```

**Option B: Docker PostgreSQL**

```bash
# Start PostgreSQL container
docker run -d \
  --name taskactivity-postgres \
  -e POSTGRES_DB=AmmoP1DB \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=your-db-password \
  -p 5432:5432 \
  postgres:15
```

### 4. Initialize Database Schema

**IMPORTANT**: Schema is managed via `src/main/resources/schema.sql`

- **First-time setup**: Run schema.sql manually or use Docker profile
- **Production/Local**: `spring.sql.init.mode=never` (schema NOT auto-created for safety)
- **Docker profile**: `spring.sql.init.mode=always` (auto-runs schema.sql in fresh containers)

```bash
# Manual schema initialization (if needed)
psql -U postgres -d AmmoP1DB -f src/main/resources/schema.sql
```

### 5. Build and Run the Application

```bash
# Full build (includes frontend)
./mvnw.cmd clean package

# Run the application
./mvnw.cmd spring-boot:run --spring.profiles.active=local
```

### 6. Access the Application

- **Backend API**: http://localhost:8080
- **Angular Frontend**: http://localhost:8080/app/ (after build)
- **Frontend Dev Server**: http://localhost:4200 (run `cd frontend && npm start`)
- **Swagger UI**: http://localhost:8080/swagger-ui.html (enable in application-local.properties first)

### 7. First Login

**Admin Account** (automatically created on first startup):

- **Username**: `admin`
- **Password**: Value from `APP_ADMIN_INITIAL_PASSWORD` environment variable
- **Password Expiration**: 90 days from first login

## Port Configuration

| Service             | Port | Configurable Via                                      |
| ------------------- | ---- | ----------------------------------------------------- |
| Backend API         | 8080 | `PORT` environment variable or `server.port` property |
| Frontend Dev Server | 4200 | Angular configuration                                 |
| PostgreSQL          | 5432 | Docker/PostgreSQL config                              |
| Jenkins (optional)  | 8081 | Jenkins configuration                                 |

## Build and Test Commands

### Backend (Spring Boot + Maven)

```bash
# Full build with frontend (takes ~120s)
./mvnw.cmd clean package

# Backend-only build (skip Angular build)
./mvnw.cmd clean package -Dskip.frontend.build=true

# Run tests only
./mvnw.cmd test

# Run backend locally (requires PostgreSQL running)
./mvnw.cmd spring-boot:run

# Skip tests during build
./mvnw.cmd clean package -DskipTests
```

### Frontend (Angular 19)

```bash
cd frontend

# Install dependencies
npm install

# Development server (localhost:4200)
npm start

# Production build (outputs to dist/app)
npm run build:prod

# Run tests once with coverage
npm run test:once

# Run tests in watch mode
npm test
```

### Docker

```bash
# Build and run with host PostgreSQL (standard multi-stage build)
docker-compose --profile host-db up -d

# Fast build (requires pre-built JAR: mvnw.cmd clean package -DskipTests)
docker-compose --profile local-fast up -d

# Full containerized stack (app + PostgreSQL)
docker-compose --profile containerized-db up -d

# Production with Docker secrets
docker-compose --profile production up -d
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

### Common Development Scenarios

#### Running Backend Tests Against PostgreSQL

Tests use H2 in-memory database by default. For PostgreSQL integration tests:

```bash
# Start Testcontainers PostgreSQL (automatic in integration tests)
./mvnw.cmd test
```

#### Testing JWT Authentication Flow

1. Login: `POST /api/auth/login` with `{username, password}` → returns `{accessToken, refreshToken}`
2. Use access token: Add `Authorization: Bearer <accessToken>` header to `/api/*` requests
3. Refresh: `POST /api/auth/refresh` with `{refreshToken}` → returns new `{accessToken, refreshToken}`
4. Logout: `POST /api/auth/logout` with `{token}` → blacklists token server-side

#### Debugging Expense Workflow Issues

Check logs for:

- Email sending failures (`EmailService`)
- Permission issues (`@RequirePermission` annotations)
- Status transition validation (`ExpenseService.updateExpenseStatus()`)

#### Adding New DropdownValue Types

1. Insert via SQL or `DropdownRestController` API
2. Frontend automatically fetches via `DropdownService.getDropdownValues(category)`
3. Categories: `client`, `project`, `phase`, `expense_type`, `payment_method`, etc.

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

The `scripts/` directory contains operational and development utilities:

### Deployment Scripts

- `docker-deployment.sh` - Automated Docker build and deployment
- `docker-rebuild-and-start.sh` - Quick rebuild and restart of containers
- `setup-production.sh` - Production environment initialization
- `setup-docker-secrets.sh` - Docker secrets configuration for sensitive data

### Monitoring and Health

- `monitor-health.sh` - Application health check monitoring
- `rotate-secrets.sh` - Automated secret rotation for security compliance

### Data Management

- `Import-CsvData.ps1` - PowerShell script for bulk CSV import (TaskActivity/Expense)
- `execute-sql-to-csv-api.ps1` - Execute SQL queries and export results to CSV

### Development Utilities

- `set-env-values.ps1` - PowerShell script to set environment variables
- `generate-token.ps1` - JWT token generation for testing
- `generate-jenkins-token.ps1` / `generate-jenkins-token-simple.ps1` - Jenkins API token generation
- `start-jenkins.ps1` - Local Jenkins startup script

### Database Optimization

The `sql/` directory contains performance optimization scripts:

- `production_optimization.sql` - Database indexes and query optimization for production
- `production_optimization_concurrent_alternative.sql` - Concurrent index creation (no downtime)

**Usage**: Run after initial deployment or when performance tuning is needed.

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

## Troubleshooting

### Critical Configuration Issues

#### Application Won't Start

**Symptoms**: Application fails immediately on startup with configuration error

**Common Causes**:

1. **Missing JWT_SECRET** - Application requires 256-bit minimum JWT secret

    ```bash
    # Generate secure secret
    openssl rand -base64 32
    # Set environment variable
    export JWT_SECRET="<generated-secret>"
    ```

2. **Missing APP_ADMIN_INITIAL_PASSWORD** - Required for admin user creation
    - Minimum 12 characters
    - Must include uppercase, lowercase, number, and special character

    ```bash
    export APP_ADMIN_INITIAL_PASSWORD="SecurePass123!"
    ```

3. **Database connection failure** - Verify DB_USERNAME, DB_PASSWORD, and DATABASE_URL
    ```bash
    # Test PostgreSQL connection
    psql -h localhost -U postgres -d AmmoP1DB
    ```

#### Authentication Issues

**Rate Limiting During Development**

- **Symptom**: "Too many requests" error when testing authentication
- **Solution**: Disable rate limiting in `application-local.properties`
    ```properties
    security.rate-limit.enabled=false
    ```

**JWT Token Validation Failures**

- **Cause**: Token blacklist not synchronized (server restart)
- **Solution**: Re-login to get fresh token
- **Note**: Password changes and logout invalidate all existing tokens

**Account Locked After Failed Logins**

- **Cause**: 5 failed login attempts triggers lockout
- **Solution**: Admin must unlock in database or wait for timeout
    ```sql
    UPDATE users SET account_locked = false WHERE username = 'user';
    ```

#### Database Issues

**Connection Pool Exhausted**

- **Symptom**: "Unable to acquire JDBC Connection" errors
- **Cause**: All 20 connections in use (high load or connection leaks)
- **Solution**: Increase pool size or investigate connection leaks
    ```properties
    spring.datasource.hikari.maximum-pool-size=30
    ```

**Schema Not Created**

- **Symptom**: "Table does not exist" errors
- **Cause**: `spring.sql.init.mode=never` (default in local/production)
- **Solution**: Run schema.sql manually
    ```bash
    psql -U postgres -d AmmoP1DB -f src/main/resources/schema.sql
    ```

**Timezone Issues**

- **Symptom**: Dates off by several hours
- **Cause**: PostgreSQL and Java using different timezones
- **Solution**: Application uses UTC in Java code; set Docker timezone
    ```bash
    docker run -e TZ=America/New_York ...
    ```

#### Build and Deployment Issues

**Frontend Build Fails**

- **Node.js version incompatibility** (CRITICAL)
    - **Symptom**: Angular compiler errors during Maven build
    - **Cause**: Node.js v20.11.0 incompatible with Angular 19.2
    - **Solution**: Use Node.js v20.11.1+ (specified in pom.xml)
    - **Verify**: Check `pom.xml` line 266 has `<nodeVersion>v20.11.1</nodeVersion>`

**Docker Compose Profile Required**

- **Symptom**: "No service selected" error
- **Solution**: Must specify profile explicitly
    ```bash
    docker-compose --profile host-db up -d
    ```

**Maven Build Slow**

- **Cause**: Frontend build takes ~120 seconds
- **Solution**: Skip frontend for backend-only changes
    ```bash
    ./mvnw.cmd clean package -Dskip.frontend.build=true
    ```

#### Runtime Issues

**Swagger UI Not Accessible**

- **Cause**: Disabled by default for security
- **Solution**: Enable in `application-local.properties`
    ```properties
    springdoc.swagger-ui.enabled=true
    springdoc.api-docs.enabled=true
    ```

**Email Notifications Not Sending**

- **Development**: Verify Spring Boot Mail configuration
- **Production**: Check AWS SES credentials and region
- **Common issue**: Email not configured in user profile
    ```properties
    # AWS SES configuration
    mail.region=us-east-1
    spring.mail.username=<AWS-SES-SMTP-USERNAME>
    spring.mail.password=<AWS-SES-SMTP-PASSWORD>
    ```

**Receipt Upload Fails**

- **Symptom**: "Invalid file type" despite correct extension
- **Cause**: Magic number validation checks file content, not extension
- **Solution**: Ensure file is genuine JPEG/PNG/PDF (not renamed)

**Admin Password Expired**

- **Symptom**: Cannot login after 90 days
- **Solution**: Update password in database or reset
    ```sql
    UPDATE users
    SET password_last_changed = CURRENT_TIMESTAMP
    WHERE username = 'admin';
    ```

#### Docker Volume Permissions

- **Symptom**: Permission denied errors for logs or receipts
- **Linux/Mac**: Ensure Docker has write permissions
    ```bash
    chmod 777 ./logs ./receipts
    ```

### Important Configuration Notes

1. **JWT_SECRET**: 256-bit minimum, application fails without it
2. **Password expiration**: 90-day policy, admin included
3. **Token revocation**: Server-side blacklist, persisted in database
4. **File upload security**: Magic number validation prevents extension spoofing
5. **Rate limiting**: 5 requests/minute on auth endpoints
6. **CORS**: Configure allowed origins for API access
7. **Schema management**: Manual migrations, no Flyway/Liquibase
8. **Node.js version**: Must use v20.11.1+ for Angular 19.2
9. **Docker profiles**: No default, must specify explicitly
10. **Email configuration**: AWS SES (production) or local SMTP (development)

## Useful Log Locations

- **Application logs**: `/var/log/app/application.log` (Docker volume-mounted to host)
- **Spring Boot logs**: Console output (stdout) in Docker
- **PostgreSQL logs**: Via `docker logs <postgres-container>`
- **Jenkins build logs**: Jenkins UI under build history

## Version Reference

Consolidated version information for all major dependencies:

### Backend

| Component      | Version    | Notes                          |
| -------------- | ---------- | ------------------------------ |
| Spring Boot    | 3.5.7      | Latest stable                  |
| Java           | 21         | LTS version, OpenJDK or Oracle |
| PostgreSQL     | 15+        | Client driver auto-configured  |
| AWS SDK        | 2.21.0     | For S3 and SES integration     |
| JJWT           | 0.12.6     | JWT authentication library     |
| HikariCP       | (included) | Connection pooling             |
| Testcontainers | (included) | Integration testing            |

### Frontend

| Component        | Version   | Notes                               |
| ---------------- | --------- | ----------------------------------- |
| Angular          | 19.2.15   | Standalone components               |
| Angular Material | 19.2.19   | UI component library                |
| Node.js          | v20.11.1+ | **CRITICAL**: v20.11.0 incompatible |
| npm              | 10.2.4+   | Package manager                     |
| TypeScript       | ~5.7.2    | (Angular dependency)                |
| RxJS             | ~7.8.0    | Reactive programming                |
| Chart.js         | ^4.5.1    | Data visualization                  |
| ng2-charts       | ^8.0.0    | Angular wrapper for Chart.js        |

### Build Tools

| Component             | Version | Notes                   |
| --------------------- | ------- | ----------------------- |
| Maven Wrapper         | 3.9.9   | Included in project     |
| frontend-maven-plugin | 1.12.1  | Builds Angular in Maven |

### Infrastructure

| Component      | Version | Notes                         |
| -------------- | ------- | ----------------------------- |
| Docker         | 20.10+  | Container runtime             |
| Docker Compose | 3.8+    | Multi-container orchestration |
| Kubernetes     | 1.24+   | Optional, for K8s deployment  |
| Jenkins        | 2.400+  | CI/CD automation              |

**Version Pinning**: Node.js version is pinned in `pom.xml` (line 266) to ensure consistent builds across environments.

## Key Documentation Files

Comprehensive documentation in `docs/` directory:

- `Developer_Guide.md` - Technical deep dive (most detailed reference)
- `Administrator_User_Guide.md` - Admin features and 12-Factor App compliance
- `Security_Measures_and_Best_Practices.md` - Complete security documentation
- `CSV_Import_User_Guide.md` - Bulk data import procedures
- `Concurrency_and_Scaling_Guide.md` - Horizontal scaling strategies
- `Docker_Build_Guide.md` - Complete Docker containerization guide

AWS and Kubernetes deployment guides in `aws/`, `cloudformation/`, and `k8s/` directories.
