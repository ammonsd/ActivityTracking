# Task Activity Tracking & Expense Management

A comprehensive web application built with Spring Boot, Angular, and PostgreSQL for tracking billable hours and managing business expenses. Designed for consultants, contractors, and teams who need professional time tracking with integrated expense management and receipt storage.

## Features

### Time Tracking

- ✅ Daily task recording with client/project/phase tracking
- 📊 Analytics & Reports Dashboard with interactive charts and visualizations
  - Time distribution by client and project
  - Daily/weekly/monthly time tracking visualizations
  - Phase distribution analysis
  - ADMIN-only user performance analytics
- 📊 Weekly timesheet view (Monday-Sunday format)
- 📥 Export filtered tasks and weekly timesheets to CSV format
- � **Bulk CSV import** for TaskActivity records (ADMIN/MANAGER roles)
- �🔍 Filter and search capabilities

### Expense Management

- 💰 Travel and business expense tracking with receipt management
- 📸 Receipt upload/download (JPEG, PNG, PDF validated via magic number signatures) with AWS S3 or local storage
- 💳 Payment method and vendor tracking
- 📋 Expense categorization by type (travel, meals, office supplies, etc.)
- ✅ Multi-stage approval workflow (Draft → Submitted → Approved/Rejected → Reimbursed)
- 📧 **Email notifications for expense status changes** (AWS SES integration):
  - Automatic notifications when expense status changes (submitted, approved, rejected, reimbursed)
  - Includes expense details, status change reason, and approval notes
  - Requires user email address configured in profile
- 👔 Role-based access control:
  - **USER/GUEST**: Create, view, and submit own expenses
  - **EXPENSE_ADMIN**: Approve/reject expenses and mark as reimbursed
  - **ADMIN**: Full expense management and approval authority
- 📊 Expense filtering by client, project, type, status, and date range
- 💵 Automatic expense totals and status tracking
- � **Bulk CSV import** for Expense records (ADMIN/MANAGER roles)
- �🔒 Users can only modify Draft, Submitted, or Resubmitted status
- 🚫 Non-admins cannot modify approval/reimbursement fields

### Profile Management

- 👤 **My Profile**: Self-service profile management for non-admin users
  - **Angular UI**: Modern Material Design profile editor accessible from dashboard and side menu
  - **Backend UI**: Thymeleaf-based profile editor with success/error notifications
  - Users can update their own first name, last name, company, and email
  - Email address required for expense management features
  - Password changes available via dedicated Change Password page
  - Profile updates return to My Profile with confirmation message
  - Password changes redirect back to My Profile after completion
- 🔐 Secure password management with 90-day expiration policy
- 🔒 Account lockout protection (5 failed login attempts)

### General Features

- 🎯 Dynamic dropdown management for clients, projects, phases, and expense types
- ✔️ Data validation and error handling
- 📚 Comprehensive API documentation (Swagger/OpenAPI)

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.5.7 (MVC + Thymeleaf), Spring Data JPA
- **Frontend:** Angular 19, Angular Material, TypeScript, Chart.js
- **Database:** PostgreSQL 15+
- **Storage:** AWS S3 (production) / Local file system (development) for receipt storage
- **API Documentation:** Springdoc OpenAPI 2.7.0 (Swagger UI)
- **Build:** Maven, npm
- **Testing:** JUnit 5 (290+ tests), Testcontainers, Karma/Jasmine
- **Deployment:** Docker, AWS ECS (optional)

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ (or Docker)

### Database Setup

The application automatically creates tables and populates initial data on startup. The database includes:

**Tables:**

- `users` - User accounts and authentication
- `taskactivity` - Time tracking records
- `dropdownvalues` - Dynamic dropdown values for clients, projects, phases, and expense types
- `expenses` - Expense records with approval workflow

**Initial Data:**

- Default admin user (username: `admin`, password: `admin123`)
- Sample clients, projects, and phases
- Expense types (travel, meals, office supplies, etc.)
- Expense statuses (Draft, Submitted, Approved, Rejected, Reimbursed)
- Payment methods and receipt statuses

⚠️ **Security:** Change the default admin password immediately after first login!

**Built-in Security Features:**

- **Account Lockout**: 5 failed login attempts locks account (admin can unlock)
- **Rate Limiting**: 5 authentication requests per minute per IP address
- **Security Headers**: X-Frame-Options, CSP, HSTS, Referrer-Policy, Permissions-Policy
- **JWT Authentication**: Secure token-based API authentication with configurable expiration
- **JWT Token Revocation**: Server-side token blacklisting on logout and password change
- **File Upload Security**: Magic number validation prevents malicious file uploads (JPEG/PNG/PDF signatures verified)
- **Password Security**: Password hashes removed from debug logs, BCrypt hashing
- **Docker Secrets**: Production uses secrets for all sensitive credentials
- **Disabled by Default**: Swagger UI and detailed Actuator endpoints require explicit enablement

### Run Locally

```powershell
# Clone the repository
git clone https://github.com/ammonsd/ActivityTracking.git
cd ActivityTracking

# Run with Maven
.\mvnw.cmd spring-boot:run

# Or run with Docker
docker-compose --profile host-db up -d
```

Access the application at **http://localhost:8080**

### Configuration

#### Required Environment Variables

The application requires the following environment variables to be set:

