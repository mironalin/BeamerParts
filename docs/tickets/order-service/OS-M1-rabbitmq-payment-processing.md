# OS-M1: Order Service RabbitMQ & Payment Processing

**Phase**: M1 Messaging | **Service**: order-service | **Priority**: Critical | **Estimated Effort**: 6-7 days

## 🎯 **Summary**
Implement event-driven architecture for order processing, Stripe payment integration, and comprehensive payment workflows. This establishes the critical payment processing infrastructure with webhook handling, refund processing, and order lifecycle event coordination.

## 📋 **Scope**

### **Event Publishing (RabbitMQ)**
```yaml
Events Published:
  - OrderCreatedEvent: Order creation from cart conversion
  - OrderStatusChangedEvent: Order status transitions with context
  - PaymentInitiatedEvent: Payment processing started
  - PaymentCompletedEvent: Payment successfully processed
  - PaymentFailedEvent: Payment processing failed
  - OrderConfirmedEvent: Order confirmed after successful payment
  - OrderCancelledEvent: Order cancellation with refund coordination
  - InventoryReservationRequestedEvent: Inventory coordination
```

### **Event Listening (RabbitMQ)**
```yaml
Events Consumed:
  - CartUpdatedEvent: From user-service (cart changes affecting pending orders)
  - ProductPriceUpdatedEvent: From product-service (price change impact)
  - ProductDiscontinuedEvent: From product-service (handle discontinued items)
  - InventoryReservationConfirmedEvent: From product-service
  - InventoryReservationFailedEvent: From product-service
  - UserAccountLinkedEvent: From user-service (guest-to-user conversion)
```

### **Stripe Payment Integration**
- Complete Stripe payment intent creation and management
- Webhook handling for payment status updates
- Refund processing (partial and full)
- Payment failure handling and retry logic
- Fraud detection and security validation

## 🏗️ **Implementation Requirements**

