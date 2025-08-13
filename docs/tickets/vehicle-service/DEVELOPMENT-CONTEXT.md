# Vehicle Service Development Context

## 🎯 **Service Overview**
**Port**: 8082 | **Database**: vehicle_db (PostgreSQL 5434) | **Dependencies**: RabbitMQ

Vehicle Service manages BMW vehicle hierarchy, compatibility data, and synchronization with product-service for the BeamerParts platform. Acts as the master data source for BMW vehicle information.

## 📋 **Current Implementation Status**

### ✅ **Completed (Existing)**
- **Entities**: BmwSeries, BmwGeneration, VehicleCompatibilityRegistry, VehicleSyncEvent  
- **Repositories**: BmwSeriesRepository, BmwGenerationRepository, VehicleSyncEventRepository
- **DTOs**: Basic request/response DTOs for BMW series/generation data
- **Database**: Complete Flyway migrations with BMW hierarchy and compatibility structure
- **Configuration**: Application.yml with database and RabbitMQ settings

### 🚧 **To Implement (Following M0→M1→M2→M3 Pattern)**
- **Domain Services**: BMW compatibility validation, data synchronization logic
- **Controllers**: External APIs (vehicle lookup), internal APIs (compatibility validation), admin APIs
- **Sync Services**: Event-driven synchronization with product-service BMW cache
- **Mappers**: MapStruct entity ↔ DTO conversion with business logic integration
- **Event Publishing**: RabbitMQ events for BMW data changes
- **Comprehensive Tests**: BMW-specific compatibility scenarios and edge cases

## 🎯 **Strategic Implementation Approach**

Based on product-service patterns and BMW-specific business requirements:

### **TIER 1 Components (TDD Required - High Risk)**
- **BMW Compatibility Logic**: Year ranges, body code validation, generation matching
- **Data Synchronization**: Event-driven sync with product-service, conflict resolution
- **Compatibility Validation**: Critical for preventing wrong part sales

### **TIER 2 Components (Implementation-First - Standard Risk)** 
- **BMW Data CRUD**: Series/generation management, basic lookup APIs
- **Admin Features**: BMW data management interface
- **Event Publishing**: Basic event mechanisms for data changes

## 📚 **Technology Rollout Phases**

### **Phase M0 - Basic External & Internal APIs**
- BMW series/generation lookup endpoints
- Core internal APIs for compatibility validation
- Basic CRUD operations for BMW data
- **Target**: 70% test coverage, critical compatibility paths tested

### **Phase M1 - Messaging & Synchronization**
- RabbitMQ event publishing for BMW data changes  
- Compatibility validation with advanced business rules
- Synchronization logic with product-service BMW cache
- **Target**: 80% test coverage, sync flows tested

### **Phase M2 - Caching & Performance**
- Response caching for BMW lookup data
- Performance optimization for compatibility queries
- Bulk operations for efficiency
- **Target**: Sub-100ms BMW lookup responses

### **Phase M3 - Admin & Advanced Features**
- Admin BMW data management APIs
- Advanced compatibility search and reporting
- Data validation and consistency checks
- **Target**: Complete BMW data management suite

## 🎯 **BMW-Specific Business Logic**

### **Critical Compatibility Scenarios**
- **Year Boundaries**: 2019 BMW 3 Series F30 (last year) vs G20 (first year) compatibility
- **Body Code Variants**: F30 sedan vs F31 wagon vs F34 Gran Turismo compatibility  
- **Engine Variants**: 320i vs 330d vs M3 specific parts validation
- **Series Categories**: Sedan vs X-Series vs Z-Series part compatibility rules

### **Data Synchronization Requirements**
- Master data changes in Vehicle Service → Events → Product Service cache updates
- Conflict resolution when sync fails
- Data consistency validation across services
- Audit trail for BMW data modifications

## 🧪 **Testing Strategy (BMW-Specific Patterns)**

