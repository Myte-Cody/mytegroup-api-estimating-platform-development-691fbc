# Spring Boot Migration Setup

## Overview

Create a new Spring Boot application structure with all required dependencies and infrastructure for migrating from NestJS to Spring Boot with PostgreSQL. The Spring Boot project will be located in a `spring-boot-api/` subdirectory within the existing repository, allowing both the original NestJS codebase (`src/`) and the new Spring Boot codebase to coexist. This structure enables side-by-side reference during migration while keeping both implementations available for context.

## Repository Structure

The repository will contain both the existing NestJS codebase and the new Spring Boot project:

```
mytegroup-api-estimating-platform-development-691fbc/
├── src/                          # Existing NestJS codebase (for reference)
├── dist/                         # Existing NestJS build output
├── package.json                  # Existing NestJS dependencies
├── tsconfig.json                  # Existing TypeScript config
├── ...                           # Other existing NestJS files
│
└── spring-boot-api/              # New Spring Boot project (migration target)
    ├── src/
```

## Spring Boot Project Structure

The Spring Boot project will be organized as follows:

```
spring-boot-api/
├── src/
│   ├── main/
│   │   ├── java/com/mytegroup/api/
│   │   │   ├── Application.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtConfig.java
│   │   │   │   ├── DatabaseConfig.java
│   │   │   │   ├── EmailConfig.java
│   │   │   │   └── ThymeleafConfig.java
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   └── entity/
│   │   │       └── BaseEntity.java (with Envers auditing)
│   │   ├── resources/
│   │   │   ├── application.yml
│   │   │   ├── application-dev.yml
│   │   │   ├── application-test.yml
│   │   │   └── templates/ (Thymeleaf templates)
│   │   └── db/migration/ (Flyway migrations)
│   │       └── V1__Initial_schema.sql
│   └── test/
│       └── java/com/mytegroup/api/
│           └── ApplicationTests.java
├── build.gradle
├── settings.gradle
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── Dockerfile
├── docker-compose.yml
└── .dockerignore
```

## Implementation Details

### 1. Gradle Configuration (`build.gradle` and `settings.gradle`)

- Spring Boot 4.0.x (latest) via Spring Boot Gradle plugin (based on Spring Framework 7.0)
- Java 25 (LTS)
- PostgreSQL driver
- Spring Data JPA
- Hibernate Envers (auditing)
- Lombok
- Flyway
- Spring Security
- JWT (io.jsonwebtoken:jjwt)
- Thymeleaf
- Spring Mail
- Spring Boot Actuator (for health checks and monitoring)
- Testcontainers (PostgreSQL)
- Spring Boot Test
- Gradle Wrapper for consistent builds

### 2. Application Configuration

- **`application.yml`**: Base configuration with profiles, Actuator endpoints configuration
- **`application-dev.yml`**: Development profile with MailHog settings
- **`application-test.yml`**: Test profile with Testcontainers
- Actuator health endpoints enabled at `/actuator/health`
- Actuator endpoints exposed for health checks (matching NestJS health endpoint)

### 3. Security Configuration

- JWT-based authentication
- Spring Security with stateless sessions
- Password encoding (BCrypt)
- CORS configuration
- Security filter chain
- Actuator endpoints configured for public health checks (security can be customized per environment)

### 4. Database Configuration

- PostgreSQL connection pooling (HikariCP)
- Hibernate Envers for audit trails
- Flyway for database migrations
- Multi-tenancy support structure (prepared for future implementation)

### 5. Docker Setup

- **`Dockerfile`**: Multi-stage build for Spring Boot application using Gradle
- **`docker-compose.yml`**: Complete stack with:
  - Spring Boot application
  - PostgreSQL 18 (latest LTS)
  - MailHog (for email testing)
  - Redis (for session/caching, matching NestJS setup)

### 6. Testing Infrastructure

- Testcontainers configuration for PostgreSQL
- Integration test example
- Test profile with MailHog configuration

### 7. Example Components

- Base entity with Envers auditing annotations
- Spring Actuator health endpoints (matching NestJS health endpoint)
- JWT token provider and filter
- Basic security configuration
- Actuator endpoints configured for health checks and monitoring

## Key Files to Create

