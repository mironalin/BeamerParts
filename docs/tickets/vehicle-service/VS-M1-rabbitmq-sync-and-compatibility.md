# VS-M1: Vehicle Service RabbitMQ Sync & Advanced Compatibility

**Phase**: M1 Messaging | **Service**: vehicle-service | **Priority**: Medium | **Estimated Effort**: 3-4 days

## 🎯 **Summary**
Implement event-driven BMW data synchronization with product-service and advanced compatibility validation. Establishes vehicle-service as the authoritative source for BMW data changes across the platform.

## 📋 **Scope**

### **Event Publishing (RabbitMQ)**
```yaml
Events Published:
  - BmwSeriesUpdatedEvent: When BMW series data changes
  - BmwGenerationUpdatedEvent: When generation data changes
  - CompatibilityRuleUpdatedEvent: When compatibility rules change
  - VehicleDataSyncRequestEvent: Trigger sync across services
```

### **Event Listening (RabbitMQ)**
```yaml
Events Consumed:
  - ProductServiceSyncRequestEvent: Manual sync trigger from product-service
  - DataConsistencyCheckEvent: Validate data consistency across services
```

### **Advanced Compatibility APIs**
- `POST /internal/compatibility/bulk-validate` - Bulk compatibility validation for cart/order processing
- `GET /internal/compatibility/{generationCode}/compatible-products` - All compatible products for generation
- `POST /internal/compatibility/year-boundary-check` - Validate year boundary edge cases
- `PUT /internal/compatibility/{generationCode}/rules` - Update compatibility rules

## 🏗️ **Implementation Requirements**

### **Event Publishing Infrastructure**
```java
@Component
public class VehicleEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    
    public void publishBmwSeriesUpdated(BmwSeriesUpdatedEvent event) {
        // Publish to vehicle.series.updated exchange
        // Trigger product-service BMW cache update
        // Include full series data and change timestamp
    }
    
    public void publishBmwGenerationUpdated(BmwGenerationUpdatedEvent event) {
        // Publish to vehicle.generation.updated exchange
        // Include generation data and compatibility changes
        // Trigger dependent service updates
    }
    
    public void publishCompatibilityRuleUpdated(CompatibilityRuleUpdatedEvent event) {
        // Publish compatibility rule changes
        // Include affected products and generations
        // Trigger product compatibility recalculation
    }
}
```

### **Data Synchronization Service**
```java
@Service
@Transactional
public class VehicleDataSyncService {
    // BMW data synchronization with product-service
    // Conflict resolution for data inconsistencies
    // Bulk synchronization operations
    // Data validation and consistency checks
    
    public SyncResult synchronizeWithProductService() {
        // Compare BMW data between services
        // Identify inconsistencies and conflicts
        // Apply resolution strategy (vehicle-service wins)
        // Return sync results and statistics
    }
    
    public ValidationResult validateDataConsistency() {
        // Cross-service data consistency validation
        // Identify orphaned compatibility records
        // Report data quality issues
    }
}
```

### **Advanced Compatibility Domain Service**
```java
@Service
@Transactional
public class AdvancedCompatibilityDomainService {
    // Complex compatibility rule evaluation
    // Year boundary edge case handling
    // Multi-generation compatibility (universal parts)
    // Performance-optimized bulk validation
    
    public BulkCompatibilityResult validateBulkCompatibility(
        List<CompatibilityCheckRequest> requests) {
        // Bulk validation for cart/order processing
        // Optimized database queries
        // Comprehensive compatibility analysis
    }
    
    public YearBoundaryResult validateYearBoundary(
        String generationCode, Integer year) {
        // Handle BMW model year edge cases
        // Mid-year generation changes
        // Transition period compatibility
    }
}
```

### **Event Listeners**
```java
@Component
@RabbitListener
public class VehicleEventListener {
    
    @RabbitListener(queues = "vehicle.product.sync.request")
    public void handleProductServiceSyncRequest(ProductServiceSyncRequestEvent event) {
        // Handle manual sync requests from product-service
        // Perform full BMW data synchronization
        // Return sync results via response event
    }
    
    @RabbitListener(queues = "vehicle.data.consistency.check")
    public void handleDataConsistencyCheck(DataConsistencyCheckEvent event) {
        // Validate cross-service data consistency
        // Report inconsistencies and conflicts
        // Trigger corrective actions if needed
    }
}
```

## 🧪 **Testing Requirements**

