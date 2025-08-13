# Vehicle Service Business Logic Documentation

## 🎯 **Service Purpose**
Vehicle Service is the authoritative source for BMW vehicle hierarchy, compatibility data, and business rules. It manages the complex BMW model structure and provides compatibility validation for part-to-vehicle matching.

## 🏗️ **Core Domain Concepts**

### **BMW Hierarchy Structure**
```
BMW Brand
├── Series (3, 5, 7, X3, X5, etc.)
│   └── Generations (E46, E90, F30, G20, etc.)
│       ├── Body Codes (F30, F31, F34, F35)
│       ├── Year Ranges (2012-2019)
│       └── Engine Variants (320i, 330d, M3)
```

### **Key Business Entities**

#### **BmwSeries**
- Represents BMW model lines (3 Series, 5 Series, X3, etc.)
- Contains multiple generations over time
- Has display order for UI presentation
- Can be active/inactive for management

#### **BmwGeneration**
- Specific generation of a BMW series (F30, G20, etc.)
- Has production year ranges (start/end years)
- Contains body code variants for different configurations
- Links to compatibility registry for parts

#### **VehicleCompatibilityRegistry**
- Maps BMW generations to compatible products
- Uses product SKUs for cross-service references
- Contains compatibility notes and verification status
- Supports bulk compatibility operations

## 🧠 **Critical Business Logic**

### **Year Boundary Handling**
**Problem**: BMW generations can overlap in transition years, requiring precise logic.

**Example**: 2019 BMW 3 Series
- **F30 Generation**: 2012-2019 (early 2019 production)
- **G20 Generation**: 2019-present (late 2019 production)

**Business Rules**:
```java
public BmwGeneration selectGenerationForYear(String seriesCode, Integer year, String period) {
    if (year == transitionYear) {
        return period.equals("early") ? oldGeneration : newGeneration;
    }
    // Standard year range logic
}
```

### **Body Code Compatibility**
**Problem**: Same generation can have multiple body configurations with different part compatibility.

**F30 Generation Body Codes**:
- **F30**: 4-door sedan
- **F31**: 5-door wagon (Touring)
- **F34**: 5-door Gran Turismo
- **F35**: Gran Turismo LCI (Life Cycle Impulse)

**Business Rules**:
```java
public boolean isBodyCodeCompatible(String generationCode, String bodyCode, String productSku) {
    BmwGeneration generation = findByCode(generationCode);
    
    // Check if body code exists in generation
    if (!generation.hasBodyCode(bodyCode)) {
        return false;
    }
    
    // Apply part-specific body code rules
    return applyBodyCodeCompatibilityRules(bodyCode, productSku);
}
```

### **Engine Variant Compatibility**
**Problem**: Parts can be engine-specific, requiring variant validation.

**Common Engine Variants**:
- **Naturally Aspirated**: 320i, 325i, 330i
- **Turbocharged**: 320i (F30), 328i, 335i
- **Diesel**: 320d, 325d, 330d
- **Performance**: M3, M340i

**Business Rules**:
```java
public CompatibilityResult validateEngineCompatibility(
    String generationCode, String engineVariant, String productSku) {
    
    // Get part characteristics
    PartInfo partInfo = getPartInfo(productSku);
    
    // Engine-specific compatibility rules
    if (partInfo.isTurboSpecific() && !isEngineVariantTurbocharged(engineVariant)) {
        return CompatibilityResult.incompatible("Turbo part not compatible with naturally aspirated engine");
    }
    
    if (partInfo.isDieselSpecific() && !isEngineVariantDiesel(engineVariant)) {
        return CompatibilityResult.incompatible("Diesel part not compatible with gasoline engine");
    }
    
    return CompatibilityResult.compatible();
}
```

### **Series-Level Compatibility**
**Problem**: Parts designed for one BMW series typically don't fit other series.

**Series Categories**:
- **Sedan Series**: 1, 3, 5, 7 Series
- **X-Series (SUV)**: X1, X3, X5, X6, X7
- **Z-Series (Roadster)**: Z3, Z4
- **i-Series (Electric)**: i3, i4, iX

**Business Rules**:
```java
public boolean isSeriesCompatible(String sourceGeneration, String targetGeneration) {
    BmwSeries sourceSeries = getSeriesForGeneration(sourceGeneration);
    BmwSeries targetSeries = getSeriesForGeneration(targetGeneration);
    
    // Same series - always compatible
    if (sourceSeries.getCode().equals(targetSeries.getCode())) {
        return true;
    }
    
    // Cross-series compatibility is rare and part-specific
    return checkCrossSeriesCompatibility(sourceSeries, targetSeries);
}
```

