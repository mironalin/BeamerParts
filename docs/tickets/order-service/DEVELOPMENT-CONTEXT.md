# Order Service Development Context

## 🎯 **Service Overview**
**Port**: 8084 | **Database**: order_db (PostgreSQL 5435) | **Dependencies**: Stripe, Email, PDF, RabbitMQ

Order Service handles the complete order processing workflow, payment processing, invoice generation, and inventory coordination. **This is the highest-risk service requiring maximum test coverage due to money handling.**

## 📋 **Current Implementation Status**

### ✅ **Completed (Existing)**
- **Basic Structure**: Spring Boot application with essential dependencies
- **Payment Dependencies**: Stripe integration libraries
- **PDF Generation**: iText for professional invoice generation
- **Email**: Thymeleaf templates for order notifications
- **Configuration**: Basic application.properties setup

### 🚧 **To Implement (Everything - Following M0→M3 Pattern)**
- **Entities**: Order, OrderItem, Payment, OrderStatus, Invoice (complete domain model)
- **Repositories**: All order-related repositories with complex queries
- **Domain Services**: Order workflow, payment processing, inventory coordination
- **Controllers**: Order management, payment processing, checkout flow, admin APIs
- **State Machine**: Order state transitions with business rules
- **DTOs and Mappers**: Complete order processing data structures
- **Payment Integration**: Stripe webhook handling, refund processing
- **Invoice System**: PDF generation with Romanian legal compliance
- **Comprehensive Tests**: **90%+ coverage target** (highest among all services)

## 🎯 **Strategic Implementation Approach**

**Note**: Order Service skips M1/M2 phases and goes directly to M3 since it's primarily for advanced order processing features.

### **TIER 1 Components (TDD Required - CRITICAL Risk)**
- **Order State Machine**: State transitions, validation, rollback scenarios
- **Payment Processing**: Stripe integration, webhook handling, refund logic
- **Money Calculations**: Order totals, tax, shipping, currency precision
- **Inventory Coordination**: Stock reservations, release on cancellation
- **Invoice Generation**: Romanian legal compliance, sequential numbering

### **TIER 2 Components (Implementation-First - Standard Risk)**
- **Order Management**: CRUD operations, search, filtering
- **Reporting**: Analytics, sales statistics, trend analysis
- **Notifications**: Email templates, status updates
- **Admin Features**: Order fulfillment, shipping management

## 📚 **Technology Rollout Phases**

### **Phase M0 - Foundation (Skipped for Order Service)**
*Order Service starts at M3 since it's primarily advanced functionality*

### **Phase M3 - Complete Order Processing**
- **Core Order Workflow**: Complete checkout flow from cart to delivery
- **Payment Processing**: Stripe integration with failure recovery
- **Invoice System**: PDF generation with Romanian legal requirements
- **Admin Management**: Order fulfillment and tracking
- **Target**: 90%+ test coverage, all money-handling paths tested

## 🎯 **Critical Business Logic (TDD Required)**

### **Order State Machine**
```
DRAFT → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
     ↓
  CANCELLED (with refund/restock logic)
```

### **Money-Handling Scenarios (Must Test Comprehensively)**
- Order total calculations (items + tax + shipping + discounts)
- Payment processing with multiple payment methods
- Partial refunds and restocking logic
- Currency handling with 2 decimal precision (RON)
- Failed payment recovery and retry scenarios
- Concurrent order processing race conditions

### **Inventory Coordination**
- Stock reservation during order placement
- Stock release on cancellation/payment failure
- Out-of-stock handling during checkout
- Race conditions with concurrent orders
- Inventory conflict resolution

### **Romanian Invoice Requirements**
- Sequential invoice numbering
- VAT calculations and compliance
- Company information and tax registration
- Customer billing/shipping addresses
- Product details with SKUs and prices
- Professional PDF formatting

## 🧪 **Testing Strategy (Highest Standards)**

### **Order Domain Service Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderDomainServiceTest {
    // Test complete order workflow end-to-end
    // Test all state transitions and business rules
    // Test money calculations with edge cases
    // Target: 90%+ coverage (highest standard)
}
```

### **Payment Service Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentProcessingServiceTest {
    // Mock Stripe API calls
    // Test payment success/failure scenarios
    // Test webhook handling and refund logic
    // Test concurrent payment processing
    // Target: 90%+ coverage for financial logic
}
```

