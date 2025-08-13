# US-M3: User Service Admin APIs & Advanced Features

**Phase**: M3 Admin/Advanced | **Service**: user-service | **Priority**: Low | **Estimated Effort**: 2-3 days

## 🎯 **Summary**
Implement admin management APIs for user administration, guest order linking, advanced search capabilities, and user analytics. Provides comprehensive admin interface for user management.

## 📋 **Scope**

### **Admin Endpoints (via gateway `/api/admin/...`)**

#### **User Management APIs**
- `GET /admin/users` - List users with pagination, filtering, and search
- `GET /admin/users/{userId}` - Get detailed user information
- `PUT /admin/users/{userId}` - Update user information (admin only)
- `PUT /admin/users/{userId}/status` - Activate/deactivate user account
- `PUT /admin/users/{userId}/role` - Update user role (admin/super admin only)
- `DELETE /admin/users/{userId}` - Soft delete user account

#### **User Analytics APIs**
- `GET /admin/users/analytics` - User registration and activity analytics
- `GET /admin/users/search` - Advanced user search with multiple criteria
- `GET /admin/users/{userId}/activity` - User activity history and audit log
- `GET /admin/users/{userId}/orders` - User's order history (integration with order-service)

#### **Guest Order Linking APIs**
- `POST /admin/users/link-guest-orders` - Link guest orders to user account
- `GET /admin/users/orphaned-orders` - Find guest orders without linked accounts
- `POST /admin/users/{userId}/migrate-data` - Migrate guest data to user account

## 🏗️ **Implementation Requirements**

### **Admin DTOs**
```
dto/admin/request/
├── UpdateUserRequestDto.java
├── UserSearchRequestDto.java
├── LinkGuestOrdersRequestDto.java
└── UserStatusUpdateRequestDto.java

dto/admin/response/
├── UserAdminResponseDto.java
├── UserAnalyticsResponseDto.java
├── UserActivityResponseDto.java
└── GuestOrderLinkageResponseDto.java
```

### **Admin Controllers with OpenAPI**
```java
@RestController
@RequestMapping("/admin/users")
@Tag(name = "User Administration", description = "Admin APIs for user management")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    // Full OpenAPI documentation required
    // Role-based authorization on all endpoints
    // Audit logging for admin actions
}
```

### **Admin Services (Implementation-First)**
```java
@Service
@Transactional
public class UserAdminService {
    // User management operations
    // Advanced search and filtering
    // Bulk operations for admin efficiency
    // Integration with order-service for analytics
}

@Service
@Transactional
public class GuestOrderLinkingService {
    // Guest order to user account migration
    // Data consistency validation
    // Email notification for linked orders
    // Audit trail for data migration
}
```

### **Authorization & Security**
```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<PagedResponse<UserAdminResponseDto>> getAllUsers() {
    // Method-level security
    // Audit logging for admin operations
    // Rate limiting for bulk operations
}

@PreAuthorize("hasRole('SUPER_ADMIN')")  
public ResponseEntity<ApiResponse<Void>> updateUserRole() {
    // Super admin only operations
    // Extra validation for role changes
    // Notification to affected user
}
```

## 🧪 **Testing Requirements (Implementation-First)**

### **Admin Controller Testing**
```java
@WebMvcTest(UserAdminController.class)
@Import({UserMapper.class})
@WithMockUser(roles = "ADMIN")
class UserAdminControllerTest {
    // Test admin authorization requirements
    // Test pagination and search functionality
    // Test role-based access control
    // Target: 75%+ coverage
}
```

### **Admin Service Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserAdminServiceTest {
    // Test user management operations
    // Test search and filtering logic
    // Test guest order linking functionality
    // Mock order-service integration
    // Target: 80%+ coverage
}
```

### **Security Testing**
```java
@SpringBootTest
@AutoConfigureTestDatabase
class UserAdminSecurityTest {
    // Test role-based authorization
    // Test access denied scenarios
    // Test audit logging functionality
    // Test rate limiting for admin operations
}
```

## 🔗 **Integration Points**

### **Order Service Integration**
- Guest order lookup and linking
- User order history retrieval
- Order analytics for user dashboard

### **Notification Service Integration** (Future)
- Admin action notifications
- User account status change notifications
- Guest order linking confirmations

### **Analytics Service Integration** (Future)
- User behavior analytics
- Registration funnel analysis
- User engagement metrics

## ✅ **Acceptance Criteria**

### **User Management**
- [ ] Complete admin CRUD operations for users
- [ ] Advanced search with multiple criteria working
- [ ] Role-based authorization properly enforced
- [ ] Audit logging for all admin actions

### **Guest Order Linking**
- [ ] Guest orders successfully linked to user accounts
- [ ] Data migration validation and consistency checks
- [ ] Email notifications for successful linking
- [ ] Audit trail for data migration operations

### **Analytics & Reporting**
- [ ] User analytics dashboard data available
- [ ] Activity history tracking working
- [ ] Performance optimized for large datasets
- [ ] Export functionality for admin reports

### **Security & Performance**
- [ ] Proper authorization at method level
- [ ] Rate limiting for bulk operations
- [ ] Audit logging for sensitive operations
- [ ] Performance optimized for admin interfaces

## 🔧 **Advanced Features**

### **User Search Implementation**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    // Custom query methods for admin search
    // Performance optimized with proper indexing
    // Support for complex filtering criteria
}

@Service
public class UserSearchService {
    // Dynamic query building with Criteria API
    // Full-text search capabilities
    // Elasticsearch integration (future)
}
```

### **Guest Order Linking Logic**
```java
@Service
@Transactional
public class GuestOrderLinkingService {
    
    public GuestOrderLinkageResult linkGuestOrders(String email, Long userId) {
        // Find all guest orders by email
        // Validate user ownership
        // Update order records with user ID
        // Send confirmation email
        // Create audit trail
    }
}
```

### **Audit Logging**
```java
@Component
@EventListener
public class UserAdminAuditLogger {
    
    public void logAdminAction(AdminActionEvent event) {
        // Log admin user ID and action
        // Log affected user information
        // Include timestamp and IP address
        // Store in audit log table
    }
}
```

## 📊 **Performance Considerations**

### **Database Optimization**
- Proper indexing for user search queries
- Pagination for large user lists
- Optimized joins for user analytics
- Connection pooling for admin operations

### **Caching Strategy**
- Redis caching for user analytics
- Cache invalidation on user updates
- Optimized search result caching

### **Rate Limiting**
- Admin operation rate limiting
- Bulk operation throttling
- IP-based limiting for admin endpoints

## 📚 **Reference Materials**
- **API Contract**: `docs/beamerparts_api_contract.md` - Admin API patterns
- **Authorization**: `.cursorrules` - Role-based security implementation
- **Search Patterns**: `product-service` advanced search implementation
- **Audit Logging**: Spring Security audit logging patterns

## 🚀 **Getting Started**
1. **Implement** admin controllers with proper authorization
2. **Create** user search and analytics services
3. **Build** guest order linking functionality
4. **Add** comprehensive audit logging
5. **Test** role-based authorization thoroughly
6. **Optimize** for admin interface performance requirements