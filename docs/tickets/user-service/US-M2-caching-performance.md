# US-M2: User Service Caching & Performance Optimization

**Phase**: M2 Caching & Resilience | **Service**: user-service | **Priority**: Medium | **Estimated Effort**: 3-4 days

## 🎯 **Summary**
Implement Redis caching, session management, and performance optimization for user-service. Focus on cart persistence, authentication caching, and resilience patterns for external service dependencies.

## 📋 **Scope**

### **Redis Caching Implementation**

#### **Session & Authentication Caching**
- JWT token blacklist for logout/revoke functionality
- User session data caching (profile, role, vehicle preferences)
- Authentication rate limiting with Redis counters
- Login attempt tracking with automatic lockout

#### **Cart Persistence & Optimization**
- Cart data caching with Redis for performance
- Cart session persistence across browser sessions
- Real-time cart synchronization between devices
- Cart abandonment tracking and recovery

#### **User Profile Caching**
- User profile data caching with automatic invalidation
- User vehicle preferences caching
- BMW compatibility results caching (short-term)
- Frequent lookup optimization

### **Performance Optimization**

#### **Database Query Optimization**
- Implement query result caching for expensive operations
- Optimize user vehicle lookup with proper indexing
- Cart item aggregation query optimization
- User search and filtering performance improvements

#### **Response Time Optimization**
- Target: <100ms for authentication endpoints
- Target: <50ms for cart operations (cached)
- Target: <200ms for profile management
- Target: <150ms for user vehicle operations

## 🏗️ **Implementation Requirements**

### **Redis Caching Configuration**
```java
@Configuration
@EnableCaching
public class UserCacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("user-profiles", 
                config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("user-vehicles", 
                config.entryTtl(Duration.ofHours(2)))
            .withCacheConfiguration("cart-items", 
                config.entryTtl(Duration.ofMinutes(15)))
            .build();
    }
}
```

### **Cart Caching Service**
```java
@Service
public class CartCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CartDomainService cartDomainService;
    
    @Cacheable(value = "cart-items", key = "#userId")
    public List<CartItemDto> getCachedCartItems(Long userId) {
        return cartDomainService.getCartItems(userId);
    }
    
    @CacheEvict(value = "cart-items", key = "#userId")
    public void invalidateCartCache(Long userId) {
        // Cache invalidation on cart modifications
    }
    
    public void persistCartToRedis(Long userId, List<CartItemDto> cartItems) {
        String key = "cart:session:" + userId;
        redisTemplate.opsForValue().set(key, cartItems, Duration.ofDays(7));
    }
    
    public List<CartItemDto> getCartFromRedis(Long userId) {
        String key = "cart:session:" + userId;
        return (List<CartItemDto>) redisTemplate.opsForValue().get(key);
    }
}
```

### **Session Management Service**
```java
@Service
public class UserSessionService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void cacheUserSession(String sessionId, UserSessionDto session) {
        String key = "session:" + sessionId;
        redisTemplate.opsForValue().set(key, session, Duration.ofHours(8));
    }
    
    public UserSessionDto getUserSession(String sessionId) {
        String key = "session:" + sessionId;
        return (UserSessionDto) redisTemplate.opsForValue().get(key);
    }
    
    public void invalidateUserSession(String sessionId) {
        String key = "session:" + sessionId;
        redisTemplate.delete(key);
    }
    
    public void blacklistJwtToken(String jti, Duration expiry) {
        String key = "jwt:blacklist:" + jti;
        redisTemplate.opsForValue().set(key, "REVOKED", expiry);
    }
    
    public boolean isTokenBlacklisted(String jti) {
        String key = "jwt:blacklist:" + jti;
        return redisTemplate.hasKey(key);
    }
}
```

### **Authentication Rate Limiting**
```java
@Service
public class AuthenticationRateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(30);
    
    public boolean isLoginAllowed(String email, String ipAddress) {
        String emailKey = "rate:email:" + email;
        String ipKey = "rate:ip:" + ipAddress;
        
        Integer emailAttempts = (Integer) redisTemplate.opsForValue().get(emailKey);
        Integer ipAttempts = (Integer) redisTemplate.opsForValue().get(ipKey);
        
        return (emailAttempts == null || emailAttempts < MAX_LOGIN_ATTEMPTS) &&
               (ipAttempts == null || ipAttempts < MAX_LOGIN_ATTEMPTS * 3); // IP gets higher limit
    }
    
    public void recordFailedLogin(String email, String ipAddress) {
        String emailKey = "rate:email:" + email;
        String ipKey = "rate:ip:" + ipAddress;
        
        // Increment counters with expiry
        redisTemplate.opsForValue().increment(emailKey);
        redisTemplate.expire(emailKey, LOCKOUT_DURATION);
        
        redisTemplate.opsForValue().increment(ipKey);
        redisTemplate.expire(ipKey, LOCKOUT_DURATION);
    }
    
    public void clearLoginAttempts(String email, String ipAddress) {
        redisTemplate.delete("rate:email:" + email);
        // Don't clear IP attempts to prevent abuse
    }
}
```

### **Resilience Patterns**

