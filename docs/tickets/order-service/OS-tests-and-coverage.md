# OS-Tests: Order Service Comprehensive Testing & Coverage

**Phase**: Quality Assurance | **Service**: order-service | **Priority**: Critical | **Estimated Effort**: 5-6 days

## 🎯 **Summary**
Implement the most comprehensive test suite in the BeamerParts platform for order-service. This service handles money, legal compliance, and critical business workflows requiring the highest testing standards (90%+ coverage target).

## 📊 **Coverage Targets & Standards (Highest in Platform)**

### **Enterprise Coverage Goals**
- **Overall Service Coverage**: 90%+ (highest standard due to money handling)
- **Critical Domain Services**: 95%+ (checkout, payment, money calculations)
- **Entity Business Logic**: 90%+ (Order, Payment, Invoice with business methods)
- **Controllers**: 80%+ (API contracts and error handling)
- **DTOs/Mappers**: 85%+ (money transformation and validation logic)

### **Strategic TDD Application**
```yaml
TIER 1 (TDD Required - 95%+ Coverage):
  - CheckoutDomainService: Cart conversion, money calculations
  - PaymentProcessingService: Stripe integration, webhook handling
  - OrderDomainService: Order state machine, business rules
  - InvoiceGenerationService: Romanian legal compliance
  - Order Entity: State transitions, money calculations
  - Payment Entity: Payment status, refund calculations

TIER 2 (Implementation-First - 80%+ Coverage):
  - OrderAdminService: Order management, search, analytics
  - Controllers: API contracts, validation, error handling
  - Notification services: Email templates, status updates
```

## 🧪 **Money-Handling Testing Strategy (Critical)**

