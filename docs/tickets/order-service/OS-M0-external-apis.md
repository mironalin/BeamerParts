# OS-M0: Order Service External APIs & Core Domain

**Phase**: M0 Basic | **Service**: order-service | **Priority**: Critical | **Estimated Effort**: 5-6 days

## 🎯 **Summary**
Implement customer-facing order APIs and foundational domain model for order processing. This establishes the critical infrastructure for the most important service handling money, payments, and legal compliance. **Highest testing standards required due to financial implications.**

## 📋 **Scope**

### **External Endpoints (via gateway `/api/orders/...`)**

#### **Customer Order APIs**
- `GET /orders` - Get current user's order history with pagination
- `GET /orders/{orderId}` - Get detailed order information with full item breakdown
- `POST /orders/checkout` - Create order from cart and initiate payment flow
- `GET /orders/{orderId}/status` - Real-time order status tracking
- `POST /orders/{orderId}/cancel` - Cancel order (with refund if applicable)

#### **Guest Order APIs**
- `POST /orders/guest/checkout` - Guest checkout without account registration
- `GET /orders/guest/track/{token}` - Guest order tracking with secure access token
- `POST /orders/guest/send-tracking` - Email tracking link to guest

#### **Order Tracking & Status APIs**
- `GET /orders/{orderId}/track` - Detailed order timeline and status updates
- `GET /orders/{orderId}/timeline` - Complete order history with timestamps
- `GET /orders/{orderId}/shipping` - Shipping information and tracking

### **Core Domain Model (TDD Required)**
```java
@Entity
@Table(name = "orders")
public class Order {
    // Order state machine with business rules
    // Money calculation methods (Romanian tax compliance)
    // Customer identification (user vs guest)
    // Audit trail and timestamp management
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        return OrderStatusTransition.isValidTransition(this.status, newStatus);
    }
    
    public BigDecimal calculateTotal() {
        return subtotal.add(taxAmount).add(shippingAmount).subtract(discountAmount);
    }
    
    public boolean isGuestOrder() {
        return userId == null && guestEmail != null;
    }
}

@Entity
@Table(name = "order_items") 
public class OrderItem {
    // Price preservation at order time
    // Quantity validation and business rules
    // BMW compatibility tracking
    // Product reference with SKU
    
    public BigDecimal getItemTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
    
    public boolean isCompatibleWith(UserVehicle vehicle) {
        // BMW compatibility validation
    }
}
```

## 🏗️ **Implementation Requirements**

### **Order Domain Service (TDD Required)**
```java
@Service
@Transactional
public class OrderDomainService {
    // Core order lifecycle management
    // State transition orchestration
    // Money calculation with Romanian tax compliance
    // Integration with cart, inventory, and payment services
    
    public Order createOrderFromCart(Long userId, CreateOrderRequest request) {
        // 1. Validate user and cart contents
        // 2. Calculate totals with Romanian VAT (19%)
        // 3. Create order in DRAFT status
        // 4. Convert cart items to order items (price preservation)
        // 5. Generate unique order number
        // 6. Return order ready for payment
    }
    
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, String reason) {
        // 1. Validate order exists and status transition is legal
        // 2. Apply business rules for status change
        // 3. Execute side effects (inventory, notifications)
        // 4. Update order with audit trail
        // 5. Publish order status changed event
    }
}
```

### **Order State Machine (Critical Business Logic)**
```java
public enum OrderStatus {
    DRAFT,           // Created from cart, payment pending
    CONFIRMED,       // Payment successful, processing can begin
    PROCESSING,      // Order being prepared for shipment  
    SHIPPED,         // Order dispatched, tracking available
    DELIVERED,       // Order received by customer
    CANCELLED,       // Order cancelled (with refund if paid)
    REFUNDED         // Order refunded (partial or full)
}

@Component
public class OrderStatusTransition {
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
        DRAFT, Set.of(CONFIRMED, CANCELLED),
        CONFIRMED, Set.of(PROCESSING, CANCELLED),
        PROCESSING, Set.of(SHIPPED, CANCELLED),
        SHIPPED, Set.of(DELIVERED, REFUNDED),
        DELIVERED, Set.of(REFUNDED),
        CANCELLED, Set.of(), // Terminal
        REFUNDED, Set.of()   // Terminal
    );
    
    public static boolean isValidTransition(OrderStatus from, OrderStatus to) {
        return VALID_TRANSITIONS.get(from).contains(to);
    }
}
```

