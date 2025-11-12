# Task Activity Management System - Developer Guide

## Overview

The Task Activity Management System is a Spring Boot web application designed to help individuals and teams track time spent on various tasks. This guide provides technical documentation for developers working on the system.

**Technology Stack:**

- Java 21
- Spring Boot 3.5.6
- Spring Data JPA
- Spring Security
- Thymeleaf templating
- PostgreSQL database
- Maven build system
- Docker support
- Springdoc OpenAPI (Swagger) for API documentation

## Technology Stack

### Backend Framework

- **Spring Boot 3.5.6**: Main application framework
- **Spring Data JPA**: Database access and ORM
- **Spring Security**: Authentication and authorization
- **Jakarta Validation**: Input validation
- **HikariCP**: Connection pooling
- **Springdoc OpenAPI 2.6.0**: Interactive API documentation (Swagger UI)

### Frontend

The application provides two user interface options:

#### Thymeleaf (Server-Side Rendered UI)
- **Thymeleaf**: Server-side templating engine
- **HTML5/CSS3**: Modern web standards
- **CSS Architecture**: Modular external stylesheets
  - `base.css` - Core layout and navigation styles
  - `components.css` - Reusable UI components
  - `login.css`, `timesheet.css` - Page-specific styles
  - Browser-cached for improved performance
- **JavaScript**: Client-side interactions
  - Modular utility libraries (modal-utils, password-toggle, date-utils, form-utils)
  - Browser-cached static resources for improved performance
  - Reusable components across templates
- **Access**: http://localhost:8080

#### Angular (Modern SPA)
- **Angular 19**: Latest version with standalone components architecture
- **TypeScript 5.6+**: Type-safe development
- **Angular Material**: Material Design component library
- **RxJS**: Reactive programming with Observables
- **HTTP Client**: RESTful API integration with interceptors
- **Authentication**: HTTP Basic Auth with session storage
- **Routing**: Angular Router with auth guards
- **Access**: http://localhost:4200
- **Location**: `frontend/` directory
- **Development Server**: `npm start` (runs on port 4200)

Both UIs connect to the same Spring Boot backend REST API and share authentication.

### Database

- **PostgreSQL 15+**: Primary database
- **Schema**: dbo
- **Connection Pool**: Up to 20 connections

### Build Tools

- **Maven**: Dependency management and build
- **Maven Wrapper**: Included for version consistency

### Containerization

- **Docker**: Container runtime
- **Docker Compose**: Multi-container orchestration
- **Cloudflare Tunnel (cloudflared)**: Integrated in Docker container for secure HTTPS access

### CI/CD (Deployment-Ready)

- **Jenkins**: CI/CD pipeline configuration and documentation
- **Jenkinsfile**: Declarative pipeline for automated builds and deployments
- **Helper Scripts**: Deployment verification and management utilities

### Infrastructure as Code (Deployment-Ready)

- **AWS CloudFormation**: Infrastructure automation templates
- **CloudFormation Templates**: Complete AWS infrastructure provisioning (VPC, RDS, ECS, ECR, ALB, Secrets Manager, IAM, Security Groups, CloudWatch)
- **Multi-Environment Support**: Dev, staging, and production configurations
- **Deployment Scripts**: PowerShell and Bash automation for stack management

### Cloud Services (AWS)

- **Amazon ECS (Fargate)**: Serverless container orchestration
- **Amazon RDS (PostgreSQL)**: Managed database service
- **AWS Secrets Manager**: Credential and secrets management
- **Amazon CloudWatch Logs**: Real-time application logging
- **Amazon S3**: Long-term log archival
- **Amazon ECR**: Container image registry
- **Cloudflare Tunnel**: Secure HTTPS access integrated in ECS container

## Documentation

This guide is part of a comprehensive documentation set for the Task Activity Management System. All documentation is located in the `docs/` directory.

### Available Documentation

- [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) (this document) - Complete technical reference for developers
- [QUICK_START.md](QUICK_START.md) - Fast setup guide for daily development workflow
- [USER_GUIDE.md](USER_GUIDE.md) - End-user documentation for application features
- [ADMINISTRATOR_USER_GUIDE.md](ADMINISTRATOR_USER_GUIDE.md) - Admin-specific features and user management
- [DOCKER_BUILD_GUIDE.md](DOCKER_BUILD_GUIDE.md) - Containerization and Docker deployment
- [AWS_DEPLOYMENT.md](AWS_DEPLOYMENT.md) - AWS ECS Fargate deployment guide
- [CloudFormation README](../cloudformation/README.md) - Infrastructure as Code automation (deployment-ready)
- [JENKINS_CI_CD_README.md](JENKINS_CI_CD_README.md) - Jenkins CI/CD pipeline documentation
- [JENKINS_SETUP.md](JENKINS_SETUP.md) - Jenkins installation and configuration
- [JENKINS_ENVIRONMENTS.md](JENKINS_ENVIRONMENTS.md) - Environment-specific Jenkins setup
- [JENKINS_QUICK_REFERENCE.md](JENKINS_QUICK_REFERENCE.md) - Jenkins quick reference guide
- [WSL_PORT_FORWARDING.md](WSL_PORT_FORWARDING.md) - Network configuration for WSL2 development
- [HELPER_SCRIPTS_README.md](HELPER_SCRIPTS_README.md) - AWS helper scripts for log management
- [SWAGGER_API_GUIDE.md](SWAGGER_API_GUIDE.md) - Complete REST API usage guide with JWT authentication

### Related Resources

- **API Documentation**: Available via Swagger UI at http://localhost:8080/swagger-ui.html
- **OpenAPI Specification**: http://localhost:8080/v3/api-docs
- **Swagger API Guide**: Comprehensive guide for REST API usage with JWT authentication
- **Scripts Directory**: Production automation scripts in `scripts/` folder
- **SQL Scripts**: Database schema files in `sql/` folder
- **Local Documentation**: Additional guides in `localdocs/` folder (gitignored, environment-specific)

## System Requirements

### Development Environment

- **Java Development Kit (JDK)**: Version 21 or higher
- **Maven**: Version 3.6+ (or use included wrapper)
- **PostgreSQL**: Version 15 or higher
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code (recommended)
- **Docker**: For containerized development/deployment
- **Git**: Version control

### Hardware Requirements

- **Minimum**: 2GB RAM, 1GB disk space
- **Recommended**: 4GB RAM, 2GB disk space
- **Network**: Connectivity for database and external dependencies

## Installation and Setup

### Option 1: Local Development Setup

1. **Clone the Repository**
   
   ```bash
   git clone https://github.com/ammonsd/ActivityTracking.git
   cd ActivityTracking
   ```

2. **Set up PostgreSQL Database**
   
   ```bash
   # Create database
   createdb -U postgres AmmoP1DB
   
   # Execute schema script
   psql -U postgres -d AmmoP1DB -f src/main/resources/schema.sql
   psql -U postgres -d AmmoP1DB -f src/main/resources/data.sql
   ```

3. **Configure Database Connection**
   Edit `src/main/resources/application-local.properties`:
   
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/AmmoP1DB
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

4. **Build the Application**
   
   ```bash
   # Using Maven wrapper (recommended)
   ./mvnw clean package
   
   # Or on Windows
   mvnw.cmd clean package
   ```

5. **Run the Application**
   
   ```bash
   java -jar target/taskactivity-0.0.1-SNAPSHOT.jar
   ```
   
    Or run directly with Maven:
   
   ```bash
   ./mvnw spring-boot:run
   ```

6. **Access the Application**
    Open your browser and navigate to:
   
   - `http://localhost:8080` - Local access
   - `http://<YOUR_IP>:8080` - Network access (after WSL2 port forwarding setup)

### Option 2: Docker Development

#### Docker Compose Profiles

The application supports multiple Docker Compose profiles for different development scenarios:

| Profile            | Build Time | Database                 | Use Case                                    |
| ------------------ | ---------- | ------------------------ | ------------------------------------------- |
| `host-db`          | ~90-120s   | Host PostgreSQL          | Standard development with existing database |
| `containerized-db` | ~90-120s   | Containerized PostgreSQL | Full stack in containers                    |
| `local-fast`       | ~7-10s     | Host PostgreSQL          | **Rapid development iterations**            |
| `production`       | ~90-120s   | Containerized PostgreSQL | Production deployment with secrets          |

#### Profile 1: `host-db` (Standard Development)

Uses multi-stage Dockerfile with Maven build inside Docker. Connects to PostgreSQL running on the host machine.

**Usage:**

```bash
# Set environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/AmmoP1DB

# Build and run
docker compose --profile host-db build
docker compose --profile host-db up -d

# View logs
docker compose logs -f app

# Stop
docker compose --profile host-db down
```

#### Profile 2: `containerized-db` (Full Containerized Stack)

Runs both the application and PostgreSQL database in containers. Ideal for isolated development environments.

**Usage:**

```bash
# Set environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export ENABLE_FILE_LOGGING=true

# Build and run
docker compose --profile containerized-db build
docker compose --profile containerized-db up -d

# View logs
docker compose logs -f app-with-postgres
docker compose logs -f postgres

# Stop and remove volumes (fresh database)
docker compose --profile containerized-db down -v
```

#### Profile 3: `local-fast` (Rapid Development)

**Purpose:** Optimized for developers making frequent code changes. Uses pre-built JAR file instead of building inside Docker, reducing rebuild time from ~120 seconds to ~7-10 seconds.

**When to Use:**

- ✅ Active coding sessions with frequent changes
- ✅ Quick feedback loops during development
- ✅ IDE-integrated development (IDE compiles code)
- ✅ Iterative testing and debugging

**When NOT to Use:**

- ❌ First-time setup (requires initial Maven build)
- ❌ Dependency changes (need full Maven build)
- ❌ CI/CD pipelines (use standard profiles)

**Prerequisites:**

1. PostgreSQL running on host machine
2. JAR file already built locally

**Usage:**

**Step 1: Build JAR locally** (only needed when code/dependencies change)

```bash
# Windows
.\mvnw.cmd clean package -DskipTests

# Linux/WSL
./mvnw clean package -DskipTests
```

**Step 2: Build Docker image** (fast - just copies JAR)

```bash
# Set environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/AmmoP1DB

# Build image (7-10 seconds)
docker compose --profile local-fast build
```

**Step 3: Run container**

```bash
docker compose --profile local-fast up -d

# View logs
docker compose logs -f app-local-fast

# Stop (no auto-restart in this profile)
docker compose --profile local-fast down
```

**Development Workflow:**

```bash
# Morning: Initial setup
mvnw.cmd clean package -DskipTests
docker compose --profile local-fast build

# Throughout the day: Quick iterations
# 1. Make code change in IDE
# 2. IDE auto-compiles (or run: mvnw.cmd compile)
# 3. Package: mvnw.cmd package -DskipTests
# 4. Quick rebuild: docker compose --profile local-fast build (7-10s)
# 5. Restart: docker compose --profile local-fast up -d
# 6. Test immediately!
```

**Key Differences from Other Profiles:**

- Uses `Dockerfile.local` instead of multi-stage `Dockerfile`
- Assumes JAR is pre-built in `target/` directory
- No Maven build inside Docker (just copies existing JAR)
- `restart: no` - container doesn't auto-restart on failure (development mode)
- Significantly faster rebuild times for iterative development

#### Profile 4: `production` (Production Deployment)

