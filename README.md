# Task Activity Tracking

A time tracking web application built with Spring Boot and PostgreSQL for recording and managing task hours by client, project, and phase.

## Features

- 📝 Record daily task activities with client, project, phase, hours, and details
- 📊 Weekly timesheet view (Monday-Sunday) with daily and weekly totals
- 🔍 Filter and search tasks by client, project, phase, and date range
- ⚙️ Manage dropdown values for clients, projects, and phases
- 🔒 Data validation prevents deletion of referenced dropdown values
- 📖 Interactive API documentation with Swagger/OpenAPI

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

- 📚 [Quick Start Guide](docs/QUICK_START.md)
- 👨‍💻 [Developer Guide](docs/DEVELOPER_GUIDE.md)
- 📖 [User Guide](docs/USER_GUIDE.md)
- 🔐 [Administrator User Guide](docs/ADMINISTRATOR_USER_GUIDE.md)
- 📦 [Docker Build Guide](docs/DOCKER_BUILD_GUIDE.md)
- 🔧 [Swagger API Documentation](localdocs/SWAGGER_API_DOCUMENTATION.md)

## License

This project is for personal/educational use.
