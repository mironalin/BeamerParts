# OS-M0: Order Service Internal APIs & Service Integration

**Phase**: M0 Basic | **Service**: order-service | **Priority**: Critical | **Estimated Effort**: 4-5 days

## 🎯 **Summary**
Implement internal service-to-service APIs for order coordination, payment processing, and inventory management. These APIs enable seamless integration between order-service and other BeamerParts services for cart conversion, payment coordination, and order fulfillment.

## 📋 **Scope**

### **Internal Endpoints (direct service-to-service `/internal/...`)**

#### **Order Coordination APIs**
- `POST /internal/orders/from-cart` - Create order from user cart (user-service integration)
- `GET /internal/orders/{orderId}` - Get order details for processing
- `PUT /internal/orders/{orderId}/status` - Update order status with validation
- `GET /internal/orders/user/{userId}` - Get user's orders for cross-service queries
- `POST /internal/orders/{orderId}/validate` - Validate order before payment processing

#### **Payment Coordination APIs**
- `POST /internal/orders/{orderId}/payment/prepare` - Prepare order for payment processing
- `PUT /internal/orders/{orderId}/payment/confirm` - Confirm payment success
- `PUT /internal/orders/{orderId}/payment/fail` - Handle payment failure
- `GET /internal/orders/{orderId}/payment/status` - Get payment processing status

#### **Inventory Coordination APIs**
- `POST /internal/orders/{orderId}/inventory/reserve` - Reserve inventory for order
- `POST /internal/orders/{orderId}/inventory/confirm` - Confirm inventory reservation
- `POST /internal/orders/{orderId}/inventory/release` - Release inventory on cancellation
- `GET /internal/orders/{orderId}/inventory/status` - Get inventory reservation status

#### **Fulfillment APIs**
- `POST /internal/orders/{orderId}/fulfill` - Mark order as ready for shipping
- `PUT /internal/orders/{orderId}/shipping/update` - Update shipping information
- `POST /internal/orders/{orderId}/delivery/confirm` - Confirm order delivery

## 🏗️ **Implementation Requirements**

### **Order Coordination Service (TDD Required)**
```java
@Service
@Transactional
public class OrderCoordinationService {
    // Cross-service order orchestration
    // State management coordination
    // Data consistency validation
    // Error handling and rollback
    
    public Order createOrderFromCartInternal(CreateOrderFromCartRequest request) {
        // 1. Validate cart data from user-service
        // 2. Validate product availability with product-service
        // 3. Calculate totals with current pricing
        // 4. Create order in DRAFT status
        // 5. Coordinate inventory reservation
        // 6. Return order ready for payment processing
    }
    
    public OrderStatusResult updateOrderStatusInternal(Long orderId, 
            OrderStatus newStatus, OrderStatusContext context) {
        // 1. Validate order exists and status transition
        // 2. Apply business rules and side effects
        // 3. Coordinate with dependent services
        // 4. Update order with full audit trail
        // 5. Return status update result with coordination info
    }
}
```

### **Payment Coordination Service (TDD Required)**
```java
@Service
@Transactional
public class PaymentCoordinationService {
    // Payment processing orchestration
    // Order status synchronization
    // Refund coordination
    // Payment failure handling
    
    public PaymentPreparationResult prepareOrderForPayment(Long orderId) {
        Order order = validateOrderForPayment(orderId);
        
        // 1. Validate order is in correct status (DRAFT)
        // 2. Confirm inventory reservations are still valid
        // 3. Recalculate totals with current data
        // 4. Prepare payment intent data
        // 5. Set payment timeout and expiration
        // 6. Return payment preparation result
        
        return PaymentPreparationResult.builder()
            .orderId(orderId)
            .amount(order.getTotalAmount())
            .currency("RON")
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .inventoryReserved(true)
            .build();
    }
    
    public Order confirmPaymentSuccess(Long orderId, PaymentConfirmationData payment) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // 1. Validate payment data integrity
        // 2. Transition order to CONFIRMED status
        // 3. Confirm inventory reservations permanently
        // 4. Trigger invoice generation
        // 5. Publish order confirmed event
        // 6. Return confirmed order
        
        order.transitionTo(OrderStatus.CONFIRMED);
        order.setPaymentConfirmedAt(LocalDateTime.now());
        
        // Coordinate with inventory service
        inventoryCoordinationService.confirmReservation(orderId);
        
        // Publish coordination event
        eventPublisher.publishOrderConfirmed(OrderConfirmedEvent.from(order));
        
        return orderRepository.save(order);
    }
    
    public Order handlePaymentFailure(Long orderId, PaymentFailureData failure) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // 1. Record payment failure details
        // 2. Release inventory reservations
        // 3. Determine retry vs cancellation strategy
        // 4. Update order status accordingly
        // 5. Publish payment failure event
        
        inventoryCoordinationService.releaseReservation(orderId);
        
        if (failure.isRetryable()) {
            order.setPaymentRetryCount(order.getPaymentRetryCount() + 1);
            // Keep order in DRAFT for retry
        } else {
            order.transitionTo(OrderStatus.CANCELLED);
        }
        
        return orderRepository.save(order);
    }
}
```

