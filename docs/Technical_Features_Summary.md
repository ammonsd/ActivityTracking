# Technical Features Summary

**Project:** Task Activity Management System  
**Last Updated:** January 5, 2026  
**Version:** 1.1

> **🔒 Security Documentation**  
> For comprehensive security measures, controls, and best practices, see **[Security Measures and Best Practices](Security_Measures_and_Best_Practices.md)**.

---

## Overview

This document provides a comprehensive summary of all technical features, frameworks, tools, and technologies implemented in the Task Activity Management System. This is designed as a quick reference for interview preparation, highlighting technical capabilities without code examples.

---

## 🏗️ Core Technology Stack

### Backend Framework & Languages

- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.5.6** - Enterprise-grade application framework
- **Maven 3.9.9** - Build automation and dependency management
- **PostgreSQL 14+** - Development and production relational database
- **H2 Database** - In-memory database for testing only

### Build & Development Tools

- **Maven Wrapper** - Ensures consistent Maven versions across environments
- **Spring Boot DevTools** - Hot reload during development
- **Maven Compiler Plugin** - Java 21 compilation configuration

---

## 🌱 Spring Framework Features

### Spring Boot Starters

- **spring-boot-starter-web** - RESTful web services and MVC
- **spring-boot-starter-data-jpa** - Database persistence layer
- **spring-boot-starter-validation** - Bean validation framework
- **spring-boot-starter-security** - Authentication and authorization
- **spring-boot-starter-actuator** - Application monitoring and health checks
- **spring-boot-starter-thymeleaf** - Server-side template engine
- **spring-boot-starter-mail** - Email notification capabilities
- **spring-boot-starter-test** - Comprehensive testing framework
- **springdoc-openapi-starter-webmvc-ui** - Swagger/OpenAPI documentation (2.6.0)

### Spring Data JPA

- **JPA Entities** with annotations (@Entity, @Table, @Column)
- **Spring Data Repositories** with custom query methods
- **Named queries** using @Query annotation
- **Entity relationships** and cascade operations
- **Transaction management** with @Transactional
- **Database initialization** with schema.sql and data.sql
- **Hibernate** as JPA implementation

### Spring Security

- **Form-based authentication** with custom login pages
- **JWT (JSON Web Token) Authentication** (Enhanced January 2026):
  - Access tokens and refresh tokens with type differentiation
  - Token type validation prevents token misuse
  - JJWT library (0.12.6) for secure token generation
  - 512-bit minimum secret key requirement
- **Database-driven Role-Based Access Control (RBAC)**:
  - Roles stored in `roles` database table (not enum)
  - Permissions stored in `permissions` table with resource:action pattern
  - Many-to-many relationship via `role_permissions` join table
  - Four default roles: ADMIN, USER, GUEST, EXPENSE_ADMIN
  - Custom roles can be created via web UI without code changes
- **Custom Authorization Framework**:
  - `@RequirePermission` annotation for method-level security
  - `PermissionAspect` - Spring AOP interceptor for runtime permission enforcement
  - Replaces standard @PreAuthorize with flexible database-driven permission checking
  - Defense-in-depth: URL-based authentication + method-level permissions
- **Role & Permission Management UI**:
  - Web interface for creating custom roles
  - Assign/revoke permissions without code deployment
  - RoleManagementController with role-management, role-edit, role-add pages
  - Permission format: RESOURCE:ACTION (e.g., TASK_ACTIVITY:READ, EXPENSE:APPROVE)
- **Admin Endpoint Protection** (Enhanced January 2026):
  - `/api/admin/**` requires authentication
  - Permission-based access control via @RequirePermission
  - Prevents anonymous access to administrative functions
- **Account Status Enforcement** (Enhanced January 2026):
  - Post-authentication account status validation in JWT filter
  - Validates enabled, locked, expired, and credentials status
  - Prevents disabled/locked accounts from using valid tokens
- **BCrypt password encoding** for secure password storage
- **CSRF protection** with CookieCsrfTokenRepository
- **Session management** with concurrent session control
- **Custom authentication handlers** for success and failure
- **Custom access denied handler** for authorization failures
- **Force password update filter** for administrative password resets
- **Automatic password expiration** with 90-day policy and advance warnings
- **Automated password expiration notifications** - scheduled daily checks at 8:00 AM for passwords expiring within 1-7 days
- **Account lockout protection** - automatic lockout after 5 failed login attempts
- **Email notifications** for security events (account lockouts, password expiration warnings), expense submissions, and expense status changes
- **Email-based authorization** - users must have valid email address to access expense features
- **Security Headers** (Enhanced January 2026):
  - X-Content-Type-Options: nosniff (prevents MIME sniffing attacks)
  - X-Frame-Options: DENY (prevents clickjacking)
  - Content-Disposition: attachment for file downloads (prevents XSS)
- **File Upload Security**:
  - Magic number (file signature) validation for uploaded receipts
  - Configurable file size limits per environment (2MB local, 5MB production, 10MB AWS)
  - Dynamic file size limit display to users
  - Multi-layer validation: frontend, Spring Boot multipart filter, and controller
  - Rejected file types: executables, scripts, and files with mismatched content
  - Supported formats: JPEG, PNG, PDF (validated by actual file content)
- **Spring Security Test** for security-aware testing
- **Comprehensive Security Test Suite**: 24 dedicated security integration tests
- **Role-based UI features**:
  - ADMIN-only User Analysis tab in Reports
  - ADMIN-only Role & Permission Management interface
  - EXPENSE_ADMIN access to expense approval and reimbursement functions
  - Role-based data filtering (ADMIN sees all users, regular users see only own data)
  - SecurityConfig request matcher ordering for proper API access control
  - Conditional UI rendering with *ngIf directives based on user roles

### Spring MVC & Web

