# VS-M3: Vehicle Service Admin APIs & BMW Data Management

**Phase**: M3 Admin/Advanced | **Service**: vehicle-service | **Priority**: Low | **Estimated Effort**: 2-3 days

## 🎯 **Summary**
Implement comprehensive admin APIs for BMW data management, compatibility rule administration, and vehicle analytics. Provides admin interface for maintaining BMW hierarchy and compatibility data.

## 📋 **Scope**

### **Admin Endpoints (via gateway `/api/admin/...`)**

#### **BMW Series Management APIs**
- `POST /admin/vehicles/series` - Create new BMW series
- `PUT /admin/vehicles/series/{seriesCode}` - Update BMW series information
- `DELETE /admin/vehicles/series/{seriesCode}` - Deactivate BMW series
- `GET /admin/vehicles/series` - List all BMW series with management details
- `GET /admin/vehicles/series/{seriesCode}/analytics` - Series usage analytics

#### **BMW Generation Management APIs**
- `POST /admin/vehicles/generations` - Create new BMW generation
- `PUT /admin/vehicles/generations/{generationCode}` - Update generation information
- `DELETE /admin/vehicles/generations/{generationCode}` - Deactivate generation
- `GET /admin/vehicles/generations` - List all generations with filters
- `PUT /admin/vehicles/generations/{generationCode}/compatibility` - Update compatibility rules

#### **Compatibility Management APIs**
- `GET /admin/vehicles/compatibility/registry` - View compatibility registry
- `POST /admin/vehicles/compatibility/bulk-update` - Bulk compatibility updates
- `GET /admin/vehicles/compatibility/orphaned` - Find orphaned compatibility records
- `POST /admin/vehicles/compatibility/validate-all` - Validate all compatibility data

#### **Data Management APIs**
- `POST /admin/vehicles/sync/trigger` - Trigger manual sync with product-service
- `GET /admin/vehicles/sync/status` - View sync status and history
- `POST /admin/vehicles/data/import` - Import BMW data from external sources
- `GET /admin/vehicles/data/export` - Export BMW data for backup

## 🏗️ **Implementation Requirements**

### **Admin DTOs**
```
dto/admin/request/
├── CreateBmwSeriesRequestDto.java
├── UpdateBmwSeriesRequestDto.java
├── CreateBmwGenerationRequestDto.java
├── UpdateBmwGenerationRequestDto.java
├── BulkCompatibilityUpdateRequestDto.java
└── BmwDataImportRequestDto.java

dto/admin/response/
├── BmwSeriesAdminResponseDto.java
├── BmwGenerationAdminResponseDto.java
├── CompatibilityRegistryResponseDto.java
├── DataSyncStatusResponseDto.java
└── BmwAnalyticsResponseDto.java
```

### **Admin Controllers with OpenAPI**
```java
@RestController
@RequestMapping("/admin/vehicles")
@Tag(name = "BMW Vehicle Administration", description = "Admin APIs for BMW data management")
@PreAuthorize("hasRole('ADMIN')")
public class VehicleAdminController {
    // Full OpenAPI documentation required
    // Role-based authorization on all endpoints
    // Audit logging for BMW data changes
}

@RestController
@RequestMapping("/admin/vehicles/compatibility")
@Tag(name = "Compatibility Administration", description = "Admin APIs for compatibility management")
@PreAuthorize("hasRole('ADMIN')")
public class CompatibilityAdminController {
    // Compatibility rule management
    // Bulk operations for efficiency
    // Data validation and consistency checks
}
```

### **Admin Services (Implementation-First)**
```java
@Service
@Transactional
public class VehicleAdminService {
    // BMW series and generation management
    // Data validation and business rules
    // Integration with sync services
    // Audit trail for admin changes
    
    public BmwSeries createBmwSeries(CreateBmwSeriesRequestDto request) {
        // Validate BMW series data
        // Check for duplicates and conflicts
        // Create series with proper defaults
        // Trigger sync event to product-service
    }
    
    public BmwGeneration createBmwGeneration(CreateBmwGenerationRequestDto request) {
        // Validate generation data and year ranges
        // Check for overlapping generations
        // Validate body codes and compatibility
        // Create generation and update compatibility registry
    }
}

@Service
@Transactional
public class CompatibilityAdminService {
    // Compatibility rule management
    // Bulk operations for admin efficiency
    // Data consistency validation
    // Orphaned record cleanup
    
    public BulkUpdateResult updateCompatibilityRules(
        BulkCompatibilityUpdateRequestDto request) {
        // Validate compatibility rule changes
        // Apply bulk updates with transaction safety
        // Trigger product-service cache updates
        // Return update statistics and errors
    }
}

@Service
@Transactional
public class VehicleDataManagementService {
    // BMW data import/export operations
    // Manual sync triggering and monitoring
    // Data validation and cleanup
    // Backup and restore operations
}
```

### **Authorization & Security**
```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<BmwSeries>> createBmwSeries() {
    // Method-level security for BMW data changes
    // Audit logging for all admin operations
    // Rate limiting for bulk operations
}

@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<ApiResponse<Void>> triggerFullDataSync() {
    // Super admin only for critical operations
    // Extra validation and confirmation required
    // System-wide impact operations
}
```

## 🧪 **Testing Requirements (Implementation-First)**