Uses Docker secrets for credential management. See [Production Configuration](#production-configuration) section.

#### General Docker Commands

**Build Docker Image Only**

```bash
docker build -t task-activity:dev .
```

**View All Containers**

```bash
docker compose ps -a
```

**Remove All Containers and Volumes**

```bash
docker compose --profile host-db down -v
docker compose --profile containerized-db down -v
docker compose --profile local-fast down
```

### Option 3: WSL2 Docker Development (Windows)

**For Windows developers using WSL2 and Docker Engine (not Docker Desktop):**

WSL2 requires special networking configuration to access the application from your Windows network IP. See the [WSL2 Docker Guide](WSL2_DOCKER_GUIDE.md) for complete setup instructions.

**Quick Setup:**

1. **Install Docker in WSL2** (one-time setup)
   
   ```bash
   # In WSL2
   cd /mnt/c/Users/YourUsername/GitHub/ActivityTracking
   chmod +x install-docker-root.sh
   ./install-docker-root.sh
   ```

2. **Configure PostgreSQL for WSL2 Access**
   
   - Edit `C:\Program Files\PostgreSQL\17\data\pg_hba.conf`
   - Add: `host all all 172.27.0.0/16 md5`
   - Restart PostgreSQL service

3. **Start the Application**
   
   ```bash
   # In WSL2
   ./scripts/start-wsl2.sh
   ```

4. **Configure Network Access** (run PowerShell as Administrator)
   
   ```powershell
   cd C:\Users\YourUsername\GitHub\ActivityTracking
   .\scripts\setup-wsl-port-forward.ps1
   ```

**Access the Application:**

- `http://localhost:8080` - Works immediately
- `http://<YOUR_WINDOWS_IP>:8080` - Works after port forwarding setup
- `http://<YOUR_IP>:8080` - From other devices on your network

**Important Notes:**

- WSL IP addresses change when WSL restarts
- If network access stops working after reboot, run `.\scripts\update-wsl-port-forward.ps1` as Administrator
- See [WSL Port Forwarding Guide](..\docsWSL_PORT_FORWARDING.md) for detailed troubleshooting

## Configuration

### Application Properties

The application supports multiple Spring profiles for different environments:

#### Default Profile (`application.properties`)

```properties
server.port=8080
server.address=0.0.0.0
spring.application.name=taskactivity

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/AmmoP1DB
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:N1ghrd01-1948}

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Logging
# Available log levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF
logging.level.com.ammons.taskactivity=DEBUG
```

**Network Binding:**

- `server.address=0.0.0.0` - Binds to all network interfaces, allowing access from:
  - `localhost:8080` - Local machine
  - `<YOUR_IP>:8080` - Network access (requires firewall/port forwarding for WSL2)
- For production deployments, AWS/cloud infrastructure handles network routing automatically

#### Docker Profile (`application-docker.properties`)

```properties
spring.datasource.url=jdbc:postgresql://db:5432/AmmoP1DB
# Optimized for container networking
```

#### Local Profile (`application-local.properties`)

```properties
# Development-specific settings
# Available log levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF
logging.level.org.springframework.web=DEBUG
```

### Environment Variables

| Variable                 | Description         | Default                                   | Required |
| ------------------------ | ------------------- | ----------------------------------------- | -------- |
| `DB_USERNAME`            | Database username   | postgres                                  | Yes      |
| `DB_PASSWORD`            | Database password   | N1ghrd01-1948                             | Yes      |
| `SPRING_PROFILES_ACTIVE` | Active profile      | default                                   | No       |
| `SPRING_DATASOURCE_URL`  | Database URL        | jdbc:postgresql://localhost:5432/AmmoP1DB | No       |
| `ENABLE_FILE_LOGGING`    | Enable file logging | true                                      | No       |
| `LOG_PATH`               | Log file directory  | C:/Logs (local), /var/log/app (Docker)    | No       |

### Database Configuration

**Connection Pool Settings (HikariCP):**

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Logging Configuration

**Primary Location**: `src/main/resources/logback-spring.xml`

The application uses Logback for logging with configurable file and console output.

#### Logging Features

- **Console Logging**: Always enabled, outputs to stdout/stderr
- **File Logging**: Optional, controlled by `ENABLE_FILE_LOGGING` environment variable (disabled in AWS/cloud deployments)
- **CloudWatch Logs**: Enabled for AWS ECS deployments (see [AWS CloudWatch Setup](#aws-cloudwatch-logging))
- **Size-Based Rolling**: Automatic log file rotation based on size
- **Time-Based Archiving**: Old logs archived with date stamps
- **Automatic Cleanup**: Old logs deleted based on retention policies

#### Environment Variables

| Variable              | Description                 | Default                                | Values         |
| --------------------- | --------------------------- | -------------------------------------- | -------------- |
| `ENABLE_FILE_LOGGING` | Enable/disable file logging | true                                   | true, false    |
| `LOG_PATH`            | Directory for log files     | C:/Logs (local), /var/log/app (Docker) | Any valid path |

#### File Rolling Policy

**Configuration** (`logback-spring.xml`):

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>${LOG_PATH}/archived/ActivityTracking-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
    <maxFileSize>10MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>1GB</totalSizeCap>
</rollingPolicy>
```

**Behavior**:

- **Active Log**: `ActivityTracking.log` (no timestamp, always appends)
- **Archive Pattern**: `archived/ActivityTracking-2025-10-09.0.log`
- **Rollover Trigger**: File reaches 10MB
- **Multiple Files Per Day**: Indexed (`.0.log`, `.1.log`, `.2.log`, etc.)
- **Retention**: 30 days of history
- **Size Cap**: 1GB total (oldest files deleted first)

#### Log Levels

Set via Spring Boot properties:

```properties
# Application-wide logging
logging.level.com.ammons.taskactivity=DEBUG

# Spring framework logging
logging.level.org.springframework.web=DEBUG

# Available levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF
```

#### Disabling File Logging

**Docker Compose**:

```bash
export ENABLE_FILE_LOGGING=false
docker compose --profile host-db up -d
```

**Docker Run**:

```bash
docker run -e ENABLE_FILE_LOGGING=false taskactivity:latest
```

**Local Development**:
Set environment variable before starting:

```bash
# Windows PowerShell
$env:ENABLE_FILE_LOGGING="false"
mvnw.cmd spring-boot:run

# Linux/Mac
export ENABLE_FILE_LOGGING=false
./mvnw spring-boot:run
```

#### Log File Locations

| Environment | Path            | Notes                                      |
| ----------- | --------------- | ------------------------------------------ |
| Local       | `C:/Logs/`      | Windows development                        |
| Docker      | `/var/log/app/` | Inside container (mount volume to persist) |
| WSL2        | `/mnt/c/Logs/`  | WSL2 accessing Windows path                |

**Docker Volume Mount Example**:

```yaml
volumes:
  - C:/Logs:/var/log/app  # Persist logs on host
```

#### AWS CloudWatch Logging

**For AWS ECS Fargate deployments**, the application uses Amazon CloudWatch Logs instead of file logging.

**Configuration** (in `taskactivity-task-definition.json`):

```json
"logConfiguration": {
    "logDriver": "awslogs",
    "options": {
        "awslogs-group": "/ecs/taskactivity",
        "awslogs-region": "us-east-1",
        "awslogs-stream-prefix": "ecs-taskactivity"
    }
}
```

**Features:**

- **Real-time monitoring**: View logs as they're generated
- **30-day retention**: Logs automatically deleted after 30 days
- **S3 archival**: Export logs to S3 for long-term storage
- **Cost-optimized**: Automatic lifecycle policies move old logs to Glacier

**Viewing Logs:**

```powershell
# Tail live logs
aws logs tail /ecs/taskactivity --follow --region us-east-1

# View logs from last hour
aws logs tail /ecs/taskactivity --since 1h --region us-east-1

# Filter by pattern
aws logs tail /ecs/taskactivity --filter-pattern "ERROR" --follow
```

**Exporting to S3:**

```powershell
# See \scripts\export-logs-to-s3.ps1 for automated exports
cd aws
.\scripts\export-logs-to-s3.ps1 -Days 7
```

**S3 Log Archive:**

- **Bucket**: `taskactivity-logs-archive`
- **Lifecycle**: 90 days → Glacier, 365 days → Deep Archive
- **Cost savings**: Up to 96% for long-term storage

### Password Validation Configuration

**Primary Location**: `src/main/java/com/ammons/taskactivity/validation/ValidationConstants.java`

```java
// Password validation constants
public static final int PASSWORD_MIN_LENGTH = 10;
public static final String UPPERCASE_PATTERN = ".*[A-Z].*";
public static final String DIGIT_PATTERN = ".*\\d.*";
public static final String SPECIAL_CHAR_PATTERN = ".*[+&%$#@!~].*";
public static final String ALLOWED_SPECIAL_CHARS = "+&%$#@!~";

// Validation messages
public static final String PASSWORD_MIN_LENGTH_MSG = "Password must be at least "
        + PASSWORD_MIN_LENGTH + " characters long";
public static final String PASSWORD_UPPERCASE_MSG = "Password must contain at least 1 uppercase letter";
public static final String PASSWORD_DIGIT_MSG = "Password must contain at least 1 numeric digit";
public static final String PASSWORD_SPECIAL_CHAR_MSG = "Password must contain at least 1 special character ("
        + ALLOWED_SPECIAL_CHARS + ")";
```

**Used By:**

- `PasswordValidator.java` - Bean validation for DTOs

### Password Expiration Configuration

**Overview**: The application implements automatic password expiration with a 90-day policy and 7-day advance warnings. Special handling is provided for GUEST users who cannot change their own passwords.

**Key Components:**

1. **Database**: `expiration_date` column (DATE type) in `users` table
2. **Entity**: `LocalDate expirationDate` field in `User.java`
3. **Service Logic**: `UserService.java` contains expiration checking methods:
   - `isPasswordExpired(String username)` - Returns true if password has expired
   - `isPasswordExpiringSoon(String username)` - Returns true if password expires within 7 days
   - `getDaysUntilExpiration(String username)` - Returns days remaining until expiration
   - `changePassword()` - Automatically sets expiration to 90 days from password change

**Authentication Flow:**

```java
// CustomAuthenticationProvider.java - Blocks GUEST users with expired passwords
public Authentication authenticate(Authentication authentication) {
    String username = authentication.getName();
    Authentication result = super.authenticate(authentication);
    
    Optional<User> userOptional = userRepository.findByUsername(username);
    if (userOptional.isPresent()) {
        User user = userOptional.get();
        // GUEST users with expired passwords cannot authenticate
        if (user.getRole() == Role.GUEST && isPasswordExpired(user)) {
            throw new GuestPasswordExpiredException(
                "Password has expired. Contact system administrator.");
        }
    }
    return result;
}

// CustomAuthenticationSuccessHandler.java - Handles regular users
public void onAuthenticationSuccess(...) {
    // GUEST users with expired passwords are blocked at authentication level
    // This only handles USER and ADMIN roles
    if (userService.isPasswordExpired(username)) {
        request.getSession().setAttribute("passwordExpired", true);
        response.sendRedirect("/change-password?expired=true");
        return;
    }
    
    if (user.isForcePasswordUpdate()) {
        request.getSession().setAttribute("requiresPasswordUpdate", true);
        response.sendRedirect("/change-password?forced=true");
        return;
    }
    // ... normal login flow
}
```

**GUEST Role Special Handling:**

Since GUEST users cannot change passwords, expired password scenarios are handled differently:

1. **Authentication Blocked**: `CustomAuthenticationProvider` rejects authentication before session creation
2. **Custom Exception**: `GuestPasswordExpiredException` thrown for GUEST expired passwords
3. **Error Message**: Login page displays "Password has expired. Contact system administrator."
4. **No Warning Display**: Password expiration warnings are suppressed for GUEST users (they appear only for USER/ADMIN)
5. **Password Change Blocked**: `PasswordChangeController` blocks GUEST role from accessing password change pages

**UI Warning Display:**

- **Spring Boot**: `GlobalExceptionHandler.java` injects `passwordExpiringWarning` model attribute (suppressed for GUEST)
- **Angular**: `AuthService` exposes `passwordExpiringWarning$` observable (suppressed for GUEST), `AppComponent` subscribes and displays warning in toolbar
- **Warning Threshold**: 7 days before expiration
- **Warning Message**: "⚠️ Your password will expire in X day(s). Please change it soon."

**Important Technical Notes:**

- **Type Consistency**: Use `LocalDate` (not `LocalDateTime`) to match DATE column
- **GUEST Restrictions**: GUEST users with expired passwords are blocked at authentication provider level, not success handler
- **No Circular Dependency**: CustomAuthenticationProvider created as @Bean in SecurityConfig, uses UserRepository directly
- **Single Authentication Provider**: Only CustomAuthenticationProvider is registered (removed userDetailsService from SecurityConfig to prevent fallback providers)
- **Priority**: Expired password check occurs before force update check
- **Null Handling**: NULL expiration_date = password never expires (backward compatibility)

**Configuration:**

Currently hardcoded (90-day expiration, 7-day warning). Future enhancement could make these configurable:

```properties
# Future configuration options
app.security.password.expiration-days=90
app.security.password.warning-days=7
```

**Related Documentation**: See `docs/Password_Expiration_Implementation_Guide.md` for complete implementation details.

### Task List Sort Configuration

**Primary Location**: `src/main/java/com/ammons/taskactivity/config/TaskListSortConfig.java`

The application provides configurable sorting for the task activity list through a dedicated configuration class:

```java
@ConfigurationProperties(prefix = "app.task-activity.list.sort")
@Component
public class TaskListSortConfig {
    private String dateDirection = "DESC";
    private String clientDirection = "ASC";
    private String projectDirection = "ASC";

    // Creates Spring Data Sort object based on configuration
    public Sort createSort() {
        return Sort.by(
            createOrder("taskDate", dateDirection),
            createOrder("client", clientDirection),
            createOrder("project", projectDirection)
        );
    }

    private Sort.Order createOrder(String property, String direction) {
        Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        return new Sort.Order(dir, property);
    }
}
```

**Configuration Properties:**

```properties
# Task Activity List Sort Configuration
# Controls the default sort order for the task activity list
# Valid values for direction: ASC (ascending) or DESC (descending)
app.task-activity.list.sort.date-direction=DESC
app.task-activity.list.sort.client-direction=ASC
app.task-activity.list.sort.project-direction=ASC
```

**Property Descriptions:**

- `app.task-activity.list.sort.date-direction`: Controls sort order for task date (default: DESC - newest first)
- `app.task-activity.list.sort.client-direction`: Controls sort order for client column (default: ASC - alphabetical)
- `app.task-activity.list.sort.project-direction`: Controls sort order for project column (default: ASC - alphabetical)

**Usage in Controllers:**

```java
@Controller
public class TaskActivityWebController {
    private final TaskListSortConfig taskListSortConfig;

    @GetMapping("/list")
    public String showTaskList(@RequestParam(defaultValue = "0") int page,
                               Model model, Authentication authentication) {
        // Create pageable with configured sort order
        Pageable pageable = PageRequest.of(page, 20, taskListSortConfig.createSort());

        // Use pageable for repository queries...
    }
}
```

**Environment-Specific Configuration:**

- `application.properties` - Default configuration
- `application-local.properties` - Local development overrides
- `application-docker.properties` - Docker/production overrides

**Implementation Notes:**

- Sort configuration is applied at the Pageable level, not in repository @Query annotations
- Repository queries should NOT include ORDER BY clauses when using Pageable sorting
- Invalid direction values default to DESC (descending)
- Configuration is loaded at startup and cached by Spring
- `PasswordValidationService.java` - Service-layer validation
- `UserService.java` - User management operations

**To Modify Requirements:**

1. Update constants in `ValidationConstants.java`
2. The changes will automatically propagate to:
   - `PasswordValidator.java` (uses ValidationConstants)
   - `PasswordValidationService.java` (uses ValidationConstants)
   - `UserService.java` (uses PasswordValidationService)
3. Update HTML help text in templates if user-facing messages change:
   - `change-password.html`
   - `user-change-password.html`
   - `user-add.html`
4. Recompile and redeploy

### Data Pre-Loading Configuration

**Primary Location**: `src/main/java/com/ammons/taskactivity/config/DataInitializer.java`

The application supports automatic pre-loading of data through a SQL script file when running in the `docker` profile. This feature allows developers to initialize the database with test data, reference data, or other required information during application startup.

**How It Works:**

1. The `DataInitializer` component runs during application startup (only in `docker` profile)
2. After creating the default admin user, it checks for the existence of `data-load.sql` in the `src/main/resources` directory
3. If the file exists, it executes all SQL statements in the file against the database
4. Each SQL statement is separated by semicolons and executed individually
5. Comments (lines starting with `--`) are ignored
6. Errors in individual statements are logged but don't stop the process

**Usage:**

1. **Create the data load file:**
   
   ```
   src/main/resources/data-load.sql
   ```

2. **Add your SQL statements:**
   
   ```sql
   -- Example data-load.sql file
   -- Insert initial dropdown values
   INSERT INTO DropdownValues (category, itemvalue, displayorder, isactive) VALUES
   ('CLIENT', 'Acme Corporation', 1, true),
   ('CLIENT', 'TechStart Inc', 2, true),
   ('PROJECT', 'Website Redesign', 1, true),
   ('PROJECT', 'Mobile App Development', 2, true),
   ('PHASE', 'Planning', 1, true),
   ('PHASE', 'Development', 2, true),
   ('PHASE', 'Testing', 3, true);
   
   -- Insert additional users
   INSERT INTO "Users" (username, password, role, enabled, created_date, firstname, lastname) VALUES 
   ('developer', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER', true, CURRENT_TIMESTAMP, 'Test', 'Developer'),
   ('manager', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN', true, CURRENT_TIMESTAMP, 'Test', 'Manager');
   ```

3. **Deploy using Docker profile:**
   
   ```bash
   # The data-load.sql will be executed automatically during startup
   docker-compose up
   ```

**Important Notes:**

- **Profile Requirement**: This feature only works when the `docker` profile is active
- **One-Time Execution**: The script runs only when the admin user doesn't exist (fresh deployment)
- **SQL Compatibility**: Ensure your SQL statements are compatible with your target database (PostgreSQL for production)
- **Password Hashing**: User passwords in the SQL should be pre-hashed using BCrypt
- **Error Handling**: Individual statement failures are logged but don't stop the application startup
- **File Location**: The file must be in `src/main/resources/` and named exactly `data-load.sql`

**Logging:**

The DataInitializer provides detailed logging:

```
INFO  - Found data-load.sql, executing script...
DEBUG - Executed SQL: INSERT INTO DropdownValues...
INFO  - Data load script executed successfully
```

**Security Considerations:**

- Never include production passwords or sensitive data in the data-load.sql file
- The file is packaged into the application JAR, so avoid sensitive information
- Use environment variables or external configuration for production secrets
- Consider using Spring profiles to have different data-load files for different environments

**Testing:**

For unit and integration tests, use the existing `test-data.sql` file in `src/test/resources/` which serves a similar purpose for the test environment.

### Custom Configuration Properties Metadata

**Primary Location**: `src/main/resources/META-INF/additional-spring-configuration-metadata.json`

The application defines custom configuration properties that extend beyond Spring Boot's standard properties. To provide IDE auto-completion, documentation, and eliminate "unknown property" warnings, we maintain a metadata file that documents all custom properties.

**Purpose:**

- Provides IntelliSense/auto-completion in IDEs (IntelliJ IDEA, VS Code, Eclipse)
- Documents property types, descriptions, and default values
- Eliminates Spring Boot configuration processor warnings
- Improves developer experience when editing `.properties` files

**Documented Properties:**

```json
{
  "properties": [
    {
      "name": "app.task-activity.list.sort.date-direction",
      "type": "java.lang.String",
      "description": "Sort direction for task date column",
      "defaultValue": "DESC"
    },
    {
      "name": "app.task-activity.list.sort.client-direction",
      "type": "java.lang.String",
      "description": "Sort direction for client column",
      "defaultValue": "ASC"
    },
    {
      "name": "app.task-activity.list.sort.project-direction",
      "type": "java.lang.String",
      "description": "Sort direction for project column",
      "defaultValue": "ASC"
    },
    {
      "name": "app.admin.initial-password",
      "type": "java.lang.String",
      "description": "Initial password for admin user",
      "defaultValue": "Admin123!"
    },
    {
      "name": "cors.allowed-origins",
      "type": "java.lang.String",
      "description": "Allowed origins for CORS requests",
      "defaultValue": "https://yourdomain.com"
    },
    {
      "name": "cors.allowed-methods",
      "type": "java.lang.String",
      "description": "Allowed HTTP methods for CORS",
      "defaultValue": "GET,POST,PUT,DELETE,OPTIONS"
    },
    {
      "name": "cors.allowed-headers",
      "type": "java.lang.String",
      "description": "Allowed headers for CORS",
      "defaultValue": "*"
    },
    {
      "name": "cors.allow-credentials",
      "type": "java.lang.Boolean",
      "description": "Allow credentials in CORS requests",
      "defaultValue": true
    }
  ]
}
```

**Configuration Classes:**

| Property Prefix               | Configuration Class       | Description                     |
| ----------------------------- | ------------------------- | ------------------------------- |
| `app.task-activity.list.sort` | `TaskListSortConfig.java` | Task list sorting configuration |
| `app.admin`                   | `DataInitializer.java`    | Admin user initialization       |
| `cors`                        | `CorsConfig.java`         | CORS security settings          |

**Adding New Custom Properties:**

1. **Define the property in your `@ConfigurationProperties` class:**
   
   ```java
   @Component
   @ConfigurationProperties(prefix = "app.myfeature")
   public class MyFeatureConfig {
       private String myProperty = "defaultValue";
       // getters and setters
   }
   ```

2. **Add metadata to `additional-spring-configuration-metadata.json`:**
   
   ```json
   {
     "name": "app.myfeature.my-property",
     "type": "java.lang.String",
     "description": "Description of what this property does",
     "defaultValue": "defaultValue"
   }
   ```

3. **Use the property in `application.properties` or environment-specific files:**
   
   ```properties
   app.myfeature.my-property=customValue
   ```

**Benefits:**

- **IDE Support**: IntelliJ IDEA and VS Code will provide auto-completion for your custom properties
- **Type Safety**: IDEs will warn about incorrect types (e.g., using text for a boolean property)
- **Documentation**: Hover tooltips in IDEs show property descriptions
- **Validation**: Spring Boot configuration processor validates property names at build time

**File Location Requirements:**

- **Must be in**: `src/main/resources/META-INF/`
- **Must be named**: `additional-spring-configuration-metadata.json`
- **Packaged into**: Application JAR during Maven build
- **Read by**: Spring Boot Configuration Processor and IDEs

**Common Property Types:**

| Type     | JSON Value             | Example                          |
| -------- | ---------------------- | -------------------------------- |
| String   | `"java.lang.String"`   | Text values, URLs, paths         |
| Boolean  | `"java.lang.Boolean"`  | true/false flags                 |
| Integer  | `"java.lang.Integer"`  | Numeric counts, sizes            |
| Duration | `"java.time.Duration"` | Time periods (e.g., "30s", "5m") |
| Array    | `"java.util.List<T>"`  | Multiple values                  |

### CORS Configuration

**Primary Location**: `src/main/java/com/ammons/taskactivity/config/CorsConfig.java`

The application supports Cross-Origin Resource Sharing (CORS) configuration for API access from different domains.

**Configuration Class:**

```java
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsConfig {
    private String allowedOrigins = "https://yourdomain.com";
    private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
    private String allowedHeaders = "*";
    private boolean allowCredentials = true;

    // Getters and setters
}
```

**Properties:**

```properties
# CORS Configuration
cors.allowed-origins=https://example.com,https://app.example.com
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true
```

**Property Descriptions:**

- `cors.allowed-origins`: Comma-separated list of allowed origins (use `*` for all origins - not recommended for production)
- `cors.allowed-methods`: HTTP methods that are allowed for cross-origin requests
- `cors.allowed-headers`: Headers that are allowed in requests (use `*` for all headers)
- `cors.allow-credentials`: Whether to allow credentials (cookies, authorization headers) in CORS requests

**Usage:**

The CORS configuration is automatically applied through Spring Security's `CorsConfiguration`. For production deployments, restrict `allowed-origins` to specific trusted domains.

**Example - Multiple Origins:**

```properties
cors.allowed-origins=https://app.example.com,https://admin.example.com,https://mobile.example.com
```

**Example - Development (Allow All):**

```properties
cors.allowed-origins=*
cors.allow-credentials=false
```

**Security Notes:**

- Never use `cors.allowed-origins=*` with `cors.allow-credentials=true` in production
- Restrict allowed origins to specific trusted domains
- Review allowed methods - consider removing unused methods (e.g., DELETE if not needed)
- Be cautious with `allowedHeaders=*` - specify exact headers when possible

### Admin User Initial Password Configuration

**Primary Location**: `src/main/java/com/ammons/taskactivity/config/DataInitializer.java`

The default admin user is created automatically during application startup with a configurable initial password.

**Property:**

```properties
# Admin User Configuration
app.admin.initial-password=Admin123!
```

**Environment Variable Override:**

```bash
# Docker
docker run -e APP_ADMIN_INITIAL_PASSWORD=securePassword123! taskactivity:latest

# Docker Compose
export APP_ADMIN_INITIAL_PASSWORD=securePassword123!
docker-compose up

# Local Development
export APP_ADMIN_INITIAL_PASSWORD=securePassword123!
./mvnw spring-boot:run
```

**Security Recommendations:**

1. **Change the default password immediately** after first login in production environments
2. **Use environment variables** instead of hardcoding passwords in properties files
3. **Use strong passwords** that meet the application's password validation requirements:
   - Minimum 10 characters
   - At least 1 uppercase letter
   - At least 1 numeric digit
   - At least 1 special character from: `+&%$#@!~`
4. **Enable password change enforcement** to require users to change initial passwords on first login

**Example - Production Deployment:**

```bash
# Set via environment variable (recommended)
export APP_ADMIN_INITIAL_PASSWORD='MyStr0ng!P@ssw0rd'

# Or via application-production.properties
app.admin.initial-password=${APP_ADMIN_INITIAL_PASSWORD:defaultFallback123!}
```

## Architecture

### Package Structure

```
com.ammons.taskactivity/
├── config/              # Configuration classes
│   ├── SecurityConfig.java
│   ├── DropdownConfig.java
│   └── TaskListSortConfig.java
├── controller/          # MVC Controllers
│   ├── TaskActivityController.java
│   ├── UserController.java
│   └── DropdownController.java
├── dto/                 # Data Transfer Objects
│   ├── TaskActivityDto.java
│   ├── PasswordChangeDto.java
│   └── UserCreateDto.java
├── entity/             # JPA Entities
│   ├── TaskActivity.java
│   ├── DropdownValue.java
│   └── User.java
├── exception/          # Custom Exceptions
│   ├── TaskActivityNotFoundException.java
│   └── GlobalExceptionHandler.java
├── repository/         # Spring Data Repositories
│   ├── TaskActivityRepository.java
│   ├── DropdownValueRepository.java
│   └── UserRepository.java
├── service/            # Business Logic Services
│   ├── TaskActivityService.java
│   ├── UserService.java
│   ├── WeeklyTimesheetService.java
│   └── DropdownService.java
└── validation/         # Custom Validators
    ├── ValidPassword.java
    ├── PasswordValidator.java
    ├── PasswordValidationService.java
    └── ValidationConstants.java
```

### Frontend Structure

```
src/main/resources/
├── static/             # Static resources
│   ├── index.html      # Application landing page
│   ├── css/            # Stylesheet files
│   │   ├── base.css           # Base layout and navigation
│   │   ├── components.css     # Reusable UI components
│   │   ├── login.css          # Login page specific styles
│   │   └── timesheet.css      # Timesheet specific styles
│   └── js/             # JavaScript modules
│       ├── modal-utils.js      # Modal management utilities
│       ├── password-toggle.js  # Password visibility toggles
│       ├── date-utils.js       # Date manipulation utilities
│       └── form-utils.js       # Form helper functions
└── templates/          # Thymeleaf templates
    ├── access-denied.html
    ├── change-password.html
    ├── dropdown-category-management.html
    ├── dropdown-management-simple.html
    ├── login.html
    ├── task-activity-form.html
    ├── task-detail.html
    ├── task-list.html
    ├── weekly-timesheet.html
    └── admin/          # Admin-only templates
        ├── client-management.html
        ├── dropdown-edit.html
        ├── dropdown-management.html
        ├── phase-management.html
        ├── project-management.html
        ├── user-add.html
        ├── user-change-password.html
        ├── user-edit.html
        └── user-management.html
```

### JavaScript Utilities

The application uses modular JavaScript utilities to promote code reusability and maintainability:

#### 1. Modal Utilities (`modal-utils.js`)

Provides reusable modal dialog management for confirmations and user interactions.

**Functions:**

- `openDeleteModal(formId, message, modalId)` - Opens a confirmation modal
- `closeModal(modalId)` - Closes an open modal
- `confirmDelete()` - Confirms and submits the delete action
- `initClickOutsideToClose(modalId)` - Enables click-outside-to-close behavior

**Usage Example:**

```javascript
// In HTML template
<script src="/js/modal-utils.js"></script>
<script>
    function deleteItem() {
        openDeleteModal('deleteForm-123',
            'Are you sure you want to delete this item?');
    }
</script>
```

**Used in:** `task-detail.html`, `dropdown-management-simple.html`, `user-management.html`, `project-management.html`, `phase-management.html`, `client-management.html`

#### 2. Password Toggle Utilities (`password-toggle.js`)

Manages password field visibility for better user experience.

**Functions:**

- `togglePasswordVisibility(fieldId, button)` - Toggles a single password field
- `toggleAllPasswords(checkboxId, fieldIds)` - Toggles multiple password fields
- `initPasswordToggles(config)` - Initializes password toggle functionality

**Usage Example:**

```javascript
// In HTML template
<script src="/js/password-toggle.js"></script>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const checkbox = document.getElementById("showPasswords");
        checkbox.addEventListener('change', function() {
            toggleAllPasswords("showPasswords", ["password", "confirmPassword"]);
        });
    });
</script>
```

**Used in:** `change-password.html`, `user-change-password.html`, `user-add.html`

#### 3. Date Utilities (`date-utils.js`)

Provides date manipulation and formatting functions for week-based navigation.

**Functions:**

- `getUrlParameter(name)` - Gets URL query parameter value
- `updateUrl(date, endpoint)` - Updates URL with date parameter
- `getMondayOfWeek(date)` - Calculates Monday of the week
- `formatDateForInput(date)` - Formats date as YYYY-MM-DD
- `addDays(date, days)` - Adds/subtracts days from a date
- `parseDate(dateStr)` - Safely parses date string with timezone handling
- `getCurrentOrUrlMonday(paramName)` - Gets current or URL-specified Monday

**Usage Example:**

```javascript
// In HTML template
<script src="/js/date-utils.js"></script>
<script>
    const monday = DateUtils.getMondayOfWeek(new Date());
    const formatted = DateUtils.formatDateForInput(monday);
    DateUtils.updateUrl(formatted, '/task-activity/weekly-timesheet');
</script>
```

**Used in:** `weekly-timesheet.html`

#### 4. Form Utilities (`form-utils.js`)

Common form functionality including auto-growing textareas.

**Functions:**

- `initAutoGrowTextarea(textareaId)` - Makes a textarea auto-grow
- `initAllAutoGrowTextareas()` - Auto-initializes all textareas with `.auto-grow` class

**Usage Example:**

```javascript
// In HTML template
<script src="/js/form-utils.js"></script>
<script>
    // Automatically handles all textareas with class="auto-grow"
    // Or manually initialize specific textarea:
    FormUtils.initAutoGrowTextarea('detailsField');
</script>
```

**Used in:** `task-detail.html`

### CSS Architecture

The application uses a modular CSS architecture with external stylesheet files for improved maintainability, performance, and consistency.

#### CSS Files Structure

**Base Styles (`base.css`)** - 107 lines

- Core layout and structural styles used across all pages
- Body and typography defaults
- Navigation bar (`.navbar`, `.nav-content`, `.user-info`, badges)
- Container layouts (`.container`, `.header`, `.content`, `.section`)
- Responsive max-widths and spacing

**Component Styles (`components.css`)** - 321 lines

- Reusable UI components shared across multiple pages
- Buttons: All variants (`.btn-primary`, `.btn-secondary`, `.btn-danger`, etc.)
- Forms: Form groups, inputs, textareas, validation styles
- Messages: Success, error, warning, info messages
- Modals: Modal dialogs and confirmation boxes
- Tables: Data tables with hover effects
- Filters: Filter sections and controls
- Password: Password toggle and requirements display

**Login Specific (`login.css`)** - 68 lines

- Login page specific styles
- Login container centering (`.login-container`, `.login-form`)
- Login-specific button (`.btn-login`)

**Timesheet Specific (`timesheet.css`)** - 106 lines

- Weekly timesheet page specific styles
- Timesheet table formatting (`.timesheet-table`)
- Week navigation controls (`.week-navigation`, `.date-selector`)
- Day/week totals display

#### Standard Template Header

Most templates use this standard CSS import pattern:

```html
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Page Title</title>
    <link rel="stylesheet" th:href="@{/css/base.css}" />
    <link rel="stylesheet" th:href="@{/css/components.css}" />
    <!-- Add page-specific CSS if needed -->
    <style>
        /* Minimal page-specific styles only */
    </style>
</head>
```

**Import Order Matters:**

1. `base.css` - Foundation styles first
2. `components.css` - Component styles second
3. Page-specific CSS - Optional, for pages with substantial unique styling
4. Inline `<style>` - For small page-specific adjustments

#### CSS Usage Examples

**Standard Application Page:**

```html
<link rel="stylesheet" th:href="@{/css/base.css}" />
<link rel="stylesheet" th:href="@{/css/components.css}" />
```

Used by: task-list, task-detail, user-management, project-management, etc.

**Login Page:**

```html
<link rel="stylesheet" th:href="@{/css/login.css}" />
```

Used by: login.html (includes all login-specific styles)

**Weekly Timesheet:**

```html
<link rel="stylesheet" th:href="@{/css/base.css}" />
<link rel="stylesheet" th:href="@{/css/components.css}" />
<link rel="stylesheet" th:href="@{/css/timesheet.css}" />
```

Used by: weekly-timesheet.html

#### Common CSS Classes Reference

**Layout:**

- `.container` - Main content container (max-width: 1200px by default)
- `.container.narrow` - Narrow container (max-width: 800px)
- `.container.wide` - Wide container (max-width: 1400px)
- `.navbar` - Navigation bar
- `.header` - Page header section
- `.content` - Main content area with padding

**Buttons:**

- `.btn` - Base button style
- `.btn-primary` - Primary action (blue)
- `.btn-success` - Success action (green)
- `.btn-danger` - Dangerous action (red)
- `.btn-warning` - Warning action (yellow)
- `.btn-secondary` - Secondary action (gray)
- `.btn-info` - Info action (cyan)
- `.btn-sm` - Small button variant

**Forms:**

- `.form-group` - Form field container
- `.form-row` - Horizontal form layout
- `.form-container` - Form wrapper with padding
- `.required` - Required field indicator (red asterisk)
- `.filter-section` - Filter area styling
- `.filter-group` - Individual filter field

**Messages:**

- `.success-message` - Success notification (green)
- `.error-message` - Error notification (red)
- `.warning-message` - Warning notification (yellow)
- `.info-message` - Info notification (blue)

**Modals:**

- `.modal` - Modal overlay
- `.modal-content` - Modal dialog box
- `.modal-buttons` - Modal action buttons container

**Tables:**

- `.data-table` - Standard data table
- `.timesheet-table` - Timesheet-specific table

**Badges:**

- `.user-badge` - User role badge (green)
- `.admin-badge` - Admin role badge (red)

#### CSS Maintenance Guidelines

**Adding New Styles:**

1. **Reusable Component**: Add to `components.css` if used on 2+ pages
2. **Layout/Structure**: Add to `base.css` if it affects overall layout
3. **Page-Specific**: Create new page CSS file if 50+ lines, otherwise inline
4. **Small Adjustment**: Use inline `<style>` for <20 lines

**Modifying Existing Styles:**

1. Locate the style in `base.css` or `components.css`
2. Make the change in one place
3. Test across all pages that use that CSS file
4. Document breaking changes in commit message

**Naming Conventions:**

- Use semantic class names (`.btn-primary`, `.error-message`)
- Use kebab-case for multi-word classes (`.filter-group`, `.modal-content`)
- Avoid overly specific selectors (prefer classes over nested selectors)
- Keep specificity low for easier overriding

**Performance Considerations:**

- CSS files are cached by browsers (improves load time)
- Avoid `!important` unless absolutely necessary
- Minimize deep selector nesting (max 3 levels)
- Use CSS variables for colors/spacing if implementing themes

### Best Practices for Frontend Development

1. **Use External CSS**: Reference `base.css` and `components.css` in all templates for consistent styling
2. **CSS File Selection**: Use existing CSS classes before creating new ones; add to appropriate CSS file if reusable
3. **Use Existing Utilities**: Before writing inline JavaScript, check if functionality exists in utility modules
4. **Modular Approach**: Keep template-specific initialization separate from reusable functions
5. **Browser Caching**: External CSS and JS files are cached, improving performance across pages
6. **Consistent Patterns**: Follow existing modal, password toggle, and form patterns for consistency
7. **Accessibility**: JavaScript utilities include ARIA labels for better accessibility; ensure CSS provides sufficient color contrast
8. **Error Handling**: Utilities include null checks and defensive programming
9. **Responsive Design**: CSS includes mobile-friendly styles; test across different screen sizes
10. **CSS Specificity**: Keep selector specificity low; prefer classes over IDs or complex nested selectors

### Design Patterns

1. **MVC (Model-View-Controller)**
   
   - Controllers handle HTTP requests
   - Services contain business logic
   - Repositories manage data access
   - DTOs transfer data between layers

2. **Repository Pattern**
   
   - Spring Data JPA repositories
   - Abstraction over data access
   - Query method derivation

3. **Service Layer Pattern**
   
   - Business logic separation
   - Transaction management
   - Reusable components

4. **Validation Pattern**
   
   - Custom annotation validators
   - Jakarta Bean Validation
   - Server-side and client-side validation

## Database Schema

### TaskActivity Table

```sql
CREATE TABLE "TaskActivity" (
    "id" BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "taskdate" DATE NOT NULL,
    "client" VARCHAR(255) NOT NULL,
    "project" VARCHAR(255) NOT NULL,
    "phase" VARCHAR(255) NOT NULL,
    "taskhours" DECIMAL(4,2) NOT NULL,
    "details" VARCHAR(255),  -- Optional field
    "username" VARCHAR(50) NOT NULL,
    CONSTRAINT fk_taskactivity_username
        FOREIGN KEY ("username")
        REFERENCES "Users"("username")
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- Performance indexes
CREATE INDEX idx_taskactivity_date ON "TaskActivity"("taskdate");
CREATE INDEX idx_taskactivity_client ON "TaskActivity"("client");
CREATE INDEX idx_taskactivity_project ON "TaskActivity"("project");
CREATE INDEX idx_taskactivity_username ON "TaskActivity"("username");
```

**Entity Mapping:**

```java
@Entity
@Table(name = "taskactivity", schema = "public")
public class TaskActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "taskdate", nullable = false)
    private LocalDate taskDate;

    @Column(name = "client", nullable = false)
    private String client;

    @Column(name = "project", nullable = false)
    private String project;

    @Column(name = "phase", nullable = false)
    private String phase;

    @Column(name = "taskhours", nullable = false)
    private BigDecimal hours;

    @Column(name = "details", nullable = true)  // Optional field
    private String details;

    @Column(name = "username", nullable = false, length = 50)
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username cannot exceed 50 characters")
    private String username;
}
```

**Key Features:**

- **Automatic Population**: The `username` field is automatically populated from the logged-in user's authentication context when creating a new task
- **Read-Only**: The username field is not editable in the UI and is not included in update operations to maintain data integrity
- **Foreign Key**: References the `Users` table with CASCADE updates and RESTRICT deletes to prevent orphaned records

### DropdownValue Table

```sql
CREATE TABLE "DropdownValues" (
    "id" BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "category" VARCHAR(50) NOT NULL,
    "subcategory" VARCHAR(50) NOT NULL,
    "itemvalue" VARCHAR(255) NOT NULL,
    "displayorder" INTEGER NOT NULL DEFAULT 0,
    "isactive" BOOLEAN DEFAULT TRUE,
    UNIQUE("category", "subcategory", "itemvalue")
);

-- Performance index
CREATE INDEX idx_dropdownvalues_category ON "DropdownValues"("category");
```

### Users Table

```sql
CREATE TABLE "Users" (
    "id" BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    "username" VARCHAR(50) UNIQUE NOT NULL,
    "firstname" VARCHAR(50),
    "lastname" VARCHAR(50) NOT NULL,
    "company" VARCHAR(100),
    "userpassword" VARCHAR(255) NOT NULL,
    "userrole" VARCHAR(20) NOT NULL,
    "enabled" BOOLEAN DEFAULT TRUE,
    "forcepasswordupdate" BOOLEAN DEFAULT FALSE,
    "expiration_date" DATE NULL,
    "failed_login_attempts" INTEGER NOT NULL DEFAULT 0,
    "account_locked" BOOLEAN NOT NULL DEFAULT FALSE,
    "created_date" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "last_login" TIMESTAMP
);
```

**Entity Mapping:**

```java
@Entity
@Table(name = "Users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "firstname", length = 50)
    private String firstname;

    @Column(name = "lastname", nullable = false, length = 50)
    private String lastname;

    @Column(name = "company", length = 100)
    private String company;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "forcePasswordUpdate")
    private Boolean forcePasswordUpdate = false;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;
}
```

**Key Features:**

- **First Name**: Optional field (nullable) for the user's first name, maximum 50 characters
- **Last Name**: Required field for the user's last name, maximum 50 characters
- **Company**: Optional field (nullable) for the user's company/organization, maximum 100 characters
- **Expiration Date**: Optional password expiration date for automatic password rotation
- **Failed Login Attempts**: Counter for failed login attempts, automatically incremented on authentication failures
- **Account Locked**: Boolean flag set to true when failed login attempts exceed the configured maximum (default: 5)
- **Created Date**: Automatically set to current timestamp when user is created
- **Last Login**: Automatically updated when user successfully authenticates
- **Display Format**: Throughout the application, users are displayed as "firstname lastname (username)" with automatic space normalization

**Security Features:**

- **Account Lockout**: After 5 failed login attempts (configurable via `security.login.max-attempts`), the account is automatically locked
- **Automatic Reset**: Failed login counter resets to 0 on successful authentication
- **Admin Unlock**: Administrators can unlock accounts via the User Management UI
- **Lockout Visibility**: Locked accounts display a 🔒 indicator in the user management interface

## API Reference

### Base URL

```
http://localhost:8080/api
```

### Authentication

All API endpoints require session-based authentication. Users must authenticate via the web interface before making API calls.

### Task Activities API

#### Get All Task Activities

```http
GET /api/task-activities
```

**Response:**

```json
[
    {
        "id": 1,
        "taskDate": "2025-10-01",
        "client": "Client A",
        "project": "Project X",
        "phase": "Development",
        "hours": 8.0,
        "details": "Work description",
        "username": "jdoe"
    }
]
```

#### Get Task by ID

```http
GET /api/task-activities/{id}
```

**Parameters:**

- `id` (path): Task activity ID (Long)

**Response:**

```json
{
    "id": 1,
    "taskDate": "2025-10-01",
    "client": "Client A",
    "project": "Project X",
    "phase": "Development",
    "hours": 8.0,
    "details": "Work description",
    "username": "jdoe"
}
```

#### Create Task Activity

```http
POST /api/task-activities
Content-Type: application/json

{
  "taskDate": "2025-10-01",
  "client": "Client A",
  "project": "Project X",
  "phase": "Development",
  "hours": 8.0,
  "details": "Work description"
}
```

**Note:** The `username` field is automatically populated from the authenticated user's session and should not be included in the request body.
"hours": 8.0,
"details": "Work description"
}

```

#### Update Task Activity

```http
PUT /api/task-activities/{id}
Content-Type: application/json

{
  "taskDate": "2025-10-01",
  "client": "Client A",
  "project": "Project X",
  "phase": "Testing",
  "hours": 6.5,
  "details": "Updated description"
}
```

#### Delete Task Activity

```http
DELETE /api/task-activities/{id}
```

### Health Check API

#### Application Health

```http
GET /api/health
```

**Response:**

```json
{
    "status": "UP",
    "database": "connected",
    "timestamp": "2025-10-02T12:00:00Z"
}
```

### Dropdown Management API

The Dropdown Management API provides consolidated endpoints for managing dropdown values across multiple categories (CLIENT, PROJECT, PHASE, etc.). This API supports the Angular frontend and follows a dynamic, extensible design that automatically adapts to new categories added to the database.

**Add New Category Feature:**

The Thymeleaf admin interface includes an "Add New Category" button that allows administrators to create entirely new dropdown categories without database manipulation. This feature:

- Opens a modal dialog with three required fields: Category, Subcategory, and Value
- Auto-converts both category and subcategory to uppercase (CSS `text-transform` + JavaScript)
- Validates category uniqueness on the server side before creation
- Creates the first dropdown value for the new category in a single operation
- Redirects to the new category's filter view after successful creation
- Uses Spring MVC form handling with CSRF protection and flash messages
- **Endpoint**: `POST /admin/dropdowns/add-category`
- **Parameters**: `category`, `subcategory`, `value` (all required)
- **Validation**: Checks against `getAllCategories()` for duplicates

**Note:** The Angular dashboard currently does not implement category creation (only Edit/Delete for existing values). Category creation is available only through the Thymeleaf admin interface.

#### Get All Categories

```http
GET /api/dropdowns/categories
```

**Description:** Returns a list of all distinct categories from the dropdown values table.

**Access Control:** Requires authentication (USER, ADMIN, or GUEST role)

**Response:**

```json
[
    "CLIENT",
    "PHASE",
    "PROJECT"
]
```

**Use Case:** Populates the category filter dropdown in the Angular Dashboard

#### Get All Dropdown Values

```http
GET /api/dropdowns/all
```

**Description:** Returns all dropdown values across all categories, including their category, item value, display order, and active status.

**Access Control:** Requires authentication (USER, ADMIN, or GUEST role)

**Response:**

```json
[
    {
        "id": 1,
        "category": "PHASE",
        "itemValue": "Development",
        "displayOrder": 1,
        "isActive": true
    },
    {
        "id": 2,
        "category": "CLIENT",
        "itemValue": "Acme Corp",
        "displayOrder": 1,
        "isActive": true
    }
]
```

**Use Case:** Displays all dropdown values when "All Categories" is selected in the filter

#### Get Values by Category

```http
GET /api/dropdowns/category/{category}
```

**Parameters:**

- `category` (path): Category name (e.g., "CLIENT", "PROJECT", "PHASE")

**Description:** Returns all dropdown values for a specific category.

**Access Control:** Requires authentication (USER, ADMIN, or GUEST role)

**Response:**

```json
[
    {
        "id": 1,
        "category": "CLIENT",
        "subcategory": "GENERAL",
        "itemValue": "Acme Corp",
        "displayOrder": 1,
        "isActive": true
    },
    {
        "id": 2,
        "category": "CLIENT",
        "itemValue": "TechStart Inc",
        "displayOrder": 2,
        "isActive": true
    }
]
```

**Use Case:** Filters dropdown values when a specific category is selected

#### Legacy Category Endpoint

```http
GET /api/dropdowns/{category}
```

**Note:** This endpoint is maintained for backwards compatibility and functions identically to `/api/dropdowns/category/{category}`.

#### Get Specific Category Shortcuts

```http
GET /api/dropdowns/clients
GET /api/dropdowns/projects
GET /api/dropdowns/phases
```

**Description:** Convenience endpoints that return values for specific categories.

**Access Control:** Requires authentication (USER, ADMIN, or GUEST role)

**Note:** These are shortcuts to the generic category endpoint and are used by the task creation/editing forms.

#### Create Dropdown Value

```http
POST /api/dropdowns
Content-Type: application/json

{
  "category": "CLIENT",
  "subcategory": "GENERAL",
  "itemValue": "New Client Name"
}
```

**Description:** Creates a new dropdown value. Display order is automatically assigned. The subcategory field is required and allows for finer-grained categorization (e.g., 'TASK' for PHASE entries).

**Access Control:** Requires ADMIN role

**Response:**

```json
{
    "id": 10,
    "category": "CLIENT",
    "subcategory": "GENERAL",
    "itemValue": "New Client Name",
    "displayOrder": 5,
    "isActive": true
}
```

#### Update Dropdown Value

```http
PUT /api/dropdowns/{id}
Content-Type: application/json

{
  "category": "CLIENT",
  "subcategory": "GENERAL",
  "itemValue": "Updated Client Name",
  "displayOrder": 3,
  "isActive": true
}
```

**Parameters:**

- `id` (path): Dropdown value ID

**Description:** Updates an existing dropdown value. All fields are updatable including the subcategory field.

**Access Control:** Requires ADMIN role

**Response:**

```json
{
    "id": 10,
    "category": "CLIENT",
    "subcategory": "GENERAL",
    "itemValue": "Updated Client Name",
    "displayOrder": 3,
    "isActive": true
}
```

#### Delete Dropdown Value

```http
DELETE /api/dropdowns/{id}
```

**Parameters:**

- `id` (path): Dropdown value ID

**Description:** Deletes a dropdown value. Cannot delete values that are referenced by existing tasks.

**Access Control:** Requires ADMIN role

**Response:** 204 No Content (success) or 400 Bad Request (if value is in use)

**Implementation Notes:**

- **Dynamic Categories**: The API automatically supports any new categories added to the database without code changes
- **Subcategory Support**: New subcategory field allows for finer-grained categorization within categories
- **Cascading Filters**: Admin UI (both Thymeleaf and Angular) provides category and subcategory filters with auto-reset behavior
- **Filter-First Design**: The Angular UI enforces category selection before allowing value creation to prevent errors
- **Consolidated Management**: Single set of endpoints handles all dropdown categories, eliminating duplicate code
- **Active/Inactive Support**: Values can be marked inactive instead of deleted to maintain referential integrity
- **Automatic Ordering**: Display order is automatically assigned when creating new values, sorted by displayOrder then itemValue
- **Validation**: Backend validates category names and prevents duplicate values within the same category and subcategory combination

### Web Controller Endpoints

The application provides web-based endpoints for user interaction through Thymeleaf templates.

#### Task List Endpoint

```http
GET /task-activity/list
```

**Query Parameters:**

- `client` (optional): Filter by client name
- `project` (optional): Filter by project name
- `phase` (optional): Filter by phase name
- `username` (optional, admin only): Filter by specific username
- `startDate` (optional): Filter by start date (ISO date format)
- `endDate` (optional): Filter by end date (ISO date format)
- `page` (optional, default=0): Page number for pagination

**Access Control:**

- Regular users: Automatically filtered to show only their own tasks
- Administrators: Can view all tasks or filter by specific username

**Response:** Renders `task-list.html` template with paginated results (20 per page)

**Example Usage:**

```
# View all tasks (admin)
GET /task-activity/list

# Filter by specific user (admin)
GET /task-activity/list?username=jdoe

# Filter by date range
GET /task-activity/list?startDate=2025-10-01&endDate=2025-10-31

# Combined filters (admin)
GET /task-activity/list?username=jdoe&client=Client%20A&page=1
```

**Implementation Notes:**

- Uses `TaskActivityWebController.showTaskList()` method
- Automatically enforces role-based access control
- Supports in-memory phase filtering due to database limitations
- Returns 20 tasks per page, sorted by date (desc), client (asc), project (asc)

#### Weekly Timesheet Endpoint

```http
GET /task-activity/weekly-timesheet
```

**Query Parameters:**

- `weekStartDate` (optional): ISO date for the Monday of the week to display
- If not provided, defaults to current week

**Access Control:**

- Regular users: View only their own tasks
- Administrators: View all tasks across all users

**Response:** Renders `weekly-timesheet.html` with tasks grouped by day

#### Task List CSV Export Endpoint

```http
GET /task-activity/list/export-csv
```

**Query Parameters:**

- `client` (optional): Filter by client name
- `project` (optional): Filter by project name
- `phase` (optional): Filter by phase name
- `username` (optional, admin only): Filter by specific username
- `startDate` (optional): Filter by start date (ISO date format)
- `endDate` (optional): Filter by end date (ISO date format)

**Access Control:**

- Regular users: Automatically filtered to export only their own tasks
- Administrators: Can export all tasks or filter by specific username

**Response:** Returns CSV formatted string with all filtered tasks (bypasses pagination)

**CSV Format:**

```csv
Date,Client,Project,Phase,Hours,Details
10/01/2025,Client A,Project X,Development,8.0,"Task details here"
```

**Additional Column for Administrators:**

When accessed by admin users, includes `Username` column before `Details`:

```csv
Date,Client,Project,Phase,Hours,Username,Details
```

**Implementation Notes:**

- Uses `TaskActivityWebController.exportTaskListToCsv()` method
- Fetches ALL filtered tasks, not just the current page (bypasses 20-item pagination limit)
- Automatically enforces role-based access control
- Properly escapes CSV fields containing commas, quotes, or newlines
- Used by the Task List page's CSV export modal for copy/download functionality

#### Task Detail Endpoint

```http
GET /task-activity/detail/{id}
```

**Path Parameters:**

- `id`: Task activity ID

**Access Control:**

- Regular users: Can only access their own tasks
- Administrators: Can access any task
- Returns 403 Forbidden if access denied

**Exception Handling:**

- Throws `TaskActivityNotFoundException` if task not found (404)
- Throws `AccessDeniedException` if user lacks permission (403)

### Custom Exception Handling

**TaskActivityNotFoundException:**

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TaskActivityNotFoundException extends RuntimeException {
    public TaskActivityNotFoundException(String message) {
        super(message);
    }
}
```

**Usage:**

```java
TaskActivity task = taskActivityRepository.findById(id)
    .orElseThrow(() -> new TaskActivityNotFoundException(
        "Task activity not found with id: " + id));
```

**Error Response:**

- HTTP Status: 404 Not Found
- Renders custom error page with appropriate message

## API Documentation (Swagger/OpenAPI)

### Overview

The application includes interactive API documentation using Springdoc OpenAPI (Swagger). This provides a web-based interface to explore and test all REST API endpoints.

**Technology:**

- **Springdoc OpenAPI**: Version 2.6.0
- **OpenAPI Specification**: Version 3.0
- **Swagger UI**: Interactive API documentation interface

### Accessing Swagger UI

Once the application is running, access the API documentation at:

**Primary URLs:**

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Swagger UI (Alternative)**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

**Network Access:**

- From another machine: http://YOUR_IP:8080/swagger-ui.html

### Configuration

**Location**: `src/main/java/com/ammons/taskactivity/config/OpenApiConfig.java`

The OpenAPI configuration defines:

- API metadata (title, version, description)
- Contact information
- Server environments (local and production)
- Security schemes (Basic Authentication)

**Application Properties** (`application.properties`):

```properties
# OpenAPI/Swagger Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.disable-swagger-default-url=true
springdoc.show-actuator=true
springdoc.packages-to-scan=com.ammons.taskactivity.controller
```

### Security Configuration

Swagger UI endpoints are configured for **public access** by default in development:

```java
// SecurityConfig.java
.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", 
                "/swagger-resources/**", "/webjars/**").permitAll()
```

**Production Considerations:**

For production environments, you should restrict access to Swagger UI:

1. **Disable in Production** (`application-aws.properties` or `application-docker.properties`):
   
   ```properties
   springdoc.swagger-ui.enabled=false
   springdoc.api-docs.enabled=false
   ```

2. **Or Require Authentication** (modify `SecurityConfig.java`):
   
   ```java
   .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
    .hasRole("ADMIN")  // Only admins can view documentation
   ```

### Using Swagger UI

**1. Viewing Endpoints:**

- All REST controllers are automatically discovered and documented
- Endpoints are grouped by controller tags
- Operations are sorted by HTTP method

**2. Testing Endpoints:**

1. Expand any endpoint
2. Click "Try it out"
3. Fill in required parameters
4. Click "Execute"
5. View the response

**3. Authentication with JWT:**

The Swagger UI now supports JWT authentication for testing API endpoints:

1. Navigate to the **Authentication** section in Swagger UI
2. Expand **POST /api/auth/login**
3. Click "Try it out" and enter your credentials:
   ```json
   {
     "username": "admin",
     "password": "your_password"
   }
   ```
4. Click "Execute" and copy the `accessToken` from the response
5. Click the **"Authorize"** button (🔒) at the top of Swagger UI
6. Enter: `Bearer <your-access-token>` (include "Bearer " prefix)
7. Click "Authorize" and close the dialog
8. All subsequent "Try it out" requests will include your JWT token automatically

**Alternative: Basic Authentication (for session-based testing):**

1. Click the "Authorize" button (top right)
2. Enter username and password for HTTP Basic Auth
3. Click "Authorize"
4. All subsequent requests include authentication via session

**Note**: JWT authentication is recommended for API testing as it's stateless and better represents how external API clients will integrate with the system.

**For detailed API usage guide, see**: [Swagger API Guide](SWAGGER_API_GUIDE.md)

**4. Response Codes:**

- `200` - Success
- `201` - Created
- `400` - Bad Request (validation errors)
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `500` - Internal Server Error

### Enhancing API Documentation

**Controller-Level Documentation:**

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    @Operation(
        summary = "Get application health status", 
        description = "Returns detailed health information including database connectivity"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application is healthy"),
        @ApiResponse(responseCode = "503", description = "Application is unhealthy")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        // Implementation
    }
}
```

**Model Documentation:**

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Task Activity model representing a work task")
public class TaskActivity {

    @Schema(description = "Unique identifier", example = "1")
    private Long id;

    @Schema(description = "Client name", example = "Acme Corp")
    private String client;

    @Schema(description = "Project name", example = "Website Redesign")
    private String project;
}
```

### Exporting API Specification

The OpenAPI specification can be exported and used for:

- Generating client libraries (TypeScript, Java, Python, etc.)
- API testing tools (Postman, Insomnia)
- Documentation generation

**Export OpenAPI JSON:**

```bash
curl http://localhost:8080/v3/api-docs -o openapi.json
```

**Generate API Clients:**

```bash
# Install OpenAPI Generator
npm install -g @openapitools/openapi-generator-cli

# Generate TypeScript client
openapi-generator-cli generate -i openapi.json -g typescript-axios -o client/typescript

# Generate Java client
openapi-generator-cli generate -i openapi.json -g java -o client/java
```

### Additional Resources

- **Springdoc Documentation**: https://springdoc.org/
- **OpenAPI Specification**: https://swagger.io/specification/
- **Full Guide**: See `localdocs/SWAGGER_API_DOCUMENTATION.md`

## Security Implementation

### Spring Security Configuration

**Location**: `src/main/java/com/ammons/taskactivity/config/SecurityConfig.java`

The application implements role-based access control with three distinct user roles:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // API endpoints - GUEST has read-only access
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN", "GUEST")
                .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

                // Task Activity screens - GUEST can view
                .requestMatchers("/task-activity/list", "/task-activity/detail/**")
                    .hasAnyRole("USER", "ADMIN", "GUEST")
                .requestMatchers("/task-activity/add", "/task-activity/clone/**")
                    .hasAnyRole("USER", "ADMIN", "GUEST")
                .requestMatchers("/task-activity/submit", "/task-activity/update/**", "/task-activity/delete/**")
                    .hasAnyRole("USER", "ADMIN")

                // Admin-only endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/task-activity/manage-users/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/task-activity")
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
            );
        return http.build();
    }
}
```

**GUEST Role Restrictions:**

While GUEST users can access certain URLs (like `/task-activity/add`), the UI and controller logic prevent them from:

- Submitting new tasks (form submission blocked by `@PreAuthorize("hasRole('USER')")`)
- Editing existing tasks (UI shows disabled form fields)
- Deleting tasks (Delete button hidden in UI)
- Accessing weekly timesheet
- Managing users or dropdown values
- Cloning tasks (Clone button hidden in UI)
- **Changing passwords** (PasswordChangeController blocks GUEST role from accessing password change pages)
- **Logging in with expired passwords** (CustomAuthenticationProvider blocks authentication for GUEST users with expired passwords)

**GUEST Password Expiration Handling:**

Since GUEST users cannot change their own passwords, expired password scenarios require administrator intervention:

1. **Authentication Blocked**: GUEST users with expired passwords cannot log in
2. **Error Message**: Login page displays "Password has expired. Contact system administrator."
3. **No Warnings**: Password expiration warnings are suppressed for GUEST users
4. **Administrator Action Required**: An ADMIN must reset the GUEST user's password and update the expiration date

### Session Timeout Handling

**Configuration**: The application uses a 30-minute session timeout configured in `application.properties`:

```properties
server.servlet.session.timeout=30m
```

**Custom Authentication Entry Point**: When a session expires, the application provides a user-friendly redirect to the login page with an informative message instead of showing a browser popup or 404 error.

**Location**: `src/main/java/com/ammons/taskactivity/config/SecurityConfig.java`

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // ... authorization rules
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
                // Check if this is a session timeout (had session but expired)
                if (request.getRequestedSessionId() != null && 
                    !request.isRequestedSessionIdValid()) {
                    // Session timeout - redirect to login with timeout message
                    response.sendRedirect("/login?timeout=true");
                } else {
                    // First visit or never had session - normal login redirect
                    response.sendRedirect("/login");
                }
            })
        )
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/task-activity")
        )
        .sessionManagement(session -> session
            .sessionFixation().migrateSession()
            .maximumSessions(1)
            .maxSessionsPreventsLogin(false)
        );
    return http.build();
}
```

