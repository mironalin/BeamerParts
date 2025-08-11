# BeamerParts Tech Stack

## Core Technologies
- **Java 21** - Latest LTS version
- **Spring Boot 3.3.2** - Microservices framework
- **Spring Cloud Gateway 2025.0.0** - API Gateway and routing
- **Maven** - Build and dependency management
- **PostgreSQL** - Primary database (separate DBs per service)
- **Redis** - Caching and session storage
- **RabbitMQ** - Message queuing and event-driven communication
- **Docker & Docker Compose** - Containerization and local development

## Key Dependencies
- **JWT (jjwt 0.11.5)** - Authentication tokens
- **Spring Security** - Security framework
- **Spring Data JPA** - Data persistence
- **Flyway** - Database migrations
- **Spring Boot Actuator** - Health checks and monitoring
- **Hibernate** - ORM framework
- **Stripe (24.2.0)** - Payment processing
- **iText (5.5.13.4)** - PDF generation for invoices

## Infrastructure
- **Development**: Docker Compose with PostgreSQL (ports 5433-5435), Redis (6379), RabbitMQ (5672)
- **Services**: Each microservice runs on dedicated ports (8080-8084)
- **Package Structure**: Domain-based `live.alinmiron.beamerparts.*`
- **Module Structure**: Maven multi-module with kebab-case naming (user-service, product-service, etc.)