## 🔄 **Data Synchronization Logic**

### **Product Service Sync**
**Purpose**: Keep BMW data consistent between vehicle-service (master) and product-service (cache).

**Sync Flow**:
1. Vehicle-service publishes BMW data change events
2. Product-service receives events and updates BMW cache
3. Conflict resolution prioritizes vehicle-service data
4. Consistency validation ensures data integrity

**Business Rules**:
```java
public SyncResult synchronizeWithProductService() {
    // Get current data from both services
    List<BmwGeneration> localData = getAllGenerations();
    List<BmwGenerationDto> remoteData = productServiceClient.getBmwCache();
    
    SyncConflicts conflicts = detectConflicts(localData, remoteData);
    
    // Vehicle-service wins all conflicts (authoritative source)
    for (SyncConflict conflict : conflicts.getAll()) {
        resolveConflict(conflict, ResolutionStrategy.VEHICLE_SERVICE_WINS);
        publishUpdateEvent(conflict.getLocalData());
    }
    
    return buildSyncResult(conflicts);
}
```

### **Event-Driven Updates**
**Events Published**:
- `BmwSeriesUpdatedEvent`: Series data changes
- `BmwGenerationUpdatedEvent`: Generation data changes  
- `CompatibilityRuleUpdatedEvent`: Compatibility changes

**Event Handling**:
```java
@EventListener
public void handleBmwDataChange(BmwDataChangeEvent event) {
    // Validate change impact
    validateChangeImpact(event);
    
    // Update dependent data
    updateDependentCompatibilityRecords(event);
    
    // Publish event for other services
    eventPublisher.publishBmwDataUpdated(event);
    
    // Log change for audit trail
    auditLogger.logBmwDataChange(event);
}
```

## 📊 **Performance Optimization Logic**

### **Bulk Operations**
**Problem**: Individual compatibility checks are slow for large datasets (cart/order processing).

**Solution**: Bulk validation with optimized queries.

```java
public BulkCompatibilityResult validateBulkCompatibility(
    List<CompatibilityCheckRequest> requests) {
    
    // Group requests by generation for batch processing
    Map<String, List<CompatibilityCheckRequest>> groupedRequests = 
        requests.stream().collect(groupingBy(CompatibilityCheckRequest::getGenerationCode));
    
    List<CompatibilityResult> results = new ArrayList<>();
    
    for (Map.Entry<String, List<CompatibilityCheckRequest>> entry : groupedRequests.entrySet()) {
        // Single query per generation
        BmwGeneration generation = findByCodeWithCompatibility(entry.getKey());
        
        // Validate all requests for this generation
        for (CompatibilityCheckRequest request : entry.getValue()) {
            results.add(validateSingleCompatibility(generation, request));
        }
    }
    
    return BulkCompatibilityResult.builder()
        .results(results)
        .totalRequests(requests.size())
        .processingTimeMs(System.currentTimeMillis() - startTime)
        .build();
}
```

### **Caching Strategy**
**Cache Targets**:
- BMW series/generation hierarchy (rarely changes)
- Compatibility registry (moderate changes)
- Bulk validation results (short-term cache)

**Cache Logic**:
```java
@Cacheable(value = "bmw-hierarchy", key = "#seriesCode")
public BmwSeries getSeriesWithGenerations(String seriesCode) {
    return seriesRepository.findByCodeWithGenerations(seriesCode);
}

@CacheEvict(value = "bmw-hierarchy", key = "#seriesCode")
public void updateBmwSeries(String seriesCode, BmwSeries updatedSeries) {
    // Update and invalidate cache
}
```

## 🛡️ **Data Validation Logic**

### **BMW Data Integrity**
**Year Range Validation**:
```java
public ValidationResult validateYearRange(BmwGeneration generation) {
    if (generation.getYearStart() == null) {
        return ValidationResult.error("Start year is required");
    }
    
    if (generation.getYearEnd() != null && generation.getYearEnd() < generation.getYearStart()) {
        return ValidationResult.error("End year cannot be before start year");
    }
    
    // Check for overlapping generations in same series
    List<BmwGeneration> overlapping = findOverlappingGenerations(generation);
    if (!overlapping.isEmpty()) {
        return ValidationResult.warning("Generation overlaps with: " + 
            overlapping.stream().map(BmwGeneration::getCode).collect(joining(", ")));
    }
    
    return ValidationResult.success();
}
```