### **Event Publishing Testing**
```java
@SpringBootTest
@ActiveProfiles("test")
class VehicleEventPublisherTest {
    @MockBean
    private RabbitTemplate rabbitTemplate;
    
    @Test
    void publishBmwSeriesUpdated_ShouldSendCorrectEvent() {
        // Test BMW series update event publishing
        // Verify event payload structure and routing
        // Test event correlation IDs
    }
    
    @Test
    void publishCompatibilityRuleUpdated_ShouldIncludeAffectedProducts() {
        // Test compatibility rule change events
        // Verify affected product list included
        // Test event timestamp and metadata
    }
}
```

### **Data Synchronization Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class VehicleDataSyncServiceTest {
    @MockBean
    private ProductServiceClient productServiceClient;
    
    @Test
    void synchronizeWithProductService_WithConflicts_ShouldResolveCorrectly() {
        // Test data conflict resolution
        // Vehicle-service should be authoritative
        // Verify sync statistics and results
    }
    
    @Test
    void validateDataConsistency_WithOrphanedRecords_ShouldIdentifyIssues() {
        // Test orphaned compatibility record detection
        // Verify data quality reporting
        // Test consistency validation logic
    }
}
```

### **Advanced Compatibility Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AdvancedCompatibilityDomainServiceTest {
    
    @Test
    void validateBulkCompatibility_WithMixedGenerations_ShouldReturnCorrectResults() {
        // Test bulk compatibility validation
        // Mix of compatible and incompatible items
        // Verify performance with large datasets
    }
    
    @Test
    void validateYearBoundary_2019_F30_vs_G20_ShouldHandleTransition() {
        // Test critical BMW year boundary case
        // F30 ends 2019, G20 starts 2019
        // Verify correct generation selection
    }
}
```

## 🔗 **Integration Points**

### **Product Service Integration**
- BMW cache synchronization events
- Compatibility data updates
- Data consistency validation

### **User Service Integration**
- User vehicle compatibility validation
- BMW data validation for user garage

### **Order Service Integration** (Future)
- Bulk compatibility validation for orders
- Year boundary validation during checkout

## ✅ **Acceptance Criteria**

### **Event-Driven Architecture**
- [ ] BMW data change events published to RabbitMQ
- [ ] Product-service receives and processes BMW update events
- [ ] Event replay capability for system recovery
- [ ] Proper error handling and dead letter queues

### **Data Synchronization**
- [ ] Full BMW data sync with product-service working
- [ ] Conflict resolution strategy implemented (vehicle-service wins)
- [ ] Data consistency validation across services
- [ ] Sync performance optimized for large datasets

### **Advanced Compatibility**
- [ ] Bulk compatibility validation performing efficiently
- [ ] Year boundary edge cases handled correctly
- [ ] Multi-generation compatibility support
- [ ] Complex compatibility rules evaluated accurately

### **Performance & Reliability**
- [ ] Event publishing is asynchronous and non-blocking
- [ ] Sync operations complete within acceptable timeframes
- [ ] Graceful handling of external service failures
- [ ] Comprehensive error reporting and recovery

## 🔧 **RabbitMQ Configuration**

### **Exchange and Queue Setup**
```yaml
Exchanges:
  - vehicle.events (topic exchange)
  - vehicle.sync (topic exchange)
  
Queues:
  - vehicle.series.updated -> product.service
  - vehicle.generation.updated -> product.service
  - vehicle.compatibility.updated -> product.service
  - vehicle.product.sync.request -> vehicle.service
  - vehicle.data.consistency.check -> vehicle.service

Routing Keys:
  - vehicle.series.updated.{seriesCode}
  - vehicle.generation.updated.{generationCode}
  - vehicle.compatibility.updated.{generationCode}
  - vehicle.sync.request.{requestId}
```

### **Event Schema Design**
```java
public class BmwSeriesUpdatedEvent {
    private String seriesCode;
    private BmwSeriesEventData seriesData;
    private LocalDateTime updatedAt;
    private String changeReason;
    private List<String> affectedGenerations;
}

public class BmwGenerationUpdatedEvent {
    private String generationCode;
    private BmwGenerationEventData generationData;
    private LocalDateTime updatedAt;
    private List<String> affectedProducts;
    private CompatibilityChangeType changeType;
}
```

## 📚 **Reference Materials**
- **API Contract**: `docs/beamerparts_api_contract.md` - Vehicle Service M1 section
- **Event Patterns**: `.cursorrules` - RabbitMQ configuration and event handling
- **Sync Patterns**: Product-service sync implementation examples
- **BMW Business Logic**: `docs/tickets/vehicle-service/BUSINESS-LOGIC.md`

## 🚀 **Getting Started**
1. **Configure** RabbitMQ exchanges and queues for BMW events
2. **Implement** event publishing infrastructure with proper error handling
3. **Create** data synchronization service with conflict resolution
4. **Add** advanced compatibility validation with bulk operations
5. **Test** end-to-end sync flows with product-service integration
6. **Validate** year boundary and edge case handling