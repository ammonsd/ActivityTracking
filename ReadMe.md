# Task Activity Tracking

A time tracking web application built with Spring Boot and PostgreSQL for recording and managing task hours by client, project, and phase.

## Features

- ✅ Daily task recording with client/project/phase tracking
- 📊 Weekly timesheet view (Monday-Sunday format)
- 📥 Export filtered tasks and weekly timesheets to CSV format
- 🔍 Filter and search capabilities
- 🎯 Dynamic dropdown management for clients, projects, and phases
- ✔️ Data validation and error handling
- 📚 Comprehensive API documentation (Swagger/OpenAPI)

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.5.6 (MVC + Thymeleaf), Spring Data JPA
- **Database:** PostgreSQL 15+
- **API Documentation:** Springdoc OpenAPI 2.6.0 (Swagger UI)
- **Build:** Maven
- **Testing:** JUnit 5, Testcontainers
- **Deployment:** Docker, AWS ECS (optional)

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ (or Docker)

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

### Core Guides

- 👨‍💻 [Developer Guide](docs/Developer_Guide.md) - Complete technical reference and development workflow
- 📖 [User Guide](docs/User_Guide.md) - End-user documentation for daily task tracking
- 🔐 [Administrator User Guide](docs/Administrator_User_Guide.md) - Admin features and user management

### Docker & Containerization

- 📦 [Docker Build Guide](docs/Docker_Build_Guide.md) - Complete Docker containerization guide
- 🚀 [Docker Quick Start](docs/Docker_Quick_Start.md) - Fast Docker setup for local development

### AWS Deployment

- ☁️ [AWS Deployment Guide](aws/AWS_Deployment.md) - AWS ECS Fargate deployment
- 🏗️ [CloudFormation Guide](cloudformation/README.md) - Infrastructure as Code automation (deployment-ready)
- 📋 [AWS Console Guide](aws/AWS_Console_Guide.md) - Manual AWS setup via console

### Kubernetes

- ⚓ [Kubernetes Deployments](k8s/) - K8s manifests with RBAC and secrets management (deployment-ready)

### CI/CD & Automation

- 🚀 [Jenkins CI/CD Guide](jenkins/README.md) - Continuous integration and deployment pipeline (deployment-ready)

### Architecture & Design

- 🏛️ [Blueprint Part 1: Overview and Architecture](docs/Blueprint_Part_1_Overview_and_Architecture.md) - System architecture
- 💾 [Blueprint Part 2: Database and Configuration](docs/Blueprint_Part_2_Database_and_Configuration.md) - Data model
- � [Blueprint Part 3: Entities and Repositories](docs/Blueprint_Part_3_Entities_and_Repositories.md) - JPA layer
- 📝 [Blueprint Part 4-8: Complete Implementation](docs/Blueprint_Part_4-8_Complete_Implementation.md) - Services and controllers
- 📊 [Technical Features Summary](docs/Technical_Features_Summary.md) - Comprehensive feature list

## License

This project is for personal/educational use.
