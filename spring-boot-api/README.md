# Spring Boot API

Spring Boot migration of the MyteGroup API from NestJS to Spring Boot with PostgreSQL.

## Prerequisites

- Java 25 (LTS)
- Gradle 8.10.2+ (or use Gradle Wrapper)
- Docker and Docker Compose (for local development)

## Quick Start

### Using Docker Compose (Recommended)

```bash
cd spring-boot-api
docker-compose up -d
```

This will start:
- Spring Boot application on port 8080
- PostgreSQL 18 on port 5432
- MailHog on ports 1025 (SMTP) and 8025 (Web UI)
- Redis on port 6379

### Local Development

1. Start PostgreSQL, MailHog, and Redis using Docker Compose:
```bash
docker-compose up -d postgres mailhog redis
```

2. Build and run the application:
```bash
./gradlew bootRun
```

Or using the Gradle wrapper:
```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## Configuration

Configuration is managed via Spring profiles:

- **dev** (default): Development profile with MailHog email settings
- **test**: Test profile with Testcontainers support

Environment variables can override defaults:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `SMTP_HOST`, `SMTP_PORT`
- `PORT` (default: 8080)
- `SPRING_PROFILES_ACTIVE`

## Health Checks

Spring Actuator health endpoint:
```bash
curl http://localhost:8080/actuator/health
```

## Testing

Run tests with Testcontainers:
```bash
./gradlew test
```

Tests automatically start PostgreSQL containers using Testcontainers.

## Building

Build the application JAR:
```bash
./gradlew bootJar
```

The JAR will be created in `build/libs/spring-boot-api.jar`

## Docker Build

Build the Docker image:
```bash
docker build -t spring-boot-api .
```

## Project Structure

```
spring-boot-api/
├── src/
│   ├── main/
│   │   ├── java/com/mytegroup/api/
│   │   │   ├── Application.java          # Main application class
│   │   │   ├── config/                   # Configuration classes
│   │   │   ├── security/                 # Security and JWT components
│   │   │   └── entity/                  # JPA entities
│   │   └── resources/
│   │       ├── application.yml           # Base configuration
│   │       ├── application-dev.yml       # Development profile
│   │       ├── application-test.yml      # Test profile
│   │       └── db/migration/            # Flyway migrations
│   └── test/                             # Test classes
├── build.gradle                          # Gradle build configuration
├── settings.gradle                       # Gradle settings
├── Dockerfile                            # Docker build file
└── docker-compose.yml                    # Docker Compose configuration
```

## Technologies

- **Spring Boot 4.0.x** - Application framework
- **Spring Framework 7.0.x** - Underlying framework
- **Java 25** - Programming language (LTS)
- **PostgreSQL 18** - Database
- **Hibernate Envers** - Audit trail support
- **Flyway** - Database migrations
- **Spring Security 7.x** - Security framework
- **JWT** - JSON Web Token authentication
- **Thymeleaf** - Template engine
- **Spring Mail** - Email support
- **Spring Boot Actuator** - Health checks and monitoring
- **Testcontainers** - Integration testing
- **Lombok** - Code generation

## Migration Notes

This project is being migrated from the NestJS codebase located in `../src/`. Reference the existing NestJS implementation for:
- Business logic patterns
- API structure
- Entity models (Mongoose schemas → JPA entities)
- Authentication/authorization flows
- Multi-tenancy implementation

## Next Steps

1. Migrate entity models from Mongoose schemas to JPA entities
2. Migrate controllers and services from NestJS
3. Implement multi-tenancy using Spring's AbstractRoutingDataSource
4. Migrate authentication/authorization logic
5. Set up queue processing (replace BullMQ)

