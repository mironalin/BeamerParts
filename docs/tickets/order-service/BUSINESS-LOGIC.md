# Order Service Business Logic Documentation

## 🎯 **Service Purpose**
Order Service handles the complete order processing lifecycle from cart conversion to delivery, including payment processing, inventory coordination, and Romanian legal compliance. **This is the most critical service handling money and legal requirements.**

## 🏗️ **Core Domain Concepts**

### **Order Lifecycle**
```
Cart → Order Creation → Payment → Fulfillment → Delivery → Completion
  ↓         ↓            ↓          ↓           ↓          ↓
DRAFT → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → CLOSED
  ↓                               
CANCELLED (with potential refund)
```

### **Key Business Entities**

#### **Order**
- Central entity representing customer purchase
- Contains order items, totals, status, and customer information
- Manages state transitions with business rule validation
- Links to payment, invoice, and shipping information

#### **OrderItem**
- Individual products within an order
- Preserves price at time of purchase (immutable pricing)
- Contains quantity, BMW compatibility validation
- Links to product SKU for inventory coordination

#### **Payment**
- Payment processing and status tracking
- Stripe integration for payment processing
- Refund calculation and processing logic
- Security audit trail and fraud prevention

#### **Invoice**
- Romanian legal compliance for invoicing
- Sequential numbering required by law
- VAT calculation and breakdown
- PDF generation with professional layout

## 🧠 **Critical Business Logic**

### **Order State Machine**
**Purpose**: Enforce valid order status transitions and business rules.

**Valid State Transitions**:
```
DRAFT → [CONFIRMED, CANCELLED]
CONFIRMED → [PROCESSING, CANCELLED]  
PROCESSING → [SHIPPED, CANCELLED]
SHIPPED → [DELIVERED, REFUNDED]
DELIVERED → [REFUNDED]
CANCELLED → [] (terminal)
REFUNDED → [] (terminal)
```

**Business Rules**:
```java
public boolean canTransitionTo(OrderStatus newStatus) {
    return OrderStatusTransition.isValidTransition(this.status, newStatus);
}

public void transitionTo(OrderStatus newStatus) {
    if (!canTransitionTo(newStatus)) {
        throw new InvalidOrderStateTransitionException(
            String.format("Cannot transition from %s to %s", status, newStatus));
    }
    
    // Log state transition for audit
    auditLogger.logStatusTransition(this.id, this.status, newStatus);
    
    this.status = newStatus;
    this.updatedAt = LocalDateTime.now();
    
    // Trigger side effects based on new status
    handleStatusTransitionSideEffects(newStatus);
}

private void handleStatusTransitionSideEffects(OrderStatus newStatus) {
    switch (newStatus) {
        case CONFIRMED:
            // Confirm inventory reservations
            inventoryService.confirmReservations(this.id);
            // Generate invoice
            invoiceService.generateInvoice(this.id);
            // Send confirmation email
            emailService.sendOrderConfirmation(this.id);
            break;
        case CANCELLED:
            // Release inventory reservations
            inventoryService.releaseReservations(this.id);
            // Process refund if payment was made
            if (hasPayment() && payment.isCompleted()) {
                paymentService.processRefund(this.id);
            }
            break;
        // ... other status transitions
    }
}
```

### **Money Calculation Logic**
**Problem**: Accurate money calculations for Romanian market with VAT compliance.

**Romanian Tax Requirements**:
- **VAT Rate**: 19% for most products
- **Currency**: Romanian Lei (RON) with 2 decimal precision
- **Rounding**: Standard rounding rules for currency