**Login Template**: `src/main/resources/templates/login.html` displays the timeout message:

```html
<div th:if="${param.timeout}" class="alert alert-warning">
    ⚠️ Your session has expired. Please log in again.
</div>
```

**Benefits:**

- **User-friendly**: Clear message explaining why they need to log in again
- **No browser popup**: Avoids confusing browser authentication dialogs
- **No 404 errors**: Graceful handling instead of error pages
- **Consistent UX**: Same login page with contextual messaging

### Password Validation

**Custom Annotation**: `@ValidPassword`

```java
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password does not meet requirements";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**Validator Implementation**:

```java
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 10;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String DIGIT_PATTERN = ".*\\d.*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[+&%$#@!~].*";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < MIN_LENGTH) {
            setMessage(context, "Password must be at least 10 characters long");
            return false;
        }

        if (!password.matches(UPPERCASE_PATTERN)) {
            setMessage(context, "Password must contain at least 1 uppercase letter");
            return false;
        }

        if (!password.matches(DIGIT_PATTERN)) {
            setMessage(context, "Password must contain at least 1 numeric digit");
            return false;
        }

        if (!password.matches(SPECIAL_CHAR_PATTERN)) {
            setMessage(context, "Password must contain at least 1 special character (+&%$#@!~)");
            return false;
        }

        return true;
    }
}
```

### Role-Based Access Control

**User Roles:**

- `USER`: Basic task management access (can only view/edit their own tasks)
- `ADMIN`: Full administrative access (can view/edit all tasks and manage users)
- `GUEST`: Read-only access (can view task list and task details but cannot create, edit, or delete tasks)

**Controller Security:**

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public String manageUsers(Model model) {
    // Admin-only functionality
}
```