### **Order State Machine Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderStateMachineTest {
    // Test all valid state transitions
    // Test invalid transition prevention
    // Test rollback scenarios
    // Test concurrent state changes
}
```

### **Invoice Generation Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class InvoiceServiceTest {
    // Test PDF generation with real data
    // Test Romanian legal compliance
    // Test sequential numbering
    // Test email delivery integration
}
```

## 🔗 **Key Integrations**

### **User Service Integration**
- Cart conversion to order during checkout
- User authentication and profile data
- Guest checkout with email-only identification

### **Product Service Integration**  
- Inventory reservations and stock management
- Product validation and pricing
- BMW compatibility verification

### **Vehicle Service Integration**
- Part compatibility validation during checkout
- BMW data for compatibility checks

### **External Services**
- **Stripe**: Payment processing, webhooks, refunds
- **Email Service**: Order confirmations, shipping notifications
- **PDF Generation**: Professional invoice creation

## 🚀 **AI Agent Development Prompt**

```
CONTEXT: Developing Order Service for BeamerParts BMW e-commerce platform

CRITICAL IMPORTANCE:
- This is the HIGHEST-RISK service handling money and inventory
- Requires 90%+ test coverage (highest among all services)
- Every money-handling path must be comprehensively tested
- NO SHORTCUTS allowed for financial logic

CURRENT STATE:
- Basic Spring Boot structure with payment/PDF dependencies
- Need to build everything: entities, services, controllers, tests
- Must handle complex order workflow and payment processing

BUSINESS-CRITICAL REQUIREMENTS:
- Order state machine with proper transitions and rollback
- Stripe payment processing with failure recovery
- Romanian invoice generation with legal compliance
- Inventory coordination with race condition handling
- Guest and user checkout workflows

IMPLEMENTATION STRATEGY:
- Apply FULL TDD for all business-critical logic
- Start with M3 phase (skip M0-M2 since it's advanced functionality)
- Use product-service proven testing patterns
- Mock external services (Stripe, email) but test integration points

TESTING REQUIREMENTS:
- Use @SpringBootTest + @Transactional for domain services
- Test all state transitions and edge cases
- Test money calculations with precision requirements
- Test concurrent scenarios and race conditions
- Mock Stripe API but test webhook handling

START WITH: OS-M3-external-checkout-payments-invoices
CRITICAL: Test money-handling logic before implementation
```

## 📁 **Package Structure (Standard BeamerParts Pattern)**
```
order-service/src/main/java/live/alinmiron/beamerparts/order/
├── controller/
│   ├── external/          # Order placement, tracking (/api/orders)
│   ├── internal/          # Service-to-service APIs (/internal/orders)
│   ├── admin/            # Order management (/api/admin/orders)
│   └── *OpenApiSpec.java # OpenAPI specifications
├── service/
│   ├── domain/           # Core business logic (OrderDomainService, PaymentService)
│   └── internal/         # Internal service operations
├── payment/              # Stripe integration, webhook handling
├── invoice/              # PDF generation, Romanian compliance
├── statemachine/         # Order state management
├── mapper/               # MapStruct mappers for order data
├── event/               # RabbitMQ event publishers/listeners
├── entity/              # To create (Order, OrderItem, Payment, Invoice)
├── repository/          # To create (complex order queries)
├── dto/                 # To create (complete order data structures)
└── exception/           # Custom exceptions + GlobalExceptionHandler
```

## ⚠️ **Critical Reminders**
- **NO SHORTCUTS** on testing for money-handling logic
- **Test edge cases** thoroughly (payment failures, inventory conflicts)
- **Validate all calculations** with comprehensive test scenarios  
- **Mock external services** but test integration points thoroughly
- **Romanian legal compliance** for invoices is mandatory
- **Concurrent order processing** must handle race conditions

## 📊 **Success Metrics**
- **Coverage**: 90%+ overall (95%+ for payment/money-handling services)
- **Performance**: <500ms checkout flow, <200ms payment processing
- **Financial Accuracy**: Zero calculation errors or payment processing bugs
- **Legal Compliance**: 100% Romanian invoice standard compliance
- **Reliability**: 99.9% order processing success rate