- **RESTful API endpoints** with @RestController
- **Web controllers** with @Controller for Thymeleaf views
- **Request/Response DTOs** for clean API contracts
- **Global exception handling** with @ControllerAdvice
- **CORS configuration** for cross-origin requests
- **Multi-environment profiles** (local, docker, aws)
- **Custom error pages** and error handling

### Spring Boot Configuration

- **@ConfigurationProperties** for type-safe configuration
- **Environment-specific properties** files (application-{profile}.properties)
- **EnvironmentPostProcessor** for custom environment setup
- **Externalized configuration** via environment variables
- **Secrets management** through file-based secrets loading

### Spring Boot Actuator

- **Health check endpoints** for monitoring
- **Application metrics** and monitoring capabilities
- **Production-ready features** for operational visibility

### Spring Scheduling & Background Tasks

- **@EnableScheduling** - Application-level scheduling configuration
- **@Scheduled** - Cron-based task execution
- **Password expiration notification service** - Daily scheduled check at 8:00 AM
- **Token revocation service** - Daily cleanup at 2:00 AM for expired tokens
- **Manual trigger endpoints** - Admin endpoints for testing scheduled tasks without waiting
- **Comprehensive logging** - Detailed logs for scheduled task execution and results
- **Production-ready** - Scheduling works across multiple ECS Fargate instances

### Angular Dashboard Features

- **Cross-Dashboard Navigation** (February 2026):
  - **Admin Dashboard Link**: Available for ADMIN and GUEST roles in Angular dashboard sidebar
    - Icon: admin_panel_settings (shield icon)
    - Navigates to React Admin Dashboard at `/dashboard`
    - Provides quick access to advanced admin features and management tools
  - Preserves authentication session across dashboard transitions
  - Seamless user experience between Angular (user-focused) and React (admin-focused) interfaces
- **Expense Management in Angular UI**:
  - Full CRUD operations (Create, Read, Update, Delete) for expenses
  - Receipt upload integrated into Add/Edit expense dialogs
  - Clone expense functionality for quick duplicate entries
  - Comma-separated currency formatting for better readability
  - Streamlined action buttons layout (Add Expense, Clear Filters)
  - No browser refresh required - all operations via REST API
- **Analytics & Reports with Date Range Filtering**:
  - Preset date range buttons (This Month, Last Month, Last 3 Months, This Year, All Time)
  - Custom date range selection with Material Design date pickers
  - Real-time report updates when date range changes
  - All report components support date filtering via @Input properties
  - OnChanges lifecycle hook for automatic data refresh
  - Reports Service methods updated to accept optional date parameters

### Email Notification System

- **Email requirement for expense access** - users must have valid email to access expense features
- **Expense submission notifications** - emails sent to configured approvers when expenses are submitted
- **Automatic status notifications** - emails sent when expenses are approved, rejected, or reimbursed
- **Multiple approver support** - comma-separated list of approver emails in configuration
- **Status-specific messaging** - custom email content based on status change type
- **Processor identification** - displays full name of approver/reimbursor in emails
- **Password expiration warnings** - automated daily scheduled task (8:00 AM) sends warnings to users 1-7 days before password expires
- **Urgency-based password warnings** - email messaging adapts based on days until expiration (URGENT, IMPORTANT, standard)
- **Account lockout notifications** - automated alerts sent to administrators when accounts are locked due to failed login attempts
- **Email validation** - @Email annotation on User entity and DTOs
- **Authorization checks** - UserService.userHasEmail() validates access to expense features
- **UI access control** - expense buttons/links hidden for users without email addresses
- **AWS SES integration** - production email delivery via Amazon Simple Email Service

---

## 🔐 Security Implementation

### Authentication & Authorization

- Form-based login with secure session handling
- **Database-driven role-based authorization**
  - Roles entity with customizable permissions
  - Permission entity with resource:action pattern
  - @RequirePermission annotation for method-level security
  - PermissionAspect for AOP-based permission enforcement
  - Custom role creation via web UI
- Method-level security annotations
- Custom authentication success/failure handlers
- Session fixation protection
- Remember-me functionality
- Forced password update mechanism
- **Account lockout protection** against brute force attacks
  - Automatic lockout after 5 failed login attempts (configurable)
  - Failed attempt counter resets on successful login
  - Admin unlock capability via User Management UI
  - Visual indicators for locked accounts

### User Management

- **Dual UI interfaces**:
  - **React Dashboard**: Modern Material-UI user management with full CRUD (frontend-react/src/pages/UserManagement.tsx)
  - **Spring Boot UI**: Thymeleaf-based user management with Bootstrap styling (templates/admin/user-management.html)
- **React Dashboard Features** (Phase 4 - February 2026):
  - Full CRUD operations via REST API
  - Create users with role assignment and validation
  - Edit users (all fields except username which is immutable)
  - Delete users with confirmation and protection rules
  - Admin password change with force update option
  - Filter by username, role, company (partial and exact match)
  - Pagination with 5/10/25/50 rows per page
  - Delete button disabled for: current user and users with task activities
  - Password requirements displayed with validation
  - Show/hide password toggles
  - Real-time form validation
  - Material-UI dialogs and responsive design
- **REST API Endpoints**:
  - `GET /api/users` - List users with optional filters
  - `GET /api/users/{id}` - Get user by ID
  - `GET /api/users/me` - Current authenticated user
  - `POST /api/users` - Create new user
  - `PUT /api/users/{id}` - Update user
  - `DELETE /api/users/{id}` - Delete user
  - `PUT /api/users/{id}/password` - Admin password change (requires USER_MANAGEMENT:UPDATE permission)
  - `GET /api/users/roles` - List available roles
  - `GET /api/users/notify-eligible` - List active users with email, optional `lastNameFilter` param (requires USER_MANAGEMENT:READ)
  - `POST /api/users/notify` - Send profile notification emails to selected users (requires USER_MANAGEMENT:UPDATE)
- **Permission-based access**:
  - @RequirePermission annotations on all endpoints
  - CREATE, READ, UPDATE, DELETE permissions for USER_MANAGEMENT resource

### User Profile Management

