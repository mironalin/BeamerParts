# BeamerParts - Microservices Project Structure Guide

## Overview

In Spring Boot microservices architecture, **each microservice is a completely separate Spring Boot application** with its own:
- Main application class (`@SpringBootApplication`)
- Configuration files (`application.yml`)
- Database connection and schema
- Port number and deployment artifact (JAR file)
- Independent build and deployment process

## Project Structure Options

### Option 1: Mono-Repo Approach (Recommended for Learning)

**Single repository with multiple Spring Boot applications:**

```
beamerparts-platform/
├── README.md
├── docker-compose.yml
├── .gitignore
├── pom.xml (parent POM)
│
├── api-gateway/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/beamerparts/gateway/
│   │       │       ├── GatewayApplication.java
│   │       │       ├── config/
│   │       │       │   ├── SecurityConfig.java
│   │       │       │   ├── GatewayConfig.java
│   │       │       │   └── CorsConfig.java
│   │       │       └── filter/
│   │       │           ├── AuthenticationFilter.java
│   │       │           └── LoggingFilter.java
│   │       └── resources/
│   │           ├── application.yml
│   │           └── application-docker.yml
│   └── target/ (generated)
│
├── user-service/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/beamerparts/user/
│   │   │   │       ├── UserServiceApplication.java
│   │   │   │       ├── controller/
│   │   │   │       │   ├── AuthController.java
│   │   │   │       │   ├── UserController.java
│   │   │   │       │   └── CartController.java
│   │   │   │       ├── service/
│   │   │   │       │   ├── UserService.java
│   │   │   │       │   ├── AuthService.java
│   │   │   │       │   ├── CartService.java
│   │   │   │       │   └── UserVehicleService.java
│   │   │   │       ├── repository/
│   │   │   │       │   ├── UserRepository.java
│   │   │   │       │   ├── UserRefreshTokenRepository.java
│   │   │   │       │   ├── CartItemRepository.java
│   │   │   │       │   └── UserVehicleRepository.java
│   │   │   │       ├── entity/
│   │   │   │       │   ├── User.java
│   │   │   │       │   ├── UserRefreshToken.java
│   │   │   │       │   ├── CartItem.java
│   │   │   │       │   └── UserVehicle.java
│   │   │   │       ├── dto/
│   │   │   │       │   ├── request/
│   │   │   │       │   │   ├── UserRegistrationRequest.java
│   │   │   │       │   │   ├── LoginRequest.java
│   │   │   │       │   │   └── AddToCartRequest.java
│   │   │   │       │   └── response/
│   │   │   │       │       ├── UserDTO.java
│   │   │   │       │       ├── AuthResponse.java
│   │   │   │       │       └── CartDTO.java
│   │   │   │       ├── config/
│   │   │   │       │   ├── DatabaseConfig.java
│   │   │   │       │   ├── SecurityConfig.java
│   │   │   │       │   ├── JwtConfig.java
│   │   │   │       │   └── RabbitConfig.java
│   │   │   │       ├── client/
│   │   │   │       │   ├── ProductServiceClient.java
│   │   │   │       │   └── VehicleServiceClient.java
│   │   │   │       ├── event/
│   │   │   │       │   ├── listener/
│   │   │   │       │   │   └── UserEventListener.java
│   │   │   │       │   └── publisher/
│   │   │   │       │       └── UserEventPublisher.java
│   │   │   │       ├── exception/
│   │   │   │       │   ├── UserNotFoundException.java
│   │   │   │       │   ├── InvalidCredentialsException.java
│   │   │   │       │   └── GlobalExceptionHandler.java
│   │   │   │       └── util/
│   │   │   │           ├── JwtTokenUtil.java
│   │   │   │           └── PasswordUtil.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-docker.yml
│   │   │       └── db/migration/
│   │   │           ├── V1__Create_users_table.sql
│   │   │           ├── V2__Create_user_refresh_tokens_table.sql
│   │   │           ├── V3__Create_user_vehicles_table.sql
│   │   │           └── V4__Create_cart_items_table.sql
│   │   └── test/
│   │       └── java/
│   │           └── com/beamerparts/user/
│   │               ├── UserServiceApplicationTests.java
│   │               ├── service/
│   │               │   ├── UserServiceTest.java
│   │               │   └── CartServiceTest.java
│   │               ├── repository/
│   │               │   └── UserRepositoryTest.java
│   │               └── controller/
│   │                   ├── AuthControllerTest.java
│   │                   └── CartControllerTest.java
│   └── target/ (generated)
│
├── vehicle-service/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/beamerparts/vehicle/
│   │   │   │       ├── VehicleServiceApplication.java
│   │   │   │       ├── controller/
│   │   │   │       │   ├── BMWSeriesController.java
│   │   │   │       │   ├── BMWGenerationController.java
│   │   │   │       │   └── CompatibilityController.java
│   │   │   │       ├── service/
│   │   │   │       │   ├── BMWHierarchyService.java
│   │   │   │       │   ├── CompatibilityService.java
│   │   │   │       │   └── VehicleSyncService.java
│   │   │   │       ├── repository/
│   │   │   │       │   ├── BMWSeriesRepository.java
│   │   │   │       │   ├── BMWGenerationRepository.java
│   │   │   │       │   └── VehicleCompatibilityRepository.java
│   │   │   │       ├── entity/
│   │   │   │       │   ├── BMWSeries.java
│   │   │   │       │   ├── BMWGeneration.java
│   │   │   │       │   └── VehicleCompatibilityRegistry.java
│   │   │   │       ├── dto/
│   │   │   │       ├── config/
│   │   │   │       ├── event/
│   │   │   │       ├── exception/
│   │   │   │       └── util/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-docker.yml
│   │   │       └── db/migration/
│   │   │           ├── V1__Create_bmw_series_table.sql
│   │   │           ├── V2__Create_bmw_generations_table.sql
│   │   │           ├── V3__Create_vehicle_compatibility_table.sql
│   │   │           └── V4__Seed_bmw_data.sql
│   │   └── test/
│   └── target/ (generated)
│
├── product-service/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/beamerparts/product/
│   │   │   │       ├── ProductServiceApplication.java
│   │   │   │       ├── controller/
│   │   │   │       │   ├── ProductController.java
│   │   │   │       │   ├── CategoryController.java
│   │   │   │       │   └── InventoryController.java
│   │   │   │       ├── service/
│   │   │   │       │   ├── ProductCatalogService.java
│   │   │   │       │   ├── InventoryService.java
│   │   │   │       │   ├── BMWCacheService.java
│   │   │   │       │   └── ProductCompatibilityService.java
│   │   │   │       ├── repository/
│   │   │   │       │   ├── ProductRepository.java
│   │   │   │       │   ├── CategoryRepository.java
│   │   │   │       │   ├── InventoryRepository.java
│   │   │   │       │   ├── BMWSeriesCacheRepository.java
│   │   │   │       │   └── BMWGenerationCacheRepository.java
│   │   │   │       ├── entity/
│   │   │   │       │   ├── Product.java
│   │   │   │       │   ├── Category.java
│   │   │   │       │   ├── ProductImage.java
│   │   │   │       │   ├── ProductVariant.java
│   │   │   │       │   ├── Inventory.java
│   │   │   │       │   ├── StockMovement.java
│   │   │   │       │   ├── BMWSeriesCache.java
│   │   │   │       │   ├── BMWGenerationCache.java
│   │   │   │       │   └── ProductCompatibility.java
│   │   │   │       ├── dto/
│   │   │   │       ├── config/
│   │   │   │       ├── client/
│   │   │   │       │   └── VehicleServiceClient.java
│   │   │   │       ├── event/
│   │   │   │       ├── exception/
│   │   │   │       └── util/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-docker.yml
│   │   │       └── db/migration/
│   │   │           ├── V1__Create_categories_table.sql
│   │   │           ├── V2__Create_products_table.sql
│   │   │           ├── V3__Create_product_images_table.sql
│   │   │           ├── V4__Create_product_variants_table.sql
│   │   │           ├── V5__Create_inventory_table.sql
│   │   │           ├── V6__Create_bmw_cache_tables.sql
│   │   │           ├── V7__Create_product_compatibility_table.sql
│   │   │           └── V8__Seed_categories_data.sql
│   │   └── test/
│   └── target/ (generated)
│
├── shared/
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/
│               └── com/beamerparts/shared/
│                   ├── dto/
│                   │   ├── response/
│                   │   │   ├── ApiResponse.java
│                   │   │   └── ErrorResponse.java
│                   │   └── event/
│                   │       ├── BMWGenerationUpdatedEvent.java
│                   │       ├── UserRegisteredEvent.java
│                   │       └── ProductCreatedEvent.java
│                   ├── exception/
│                   │   ├── BeamerPartsException.java
│                   │   └── ServiceUnavailableException.java
│                   └── util/
│                       ├── Constants.java
│                       └── DateTimeUtil.java
│
├── docker/
│   ├── docker-compose.yml
│   ├── docker-compose.override.yml
│   └── infrastructure/
│       ├── postgres/
│       │   └── init-scripts/
│       ├── redis/
│       │   └── redis.conf
│       └── rabbitmq/
│           └── rabbitmq.conf
│
├── scripts/
│   ├── build-all.sh
│   ├── run-local.sh
│   ├── run-tests.sh
│   └── deploy-dev.sh
│
└── docs/
    ├── api/
    │   ├── user-service-api.md
    │   ├── vehicle-service-api.md
    │   └── product-service-api.md
    ├── architecture/
    │   ├── service-boundaries.md
    │   └── data-flow.md
    └── setup/
        ├── local-development.md
        └── docker-setup.md
```

