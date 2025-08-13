# OS-M2: Order Service Caching, Performance & Resilience

**Phase**: M2 Caching & Resilience | **Service**: order-service | **Priority**: High | **Estimated Effort**: 4-5 days

## 🎯 **Summary**
Implement Redis caching for order data, performance optimization for high-frequency operations, and resilience patterns for payment processing and external service dependencies. Focus on payment performance, order lookup optimization, and robust error handling for financial operations.

## 📋 **Scope**

### **Redis Caching Implementation**

#### **Order Data Caching**
- Order summary caching for frequent status checks
- Order history caching with user-based invalidation
- Payment status caching for real-time updates
- Order tracking information caching

#### **Performance Optimization**
- Payment processing rate limiting and fraud prevention
- Order lookup optimization for customer service
- Bulk order operations caching
- Romanian invoice data caching

#### **Session & Cart Integration**
- Order session persistence for checkout flow
- Payment timeout management with Redis
- Cart-to-order conversion caching
- Guest order token management

### **Resilience Patterns**

#### **Payment Processing Resilience**
- Circuit breaker for Stripe API calls
- Payment retry mechanisms with exponential backoff
- Graceful degradation for payment failures
- Idempotency handling for payment operations

#### **External Service Integration**
- Circuit breakers for user-service and product-service
- Timeout handling for inventory coordination
- Fallback strategies for service unavailability
- Eventual consistency patterns for order data

## 🏗️ **Implementation Requirements**

### **Order Caching Configuration**
```java
@Configuration
@EnableCaching
public class OrderCacheConfig {
    
    @Bean
    public CacheManager orderCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("order-summaries", 
                config.entryTtl(Duration.ofMinutes(15)))  // Frequent status checks
            .withCacheConfiguration("order-history", 
                config.entryTtl(Duration.ofHours(1)))     // User order history
            .withCacheConfiguration("payment-status", 
                config.entryTtl(Duration.ofMinutes(5)))   // Real-time payment updates
            .withCacheConfiguration("order-tracking", 
                config.entryTtl(Duration.ofMinutes(30)))  // Shipping tracking
            .withCacheConfiguration("invoice-data", 
                config.entryTtl(Duration.ofHours(6)))     // Romanian invoice info
            .build();
    }
}
```

### **Order Caching Service**
```java
@Service
public class OrderCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    
    @Cacheable(value = "order-summaries", key = "#orderId")
    public OrderSummaryDto getCachedOrderSummary(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        return OrderSummaryDto.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .itemCount(order.getOrderItems().size())
            .customerIdentifier(order.getCustomerIdentifier())
            .build();
    }
    
    @Cacheable(value = "order-history", key = "#userId + ':' + #page + ':' + #size")
    public Page<OrderHistoryDto> getCachedUserOrderHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        
        return orderPage.map(orderMapper::toHistoryDto);
    }
    
    @Cacheable(value = "payment-status", key = "#orderId")
    public PaymentStatusDto getCachedPaymentStatus(Long orderId) {
        Order order = orderRepository.findByIdWithPayment(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        Payment payment = order.getPayment();
        if (payment == null) {
            return PaymentStatusDto.noneRequired();
        }
        
        return PaymentStatusDto.builder()
            .orderId(orderId)
            .paymentId(payment.getId())
            .status(payment.getStatus())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .stripePaymentIntentId(payment.getStripePaymentIntentId())
            .completedAt(payment.getCompletedAt())
            .build();
    }
    
    @CacheEvict(value = {"order-summaries", "order-history", "payment-status"}, key = "#orderId")
    public void invalidateOrderCache(Long orderId) {
        log.info("Invalidating cache for order: {}", orderId);
        
        // Also invalidate user-specific caches
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && order.getUserId() != null) {
            invalidateUserOrderHistory(order.getUserId());
        }
    }
    
    @CacheEvict(value = "order-history", allEntries = true)
    public void invalidateUserOrderHistory(Long userId) {
        log.info("Invalidating order history cache for user: {}", userId);
    }
    
    public void cacheCheckoutSession(String sessionId, CheckoutSessionData sessionData) {
        String key = "checkout:session:" + sessionId;
        redisTemplate.opsForValue().set(key, sessionData, Duration.ofMinutes(30));
    }
    
    public CheckoutSessionData getCheckoutSession(String sessionId) {
        String key = "checkout:session:" + sessionId;
        return (CheckoutSessionData) redisTemplate.opsForValue().get(key);
    }
    
    public void invalidateCheckoutSession(String sessionId) {
        String key = "checkout:session:" + sessionId;
        redisTemplate.delete(key);
    }
}
```