**Task Access Control:**

The application enforces role-based access for task operations:

```java
@GetMapping("/list")
public String showTaskList(
        @RequestParam(required = false) String username,
        // ... other parameters
        Authentication authentication) {

    // Check if user is admin
    boolean isUserAdmin = isAdmin(authentication);
    String currentUsername = isUserAdmin ? null : getUsername(authentication);

    // Determine filter username
    // Admin can filter by specific user; regular users see only their tasks
    String filterUsername =
        (isUserAdmin && username != null && !username.trim().isEmpty())
            ? username
            : currentUsername;

    // Fetch tasks accordingly
    Page<TaskActivity> tasksPage = fetchTasksPage(
        client, project, startDate, endDate, pageable,
        isUserAdmin, filterUsername);
}
```

**Access Rules:**

- **USER**: Can view, create, edit, and delete only their own tasks (filtered by authenticated username)

- **ADMIN**: Can view all tasks, filter by specific username, and manage all users and tasks

- **GUEST**: Can view task list and task details in read-only mode. Cannot create, edit, delete tasks, or access administrative features like user management or dropdown management. UI controls (buttons and form fields) are disabled or hidden for GUEST users.

- Edit/delete operations verify ownership or admin role before allowing modifications

- Task detail view checks permissions and throws `AccessDeniedException` if unauthorized