### Option 2: Multi-Repo Approach (Enterprise Standard)

**Separate repository for each microservice:**

```
beamerparts-api-gateway/
├── pom.xml
├── Dockerfile
├── src/main/java/com/beamerparts/gateway/
└── ...

beamerparts-user-service/
├── pom.xml
├── Dockerfile  
├── src/main/java/com/beamerparts/user/
└── ...

beamerparts-vehicle-service/
├── pom.xml
├── Dockerfile
├── src/main/java/com/beamerparts/vehicle/
└── ...

beamerparts-product-service/
├── pom.xml
├── Dockerfile
├── src/main/java/com/beamerparts/product/
└── ...

beamerparts-shared-lib/ (Maven dependency)
├── pom.xml
├── src/main/java/com/beamerparts/shared/
└── ...

beamerparts-infrastructure/ (Docker Compose, K8s manifests)
├── docker-compose/
├── kubernetes/
└── terraform/
```

## Spring Boot Application Structure

### Each Microservice Main Class

#### User Service Application
```java
// user-service/src/main/java/com/beamerparts/user/UserServiceApplication.java
package live.alinmiron.beamerparts.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@EnableJpaRepositories
@EnableFeignClients
@EnableRabbit
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

#### Vehicle Service Application
```java
// vehicle-service/src/main/java/com/beamerparts/vehicle/VehicleServiceApplication.java
package live.alinmiron.beamerparts.vehicle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@EnableJpaRepositories
@EnableRabbit
public class VehicleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VehicleServiceApplication.class, args);
    }
}
```

#### Product Service Application
```java
// product-service/src/main/java/com/beamerparts/product/ProductServiceApplication.java
package live.alinmiron.beamerparts.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableJpaRepositories
@EnableFeignClients
@EnableRabbit
@EnableCaching
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
```

#### API Gateway Application
```java
// api-gateway/src/main/java/com/beamerparts/gateway/GatewayApplication.java
package live.alinmiron.beamerparts.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

