# VS-M2: Vehicle Service Caching & Resilience Optimization

**Phase**: M2 Caching & Resilience | **Service**: vehicle-service | **Priority**: Medium | **Estimated Effort**: 3-4 days

## 🎯 **Summary**
Implement Redis caching for BMW data, performance optimization for compatibility validation, and resilience patterns for product-service integration. Focus on caching BMW hierarchy data and optimizing high-frequency compatibility checks.

## 📋 **Scope**

### **Redis Caching Implementation**

#### **BMW Hierarchy Caching**
- BMW series and generation data caching (long-term, rarely changes)
- Compatibility registry caching with smart invalidation
- BMW data lookup optimization for frequent queries
- Hierarchical cache keys for efficient invalidation

#### **Compatibility Results Caching**
- Product compatibility validation results (short-term cache)
- Bulk compatibility check optimization
- Year boundary calculation caching
- Engine variant compatibility caching

#### **Performance Optimization**
- Database query result caching for expensive BMW lookups
- Compatibility matrix pre-computation and caching
- Search result caching with filtering
- Popular generation/series priority caching

### **Resilience Patterns**

#### **Product Service Integration**
- Circuit breaker for BMW cache sync operations
- Retry mechanisms for product compatibility updates
- Graceful degradation when product-service is unavailable
- Fallback strategies for missing BMW data

#### **Data Consistency Handling**
- Cache warming strategies for BMW data
- Eventual consistency patterns for cross-service sync
- Conflict resolution caching
- Cache invalidation coordination

## 🏗️ **Implementation Requirements**

### **BMW Data Caching Configuration**
```java
@Configuration
@EnableCaching
public class VehicleCacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(4))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("bmw-series", 
                config.entryTtl(Duration.ofDays(1)))  // BMW data rarely changes
            .withCacheConfiguration("bmw-generations", 
                config.entryTtl(Duration.ofDays(1)))
            .withCacheConfiguration("compatibility-results", 
                config.entryTtl(Duration.ofHours(2)))  // Medium-term cache
            .withCacheConfiguration("compatibility-matrix", 
                config.entryTtl(Duration.ofHours(6)))
            .build();
    }
}
```

### **BMW Hierarchy Caching Service**
```java
@Service
public class BmwDataCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final BmwSeriesRepository seriesRepository;
    private final BmwGenerationRepository generationRepository;
    
    @Cacheable(value = "bmw-series", key = "#seriesCode")
    public BmwSeriesDto getCachedSeries(String seriesCode) {
        return bmwSeriesMapper.toDto(seriesRepository.findByCode(seriesCode)
            .orElseThrow(() -> new BmwSeriesNotFoundException(seriesCode)));
    }
    
    @Cacheable(value = "bmw-generations", key = "#generationCode")
    public BmwGenerationDto getCachedGeneration(String generationCode) {
        return bmwGenerationMapper.toDto(generationRepository.findByCode(generationCode)
            .orElseThrow(() -> new BmwGenerationNotFoundException(generationCode)));
    }
    
    @Cacheable(value = "bmw-series", key = "'all-active'")
    public List<BmwSeriesDto> getCachedActiveSeries() {
        return seriesRepository.findByIsActiveTrueOrderByDisplayOrder()
            .stream()
            .map(bmwSeriesMapper::toDto)
            .collect(toList());
    }
    
    @CacheEvict(value = {"bmw-series", "bmw-generations"}, allEntries = true)
    public void invalidateBmwHierarchyCache() {
        log.info("Invalidating BMW hierarchy cache");
    }
    
    @CacheEvict(value = "bmw-series", key = "#seriesCode")
    public void invalidateSeriesCache(String seriesCode) {
        log.info("Invalidating cache for BMW series: {}", seriesCode);
    }
    
    @CacheEvict(value = "bmw-generations", key = "#generationCode")
    public void invalidateGenerationCache(String generationCode) {
        log.info("Invalidating cache for BMW generation: {}", generationCode);
    }
}
```

