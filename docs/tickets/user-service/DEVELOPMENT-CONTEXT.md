# User Service Development Context

## 🎯 **Service Overview**
**Port**: 8081 | **Database**: user_db (PostgreSQL 5433) | **Dependencies**: Redis, RabbitMQ, JWT

User Service handles authentication, authorization, user profiles, shopping cart, and user vehicle preferences for the BeamerParts BMW parts e-commerce platform.

## 📋 **Current Implementation Status**

### ✅ **Completed (Existing)**
- **Entities**: User, UserRefreshToken, UserVehicle, CartItem, UserRole enum
- **Repositories**: UserRepository, UserRefreshTokenRepository, UserVehicleRepository, CartItemRepository  
- **DTOs**: Basic request/response DTOs for registration, cart operations, vehicle management
- **Database**: Complete Flyway migrations with proper constraints, indexes, and foreign keys
- **Configuration**: Full application.yml with JWT settings, database, Redis, RabbitMQ

### 🚧 **To Implement (Following M0→M1→M2→M3 Pattern)**
- **Domain Services**: Authentication, authorization, cart management, user profile management
- **Controllers**: External APIs (via gateway), internal APIs (service-to-service), admin APIs
- **Security**: JWT token services, password hashing, role-based authorization
- **Mappers**: MapStruct entity ↔ DTO conversion with real-time data integration
- **Event Publishing**: RabbitMQ events for user registration, cart changes
- **Comprehensive Tests**: Following product-service patterns (92.38% coverage achieved)

## 🎯 **Strategic Implementation Approach**

Based on lessons learned from product-service and the `.cursorrules` strategic TDD approach:

### **TIER 1 Components (TDD Required - High Risk)**
- **Authentication & Security Logic**: Password hashing, JWT tokens, role validation
- **Cart Management**: Real-time inventory validation, session management, cart-to-order conversion
- **Authorization Services**: Role-based access control, resource ownership validation

### **TIER 2 Components (Implementation-First - Standard Risk)**
- **User Profile CRUD**: Basic profile management, vehicle preferences
- **Standard Controllers**: Registration, profile updates, basic validation
- **Admin Features**: User management, search, analytics

## 📚 **Technology Rollout Phases**

### **Phase M0 - Basic External & Internal APIs**
- Authentication endpoints (register, login, refresh, logout, me)
- Cart management endpoints (CRUD operations)
- Core internal APIs for service-to-service communication
- **Target**: 70% test coverage, all critical auth paths tested

### **Phase M1 - Messaging & Synchronization**  
- RabbitMQ event publishing (user registration, cart changes)
- Profile management with vehicle preferences
- Cart validation with product-service integration
- **Target**: 80% test coverage, event flows tested

### **Phase M2 - Caching & Performance**
- Redis session management and cart persistence
- Performance optimization for auth flows
- Response caching for user profiles
- **Target**: Sub-200ms response times

### **Phase M3 - Admin & Advanced Features**
- Admin user management APIs
- Advanced search and analytics
- Account linking for guest-to-user migration
- **Target**: Complete feature set

## 🧪 **Testing Strategy (Product-Service Proven Patterns)**

### **Domain Service Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AuthenticationDomainServiceTest {
    // Use real repositories, mock only external services
    // Test business logic comprehensively
    // Target: 90%+ coverage for security-critical logic
}
```

### **Entity Business Logic Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")  
class UserTest {
    // Test role validation, account status, security methods
    // Use timestamp-based unique data generation
    // Target: 85%+ coverage for entity business methods
}
```

### **Controller Testing**
```java
@WebMvcTest(AuthController.class)
@Import({UserMapper.class, CartMapper.class})
class AuthControllerTest {
    // Use real mappers, mock domain services
    // Test API contracts, validation, error handling
    // Target: 75%+ coverage
}
```

## 🔗 **Key Integrations**

### **Product Service Integration**
- Real-time stock validation for cart items
- Product availability checks during cart operations
- BMW compatibility validation for user vehicles

### **Vehicle Service Integration**  
- BMW series/generation validation for user vehicles
- Compatibility checking for cart items

### **Order Service Integration**
- Cart-to-order conversion during checkout
- User authentication for order placement

## 🚀 **AI Agent Development Prompt**

```
CONTEXT: Developing User Service for BeamerParts BMW e-commerce platform

CURRENT STATE:
- Entities, repositories, DTOs, and database migrations exist
- Application fully configured with JWT, Redis, RabbitMQ
- Need to implement: Services, controllers, security, mappers, tests

IMPLEMENTATION STRATEGY:
- Follow M0→M1→M2→M3 phase pattern from existing tickets
- Apply strategic TDD: Security/cart logic requires tests first
- Use product-service proven patterns (92.38% coverage achieved)

CRITICAL REQUIREMENTS:
- JWT authentication with role-based authorization
- Shopping cart with real-time inventory validation  
- User vehicle management with BMW compatibility
- Event-driven architecture with RabbitMQ

TESTING APPROACH:
- Use @SpringBootTest + @Transactional for domain services
- Use @WebMvcTest + @Import({Mappers}) for controllers  
- Mock only external services, use real repositories
- Apply timestamp-based unique data generation

START WITH: US-M0-external-apis (Authentication & Cart endpoints)
REFERENCE: .cursorrules for detailed patterns and standards
```

## 📁 **Package Structure (Standard BeamerParts Pattern)**
```
user-service/src/main/java/live/alinmiron/beamerparts/user/
├── controller/
│   ├── external/          # Public APIs via gateway (/api/auth, /api/cart)
│   ├── internal/          # Service-to-service APIs (/internal/...)
│   ├── admin/            # Admin APIs (/api/admin/users)
│   └── *OpenApiSpec.java # OpenAPI specifications
├── service/
│   ├── domain/           # Core business logic (AuthenticationDomainService, CartDomainService)
│   └── internal/         # Internal service operations
├── security/             # JWT, password hashing, authorization
├── mapper/               # MapStruct mappers with real-time data
├── event/               # RabbitMQ event publishers/listeners
├── entity/              # ✅ Already implemented
├── repository/          # ✅ Already implemented  
├── dto/                 # ✅ Basic DTOs exist
└── exception/           # Custom exceptions + GlobalExceptionHandler
```

## 📊 **Success Metrics**
- **Coverage**: 85%+ overall (90%+ for security/cart domain services)
- **Performance**: <200ms auth flows, <100ms cart operations
- **Security**: Zero authentication bypass vulnerabilities
- **Integration**: Seamless cart-to-order flow with other services
