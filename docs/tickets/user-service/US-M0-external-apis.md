# US-M0: User Service External APIs

**Phase**: M0 Basic | **Service**: user-service | **Priority**: High | **Estimated Effort**: 3-4 days

## 🎯 **Summary**
Implement core external authentication and cart management endpoints via API gateway. This establishes the foundation for user authentication and shopping cart functionality across the platform.

## 📋 **Scope**

### **External Endpoints (via gateway `/api/...`)**

#### **Authentication APIs**
- `POST /auth/register` - User registration with email verification
- `POST /auth/login` - User login with JWT token generation  
- `POST /auth/refresh` - Refresh access token using refresh token
- `POST /auth/logout` - Logout and revoke refresh token
- `GET /auth/me` - Get current authenticated user profile

#### **Shopping Cart APIs**
- `GET /cart` - Get user's current shopping cart
- `POST /cart/items` - Add item to cart with quantity validation
- `PUT /cart/items/{id}` - Update cart item quantity
- `DELETE /cart/items/{id}` - Remove specific item from cart  
- `DELETE /cart/clear` - Clear entire shopping cart

## 🏗️ **Implementation Requirements**

### **DTOs (Standard BeamerParts Pattern)**
```
dto/external/request/
├── RegisterUserRequestDto.java
├── LoginRequestDto.java  
├── RefreshTokenRequestDto.java
├── AddToCartRequestDto.java
└── UpdateCartItemRequestDto.java

dto/external/response/
├── AuthResponseDto.java
├── UserResponseDto.java
├── CartResponseDto.java
└── CartItemResponseDto.java
```

### **Controllers with OpenAPI Specifications**
```java
// Pattern: Create *OpenApiSpec classes alongside controllers
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthController {
    // Thin controllers - delegate to domain services
    // Reference OpenAPI specs for @Operation details
}

@Component
public class AuthControllerOpenApiSpec {
    // All @Operation annotations and API documentation
    // Keep controllers clean by externalizing OpenAPI specs
}
```

### **Domain Services (TDD Required)**
```java
@Service
@Transactional
public class AuthenticationDomainService {
    // Core authentication business logic
    // Password hashing with BCrypt
    // JWT token generation and validation
    // Account status management
}

@Service  
@Transactional
public class CartDomainService {
    // Shopping cart business logic
    // Real-time inventory validation with product-service
    // Cart session management
    // Price synchronization
}
```

### **Security Configuration**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // JWT authentication filter
    // Role-based authorization rules
    // Password encoder configuration
    // CORS settings for frontend
}
```

## 🧪 **Testing Requirements (Strategic TDD)**

### **TIER 1 - TDD Required (High Risk)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AuthenticationDomainServiceTest {
    // Test user registration with duplicate email handling
    // Test password hashing and verification
    // Test JWT token generation and validation
    // Test account lockout after failed attempts
    // Target: 90%+ coverage
}

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CartDomainServiceTest {
    // Test cart item addition with stock validation
    // Test cart total calculations
    // Test cart session management
    // Mock product-service for inventory checks
    // Target: 90%+ coverage
}
```

### **TIER 2 - Implementation First**
```java
@WebMvcTest(AuthController.class)
@Import({UserMapper.class, CartMapper.class})
class AuthControllerTest {
    // Test API contracts and validation
    // Test authentication flows
    // Mock domain services
    // Target: 75%+ coverage
}
```

## 🔗 **Integration Points**

### **Product Service Integration**
- Real-time stock validation for cart items
- Product price synchronization
- Product availability checks

### **Vehicle Service Integration**
- BMW compatibility validation for cart items (future)
- User vehicle preference validation

## ✅ **Acceptance Criteria**

### **Functional Requirements**
- [ ] All authentication endpoints implemented and working
- [ ] Shopping cart CRUD operations functional
- [ ] JWT token generation and validation working
- [ ] Password hashing with BCrypt implemented
- [ ] Cart items validate against product-service inventory

### **Quality Requirements**
- [ ] `scripts/dev/run-tests.sh` passes with ≥70% coverage for user-service
- [ ] All endpoints documented with OpenAPI using *OpenApiSpec pattern
- [ ] Consistent `ApiResponse` wrapper for success and error responses
- [ ] Proper validation error messages with Jakarta validation

### **Security Requirements**
- [ ] JWT tokens properly signed and validated
- [ ] Passwords never stored in plain text
- [ ] Authentication endpoints protected against brute force
- [ ] Proper CORS configuration for frontend domains

### **Performance Requirements**
- [ ] Authentication operations complete within 200ms
- [ ] Cart operations complete within 100ms
- [ ] Proper error handling for external service failures

## 📚 **Reference Materials**
- **API Contract**: `docs/beamerparts_api_contract.md` - User Service M0 section
- **Standards**: `.cursorrules` - OpenAPI, testing, and security patterns
- **Proven Patterns**: `product-service` implementation for testing and structure
- **JWT Configuration**: Already configured in `application.yml`

## 🚀 **Getting Started**
1. **Review** existing User entities and repositories  
2. **Start with TDD** for AuthenticationDomainService (high-risk security logic)
3. **Follow product-service patterns** for testing and structure
4. **Implement** controllers with proper OpenAPI documentation
5. **Validate** integration with product-service for cart functionality