### **Compatibility Results Caching Service**
```java
@Service
public class CompatibilityCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final BmwCompatibilityDomainService compatibilityService;
    
    @Cacheable(value = "compatibility-results", 
               key = "#generationCode + ':' + #productSku")
    public CompatibilityResult getCachedCompatibilityResult(String generationCode, String productSku) {
        return compatibilityService.validateCompatibility(generationCode, productSku);
    }
    
    @Cacheable(value = "compatibility-matrix", key = "#generationCode")
    public CompatibilityMatrix getCachedCompatibilityMatrix(String generationCode) {
        // Pre-compute compatibility matrix for generation
        return compatibilityService.buildCompatibilityMatrix(generationCode);
    }
    
    public BulkCompatibilityResult getCachedBulkCompatibility(
            List<CompatibilityCheckRequest> requests) {
        
        Map<String, List<CompatibilityCheckRequest>> groupedByGeneration = 
            requests.stream().collect(groupingBy(CompatibilityCheckRequest::getGenerationCode));
        
        List<CompatibilityResult> results = new ArrayList<>();
        
        for (Map.Entry<String, List<CompatibilityCheckRequest>> entry : groupedByGeneration.entrySet()) {
            String generationCode = entry.getKey();
            List<CompatibilityCheckRequest> generationRequests = entry.getValue();
            
            // Get cached compatibility matrix for this generation
            CompatibilityMatrix matrix = getCachedCompatibilityMatrix(generationCode);
            
            // Use matrix for fast lookups
            for (CompatibilityCheckRequest request : generationRequests) {
                CompatibilityResult result = matrix.getCompatibility(request.getProductSku());
                results.add(result);
            }
        }
        
        return BulkCompatibilityResult.builder()
            .results(results)
            .totalRequests(requests.size())
            .cacheHitRatio(calculateCacheHitRatio(results))
            .build();
    }
    
    @CacheEvict(value = "compatibility-results", key = "#generationCode + ':*'")
    public void invalidateCompatibilityForGeneration(String generationCode) {
        // Invalidate all compatibility results for a generation
        String pattern = "compatibility-results::" + generationCode + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

### **Year Boundary Caching Service**
```java
@Service
public class YearBoundaryCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "year-boundaries", key = "#seriesCode + ':' + #year")
    public YearBoundaryResult getCachedYearBoundary(String seriesCode, Integer year) {
        return calculateYearBoundary(seriesCode, year);
    }
    
    private YearBoundaryResult calculateYearBoundary(String seriesCode, Integer year) {
        // Complex year boundary calculation for BMW transitions
        // Example: 2019 BMW 3 Series (F30 vs G20)
        List<BmwGeneration> overlappingGenerations = 
            generationRepository.findBySeriesCodeAndYearOverlap(seriesCode, year);
        
        if (overlappingGenerations.size() > 1) {
            // Handle transition year logic
            return resolveTransitionYear(overlappingGenerations, year);
        } else if (overlappingGenerations.size() == 1) {
            return YearBoundaryResult.single(overlappingGenerations.get(0));
        } else {
            return YearBoundaryResult.noMatch();
        }
    }
    
    private YearBoundaryResult resolveTransitionYear(List<BmwGeneration> generations, Integer year) {
        // BMW-specific transition logic
        // Default: newer generation for second half of year
        BmwGeneration newerGeneration = generations.stream()
            .max(Comparator.comparing(BmwGeneration::getYearStart))
            .orElseThrow();
        
        BmwGeneration olderGeneration = generations.stream()
            .min(Comparator.comparing(BmwGeneration::getYearStart))
            .orElseThrow();
        
        return YearBoundaryResult.builder()
            .primaryGeneration(newerGeneration)
            .alternativeGeneration(olderGeneration)
            .isTransitionYear(true)
            .transitionMonth(6) // Mid-year transition
            .build();
    }
}
```

### **Resilience Patterns for Product Service**
```java
@Component
public class ProductServiceClient {
    
    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackBmwCacheSync")
    @Retry(name = "product-service")
    @TimeLimiter(name = "product-service")
    public CompletableFuture<BmwCacheSyncResult> syncBmwCacheAsync(BmwCacheSyncRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            productServiceRestClient.syncBmwCache(request));
    }
    
    public CompletableFuture<BmwCacheSyncResult> fallbackBmwCacheSync(
            BmwCacheSyncRequest request, Exception ex) {
        
        log.warn("Product service unavailable for BMW cache sync, deferring update", ex);
        
        // Store sync request for later retry
        deferredSyncService.scheduleSyncRetry(request);
        
        return CompletableFuture.completedFuture(
            BmwCacheSyncResult.builder()
                .successful(false)
                .deferred(true)
                .reason("Product service temporarily unavailable")
                .retryScheduled(true)
                .build()
        );
    }
    
    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackCompatibilityUpdate")
    public CompatibilityUpdateResult updateProductCompatibility(
            String generationCode, List<String> productSkus) {
        
        return productServiceRestClient.updateCompatibility(generationCode, productSkus);
    }
    
    public CompatibilityUpdateResult fallbackCompatibilityUpdate(
            String generationCode, List<String> productSkus, Exception ex) {
        
        log.warn("Product service unavailable for compatibility update", ex);
        
        // Mark for eventual consistency update
        eventualConsistencyService.markForCompatibilityUpdate(generationCode, productSkus);
        
        return CompatibilityUpdateResult.builder()
            .successful(false)
            .eventualConsistency(true)
            .affectedProducts(productSkus.size())
            .build();
    }
}
```

### **Cache Warming Service**
```java
@Service
public class VehicleCacheWarmingService {
    