**Body Code Validation**:
```java
public ValidationResult validateBodyCodes(String[] bodyCodes) {
    if (bodyCodes == null || bodyCodes.length == 0) {
        return ValidationResult.warning("No body codes specified");
    }
    
    // Validate BMW body code format (letter + numbers)
    Pattern bodyCodePattern = Pattern.compile("^[A-Z]\\d{2}$");
    
    for (String bodyCode : bodyCodes) {
        if (!bodyCodePattern.matcher(bodyCode).matches()) {
            return ValidationResult.error("Invalid body code format: " + bodyCode);
        }
    }
    
    return ValidationResult.success();
}
```

## 🔍 **Search and Lookup Logic**

### **BMW Hierarchy Search**
```java
public List<BmwGeneration> searchGenerations(GenerationSearchCriteria criteria) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<BmwGeneration> query = cb.createQuery(BmwGeneration.class);
    Root<BmwGeneration> root = query.from(BmwGeneration.class);
    
    List<Predicate> predicates = new ArrayList<>();
    
    // Series filter
    if (criteria.getSeriesCode() != null) {
        predicates.add(cb.equal(root.get("series").get("code"), criteria.getSeriesCode()));
    }
    
    // Year range filter
    if (criteria.getYear() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("yearStart"), criteria.getYear()));
        predicates.add(cb.or(
            cb.isNull(root.get("yearEnd")),
            cb.greaterThanOrEqualTo(root.get("yearEnd"), criteria.getYear())
        ));
    }
    
    // Active filter
    if (criteria.getActiveOnly()) {
        predicates.add(cb.isTrue(root.get("isActive")));
    }
    
    query.where(predicates.toArray(new Predicate[0]));
    query.orderBy(cb.desc(root.get("yearStart")));
    
    return entityManager.createQuery(query).getResultList();
}
```

## 📈 **Analytics and Reporting Logic**

### **BMW Data Usage Analytics**
```java
public BmwAnalyticsReport generateUsageAnalytics(AnalyticsRequest request) {
    // Most popular BMW generations
    List<GenerationPopularityDto> popularGenerations = 
        compatibilityRepository.findMostCompatibleGenerations(request.getLimit());
    
    // Series distribution
    Map<String, Long> seriesDistribution = 
        generationRepository.countGenerationsBySeries();
    
    // Year coverage analysis
    YearCoverageDto yearCoverage = calculateYearCoverage();
    
    return BmwAnalyticsReport.builder()
        .popularGenerations(popularGenerations)
        .seriesDistribution(seriesDistribution)
        .yearCoverage(yearCoverage)
        .generatedAt(LocalDateTime.now())
        .build();
}
```

## 🚨 **Error Handling Logic**

### **BMW-Specific Exceptions**
```java
public class BmwValidationException extends RuntimeException {
    private final String bmwCode;
    private final ValidationType validationType;
    
    public BmwValidationException(String bmwCode, ValidationType type, String message) {
        super(String.format("BMW %s validation failed for code '%s': %s", 
            type.getDisplayName(), bmwCode, message));
        this.bmwCode = bmwCode;
        this.validationType = type;
    }
}

public class CompatibilityValidationException extends RuntimeException {
    private final String generationCode;
    private final String productSku;
    private final CompatibilityFailureReason reason;
}
```

### **Graceful Degradation**
```java
public CompatibilityResult validateWithFallback(String generationCode, String productSku) {
    try {
        return validateCompatibility(generationCode, productSku);
    } catch (BmwValidationException e) {
        log.warn("BMW validation failed, falling back to basic check", e);
        return performBasicCompatibilityCheck(generationCode, productSku);
    } catch (Exception e) {
        log.error("Compatibility validation failed completely", e);
        return CompatibilityResult.unknown("Validation service temporarily unavailable");
    }
}
```

## 📚 **Integration Patterns**

### **Product Service Integration**
- **BMW Cache Sync**: Vehicle-service → Product-service
- **Compatibility Queries**: Product-service → Vehicle-service
- **Event-Driven Updates**: RabbitMQ messaging

### **User Service Integration**
- **User Vehicle Validation**: Validate user's BMW vehicles
- **Compatibility Checks**: For user's garage and cart items

### **Order Service Integration**
- **Checkout Validation**: Ensure part compatibility before order
- **Bulk Validation**: Optimize order processing performance

This comprehensive business logic documentation provides AI agents with the deep domain knowledge needed to implement BMW-specific functionality correctly.