**Role Permission Matrix:**

| Feature            | GUEST         | USER          | ADMIN         |
| ------------------ | ------------- | ------------- | ------------- |
| View Task List     | ✅             | ✅             | ✅             |
| View Task Details  | ✅ (read-only) | ✅             | ✅             |
| Create New Task    | ❌             | ✅ (own tasks) | ✅             |
| Edit Task          | ❌             | ✅ (own tasks) | ✅ (all tasks) |
| Delete Task        | ❌             | ✅ (own tasks) | ✅ (all tasks) |
| Clone Task         | ❌             | ✅             | ✅             |
| Weekly Timesheet   | ❌             | ✅             | ✅             |
| Manage Users       | ❌             | ❌             | ✅             |
| Manage Dropdowns   | ❌             | ❌             | ✅             |
| Change Password    | ❌             | ✅             | ❌             |
| Filter by Username | ❌             | ❌             | ✅             |

### JWT Authentication for REST API

The application provides **dual authentication mechanisms** to support both web browser access and API integration:

1. **Form-Based Authentication**: Session-based authentication for web UI (Thymeleaf and Angular)
2. **JWT Authentication**: Token-based authentication for REST API clients

**Why JWT?**

JWT (JSON Web Token) authentication enables:
- **Stateless API access**: No server-side session required
- **API integration**: External applications can authenticate and consume the REST API
- **Swagger UI testing**: "Try It Out" functionality in Swagger UI
- **Mobile/SPA support**: Token-based auth suitable for single-page applications
- **Microservices ready**: Tokens can be validated by multiple services

#### JWT Configuration

**Location**: `src/main/java/com/ammons/taskactivity/security/JwtUtil.java`

**Token Configuration** (application.properties):

```properties
# JWT Secret Key (should be changed in production and stored securely)
jwt.secret=taskactivity-secret-key-change-this-in-production-must-be-at-least-256-bits-long

# Access Token Expiration (24 hours in milliseconds)
jwt.expiration=86400000

# Refresh Token Expiration (7 days in milliseconds)
jwt.refresh.expiration=604800000
```

**Key Components:**

1. **JwtUtil** (`src/main/java/com/ammons/taskactivity/security/JwtUtil.java`):
   - Generates access tokens (24 hour expiration)
   - Generates refresh tokens (7 day expiration)
   - Validates tokens
   - Extracts user information from tokens

2. **JwtAuthenticationFilter** (`src/main/java/com/ammons/taskactivity/security/JwtAuthenticationFilter.java`):
   - Intercepts requests with `Authorization: Bearer <token>` headers
   - Validates JWT tokens
   - Sets Spring Security authentication context

3. **ApiAuthController** (`src/main/java/com/ammons/taskactivity/controller/ApiAuthController.java`):
   - **POST /api/auth/login**: Authenticate with username/password, receive JWT tokens
   - **POST /api/auth/refresh**: Refresh expired access token using refresh token

#### Authentication Workflow

**Step 1: Login and Obtain Tokens**

```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "your_password"
}
```

**Response:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "admin"
}
```

**Step 2: Use Access Token for API Calls**

```bash
GET /api/tasks
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Step 3: Refresh Token When Access Token Expires**

```bash
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "admin"
}
```

#### Security Configuration Integration

**Location**: `src/main/java/com/ammons/taskactivity/config/SecurityConfig.java`

The JWT filter is added to the Spring Security filter chain:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http, 
                                      JwtAuthenticationFilter jwtAuthenticationFilter) {
    http
        .authorizeHttpRequests(auth -> auth
            // JWT auth endpoints - public access
            .requestMatchers("/api/auth/**").permitAll()
            
            // API endpoints - require authentication (JWT or session)
            .requestMatchers("/api/**").authenticated()
            
            // ... other rules
        )
        // Add JWT filter before UsernamePasswordAuthenticationFilter
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        // ... other configuration
}
```

#### Using JWT with Swagger UI

The Swagger UI is configured with JWT authentication support:

1. Navigate to **http://localhost:8080/swagger-ui.html**
2. Find the **POST /api/auth/login** endpoint under "Authentication"
3. Click "Try it out" and enter your credentials
4. Copy the `accessToken` from the response
5. Click the **"Authorize"** button (🔒) at the top of Swagger UI
6. Enter: `Bearer <your-access-token>`
7. Click "Authorize" and close the dialog
8. Now all "Try It Out" buttons will include your JWT token automatically

**OpenAPI Configuration** (`src/main/java/com/ammons/taskactivity/config/OpenApiConfig.java`):

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("Task Activity Management API")
                    .version("1.0.0")
                    .description("REST API with JWT authentication..."))
            .components(new Components()
                    .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
}
```

#### Security Best Practices

**Production Deployment:**

1. **Change JWT Secret**: Replace the default secret with a strong, randomly generated key
2. **Environment Variables**: Store secret in environment variables or secrets manager (AWS Secrets Manager, Kubernetes Secrets)
3. **HTTPS Only**: Always use HTTPS in production to protect tokens in transit
4. **Token Storage**: Client applications should store tokens securely (not in localStorage for sensitive apps)
5. **Token Expiration**: Adjust expiration times based on security requirements
6. **Refresh Token Rotation**: Consider implementing refresh token rotation for enhanced security

**Example Production Configuration:**

```properties
# Use environment variable for secret
jwt.secret=${JWT_SECRET:fallback-secret-for-dev-only}

# Shorter expiration for production
jwt.expiration=3600000  # 1 hour
jwt.refresh.expiration=604800000  # 7 days
```

#### Integration with Existing Authentication

The JWT authentication works **alongside** the existing form-based authentication:

- **Web UI (Thymeleaf/Angular)**: Uses form-based authentication with sessions
- **REST API clients**: Use JWT authentication with Bearer tokens
- **Swagger UI**: Uses JWT authentication for "Try It Out" functionality
- **Both methods**: Can be used simultaneously (JWT for API, sessions for web)

The `JwtAuthenticationFilter` only activates when it detects a `Bearer` token in the `Authorization` header, so it doesn't interfere with session-based authentication.

#### JWT Authentication DTOs

**Request/Response Models:**

```java
// LoginRequest.java
public class LoginRequest {
    @NotBlank
    private String username;
    
    @NotBlank
    private String password;
}

// LoginResponse.java
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private String username;
}

// RefreshTokenRequest.java
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}
```

**For complete API usage guide, see**: [Swagger API Guide](SWAGGER_API_GUIDE.md)

### User Display Name Format

**Implementation**: All controllers that display user information use a consistent helper method to format user display names.

**Controller Helper Method Pattern:**

```java
/**
 * Add user display information to the model
 */
private void addUserDisplayInfo(Model model, Authentication authentication) {
    String username = authentication.getName();
    userService.getUserByUsername(username).ifPresent(user -> {
        String firstname = user.getFirstname();
        String lastname = user.getLastname();
        String displayName = (firstname + " " + lastname + " (" + username + ")").trim()
                .replaceAll("\\s+", " ");
        model.addAttribute("userDisplayName", displayName);
    });
}
```

**Template Usage:**

```html
<span th:text="${userDisplayName}">Username</span>
```

**Key Implementation Notes:**

- Display format: "Firstname Lastname (username)"
- Handles null/empty first names gracefully
- Normalizes multiple spaces to single space
- Used consistently across all templates
- Implemented in: TaskActivityWebController, UserManagementController, PasswordChangeController, DropdownAdminController, ErrorController

### User Management Service Methods

The **UserService** provides comprehensive user management functionality including filtering capabilities.

**Core Methods:**

```java
// Retrieve all users
public List<User> getAllUsers()

// Filter users by multiple criteria
public List<User> filterUsers(String username, Role role, String company)

// Get user by ID
public Optional<User> getUserById(Long id)

// Get user by username
public Optional<User> getUserByUsername(String username)

// Create new user
public User createUser(String username, String firstname, String lastname, 
                      String company, String password, Role role, 
                      Boolean forcePasswordUpdate)

// Update user information
public User updateUser(Long id, String username, String firstname, 
                      String lastname, String company, Role role, Boolean enabled)

// Change user password
public void changePassword(String username, String newPassword, 
                          Boolean forcePasswordUpdate)

// Delete user
public void deleteUser(Long id)

// Update last login timestamp
public void updateLastLoginTime(String username)
```

**Filtering Functionality:**

The `filterUsers()` method enables flexible user searching on the User Management screen:

```java
public List<User> filterUsers(String username, Role role, String company) {
    List<User> users = userRepository.findAll();

    // Username filter: case-insensitive partial match
    if (username != null && !username.trim().isEmpty()) {
        String lowerUsername = username.toLowerCase().trim();
        users = users.stream()
                .filter(user -> user.getUsername().toLowerCase().contains(lowerUsername))
                .toList();
    }

    // Role filter: exact match
    if (role != null) {
        users = users.stream()
                .filter(user -> user.getRole().equals(role))
                .toList();
    }

    // Company filter: case-insensitive partial match (excludes null company values)
    if (company != null && !company.trim().isEmpty()) {
        String lowerCompany = company.toLowerCase().trim();
        users = users.stream()
                .filter(user -> user.getCompany() != null && 
                               user.getCompany().toLowerCase().contains(lowerCompany))
                .toList();
    }

    return users;
}
```

**Filter Behavior:**

- **Username**: Case-insensitive partial match (e.g., "john" matches "johndoe", "JohnSmith")
- **Role**: Exact match from enum values (ADMIN, USER, VIEWER)
- **Company**: Case-insensitive partial match, excludes users with null company values
- **Combined**: Multiple filters are applied in sequence (AND logic)
- **No Filters**: Returns all users when no filter parameters are provided

**Controller Integration:**

```java
@GetMapping
public String manageUsers(@RequestParam(required = false) String username,
                         @RequestParam(required = false) Role role,
                         @RequestParam(required = false) String company,
                         Model model, Authentication authentication) {

    List<User> users;

    // Apply filters if any are present
    if ((username != null && !username.trim().isEmpty()) || 
        role != null || 
        (company != null && !company.trim().isEmpty())) {
        users = userService.filterUsers(username, role, company);
    } else {
        users = userService.getAllUsers();
    }

    model.addAttribute("users", users);
    model.addAttribute("filterUsername", username != null ? username : "");
    model.addAttribute("filterRole", role);
    model.addAttribute("filterCompany", company != null ? company : "");

    return "admin/user-management";
}
```

**Template Implementation:**

The User Management template includes a filter section with form inputs:

```html
<div class="filter-section">
    <form th:action="@{/task-activity/manage-users}" method="get">
        <div class="filter-group">
            <label for="username">Username:</label>
            <input type="text" id="username" name="username" 
                   th:value="${filterUsername}" />
        </div>
        <div class="filter-group">
            <label for="role">Role:</label>
            <select id="role" name="role">
                <option value="">All Roles</option>
                <option th:each="r : ${roles}" 
                        th:value="${r}" 
                        th:text="${r}"
                        th:selected="${filterRole == r}"></option>
            </select>
        </div>
        <div class="filter-group">
            <label for="company">Company:</label>
            <input type="text" id="company" name="company" 
                   th:value="${filterCompany}" />
        </div>
        <button type="submit" class="btn btn-primary">Search</button>
        <a th:href="@{/task-activity/manage-users}" class="btn btn-secondary">
            Reset filters
        </a>
    </form>
</div>
```

### Task Username Tracking

**Automatic Population**: The username field in TaskActivity is automatically populated from the authenticated user's context during task creation.

**Controller Implementation:**

```java
@PostMapping("/submit")
public String submitForm(@Valid @ModelAttribute TaskActivityDto taskActivityDto,
        BindingResult bindingResult, Model model, Authentication authentication,
        RedirectAttributes redirectAttributes) {

    // Set username from authenticated user
    String username = authentication.getName();
    taskActivityDto.setUsername(username);

    // Save task
    TaskActivity savedTask = taskActivityService.save(taskActivityDto);
    // ...
}
```

**Data Integrity Rules:**

- Username is set during creation and cannot be modified
- Foreign key constraint to Users table prevents invalid usernames
- ON DELETE RESTRICT prevents deletion of users with tasks
- ON UPDATE CASCADE maintains referential integrity if username changes

**UI Enforcement:**

The User Management interface prevents user deletion violations before they reach the database:

```java
// In UserManagementController.manageUsers()
Map<String, Boolean> userHasTasks = new HashMap<>();
for (User user : users) {
    userHasTasks.put(user.getUsername(),
        taskActivityService.userHasTaskActivities(user.getUsername()));
}
model.addAttribute("userHasTasks", userHasTasks);
```

**Delete Button States:**

- **Enabled**: User has no task activities and is not the current user
- **Disabled (own account)**: Shows tooltip "Cannot delete your own account"
- **Disabled (has tasks)**: Shows tooltip "Cannot delete user with existing task entries"

This proactive approach prevents database constraint violations and provides clear user feedback before an action is attempted.

## Secrets Management

The Task Activity Management System implements comprehensive secrets management to protect sensitive data like database credentials, API keys, and other confidential information. This section covers the various approaches available for different deployment scenarios.

### Overview

**Security Principles:**

- Never store secrets in source code or configuration files
- Use environment variables for local development  
- Implement proper secrets management for production deployments
- Support multiple deployment platforms (Docker, Kubernetes, etc.)
- Provide secure fallback mechanisms without hardcoded values

### Local Development

#### Environment Variables

For local development, use environment variables to provide database credentials:

**Unix/Linux/Mac:**

```bash
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_secure_password
```

**Windows Command Prompt:**

```cmd
set DB_USERNAME=your_db_user
set DB_PASSWORD=your_secure_password
```

**Windows PowerShell:**

```powershell
$env:DB_USERNAME="your_db_user"
$env:DB_PASSWORD="your_secure_password"
```

#### .env File Support

The application supports `.env` files for local development convenience:

1. **Copy the template:**
   
   ```bash
   cp .env.example .env
   ```

2. **Edit with your credentials:**
   
   ```bash
   # .env file
   DB_USERNAME=postgres
   DB_PASSWORD=your_secure_password
   SPRING_PROFILES_ACTIVE=local
   ```

3. **Load environment variables:**
   
   ```bash
   # Using direnv (recommended)
   direnv allow
   
   # Or source manually
   set -a; source .env; set +a
   ```

**Important Notes:**

- The `.env` file is automatically ignored by git
- Never commit actual credentials to version control
- Use strong passwords (minimum 12 characters, mixed case, numbers, symbols)

### Docker Deployment

#### Environment Variables (Development)

For development Docker deployments, use environment variables:

```bash
docker run -d \
  -p 8080:8080 \
  -e DB_USERNAME=dev_user \
  -e DB_PASSWORD=dev_password \
  -e SPRING_PROFILES_ACTIVE=docker \
  taskactivity:latest
```

**Docker Compose with .env:**

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
```

#### Docker Secrets (Production)

For production deployments, use Docker secrets for enhanced security:

1. **Initialize Docker Swarm:**
   
   ```bash
   docker swarm init
   ```

2. **Create secrets:**
   
   ```bash
   # Run the setup script
   ./scripts/setup-docker-secrets.sh
   
   # Or create manually
   echo "production_user" | docker secret create taskactivity_db_username -
   echo "super_secure_password" | docker secret create taskactivity_db_password -
   ```

3. **Deploy with secrets:**
   
   ```bash
   docker-compose --profile production up -d
   ```

**How Docker Secrets Work:**

The application includes a custom `SecretsEnvironmentPostProcessor` that automatically reads secrets from files when the following environment variables are set:

- `DB_USERNAME_FILE=/run/secrets/db_username`
- `DB_PASSWORD_FILE=/run/secrets/db_password`

**Production docker-compose.yml:**

```yaml
services:
  app-production:
    profiles: ["production"]
    environment:
      - DB_USERNAME_FILE=/run/secrets/db_username
      - DB_PASSWORD_FILE=/run/secrets/db_password
    secrets:
      - db_username
      - db_password

secrets:
  db_username:
    external: true
    name: taskactivity_db_username
  db_password:
    external: true
    name: taskactivity_db_password
```

### Kubernetes Deployment

#### Kubernetes Secrets

For Kubernetes deployments, use native Kubernetes secrets:

1. **Create secrets:**
   
   ```bash
   # Using the deployment script
   ./scripts/deploy-k8s.sh
   
   # Or create manually
   kubectl create secret generic taskactivity-db-credentials \
     --from-literal=username=prod_user \
     --from-literal=password=super_secure_password \
     -n taskactivity
   ```

2. **Reference in deployment:**
   
   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   spec:
     template:
       spec:
         containers:
         - name: taskactivity
           env:
           - name: DB_USERNAME
             valueFrom:
               secretKeyRef:
                 name: taskactivity-db-credentials
                 key: username
           - name: DB_PASSWORD
             valueFrom:
               secretKeyRef:
                 name: taskactivity-db-credentials
                 key: password
   ```