## Configuration Files Per Service

### User Service Configuration
```yaml
# user-service/src/main/resources/application.yml
server:
  port: 8081
  servlet:
    context-path: /user-service

spring:
  application:
    name: user-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/user_db
    username: ${DB_USERNAME:beamerparts_user}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
  
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}

beamerparts:
  jwt:
    secret: ${JWT_SECRET:your-256-bit-secret-key}
    access-token-expiry: 15m
    refresh-token-expiry: 7d
  
  services:
    product-service:
      url: ${PRODUCT_SERVICE_URL:http://localhost:8083}
    vehicle-service:
      url: ${VEHICLE_SERVICE_URL:http://localhost:8082}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    live.alinmiron.beamerparts.user: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level [%logger{36}] - %msg%n"
```

### Vehicle Service Configuration
```yaml
# vehicle-service/src/main/resources/application.yml
server:
  port: 8082
  servlet:
    context-path: /vehicle-service

spring:
  application:
    name: vehicle-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/vehicle_db
    username: ${DB_USERNAME:beamerparts_vehicle}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

logging:
  level:
    live.alinmiron.beamerparts.vehicle: DEBUG
```

### Product Service Configuration
```yaml
# product-service/src/main/resources/application.yml
server:
  port: 8083
  servlet:
    context-path: /product-service

spring:
  application:
    name: product-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/product_db
    username: ${DB_USERNAME:beamerparts_product}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    timeout: 2000ms
  
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
  
  cache:
    redis:
      time-to-live: 600000 # 10 minutes

beamerparts:
  services:
    vehicle-service:
      url: ${VEHICLE_SERVICE_URL:http://localhost:8082}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

logging:
  level:
    live.alinmiron.beamerparts.product: DEBUG
```