**Calculation Logic**:
```java
public class OrderTotalCalculator {
    
    private static final BigDecimal ROMANIAN_VAT_RATE = new BigDecimal("0.19");
    private static final BigDecimal STANDARD_SHIPPING_RATE = new BigDecimal("15.00");
    
    public OrderTotals calculateTotals(List<OrderItem> items, Address shippingAddress) {
        // 1. Calculate subtotal from items
        BigDecimal subtotal = items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 2. Calculate shipping based on address and weight
        BigDecimal shippingAmount = calculateShipping(items, shippingAddress);
        
        // 3. Calculate Romanian VAT (19%)
        BigDecimal taxAmount = subtotal.multiply(ROMANIAN_VAT_RATE)
            .setScale(2, RoundingMode.HALF_UP);
        
        // 4. Apply discounts if any
        BigDecimal discountAmount = calculateDiscounts(subtotal);
        
        // 5. Calculate final total
        BigDecimal totalAmount = subtotal
            .add(taxAmount)
            .add(shippingAmount)
            .subtract(discountAmount)
            .setScale(2, RoundingMode.HALF_UP);
        
        return OrderTotals.builder()
            .subtotal(subtotal)
            .taxAmount(taxAmount)
            .shippingAmount(shippingAmount)
            .discountAmount(discountAmount)
            .totalAmount(totalAmount)
            .build();
    }
    
    private BigDecimal calculateShipping(List<OrderItem> items, Address address) {
        // Basic shipping calculation - can be enhanced with weight/size
        if (isEligibleForFreeShipping(items)) {
            return BigDecimal.ZERO;
        }
        
        // Different rates for different regions in Romania
        if (address.getCounty().equals("București")) {
            return new BigDecimal("10.00"); // Reduced rate for Bucharest
        }
        
        return STANDARD_SHIPPING_RATE;
    }
    
    private boolean isEligibleForFreeShipping(List<OrderItem> items) {
        BigDecimal subtotal = calculateSubtotal(items);
        return subtotal.compareTo(new BigDecimal("200.00")) >= 0; // Free shipping over 200 RON
    }
}
```

### **Payment Processing Logic**
**Purpose**: Secure payment processing with Stripe integration and fraud prevention.

**Payment Flow**:
1. Create payment intent with Stripe
2. Customer completes payment on frontend
3. Stripe webhook confirms payment
4. Update order status and confirm inventory
5. Generate invoice and send confirmation

**Implementation**:
```java
public class PaymentProcessingService {
    
    public PaymentIntent createPaymentIntent(Order order) {
        // Validate order is in correct status for payment
        if (!order.getStatus().equals(OrderStatus.DRAFT)) {
            throw new InvalidOrderStateException("Order must be in DRAFT status for payment");
        }
        
        // Create Stripe payment intent
        StripePaymentIntentRequest stripeRequest = StripePaymentIntentRequest.builder()
            .amount(convertToStripeCents(order.getTotalAmount()))
            .currency("ron")
            .metadata(Map.of(
                "order_id", order.getId().toString(),
                "order_number", order.getOrderNumber()
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
            .build();
        
        paymentRepository.save(payment);
        
        return PaymentIntent.builder()
            .id(stripeIntent.getId())
            .clientSecret(stripeIntent.getClientSecret())
            .amount(order.getTotalAmount())
            .expiresAt(LocalDateTime.now().plusMinutes(15)) // 15 minute timeout
            .build();
    }
    
    @EventListener
    public void handleStripeWebhook(StripeWebhookEvent event) {
        // Security: Verify webhook signature
        if (!stripeWebhookValidator.isValidSignature(event)) {
            throw new InvalidWebhookSignatureException("Invalid Stripe webhook signature");
        }
        
        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentSuccess(event);
                break;
            case "payment_intent.payment_failed":
                handlePaymentFailure(event);
                break;
            case "charge.dispute.created":
                handleChargeback(event);
                break;
        }
    }
    
    private void handlePaymentSuccess(StripeWebhookEvent event) {
        String paymentIntentId = event.getPaymentIntentId();
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentIntentId));
        
        // Update payment status
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setStripeChargeId(event.getChargeId());
        
        // Transition order to confirmed
        Order order = payment.getOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        
        // Save changes
        paymentRepository.save(payment);
        orderRepository.save(order);
        
        // Publish order confirmed event
        eventPublisher.publishOrderConfirmed(OrderConfirmedEvent.from(order));
    }
    
    private Long convertToStripeCents(BigDecimal amount) {
        // Stripe expects amounts in smallest currency unit (bani for RON)
        return amount.multiply(new BigDecimal("100")).longValue();
    }
}
```

### **Inventory Coordination Logic**
**Purpose**: Coordinate with product-service for stock reservations and management.

**Coordination Flow**:
1. Reserve stock during order creation
2. Confirm reservations on payment success
3. Release reservations on order cancellation
4. Handle partial stock availability