#### External Secrets Operator

For advanced Kubernetes deployments, consider using External Secrets Operator:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: vault-backend
spec:
  provider:
    vault:
      server: "https://vault.company.com"
      path: "secret"
      version: "v2"
      auth:
        kubernetes:
          mountPath: "kubernetes"
          role: "taskactivity-role"
---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: taskactivity-db-secret
spec:
  refreshInterval: 15s
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: taskactivity-db-credentials
  data:
  - secretKey: username
    remoteRef:
      key: taskactivity/database
      property: username
  - secretKey: password
    remoteRef:
      key: taskactivity/database
      property: password
```

### Cloud Provider Solutions

#### AWS Secrets Manager

```yaml
# Using AWS Secrets Manager with EKS
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-west-2
      auth:
        serviceAccount:
          name: taskactivity-sa
```

#### Azure Key Vault

```yaml
# Using Azure Key Vault with AKS
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: azure-keyvault
spec:
  provider:
    azurekv:
      vaultUrl: "https://taskactivity-kv.vault.azure.net/"
      tenantId: "tenant-id"
      authSecretRef:
        clientId:
          name: azure-secret
          key: client-id
        clientSecret:
          name: azure-secret
          key: client-secret
```

#### Google Secret Manager

```yaml
# Using Google Secret Manager with GKE
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: google-secret-manager
spec:
  provider:
    gcpsm:
      projectId: "your-project-id"
      auth:
        workloadIdentity:
          serviceAccountRef:
            name: taskactivity-sa
```

### Implementation Details

#### SecretsEnvironmentPostProcessor

**Location:** `src/main/java/com/ammons/taskactivity/config/SecretsEnvironmentPostProcessor.java`

This component automatically reads secrets from files when file-based environment variables are detected:

```java
@Override
public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Properties secrets = new Properties();

    // Check for username file
    String usernameFile = environment.getProperty("DB_USERNAME_FILE");
    if (usernameFile != null && !usernameFile.isEmpty()) {
        String username = readSecretFromFile(usernameFile, "DB_USERNAME");
        if (username != null) {
            secrets.setProperty("DB_USERNAME", username);
        }
    }

    // Similar for password file...

    if (!secrets.isEmpty()) {
        environment.getPropertySources().addFirst(
            new PropertiesPropertySource("docker-secrets", secrets)
        );
    }
}
```

**Supported File Locations:**

- `/run/secrets/` (Docker Swarm secrets)
- `/var/secrets/` (Custom mount points)
- `/etc/secrets/` (Alternative mount points)
- Any path specified in `*_FILE` environment variables

### Security Best Practices

#### Development Environment

- Use `.env` files for local development
- Never commit `.env` files to version control
- Use different credentials for each environment
- Rotate development credentials regularly

#### Production Environment

- Use dedicated secrets management systems
- Implement secrets rotation policies
- Use least-privilege access principles
- Monitor secrets access and usage
- Enable audit logging for secrets operations

#### General Guidelines

- Use strong, unique passwords (minimum 12 characters)
- Implement secrets scanning in CI/CD pipelines
- Use encrypted connections for secrets transmission
- Regularly audit and rotate all secrets
- Implement secrets versioning and rollback capabilities

### Troubleshooting

#### Common Issues

**1. Environment Variables Not Loaded**

```bash
# Check if variables are set
echo $DB_USERNAME
echo $DB_PASSWORD

# Verify Spring Boot can see them
java -jar app.jar --debug
```

**2. Docker Secrets Not Found**

```bash
# Check if secrets exist
docker secret ls

# Verify secret content (be careful in production)
docker secret inspect taskactivity_db_username
```

**3. Kubernetes Secrets Issues**

```bash
# Check secret exists
kubectl get secrets -n taskactivity

# Verify secret content
kubectl describe secret taskactivity-db-credentials -n taskactivity

# Check pod environment
kubectl exec -it pod-name -n taskactivity -- printenv | grep DB_
```

#### Debug Logging

Enable debug logging for secrets processing:

```properties
# application.properties
# Available log levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF
logging.level.com.ammons.taskactivity.config.SecretsEnvironmentPostProcessor=DEBUG
```

This will log when secrets are loaded from files:

```
INFO  - Loaded DB_USERNAME from file: /run/secrets/db_username
INFO  - Loaded DB_PASSWORD from file: /run/secrets/db_password
```

### Migration Guide

#### From Hardcoded to Environment Variables

1. **Remove hardcoded values** from properties files
2. **Set environment variables** in your deployment
3. **Test connectivity** before deploying to production

#### From Environment Variables to Secrets

1. **Create secrets** in your chosen platform
2. **Update deployment configuration** to use secrets
3. **Remove environment variables** from deployment files
4. **Verify application** can access secrets

## Angular Frontend Development

The application includes a modern Angular 19 frontend as an alternative to the Thymeleaf server-side rendered UI. This section covers Angular-specific implementation details.

### Angular Architecture

**Location**: `frontend/` directory

**Key Technologies:**
- **Angular 19**: Standalone components architecture (no NgModules)
- **TypeScript 5.6+**: Type-safe development
- **Angular Material**: Material Design component library
- **RxJS**: Reactive programming with Observables
- **Angular Router**: Client-side routing with guards

**Access:**
- Development: `http://localhost:4200` (Angular dev server)
- Production: `http://localhost:8080/app/*` (served by Spring Boot)

### Authentication and Session Management

The Angular frontend uses HTTP Basic Authentication with session-based storage and implements session timeout handling.

#### Authentication Service

**Location**: `frontend/src/app/services/auth.service.ts`

**Key Features:**
- Stores credentials in sessionStorage
- Provides reactive user state with RxJS Observables
- Fetches user details from `/api/users/me` on app initialization
- Emits username and role changes to subscribers

**Important**: The AuthService constructor makes the `/api/users/me` call automatically when the path starts with `/app` to load user details:

```typescript
constructor(private http: HttpClient) {
  // Auto-load user details when accessing app routes
  if (globalThis.location.pathname.startsWith('/app')) {
    this.http.get<any>(`${environment.apiUrl}/users/me`).subscribe({
      next: (response) => {
        if (response.data) {
          this.currentUserSubject.next(response.data.username);
          this.userRoleSubject.next(response.data.role);
        }
      },
      error: (error) => console.error('Failed to load user details', error)
    });
  }
}
```

#### HTTP Interceptor

**Location**: `frontend/src/app/interceptors/auth.interceptor.ts`

**Purpose**: Handles session management, CSRF tokens, and session timeout redirects.

**Key Features:**

1. **Credentials**: Includes cookies (JSESSIONID) with every request using `withCredentials: true`

2. **CSRF Protection**: Adds `X-XSRF-TOKEN` header for state-changing requests (POST, PUT, DELETE, PATCH)

3. **Session Timeout Handling**: Intercepts 401 errors and redirects to login page with timeout message

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Clone request with credentials
  let authReq = req.clone({ withCredentials: true });

  // Add CSRF token for non-GET requests
  if (req.method !== 'GET' && req.method !== 'HEAD' && req.method !== 'OPTIONS') {
    const csrfToken = getCsrfTokenFromCookie();
    if (csrfToken) {
      authReq = authReq.clone({
        setHeaders: { 'X-XSRF-TOKEN': csrfToken }
      });
    }
  }

  // Handle 401 errors (session timeout)
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Clear session storage
        sessionStorage.removeItem('auth');
        sessionStorage.removeItem('username');
        sessionStorage.removeItem('userRole');
        
        // Redirect to login with timeout message
        globalThis.location.href = '/login?timeout=true';
        return throwError(() => new Error('Session expired'));
      }
      return throwError(() => error);
    })
  );
};
```

**Important**: The interceptor does NOT inject AuthService to avoid circular dependency issues:
- HttpClient uses authInterceptor
- AuthService injects HttpClient
- If authInterceptor injected AuthService → circular dependency → NG0200 error

**Solution**: Interceptor directly clears sessionStorage instead of calling `authService.logout()`.

### User Management Components

#### User Edit Dialog

**Location**: `frontend/src/app/components/user-edit-dialog/`

**Purpose**: Modal dialog for editing user details (Admin only).

**Form Fields:**
- **First Name** (optional) - No validation required
- **Last Name** (required) - Validators.required
- **Company** (required) - Validators.required  
- **Role** (required) - Dropdown with options: GUEST, USER, ADMIN
- **Account Enabled** (checkbox)
- **Force Password Update** (checkbox)
- **Account Locked** (checkbox) - Displays in highlighted section when account is locked; admin can uncheck to unlock
- **Failed Login Attempts** (read-only) - Shows count of failed login attempts when applicable

**Implementation Note**: First Name field was changed from required to optional. Both the TypeScript form validator and HTML `required` attribute were removed:

```typescript
this.userForm = this.fb.group({
  firstname: [data.user.firstname],  // No Validators.required
  lastname: [data.user.lastname, Validators.required],
  company: [data.user.company, Validators.required],
  role: [data.user.role, Validators.required],
  enabled: [data.user.enabled],
  forcePasswordUpdate: [data.user.forcePasswordUpdate],
  accountLocked: [data.user.accountLocked],  // Added for account lockout
});
```

```html
<mat-form-field appearance="outline" class="full-width">
  <mat-label>First Name</mat-label>
  <input matInput formControlName="firstname" />  <!-- No required attribute -->
</mat-form-field>

<!-- Account Lock Status Section -->
<div class="lock-status-section" *ngIf="data.user.accountLocked || data.user.failedLoginAttempts">
  <h3>Account Lock Status</h3>
  <p class="lock-info">
    <strong>Failed Login Attempts:</strong> {{ data.user.failedLoginAttempts || 0 }}
  </p>
  <mat-checkbox 
    formControlName="accountLocked"
    [disabled]="!data.user.accountLocked"
    *ngIf="data.user.accountLocked"
  >
    <span class="locked-text">🔒 Account is Locked - Uncheck to unlock</span>
  </mat-checkbox>
</div>
```

**Styling Fix**: To prevent label clipping, the dialog content uses proper padding and the first form field has top margin:

```scss
mat-dialog-content {
  min-width: 500px;
  padding: 20px 24px;
}

.full-width {
  width: 100%;
  margin-bottom: 16px;
  
  &:first-of-type {
    margin-top: 8px;  // Prevents "First Name" label from being cut off
  }
}
```

### Task Activity Management Components

#### Task List Component

**Location**: `frontend/src/app/components/task-list/`

**Purpose**: Main dashboard for displaying, creating, editing, cloning, and deleting task activities.

**Features:**

- **View Task Activities**: Paginated table with sorting and filtering
- **Add Task**: Create new task activities via toolbar button
- **Edit Task**: Modify existing task activities via Actions column
- **Clone Task**: Duplicate existing tasks with today's date
- **Delete Task**: Remove task activities with confirmation dialog
- **Mobile Responsive**: Horizontal scrolling on mobile devices

**User Interface Elements:**

1. **Toolbar**:
   - **Add Task Button** (`<button mat-raised-button color="primary">`): Opens add task dialog
   - **Refresh Button** (`mat-icon-button`): Reloads task list
   - Search and filter controls

2. **Actions Column** (per row):
   - **Edit Button** (`mat-icon-button` with `edit` icon): Opens edit dialog for selected task
   - **Clone Button** (`mat-icon-button` with `content_copy` icon): Creates duplicate of selected task
   - **Delete Button** (`mat-icon-button` with `delete` icon): Deletes selected task with confirmation

**Key Methods:**

```typescript
export class TaskListComponent implements OnInit {

  // Create new task with today's date
  addTask(): void {
    const newTask: TaskActivity = {
      taskDate: new Date(),
      client: '',
      project: '',
      phase: '',
      hours: 0,
      details: '',
      username: this.authService.getCurrentUsername() || '',
    };

    const dialogRef = this.dialog.open(TaskEditDialogComponent, {
      width: '600px',
      data: { task: newTask, isAddMode: true },
    });

    dialogRef.afterClosed().subscribe((result: TaskActivity | undefined) => {
      if (result) {
        this.taskService.createTask(result).subscribe({
          next: () => {
            this.loadTasks();
            this.snackBar.open('Task created successfully', 'Close', {
              duration: 3000,
            });
          },
          error: (error) => {
            console.error('Error creating task:', error);
            this.snackBar.open('Failed to create task', 'Close', {
              duration: 3000,
            });
          },
        });
      }
    });
  }

  // Clone existing task with today's date
  cloneTask(task: TaskActivity): void {
    const clonedTask: TaskActivity = {
      ...task,
      id: undefined, // Remove ID to create new record
      taskDate: new Date(), // Set to today
    };

    const dialogRef = this.dialog.open(TaskEditDialogComponent, {
      width: '600px',
      data: { task: clonedTask, isAddMode: true },
    });

    dialogRef.afterClosed().subscribe((result: TaskActivity | undefined) => {
      if (result) {
        this.taskService.createTask(result).subscribe({
          next: () => {
            this.loadTasks();
            this.snackBar.open('Task cloned successfully', 'Close', {
              duration: 3000,
            });
          },
          error: (error) => {
            console.error('Error cloning task:', error);
            this.snackBar.open('Failed to clone task', 'Close', {
              duration: 3000,
            });
          },
        });
      }
    });
  }

  // Edit existing task
  editTask(task: TaskActivity): void {
    const dialogRef = this.dialog.open(TaskEditDialogComponent, {
      width: '600px',
      data: { task: { ...task }, isAddMode: false },
    });

    dialogRef.afterClosed().subscribe((result: TaskActivity | undefined) => {
      if (result && result.id) {
        this.taskService.updateTask(result.id, result).subscribe({
          next: () => {
            this.loadTasks();
            this.snackBar.open('Task updated successfully', 'Close', {
              duration: 3000,
            });
          },
          error: (error) => {
            console.error('Error updating task:', error);
            this.snackBar.open('Failed to update task', 'Close', {
              duration: 3000,
            });
          },
        });
      }
    });
  }
}
```

**Implementation Details:**

1. **Add Task Flow**:
   - User clicks "Add Task" button in toolbar
   - Dialog opens with empty form fields and today's date
   - Dialog title shows "Add Task Activity"
   - On save, calls `taskService.createTask()` (POST request)
   - Success: Shows confirmation, reloads task list

2. **Clone Task Flow**:
   - User clicks clone icon button (content_copy) in Actions column for specific row
   - Copies all task data except ID
   - Sets `taskDate` to today's date
   - Dialog opens with cloned data
   - Dialog title shows "Add Task Activity" (uses `isAddMode: true`)
   - On save, calls `taskService.createTask()` (POST request)
   - Success: Shows "Task cloned successfully", reloads task list

3. **Edit Task Flow**:
   - User clicks edit icon button (pencil) in Actions column
   - Dialog opens with existing task data
   - Dialog title shows "Edit Task Activity" (uses `isAddMode: false`)
   - On save, calls `taskService.updateTask()` (PUT request)
   - Success: Shows confirmation, reloads task list

**Important Notes:**

- Both Add and Clone use the same dialog component (`TaskEditDialogComponent`)
- The `isAddMode` parameter controls dialog title and behavior
- Clone preserves all task details (client, project, phase, hours, details) but updates the date
- All operations use the shared `TaskActivityService` for API calls
- Success/error messages use Angular Material's `MatSnackBar`

#### Task Edit Dialog Component

**Location**: `frontend/src/app/components/task-edit-dialog/`

**Purpose**: Reusable modal dialog for adding and editing task activities.

**Form Fields:**

- **Task Date** (required) - Date picker with validation
- **Client** (required) - Dropdown populated from `DropdownService`
- **Project** (required) - Dropdown populated from `DropdownService`
- **Phase** (required) - Dropdown populated from `DropdownService`
- **Hours** (required) - Number input with min/max validation
- **Details** (optional) - Multi-line text area

**Configuration:**

```typescript
export class TaskEditDialogComponent implements OnInit {
  taskForm: FormGroup;
  isAddMode: boolean;

  constructor(
    public dialogRef: MatDialogRef<TaskEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { task: TaskActivity; isAddMode?: boolean },
    private fb: FormBuilder,
    private dropdownService: DropdownService
  ) {
    this.isAddMode = data.isAddMode || false;
    
    this.taskForm = this.fb.group({
      taskDate: [data.task.taskDate, Validators.required],
      client: [data.task.client, Validators.required],
      project: [data.task.project, Validators.required],
      phase: [data.task.phase, Validators.required],
      hours: [data.task.hours, [Validators.required, Validators.min(0), Validators.max(24)]],
      details: [data.task.details],
    });
  }
}
```

**Template (Dynamic Title):**

```html
<h2 mat-dialog-title>{{ isAddMode ? 'Add Task Activity' : 'Edit Task Activity' }}</h2>

<mat-dialog-content>
  <form [formGroup]="taskForm">
    <mat-form-field appearance="outline" class="full-width">
      <mat-label>Task Date</mat-label>
      <input matInput [matDatepicker]="picker" formControlName="taskDate" required>
      <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
      <mat-datepicker #picker></mat-datepicker>
    </mat-form-field>

    <!-- Client, Project, Phase dropdowns -->
    <!-- Hours input -->
    <!-- Details textarea -->
  </form>
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="onCancel()">Cancel</button>
  <button mat-raised-button color="primary" (click)="onSave()" [disabled]="!taskForm.valid">
    Save
  </button>
</mat-dialog-actions>
```

**Usage Modes:**

| Mode  | `isAddMode` | Dialog Title          | API Call       | ID Handling           |
| ----- | ----------- | --------------------- | -------------- | --------------------- |
| Add   | `true`      | "Add Task Activity"   | `createTask()` | ID undefined/omitted  |
| Clone | `true`      | "Add Task Activity"   | `createTask()` | ID removed from clone |
| Edit  | `false`     | "Edit Task Activity"  | `updateTask()` | ID preserved          |

**Validation:**

- All fields except "Details" are required
- Hours must be between 0 and 24
- Task Date uses Angular Material date picker
- Save button is disabled until form is valid

**Styling:**

```scss
.full-width {
  width: 100%;
  margin-bottom: 16px;
}

mat-dialog-content {
  min-width: 600px;
  padding: 20px 24px;
}
```

### Build Integration

The Angular frontend is automatically built during Maven build using the `frontend-maven-plugin`.

**Maven Configuration** (`pom.xml`):

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.1</version>
    <configuration>
        <workingDirectory>frontend</workingDirectory>
    </configuration>
    <executions>
        <execution>
            <id>npm run build</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <configuration>
                <arguments>run build:prod</arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Build Process:**

```bash
# Maven automatically runs npm build
./mvnw clean package