### **Inventory Coordination Service (TDD Required)**
```java
@Service
@Transactional
public class InventoryCoordinationService {
    // Product-service inventory coordination
    // Reservation management
    // Stock validation
    // Inventory conflict resolution
    
    public InventoryReservationResult reserveInventoryForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        List<InventoryReservationItem> reservationItems = order.getOrderItems().stream()
            .map(item -> InventoryReservationItem.builder()
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .reservationTimeoutMinutes(15)
                .orderId(orderId)
                .build())
            .collect(toList());
        
        try {
            // Call product-service for inventory reservation
            InventoryReservationResponse response = productServiceClient
                .reserveInventory(InventoryReservationRequest.builder()
                    .items(reservationItems)
                    .orderId(orderId)
                    .build());
            
            if (response.isSuccessful()) {
                // Store reservation IDs in order items
                updateOrderItemsWithReservationIds(order, response.getReservationIds());
                return InventoryReservationResult.success(response);
            } else {
                return InventoryReservationResult.failure(response.getFailureReasons());
            }
        } catch (InsufficientStockException e) {
            return InventoryReservationResult.failure(List.of(e.getMessage()));
        }
    }
    
    public void confirmReservation(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        List<String> reservationIds = order.getOrderItems().stream()
            .map(OrderItem::getInventoryReservationId)
            .filter(Objects::nonNull)
            .collect(toList());
        
        if (!reservationIds.isEmpty()) {
            productServiceClient.confirmInventoryReservation(
                InventoryConfirmationRequest.builder()
                    .orderId(orderId)
                    .reservationIds(reservationIds)
                    .build());
        }
    }
    
    public void releaseReservation(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        List<String> reservationIds = order.getOrderItems().stream()
            .map(OrderItem::getInventoryReservationId)
            .filter(Objects::nonNull)
            .collect(toList());
        
        if (!reservationIds.isEmpty()) {
            productServiceClient.releaseInventoryReservation(
                InventoryReleaseRequest.builder()
                    .orderId(orderId)
                    .reservationIds(reservationIds)
                    .reason("ORDER_CANCELLED_OR_PAYMENT_FAILED")
                    .build());
        }
    }
}
```

### **Internal DTOs**
```
dto/internal/request/
├── CreateOrderFromCartRequestDto.java
├── OrderStatusUpdateRequestDto.java
├── PaymentPreparationRequestDto.java
├── PaymentConfirmationRequestDto.java
├── InventoryReservationRequestDto.java
└── OrderValidationRequestDto.java

dto/internal/response/
├── OrderInternalDto.java
├── OrderStatusResultDto.java
├── PaymentPreparationResultDto.java
├── InventoryReservationResultDto.java
├── OrderCoordinationResultDto.java
└── OrderValidationResultDto.java
```

### **Internal Controllers**
```java
@RestController
@RequestMapping("/internal/orders")
@Tag(name = "Order Internal APIs", description = "Service-to-service order operations")
public class OrderInternalController {
    
    @PostMapping("/from-cart")
    public ResponseEntity<ApiResponse<OrderInternalDto>> createOrderFromCart(
            @Valid @RequestBody CreateOrderFromCartRequestDto request) {
        
        Order order = orderCoordinationService.createOrderFromCartInternal(request);
        OrderInternalDto response = orderMapper.toInternalDto(order);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderStatusResultDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequestDto request) {
        
        OrderStatusResult result = orderCoordinationService
            .updateOrderStatusInternal(orderId, request.getNewStatus(), request.getContext());
        
        return ResponseEntity.ok(ApiResponse.success(orderMapper.toStatusResultDto(result)));
    }
}

@RestController
@RequestMapping("/internal/orders")
public class PaymentCoordinationController {
    
    @PostMapping("/{orderId}/payment/prepare")
    public ResponseEntity<ApiResponse<PaymentPreparationResultDto>> preparePayment(
            @PathVariable Long orderId) {
        
        PaymentPreparationResult result = paymentCoordinationService
            .prepareOrderForPayment(orderId);
        
        return ResponseEntity.ok(ApiResponse.success(
            paymentMapper.toPreparationResultDto(result)));
    }
    
    @PutMapping("/{orderId}/payment/confirm")
    public ResponseEntity<ApiResponse<OrderInternalDto>> confirmPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentConfirmationRequestDto request) {
        
        Order order = paymentCoordinationService
            .confirmPaymentSuccess(orderId, request.getPaymentData());
        
        return ResponseEntity.ok(ApiResponse.success(orderMapper.toInternalDto(order)));
    }
}
```

## 🧪 **Testing Requirements (Maximum Standards)**