- **Self-service profile editing** for USER, ADMIN, and EXPENSE_ADMIN roles
- **Dual UI interfaces**:
  - **Angular UI**: Modern Material Design profile component with integrated password change dialog (frontend/src/app/components/profile/)
  - **Backend UI**: Thymeleaf-based profile editor with Bootstrap styling and dedicated password change page
- **User-controlled fields**: First name, last name, company, email address
- **Protected fields**: Username (immutable), role, account status, lock status
- **Profile access points**:
  - Angular: "My Profile" card on dashboard and side menu option
  - Backend: "My Profile" link in user menu
- **Password Change Features**:
  - **Angular**: Inline dialog with current password verification, show/hide toggles, real-time validation feedback, and specific error messages
  - **Backend**: Dedicated page with form validation and success/error notifications
  - Current password verification required for self-service changes
  - Cannot reuse last 5 passwords
  - Password complexity requirements enforced (10+ chars, uppercase, digit, special character)
- **REST API endpoints**:
  - `GET /api/users/me` - Current user information
  - `GET /api/users/profile` - Current user's profile for editing
  - `PUT /api/users/profile` - Update current user's profile
  - `PUT /api/users/profile/password` - Self-service password change (requires current password verification, no admin permission required)
  - `GET /profile/edit` - Backend Thymeleaf profile editor
  - `POST /profile/edit` - Backend profile update handler