# Or with Spring Boot run
./mvnw spring-boot:run
```

**Output**: Compiled Angular files are copied to `src/main/resources/static/app/` and served by Spring Boot.

### Development Workflow

**Option 1: Angular Dev Server** (fastest iteration)
```bash
cd frontend
npm start
# Access at http://localhost:4200
```

**Option 2: Integrated with Spring Boot** (production-like)
```bash
# From project root
./mvnw spring-boot:run
# Maven builds Angular automatically
# Access at http://localhost:8080/app/dashboard
```

### Common Issues and Solutions

#### NG0200 Circular Dependency Error

**Symptom**: Console shows "Error message: NG0200: t" and no HTTP requests are made.

**Cause**: Circular dependency between HttpClient and authInterceptor through AuthService:
- HttpClient → uses authInterceptor
- authInterceptor → injects AuthService  
- AuthService → injects HttpClient

**Solution**: Remove AuthService injection from interceptor. Clear sessionStorage directly instead of calling `authService.logout()`.

#### Username/Role Not Loading

**Symptom**: Username doesn't display in header, role-based buttons don't appear.

**Cause**: `/api/users/me` call not being made, usually due to circular dependency blocking HTTP calls.

**Solution**: 
1. Verify no circular dependencies exist (check console for NG0200)
2. Ensure AuthService constructor makes the `/api/users/me` call
3. Check Network tab to verify XHR request is made

#### Build Files Not Updating

**Symptom**: Code changes don't appear after `mvnw spring-boot:run`.

**Cause**: Maven's frontend-maven-plugin runs npm build during Maven lifecycle.

**Solution**: Maven automatically builds Angular. Just restart Spring Boot:
```bash
./mvnw spring-boot:run  # Builds Angular and starts server
```

## Production Automation Scripts

The Task Activity Management System includes a comprehensive set of automation scripts designed to streamline production operations, secrets management, and system maintenance. These scripts are located in the `scripts/` directory and provide enterprise-grade automation for DevOps teams.

### Overview

The automation suite consists of six main scripts:

| Script                    | Purpose                                | Use Case                                 |
| ------------------------- | -------------------------------------- | ---------------------------------------- |
| `setup-production.sh`     | Complete production environment setup  | Initial deployment, infrastructure setup |
| `rotate-secrets.sh`       | Zero-downtime secrets rotation         | Security maintenance, compliance         |
| `backup-secrets.sh`       | Backup, restore, and disaster recovery | Data protection, disaster recovery       |
| `monitor-health.sh`       | Health monitoring and alerting         | Operations, incident detection           |
| `setup-docker-secrets.sh` | Docker Swarm secrets initialization    | Docker-specific secret setup             |
| `test-docker-secrets.sh`  | Secrets functionality validation       | Testing, troubleshooting                 |

### Production Setup Script

**File:** `scripts/setup-production.sh`

**Purpose:** Automated production environment initialization with Docker Swarm, encrypted networks, and optimized system configuration.

**Features:**

- Docker Swarm cluster initialization
- Encrypted overlay network creation
- System optimization for production workloads
- Automated secrets creation with validation
- Security hardening and firewall configuration
- Health monitoring setup
- Production docker-compose deployment

**Usage:**

```bash
# Basic setup
./scripts/setup-production.sh

# Custom configuration
./scripts/setup-production.sh --stack-name=myapp --replicas=3
```

**What it does:**

1. **System Preparation**
   
   - Updates system packages
   - Optimizes kernel parameters
   - Configures Docker daemon settings

2. **Docker Swarm Setup**
   
   - Initializes Swarm cluster
   - Creates encrypted overlay networks
   - Sets up node labels and constraints

3. **Security Configuration**
   
   - Creates Docker secrets for database credentials
   - Configures firewall rules
   - Sets up SSL/TLS certificates

4. **Application Deployment**
   
   - Deploys production stack with health checks
   - Configures load balancing and scaling
   - Validates deployment success

### Secrets Rotation Script

**File:** `scripts/rotate-secrets.sh`

**Purpose:** Zero-downtime rotation of Docker secrets with comprehensive backup and rollback capabilities.

**Features:**

- Zero-downtime rolling updates
- Automatic backup before rotation
- Password strength validation
- Rollback capabilities on failure
- Audit logging and reporting

**Usage:**

```bash
# Interactive rotation
./scripts/rotate-secrets.sh

# Emergency rotation
./scripts/rotate-secrets.sh --emergency

# Scheduled rotation
./scripts/rotate-secrets.sh --scheduled
```

**Security Features:**

- **Password Validation**: Enforces strong password policies
- **Backup Integration**: Automatic backup before changes
- **Health Monitoring**: Validates services after rotation
- **Audit Trail**: Complete logging of rotation activities

### Backup and Recovery Script

**File:** `scripts/backup-secrets.sh`

**Purpose:** Comprehensive backup, restore, and disaster recovery capabilities for Docker secrets and system configuration.

**Features:**

- Encrypted backup creation
- Backup verification and integrity checking
- Disaster recovery procedures
- Automated cleanup of old backups
- Cross-platform restoration support

**Usage:**

```bash
# Create encrypted backup
./scripts/backup-secrets.sh backup

# List available backups
./scripts/backup-secrets.sh list

# Restore from backup
./scripts/backup-secrets.sh restore /path/to/backup

# Disaster recovery
./scripts/backup-secrets.sh disaster-recovery

# Cleanup old backups (keep last 10)
./scripts/backup-secrets.sh cleanup
```

**Backup Components:**

- **Secrets Metadata**: Configuration and creation timestamps
- **Stack Configuration**: Complete service definitions
- **Network Configuration**: Overlay network settings
- **System State**: Swarm cluster information

### Health Monitoring Script

**File:** `scripts/monitor-health.sh`

**Purpose:** Comprehensive application and infrastructure monitoring with real-time alerting capabilities.

**Features:**

- Multi-layer health checking (application, database, system)
- Real-time performance monitoring
- Automated alerting (Slack/Teams webhooks)
- Comprehensive reporting
- Continuous monitoring mode

**Usage:**

```bash
# Single health check
./scripts/monitor-health.sh check

# Continuous monitoring (60s intervals)
./scripts/monitor-health.sh monitor

# Generate detailed report
./scripts/monitor-health.sh report

# View current status
./scripts/monitor-health.sh status
```

**Monitoring Capabilities:**

- **Application Health**: Response times, error rates, endpoint availability
- **Database Health**: Connection pool status, query performance
- **System Resources**: CPU, memory, disk usage
- **Security Monitoring**: Failed login attempts, suspicious activities
- **Docker Services**: Container health, replica status

**Alerting Integration:**

```bash
# Configure webhook for alerts
export WEBHOOK_URL="https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"

# Set custom thresholds
export CPU_THRESHOLD=75
export MEMORY_THRESHOLD=80
export RESPONSE_TIME_THRESHOLD=3000
```

### Docker Secrets Setup Script

**File:** `scripts/setup-docker-secrets.sh`

**Purpose:** Simplified Docker Swarm secrets initialization for development and testing environments.

**Usage:**

```bash
# Interactive setup
./scripts/setup-docker-secrets.sh

# Non-interactive with defaults
./scripts/setup-docker-secrets.sh --auto
```

### Secrets Testing Script

**File:** `scripts/test-docker-secrets.sh`

**Purpose:** Comprehensive validation of Docker secrets functionality and application integration.

**Usage:**

```bash
# Test secrets functionality
./scripts/test-docker-secrets.sh

# Verbose testing
./scripts/test-docker-secrets.sh --verbose
```

### Script Integration Examples

#### Automated Production Deployment

```bash
#!/bin/bash
# Complete production deployment pipeline

# 1. Set up production environment
./scripts/setup-production.sh --stack-name=taskactivity

# 2. Create initial backup
./scripts/backup-secrets.sh backup

# 3. Start monitoring
./scripts/monitor-health.sh monitor &

# 4. Validate deployment
./scripts/test-docker-secrets.sh --validate
```

#### Scheduled Maintenance

```bash
#!/bin/bash
# Weekly maintenance routine

# Rotate secrets (monthly on 1st week)
if [[ $(date +%U) -eq 1 ]]; then
    ./scripts/rotate-secrets.sh --scheduled
fi

# Create backup
./scripts/backup-secrets.sh backup

# Clean old backups
./scripts/backup-secrets.sh cleanup 10

# Health check and report
./scripts/monitor-health.sh report
```

#### CI/CD Integration

```yaml
# GitLab CI example
stages:
  - build
  - test
  - deploy
  - monitor

deploy_production:
  stage: deploy
  script:
    - ./scripts/setup-production.sh --automated
    - ./scripts/test-docker-secrets.sh --validate
  environment:
    name: production
    url: https://taskactivity.company.com

monitor_deployment:
  stage: monitor
  script:
    - ./scripts/monitor-health.sh check
    - ./scripts/backup-secrets.sh backup
  when: on_success
