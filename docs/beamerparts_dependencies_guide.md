# BeamerParts - Complete Dependencies & POM Configuration Guide

## Overview

This document provides complete Maven POM configurations for all microservices in the BeamerParts platform. Version management is handled through Spring Boot BOM and parent POM properties.

## How Shared Dependencies Work

### Shared Module Concept
The **shared** module is a Maven library that contains common code used across multiple microservices:

```
shared/
├── src/main/java/com/beamerparts/shared/
│   ├── dto/
│   │   ├── response/
│   │   │   ├── ApiResponse.java
│   │   │   ├── ErrorResponse.java
│   │   │   └── PagedResponse.java
│   │   └── event/
│   │       ├── BMWGenerationUpdatedEvent.java
│   │       ├── UserRegisteredEvent.java
│   │       └── OrderPlacedEvent.java
│   ├── exception/
│   │   ├── BeamerPartsException.java
│   │   ├── ValidationException.java
│   │   └── ServiceUnavailableException.java
│   ├── enums/
│   │   ├── OrderStatus.java
│   │   ├── PaymentStatus.java
│   │   └── UserRole.java
│   └── util/
│       ├── Constants.java
│       ├── DateTimeUtil.java
│       └── ValidationUtil.java
```

### Why Use a Shared Module?
- **Consistency**: Same DTOs and exceptions across all services
- **DRY Principle**: Avoid duplicating common code
- **Type Safety**: Shared event classes ensure type-safe inter-service communication
- **Maintenance**: Single place to update common functionality

### How Services Use Shared Module
Each service includes the shared module as a dependency:
```xml
<dependency>
    <groupId>live.alinmiron.beamerparts</groupId>
    <artifactId>shared</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Version Management Strategy

### Parent POM Properties
```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    
    <!-- Only specify versions for dependencies NOT managed by Spring Boot BOM -->
    <jjwt.version>0.11.5</jjwt.version>
    <stripe.version>24.2.0</stripe.version>
    <itext.version>8.0.2</itext.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <hibernate-types.version>2.21.1</hibernate-types.version>
    <thumbnailator.version>0.4.20</thumbnailator.version>
    <commons-fileupload.version>1.5</commons-fileupload.version>
    <hibernate-search.version>7.0.0.Final</hibernate-search.version>