### **Payment Processing Service (TDD Required)**
```java
@Service
@Transactional
public class PaymentProcessingService {
    
    private final StripeClient stripeClient;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    
    public PaymentIntent createPaymentIntent(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Validate order status
        if (!order.getStatus().equals(OrderStatus.DRAFT)) {
            throw new InvalidOrderStateException("Order must be in DRAFT status for payment");
        }
        
        // Create Stripe payment intent
        StripePaymentIntentRequest stripeRequest = StripePaymentIntentRequest.builder()
            .amount(convertToStripeCents(order.getTotalAmount()))
            .currency("ron")
            .metadata(Map.of(
                "order_id", order.getId().toString(),
                "order_number", order.getOrderNumber(),
                "customer_email", order.getCustomerEmail()
            ))
            .automaticPaymentMethods(Map.of("enabled", true))
            .build();
        
        StripePaymentIntent stripeIntent = stripeClient.createPaymentIntent(stripeRequest);
        
        // Create local payment record
        Payment payment = Payment.builder()
            .order(order)
            .amount(order.getTotalAmount())
            .currency("RON")
            .status(PaymentStatus.PENDING)
            .stripePaymentIntentId(stripeIntent.getId())
            .createdAt(LocalDateTime.now())
            .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Publish payment initiated event
        eventPublisher.publishPaymentInitiated(PaymentInitiatedEvent.builder()
            .orderId(orderId)
            .paymentId(savedPayment.getId())
            .amount(order.getTotalAmount())
            .currency("RON")
            .stripePaymentIntentId(stripeIntent.getId())
            .build());
        
        return PaymentIntent.builder()
            .id(stripeIntent.getId())
            .clientSecret(stripeIntent.getClientSecret())
            .amount(order.getTotalAmount())
            .currency("RON")
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .build();
    }
    
    public PaymentResult processPaymentSuccess(String paymentIntentId, StripeWebhookEvent event) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentIntentId));
        
        Order order = payment.getOrder();
        
        // Update payment status
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setStripeChargeId(event.getChargeId());
        payment.setStripeTransactionId(event.getTransactionId());
        
        // Transition order to confirmed
        order.transitionTo(OrderStatus.CONFIRMED);
        order.setPaymentConfirmedAt(LocalDateTime.now());
        
        // Save changes
        paymentRepository.save(payment);
        orderRepository.save(order);
        
        // Publish events
        eventPublisher.publishPaymentCompleted(PaymentCompletedEvent.builder()
            .orderId(order.getId())
            .paymentId(payment.getId())
            .amount(payment.getAmount())
            .stripeChargeId(event.getChargeId())
            .completedAt(LocalDateTime.now())
            .build());
        
        eventPublisher.publishOrderConfirmed(OrderConfirmedEvent.builder()
            .orderId(order.getId())
            .orderNumber(order.getOrderNumber())
            .customerId(order.getCustomerIdentifier())
            .amount(order.getTotalAmount())
            .confirmedAt(LocalDateTime.now())
            .build());
        
        return PaymentResult.builder()
            .successful(true)
            .orderId(order.getId())
            .paymentId(payment.getId())
            .orderStatus(OrderStatus.CONFIRMED)
            .build();
    }
    
    public RefundResult processRefund(Long orderId, RefundRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        Payment payment = order.getPayment();
        if (payment == null || !payment.getStatus().equals(PaymentStatus.COMPLETED)) {
            throw new InvalidRefundRequestException("Order has no completed payment to refund");
        }
        
        // Calculate refund amount
        BigDecimal refundAmount = request.isFullRefund() 
            ? payment.getAmount() 
            : request.getAmount();
        
        // Validate refund amount
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new InvalidRefundAmountException("Refund amount cannot exceed payment amount");
        }
        
        // Process refund with Stripe
        StripeRefundRequest stripeRefundRequest = StripeRefundRequest.builder()
            .chargeId(payment.getStripeChargeId())
            .amount(convertToStripeCents(refundAmount))
            .reason(request.getReason())
            .metadata(Map.of(
                "order_id", orderId.toString(),
                "refund_reason", request.getReason()
            ))
            .build();
        
        StripeRefundResponse stripeRefund = stripeClient.createRefund(stripeRefundRequest);
        
        // Create refund record
        Refund refund = Refund.builder()
            .payment(payment)
            .amount(refundAmount)
            .reason(request.getReason())
            .status(RefundStatus.PROCESSING)
            .stripeRefundId(stripeRefund.getId())
            .requestedAt(LocalDateTime.now())
            .build();
        
        refundRepository.save(refund);
        
        // Update order status if full refund
        if (request.isFullRefund()) {
            order.transitionTo(OrderStatus.REFUNDED);
        }
        
        orderRepository.save(order);
        
        return RefundResult.builder()
            .successful(true)
            .refundId(refund.getId())
            .amount(refundAmount)
            .stripeRefundId(stripeRefund.getId())
            .build();
    }
    
    private Long convertToStripeCents(BigDecimal amount) {
        return amount.multiply(new BigDecimal("100")).longValue();
    }
}
```

### **Stripe Webhook Handler**
```java
@Component
public class StripeWebhookHandler {
    
    private final PaymentProcessingService paymentService;
    private final StripeWebhookValidator webhookValidator;
    
    @EventListener
    public void handleStripeWebhook(StripeWebhookEvent event) {
        // Verify webhook signature for security
        if (!webhookValidator.isValidSignature(event)) {
            log.error("Invalid Stripe webhook signature for event: {}", event.getId());
            throw new InvalidWebhookSignatureException("Invalid Stripe webhook signature");
        }
        
        log.info("Processing Stripe webhook event: {} of type: {}", event.getId(), event.getType());
        
        try {
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                case "charge.dispute.created":
                    handleChargeDispute(event);
                    break;
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                default:
                    log.info("Unhandled Stripe webhook event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing Stripe webhook event: {}", event.getId(), e);
            throw new WebhookProcessingException("Failed to process webhook event", e);
        }
    }
    
    private void handlePaymentIntentSucceeded(StripeWebhookEvent event) {
        String paymentIntentId = event.getPaymentIntentId();
        PaymentResult result = paymentService.processPaymentSuccess(paymentIntentId, event);
        
        log.info("Payment succeeded for order: {}, payment: {}", 
            result.getOrderId(), result.getPaymentId());
    }
    
    private void handlePaymentIntentFailed(StripeWebhookEvent event) {
        String paymentIntentId = event.getPaymentIntentId();
        PaymentFailureResult result = paymentService.processPaymentFailure(paymentIntentId, event);
        
        log.warn("Payment failed for order: {}, reason: {}", 
            result.getOrderId(), result.getFailureReason());
        
        // Publish payment failed event
        eventPublisher.publishPaymentFailed(PaymentFailedEvent.builder()
            .orderId(result.getOrderId())
            .paymentIntentId(paymentIntentId)
            .failureReason(result.getFailureReason())
            .isRetryable(result.isRetryable())
            .failedAt(LocalDateTime.now())
            .build());
    }
}
```