### API Gateway Configuration
```yaml
# api-gateway/src/main/resources/application.yml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  
  cloud:
    gateway:
      routes:
        # User Service Routes
        - id: user-service-auth
          uri: http://localhost:8081
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
        
        - id: user-service-cart
          uri: http://localhost:8081
          predicates:
            - Path=/api/cart/**
          filters:
            - StripPrefix=1
            - AuthenticationFilter
        
        # Vehicle Service Routes
        - id: vehicle-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/vehicles/**
          filters:
            - StripPrefix=1
        
        # Product Service Routes
        - id: product-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/products/**,/api/categories/**
          filters:
            - StripPrefix=1
      
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: 
              - "http://localhost:3000"
              - "http://localhost:4200"
            allowedMethods: 
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

beamerparts:
  jwt:
    secret: ${JWT_SECRET:your-256-bit-secret-key}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,gateway

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    live.alinmiron.beamerparts.gateway: DEBUG
```

## Docker Configuration Per Service

### User Service Dockerfile
```dockerfile
# user-service/Dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# Run application
EXPOSE 8081
CMD ["java", "-jar", "target/user-service-1.0.0.jar"]
```

### Multi-Service Docker Compose
```yaml
# docker-compose.yml
version: '3.8'

services:
  # Infrastructure Services
  postgres-user:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: user_db
      POSTGRES_USER: beamerparts_user
      POSTGRES_PASSWORD: password
    ports:
      - "5433:5432"
    volumes:
      - postgres_user_data:/var/lib/postgresql/data

  postgres-vehicle:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: vehicle_db
      POSTGRES_USER: beamerparts_vehicle
      POSTGRES_PASSWORD: password
    ports:
      - "5434:5432"
    volumes:
      - postgres_vehicle_data:/var/lib/postgresql/data

  postgres-product:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: product_db
      POSTGRES_USER: beamerparts_product
      POSTGRES_PASSWORD: password
    ports:
      - "5435:5432"
    volumes:
      - postgres_product_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  rabbitmq:
    image: rabbitmq:3-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: beamerparts
      RABBITMQ_DEFAULT_PASS: password
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

  # Microservices
  user-service:
    build: ./user-service
    ports:
      - "8081:8081"
    environment:
      DB_USERNAME: beamerparts_user
      DB_PASSWORD: password
      REDIS_HOST: redis
      RABBITMQ_HOST: rabbitmq
      PRODUCT_SERVICE_URL: http://product-service:8083
      VEHICLE_SERVICE_URL: http://vehicle-service:8082
    depends_on:
      - postgres-user
      - redis
      - rabbitmq
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/user-service/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  vehicle-service:
    build: ./vehicle-service
    ports:
      - "8082:8082"
    environment:
      DB_USERNAME: beamerparts_vehicle
      DB_PASSWORD: password
      RABBITMQ_HOST: rabbitmq
    depends_on:
      - postgres-vehicle
      - rabbitmq
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/vehicle-service/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  product-service:
    build: ./product-service
    ports:
      - "8083:8083"
    environment:
      DB_USERNAME: beamerparts_product
      DB_PASSWORD: password
      REDIS_HOST: redis
      RABBITMQ_HOST: rabbitmq
      VEHICLE_SERVICE_URL: http://vehicle-service:8082
    depends_on:
      - postgres-product
      - redis
      - rabbitmq
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/product-service/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    environment:
      REDIS_HOST: redis
      USER_SERVICE_URL: http://user-service:8081
      VEHICLE_SERVICE_URL: http://vehicle-service:8082
      PRODUCT_SERVICE_URL: http://product-service:8083
    depends_on:
      - user-service
      - vehicle-service
      - product-service
      - redis
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_user_data:
  postgres_vehicle_data:
  postgres_product_data:
  redis_data:
  rabbitmq_data:

networks:
  default:
    driver: bridge
```