    private final BmwDataCacheService cacheService;
    private final BmwSeriesRepository seriesRepository;
    
    @EventListener
    @Async
    public void handleApplicationStartup(ApplicationReadyEvent event) {
        log.info("Starting BMW data cache warming");
        warmBmwHierarchyCache();
        warmPopularCompatibilityCache();
    }
    
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void scheduledCacheWarming() {
        log.info("Performing scheduled cache warming");
        warmBmwHierarchyCache();
    }
    
    private void warmBmwHierarchyCache() {
        // Pre-load all active BMW series
        List<BmwSeries> activeSeries = seriesRepository.findByIsActiveTrueOrderByDisplayOrder();
        
        for (BmwSeries series : activeSeries) {
            // Warm series cache
            cacheService.getCachedSeries(series.getCode());
            
            // Warm generation cache for this series
            series.getGenerations().stream()
                .filter(BmwGeneration::getIsActive)
                .forEach(generation -> 
                    cacheService.getCachedGeneration(generation.getCode()));
        }
        
        log.info("Warmed cache for {} BMW series", activeSeries.size());
    }
    
    private void warmPopularCompatibilityCache() {
        // Pre-compute compatibility matrices for popular generations
        List<String> popularGenerations = getPopularGenerationCodes();
        
        for (String generationCode : popularGenerations) {
            compatibilityCacheService.getCachedCompatibilityMatrix(generationCode);
        }
        
        log.info("Warmed compatibility cache for {} popular generations", popularGenerations.size());
    }
}
```

## 🧪 **Testing Requirements (Implementation-First)**

### **Caching Integration Tests**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BmwDataCacheServiceTest {
    
    @Autowired
    private BmwDataCacheService cacheService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Test
    void getCachedSeries_WithCacheHit_ShouldReturnCachedData() {
        // First call - should hit database and cache result
        BmwSeriesDto firstCall = cacheService.getCachedSeries("3");
        
        // Second call - should return from cache
        BmwSeriesDto secondCall = cacheService.getCachedSeries("3");
        
        assertThat(firstCall).isEqualTo(secondCall);
        // Verify cache contains the data
        assertThat(redisTemplate.hasKey("bmw-series::3")).isTrue();
    }
    
    @Test
    void invalidateSeriesCache_ShouldRemoveFromRedis() {
        // Add data to cache
        cacheService.getCachedSeries("3");
        assertThat(redisTemplate.hasKey("bmw-series::3")).isTrue();
        
        // Invalidate cache
        cacheService.invalidateSeriesCache("3");
        
        // Verify removal
        assertThat(redisTemplate.hasKey("bmw-series::3")).isFalse();
    }
    
    // Target: 75%+ coverage for caching logic
}
```

### **Performance Testing**
```java
@SpringBootTest
@ActiveProfiles("test")
class VehicleServicePerformanceTest {
    
    @Test
    void getBmwGeneration_WithCache_ShouldCompleteWithin10ms() {
        // Performance test for cached BMW data lookup
        StopWatch stopWatch = new StopWatch();
        
        stopWatch.start();
        BmwGenerationDto generation = cacheService.getCachedGeneration("F30");
        stopWatch.stop();
        
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(10);
    }
    
    @Test
    void validateBulkCompatibility_WithCache_ShouldImprovePerformance() {
        // Test bulk compatibility performance with caching
        List<CompatibilityCheckRequest> requests = createLargeCompatibilityRequest();
        
        // First run - cache miss
        StopWatch firstRun = new StopWatch();
        firstRun.start();
        BulkCompatibilityResult firstResult = compatibilityService.validateBulkCompatibility(requests);
        firstRun.stop();
        
        // Second run - cache hit
        StopWatch secondRun = new StopWatch();
        secondRun.start();
        BulkCompatibilityResult secondResult = compatibilityService.validateBulkCompatibility(requests);
        secondRun.stop();
        
        // Cache should significantly improve performance
        assertThat(secondRun.getTotalTimeMillis()).isLessThan(firstRun.getTotalTimeMillis() * 0.5);
    }
}
```