### **Compatibility Domain Service Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BmwCompatibilityDomainServiceTest {
    // Test year range validation (2019 F30 vs 2019 G20)
    // Test body code matching (F30, F31, F34, F35)
    // Test engine variant compatibility
    // Target: 90%+ coverage for compatibility logic
}
```

### **BMW Entity Business Logic Testing**
```java
@SpringBootTest  
@Transactional
@ActiveProfiles("test")
class BmwGenerationTest {
    // Test year range methods (isCurrentGeneration, includesYear)
    // Test body code validation (hasBodyCode)
    // Test display name generation
    // Target: 85%+ coverage for entity business methods
}
```

### **Sync Service Testing**
```java
@SpringBootTest
@Transactional  
@ActiveProfiles("test")
class VehicleSyncServiceTest {
    // Test event publishing for BMW data changes
    // Test sync conflict resolution
    // Test data consistency validation
    // Mock product-service endpoints
}
```

## 🔗 **Key Integrations**

### **Product Service Integration**
- BMW cache synchronization via RabbitMQ events
- Compatibility data updates propagation
- Conflict resolution for sync failures

### **User Service Integration**
- BMW validation for user vehicle preferences
- Compatibility checking for user's registered vehicles

### **Order Service Integration**
- Part compatibility validation during checkout
- BMW data lookup for order processing

## 🚀 **AI Agent Development Prompt**

```
CONTEXT: Developing Vehicle Service for BeamerParts BMW e-commerce platform

CURRENT STATE:  
- BMW entities, repositories, DTOs, database migrations exist
- Basic Spring Boot configuration with RabbitMQ
- Need to implement: Compatibility logic, sync services, controllers, mappers, tests

BMW-SPECIFIC REQUIREMENTS:
- Handle BMW model year edge cases (F30 2019 vs G20 2019)
- Validate part compatibility across generations and body codes  
- Manage data synchronization with product-service BMW cache
- Support advanced compatibility queries

IMPLEMENTATION STRATEGY:
- Follow M0→M1→M2→M3 phase pattern from existing tickets
- Apply strategic TDD: Compatibility logic requires tests first
- Use product-service BMW compatibility patterns as reference

CRITICAL BMW SCENARIOS:
- Year boundary compatibility (generation transitions)
- Body code variant validation (sedan vs wagon vs GT)
- Engine-specific part compatibility  
- Series-level compatibility rules

TESTING APPROACH:
- Use @SpringBootTest + @Transactional for domain services
- Test BMW-specific edge cases comprehensively
- Mock only external services (product-service)
- Apply BMW generation/series test data patterns

START WITH: VS-M0-external-internal-apis (BMW lookup & compatibility)
REFERENCE: product-service BMW compatibility tests for proven patterns
```

## 📁 **Package Structure (Standard BeamerParts Pattern)**
```
vehicle-service/src/main/java/live/alinmiron/beamerparts/vehicle/
├── controller/
│   ├── external/          # Public BMW lookup APIs (/api/vehicles)
│   ├── internal/          # Service-to-service APIs (/internal/...)
│   ├── admin/            # Admin BMW management (/api/admin/vehicles) 
│   └── *OpenApiSpec.java # OpenAPI specifications
├── service/
│   ├── domain/           # Core business logic (BmwCompatibilityDomainService, VehicleSyncService)
│   └── internal/         # Internal service operations
├── mapper/               # MapStruct mappers with BMW business logic
├── event/               # RabbitMQ event publishers/listeners for sync
├── entity/              # ✅ Already implemented
├── repository/          # ✅ Already implemented
├── dto/                 # ✅ Basic DTOs exist  
└── exception/           # Custom exceptions + GlobalExceptionHandler
```

## 📊 **Success Metrics**
- **Coverage**: 85%+ overall (90%+ for compatibility domain services)
- **Performance**: <100ms BMW lookup, <50ms compatibility validation
- **Data Quality**: 100% sync consistency with product-service
- **BMW Accuracy**: Zero incorrect compatibility validations
