# BeamerParts Code Style and Conventions

## Package Structure
- **Domain-based packages**: `live.alinmiron.beamerparts.{domain}`
- **No redundant suffixes**: Use `user` not `user.service`
- **Maven modules**: kebab-case naming (user-service, product-service)
- **Group ID**: `live.alinmiron.beamerparts` (follows domain ownership)

## Java Code Style
- **Java 21** language features encouraged
- **Spring Boot annotations**: Standard Spring patterns
- **Package naming**: Reverse domain based on alinmiron.live ownership
- **Class naming**: PascalCase (UserService, ProductController)
- **Method naming**: camelCase
- **Constants**: UPPER_SNAKE_CASE

## Service Layer Structure
```
src/main/java/live/alinmiron/beamerparts/{domain}/
├── {Domain}Application.java         # Main Spring Boot class
├── controller/                      # REST controllers
├── service/                         # Business logic
├── repository/                      # Data access layer
├── entity/                          # JPA entities
├── dto/
│   ├── request/                     # Request DTOs
│   └── response/                    # Response DTOs
├── config/                          # Configuration classes
├── exception/                       # Custom exceptions
└── util/                            # Utility classes
```

## API Design Patterns
- **Gateway routing**: `/api/v1/{domain}/{resource}` for versioned APIs
- **Internal routing**: `/internal/{resource}` for service-to-service
- **User context injection**: JWT-based with automatic user ID injection
- **RESTful conventions**: Standard HTTP methods and status codes

## Database Conventions
- **Flyway migrations**: `V{number}__{Description}.sql`
- **Separate databases**: One per microservice
- **Snake_case**: Database table and column naming
- **Entity mapping**: JPA annotations with proper relationships

## Configuration Management
- **application.yml**: Service-specific configuration
- **Profiles**: Support for dev, docker, production environments
- **Environment variables**: For sensitive configuration
- **Port conventions**: 8080 (gateway), 8081-8084 (services)