**Implementation**:
```java
public class InventoryCoordinationService {
    
    public InventoryReservationResult reserveOrderInventory(Order order) {
        List<StockReservationItem> reservationItems = order.getOrderItems().stream()
            .map(item -> StockReservationItem.builder()
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .reservationTimeoutMinutes(15) // 15 minute reservation
                .build())
            .collect(toList());
        
        StockReservationRequest request = StockReservationRequest.builder()
            .orderId(order.getId())
            .items(reservationItems)
            .build();
        
        try {
            StockReservationResponse response = productServiceClient.reserveStock(request);
            
            if (response.isSuccessful()) {
                // Store reservation IDs for later confirmation/release
                order.getOrderItems().forEach(item -> {
                    String reservationId = response.getReservationId(item.getProductSku());
                    item.setInventoryReservationId(reservationId);
                });
                
                return InventoryReservationResult.success(response.getReservationIds());
            } else {
                return InventoryReservationResult.failure(response.getFailures());
            }
        } catch (InsufficientStockException e) {
            return InventoryReservationResult.failure(List.of(e.getMessage()));
        }
    }
    
    public void confirmInventoryReservation(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        List<String> reservationIds = order.getOrderItems().stream()
            .map(OrderItem::getInventoryReservationId)
            .filter(Objects::nonNull)
            .collect(toList());
        
        if (!reservationIds.isEmpty()) {
            StockConfirmationRequest request = StockConfirmationRequest.builder()
                .orderId(orderId)
                .reservationIds(reservationIds)
                .build();
            
            productServiceClient.confirmStockReservation(request);
        }
    }
    
    public void releaseInventoryReservation(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        List<String> reservationIds = order.getOrderItems().stream()
            .map(OrderItem::getInventoryReservationId)
            .filter(Objects::nonNull)
            .collect(toList());
        
        if (!reservationIds.isEmpty()) {
            StockReleaseRequest request = StockReleaseRequest.builder()
                .orderId(orderId)
                .reservationIds(reservationIds)
                .reason("ORDER_CANCELLED")
                .build();
            
            productServiceClient.releaseStockReservation(request);
        }
    }
}
```

## 📄 **Romanian Invoice Compliance**

### **Legal Requirements**
- **Sequential numbering**: Invoices must be numbered sequentially
- **VAT registration**: Company VAT number must be displayed
- **Customer information**: Complete billing address required
- **Product details**: SKU, description, quantity, unit price, total
- **VAT breakdown**: Separate VAT amount and rate display

### **Invoice Generation Logic**
```java
public class InvoiceGenerationService {
    
    public Invoice generateInvoice(Order order) {
        // Validate order status
        if (!order.getStatus().equals(OrderStatus.CONFIRMED)) {
            throw new InvalidOrderStateException("Cannot generate invoice for unconfirmed order");
        }
        
        // Generate sequential invoice number (Romanian requirement)
        String invoiceNumber = generateSequentialInvoiceNumber();
        
        // Get company legal information
        CompanyInfo companyInfo = getCompanyLegalInfo();
        
        // Get customer information
        CustomerInfo customerInfo = getCustomerInfo(order);
        
        // Calculate Romanian tax breakdown
        RomanianTaxBreakdown taxBreakdown = calculateTaxBreakdown(order);
        
        // Create invoice entity
        Invoice invoice = Invoice.builder()
            .order(order)
            .invoiceNumber(invoiceNumber)
            .issueDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(30))
            .companyInfo(companyInfo)
            .customerInfo(customerInfo)
            .subtotal(order.getSubtotal())
            .vatAmount(taxBreakdown.getVatAmount())
            .vatRate(taxBreakdown.getVatRate())
            .totalAmount(order.getTotalAmount())
            .build();
        
        // Generate PDF content
        byte[] pdfContent = generateInvoicePdf(invoice);
        invoice.setPdfContent(pdfContent);
        
        // Save invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        // Link invoice to order
        order.setInvoice(savedInvoice);
        orderRepository.save(order);
        
        return savedInvoice;
    }
    
    private String generateSequentialInvoiceNumber() {
        // Romanian law requires sequential numbering within calendar year
        int currentYear = LocalDate.now().getYear();
        
        // Get next number for current year
        Long nextNumber = invoiceRepository.getNextInvoiceNumberForYear(currentYear);
        
        // Format: YYYY-NNNNNN (e.g., 2024-000001)
        return String.format("%d-%06d", currentYear, nextNumber);
    }
    
    private RomanianTaxBreakdown calculateTaxBreakdown(Order order) {
        // Standard VAT rate in Romania is 19%
        BigDecimal vatRate = new BigDecimal("0.19");
        BigDecimal vatAmount = order.getSubtotal()
            .multiply(vatRate)
            .setScale(2, RoundingMode.HALF_UP);
        
        return RomanianTaxBreakdown.builder()
            .vatRate(vatRate)
            .vatAmount(vatAmount)
            .taxableAmount(order.getSubtotal())
            .build();
    }
    
    private byte[] generateInvoicePdf(Invoice invoice) {
        // Generate professional PDF using iText
        // Include company logo and branding
        // Romanian language content
        // Proper VAT breakdown display
        // QR code for digital verification (optional)
        
        return pdfGenerator.generateInvoicePdf(invoice);
    }
}
```

