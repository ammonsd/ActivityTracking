# Task Activity Tracking (Spring Boot, PostgreSQL)

Track task hours by client, project, and phase. Java 17 + Spring Boot 3 (MVC + Thymeleaf), Spring Data JPA, and Flyway. Database: PostgreSQL. Integration tests: Testcontainers (PostgreSQL).

---

## Table of Contents

- [Features](#features)
- [Screens & Flow](#screens--flow)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Setup](#setup)
- [Usage](#usage)
- [API](#api)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Project Structure](#project-structure)

## Features

- Record daily task activity: `taskDate`, `client`, `project`, `phase`, `hours`, `details`.
- Dropdown-driven values (Client / Project / Phase) with active/inactive control.
- Weekly timesheet view (Mon–Sun) with day and week totals.
- CRUD for dropdown values; deletion blocked if referenced.
- Flyway migrations and environment profiles (`dev`, `prod`).
- Server-side pagination and filters on the list view.

## Screens & Flow

- **Task Activity List:** filter by client/project/phase/date range; 20 per page; links to Weekly Timesheet, New Task, Client Mgmt.
- **Weekly Timesheet:** Mon–Sun grid, Monday as selectable start; shows day and week totals.
- **New Task:** all fields required; stays on page and retains values after submit.
- **Client/Project/Phase Management:** CRUD; cannot delete if referenced; toggle `is_active`.

## Architecture

- **Tech:** Java 17, Spring Boot 3 (MVC + Thymeleaf), Spring Data JPA, Flyway.
- **DB:** PostgreSQL, schema `public`.
- **Build:** Maven.
- **Tests:** JUnit 5, Testcontainers.

## Requirements

- Java 17
- Maven 3.9+
- PostgreSQL 14+ (local or container)
- (Optional) Docker Desktop for local DB

## Setup

### 1) Clone

```powershell
cd C:\Users\deana\GitHub
git clone https://github.com/ammonsd/ActivityTracking.git
cd ActivityTracking
```

## Usage

### Run the app

- java -jar target/activity-tracking.jar
- mvn spring-boot:run

### UI

- App: http://localhost:8080/
- Health: http://localhost:8080/actuator/health

### API

GET   /tasks?client=&project=&phase=&from=yyyy-MM-dd&to=yyyy-MM-dd&page=&size=
POST  /tasks
      { "taskDate":"2024-06-11", "client":"Corporate", "project":"General Administration",
        "phase":"Coding/Programming", "hours":4.5, "details":"Greenville SC" }

GET   /timesheet?weekStart=yyyy-MM-dd
GET   /dropdowns/{category}           # CLIENT|PROJECT|PHASE
POST  /dropdowns/{category}           # { value, displayOrder, isActive }
DELETE /dropdowns/{category}/{id}     # 409 if referenced

## Configuration

The application can be configured through application properties. Key configuration options include:

### Task Activity List Sorting

Control the default sort order for the task activity list through these properties:

```properties
# Task Activity List Sort Configuration
# Controls the default sort order for the task activity list
# Valid values for direction: ASC (ascending) or DESC (descending)
app.task-activity.list.sort.date-direction=DESC
app.task-activity.list.sort.client-direction=ASC
app.task-activity.list.sort.project-direction=ASC
```

- **app.task-activity.list.sort.date-direction**: Sort direction for task date column (default: DESC - newest first)
- **app.task-activity.list.sort.client-direction**: Sort direction for client column (default: ASC - alphabetical)
- **app.task-activity.list.sort.project-direction**: Sort direction for project column (default: ASC - alphabetical)

These settings can be configured per environment in the respective properties files:
- `application.properties` (default)
- `application-local.properties` (local development)
- `application-docker.properties` (Docker/production)

Example: To show oldest tasks first, set `app.task-activity.list.sort.date-direction=ASC`

## Testing

mvn test
mvn verify    # integration tests with Testcontainers 

## Docker

See [DOCKER_BUILD_GUIDE.md](DOCKER_BUILD_GUIDE.md) for detailed Docker build instructions.

**Quick Start:**
```powershell
# Fast local development build (~10 seconds)
.\mvnw.cmd clean package -DskipTests
docker-compose --profile local-fast build
docker-compose --profile local-fast up -d

# Standard production build (~120 seconds first time, ~20-30s cached)
docker-compose --profile host-db build
docker-compose --profile host-db up -d
```

**Troubleshooting:** If you encounter Docker build issues, see [DOCKER_TROUBLESHOOTING.md](DOCKER_TROUBLESHOOTING.md)

## CI/CD

- Pipeline: Build =>Test => Package => (optional) Docker image => Deploy
- Example GitHub Actions workflow: .github/workflows/ci.yml
  - Cache Maven
  - Start PostgreSQL service
  - Run unit + integration tests
  - Publish artifacts / images as needed

## Project Structure

src/
  main/
    java/.../controller
    java/.../service
    java/.../repository
    java/.../model
    resources/
      templates/             # Thymeleaf
      static/                      # CSS/JS
      db/migration/        # Flyway SQL
  test/
    java/...                       # JUnit + Testcontainers ITs