### **Payment Rate Limiting Service**
```java
@Service
public class PaymentRateLimitingService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_PAYMENT_ATTEMPTS_PER_HOUR = 5;
    private static final int MAX_PAYMENT_ATTEMPTS_PER_DAY = 20;
    
    public boolean isPaymentAllowed(String customerIdentifier, String ipAddress) {
        String hourlyKey = "payment:rate:hourly:" + customerIdentifier;
        String dailyKey = "payment:rate:daily:" + customerIdentifier;
        String ipKey = "payment:rate:ip:" + ipAddress;
        
        Integer hourlyAttempts = (Integer) redisTemplate.opsForValue().get(hourlyKey);
        Integer dailyAttempts = (Integer) redisTemplate.opsForValue().get(dailyKey);
        Integer ipAttempts = (Integer) redisTemplate.opsForValue().get(ipKey);
        
        return (hourlyAttempts == null || hourlyAttempts < MAX_PAYMENT_ATTEMPTS_PER_HOUR) &&
               (dailyAttempts == null || dailyAttempts < MAX_PAYMENT_ATTEMPTS_PER_DAY) &&
               (ipAttempts == null || ipAttempts < MAX_PAYMENT_ATTEMPTS_PER_HOUR * 2);
    }
    
    public void recordPaymentAttempt(String customerIdentifier, String ipAddress) {
        String hourlyKey = "payment:rate:hourly:" + customerIdentifier;
        String dailyKey = "payment:rate:daily:" + customerIdentifier;
        String ipKey = "payment:rate:ip:" + ipAddress;
        
        // Increment counters with appropriate expiry
        redisTemplate.opsForValue().increment(hourlyKey);
        redisTemplate.expire(hourlyKey, Duration.ofHours(1));
        
        redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, Duration.ofDays(1));
        
        redisTemplate.opsForValue().increment(ipKey);
        redisTemplate.expire(ipKey, Duration.ofHours(1));
    }
    
    public void recordSuccessfulPayment(String customerIdentifier) {
        // Clear rate limiting on successful payment to allow legitimate retries
        String hourlyKey = "payment:rate:hourly:" + customerIdentifier;
        redisTemplate.delete(hourlyKey);
    }
}
```

### **Payment Processing Resilience**
```java
@Component
public class StripeClient {
    
    private final RestTemplate restTemplate;
    private final PaymentRateLimitingService rateLimitingService;
    
    @CircuitBreaker(name = "stripe-payment", fallbackMethod = "fallbackCreatePaymentIntent")
    @Retry(name = "stripe-payment")
    @TimeLimiter(name = "stripe-payment")
    public CompletableFuture<StripePaymentIntent> createPaymentIntentAsync(
            StripePaymentIntentRequest request) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Rate limiting check
            if (!rateLimitingService.isPaymentAllowed(request.getCustomerIdentifier(), 
                    request.getIpAddress())) {
                throw new PaymentRateLimitExceededException("Payment rate limit exceeded");
            }
            
            // Record attempt
            rateLimitingService.recordPaymentAttempt(request.getCustomerIdentifier(), 
                request.getIpAddress());
            
            // Make Stripe API call
            return stripeApiClient.createPaymentIntent(request);
        });
    }
    
    public CompletableFuture<StripePaymentIntent> fallbackCreatePaymentIntent(
            StripePaymentIntentRequest request, Exception ex) {
        
        log.warn("Stripe payment intent creation failed, using fallback", ex);
        
        // Return degraded response that allows order creation but delays payment
        return CompletableFuture.completedFuture(
            StripePaymentIntent.builder()
                .id("fallback_" + UUID.randomUUID().toString())
                .clientSecret("fallback_secret")
                .status("requires_retry")
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .fallbackUsed(true)
                .build()
        );
    }
    
    @CircuitBreaker(name = "stripe-refund", fallbackMethod = "fallbackCreateRefund")
    @Retry(name = "stripe-refund")
    public StripeRefundResponse createRefund(StripeRefundRequest request) {
        return stripeApiClient.createRefund(request);
    }
    
    public StripeRefundResponse fallbackCreateRefund(StripeRefundRequest request, Exception ex) {
        log.error("Stripe refund creation failed, queuing for manual processing", ex);
        
        // Queue refund for manual processing
        manualRefundQueueService.queueRefundForManualProcessing(request);
        
        return StripeRefundResponse.builder()
            .id("manual_processing_" + UUID.randomUUID().toString())
            .status("manual_processing_required")
            .amount(request.getAmount())
            .manualProcessingRequired(true)
            .build();
    }
}
```