### **Admin Controller Testing**
```java
@WebMvcTest(VehicleAdminController.class)
@Import({VehicleMapper.class})
@WithMockUser(roles = "ADMIN")
class VehicleAdminControllerTest {
    // Test admin authorization requirements
    // Test BMW data validation and creation
    // Test role-based access control
    // Target: 75%+ coverage
}
```

### **Admin Service Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class VehicleAdminServiceTest {
    // Test BMW series/generation management
    // Test data validation and business rules
    // Test integration with sync services
    // Mock product-service for sync operations
    // Target: 80%+ coverage
}
```

### **Security Testing**
```java
@SpringBootTest
@AutoConfigureTestDatabase
class VehicleAdminSecurityTest {
    // Test role-based authorization
    // Test access denied scenarios for non-admin users
    // Test audit logging functionality
    // Test rate limiting for admin operations
}
```

## 🔗 **Integration Points**

### **Product Service Integration**
- Trigger BMW cache updates after admin changes
- Validate compatibility data consistency
- Monitor sync status and resolve conflicts

### **User Service Integration**
- Validate user vehicle data against admin changes
- Handle orphaned user vehicle references
- Update user compatibility data

### **Analytics Service Integration** (Future)
- BMW data usage analytics
- Popular generation and series tracking
- Compatibility rule effectiveness metrics

## ✅ **Acceptance Criteria**

### **BMW Data Management**
- [ ] Complete admin CRUD operations for BMW series/generations
- [ ] Data validation preventing invalid BMW configurations
- [ ] Year range validation preventing overlapping generations
- [ ] Body code validation ensuring proper BMW structure

### **Compatibility Administration**
- [ ] Bulk compatibility rule updates working efficiently
- [ ] Orphaned compatibility record detection and cleanup
- [ ] Data consistency validation across services
- [ ] Performance optimization for large compatibility datasets

### **Data Synchronization Management**
- [ ] Manual sync triggering with product-service
- [ ] Sync status monitoring and history tracking
- [ ] Conflict resolution interface for admin users
- [ ] Data import/export functionality for backup/restore

### **Security & Auditing**
- [ ] Proper authorization at method level
- [ ] Comprehensive audit logging for all BMW data changes
- [ ] Rate limiting for bulk administrative operations
- [ ] Data integrity validation before and after changes

## 🔧 **Advanced Features**

### **BMW Data Validation**
```java
@Service
public class BmwDataValidationService {
    
    public ValidationResult validateBmwSeries(BmwSeries series) {
        // Validate BMW series naming conventions
        // Check for series code conflicts
        // Validate display order and hierarchy
        // Return comprehensive validation results
    }
    
    public ValidationResult validateBmwGeneration(BmwGeneration generation) {
        // Validate year ranges (start < end, no overlaps)
        // Validate body codes against BMW standards
        // Check for generation naming consistency
        // Validate against existing series data
    }
}
```

### **Bulk Operations Implementation**
```java
@Service
@Transactional
public class BulkOperationsService {
    
    public BulkUpdateResult performBulkCompatibilityUpdate(
        List<CompatibilityUpdateRequest> requests) {
        
        List<String> successfulUpdates = new ArrayList<>();
        List<BulkUpdateError> errors = new ArrayList<>();
        
        for (CompatibilityUpdateRequest request : requests) {
            try {
                // Validate individual update
                // Apply update with proper error handling
                // Track success/failure statistics
                successfulUpdates.add(request.getGenerationCode());
            } catch (Exception e) {
                errors.add(new BulkUpdateError(request, e.getMessage()));
            }
        }
        
        return BulkUpdateResult.builder()
            .totalRequests(requests.size())
            .successfulUpdates(successfulUpdates.size())
            .errors(errors)
            .build();
    }
}
```

### **Data Import/Export**
```java
@Service
public class VehicleDataImportExportService {
    
    public ImportResult importBmwData(BmwDataImportRequestDto request) {
        // Validate import data format and structure
        // Check for conflicts with existing data
        // Perform transactional import with rollback capability
        // Generate import report with statistics
    }
    
    public ExportResult exportBmwData(BmwDataExportRequestDto request) {
        // Export BMW data in specified format (JSON, CSV)
        // Include compatibility data if requested
        // Generate downloadable export file
        // Track export operations for audit
    }
}
```

## 📊 **Performance Considerations**

### **Database Optimization**
- Proper indexing for admin search and filtering operations
- Optimized queries for bulk operations
- Connection pooling for admin interface
- Read replicas for analytics and reporting

### **Caching Strategy**
- Redis caching for BMW hierarchy data
- Cache invalidation on admin updates
- Optimized compatibility registry caching

### **Rate Limiting**
- Admin operation rate limiting to prevent abuse
- Bulk operation throttling for system stability
- IP-based limiting for admin endpoints

## 📚 **Reference Materials**
- **API Contract**: `docs/beamerparts_api_contract.md` - Admin API patterns
- **Authorization**: `.cursorrules` - Role-based security implementation
- **BMW Business Logic**: `docs/tickets/vehicle-service/BUSINESS-LOGIC.md`
- **Audit Logging**: Spring Security audit logging patterns

## 🚀 **Getting Started**
1. **Implement** admin controllers with proper authorization
2. **Create** BMW data management services with validation
3. **Build** compatibility administration functionality
4. **Add** data import/export capabilities
5. **Test** role-based authorization and bulk operations
6. **Optimize** for admin interface performance requirements