- **Security architecture**:
  - SecurityConfig order-dependent requestMatchers (specific before broad)
  - Profile endpoints allowed for USER/ADMIN/EXPENSE_ADMIN before general /api/users/** ADMIN restriction
  - Method-level @PreAuthorize annotations on UserRestController
  - UserProfileController with role-based @PreAuthorize
- **Navigation flow**:
  - Profile update success returns to profile page with confirmation message
  - Password change redirects back to My Profile after completion
  - Cancel button returns to dashboard (Angular) or uses top navigation (backend)
- **Email requirement enforcement**: Email address required for expense management access

### Password Security

- BCrypt password hashing
- Password strength validation (minimum length, complexity requirements)
- Custom password validator with configurable rules
- Support for password rotation and forced updates
- **Automatic password expiration** (90-day policy)
- **7-day advance warning** for expiring passwords
- **Expired password enforcement** at login
- Password expiration warnings displayed in both Spring Boot and Angular UIs
- **Password change access**:
  - All authenticated users (USER, ADMIN, EXPENSE_ADMIN) can change own password
  - Accessed via "Update Password" button in My Profile
  - Standalone `/change-password` endpoint with role-based security
  - Redirects back to My Profile after successful password change
- **GUEST role password restrictions**:
  - GUEST users cannot change their own passwords
  - Password change pages blocked for GUEST role
  - **Password reset blocked for GUEST users** (silent block for security)
  - Reset requests accepted but no email sent to GUEST accounts
  - Blocked attempts logged with warning: "Password reset blocked for GUEST user"
  - GUEST users with expired passwords blocked from authentication
  - Special error message directs GUEST users to contact administrator
  - Administrators must manually reset GUEST passwords through user management

### CSRF & Session Security

- CSRF token protection on all forms
- Cookie-based CSRF token repository
- Session timeout configuration
- Concurrent session control
- Secure cookie attributes

### Secrets Management

- Docker Swarm secrets integration
- Kubernetes secrets support
- File-based secrets via custom EnvironmentPostProcessor
- AWS Secrets Manager ready

### Email Notifications

- **Spring Boot Mail** integration with JavaMailSender
- **Account lockout notifications** sent to administrators
  - Automatic email when account is locked after failed login attempts
  - Includes username, failed attempt count, IP address, and timestamp
  - Configurable SMTP settings (Gmail, custom servers)
- **Environment-specific configuration** for local and AWS deployments
- **AWS Secrets Manager integration** for email credentials
- **Graceful degradation** - application continues if email fails
- **EmailService** with comprehensive error handling and logging
- **Unit tested** with 8 comprehensive test cases
- Zero hardcoded credentials

---

## 🗄️ Database & Persistence

### Database Technologies

- PostgreSQL for development and production
- H2 for testing only
- JDBC connection pooling
- Database schema initialization
- Data initialization scripts

### JPA & Hibernate Features

- Entity modeling with JPA annotations
- Bean validation constraints (@NotNull, @NotBlank, @Size)
- Automatic schema generation
- Custom repository methods
- Query derivation from method names
- Named queries with JPQL

### Data Management

- Transaction management
- Optimistic locking
- Database migration support
- Reference data management (dropdown values)
- Referential integrity enforcement

---

## 🎨 Frontend Technologies

### Angular Frontend (Modern SPA)

- **Angular 19** - Latest version with standalone components architecture
- **TypeScript** - Type-safe development
- **RxJS** - Reactive programming with observables
- **Angular Material** - Google's Material Design components
- **Chart.js 4.4.0** - Canvas-based chart rendering
- **ng2-charts 6.0.0** - Angular wrapper for Chart.js
- **Responsive design** - Mobile-friendly layouts
- **HTTP Client** - RESTful API integration
- **Angular Router** - Client-side routing with guards
- **Reactive Forms** - Data binding and validation

### React Admin Dashboard (Modern SPA)

- **React 19.2.0** - Latest version with modern hooks and concurrent features
- **TypeScript 5.9.3** - Type-safe development with strict mode
- **Material-UI v7.3.7** - Google's Material Design component library
- **Vite 7.2.4** - Next-generation frontend build tool
  - Hot Module Replacement (HMR) for instant updates
  - Optimized production builds with code splitting
- **Axios** - Promise-based HTTP client for RESTful API integration
- **Zustand** - Lightweight state management library
- **Feature flags** - Modular feature enablement via configuration
- **Session-based auth** - Shares Spring Security session with backend
- **ADMIN-only access** - Role enforcement at Spring Security level

### React Dashboard Features

- **Cross-Dashboard Navigation** (February 2026):
  - **User Dashboard Link**: Available for all roles (ADMIN, USER, GUEST, EXPENSE_ADMIN) in React dashboard sidebar
    - Icon: dashboard icon
    - Navigates to Angular User Dashboard at `/app`
    - Provides quick access to user-focused features (tasks, expenses, reports)
  - Preserves authentication session across dashboard transitions
  - Enables admins to quickly switch between management and operational views
- **Dropdown Management** (Phase 5 - February 2026)
  - Comprehensive dropdown value management for TASK and EXPENSE categories
  - Dynamic category and subcategory filtering with real-time updates
  - "Add New Category" dialog for creating category/subcategory/value combinations
  - Inline "Add Value" form (disabled until category selected)
  - Material-UI table displaying: Category, Subcategory, Value, Display Order, Status (Active/Inactive), Actions
  - Edit dialog for modifying value, display order, and active status fields
  - Delete confirmation dialog with contextual information
  - Summary statistics showing total values and active count
  - Success/Error notifications via Snackbar
  - TypeScript types: `DropdownValue`, `DropdownFilters`, request/response types
  - API client wrapper: `dropdownApi` with full CRUD operations
  - REST API endpoints:
    - GET `/api/dropdowns/categories` - Fetch all categories
    - GET `/api/dropdowns/all` - Fetch all dropdown values
    - GET `/api/dropdowns/category/{category}` - Fetch values by category
    - POST `/api/dropdowns` - Create new dropdown value (requires CREATE permission)
    - PUT `/api/dropdowns/{id}` - Update value (requires UPDATE permission)
    - DELETE `/api/dropdowns/{id}` - Delete value (requires DELETE permission)
  - Access control via `@RequirePermission` annotations (USER_MANAGEMENT permissions)
  - Components: FilterSection, AddCategoryDialog, AddValueForm, DropdownTable, EditDialog, DeleteConfirmDialog, StatsSection
  - State management with React hooks (useState, useEffect)
  - Responsive design following Material-UI patterns
  - Automatic subcategory population based on selected category
  - No link to Task Activity List (per React dashboard design)
- **Roles Management** (Phase 7 - February 2026)
  - Comprehensive role and permission management system
  - Material-UI table displaying all roles with grouped permissions
  - Create new roles with hierarchical permission selection
  - Edit existing roles (role name read-only, description and permissions editable)
  - Delete roles with constraint validation (cannot delete assigned roles)
  - Permission grouping by resource (USER_MANAGEMENT, TASK_MANAGEMENT, ROLE_MANAGEMENT, SYSTEM_CONFIGURATION)
  - Master checkboxes for resource-level permission control with indeterminate state
  - Individual permission selection (CREATE, READ, UPDATE, DELETE actions)
  - Real-time validation and error handling
  - TypeScript types for type-safe API integration
  - API client wrapper using Axios
  - Access control via @RequirePermission annotations
  - REST API: `/api/roles` endpoints (GET all, GET by ID, GET permissions, POST create, PUT update, DELETE)
  - DTOs: `RoleDto`, `PermissionDto` for clean data transfer
  - Error handling for constraint violations (role assigned to users)
  - Empty states with informative messages
  - Responsive dialog forms with Material-UI components
- **Notify Users** (February 2026)
  - Admin-only page for sending profile notification emails to active users
  - Displays all active users who have an email address on file
  - Optional last-name prefix filter for narrowing the user list
  - Checkbox table with individual row selection and Select All / Deselect All
  - Selected user count chip displayed inline with action bar
  - Send Profile Notifications button with loading spinner during submission
  - Success alert showing sent count and skipped count (users with no email or not found)
  - Warning banner displayed when email sending is disabled on the server (`spring.mail.enabled=false`)
  - Accessible only to ADMIN role (route protected by `requiredRole="ADMIN"` in React Router and `@RequirePermission` on API)
  - New sidebar entry with Email icon, positioned between Guest Activity and User Dashboard
  - New dashboard card added to DashboardHome for ADMIN users
  - REST API: `GET /api/users/notify-eligible`, `POST /api/users/notify` (both in `UserRestController`)
  - TypeScript types: `NotifyEligibleUser`, `NotifyRequest`, `NotifyResult`
  - API client functions: `fetchNotifyEligibleUsers()`, `sendUserNotifications()` in `userManagement.api.ts`
- **Guest Activity Report** (Phase 6 - February 2026)
  - Real-time login audit tracking for GUEST users
  - Statistics dashboard with 4 metric cards:
    - Total Logins (purple gradient card)
    - Unique Locations (blue gradient card)
    - Last Login (red gradient card with compact date format)
    - Success Rate (green gradient card)
  - Login audit table with sortable columns:
    - Date/Time with full date format for table rows
    - IP Address
    - Location identifier
    - Status (Success/Failed with colored chips)
  - CSV export functionality:
    - Export dialog with Copy to Clipboard button
    - Download CSV with timestamped filename
    - Close button to dismiss dialog
  - Refresh button for manual data reload
  - Responsive Material-UI Grid layout (4-3-3-2 column distribution)
  - Compact date formatting for card displays ("Feb 9, 11:59 AM")
  - In-memory data storage (resets with deployment)
- **Material Design** - Professional, gradient card designs
- **TypeScript types** - Strongly-typed API contracts and component props
- **Error handling** - User-friendly error messages and loading states
- **Empty states** - Informative messages when no data available

### Angular Dashboard Features

- **Task Management** - View, create, edit, clone, and delete tasks
- **User Management** - Admin interface for managing users
  - User list with role badges and status indicators
  - **Account lock status display** - 🔒 indicator for locked accounts
  - **Failed login attempts** - View attempt count in user details
  - **Admin unlock capability** - Unlock locked accounts via checkbox
  - Edit dialog with comprehensive user details
- **Dropdown Management** - Manage system dropdown values
- **Reports & Analytics Dashboard** - Interactive data visualization
  - **8 report components** with Chart.js integration:
    - Overview (summary metrics, top clients, top projects)
    - Client Analysis (time distribution pie chart, top activities)
    - Project Analysis (time by project bar chart, phase donut chart)
    - Time Trends (daily line chart, weekly trends, monthly comparison)
    - **User Analysis** (ADMIN-only: user performance table with trophy rankings, hours by user bar chart)
  - **Role-based data filtering**: ADMIN sees all users, regular users see only their own data
  - Interactive charts with hover tooltips and real-time data
  - Trophy rankings for top 3 performers (🏆 🥈 🥉)
  - **Billable/Non-Billable Hours Tracking**:
    - Flag-based system using `non_billable` column in `dropdownvalues` table
    - BillabilityService evaluates tasks and expenses using component flags
    - Task billability: ALL components (client, project, phase) must be billable
    - Expense billability: ALL components (client, project, type) must be billable
    - Separate columns for billable and non-billable hours in User Performance Summary
    - Color-coded display (green for billable, orange for non-billable)
    - Average billable hours per day calculation
    - Weekly timesheet and expense sheet filtering by billability status (All/Billable/Non-Billable)
    - Visual indicators: 🚫 Non-Billable badge in dropdown management UIs
  - Responsive chart layouts with Material Design
- **Role-based UI** - Different views for ADMIN, USER, and GUEST roles
- **Material Design** - Professional, modern interface
- **Session Management** - Integrated with Spring Security

### Template Engine (Thymeleaf)

- **Thymeleaf** - Server-side HTML template engine
- Thymeleaf Spring Security integration
- Natural templating with HTML5
- Template fragments for reusability
- Expression language for dynamic content

### Client-Side Technologies

- **Vanilla JavaScript** - Modern ES6+ features
- **CSS3** - Custom styling and responsive design
- **HTML5** - Semantic markup
- **AJAX** - Asynchronous form submissions
- **Responsive design** - Mobile-friendly layouts

### Chart.js Integration

- **Chart Components**: 8 specialized chart components for different analytics views
- **Chart Types Used**:
  - Pie charts (time distribution by client)
  - Bar charts (time by project, hours by user)
  - Doughnut charts (phase distribution)
  - Line charts (daily time tracking)
- **Chart Configuration**:
  - Responsive charts that adapt to container size
  - Custom color palettes for professional appearance
  - Interactive tooltips with detailed data
  - Percentage calculations for relative comparisons
  - Legend positioning and formatting
- **Data Aggregation**: Client-side grouping and calculation of metrics from task data
- **Real-time Updates**: Charts reflect current database state

### JavaScript Features Implemented

- Modal dialogs for user interactions
- Form validation and utilities
- Date handling utilities
- Password visibility toggle
- CSRF token management for AJAX requests
- Dynamic DOM manipulation

### Static Assets

- Custom favicon set (multiple sizes)
- Progressive Web App manifest
- CSS modular organization
- JavaScript utility modules

---

## 🐳 Containerization & Orchestration

### Docker

- **Multi-stage Docker builds** for optimized images
- **Docker Compose** - Multi-container orchestration
- **Multiple build profiles** (host-db, local-fast, containerized-db)
- **Non-root container user** for security
- **Health checks** in Docker configurations
- **Volume management** for persistent data and logs
- **Network isolation** with custom networks
- **Environment-based configuration**

### Docker Features

- Optimized layer caching
- JVM memory tuning for containers
- Eclipse Temurin base images
- Separate development and production Dockerfiles
- Docker secrets integration

### Kubernetes (Deployment-Ready)

**Status:** Comprehensive deployment manifests prepared for future Kubernetes deployment to any cloud provider (AWS EKS, Google GKE, Azure AKS) or on-premises cluster.

- Kubernetes deployment manifests with complete application and database configuration
- Service definitions (ClusterIP for internal communication)
- ConfigMaps for non-sensitive configuration
- Secrets for sensitive data (database credentials)
- Resource limits and requests for optimal resource management
- Persistent volume claims for database storage
- RBAC (Role-Based Access Control) definitions for security
- Namespace isolation for multi-tenancy
- Health probes (liveness and readiness) for self-healing
- Ingress configuration for external access with TLS/SSL support
- Multi-replica application deployment for high availability

### Jenkins CI/CD (Deployment-Ready)

**Status:** Comprehensive CI/CD pipeline configuration prepared for Jenkins deployment. Complete Jenkinsfile and supporting documentation ready for integration with any Jenkins server.

- **Declarative Jenkinsfile** - Complete pipeline definition in source control
- **Multi-stage pipeline configuration** - Build, test, Docker build, push to ECR, deploy to ECS
- **Infrastructure deployment stage** - Optional CloudFormation stack deployment
- **Maven integration** - Automated compilation and testing configuration
- **Docker image build steps** - Automated containerization pipeline stages
- **Amazon ECR integration** - Push configuration for Elastic Container Registry
- **ECS Fargate deployment stages** - Service update and deployment automation
- **Health check validation** - Post-deployment health verification steps
- **Automated rollback support** - Rollback procedures on deployment failure
- **Environment-specific deployments** - Support for dev, staging, production environments
- **GitHub webhook integration ready** - Configuration for triggered builds on commits
- **Parallel execution configuration** - Optimized build times with parallel stages
- **Artifact management** - Build artifact archival and retention policies
- **Environment variables management** - Secure credential handling patterns
- **Deployment verification scripts** - ECS task and service validation utilities
- **CloudWatch integration** - Log streaming and monitoring configuration

**Helper Scripts:**
- `check-deployment.sh` - Verify ECS deployment status and health
- `trigger-build.sh` - Trigger Jenkins builds via CLI/API
- `verify-environment.sh` - Validate AWS resources before deployment
- `cleanup-old-images.sh` - Clean up old ECR images to reduce costs

**Documentation:**
- Complete Jenkins setup guide with step-by-step installation instructions
- Environment configuration guide for dev/staging/production
- Quick reference guide for common operations
- Troubleshooting procedures and solutions
- Security best practices for CI/CD pipelines

### AWS CloudFormation (Deployment-Ready)

**Status:** Complete infrastructure-as-code templates and automation scripts prepared for AWS CloudFormation. Ready to deploy entire AWS infrastructure with a single command.

**Infrastructure Template Features:**
- **Complete AWS stack automation** - VPC, subnets, gateways, route tables
- **RDS PostgreSQL provisioning** - Multi-AZ support, automated backups, encryption
- **ECS Fargate cluster setup** - Container orchestration, auto-scaling configuration
- **Application Load Balancer** - Target groups, health checks, listeners
- **ECR repository creation** - Lifecycle policies, image scanning
- **Secrets Manager integration** - Database credentials, admin passwords, Cloudflare config
- **IAM roles and policies** - Task execution roles, task roles, least-privilege access
- **Security groups** - Proper ingress/egress rules for ALB, ECS, RDS
- **CloudWatch log groups** - Application and database logging with retention policies
- **Multi-environment support** - Separate configurations for dev, staging, production

**Deployment Scripts:**
- **PowerShell script** (`deploy-infrastructure.ps1`) - Windows deployment automation
- **Bash script** (`deploy-infrastructure.sh`) - Linux/Mac/Jenkins deployment automation
- **Actions supported:** create, update, delete, status, preview, validate
- **Change set previews** - Review infrastructure changes before applying
- **Automatic rollback** - Stack rollback on deployment failures
- **Production safeguards** - Confirmation required for production operations

**Environment-Specific Features:**
- **Development** - Cost-optimized (db.t3.micro, 256 CPU, 512 MB, single-AZ)
- **Staging** - Production-like (db.t3.small, 512 CPU, 1024 MB, single-AZ)
- **Production** - High availability (Multi-AZ RDS, 2+ tasks, deletion protection)

**Documentation:**
- Comprehensive README with prerequisites and usage guide
- Quick reference for common commands and workflows
- Environment configuration details and cost estimates
- Troubleshooting procedures and best practices
- Migration guide from manual AWS setup

**Stack Outputs:**
- VPC and subnet identifiers
- RDS database endpoint and port
- ECR repository URI
- Load Balancer DNS name and URL
- ECS cluster and service names
- Secrets Manager ARNs

---

## ☁️ AWS Cloud Services

### AWS Services Used

- **Amazon ECS (Elastic Container Service)** - Container orchestration
- **AWS Fargate** - Serverless container compute
- **Amazon ECR (Elastic Container Registry)** - Container image registry
- **Amazon RDS** - Managed PostgreSQL database
- **AWS Secrets Manager** - Centralized secrets management
- **Application Load Balancer (ALB)** - HTTP/HTTPS load balancing (deployment-ready)
- **Amazon VPC** - Network isolation and configuration
- **AWS IAM** - Identity and access management
- **Amazon CloudWatch Logs** - Real-time logging and monitoring
- **Amazon S3** - Long-term log archival with lifecycle management
- **AWS CloudFormation** - Infrastructure as Code automation (deployment-ready)
- **AWS CLI** - Command-line deployment automation

### AWS Deployment Features

- Automated deployment scripts
- CloudFormation infrastructure templates (deployment-ready)
- Task definitions for ECS
- IAM roles for task execution
- Security groups and network configuration
- Environment-specific configurations
- Blue-green deployment capability
- Auto-scaling configuration ready
- Automated IP change monitoring and recovery
- Complete infrastructure automation with CloudFormation

### Cloudflare Integration

- **Cloudflare Tunnel** - Secure HTTPS access without exposing ports
- **DNS Management** - Automated DNS configuration with proxying
- **SSL/TLS** - Automatic certificate management
- **Custom Domain** - Production domain routing (taskactivitytracker.com)
- **Zero Trust Access** - Secure tunnel without VPN or port forwarding
- **Integrated AWS Deployment** - Cloudflared runs in the same ECS container as the application

### Cloudflare Tunnel on AWS Features

- **Container Integration** - Cloudflared binary integrated in application Docker image
- **Startup Orchestration** - Custom docker-entrypoint.sh manages both cloudflared and Spring Boot
- **AWS Secrets Manager** - Tunnel credentials and configuration stored securely
- **Multi-Connection Architecture** - 4 connections to Cloudflare edge servers (iad region)
- **Zero Maintenance** - Tunnel runs 24/7 with no laptop dependency
- **Automatic Failover** - ECS health checks and automatic container recovery
- **Centralized Logging** - Tunnel logs integrated with CloudWatch Logs
- **Production Ready** - Both domains verified (taskactivitytracker.com and www)
- **Localhost Communication** - Tunnel connects to application via localhost:8080 (internal)
- **No Port Exposure** - No inbound ports required, secure outbound-only connections

---

## 🧪 Testing

### Testing Frameworks

- **JUnit 5** - Modern unit testing framework
- **Mockito** - Mocking framework for unit tests
- **Spring Boot Test** - Integration testing support
- **Spring Security Test** - Security testing utilities
- **MockMvc** - Web layer testing
- **@DataJpaTest** - Repository layer testing
- **@WebMvcTest** - Controller layer testing
- **@SpringBootTest** - Full application context testing

### Testing Approaches

- Unit tests for service layer
- Repository integration tests
- Controller web layer tests
- Security configuration testing
- Mock-based isolated testing
- Test data builders
- Parameterized tests

---

## 📝 Application Architecture & Design Patterns

### Architectural Patterns

- **Layered Architecture** - Clear separation of concerns
- **MVC Pattern** - Model-View-Controller
- **Repository Pattern** - Data access abstraction
- **Service Layer Pattern** - Business logic encapsulation
- **DTO Pattern** - Data transfer objects for API contracts
- **Dependency Injection** - Spring IoC container
- **Singleton Pattern** - Spring-managed beans

### Code Organization

- Package-by-feature structure
- Separation of concerns across layers
- Entity, Repository, Service, Controller separation
- Configuration classes for cross-cutting concerns
- Custom validators and validation logic

### Design Principles Applied

- SOLID principles
- DRY (Don't Repeat Yourself)
- Single Responsibility Principle
- Dependency Inversion
- Interface-based programming

---

## 🔧 Development Tools & Practices

### Version Control

- Git for source control
- GitHub for repository hosting
- Branching strategies
- Comprehensive documentation

### Development Environment

- Multi-environment configuration (local, docker, aws)
- Environment variable management
- Profile-based configuration
- Externalized properties

### Logging & Monitoring

- **Logback** - Logging framework
- **SLF4J** - Logging facade
- Conditional logging configuration with Janino
- Environment-specific log levels
- File and console logging (local/Docker environments)
- **Amazon CloudWatch Logs** - Real-time cloud logging for AWS deployments
- Log rotation and archival
- Structured logging with context

### CloudWatch Logs (AWS)

- **Real-time Monitoring** - Live log streaming from ECS Fargate containers
- **30-Day Retention** - Automatic log retention management in CloudWatch
- **Log Group** - /ecs/taskactivity for centralized application logs
- **awslogs Driver** - Native Docker logging integration
- **Log Stream per Task** - Separate streams for each ECS task instance
- **CloudWatch Insights** - Query and analyze logs with AWS Insights

### S3 Log Archival

- **Amazon S3 Bucket** - Long-term log storage (taskactivity-logs-archive)
- **Lifecycle Policies** - Automated cost optimization
  - 90 days: Transition to Glacier Flexible Retrieval
  - 365 days: Transition to Glacier Deep Archive
- **96% Cost Savings** - Deep Archive reduces storage costs significantly
- **Lambda Automation** - AWS Lambda function for automated daily exports
  - Triggered by EventBridge Scheduler at 2:00 AM UTC daily
  - Serverless, no PC dependency
  - Exports previous day's logs automatically
- **Manual Export Scripts** - PowerShell scripts for on-demand exports
  - `export-logs-to-s3.ps1` - Manual CloudWatch to S3 exports
- **IAM Security** - Fine-grained permissions for log exports and access
- **Cost-Effective** - ~$3.06/month for 10GB logs with lifecycle policies
- **Date-Organized** - Logs organized by export date (YYYY-MM-DD)

### Build & Deployment

- Maven lifecycle management
- Automated build scripts
- Docker build optimization
- Multi-stage builds for smaller images
- Deployment automation scripts
- Health check monitoring scripts

---

## 🌐 API & Integration Features

### RESTful API

- REST endpoints for CRUD operations
- JSON request/response handling
- HTTP method-based routing (GET, POST, PUT, DELETE)
- Proper HTTP status codes
- Content negotiation
- API versioning ready

### API Documentation (Swagger/OpenAPI)

- **Springdoc OpenAPI 2.6.0** - OpenAPI 3.0 specification implementation
- **Swagger UI** - Interactive API documentation interface
- **Auto-generated documentation** from Spring MVC controllers
- **Security scheme documentation** (Basic Authentication)
- **Multiple server environments** configuration (local, production)
- **API metadata** (title, version, description, contact, license)
- **Try-it-out functionality** for testing endpoints directly
- **OpenAPI JSON/YAML export** for client generation
- **Customizable UI** with operation and tag sorting
- **Integration with Spring Security** for authenticated testing
- **Public access in development**, restrictable in production
- **Support for annotations** (@Operation, @ApiResponse, @Schema) for enhanced documentation

**Access Points:**
- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- OpenAPI YAML: `/v3/api-docs.yaml`

### Data Serialization

- **Jackson** - JSON processing library
- JSR310 module for Java 8+ date/time types
- Custom serialization/deserialization
- LocalDate handling in JSON

### CORS (Cross-Origin Resource Sharing)

- Configurable allowed origins
- Support for credentials
- Pre-flight request handling
- Production-ready CORS configuration

---

## 📦 Data Validation

### Bean Validation (JSR-303/JSR-380)

- Field-level validation annotations
- Custom validators
- Validation groups
- Method parameter validation
- Return value validation

### Validation Features

- @NotNull, @NotBlank, @Size constraints
- @DecimalMin, @DecimalMax for numeric validation
- Custom password validation logic
- Centralized validation constants
- Client and server-side validation coordination

---

## 🚀 Production Features

### Production Readiness

- Health check endpoints
- Graceful shutdown
- JVM tuning for production
- Memory optimization
- Production logging configuration
- Error handling and recovery
- Monitoring and metrics

### Automation Scripts

- Production setup automation
- Secret rotation scripts
- Backup and recovery scripts
- Health monitoring scripts
- Docker secret management
- Deployment automation
- **CloudWatch log export automation** - Scheduled exports to S3
- **S3 log browsing and download** - PowerShell helper scripts

### Multi-Environment Support

- Local development configuration (PostgreSQL)
- Docker container configuration (containerized or host database)
- Kubernetes deployment-ready (manifests prepared)
- AWS cloud deployment (ECS Fargate with RDS)
- Profile-based environment switching

---

## 📊 Application Features

### Business Functionality

**Task Activity Tracking:**
- Task activity tracking and management
- Weekly timesheet view with calculations
- Client, project, and phase categorization
- Task cloning for repetitive entries
- Search and filtering capabilities
- Date range queries
- CSV export for reporting

**Expense Management:**
- Business expense tracking with client/project association
- Multi-step approval workflow (Draft → Submitted → Approved/Rejected → Reimbursed)
- Receipt upload and storage (local filesystem or AWS S3 ready)
- Role-based expense administration (EXPENSE_ADMIN role)
- Comprehensive filtering by type, status, payment method, date range
- Weekly expense sheet view
- Reimbursement tracking with approval notes
- CSV export for expense reports

**Administrative Features:**
- Dropdown value management system
- User management and administration
- Role-based feature access (ADMIN, USER, GUEST, EXPENSE_ADMIN)
- Data aggregation and reporting
- Interactive API documentation (Swagger UI)

### User Interface

- Responsive web design with mobile-friendly layouts
- **Sidebar Menu Navigation** (January 2026):
  - Floating toggle button (☰/✕) for accessing administrative functions
  - Slide-in sidebar menu with role-based item visibility
  - Menu items: Manage Users, Guest Activity, Manage Dropdowns, Export CSV
  - Auto-close on Export CSV selection for improved UX
  - CSRF token management for secure logout functionality
- Compact CSS Grid layouts for filters
- Modal-based data entry
- AJAX form submissions
- Client-side validation
- User-friendly error messages
- Dynamic date calculations
- Accessible HTML markup
- Receipt image preview and download

---

## 🔄 DevOps & CI/CD Readiness

### Infrastructure as Code

- Docker Compose configurations for local and containerized deployments
- Kubernetes YAML manifests (deployment-ready for any K8s cluster)
- AWS ECS task definitions for Fargate deployment
- Automated deployment scripts for AWS

### Deployment Strategies

- Blue-green deployment support
- Rolling updates capability
- Zero-downtime deployments
- Health-check based routing

### Monitoring & Observability

- Application health endpoints
- Structured logging
- **CloudWatch Logs** - Real-time cloud logging with 30-day retention
- **S3 Log Archival** - Long-term storage with lifecycle policies (90d→Glacier, 365d→Deep Archive)
- **Log Export Automation** - PowerShell scripts for scheduled CloudWatch to S3 exports
- Metrics collection via Actuator
- Cost-optimized logging (~$3/month for 10GB with archival)

---

## 🛡️ Enterprise Features

### Scalability

- Stateless application design
- Horizontal scaling ready
- Container orchestration support
- Database connection pooling

### High Availability

- Multi-instance deployment support
- Load balancer integration
- Session management for distributed systems
- Database high availability ready

### Security Best Practices

- Principle of least privilege
- Defense in depth
- Secure defaults
- Input validation and sanitization
- Output encoding
- Secure session management
- Audit logging capabilities

---

## 📚 Documentation

### Documentation Types

- User guides
- Developer guides
- Deployment guides
- Operations runbooks
- Quick start guides
- Architecture documentation
- Interactive API documentation (Swagger UI)
- Learning guides
- Troubleshooting guides
- AWS deployment guides

### Code Documentation

- JavaDoc comments
- Inline educational comments
- README files
- Configuration examples
- Script documentation

---

## 💡 Key Technical Accomplishments

1. **Full-Stack Enterprise Application** - Complete web application from database to UI
2. **Multi-Platform Deployment** - Docker (active), AWS ECS (active), Kubernetes (deployment-ready)
3. **Enterprise Security** - Comprehensive authentication and authorization with RBAC
4. **Production-Ready** - Health checks, monitoring, and automation
5. **Scalable Architecture** - Designed for horizontal scaling
6. **Modern Java Practices** - Java 21, Spring Boot 3.x, modern patterns
7. **DevOps Integration** - Containerization, orchestration, and automation
8. **Secrets Management** - Enterprise-grade credential handling across all platforms
9. **Comprehensive Testing** - Unit, integration, and security tests
10. **Professional Documentation** - Extensive guides and runbooks
11. **Self-Healing Infrastructure** - Automated monitoring and recovery for cloud deployments
12. **Cloudflare Zero Trust** - Secure HTTPS access with automatic failover

---

## 🎯 Interview Talking Points

### Backend Development

- Designed and implemented RESTful APIs using Spring Boot
- Integrated interactive API documentation with Swagger/OpenAPI (Springdoc)
- Architected layered application following SOLID principles
- Implemented comprehensive security with Spring Security
- Managed database persistence with Spring Data JPA and PostgreSQL
- Applied transaction management and data validation

### Cloud & DevOps

- Containerized application using Docker with multi-stage builds
- Orchestrated containers with Docker Compose for local/development environments
- Created comprehensive Kubernetes deployment manifests for enterprise orchestration
- Deployed to AWS using ECS Fargate with Application Load Balancer
- Implemented secrets management across Docker, Kubernetes, and AWS platforms
- Created automation scripts for deployment, monitoring, and operations
- **Integrated Cloudflare Tunnel in AWS ECS** - Cloudflared runs in same container as application for 24/7 secure HTTPS access
- Built production-ready infrastructure with integrated tunnel eliminating laptop dependency
- Configured multi-connection Cloudflare architecture with automatic failover
- Implemented cost-optimization strategies with pause/resume capabilities
- **Configured CloudWatch Logs** - Real-time logging with 30-day retention for AWS deployments
- **Implemented S3 Log Archival** - Long-term storage with lifecycle policies for cost optimization
- **Created Log Export Automation** - PowerShell scripts for scheduled CloudWatch to S3 exports

### Security

- Implemented role-based access control (RBAC) with ADMIN, USER, and GUEST roles
- Configured authentication and authorization with Spring Security
- Applied security best practices including CSRF protection
- Managed credentials securely using Docker Swarm secrets, Kubernetes secrets, and AWS Secrets Manager
- Implemented password policies and secure session handling

### Architecture & Design

- Applied layered architecture with clear separation of concerns
- Utilized dependency injection and inversion of control
- Implemented repository and service layer patterns
- Designed DTOs for clean API contracts
- Applied enterprise design patterns throughout the application

### Testing

- Wrote comprehensive unit tests with JUnit 5 and Mockito
- Implemented integration tests for repositories and services
- Created security-aware tests with Spring Security Test
- Used MockMvc for web layer testing
- Achieved good test coverage across all layers

### Full-Stack Development

- Developed server-side rendered templates with Thymeleaf
- Implemented responsive UI with modern JavaScript
- Created AJAX-based interactions for better UX
- Integrated frontend security with backend authentication
- Managed form validation on both client and server sides

---