### **Order Event Publisher**
```java
@Component
public class OrderEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing order created event for order: {}", event.getOrderId());
        
        rabbitTemplate.convertAndSend(
            "order.events",
            "order.created",
            event,
            message -> {
                message.getMessageProperties().setCorrelationId(event.getOrderId().toString());
                message.getMessageProperties().setTimestamp(new Date());
                return message;
            }
        );
    }
    
    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Publishing order confirmed event for order: {}", event.getOrderId());
        
        rabbitTemplate.convertAndSend(
            "order.events",
            "order.confirmed",
            event,
            message -> {
                message.getMessageProperties().setCorrelationId(event.getOrderId().toString());
                message.getMessageProperties().setTimestamp(new Date());
                return message;
            }
        );
    }
    
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing payment completed event for order: {}", event.getOrderId());
        
        rabbitTemplate.convertAndSend(
            "payment.events",
            "payment.completed",
            event,
            message -> {
                message.getMessageProperties().setCorrelationId(event.getOrderId().toString());
                return message;
            }
        );
    }
    
    public void publishInventoryReservationRequested(InventoryReservationRequestedEvent event) {
        log.info("Publishing inventory reservation request for order: {}", event.getOrderId());
        
        rabbitTemplate.convertAndSend(
            "inventory.events",
            "inventory.reservation.requested",
            event,
            message -> {
                message.getMessageProperties().setCorrelationId(event.getOrderId().toString());
                return message;
            }
        );
    }
}
```

### **Order Event Listener**
```java
@Component
@RabbitListener
public class OrderEventListener {
    
    private final OrderDomainService orderService;
    private final PaymentProcessingService paymentService;
    
    @RabbitListener(queues = "order.inventory.reservation.confirmed")
    public void handleInventoryReservationConfirmed(InventoryReservationConfirmedEvent event) {
        log.info("Inventory reservation confirmed for order: {}", event.getOrderId());
        
        try {
            // Update order with confirmed inventory reservation
            orderService.updateInventoryReservationStatus(event.getOrderId(), 
                InventoryReservationStatus.CONFIRMED);
            
            // Continue with order processing if payment is also ready
            Order order = orderRepository.findById(event.getOrderId()).orElse(null);
            if (order != null && order.isReadyForConfirmation()) {
                orderService.processOrderConfirmation(order.getId());
            }
        } catch (Exception e) {
            log.error("Error handling inventory reservation confirmation for order: {}", 
                event.getOrderId(), e);
            throw new EventProcessingException("Failed to process inventory confirmation", e);
        }
    }
    
    @RabbitListener(queues = "order.inventory.reservation.failed")
    public void handleInventoryReservationFailed(InventoryReservationFailedEvent event) {
        log.warn("Inventory reservation failed for order: {}, reason: {}", 
            event.getOrderId(), event.getFailureReason());
        
        try {
            // Cancel order due to inventory failure
            orderService.cancelOrder(event.getOrderId(), 
                "Inventory reservation failed: " + event.getFailureReason());
            
            // If payment was already processed, initiate refund
            Order order = orderRepository.findById(event.getOrderId()).orElse(null);
            if (order != null && order.hasCompletedPayment()) {
                paymentService.processRefund(event.getOrderId(), 
                    RefundRequest.fullRefund("Inventory unavailable"));
            }
        } catch (Exception e) {
            log.error("Error handling inventory reservation failure for order: {}", 
                event.getOrderId(), e);
            throw new EventProcessingException("Failed to process inventory failure", e);
        }
    }
    
    @RabbitListener(queues = "order.product.price.updated")
    public void handleProductPriceUpdated(ProductPriceUpdatedEvent event) {
        log.info("Product price updated for SKU: {}, new price: {}", 
            event.getProductSku(), event.getNewPrice());
        
        // Find draft orders with this product and update pricing if needed
        List<Order> draftOrders = orderRepository.findDraftOrdersWithProduct(event.getProductSku());
        
        for (Order order : draftOrders) {
            orderService.updateProductPricing(order.getId(), event.getProductSku(), event.getNewPrice());
        }
    }
    
    @RabbitListener(queues = "order.user.account.linked")
    public void handleUserAccountLinked(UserAccountLinkedEvent event) {
        log.info("User account linked: guest email {} to user ID {}", 
            event.getGuestEmail(), event.getUserId());
        
        // Link guest orders to user account
        List<Order> guestOrders = orderRepository.findByGuestEmail(event.getGuestEmail());
        
        for (Order order : guestOrders) {
            order.linkToUserAccount(event.getUserId());
            orderRepository.save(order);
        }
        
        log.info("Linked {} guest orders to user account {}", guestOrders.size(), event.getUserId());
    }
}
```