### **Checkout Domain Service Testing (TIER 1 - TDD)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CheckoutDomainServiceTest {
    
    @Autowired
    private CheckoutDomainService checkoutService;
    
    @MockBean
    private ProductServiceClient productServiceClient;
    
    @MockBean
    private StripeClient stripeClient;
    
    // ===== Money Calculation Tests (Critical) =====
    
    @Test
    void processCheckout_WithRomanianTax_ShouldCalculateCorrectly() {
        // Create cart with BMW parts
        CheckoutRequest request = createCheckoutRequestWithRomanianAddress();
        request.getItems().addAll(List.of(
            createCartItem("BMW-F30-AC-001", 2, new BigDecimal("45.99")),
            createCartItem("BMW-F30-AC-002", 1, new BigDecimal("25.50"))
        ));
        
        CheckoutResult result = checkoutService.processCheckout(request);
        Order order = result.getOrder();
        
        // Verify subtotal calculation
        BigDecimal expectedSubtotal = new BigDecimal("117.48"); // (45.99 * 2) + 25.50
        assertThat(order.getSubtotal()).isEqualTo(expectedSubtotal);
        
        // Verify Romanian VAT (19%)
        BigDecimal expectedVat = expectedSubtotal.multiply(new BigDecimal("0.19"));
        assertThat(order.getTaxAmount()).isEqualTo(expectedVat);
        
        // Verify shipping calculation
        assertThat(order.getShippingAmount()).isEqualTo(new BigDecimal("15.00"));
        
        // Verify total calculation  
        BigDecimal expectedTotal = expectedSubtotal.add(expectedVat).add(new BigDecimal("15.00"));
        assertThat(order.getTotalAmount()).isEqualTo(expectedTotal);
    }
    
    @Test
    void processCheckout_WithCurrencyPrecision_ShouldHandleRonCorrectly() {
        // Test Romanian Lei (RON) currency precision
        CheckoutRequest request = createCheckoutRequest();
        request.getItems().add(createCartItem("BMW-F30-AC-001", 3, new BigDecimal("33.333")));
        
        CheckoutResult result = checkoutService.processCheckout(request);
        
        // Verify RON precision (2 decimal places)
        assertThat(result.getOrder().getSubtotal()).isEqualTo(new BigDecimal("99.99"));
        assertThat(result.getOrder().getTotalAmount().scale()).isEqualTo(2);
    }
    
    @Test
    void processCheckout_WithInventoryReservation_ShouldCoordinateCorrectly() {
        CheckoutRequest request = createValidCheckoutRequest();
        
        // Mock successful inventory reservation
        InventoryReservationResult mockReservation = InventoryReservationResult.success();
        when(productServiceClient.reserveStock(any())).thenReturn(mockReservation);
        
        CheckoutResult result = checkoutService.processCheckout(request);
        
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.DRAFT);
        
        // Verify inventory reservation called with correct items
        ArgumentCaptor<StockReservationRequest> captor = 
            ArgumentCaptor.forClass(StockReservationRequest.class);
        verify(productServiceClient).reserveStock(captor.capture());
        
        StockReservationRequest reservationRequest = captor.getValue();
        assertThat(reservationRequest.getItems()).hasSize(request.getItems().size());
    }
    
    @Test
    void processCheckout_WithInsufficientStock_ShouldReleaseReservationsAndFail() {
        CheckoutRequest request = createValidCheckoutRequest();
        
        // Mock insufficient stock
        when(productServiceClient.reserveStock(any()))
            .thenThrow(new InsufficientStockException("BMW-F30-AC-001 out of stock"));
        
        assertThatThrownBy(() -> checkoutService.processCheckout(request))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("BMW-F30-AC-001 out of stock");
        
        // Verify no order created in database
        assertThat(orderRepository.count()).isEqualTo(0);
    }
    
    // ===== Concurrent Processing Tests =====
    
    @Test
    void processCheckout_ConcurrentRequests_ShouldHandleInventoryRaceConditions() {
        // Test concurrent checkout for same products
        // Ensure only one succeeds when stock is limited
        // Verify proper error handling for failures
    }
    
    // Target: 95%+ coverage with comprehensive money-handling scenarios
}
```

### **Payment Processing Service Testing (TIER 1)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentProcessingServiceTest {
    
    @Autowired
    private PaymentProcessingService paymentService;
    
    @MockBean
    private StripeClient stripeClient;
    
    @MockBean
    private VehicleEventPublisher eventPublisher;
    
    // ===== Payment Success Scenarios =====
    
    @Test
    void processPayment_WithSuccessfulStripePayment_ShouldConfirmOrderAndTriggerInvoice() {
        Order draftOrder = createAndSaveDraftOrder();
        ProcessPaymentRequest request = createValidPaymentRequest(draftOrder.getId());
        
        // Mock successful Stripe payment
        StripePaymentResult successResult = StripePaymentResult.builder()
            .successful(true)
            .paymentIntentId("pi_test_123")
            .chargeId("ch_test_456")
            .amount(draftOrder.getTotalAmount())
            .currency("RON")
            .build();
        
        when(stripeClient.processPayment(any())).thenReturn(successResult);
        
        PaymentResult result = paymentService.processPayment(request);
        
        assertThat(result.isSuccessful()).isTrue();
        
        // Verify order status transition
        Order updatedOrder = orderRepository.findById(draftOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        
        // Verify payment record created
        Payment payment = updatedOrder.getPayment();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getStripePaymentIntentId()).isEqualTo("pi_test_123");
        assertThat(payment.getAmount()).isEqualTo(draftOrder.getTotalAmount());
        
        // Verify inventory confirmation triggered
        verify(productServiceClient).confirmStockReservation(draftOrder.getId());
        
        // Verify order confirmed event published
        verify(eventPublisher).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }
    
    // ===== Payment Failure Scenarios =====
    
    @Test
    void processPayment_WithFailedStripePayment_ShouldReleaseInventoryAndUpdateStatus() {
        Order draftOrder = createAndSaveDraftOrder();
        ProcessPaymentRequest request = createValidPaymentRequest(draftOrder.getId());
        
        // Mock failed Stripe payment
        StripePaymentResult failureResult = StripePaymentResult.builder()
            .successful(false)
            .errorCode("card_declined")
            .errorMessage("Your card was declined.")
            .build();
        
        when(stripeClient.processPayment(any())).thenReturn(failureResult);
        
        PaymentResult result = paymentService.processPayment(request);
        
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage()).contains("card was declined");
        
        // Verify order remains in DRAFT status
        Order updatedOrder = orderRepository.findById(draftOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.DRAFT);
        
        // Verify payment failure recorded
        Payment payment = updatedOrder.getPayment();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).contains("card was declined");
        
        // Verify inventory reservation released
        verify(productServiceClient).releaseStockReservation(draftOrder.getId());
    }
    
    // ===== Webhook Security Tests =====
    
    @Test
    void handleStripeWebhook_WithValidSignature_ShouldProcessPaymentSuccessEvent() {
        Order order = createAndSaveOrderWithPendingPayment();
        
        StripeWebhookEvent event = StripeWebhookEvent.builder()
            .type("payment_intent.succeeded")
            .paymentIntentId(order.getPayment().getStripePaymentIntentId())
            .signature("valid_stripe_signature")
            .timestamp(Instant.now())
            .build();
        
        // Mock webhook signature validation
        when(stripeWebhookValidator.isValidSignature(event)).thenReturn(true);
        
        paymentService.handleStripeWebhook(event);
        
        // Verify payment status updated
        Payment updatedPayment = paymentRepository.findById(order.getPayment().getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        
        // Verify order confirmed
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
    
    @Test
    void handleStripeWebhook_WithInvalidSignature_ShouldThrowSecurityException() {
        StripeWebhookEvent event = createWebhookEventWithInvalidSignature();
        
        when(stripeWebhookValidator.isValidSignature(event)).thenReturn(false);
        
        assertThatThrownBy(() -> paymentService.handleStripeWebhook(event))
            .isInstanceOf(InvalidWebhookSignatureException.class)
            .hasMessageContaining("Invalid webhook signature");
        
        // Verify no payment status changes occurred
        verifyNoInteractions(paymentRepository);
    }
    
    // Target: 95%+ coverage with payment security and money-handling focus
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
    void calculateTotal_WithComplexItems_ShouldReturnCorrectAmount() {
        Order order = Order.builder()
            .subtotal(new BigDecimal("157.48"))
            .taxAmount(new BigDecimal("29.92"))   // 19% VAT
            .shippingAmount(new BigDecimal("15.00"))
            .discountAmount(new BigDecimal("5.00"))
            .build();
        
        BigDecimal total = order.calculateTotal();
        
        // 157.48 + 29.92 + 15.00 - 5.00 = 197.40
        assertThat(total).isEqualTo(new BigDecimal("197.40"));
    }
    
    @Test
    void calculateTotal_WithZeroAmounts_ShouldHandleCorrectly() {
        Order order = Order.builder()
            .subtotal(new BigDecimal("100.00"))
            .taxAmount(BigDecimal.ZERO)
            .shippingAmount(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .build();
        
        BigDecimal total = order.calculateTotal();
        
        assertThat(total).isEqualTo(new BigDecimal("100.00"));
    }
    
    // ===== Order State Machine Tests =====
    
    @Test
    void canTransitionTo_ValidOrderStatusTransitions_ShouldReturnTrue() {
        Order order = createOrderWithStatus(OrderStatus.CONFIRMED);
        
        assertThat(order.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
        assertThat(order.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }
    
    @Test
    void canTransitionTo_InvalidOrderStatusTransitions_ShouldReturnFalse() {
        Order order = createOrderWithStatus(OrderStatus.DRAFT);
        
        assertThat(order.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(order.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
    }
    
    @Test
    void transitionTo_ValidTransition_ShouldUpdateStatusAndTimestamp() {
        Order order = createOrderWithStatus(OrderStatus.CONFIRMED);
        LocalDateTime beforeTransition = LocalDateTime.now();
        
        order.transitionTo(OrderStatus.PROCESSING);
        
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getUpdatedAt()).isAfterOrEqualTo(beforeTransition);
    }
    
    @Test
    void transitionTo_InvalidTransition_ShouldThrowBusinessException() {
        Order order = createOrderWithStatus(OrderStatus.DELIVERED);
        
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.DRAFT))
            .isInstanceOf(InvalidOrderStateTransitionException.class)
            .hasMessageContaining("Cannot transition from DELIVERED to DRAFT");
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
    
    @Test
    void getCustomerIdentifier_ForGuestOrder_ShouldReturnEmail() {
        Order guestOrder = Order.builder()
            .userId(null)
            .guestEmail("guest@example.com")
            .build();
        
        assertThat(guestOrder.getCustomerIdentifier()).isEqualTo("guest@example.com");
    }
    
    @Test
    void getCustomerIdentifier_ForUserOrder_ShouldReturnUserId() {
        Order userOrder = Order.builder()
            .userId(123L)
            .guestEmail(null)
            .build();
        
        assertThat(userOrder.getCustomerIdentifier()).isEqualTo("USER_123");
    }
    
    // Helper methods with timestamp-based unique data
    private Order createAndSaveOrder() {
        Order order = Order.builder()
            .orderNumber("ORDER-" + System.currentTimeMillis())
            .status(OrderStatus.DRAFT)
            .userId(1L)
            .subtotal(new BigDecimal("100.00"))
            .taxAmount(new BigDecimal("19.00"))
            .shippingAmount(new BigDecimal("15.00"))
            .totalAmount(new BigDecimal("134.00"))
            .build();
        
        return orderRepository.saveAndFlush(order);
    }
    
    // Target: 90%+ coverage with money-handling and state machine focus
}
```