### **Order Coordination Service Testing (TIER 1 - TDD)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderCoordinationServiceTest {
    
    @Autowired
    private OrderCoordinationService coordinationService;
    
    @MockBean
    private ProductServiceClient productServiceClient;
    
    @MockBean
    private UserServiceClient userServiceClient;
    
    @Test
    void createOrderFromCartInternal_WithValidCart_ShouldCreateAndReserveInventory() {
        // Setup valid cart data from user-service
        CreateOrderFromCartRequest request = createValidCartRequest();
        mockValidProductData();
        mockSuccessfulInventoryReservation();
        
        Order order = coordinationService.createOrderFromCartInternal(request);
        
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getOrderItems()).hasSize(2);
        
        // Verify inventory reservation called
        verify(productServiceClient).reserveInventory(any(InventoryReservationRequest.class));
        
        // Verify totals calculated correctly
        assertThat(order.getTotalAmount()).isPositive();
    }
    
    @Test
    void createOrderFromCartInternal_WithInsufficientStock_ShouldThrowAndNotCreateOrder() {
        CreateOrderFromCartRequest request = createValidCartRequest();
        mockValidProductData();
        when(productServiceClient.reserveInventory(any()))
            .thenThrow(new InsufficientStockException("BMW-F30-AC-001 out of stock"));
        
        assertThatThrownBy(() -> coordinationService.createOrderFromCartInternal(request))
            .isInstanceOf(InsufficientStockException.class);
        
        // Verify no order created
        assertThat(orderRepository.count()).isEqualTo(0);
    }
    
    // Target: 90%+ coverage for coordination logic
}
```

### **Payment Coordination Service Testing (TIER 1)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentCoordinationServiceTest {
    
    @Autowired
    private PaymentCoordinationService paymentCoordinationService;
    
    @MockBean
    private InventoryCoordinationService inventoryCoordinationService;
    
    @Test
    void confirmPaymentSuccess_WithValidPayment_ShouldConfirmOrderAndInventory() {
        Order order = createAndSaveDraftOrder();
        PaymentConfirmationData paymentData = createValidPaymentData();
        
        Order confirmedOrder = paymentCoordinationService
            .confirmPaymentSuccess(order.getId(), paymentData);
        
        assertThat(confirmedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmedOrder.getPaymentConfirmedAt()).isNotNull();
        
        // Verify inventory confirmation
        verify(inventoryCoordinationService).confirmReservation(order.getId());
        
        // Verify event published
        verify(eventPublisher).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }
    
    @Test
    void handlePaymentFailure_WithNonRetryableFailure_ShouldCancelOrderAndReleaseInventory() {
        Order order = createAndSaveDraftOrder();
        PaymentFailureData failureData = createNonRetryableFailure();
        
        Order cancelledOrder = paymentCoordinationService
            .handlePaymentFailure(order.getId(), failureData);
        
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        
        // Verify inventory released
        verify(inventoryCoordinationService).releaseReservation(order.getId());
    }
    
    // Target: 90%+ coverage for payment coordination logic
}
```

### **Internal Controller Testing (Implementation-First)**
```java
@WebMvcTest(OrderInternalController.class)
@Import({OrderMapper.class})
class OrderInternalControllerTest {
    
    @MockBean
    private OrderCoordinationService coordinationService;
    
    @Test
    void createOrderFromCart_WithValidRequest_ShouldReturnOrder() throws Exception {
        CreateOrderFromCartRequestDto request = createValidInternalRequest();
        Order order = createMockOrder();
        
        when(coordinationService.createOrderFromCartInternal(any())).thenReturn(order);
        
        mockMvc.perform(post("/internal/orders/from-cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(order.getId()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }
    
    // Target: 75%+ coverage for internal API contracts
}
```

## ✅ **Acceptance Criteria**

### **Service Integration**
- [ ] Complete cart-to-order conversion via internal API
- [ ] Payment coordination with proper status management
- [ ] Inventory reservation and confirmation workflow
- [ ] Cross-service error handling and rollback

### **Internal API Implementation**
- [ ] Order coordination APIs functional and tested
- [ ] Payment coordination with timeout and retry handling
- [ ] Inventory coordination with conflict resolution
- [ ] Fulfillment APIs for shipping and delivery updates

### **Data Consistency**
- [ ] Transactional integrity across service boundaries
- [ ] Proper rollback on coordination failures
- [ ] Event publishing for order state changes
- [ ] Audit trail for all internal operations

### **Testing Standards (Highest)**
- [ ] 90%+ coverage for coordination services
- [ ] 75%+ coverage for internal controllers
- [ ] All coordination scenarios tested with mock services
- [ ] Error handling and rollback scenarios validated

## 📚 **Reference Materials**
- **API Contract**: `docs/beamerparts_api_contract.md` - Order Service Internal APIs
- **Business Logic**: `docs/tickets/order-service/BUSINESS-LOGIC.md`
- **Service Integration**: Cross-service communication patterns
- **Testing Standards**: `.cursorrules` - Internal API testing guidelines

## 🚀 **Getting Started**
1. **Implement coordination services** with comprehensive business logic
2. **Create internal controllers** with proper error handling
3. **Test service integration** with mock external dependencies
4. **Validate coordination flows** for cart-to-order conversion
5. **Test payment coordination** scenarios (success, failure, retry)
6. **Ensure data consistency** across all coordination operations