</properties>
```

### Spring Boot BOM Manages These Automatically
- All `spring-boot-starter-*` dependencies
- Spring Framework dependencies
- PostgreSQL driver
- Flyway
- Jackson
- Lombok
- TestContainers
- And many more...

## Complete POM Configurations

### Parent POM (Root Level)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>live.alinmiron.beamerparts</groupId>
    <artifactId>beamerparts-platform</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>BeamerParts Platform</name>
    <description>BMW Aftermarket Parts E-commerce Platform</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Framework Versions -->
        <spring-boot.version>3.2.1</spring-boot.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        
        <!-- Custom Dependencies (not managed by Spring Boot BOM) -->
        <jjwt.version>0.11.5</jjwt.version>
        <stripe.version>24.2.0</stripe.version>
        <itext.version>8.0.2</itext.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <hibernate-types.version>2.21.1</hibernate-types.version>
        <thumbnailator.version>0.4.20</thumbnailator.version>
        <commons-fileupload.version>1.5</commons-fileupload.version>
        <hibernate-search.version>7.0.0.Final</hibernate-search.version>
    </properties>

    <modules>
        <module>shared</module>
        <module>api-gateway</module>
        <module>user-service</module>
        <module>vehicle-service</module>
        <module>product-service</module>
        <module>order-service</module>
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
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### Shared Module POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>live.alinmiron.beamerparts</groupId>
        <artifactId>beamerparts-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>shared</artifactId>
    <name>Shared Library</name>
    <description>Common DTOs, exceptions, and utilities</description>

    <dependencies>
        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>
        
        <!-- Validation -->
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Apache Commons -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### API Gateway POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>live.alinmiron.beamerparts</groupId>
        <artifactId>beamerparts-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>api-gateway</artifactId>
    <name>API Gateway</name>
    <description>Spring Cloud Gateway for routing and security</description>

    <dependencies>
        <!-- Shared Module -->
        <dependency>
            <groupId>live.alinmiron.beamerparts</groupId>
            <artifactId>shared</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- Spring Cloud Gateway -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        
        <!-- Circuit Breaker -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
        </dependency>
        
        <!-- Load Balancer -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
        
        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Redis for Rate Limiting -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>
        
        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- DevTools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
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

### User Service POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>live.alinmiron.beamerparts</groupId>
        <artifactId>beamerparts-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>user-service</artifactId>
    <name>User Service</name>
    <description>User management and authentication microservice</description>

    <dependencies>
        <!-- Shared Module -->
        <dependency>
            <groupId>live.alinmiron.beamerparts</groupId>
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
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
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
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- DevTools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
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

### Vehicle Service POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>live.alinmiron.beamerparts</groupId>
        <artifactId>beamerparts-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>vehicle-service</artifactId>
    <name>Vehicle Service</name>
    <description>BMW vehicle hierarchy and compatibility microservice</description>

    <dependencies>
        <!-- Shared Module -->
        <dependency>
            <groupId>live.alinmiron.beamerparts</groupId>
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
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
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

        <!-- JSON Processing for Arrays -->
        <dependency>
            <groupId>com.vladmihalcea</groupId>
            <artifactId>hibernate-types-60</artifactId>
            <version>${hibernate-types.version}</version>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- DevTools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
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

### Product Service POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>live.alinmiron.beamerparts</groupId>
        <artifactId>beamerparts-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>product-service</artifactId>
    <name>Product Service</name>
    <description>Product catalog and inventory microservice</description>

    <dependencies>
        <!-- Shared Module -->
        <dependency>
            <groupId>live.alinmiron.beamerparts</groupId>
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
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
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

        <!-- Search Engine -->
        <dependency>
            <groupId>org.hibernate.search</groupId>
            <artifactId>hibernate-search-mapper-orm</artifactId>
            <version>${hibernate-search.version}</version>
        </dependency>
        <dependency>
            <groupId>org.hibernate.search</groupId>
            <artifactId>hibernate-search-backend-lucene</artifactId>
            <version>${hibernate-search.version}</version>
        </dependency>

        <!-- File Upload -->
        <dependency>
            <groupId>commons-fileupload</groupId>
            <artifactId>commons-fileupload</artifactId>
            <version>${commons-fileupload.version}</version>
        </dependency>

        <!-- Image Processing -->
        <dependency>
            <groupId>net.coobird</groupId>
            <artifactId>thumbnailator</artifactId>
            <version>${thumbnailator.version}</version>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.vladmihalcea</groupId>
            <artifactId>hibernate-types-60</artifactId>
            <version>${hibernate-types.version}</version>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- DevTools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
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

### Order Service POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>live.alinmiron.beamerparts</groupId>
        <artifactId>beamerparts-platform</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>order-service</artifactId>
    <name>Order Service</name>
    <description>Order processing and payment microservice</description>

    <dependencies>
        <!-- Shared Module -->
        <dependency>
            <groupId>live.alinmiron.beamerparts</groupId>
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
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
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

        <!-- Payment Processing -->
        <dependency>
            <groupId>com.stripe</groupId>
            <artifactId>stripe-java</artifactId>
            <version>${stripe.version}</version>
        </dependency>

        <!-- PDF Generation -->
        <dependency>
            <groupId>com.itextpdf</groupId>
            <artifactId>itext-core</artifactId>
            <version>${itext.version}</version>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.vladmihalcea</groupId>
            <artifactId>hibernate-types-60</artifactId>
            <version>${hibernate-types.version}</version>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- DevTools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
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

## Complete Dependency Matrix

| Dependency Category | Shared | API Gateway | User Service | Vehicle Service | Product Service | Order Service |
|-------------------|--------|-------------|--------------|-----------------|-----------------|---------------|
| **Core Spring Boot** |
| spring-boot-starter-web | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| spring-boot-starter-data-jpa | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| spring-boot-starter-validation | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| spring-boot-starter-actuator | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| spring-boot-devtools | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Security & Authentication** |
| spring-boot-starter-security | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| jjwt-api | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| jjwt-impl | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| jjwt-jackson | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| spring-security-test | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Database & Persistence** |
| postgresql | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| flyway-core | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| hibernate-types-60 | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| **Caching & Data** |
| spring-boot-starter-data-redis | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ |
| spring-boot-starter-data-redis-reactive | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| spring-boot-starter-cache | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| **Messaging** |
| spring-boot-starter-amqp | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Spring Cloud** |
| spring-cloud-starter-gateway | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| spring-cloud-starter-openfeign | ❌ | ❌ | ✅ | ❌ | ✅ | ✅ |
| spring-cloud-starter-circuitbreaker-* | ❌ | ✅ | ✅ | ❌ | ✅ | ✅ |
| spring-cloud-starter-loadbalancer | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Email & Communication** |
| spring-boot-starter-mail | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| spring-boot-starter-thymeleaf | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Payment & PDF** |
| stripe-java | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| itext-core | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Search & File Processing** |
| hibernate-search-mapper-orm | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| hibernate-search-backend-lucene | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| commons-fileupload | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| thumbnailator | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| **Utilities** |
| lombok | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| mapstruct | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| commons-lang3 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| jackson-annotations | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Testing** |
| spring-boot-starter-test | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| testcontainers-junit-jupiter | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| testcontainers-postgresql | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |

## Key Points About Dependency Management

### 1. Version Management
- **Spring Boot BOM** manages most versions automatically
- **Parent POM** only specifies versions for dependencies not managed by Spring Boot
- **Consistency** across all services through parent POM properties

### 2. Shared Module Benefits
- **Common DTOs**: `ApiResponse`, `ErrorResponse`, event classes
- **Common Exceptions**: Base exception classes and error handling
- **Utilities**: Date/time helpers, validation utilities, constants
- **Type Safety**: Ensures consistent data structures across services

### 3. Service-Specific Dependencies
- **API Gateway**: Reactive dependencies, circuit breakers, security
- **User Service**: Security, JWT, Redis for sessions, OpenFeign for service calls
- **Vehicle Service**: Minimal dependencies, focused on data management
- **Product Service**: Search engine, file processing, image handling
- **Order Service**: Payment processing, PDF generation, email templates

### 4. Testing Strategy
- **TestContainers** for integration testing with real databases
- **Service-specific test dependencies** only where needed
- **Consistent test framework** across all services

---

**This dependency structure ensures clean separation of concerns while maintaining consistency and avoiding version conflicts across the microservices platform.**