## Maven Configuration

### Parent POM (Root Level)
```xml
<!-- pom.xml (root) -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.beamerparts</groupId>
    <artifactId>beamerparts-platform</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>BeamerParts Platform</name>
    <description>BMW Aftermarket Parts E-commerce Platform</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.1</spring-boot.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>

    <modules>
        <module>shared</module>
        <module>api-gateway</module>
        <module>user-service</module>
        <module>vehicle-service</module>
        <module>product-service</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>repackage</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### User Service POM
```xml
<!-- user-service/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.beamerparts</groupId>
        <artifactId>beamerparts-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>user-service</artifactId>
    <name>User Service</name>
    <description>User management and authentication microservice</description>

    <dependencies>
        <!-- Shared Module -->
        <dependency>
            <groupId>com.beamerparts</groupId>
            <artifactId>shared</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Cloud -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## Development Workflow

### Build and Run Commands
```bash
# Build all services
mvn clean install

# Build individual service
cd user-service && mvn clean package

# Run with Docker Compose
docker-compose up -d

# Run individual service locally
cd user-service && mvn spring-boot:run

# Run tests
mvn test

# Run integration tests
mvn integration-test
```

### Development Scripts
```bash
#!/bin/bash
# scripts/run-local.sh

echo "Starting BeamerParts microservices locally..."

# Start infrastructure
docker-compose up -d postgres-user postgres-vehicle postgres-product redis rabbitmq

# Wait for databases to be ready
sleep 10

# Start services in background
cd user-service && mvn spring-boot:run &
cd vehicle-service && mvn spring-boot:run &
cd product-service && mvn spring-boot:run &
cd api-gateway && mvn spring-boot:run &

echo "All services started. API Gateway available at http://localhost:8080"
```

## Key Benefits of This Structure

### Independent Development
- Each service can be developed, tested, and deployed independently
- Different teams can work on different services
- Technology choices can vary per service (within Spring Boot ecosystem)

### Scalability
- Scale individual services based on demand
- Different database configurations per service
- Independent resource allocation

### Maintainability
- Clear service boundaries and responsibilities
- Isolated testing and debugging
- Easy to understand and modify individual services

### Production Readiness
- Each service builds to its own JAR file
- Individual Docker containers for deployment
- Kubernetes-ready with proper health checks

---

**This structure provides a solid foundation for enterprise-grade microservices development while maintaining clear separation of concerns and enabling independent development and deployment of each service.**