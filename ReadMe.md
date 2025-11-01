# Task Activity Tracking

A time tracking web application built with Spring Boot and PostgreSQL for recording and managing task hours by client, project, and phase.

## Features

## Features

- ✅ Daily task recording with client/project/phase tracking
- 📊 Weekly timesheet view (Monday-Sunday format)
- 📥 Export filtered tasks and weekly timesheets to CSV format
- 🔍 Filter and search capabilities
- 🎯 Dynamic dropdown management for clients, projects, and phases
- ✔️ Data validation and error handling
- � Comprehensive API documentation (Swagger/OpenAPI)

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.5.6 (MVC + Thymeleaf), Spring Data JPA
- **Database:** PostgreSQL 15+
- **API Documentation:** Springdoc OpenAPI 2.6.0 (Swagger UI)
- **Build:** Maven
- **Testing:** JUnit 5, Testcontainers
- **Deployment:** Docker, AWS ECS (optional)

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 14+ (or Docker)

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

### API Documentation

Interactive API documentation is available via Swagger UI:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## Documentation

- 📚 [Quick Start Guide](docs/Quick_Start.md) - Fast setup for daily development workflow
- 👨‍💻 [Developer Guide](docs/Developer_Guide.md) - Complete technical reference
- 📖 [User Guide](docs/User_Guide.md) - End-user documentation
- 🔐 [Administrator User Guide](docs/Administrator_User_Guide.md) - Admin features and user management
- 📦 [Docker Build Guide](docs/Docker_Build_Guide.md) - Containerization and Docker deployment
- ☁️ [AWS Deployment Guide](docs/AWS_Deployment.md) - AWS ECS Fargate deployment
- 🏗️ [CloudFormation Guide](cloudformation/README.md) - Infrastructure as Code automation (deployment-ready)
- 🌐 [WSL Port Forwarding Guide](docs/WSL_PORT_FORWARDING.md) - Network configuration for WSL2
- 🛠️ [Helper Scripts Guide](docs/HELPER_SCRIPTS_README.md) - AWS log export scripts and automation
- ⚡ [Lambda Setup Guide](aws/LAMBDA_CONSOLE_SETUP_GUIDE.md) - Automated CloudWatch log exports with Lambda
- 🚀 [Jenkins CI/CD Guide](jenkins/README.md) - Continuous integration and deployment pipeline (deployment-ready)

## License

This project is for personal/educational use.