1. **`spring-boot-api/build.gradle`** - Gradle dependencies and build configuration
2. **`spring-boot-api/settings.gradle`** - Gradle project settings
3. **`spring-boot-api/gradle/wrapper/gradle-wrapper.properties`** - Gradle wrapper configuration
4. **`spring-boot-api/src/main/java/com/mytegroup/api/Application.java`** - Main Spring Boot application class
5. **`spring-boot-api/src/main/resources/application.yml`** - Base application configuration
6. **`spring-boot-api/src/main/resources/application-dev.yml`** - Development profile
7. **`spring-boot-api/src/main/resources/application-test.yml`** - Test profile
8. **`spring-boot-api/src/main/java/com/mytegroup/api/config/SecurityConfig.java`** - Spring Security configuration (with Actuator endpoints security)
9. **`spring-boot-api/src/main/java/com/mytegroup/api/config/JwtConfig.java`** - JWT configuration
10. **`spring-boot-api/src/main/java/com/mytegroup/api/security/JwtTokenProvider.java`** - JWT token generation/validation
11. **`spring-boot-api/src/main/java/com/mytegroup/api/security/JwtAuthenticationFilter.java`** - JWT authentication filter
12. **`spring-boot-api/src/main/java/com/mytegroup/api/entity/BaseEntity.java`** - Base entity with Envers auditing
13. **`spring-boot-api/src/main/resources/db/migration/V1__Initial_schema.sql`** - Initial Flyway migration
14. **`spring-boot-api/Dockerfile`** - Docker build configuration (using Gradle)
15. **`spring-boot-api/docker-compose.yml`** - Docker Compose for local development
16. **`spring-boot-api/.dockerignore`** - Docker ignore file
17. **`spring-boot-api/src/test/java/com/mytegroup/api/ApplicationTests.java`** - Basic test with Testcontainers

## Dependencies Summary

- **Spring Boot**: 4.0.x (latest)
- **Spring Framework**: 7.0.x (underlying framework)
- **Java**: 25 (LTS)
- **PostgreSQL**: 18 (via Docker)
- **Hibernate Envers**: Latest (via Spring Data JPA, compatible with Spring Boot 4.0)
- **Lombok**: Latest
- **Flyway**: Latest
- **Spring Security**: 7.x (included with Spring Boot 4.0)
- **JWT**: jjwt 0.12.x
- **Thymeleaf**: Latest
- **Spring Mail**: Latest
- **Spring Boot Actuator**: Latest
- **Testcontainers**: Latest

## Docker Compose Services

1. **app**: Spring Boot application (port 8080)
2. **postgres**: PostgreSQL 18 (port 5432)
3. **mailhog**: MailHog for email testing (ports 1025, 8025)
4. **redis**: Redis for caching/sessions (port 6379)

## Next Steps After Setup

After this initial setup, you can:

1. Reference the existing NestJS codebase in `src/` for business logic and API structure
2. Migrate entity models from Mongoose schemas (in `src/features/*/schemas/`) to JPA entities in `spring-boot-api/src/main/java/com/mytegroup/api/entity/`
3. Migrate controllers and services from NestJS (in `src/features/`) to Spring Boot controllers and services
4. Implement multi-tenancy using Spring's AbstractRoutingDataSource (reference `src/common/tenancy/` for existing multi-tenancy logic)
5. Migrate authentication/authorization logic (reference `src/features/auth/` and `src/common/guards/`)
6. Set up queue processing (replace BullMQ with Spring's @Async or RabbitMQ, reference `src/queues/` for existing queue logic)
7. Migrate email templates and services (reference `src/features/email/` and `src/features/email-templates/`)

## Notes

- **Repository Structure**: The Spring Boot project (`spring-boot-api/`) exists alongside the existing NestJS codebase (`src/`) in the same repository, allowing side-by-side reference during migration
- **Build Tools**: Both projects coexist independently - NestJS uses npm/package.json, Spring Boot uses Gradle/build.gradle
- **Port Configuration**: Spring Boot will run on port 8080 (configurable), while NestJS runs on port 7070 (from existing config), allowing both to run simultaneously for comparison
- The project uses Gradle as the build tool with Java 25 (LTS) and Spring Boot 4.0.x (requires Java 17+, fully supports Java 25)
- Spring Boot 4.0 is based on Spring Framework 7.0 and uses Jakarta EE 11 (javax.* packages replaced with jakarta.*)
- All imports will use jakarta.* namespace (e.g., jakarta.persistence.*, jakarta.servlet.*) instead of javax.*
- Gradle Wrapper is included for consistent builds across environments
- All configurations are environment-aware via Spring profiles
- Testcontainers will automatically start PostgreSQL for tests
- MailHog is configured for development and test profiles
- Hibernate Envers is enabled globally for audit trail support
- Flyway migrations run automatically on application startup
- Dockerfile uses Gradle build commands instead of Maven
- Spring Actuator provides health check endpoints at `/actuator/health` (replaces custom HealthController)
- Actuator endpoints are configured to be accessible for monitoring and health checks