### **External DTOs**
```
dto/external/request/
├── CreateOrderRequestDto.java
├── CheckoutRequestDto.java
├── GuestCheckoutRequestDto.java
├── CancelOrderRequestDto.java
└── UpdateShippingRequestDto.java

dto/external/response/
├── OrderResponseDto.java
├── OrderItemResponseDto.java
├── OrderStatusResponseDto.java
├── OrderTimelineResponseDto.java
├── CheckoutResponseDto.java
└── GuestOrderTrackingResponseDto.java
```

### **Controllers with OpenAPI**
```java
@RestController
@RequestMapping("/orders")
@Tag(name = "Order Management", description = "Customer order operations")
public class OrderController {
    
    @Operation(summary = "Create order from cart", 
               description = "Convert user's cart to order and prepare for payment")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponseDto>> checkout(
            @Valid @RequestBody CheckoutRequestDto request,
            Authentication authentication) {
        // Comprehensive checkout workflow
        // Full OpenAPI documentation
        // Proper error handling for payment scenarios
    }
    
    @Operation(summary = "Get user's order history",
               description = "Retrieve paginated list of user's orders")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        // Paginated order history
        // Filtering and sorting options
    }
}

@RestController
@RequestMapping("/orders/guest")  
@Tag(name = "Guest Orders", description = "Guest order operations without account")
public class GuestOrderController {
    
    @Operation(summary = "Guest checkout",
               description = "Create order without user account")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponseDto>> guestCheckout(
            @Valid @RequestBody GuestCheckoutRequestDto request) {
        // Guest order creation
        // Email-based order tracking
    }
}
```

## 🧪 **Testing Requirements (Maximum Standards)**

### **Order Domain Service Testing (TIER 1 - TDD)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderDomainServiceTest {
    
    @Autowired
    private OrderDomainService orderService;
    
    @MockBean
    private UserServiceClient userServiceClient;
    
    @MockBean
    private ProductServiceClient productServiceClient;
    
    // ===== Order Creation Tests =====
    
    @Test
    void createOrderFromCart_WithValidCart_ShouldCreateDraftOrder() {
        // Setup valid cart with BMW parts
        CreateOrderRequest request = createValidOrderRequest();
        mockValidUserAndCart();
        
        Order order = orderService.createOrderFromCart(1L, request);
        
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getOrderNumber()).matches("ORDER-\\d{4}-\\d{6}");
        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getOrderItems()).hasSize(2);
        
        // Verify Romanian tax calculation (19%)
        BigDecimal expectedTax = order.getSubtotal().multiply(new BigDecimal("0.19"));
        assertThat(order.getTaxAmount()).isEqualTo(expectedTax);
    }
    
    @Test
    void createOrderFromCart_WithEmptyCart_ShouldThrowException() {
        CreateOrderRequest request = createEmptyCartRequest();
        
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, request))
            .isInstanceOf(EmptyCartException.class)
            .hasMessageContaining("Cannot create order from empty cart");
    }
    
    // ===== Money Calculation Tests =====
    
    @Test
    void createOrderFromCart_WithRomanianAddress_ShouldCalculateVatCorrectly() {
        CreateOrderRequest request = createOrderWithRomanianAddress();
        
        Order order = orderService.createOrderFromCart(1L, request);
        
        // Verify 19% VAT for Romania
        BigDecimal subtotal = new BigDecimal("100.00");
        BigDecimal expectedVat = new BigDecimal("19.00");
        BigDecimal expectedTotal = new BigDecimal("134.00"); // 100 + 19 + 15 shipping
        
        assertThat(order.getSubtotal()).isEqualTo(subtotal);
        assertThat(order.getTaxAmount()).isEqualTo(expectedVat);
        assertThat(order.getTotalAmount()).isEqualTo(expectedTotal);
    }
    
    // ===== State Transition Tests =====
    
    @Test
    void updateOrderStatus_ValidTransition_ShouldUpdateAndPublishEvent() {
        Order order = createAndSaveOrder(OrderStatus.CONFIRMED);
        
        Order updatedOrder = orderService.updateOrderStatus(order.getId(), 
            OrderStatus.PROCESSING, "Order preparation started");
        
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        
        // Verify event published
        verify(eventPublisher).publishOrderStatusChanged(any(OrderStatusChangedEvent.class));
    }
    
    @Test
    void updateOrderStatus_InvalidTransition_ShouldThrowException() {
        Order order = createAndSaveOrder(OrderStatus.DRAFT);
        
        assertThatThrownBy(() -> orderService.updateOrderStatus(order.getId(), 
            OrderStatus.SHIPPED, "Invalid transition"))
            .isInstanceOf(InvalidOrderStateTransitionException.class)
            .hasMessageContaining("Cannot transition from DRAFT to SHIPPED");
    }
    
    // Helper methods with timestamp-based unique data
    private CreateOrderRequest createValidOrderRequest() {
        return CreateOrderRequest.builder()
            .shippingAddress(createRomanianAddress())
            .billingAddress(createRomanianAddress())
            .cartItems(createValidCartItems())
            .build();
    }
    
    // Target: 90%+ coverage for money-handling critical logic
}
```

### **Order Entity Business Logic Testing (TIER 1)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderTest {
    
    @Autowired
    private OrderRepository orderRepository;
    
    // ===== Money Calculation Tests =====
    
    @Test
    void calculateTotal_WithAllComponents_ShouldReturnCorrectAmount() {
        Order order = Order.builder()
            .subtotal(new BigDecimal("100.00"))
            .taxAmount(new BigDecimal("19.00"))
            .shippingAmount(new BigDecimal("15.00"))
            .discountAmount(new BigDecimal("5.00"))
            .build();
        
        BigDecimal total = order.calculateTotal();
        
        // 100.00 + 19.00 + 15.00 - 5.00 = 129.00
        assertThat(total).isEqualTo(new BigDecimal("129.00"));
    }
    
    // ===== State Machine Tests =====
    
    @Test
    void canTransitionTo_ValidTransitions_ShouldReturnTrue() {
        Order order = createOrderWithStatus(OrderStatus.CONFIRMED);
        
        assertThat(order.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
        assertThat(order.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }
    
    @Test
    void canTransitionTo_InvalidTransitions_ShouldReturnFalse() {
        Order order = createOrderWithStatus(OrderStatus.DRAFT);
        
        assertThat(order.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(order.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
    }
    
    // ===== Business Logic Tests =====
    
    @Test
    void isGuestOrder_WithNullUserId_ShouldReturnTrue() {
        Order guestOrder = Order.builder()
            .userId(null)
            .guestEmail("guest@example.com")
            .build();
        
        assertThat(guestOrder.isGuestOrder()).isTrue();
    }
    
    @Test
    void isGuestOrder_WithUserId_ShouldReturnFalse() {
        Order userOrder = Order.builder()
            .userId(123L)
            .guestEmail(null)
            .build();
        
        assertThat(userOrder.isGuestOrder()).isFalse();
    }
    
    // Target: 90%+ coverage for order business logic
}
```

