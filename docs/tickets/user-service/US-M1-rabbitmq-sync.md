# US-M1: User Service RabbitMQ Messaging & Profile Management

**Phase**: M1 Messaging | **Service**: user-service | **Priority**: Medium | **Estimated Effort**: 3-4 days

## 🎯 **Summary**
Implement event-driven architecture with RabbitMQ for user registration events, profile management with vehicle preferences, and cart synchronization. Adds messaging layer for inter-service communication.

## 📋 **Scope**

### **New External Endpoints (via gateway `/api/...`)**
- `GET /profile` - Get user profile with completion status
- `PUT /profile` - Update user profile information
- `GET /profile/vehicles` - List user's BMW vehicles
- `POST /profile/vehicles` - Add BMW vehicle to user's garage
- `PUT /profile/vehicles/{id}` - Update vehicle information
- `DELETE /profile/vehicles/{id}` - Remove vehicle from user's garage

### **Event Publishing (RabbitMQ)**
```yaml
Events Published:
  - UserRegisteredEvent: When user completes registration
  - UserProfileUpdatedEvent: When profile information changes
  - UserVehicleAddedEvent: When user adds BMW vehicle
  - CartItemAddedEvent: When items added to cart
  - CartItemRemovedEvent: When items removed from cart
```

### **Event Listening (RabbitMQ)**
```yaml
Events Consumed:
  - ProductPriceUpdatedEvent: Update cart item prices
  - ProductDiscontinuedEvent: Remove from carts
  - InventoryLowEvent: Notify users of low stock items
```

## 🏗️ **Implementation Requirements**

### **Profile Management DTOs**
```
dto/external/request/
├── UpdateProfileRequestDto.java
├── AddVehicleRequestDto.java
└── UpdateVehicleRequestDto.java

dto/external/response/
├── UserProfileResponseDto.java
├── UserVehicleResponseDto.java
└── ProfileCompletionResponseDto.java
```

### **Domain Services Enhancement**
```java
@Service
@Transactional
public class UserProfileDomainService {
    // Profile management business logic
    // Vehicle compatibility validation with vehicle-service
    // Profile completion calculation
    // Data validation and business rules
}

@Service
@Transactional  
public class UserVehicleDomainService {
    // BMW vehicle management for users
    // Compatibility validation with vehicle-service
    // Primary vehicle selection logic
    // Vehicle recommendation based on parts
}
```

### **Event Publishing**
```java
@Component
public class UserEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    
    public void publishUserRegistered(UserRegisteredEvent event) {
        // Publish to user.registered exchange
        // Include user ID, email, registration timestamp
        // Trigger welcome email from notification service
    }
    
    public void publishCartItemAdded(CartItemAddedEvent event) {
        // Publish to cart.item.added exchange  
        // Include user ID, product SKU, quantity
        // Enable analytics and recommendations
    }
}
```

### **Event Listeners**
```java
@Component
@RabbitListener
public class UserEventListener {
    
    @RabbitListener(queues = "user.product.price.updated")
    public void handleProductPriceUpdated(ProductPriceUpdatedEvent event) {
        // Update cart item prices
        // Notify users of price changes
        // Recalculate cart totals
    }
    
    @RabbitListener(queues = "user.inventory.low")
    public void handleInventoryLow(InventoryLowEvent event) {
        // Find users with item in cart
        // Send low stock notifications
        // Suggest similar products
    }
}
```

## 🧪 **Testing Requirements**

### **Profile Management Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserProfileDomainServiceTest {
    // Test profile completion calculation
    // Test vehicle addition with BMW validation
    // Test vehicle compatibility checking
    // Mock vehicle-service for BMW validation
    // Target: 85%+ coverage
}
```

### **Event Publishing Testing**
```java
@SpringBootTest
@ActiveProfiles("test")
class UserEventPublisherTest {
    @MockBean
    private RabbitTemplate rabbitTemplate;
    
    // Test event publishing for user registration
    // Test cart event publishing
    // Verify correct exchange and routing key usage
    // Test event payload structure
}
```

### **Event Listening Testing**
```java
@SpringBootTest
@ActiveProfiles("test") 
class UserEventListenerTest {
    // Test product price update handling
    // Test inventory low stock notifications
    // Test event processing and error handling
    // Use TestRabbitTemplate for integration testing
}
```

## 🔗 **Integration Points**

### **Vehicle Service Integration**
- BMW vehicle validation (series, generation, year)
- Compatibility checking for user vehicles
- Vehicle data lookup and validation

### **Product Service Integration**
- Cart item price synchronization
- Product availability notifications
- Compatibility validation for user vehicles

### **Notification Service Integration** (Future)
- Welcome email triggering on user registration
- Low stock notifications for cart items
- Profile completion reminders

## ✅ **Acceptance Criteria**

### **Profile Management**
- [ ] User profile CRUD operations working
- [ ] BMW vehicle management integrated with vehicle-service
- [ ] Profile completion calculation accurate
- [ ] Vehicle compatibility validation working

### **Event-Driven Architecture**
- [ ] RabbitMQ events published for user actions
- [ ] Event listeners processing external events correctly
- [ ] Proper error handling and dead letter queues
- [ ] Event replay capability for system recovery

### **Performance & Reliability**
- [ ] Profile operations complete within 200ms
- [ ] Event publishing is asynchronous and non-blocking
- [ ] Graceful handling of messaging failures
- [ ] Proper event serialization and deserialization

## 🔧 **RabbitMQ Configuration**

### **Exchange and Queue Setup**
```yaml
Exchanges:
  - user.events (topic exchange)
  - cart.events (topic exchange)
  
Queues:
  - user.registered -> notification.service
  - cart.item.added -> analytics.service
  - user.product.price.updated -> user.service
  - user.inventory.low -> user.service

Routing Keys:
  - user.registered.{userId}
  - cart.item.added.{userId}.{productSku}
  - product.price.updated.{productSku}
  - inventory.low.{productSku}
```

### **Event Schema Design**
```java
public class UserRegisteredEvent {
    private String userId;
    private String email;
    private LocalDateTime registeredAt;
    private String referralSource;
    // Immutable event with validation
}

public class CartItemAddedEvent {
    private String userId;
    private String productSku;
    private Integer quantity;
    private LocalDateTime addedAt;
    private BigDecimal price;
}
```

## 📚 **Reference Materials**
- **API Contract**: `docs/beamerparts_api_contract.md` - User Service M1 section
- **Messaging Patterns**: `.cursorrules` - RabbitMQ configuration and event handling
- **Vehicle Integration**: `vehicle-service` API documentation
- **Event Examples**: `product-service` event publishing patterns

## 🚀 **Getting Started**
1. **Configure** RabbitMQ exchanges and queues
2. **Implement** profile management domain services with TDD
3. **Create** event publishing infrastructure
4. **Add** event listeners for external service events
5. **Test** end-to-end event flows with integration tests
6. **Validate** vehicle-service integration for BMW compatibility