**Security (Required):**

```bash
# JWT authentication configuration
export JWT_SECRET=$(openssl rand -base64 32)  # Generate secure 256-bit key
export APP_ADMIN_INITIAL_PASSWORD="YourSecure123!Pass"  # Min 12 chars, mixed case, numbers, special chars

# Database credentials
export DB_USERNAME="your_db_user"
export DB_PASSWORD="your_db_password"
```

**Optional JWT Customization:**

```bash
export JWT_EXPIRATION=86400000        # Access token lifetime in ms (default: 24 hours)
export JWT_REFRESH_EXPIRATION=604800000  # Refresh token lifetime in ms (default: 7 days)
```

⚠️ **Security Note:** The application will fail to start if `JWT_SECRET` is not set or uses an insecure default value.

#### Receipt Storage Configuration

The application supports two storage backends for expense receipts:

**Local File Storage (Development):**

```properties
# In application.properties or application-local.properties
storage.type=local
storage.local.base-path=./receipts
```

**AWS S3 Storage (Production):**

```properties
# In application.properties or application-aws.properties
storage.type=s3
storage.s3.bucket-name=your-bucket-name
storage.s3.region=us-east-1
```

For AWS deployment, ensure:

- S3 bucket is created with appropriate permissions
- ECS task role has S3 read/write permissions
- See [AWS Deployment Guide](aws/AWS_Deployment.md) for complete setup

### API Documentation

⚠️ **Security Note:** Swagger UI is **disabled by default** for security. To enable for development/testing:

```properties
# In application-local.properties or set environment variable
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
```

Interactive API documentation is available via Swagger UI when enabled:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

📘 **[Swagger API Guide](docs/Swagger_API_Guide.md)** - Complete guide for using the REST API with JWT authentication

#### Key API Endpoints

**Authentication:**

- `/api/auth/login` - User login with JWT token generation
- `/api/auth/logout` - Logout with server-side token revocation
- `/api/auth/refresh` - Refresh access token

**Time Tracking:**

- `/api/task-activities` - Task CRUD operations, filtering, and reports
- `/api/import/taskactivities` - Bulk import TaskActivity records from CSV
- `/api/import/taskactivities/template` - Get CSV template information
- `/api/dropdowns` - Client, project, and phase management

**Expense Management:**

- `/api/expenses` - Expense CRUD operations, filtering, and status management
- `/api/expenses/{id}/submit` - Submit expense for approval
- `/api/expenses/{id}/approve` - Approve expense (Admin/Expense Admin only)
- `/api/import/expenses` - Bulk import Expense records from CSV
- `/api/import/expenses/template` - Get CSV template information

**User Profile Management:**

- `/api/users/me` - Get current user information
- `/api/users/profile` - Get/update current user's profile (USER, ADMIN, EXPENSE_ADMIN)
- `/profile/edit` - Backend My Profile page (Thymeleaf UI)
- `/change-password` - Password change page with redirect to My Profile
- `/api/expenses/{id}/reject` - Reject expense (Admin/Expense Admin only)
- `/api/expenses/{id}/reimburse` - Mark as reimbursed (Admin/Expense Admin only)
- `/api/expenses/pending-approvals` - View pending expenses for approval
- `/api/receipts/{expenseId}` - Upload, download, and delete receipts

## Documentation

### Core Guides

- 👨‍💻 [Developer Guide](docs/Developer_Guide.md) - Complete technical reference and development workflow
- 📖 [User Guide](docs/User_Guide.md) - End-user documentation for daily task tracking and expense management
- 🔐 [Administrator User Guide](docs/Administrator_User_Guide.md) - Admin features, user management, expense approvals, and 12-Factor App compliance
- � [CSV Import User Guide](docs/CSV_Import_User_Guide.md) - Bulk data import via CSV files (NEW)
- 🔒 [Security Measures and Best Practices](docs/Security_Measures_and_Best_Practices.md) - Comprehensive security documentation

### Docker & Containerization

- 📦 [Docker Build Guide](docs/Docker_Build_Guide.md) - Complete Docker containerization guide
- 🚀 [Docker Quick Start](docs/Docker_Quick_Start.md) - Fast Docker setup for local development

### AWS Deployment

- ☁️ [AWS Deployment Guide](aws/AWS_Deployment.md) - AWS ECS Fargate deployment
- 🏗️ [CloudFormation Guide](cloudformation/ReadMe.md) - Infrastructure as Code automation (deployment-ready)
- 📋 [AWS Console Guide](aws/AWS_Console_Guide.md) - Manual AWS setup via console

### Kubernetes

- ⚓ [Kubernetes Deployment Guide](k8s/ReadMe.md) - K8s manifests with RBAC and secrets management (deployment-ready)

### CI/CD & Automation

- 🚀 [Jenkins CI/CD Guide](jenkins/ReadMe.md) - Continuous integration and deployment pipeline (deployment-ready)

### Architecture & Design

- 📊 [Technical Features Summary](docs/Technical_Features_Summary.md) - Comprehensive feature list
- ⚖️ [Concurrency and Scaling Guide](docs/Concurrency_and_Scaling_Guide.md) - Horizontal scaling, load balancing, and concurrency strategies

## License

This project is for personal/educational use.

© 2025-2026 | Dean Ammons