## 🔄 **Event-Driven Architecture**

### **Events Published**
```java
// Order lifecycle events
public class OrderCreatedEvent {
    private Long orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String customerIdentifier;
    private LocalDateTime createdAt;
}

public class OrderConfirmedEvent {
    private Long orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private List<OrderItemEvent> items;
    private LocalDateTime confirmedAt;
}

public class PaymentCompletedEvent {
    private Long orderId;
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime completedAt;
}
```

### **Events Consumed**
```java
// From product-service
@EventListener
public void handleProductPriceUpdated(ProductPriceUpdatedEvent event) {
    // Update draft orders with new pricing
    // Notify customers of price changes
}

// From user-service
@EventListener  
public void handleUserAccountCreated(UserAccountCreatedEvent event) {
    // Link guest orders to new user account
    // Migrate order history
}
```

## 🚨 **Error Handling & Recovery**

### **Payment Failure Recovery**
```java
public class PaymentRecoveryService {
    
    public void handlePaymentFailure(Order order, PaymentFailureReason reason) {
        switch (reason) {
            case INSUFFICIENT_FUNDS:
                // Send payment retry notification
                emailService.sendPaymentRetryEmail(order);
                // Keep inventory reserved for 24 hours
                inventoryService.extendReservation(order.getId(), Duration.ofHours(24));
                break;
                
            case CARD_DECLINED:
                // Release inventory immediately
                inventoryService.releaseReservation(order.getId());
                // Suggest alternative payment method
                emailService.sendAlternativePaymentEmail(order);
                break;
                
            case FRAUD_SUSPECTED:
                // Flag order for manual review
                order.setStatus(OrderStatus.UNDER_REVIEW);
                // Notify security team
                securityService.flagSuspiciousOrder(order);
                break;
        }
    }
}
```

### **Inventory Conflict Resolution**
```java
public class InventoryConflictResolver {
    
    public void handleStockShortage(Order order, List<String> unavailableSkus) {
        // Partial fulfillment option
        if (order.allowsPartialFulfillment()) {
            // Remove unavailable items
            order.removeItems(unavailableSkus);
            // Recalculate totals
            order.recalculateTotals();
            // Process partial refund
            paymentService.processPartialRefund(order, unavailableSkus);
        } else {
            // Cancel entire order
            order.transitionTo(OrderStatus.CANCELLED);
            // Full refund
            paymentService.processFullRefund(order);
        }
        
        // Notify customer
        emailService.sendStockShortageNotification(order, unavailableSkus);
    }
}
```

## 📊 **Analytics & Reporting Logic**

### **Order Analytics**
```java
public class OrderAnalyticsService {
    
    public OrderAnalyticsReport generateReport(AnalyticsRequest request) {
        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();
        
        // Key metrics calculation
        OrderMetrics metrics = OrderMetrics.builder()
            .totalOrders(orderRepository.countByDateRange(startDate, endDate))
            .totalRevenue(orderRepository.sumRevenueByDateRange(startDate, endDate))
            .averageOrderValue(calculateAverageOrderValue(startDate, endDate))
            .conversionRate(calculateConversionRate(startDate, endDate))
            .build();
        
        // Popular products
        List<ProductSalesDto> popularProducts = 
            orderItemRepository.findMostPopularProducts(startDate, endDate, 10);
        
        // Customer segments
        Map<String, Long> customerSegments = 
            orderRepository.groupOrdersByCustomerType(startDate, endDate);
        
        return OrderAnalyticsReport.builder()
            .period(request.getPeriod())
            .metrics(metrics)
            .popularProducts(popularProducts)
            .customerSegments(customerSegments)
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
```

This comprehensive business logic documentation provides AI agents with deep understanding of order processing, payment handling, Romanian compliance, and critical business workflows needed for successful implementation.