### **External Service Resilience**
```java
@Component
public class ProductServiceClient {
    
    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackInventoryReservation")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    public CompletableFuture<InventoryReservationResponse> reserveInventoryAsync(
            InventoryReservationRequest request) {
        
        return CompletableFuture.supplyAsync(() -> 
            productServiceRestClient.reserveInventory(request));
    }
    
    public CompletableFuture<InventoryReservationResponse> fallbackInventoryReservation(
            InventoryReservationRequest request, Exception ex) {
        
        log.warn("Product service unavailable for inventory reservation, using optimistic approach", ex);
        
        // Optimistic reservation - assume success but flag for verification
        return CompletableFuture.completedFuture(
            InventoryReservationResponse.builder()
                .successful(true)
                .optimistic(true)
                .requiresVerification(true)
                .reservationIds(request.getItems().stream()
                    .collect(toMap(
                        InventoryReservationItem::getProductSku,
                        item -> "optimistic_" + UUID.randomUUID().toString())))
                .message("Optimistic reservation - verification required")
                .build()
        );
    }
}

@Component
public class UserServiceClient {
    
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUserCart")
    @Retry(name = "user-service")
    public UserCartDto getUserCart(Long userId) {
        return userServiceRestClient.getUserCart(userId);
    }
    
    public UserCartDto fallbackGetUserCart(Long userId, Exception ex) {
        log.warn("User service unavailable, checking cached cart data", ex);
        
        // Try to get cart from cache
        UserCartDto cachedCart = cartCacheService.getCachedUserCart(userId);
        if (cachedCart != null) {
            cachedCart.setFromCache(true);
            cachedCart.setRequiresVerification(true);
            return cachedCart;
        }
        
        throw new UserServiceUnavailableException("User service unavailable and no cached cart data");
    }
}
```

### **Order Performance Optimization Service**
```java
@Service
public class OrderPerformanceService {
    
    private final OrderCacheService cacheService;
    private final OrderRepository orderRepository;
    
    public Page<OrderSummaryDto> getOptimizedOrderHistory(Long userId, int page, int size) {
        // Try cache first
        try {
            return cacheService.getCachedUserOrderHistory(userId, page, size);
        } catch (Exception e) {
            log.warn("Cache miss for user order history, falling back to database", e);
            
            // Fallback to optimized database query
            return getOptimizedOrderHistoryFromDb(userId, page, size);
        }
    }
    
    private Page<OrderSummaryDto> getOptimizedOrderHistoryFromDb(Long userId, int page, int size) {
        // Use optimized projection query to avoid loading full order entities
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        return orderRepository.findOrderSummariesByUserId(userId, pageable);
    }
    
    public List<OrderStatusUpdateDto> getBulkOrderStatuses(List<Long> orderIds) {
        // Batch operation for customer service interfaces
        List<OrderStatusUpdateDto> results = new ArrayList<>();
        
        // Try to get from cache first
        Map<Long, OrderSummaryDto> cachedOrders = new HashMap<>();
        List<Long> uncachedIds = new ArrayList<>();
        
        for (Long orderId : orderIds) {
            try {
                OrderSummaryDto cached = cacheService.getCachedOrderSummary(orderId);
                cachedOrders.put(orderId, cached);
            } catch (Exception e) {
                uncachedIds.add(orderId);
            }
        }
        
        // Batch fetch uncached orders
        if (!uncachedIds.isEmpty()) {
            List<Order> orders = orderRepository.findByIdIn(uncachedIds);
            for (Order order : orders) {
                OrderSummaryDto summary = orderMapper.toSummaryDto(order);
                cachedOrders.put(order.getId(), summary);
                
                // Cache for future requests
                cacheService.cacheOrderSummary(order.getId(), summary);
            }
        }
        
        // Convert to status update DTOs
        for (Long orderId : orderIds) {
            OrderSummaryDto summary = cachedOrders.get(orderId);
            if (summary != null) {
                results.add(OrderStatusUpdateDto.builder()
                    .orderId(orderId)
                    .status(summary.getStatus())
                    .lastUpdated(summary.getUpdatedAt())
                    .build());
            }
        }
        
        return results;
    }
}
```

## 🧪 **Testing Requirements (Implementation-First)**