## ✅ **Acceptance Criteria**

### **Domain Model Foundation**
- [ ] Complete Order and OrderItem entities with business methods
- [ ] Order state machine with comprehensive transition validation
- [ ] Money calculation methods with Romanian tax compliance (19% VAT)
- [ ] Guest vs registered user order differentiation

### **External API Implementation**
- [ ] Customer order history with pagination and filtering
- [ ] Complete checkout workflow (cart → order → payment ready)
- [ ] Guest checkout without account requirement
- [ ] Order tracking with detailed timeline and status updates

### **Business Logic Validation**
- [ ] Order state transitions follow business rules strictly
- [ ] Romanian VAT calculation with 2-decimal precision
- [ ] Cart-to-order conversion preserves pricing at order time
- [ ] Order number generation with unique format

### **Testing Standards (Highest)**
- [ ] 90%+ coverage for domain services (money-handling critical)
- [ ] 90%+ coverage for entity business logic
- [ ] All money calculation paths tested with edge cases
- [ ] Order state machine completely tested
- [ ] `scripts/dev/run-tests.sh` passes with highest standards

## 📚 **Reference Materials**
- **API Contract**: `docs/beamerparts_api_contract.md` - Order Service External APIs
- **Business Logic**: `docs/tickets/order-service/BUSINESS-LOGIC.md`
- **Testing Standards**: `.cursorrules` - Highest testing requirements
- **Romanian Legal**: VAT rates and currency formatting requirements

## 🚀 **Getting Started**
1. **Start with TDD**: Order entity and domain service tests FIRST
2. **Apply highest testing standards**: Every money calculation tested
3. **Focus on state machine**: Order status transitions are critical
4. **Test Romanian compliance**: VAT calculations and currency precision
5. **Create order workflows**: Complete cart-to-order conversion
6. **Validate business rules**: State transitions and money handling
