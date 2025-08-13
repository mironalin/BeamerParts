# US-M0: User Service Internal APIs

**Phase**: M0 Basic | **Service**: user-service | **Priority**: High | **Estimated Effort**: 2-3 days

## 🎯 **Summary**
Implement internal service-to-service APIs for user authentication validation, cart operations, and user data access. These endpoints enable other services to integrate with user functionality.

## 📋 **Scope**

### **Internal Endpoints (direct service-to-service `/internal/...`)**

#### **Authentication APIs**
- `POST /internal/auth/validate-token` - Validate JWT token and return user info
- `GET /internal/users/{userId}` - Get user details by ID
- `POST /internal/auth/create-service-token` - Generate service-to-service tokens

#### **Cart APIs**  
- `GET /internal/users/{userId}/cart` - Get user's cart for checkout processing
- `POST /internal/users/{userId}/cart/validate` - Validate cart contents with inventory
- `POST /internal/cart/bulk-validate` - Bulk cart validation for multiple users
- `DELETE /internal/users/{userId}/cart/reserve` - Reserve cart items for checkout

#### **User Data APIs**
- `GET /internal/users/{userId}/profile` - Get user profile for order processing
- `GET /internal/users/{userId}/vehicles` - Get user's BMW vehicles for compatibility
- `POST /internal/users/bulk` - Bulk user lookup by IDs or emails

## 🏗️ **Implementation Requirements**

### **Internal DTOs (Service-to-Service)**
```
dto/internal/request/
├── ValidateTokenRequestDto.java
├── BulkCartValidationRequestDto.java
├── CartReservationRequestDto.java  
└── BulkUserLookupRequestDto.java

dto/internal/response/
├── TokenValidationResponseDto.java
├── UserInternalDto.java
├── CartInternalDto.java
├── CartValidationResponseDto.java
└── UserVehicleInternalDto.java
```

### **Internal Controllers**
```java
@RestController
@RequestMapping("/internal/auth")
public class AuthInternalController {
    // No OpenAPI documentation required for internal APIs
    // Focus on performance and reliability
    // Simple error handling for service-to-service calls
}

@RestController
@RequestMapping("/internal/users")  
public class UserInternalController {
    // User data access for other services
    // Cart operations for checkout processing
    // Bulk operations for performance
}
```

### **Internal Services**
```java
@Service
public class UserInternalService {
    // Delegates to domain services
    // Handles service-to-service communication patterns
    // Maps between domain models and internal DTOs
}

@Service
public class CartInternalService {
    // Cart validation with product-service integration
    // Cart reservation for checkout process
    // Bulk operations for performance
}
```

## 🧪 **Testing Requirements**

### **Integration Testing (Primary Focus)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserInternalServiceTest {
    // Test service-to-service communication patterns
    // Test bulk operations and performance scenarios
    // Mock external service calls to product-service
    // Target: 80%+ coverage
}

@WebMvcTest(AuthInternalController.class)
@Import({UserMapper.class})
class AuthInternalControllerTest {
    // Test internal API contracts
    // Test error handling for service failures
    // Test authentication validation flows
    // Target: 75%+ coverage
}
```

### **Security Testing**
```java
@SpringBootTest
class InternalApiSecurityTest {
    // Test that internal endpoints are not exposed via gateway
    // Test service-to-service authentication (if implemented)
    // Test request validation and error handling
}
```

## 🔗 **Integration Points**

### **Product Service Integration**
- Cart validation requires real-time inventory checks
- Product availability and pricing validation
- Stock reservation coordination

### **Order Service Integration**
- Cart-to-order conversion during checkout
- User profile data for order processing
- Authentication validation for order placement

### **Vehicle Service Integration**
- User vehicle compatibility validation
- BMW generation lookup for user vehicles

## ✅ **Acceptance Criteria**

### **Functional Requirements**
- [ ] Token validation working for other services
- [ ] Cart validation with real-time inventory checking
- [ ] User data access for order processing
- [ ] Bulk operations for performance optimization

### **Performance Requirements**
- [ ] Token validation completes within 50ms
- [ ] Cart validation completes within 200ms
- [ ] Bulk operations handle 100+ items efficiently
- [ ] Proper connection pooling for database access

### **Reliability Requirements**
- [ ] Graceful handling of external service failures
- [ ] Proper error responses for service-to-service calls
- [ ] Circuit breaker pattern for product-service integration
- [ ] Request timeout handling (5 second maximum)

### **Security Requirements**
- [ ] Internal endpoints not accessible via gateway
- [ ] Proper request validation and sanitization
- [ ] Audit logging for sensitive operations
- [ ] Rate limiting for bulk operations

## 🔧 **Technical Implementation**

### **Service-to-Service Communication**
```java
@Component
public class ProductServiceClient {
    // HTTP client for product-service integration
    // Circuit breaker for fault tolerance
    // Proper timeout and retry configuration
    // Error handling and fallback responses
}
```

### **Internal Mappers**
```java
@Mapper(componentModel = "spring")
public interface UserInternalMapper {
    // Internal DTO mapping (different from external DTOs)
    // Include only necessary data for service-to-service
    // Performance-optimized mapping
}
```

### **Error Handling**
```java
@ControllerAdvice
public class InternalApiExceptionHandler {
    // Different error format for internal APIs
    // Service-specific error codes
    // Correlation IDs for distributed tracing
}
```

## 📚 **Reference Materials**
- **API Contract**: `docs/beamerparts_api_contract.md` - User Service Internal APIs
- **Service Communication**: `.cursorrules` - Client configuration and patterns
- **Testing Patterns**: `product-service` internal service tests
- **Circuit Breaker**: Spring Cloud Circuit Breaker configuration

## 🚀 **Getting Started**
1. **Review** API contract for exact internal endpoint specifications
2. **Implement** internal DTOs focusing on performance and necessary data only
3. **Create** internal controllers with minimal but effective error handling
4. **Test** service-to-service communication patterns thoroughly
5. **Validate** integration with product-service for cart operations