### **Resilience Testing**
```java
@SpringBootTest
@ActiveProfiles("test")
class VehicleServiceResilienceTest {
    
    @MockBean
    private ProductServiceRestClient productServiceRestClient;
    
    @Test
    void syncBmwCache_WithProductServiceDown_ShouldUseFallback() {
        // Mock product service failure
        when(productServiceRestClient.syncBmwCache(any()))
            .thenThrow(new ServiceUnavailableException("Product service down"));
        
        BmwCacheSyncRequest request = createSyncRequest();
        BmwCacheSyncResult result = productServiceClient.syncBmwCacheAsync(request).join();
        
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.isDeferred()).isTrue();
        assertThat(result.isRetryScheduled()).isTrue();
    }
    
    @Test
    void validateCompatibility_WithCircuitBreakerOpen_ShouldReturnFallback() {
        // Test circuit breaker fallback behavior
        // Verify graceful degradation
    }
}
```

## ✅ **Acceptance Criteria**

### **Caching Implementation**
- [ ] Redis caching configured for BMW hierarchy data (1 day TTL)
- [ ] Compatibility results caching with smart invalidation (2 hour TTL)
- [ ] Bulk compatibility optimization with matrix pre-computation
- [ ] Cache warming for BMW data on application startup

### **Performance Targets**
- [ ] BMW data lookup: <10ms response time (cached)
- [ ] Compatibility validation: <20ms (cached) vs <100ms (uncached)
- [ ] Bulk compatibility checks: 50%+ performance improvement with cache
- [ ] Year boundary calculations: <5ms (cached)

### **Resilience Patterns**
- [ ] Circuit breaker for product-service BMW cache sync
- [ ] Graceful degradation when product-service unavailable
- [ ] Deferred sync mechanism for failed operations
- [ ] Eventual consistency handling for cross-service updates

### **Cache Management**
- [ ] Hierarchical cache invalidation (series → generations → compatibility)
- [ ] Cache hit/miss ratio monitoring
- [ ] Memory usage optimization for Redis
- [ ] Automatic cache warming strategies

## 🔧 **Configuration**

### **Resilience4j Configuration**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      product-service:
        failure-rate-threshold: 60
        minimum-number-of-calls: 5
        automatic-transition-from-open-to-half-open-enabled: true
        wait-duration-in-open-state: 45s
        permitted-number-of-calls-in-half-open-state: 3
  
  retry:
    instances:
      product-service:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
        retryExceptions:
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
  
  timelimiter:
    instances:
      product-service:
        timeout-duration: 5s
```

### **Redis Cache Configuration**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 14400000  # 4 hours default
      cache-null-values: false
      key-prefix: "beamerparts:vehicle:"
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 15
          max-idle: 8
          min-idle: 3
```

## 📊 **Performance Metrics**

### **Cache Performance**
- BMW hierarchy cache hit ratio (target: >95%)
- Compatibility cache hit ratio (target: >80%)
- Average cache lookup time (<5ms)
- Cache memory usage and optimization

### **BMW Data Access**
- Generation lookup time (cached vs uncached)
- Compatibility validation performance improvements
- Bulk operation optimization ratios
- Year boundary calculation efficiency

### **External Service Resilience**
- Product-service sync success rate
- Circuit breaker activation frequency
- Fallback usage patterns
- Deferred operation completion rates

## 📚 **Reference Materials**
- **Redis Documentation**: BMW data caching patterns
- **Resilience4j Guide**: BMW service integration patterns
- **Spring Cache**: Hierarchical cache invalidation
- **BMW Business Logic**: Year boundary and compatibility caching

## 🚀 **Getting Started**
1. **Configure Redis caching** for BMW hierarchy data
2. **Implement compatibility result caching** with matrix optimization
3. **Add circuit breakers** for product-service integration
4. **Create cache warming service** for BMW data
5. **Test performance improvements** and resilience patterns
6. **Monitor cache effectiveness** and BMW data access patterns