```

### Best Practices

#### Script Security

1. **Permissions**: Set appropriate file permissions
   
   ```bash
   chmod 750 scripts/*.sh
   chown root:docker scripts/*.sh
   ```

2. **Environment Variables**: Use secure environment variable handling
   
   ```bash
   # Secure variable loading
   set -a; source .env; set +a
   unset SENSITIVE_VAR  # Clear after use
   ```

3. **Logging**: Implement secure logging practices
   
   ```bash
   # Mask sensitive data in logs
   echo "Processing user: ${USERNAME:0:3}***" >> logfile
   ```

#### Operational Guidelines

1. **Regular Schedules**
   
   - Daily: Health monitoring
   - Weekly: Backup creation and verification
   - Monthly: Secrets rotation
   - Quarterly: Disaster recovery testing

2. **Change Management**
   
   - Test all scripts in staging environment
   - Use version control for script changes
   - Document script modifications

3. **Monitoring Integration**
   
   - Set up alerts for script failures
   - Monitor script execution times
   - Track resource usage during operations

### Troubleshooting Scripts

#### Common Issues

**Script Permission Errors:**

```bash
# Fix permissions
chmod +x scripts/*.sh

# Check ownership
ls -la scripts/
```

**Docker Swarm Not Initialized:**

```bash
# Initialize manually
docker swarm init

# Or run setup script
./scripts/setup-production.sh
```

**Secrets Already Exist:**

```bash
# List existing secrets
docker secret ls

# Remove if needed (careful!)
docker secret rm old_secret_name
```

**Monitoring Alerts Not Working:**

```bash
# Test webhook
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{"text":"Test alert"}'

# Check environment variables
echo $WEBHOOK_URL
```

For additional support and troubleshooting, refer to the [Operations Runbook](./OPERATIONS_RUNBOOK.md) and [Security Guidelines](./SECURITY.md).

## Development Guidelines

### Code Style

1. **Java Naming Conventions**
   
   - Classes: PascalCase
   - Methods/Variables: camelCase
   - Constants: UPPER_SNAKE_CASE

2. **Package Organization**
   
   - Keep related classes together
   - Follow standard Spring Boot structure
   - Separate concerns (controller/service/repository)

3. **Documentation**
   
   - Add Javadoc for public methods
   - Comment complex business logic
   - Keep README files updated

### Best Practices

1. **Service Layer**
   
   - Keep controllers thin
   - Put business logic in services
   - Use transactions appropriately

2. **Data Validation**
   
   - Validate at DTO level
   - Use Jakarta Validation annotations
   - Provide meaningful error messages

3. **Error Handling**
   
   - Use appropriate exception types
   - Log errors with context
   - Return user-friendly messages
   - Follow exception handling patterns (see Exception Handling section below)

### Exception Handling

**Primary Location**: `src/main/java/com/ammons/taskactivity/exception/GlobalExceptionHandler.java`

The application uses a centralized exception handling approach through `@ControllerAdvice` to provide consistent error responses across all controllers.

#### Global Exception Handler

**Key Features:**

- Centralized exception handling for all `@Controller` classes
- Returns appropriate HTTP status codes and user-friendly error pages
- Uses Java pattern matching for cleaner type checking
- Prioritized exception handlers with `@Order` annotation
- Consistent error model attributes across all exception types

**Implementation:**

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Highest priority handler for missing static resources
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResourceFound(NoResourceFoundException ex, Model model,
            HttpServletResponse response) {
        logger.debug("Static resource not found: {}", ex.getResourcePath());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("errorMessage", "The requested resource was not found.");
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Not Found");
        return "error";
    }

    // Generic exception handler with pattern matching
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model, 
            HttpServletResponse response) {
        // Modern Java pattern matching for instanceof
        if (ex instanceof NoResourceFoundException noResourceFoundException) {
            logger.debug("NoResourceFoundException caught in generic handler: {}",
                    noResourceFoundException.getResourcePath());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("errorMessage", "The requested resource was not found.");
            model.addAttribute("errorCode", "404");
            model.addAttribute("errorTitle", "Not Found");
        } else {
            logger.error("Unexpected error occurred", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            model.addAttribute("errorMessage", 
                    "An unexpected error occurred. Please try again later.");
            model.addAttribute("errorCode", "500");
            model.addAttribute("errorTitle", "Internal Server Error");
        }
        return "error";
    }
}
```

#### Pattern Matching for instanceof

**Modern Approach (Java 16+):**

The application uses Java's pattern matching feature for cleaner and more type-safe instanceof checks:

```java
// ✅ Modern pattern matching (Used in this application)
if (ex instanceof NoResourceFoundException noResourceFoundException) {
    // Variable 'noResourceFoundException' is automatically cast and available here
    String path = noResourceFoundException.getResourcePath();
}

// ❌ Old-style with explicit cast (Avoid)
if (ex instanceof NoResourceFoundException) {
    String path = ((NoResourceFoundException) ex).getResourcePath();
}
```

**Benefits:**

- **Cleaner Code**: Eliminates explicit casting
- **Type Safety**: Compiler ensures correct usage of cast variable
- **Null Safety**: Pattern matching prevents ClassCastException
- **Readability**: Intent is clearer with named pattern variable
- **SonarQube Compliant**: Modern pattern recommended by code quality tools

#### Exception Handler Priority

Exception handlers are executed in priority order using `@Order` annotation:

```java
@Order(Ordered.HIGHEST_PRECEDENCE)  // Priority 1 - Executes first
@ExceptionHandler(NoResourceFoundException.class)
public String handleNoResourceFound(...) { }

// Default priority (Ordered.LOWEST_PRECEDENCE)
@ExceptionHandler(Exception.class)  // Priority 2 - Fallback handler
public String handleGenericException(...) { }
```

**Why Priority Matters:**

- Spring's exception resolution checks handlers in priority order
- More specific handlers (like `NoResourceFoundException`) should have higher priority
- Generic handlers (like `Exception`) serve as fallbacks with lower priority
- Without `@Order`, execution order is unpredictable

#### HTTP Status Code Setting

**Important:** When returning `ModelAndView` or view names, use `HttpServletResponse.setStatus()` instead of `@ResponseStatus`:

```java
// ✅ Correct - Sets status programmatically
@ExceptionHandler(NoResourceFoundException.class)
public String handleNoResourceFound(HttpServletResponse response, ...) {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);  // HTTP 404
    return "error";
}

// ❌ Incorrect - @ResponseStatus doesn't always work with ModelAndView
@ResponseStatus(HttpStatus.NOT_FOUND)
@ExceptionHandler(NoResourceFoundException.class)
public String handleNoResourceFound(...) {
    return "error";  // May still return 500 instead of 404
}
```

**Reason:** `@ResponseStatus` is designed for `@ResponseBody` methods (REST APIs). For view-based controllers returning templates, use `HttpServletResponse.setStatus()` for reliable HTTP status codes.

#### Common Exception Types

| Exception                         | HTTP Status | Use Case                                            |
| --------------------------------- | ----------- | --------------------------------------------------- |
| `NoResourceFoundException`        | 404         | Missing static resources (CSS, JS, images, favicon) |
| `TaskActivityNotFoundException`   | 404         | Task entity not found                               |
| `AccessDeniedException`           | 403         | User lacks required permissions                     |
| `UsernameNotFoundException`       | 401         | Authentication failure                              |
| `MethodArgumentNotValidException` | 400         | Form validation errors                              |
| `Exception` (fallback)            | 500         | Unexpected errors                                   |

#### Error Model Attributes

All exception handlers set consistent model attributes for the error view:

```java
model.addAttribute("errorMessage", "User-friendly error message");
model.addAttribute("errorCode", "404");
model.addAttribute("errorTitle", "Not Found");
```

**Error Template** (`error.html`):

```html
<h1 th:text="${errorTitle}">Error</h1>
<p th:text="${errorCode}">Error Code</p>
<p th:text="${errorMessage}">An error occurred</p>
```

#### Custom Exception Example

**Creating a custom exception:**

```java
// 1. Define custom exception
public class TaskActivityNotFoundException extends RuntimeException {
    public TaskActivityNotFoundException(Long id) {
        super("Task activity not found with ID: " + id);
    }
}

// 2. Add handler to GlobalExceptionHandler
@ExceptionHandler(TaskActivityNotFoundException.class)
public String handleTaskNotFound(TaskActivityNotFoundException ex, Model model,
        HttpServletResponse response) {
    logger.warn("Task not found: {}", ex.getMessage());
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    model.addAttribute("errorMessage", ex.getMessage());
    model.addAttribute("errorCode", "404");
    model.addAttribute("errorTitle", "Task Not Found");
    return "error";
}

// 3. Throw in service layer
public TaskActivity getTaskById(Long id) {
    return taskActivityRepository.findById(id)
            .orElseThrow(() -> new TaskActivityNotFoundException(id));
}
```

#### Logging Best Practices

```java
// ✅ Good - Structured logging with context
logger.debug("Static resource not found: {}", ex.getResourcePath());
logger.error("Failed to process task {}: {}", taskId, ex.getMessage(), ex);

// ❌ Bad - String concatenation
logger.error("Failed to process task " + taskId + ": " + ex.getMessage());

// ✅ Good - Appropriate log levels
logger.debug(...)  // Expected conditions (404 for favicon, etc.)
logger.warn(...)   // Unexpected but recoverable (validation failures)
logger.error(...)  // Serious errors requiring attention (500 errors)

// ❌ Bad - Wrong log levels
logger.error(...)  // For expected 404s (creates noise)
logger.debug(...)  // For critical failures (hides important errors)
```

#### Static Resource Exception Handling

**Special Case:** Missing static resources (favicon, CSS, JS) should return 404 but not pollute error logs:

```java
@Order(Ordered.HIGHEST_PRECEDENCE)
@ExceptionHandler(NoResourceFoundException.class)
public String handleNoResourceFound(NoResourceFoundException ex, ...) {
    // DEBUG level - not ERROR - because missing favicon is normal
    logger.debug("Static resource not found: {}", ex.getResourcePath());
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    // ...
}
```

**Why DEBUG level?**

- Browsers automatically request `/favicon.ico` even if not declared
- Missing CSS/JS may be intentional during development
- 404 for static resources is expected, not an application error
- Reduces log noise in production

#### Testing Exception Handlers

**Unit Test Example:**

```java
@SpringBootTest
class GlobalExceptionHandlerTest {

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @Test
    void shouldHandleNoResourceFoundExceptionWithStatus404() {
        NoResourceFoundException ex = new NoResourceFoundException(...);
        Model model = new BindingAwareModelMap();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String viewName = exceptionHandler.handleNoResourceFound(ex, model, response);

        assertThat(viewName).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(model.getAttribute("errorCode")).isEqualTo("404");
        assertThat(model.getAttribute("errorTitle")).isEqualTo("Not Found");
    }
}
```

#### Migration Notes

**When upgrading to newer Java versions:**

- Java 16+ enables pattern matching for instanceof
- Update all `instanceof` checks to use pattern matching
- Remove explicit casts: `((Type) obj).method()` → `patternVar.method()`
- SonarQube will flag old-style instanceof as code smell

**Example migration:**

```java
// Before (Java 11-15)
if (ex instanceof NoResourceFoundException) {
    NoResourceFoundException nrfe = (NoResourceFoundException) ex;
    logger.debug("Resource not found: {}", nrfe.getResourcePath());
}

// After (Java 16+)
if (ex instanceof NoResourceFoundException noResourceFoundException) {
    logger.debug("Resource not found: {}", noResourceFoundException.getResourcePath());
}
```

4. **Database Access**
   
   - Use repository query methods
   - Avoid N+1 query problems
   - Index frequently queried columns

### Git Workflow

1. **Branch Strategy**
   
   - `main`: Production-ready code
   - `develop`: Integration branch
   - `feature/*`: New features
   - `bugfix/*`: Bug fixes

2. **Commit Messages**
   
   ```
   Type: Brief description
   
   Detailed explanation of changes
   
   Fixes #issue-number
   ```

3. **Code Review**
   
   - All changes require review
   - Run tests before PR
   - Update documentation

## Testing

### Unit Testing

**Location**: `src/test/java/com/ammons/taskactivity/`

**Example Service Test:**

```java
@SpringBootTest
class TaskActivityServiceTest {

    @Autowired
    private TaskActivityService service;

    @MockBean
    private TaskActivityRepository repository;

    @Test
    void testCreateTaskActivity() {
        TaskActivityDto dto = new TaskActivityDto();
        dto.setTaskDate(LocalDate.now());
        dto.setClient("Test Client");
        dto.setUsername("testuser");
        // ... set other fields

        TaskActivity saved = service.save(dto);
        assertNotNull(saved.getId());
        assertEquals("testuser", saved.getUsername());
    }

    @Test
    void testUsernameIsImmutableOnUpdate() {
        TaskActivity existing = repository.findById(1L).orElseThrow();
        String originalUsername = existing.getUsername();

        TaskActivityDto updateDto = new TaskActivityDto();
        updateDto.setUsername("differentuser");
        // ... set other fields

        service.updateEntityFromDto(existing, updateDto);

        // Username should remain unchanged
        assertEquals(originalUsername, existing.getUsername());
    }
}
```

**Example User Service Test:**

```java
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testCreateUserWithNames() {
        User user = userService.createUser(
            "jdoe",
            "John",
            "Doe",
            "SecurePass123!",
            Role.USER,
            false
        );

        assertNotNull(user.getId());
        assertEquals("John", user.getFirstname());
        assertEquals("Doe", user.getLastname());
        assertEquals("jdoe", user.getUsername());
    }

    @Test
    void testLastNameIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser("jdoe", "John", null, "Pass123!", Role.USER, false);
        });
    }
}
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=TaskActivityServiceTest

# Run with coverage
./mvnw test jacoco:report

# Skip tests during build
./mvnw clean package -DskipTests
```

### Integration Testing

```bash
# Run integration tests with test database
./mvnw verify -Pintegration-test
```

## Deployment

### Production Build

```bash
# Clean and build
./mvnw clean package -DskipTests

# Output: target/taskactivity-0.0.1-SNAPSHOT.jar
```

### Docker Deployment

```bash
# Build image
docker build -t task-activity:1.0 .

# Run container
docker run -d \
  -p 8080:8080 \
  -e DB_USERNAME=prod_user \
  -e DB_PASSWORD=secure_password \
  -e SPRING_PROFILES_ACTIVE=docker \
  --name task-activity \
  task-activity:1.0
```

### Docker Compose Production

```yaml
version: "3.8"
services:
    app:
        build: .
        ports:
            - "8080:8080"
        environment:
            - SPRING_PROFILES_ACTIVE=docker
            - DB_USERNAME=${DB_USERNAME}
            - DB_PASSWORD=${DB_PASSWORD}
        depends_on:
            - db

    db:
        image: postgres:15
        environment:
            - POSTGRES_DB=AmmoP1DB
            - POSTGRES_USER=${DB_USERNAME}
            - POSTGRES_PASSWORD=${DB_PASSWORD}
        volumes:
            - postgres_data:/var/lib/postgresql/data

volumes:
    postgres_data:
```

### AWS ECS Fargate Deployment

For production deployments to AWS, use the deployment scripts in the `aws/` directory.

**Prerequisites:**

- AWS CLI configured with IAM user credentials
- Docker installed and running
- ECR repository created
- RDS PostgreSQL instance configured
- AWS Secrets Manager secrets configured

**Quick Deploy:**

```powershell
# Windows PowerShell
cd aws
.\deploy-aws.ps1 -Environment production

# Linux/Mac Bash
cd aws
chmod +x deploy-aws.sh
./deploy-aws.sh production
```

**Features:**

- **CloudWatch Logs**: Automatic log collection with 30-day retention
- **S3 Log Archival**: Export logs to S3 for long-term storage
- **Secrets Manager**: Secure credential management
- **Health Checks**: Automatic health monitoring and recovery
- **Auto-scaling**: Scale based on demand

**Monitoring:**

```powershell
# View live logs
aws logs tail /ecs/taskactivity --follow --region us-east-1

# Check service status
aws ecs describe-services --cluster taskactivity-cluster --services taskactivity-service --region us-east-1

# Verify Cloudflare tunnel (if enabled)
aws logs tail /ecs/taskactivity --filter-pattern "cloudflared" --follow --region us-east-1

# Export logs to S3
cd aws
.\export-logs-to-s3.ps1 -Days 7
```

### Environment Setup

**Production Checklist:**

- [ ] Update database credentials
- [ ] Change default admin password
- [ ] Configure logging
- [ ] Set up database backups
- [ ] Configure SSL/TLS
- [ ] Set up monitoring
- [ ] Configure firewall rules
- [ ] Enable audit logging

## Troubleshooting

### Common Development Issues

#### 1. Maven Build Failures

**Problem**: Dependencies not downloading

**Solution**:

```bash
# Clear local repository
mvn dependency:purge-local-repository

# Force update
mvn clean install -U
```

#### 2. Database Connection Issues

**Problem**: Cannot connect to PostgreSQL

**Solution**:

```bash
# Check PostgreSQL status
systemctl status postgresql

# Test connection
psql -h localhost -U postgres -d AmmoP1DB

# Check application.properties settings
```

#### 3. Port Already in Use

**Problem**: Port 8080 is occupied

**Solution**:

```bash
# Find process using port
netstat -tulpn | grep :8080

# Kill process (Linux/Mac)
kill -9 <PID>

# Or change port in application.properties
server.port=8081
```

#### 4. Hibernate DDL Issues

**Problem**: Schema mismatch errors

**Solution**:

```properties
# Temporarily set to update or create-drop
spring.jpa.hibernate.ddl-auto=update

# Then revert to validate for production
spring.jpa.hibernate.ddl-auto=validate
```

#### 5. WSL2 Network Access Issues (Windows)

**Problem**: Application accessible at `http://localhost:8080` but not at `http://<YOUR_IP>:8080`

**Cause**: WSL2 port forwarding only binds to localhost by default

**Solution**:

```powershell
# Run PowerShell as Administrator
cd C:\Users\YourUsername\GitHub\ActivityTracking

# Option 1: Run the setup script
.\scripts\setup-wsl-port-forward.ps1

# Option 2: Manual setup
# Get WSL IP
wsl bash -c "hostname -I"

# Add port forwarding (replace <WSL_IP> with actual IP)
netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=8080 connectaddress=<WSL_IP> connectport=8080

# Add firewall rule
New-NetFirewallRule -DisplayName "WSL2 App Port 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

**After WSL Restart:**

- WSL IP addresses change when WSL restarts
- Run `.\scripts\update-wsl-port-forward.ps1` as Administrator to update the IP
- See [WSL Port Forwarding Guide](../WSL_PORT_FORWARDING.md) for details

**Verify Setup:**

```powershell
# Check port forwarding rules
netsh interface portproxy show v4tov4

# Check what's listening on port 8080
netstat -ano | findstr :8080
```

### Logging and Debugging

#### Log File Configuration

The application uses **Logback** for logging with the following configuration:

**Location**: `src/main/resources/logback-spring.xml`

**Log File Output:**

- **Local/Windows**: `C:\Logs\ActivityTracking-yyyy-MM-dd_HH-mm-ss.log`
- **Docker**: `/var/log/app/ActivityTracking-yyyy-MM-dd_HH-mm-ss.log` (mapped to host `C:\Logs`)

**Key Features:**

- Time-based rolling policy with date/time in filename
- Logs created immediately on startup with timestamp
- Keeps 30 days of history
- Total size cap: 1GB
- Separate console and file output patterns
- **File logging can be disabled via environment variable** (see below)

#### Controlling File Logging

File logging can be temporarily disabled without code changes using the `ENABLE_FILE_LOGGING` environment variable.

**To Disable File Logging:**

1. **Using WSL Startup Script** (Easiest)
   Edit `start-wsl2.sh` and uncomment:
   
   ```bash
   # Change this:
   # export ENABLE_FILE_LOGGING=false
   
   # To this:
   export ENABLE_FILE_LOGGING=false
   ```

2. **Using Environment Variable**
   
   ```bash
   # Before starting the application
   export ENABLE_FILE_LOGGING=false
   ./start-wsl2.sh host-db
   ```

3. **In application.properties**
   
   ```properties
   # Add to application.properties (affects all environments)
   ENABLE_FILE_LOGGING=false
   ```

**To Re-Enable File Logging:**

- Comment out or remove the `ENABLE_FILE_LOGGING=false` setting
- Or explicitly set it to `true`: `export ENABLE_FILE_LOGGING=true`
- Restart the application

**Use Cases for Disabling File Logging:**

- Development/testing to reduce disk I/O
- Performance testing to eliminate file overhead
- Short debug sessions where console logs are sufficient
- Disk space concerns

**Note:** Console logging is always active regardless of file logging setting. You can always view logs using:

```bash
docker logs activitytracking-app-1
# or
docker compose logs -f app
```

For complete details, see [LOGGING_CONTROL.md](../LOGGING_CONTROL.md).

#### Log File Configuration Details

**Configuration:**

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- Log path is configurable via environment variable -->
    <property name="LOG_PATH" value="${LOG_PATH:-C:/Logs}"/>

    <!-- File appender with time-based rolling -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/ActivityTracking-%d{yyyy-MM-dd_HH-mm-ss}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>
</configuration>
```

**Environment Variable:**

- `LOG_PATH`: Override default log directory
  - Default (local): `C:/Logs`
  - Docker: `/var/log/app` (set in docker-compose.yml)

**Docker Configuration:**

The `docker-compose.yml` includes volume mapping for log files:

```yaml
services:
    app:
        environment:
            - LOG_PATH=/var/log/app
        volumes:
            - C:\Logs:/var/log/app  # Maps container logs to host
```

This allows you to view logs from both local and containerized applications in the same `C:\Logs` directory on your Windows host.

#### Enable Debug Logging

Configure logging levels in `application.properties` or `application-docker.properties`:

```properties
# Application logging
# Available log levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF
logging.level.com.ammons.taskactivity=DEBUG

# SQL logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Spring Security logging
logging.level.org.springframework.security=DEBUG
```

**Note:** Log levels configured in properties files control which messages are logged. The `logback-spring.xml` controls where and how they are written (console and file).

#### View SQL Queries

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

#### Log File Management

**Viewing Logs:**

```bash
# View latest log file (Windows)
Get-Content "C:\Logs\ActivityTracking-*.log" | Select-Object -Last 50

# Tail logs in real-time
Get-Content "C:\Logs\ActivityTracking-*.log" -Wait -Tail 20

# List all log files
Get-ChildItem "C:\Logs" -Filter "ActivityTracking-*.log" | Sort-Object LastWriteTime -Descending
```

**Cleanup Old Logs:**
Logback automatically manages log retention (30 days, 1GB cap), but you can manually clean up:

```bash
# Delete logs older than 30 days
Get-ChildItem "C:\Logs" -Filter "ActivityTracking-*.log" | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-30) } | Remove-Item
```

## Troubleshooting

### WSL Port Forwarding Issues

#### Problem: `http://localhost:8080/task-activity` not accessible from Windows

If you're running the application in WSL/Docker and cannot access it from Windows using `localhost:8080`, follow these troubleshooting steps:

**Symptoms:**

- Application is running in WSL (confirmed by `docker ps`)
- `curl http://localhost:8080` from Windows fails with "Unable to connect"
- Direct WSL IP works (e.g., `http://172.27.85.228:8080`)

**Root Cause:**
The Windows **IP Helper service** (`iphlpsvc`) is required for port forwarding from Windows to WSL. If this service is stopped, the port proxy rules won't function.

#### Solution Steps

**Step 1: Verify Application is Running**

```powershell
# From Windows PowerShell
wsl -u root bash -c "docker ps"

# Should show container running on port 8080
# Example: activitytracking-app-1 ... 0.0.0.0:8080->8080/tcp
```

**Step 2: Check IP Helper Service Status**

```powershell
Get-Service iphlpsvc | Select-Object Name, Status, StartType
```

If `Status` shows **Stopped**, this is your issue.

**Step 3: Start IP Helper Service (Requires Administrator)**

Open PowerShell **as Administrator** and run:

```powershell
# Start the service immediately
Start-Service iphlpsvc

# Ensure it starts automatically on boot
Set-Service iphlpsvc -StartupType Automatic

# Verify it's running
Get-Service iphlpsvc
```

Expected output:

```
Name      Status StartType
----      ------ ---------
iphlpsvc Running Automatic
```

**Step 4: Verify Port Forwarding Configuration**

```powershell
# Check port proxy rules
netsh interface portproxy show all

# Should show:
# Address         Port        Address         Port
# 0.0.0.0         8080        <WSL-IP>        8080
```

If no rules exist, run the setup script as Administrator:

```powershell
.\scripts\setup-wsl-port-forward.ps1
```

This script will:

- Get the current WSL IP address
- Add Windows Firewall rule for port 8080
- Configure port forwarding from Windows to WSL
- Verify the configuration

**Step 5: Test Connection**

```powershell
# Test TCP connectivity
Test-NetConnection -ComputerName localhost -Port 8080

# Should show: TcpTestSucceeded : True
```

```powershell
# Test HTTP endpoint
curl http://localhost:8080/task-activity

# Should receive HTML response or 302 redirect
```

#### Manual Port Forwarding Setup

If the script doesn't work, manually configure port forwarding (requires Administrator):

```powershell
# Get WSL IP address
$wslIp = (wsl hostname -I).Split(" ")[0].Trim()
Write-Host "WSL IP: $wslIp"

# Add firewall rule
New-NetFirewallRule -DisplayName "WSL2 App Port 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow

# Remove old port proxy rule (if exists)
netsh interface portproxy delete v4tov4 listenaddress=0.0.0.0 listenport=8080

# Add new port proxy rule
netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=8080 connectaddress=$wslIp connectport=8080

# Verify
netsh interface portproxy show all
```

#### Alternative: Update Port Forwarding After WSL Restart

If WSL is restarted and gets a new IP address, update the port forwarding rule:

```powershell
# Run as Administrator
.\scripts\update-wsl-port-forward.ps1
```

#### Workaround: Use WSL IP Directly

If you cannot use Administrator privileges or need immediate access:

```powershell
# Get WSL IP
wsl hostname -I

# Access application using WSL IP (example)
# http://172.27.85.228:8080/task-activity
```

**Note:** The WSL IP address can change after WSL restarts, so this is only a temporary workaround.

#### Common Issues and Solutions

| Issue                    | Cause                     | Solution                                                    |
| ------------------------ | ------------------------- | ----------------------------------------------------------- |
| Connection timeout       | IP Helper service stopped | Start `iphlpsvc` service                                    |
| Connection refused       | Firewall blocking port    | Add firewall rule for port 8080                             |
| Wrong IP in port proxy   | WSL IP changed            | Run `update-wsl-port-forward.ps1`                           |
| Port proxy not working   | Need admin rights         | Open PowerShell as Administrator                            |
| Application not starting | Database connection issue | Check `DB_USERNAME` and `DB_PASSWORD` environment variables |

#### Verification Checklist

Use this checklist to verify your setup:

- [ ] Application container is running (`docker ps` shows status)
- [ ] IP Helper service is running (`Get-Service iphlpsvc`)
- [ ] Port proxy rule exists (`netsh interface portproxy show all`)
- [ ] Firewall rule allows port 8080 (`Get-NetFirewallRule -DisplayName "WSL2 App Port 8080"`)
- [ ] TCP connection succeeds (`Test-NetConnection -ComputerName localhost -Port 8080`)
- [ ] HTTP request returns data (`curl http://localhost:8080/task-activity`)

### Database Connection Issues

#### Application fails to start with database errors

**Check environment variables:**

```bash
# For WSL/Docker deployments
wsl -u root bash -c "echo DB_USERNAME: \$DB_USERNAME"
wsl -u root bash -c "echo DB_PASSWORD: \$DB_PASSWORD"
```

**Verify database is accessible:**

```bash
# From WSL
wsl -u root bash -c "psql -h <WINDOWS_IP> -U postgres -d AmmoP1DB -c 'SELECT version();'"
```

**Check Docker logs:**

```bash
wsl -u root bash -c "docker compose logs app | tail -50"
```

### Port Already in Use

If port 8080 is already in use:

```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Note the PID and stop the process
# Or change the application port in application.properties
```

## 

### 