## ✅ **Acceptance Criteria**

### **Coverage Achievement (Highest Standards)**
- [ ] Overall service coverage ≥ 90% (highest in platform)
- [ ] Critical domain services ≥ 95% (checkout, payment, money calculations)
- [ ] Entity business logic ≥ 90% (Order, Payment, Invoice)
- [ ] Controllers ≥ 80%
- [ ] All tests pass with `scripts/dev/run-tests.sh`

### **Money-Handling Test Quality**
- [ ] Every money calculation path tested with edge cases
- [ ] Romanian currency (RON) precision validated (2 decimal places)
- [ ] Romanian tax calculations (19% VAT) comprehensively tested
- [ ] Payment security scenarios (webhooks, fraud) validated
- [ ] Concurrent order processing race conditions tested

### **Business Logic Validation**
- [ ] Order state machine transitions fully tested
- [ ] Payment processing workflows (success/failure) covered
- [ ] Inventory coordination scenarios validated
- [ ] Romanian invoice compliance tested
- [ ] Guest vs user order workflows differentiated

## 📚 **Reference Materials**
- **Proven Patterns**: `product-service/src/test/` (92.38% coverage achieved)
- **Business Logic**: `docs/tickets/order-service/BUSINESS-LOGIC.md`
- **Testing Standards**: `.cursorrules` - Highest testing requirements
- **Romanian Compliance**: Legal requirements for VAT and invoicing

## 🚀 **Implementation Approach**
1. **Start with TDD for money-handling**: Checkout and payment services first
2. **Apply highest testing standards**: 95%+ for financial logic
3. **Focus on business scenarios**: Complete order workflows end-to-end
4. **Test security extensively**: Payment webhooks, fraud prevention
5. **Validate Romanian compliance**: Tax calculations, invoice generation
6. **Test concurrent scenarios**: Race conditions, inventory conflicts
7. **Achieve enterprise coverage**: Highest standards in the platform