### **Caching Integration Tests**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderCacheServiceTest {
    
    @Autowired
    private OrderCacheService cacheService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Test
    void getCachedOrderSummary_WithCacheHit_ShouldReturnCachedData() {
        Order order = createAndSaveOrder();
        
        // First call - should cache result
        OrderSummaryDto firstCall = cacheService.getCachedOrderSummary(order.getId());
        
        // Second call - should return from cache
        OrderSummaryDto secondCall = cacheService.getCachedOrderSummary(order.getId());
        
        assertThat(firstCall).isEqualTo(secondCall);
        assertThat(redisTemplate.hasKey("order-summaries::" + order.getId())).isTrue();
    }
    
    @Test
    void invalidateOrderCache_ShouldRemoveFromRedis() {
        Order order = createAndSaveOrder();
        
        // Cache the order
        cacheService.getCachedOrderSummary(order.getId());
        assertThat(redisTemplate.hasKey("order-summaries::" + order.getId())).isTrue();
        
        // Invalidate cache
        cacheService.invalidateOrderCache(order.getId());
        
        // Verify removal
        assertThat(redisTemplate.hasKey("order-summaries::" + order.getId())).isFalse();
    }
    
    // Target: 75%+ coverage for caching logic
}
```

### **Payment Rate Limiting Tests**
```java
@SpringBootTest
@ActiveProfiles("test")
class PaymentRateLimitingServiceTest {
    
    @Autowired
    private PaymentRateLimitingService rateLimitingService;
    
    @Test
    void isPaymentAllowed_WithinLimits_ShouldReturnTrue() {
        String customer = "customer_" + System.currentTimeMillis();
        String ip = "127.0.0.1";
        
        boolean allowed = rateLimitingService.isPaymentAllowed(customer, ip);
        
        assertThat(allowed).isTrue();
    }
    
    @Test
    void isPaymentAllowed_ExceedingHourlyLimit_ShouldReturnFalse() {
        String customer = "customer_" + System.currentTimeMillis();
        String ip = "127.0.0.1";
        
        // Exhaust hourly limit
        for (int i = 0; i < 5; i++) {
            rateLimitingService.recordPaymentAttempt(customer, ip);
        }
        
        boolean allowed = rateLimitingService.isPaymentAllowed(customer, ip);
        
        assertThat(allowed).isFalse();
    }
    
    // Target: 80%+ coverage for rate limiting logic
}
```

### **Resilience Testing**
```java
@SpringBootTest
@ActiveProfiles("test")
class PaymentResilienceTest {
    
    @MockBean
    private StripeApiClient stripeApiClient;
    
    @Test
    void createPaymentIntent_WithStripeFailure_ShouldUseFallback() {
        StripePaymentIntentRequest request = createValidRequest();
        
        when(stripeApiClient.createPaymentIntent(any()))
            .thenThrow(new StripeServiceException("Stripe temporarily unavailable"));
        
        CompletableFuture<StripePaymentIntent> result = stripeClient.createPaymentIntentAsync(request);
        StripePaymentIntent intent = result.join();
        
        assertThat(intent.isFallbackUsed()).isTrue();
        assertThat(intent.getStatus()).isEqualTo("requires_retry");
    }
    
    // Target: 80%+ coverage for resilience patterns
}
```

## ✅ **Acceptance Criteria**

### **Caching Implementation**
- [ ] Order summary caching with 15-minute TTL
- [ ] User order history caching with smart invalidation
- [ ] Payment status caching for real-time updates
- [ ] Checkout session persistence with Redis

### **Performance Targets**
- [ ] Order status lookup: <20ms response time (cached)
- [ ] Order history retrieval: <100ms for cached pages
- [ ] Payment processing: <3s end-to-end
- [ ] Bulk order operations: 50%+ performance improvement

### **Resilience Patterns**
- [ ] Circuit breakers for Stripe and external services
- [ ] Payment rate limiting and fraud prevention
- [ ] Graceful degradation for service failures
- [ ] Retry mechanisms with exponential backoff

### **Payment Security & Performance**
- [ ] Payment rate limiting per customer and IP
- [ ] Idempotency handling for payment operations
- [ ] Fraud detection integration
- [ ] Payment timeout management

## 📚 **Reference Materials**
- **Redis Patterns**: Order data caching strategies
- **Resilience4j**: Payment processing resilience
- **Stripe Best Practices**: Payment performance optimization
- **Performance Testing**: Load testing for order operations

## 🚀 **Getting Started**
1. **Configure Redis caching** for order and payment data
2. **Implement rate limiting** for payment operations
3. **Add circuit breakers** for external service dependencies
4. **Create performance optimizations** for order lookups
5. **Test resilience patterns** and caching effectiveness
6. **Monitor performance improvements** and cache hit ratios