#### **Circuit Breaker for External Services**
```java
@Component
public class ProductServiceClient {
    
    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackInventoryCheck")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    public CompletableFuture<InventoryCheckResult> checkInventoryAsync(String productSku, Integer quantity) {
        return CompletableFuture.supplyAsync(() -> 
            productServiceRestClient.checkInventory(productSku, quantity));
    }
    
    public CompletableFuture<InventoryCheckResult> fallbackInventoryCheck(String productSku, Integer quantity, Exception ex) {
        log.warn("Product service unavailable, using fallback for inventory check", ex);
        
        // Return optimistic result - allow cart addition but flag for later validation
        return CompletableFuture.completedFuture(
            InventoryCheckResult.builder()
                .available(true)
                .availableQuantity(quantity)
                .needsValidation(true)
                .fallbackUsed(true)
                .build()
        );
    }
}

@Component
public class VehicleServiceClient {
    
    @CircuitBreaker(name = "vehicle-service", fallbackMethod = "fallbackCompatibilityCheck")
    @Retry(name = "vehicle-service")
    public CompatibilityResult validateCompatibility(String generationCode, String productSku) {
        return vehicleServiceRestClient.validateCompatibility(generationCode, productSku);
    }
    
    public CompatibilityResult fallbackCompatibilityCheck(String generationCode, String productSku, Exception ex) {
        log.warn("Vehicle service unavailable, skipping compatibility check", ex);
        
        // Return unknown result - don't block cart operations
        return CompatibilityResult.builder()
            .compatible(true)
            .confidence(CompatibilityConfidence.UNKNOWN)
            .reason("Vehicle service temporarily unavailable")
            .needsRevalidation(true)
            .build();
    }
}
```

## 🧪 **Testing Requirements (Implementation-First)**

### **Caching Integration Tests**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CartCacheServiceTest {
    
    @Autowired
    private CartCacheService cartCacheService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Test
    void getCachedCartItems_WithCacheHit_ShouldReturnCachedData() {
        // Test cache hit scenario
        // Verify data comes from Redis, not database
    }
    
    @Test
    void invalidateCartCache_ShouldRemoveFromRedis() {
        // Test cache invalidation
        // Verify data is removed from Redis
    }
    
    // Target: 75%+ coverage for caching logic
}
```

### **Performance Testing**
```java
@SpringBootTest
@ActiveProfiles("test")
class UserServicePerformanceTest {
    
    @Test
    void authenticateUser_ShouldCompleteWithin100ms() {
        // Performance test for authentication
        // Measure response times under load
    }
    
    @Test
    void getCartItems_WithCache_ShouldCompleteWithin50ms() {
        // Performance test for cached cart operations
        // Verify cache performance improvements
    }
}
```

### **Resilience Testing**
```java
@SpringBootTest
@ActiveProfiles("test")
class ServiceResilienceTest {
    
    @Test
    void addToCart_WithProductServiceDown_ShouldUseFallback() {
        // Test circuit breaker fallback
        // Verify graceful degradation
    }
    
    @Test
    void validateCompatibility_WithVehicleServiceTimeout_ShouldReturnUnknown() {
        // Test timeout handling
        // Verify fallback behavior
    }
}
```

## ✅ **Acceptance Criteria**

### **Caching Implementation**
- [ ] Redis caching configured for user profiles, cart items, and sessions
- [ ] JWT token blacklist functionality implemented
- [ ] Cart persistence across sessions working
- [ ] Authentication rate limiting with Redis counters

### **Performance Targets**
- [ ] Authentication endpoints: <100ms response time
- [ ] Cart operations (cached): <50ms response time
- [ ] Profile management: <200ms response time
- [ ] User vehicle operations: <150ms response time

### **Resilience Patterns**
- [ ] Circuit breakers implemented for product-service and vehicle-service
- [ ] Retry mechanisms with exponential backoff
- [ ] Graceful degradation when external services are unavailable
- [ ] Timeout handling with appropriate fallbacks

### **Cache Management**
- [ ] Automatic cache invalidation on data changes
- [ ] Cache warming strategies for frequently accessed data
- [ ] Cache monitoring and metrics collection
- [ ] Proper cache key naming and TTL configuration

## 🔧 **Configuration**

### **Resilience4j Configuration**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      product-service:
        failure-rate-threshold: 50
        minimum-number-of-calls: 5
        automatic-transition-from-open-to-half-open-enabled: true
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
      vehicle-service:
        failure-rate-threshold: 60
        minimum-number-of-calls: 3
        wait-duration-in-open-state: 20s
  
  retry:
    instances:
      product-service:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2
      vehicle-service:
        max-attempts: 2
        wait-duration: 300ms
  
  timelimiter:
    instances:
      product-service:
        timeout-duration: 3s
      vehicle-service:
        timeout-duration: 2s
```

### **Redis Cache Configuration**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 1800000  # 30 minutes default
      cache-null-values: false
      key-prefix: "beamerparts:user:"
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

## 📊 **Monitoring & Metrics**

### **Cache Metrics**
- Cache hit/miss ratios for different cache types
- Cache eviction rates and patterns
- Memory usage for Redis caches
- Average cache lookup times

### **Performance Metrics**
- Response time percentiles (95th, 99th)
- Throughput for authentication and cart operations
- Database query execution times
- External service call latencies

### **Resilience Metrics**
- Circuit breaker state changes
- Retry attempt counts and success rates
- Fallback invocation frequencies
- Service availability metrics

## 📚 **Reference Materials**
- **Redis Documentation**: Caching patterns and best practices
- **Resilience4j Guide**: Circuit breakers, retries, and timeouts
- **Spring Cache**: Caching abstraction and configuration
- **Performance Testing**: JMeter/Gatling integration patterns

## 🚀 **Getting Started**
1. **Configure Redis caching** with appropriate TTL settings
2. **Implement cart caching service** with proper invalidation
3. **Add authentication rate limiting** with Redis counters
4. **Create circuit breakers** for external service dependencies
5. **Test performance improvements** and resilience patterns
6. **Monitor cache effectiveness** and adjust configurations