## 🧪 **Testing Requirements (Maximum Standards)**

### **Payment Processing Service Testing (TIER 1 - TDD)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentProcessingServiceTest {
    
    @Autowired
    private PaymentProcessingService paymentService;
    
    @MockBean
    private StripeClient stripeClient;
    
    @Test
    void createPaymentIntent_WithValidOrder_ShouldCreateStripeIntentAndLocalPayment() {
        Order order = createAndSaveDraftOrder();
        StripePaymentIntent mockStripeIntent = createMockStripeIntent();
        
        when(stripeClient.createPaymentIntent(any())).thenReturn(mockStripeIntent);
        
        PaymentIntent paymentIntent = paymentService.createPaymentIntent(order.getId());
        
        assertThat(paymentIntent.getId()).isEqualTo(mockStripeIntent.getId());
        assertThat(paymentIntent.getAmount()).isEqualTo(order.getTotalAmount());
        assertThat(paymentIntent.getCurrency()).isEqualTo("RON");
        
        // Verify local payment record created
        Payment payment = paymentRepository.findByStripePaymentIntentId(mockStripeIntent.getId())
            .orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getAmount()).isEqualTo(order.getTotalAmount());
        
        // Verify event published
        verify(eventPublisher).publishPaymentInitiated(any(PaymentInitiatedEvent.class));
    }
    
    @Test
    void processPaymentSuccess_WithValidPayment_ShouldConfirmOrderAndPublishEvents() {
        Order order = createAndSaveDraftOrder();
        Payment payment = createAndSavePayment(order);
        StripeWebhookEvent event = createSuccessfulWebhookEvent();
        
        PaymentResult result = paymentService.processPaymentSuccess(
            payment.getStripePaymentIntentId(), event);
        
        assertThat(result.isSuccessful()).isTrue();
        
        // Verify payment updated
        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(updatedPayment.getCompletedAt()).isNotNull();
        
        // Verify order confirmed
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        
        // Verify events published
        verify(eventPublisher).publishPaymentCompleted(any(PaymentCompletedEvent.class));
        verify(eventPublisher).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }
    
    @Test
    void processRefund_WithFullRefund_ShouldCreateRefundAndUpdateOrder() {
        Order order = createAndSaveConfirmedOrder();
        Payment payment = createAndSaveCompletedPayment(order);
        RefundRequest request = RefundRequest.fullRefund("Customer requested refund");
        
        StripeRefundResponse mockRefund = createMockStripeRefund();
        when(stripeClient.createRefund(any())).thenReturn(mockRefund);
        
        RefundResult result = paymentService.processRefund(order.getId(), request);
        
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getAmount()).isEqualTo(payment.getAmount());
        
        // Verify refund record created
        Refund refund = refundRepository.findById(result.getRefundId()).orElseThrow();
        assertThat(refund.getAmount()).isEqualTo(payment.getAmount());
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
        
        // Verify order status updated
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.REFUNDED);
    }
    
    // Target: 95%+ coverage for payment processing logic
}
```

### **Stripe Webhook Handler Testing (TIER 1)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StripeWebhookHandlerTest {
    
    @Autowired
    private StripeWebhookHandler webhookHandler;
    
    @MockBean
    private StripeWebhookValidator webhookValidator;
    
    @MockBean
    private PaymentProcessingService paymentService;
    
    @Test
    void handleStripeWebhook_WithValidPaymentSuccessEvent_ShouldProcessPayment() {
        StripeWebhookEvent event = createPaymentSuccessEvent();
        when(webhookValidator.isValidSignature(event)).thenReturn(true);
        
        PaymentResult mockResult = PaymentResult.success(1L, 1L);
        when(paymentService.processPaymentSuccess(any(), any())).thenReturn(mockResult);
        
        webhookHandler.handleStripeWebhook(event);
        
        verify(paymentService).processPaymentSuccess(event.getPaymentIntentId(), event);
    }
    
    @Test
    void handleStripeWebhook_WithInvalidSignature_ShouldThrowException() {
        StripeWebhookEvent event = createPaymentSuccessEvent();
        when(webhookValidator.isValidSignature(event)).thenReturn(false);
        
        assertThatThrownBy(() -> webhookHandler.handleStripeWebhook(event))
            .isInstanceOf(InvalidWebhookSignatureException.class)
            .hasMessageContaining("Invalid Stripe webhook signature");
        
        verifyNoInteractions(paymentService);
    }
    
    // Target: 90%+ coverage for webhook handling
}
```

### **Event Publishing Testing (TIER 1)**
```java
@SpringBootTest
@ActiveProfiles("test")
class OrderEventPublisherTest {
    
    @Autowired
    private OrderEventPublisher eventPublisher;
    
    @MockBean
    private RabbitTemplate rabbitTemplate;
    
    @Test
    void publishOrderConfirmed_ShouldSendEventToCorrectExchange() {
        OrderConfirmedEvent event = createOrderConfirmedEvent();
        
        eventPublisher.publishOrderConfirmed(event);
        
        verify(rabbitTemplate).convertAndSend(
            eq("order.events"),
            eq("order.confirmed"),
            eq(event),
            any(MessagePostProcessor.class)
        );
    }
    
    // Target: 85%+ coverage for event publishing
}
```

## ✅ **Acceptance Criteria**

### **Payment Processing**
- [ ] Complete Stripe payment intent creation and management
- [ ] Webhook handling with signature verification
- [ ] Payment success and failure processing
- [ ] Refund processing (partial and full)
- [ ] Payment retry logic for recoverable failures

### **Event-Driven Architecture**
- [ ] Order lifecycle events published to RabbitMQ
- [ ] Payment events coordinated across services
- [ ] Inventory reservation events handled properly
- [ ] Product price updates reflected in draft orders
- [ ] User account linking for guest orders

### **Payment Security**
- [ ] Webhook signature validation for security
- [ ] Fraud detection integration
- [ ] Payment data encryption and compliance
- [ ] Audit logging for all payment operations

### **Testing Standards (Maximum)**
- [ ] 95%+ coverage for payment processing service
- [ ] 90%+ coverage for webhook handling
- [ ] 85%+ coverage for event publishing/listening
- [ ] All payment scenarios tested (success, failure, refund)
- [ ] Event flow integration tested end-to-end

## 📚 **Reference Materials**
- **Stripe Documentation**: Payment intents, webhooks, and refunds
- **RabbitMQ Patterns**: Event-driven order processing
- **Business Logic**: `docs/tickets/order-service/BUSINESS-LOGIC.md`
- **Security Standards**: Payment data protection and compliance

## 🚀 **Getting Started**
1. **Implement Stripe integration** with payment intent creation
2. **Create webhook handlers** with signature verification
3. **Build event publishing** for order lifecycle events
4. **Add event listeners** for cross-service coordination
5. **Test payment flows** comprehensively (success, failure, refund)
6. **Validate event-driven** order processing workflows
