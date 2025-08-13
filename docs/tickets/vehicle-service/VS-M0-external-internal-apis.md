# VS-M0: Vehicle Service External & Internal APIs

**Phase**: M0 Basic | **Service**: vehicle-service | **Priority**: High | **Estimated Effort**: 3-4 days

## 🎯 **Summary**
Implement core BMW vehicle lookup APIs and compatibility validation services. Establishes vehicle-service as the master data source for BMW hierarchy and compatibility information.

## 📋 **Scope**

### **External Endpoints (via gateway `/api/...`)**

#### **BMW Vehicle Lookup APIs**
- `GET /vehicles/series` - List all BMW series with active generations
- `GET /vehicles/series/{seriesCode}/generations` - Get generations for specific series
- `GET /vehicles/generations/{generationCode}` - Get detailed generation information
- `GET /vehicles/generations/{generationCode}/products` - Compatible products (delegates to Product Service)

### **Internal Endpoints (direct service-to-service `/internal/...`)**

#### **Vehicle Data APIs**
- `GET /internal/series/{seriesCode}` - Internal series details
- `GET /internal/generations/{generationCode}` - Internal generation details
- `POST /internal/series/bulk` - Bulk series lookup for performance
- `POST /internal/generations/bulk` - Bulk generation lookup for performance

#### **Compatibility APIs**
- `POST /internal/compatibility/validate` - Validate BMW generation compatibility
- `GET /internal/compatibility/{generationCode}/products` - Products compatible with generation
- `POST /internal/compatibility/bulk-validate` - Bulk compatibility validation

## 🏗️ **Implementation Requirements**

### **External DTOs**
```
dto/external/response/
├── BmwSeriesResponseDto.java
├── BmwGenerationResponseDto.java
├── VehicleLookupResponseDto.java
└── CompatibilityResponseDto.java
```

### **Internal DTOs**
```
dto/internal/request/
├── BulkSeriesLookupRequestDto.java
├── BulkGenerationLookupRequestDto.java
└── CompatibilityValidationRequestDto.java

dto/internal/response/
├── BmwSeriesInternalDto.java
├── BmwGenerationInternalDto.java
└── CompatibilityValidationResponseDto.java
```

### **Domain Services (TDD Required)**
```java
@Service
@Transactional
public class BmwCompatibilityDomainService {
    // Core BMW compatibility business logic
    // Year range validation (F30 2019 vs G20 2019)
    // Body code variant validation (F30, F31, F34, F35)
    // Engine variant compatibility rules
    // Generation transition handling
}

@Service
@Transactional
public class VehicleLookupDomainService {
    // BMW series and generation lookup
    // Display name generation
    // Active status management
    // Performance-optimized bulk operations
}
```

### **Controllers with OpenAPI**
```java
@RestController
@RequestMapping("/vehicles")
@Tag(name = "BMW Vehicles", description = "BMW vehicle lookup and compatibility")
public class VehicleController {
    // External APIs for BMW vehicle information
    // Proper OpenAPI documentation
    // Performance optimization for lookups
}

@RestController
@RequestMapping("/internal/compatibility")
public class CompatibilityInternalController {
    // Service-to-service compatibility validation
    // Bulk operations for performance
    // Simple error handling for internal APIs
}
```

## 🧪 **Testing Requirements (Strategic TDD)**

### **TIER 1 - TDD Required (High Risk)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BmwCompatibilityDomainServiceTest {
    
    // ===== BMW-Specific Compatibility Scenarios =====
    
    @Test
    void validateCompatibility_F30_2019_vs_G20_2019_ShouldReturnCorrectResults() {
        // Test year boundary compatibility
        // F30 generation ends in 2019, G20 starts in 2019
        // Ensure correct generation selection for 2019 models
    }
    
    @Test
    void validateBodyCodeCompatibility_F30_vs_F31_ShouldHandleVariants() {
        // Test body code variant compatibility
        // F30 sedan vs F31 wagon vs F34 Gran Turismo
        // Parts may be compatible across some but not all variants
    }
    
    @Test
    void validateEngineVariantCompatibility_320i_vs_330d_ShouldDifferentiate() {
        // Test engine-specific part compatibility
        // Some parts are engine variant specific
        // Others are universal across engines
    }
    
    @Test
    void validateSeriesCompatibility_3Series_vs_XSeries_ShouldRejectCrossover() {
        // Test series-level compatibility rules
        // Sedan parts typically not compatible with X-Series
        // Verify proper rejection of incompatible combinations
    }
    
    // Target: 90%+ coverage with BMW-specific edge cases
}
```

### **BMW Entity Business Logic Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BmwGenerationTest {
    
    @Autowired
    private BmwGenerationRepository generationRepository;
    
    @Autowired
    private BmwSeriesRepository seriesRepository;
    
    @Test
    void getDisplayName_ShouldFormatCorrectly() {
        BmwGeneration generation = createAndSaveGeneration("F30", 2012, 2019);
        
        assertThat(generation.getDisplayName())
            .isEqualTo("BMW 3 Series F30 (2012-2019)");
    }
    
    @Test
    void getYearRange_ForCurrentGeneration_ShouldShowOngoing() {
        BmwGeneration generation = createAndSaveGeneration("G20", 2019, null);
        
        assertThat(generation.getYearRange())
            .isEqualTo("2019-present");
    }
    
    @Test
    void isCurrentGeneration_WithNullEndYear_ShouldReturnTrue() {
        BmwGeneration generation = createAndSaveGeneration("G20", 2019, null);
        
        assertThat(generation.isCurrentGeneration()).isTrue();
    }
    
    @Test
    void hasBodyCode_WithMatchingCode_ShouldReturnTrue() {
        BmwGeneration generation = createGenerationWithBodyCodes(
            "F30", new String[]{"F30", "F31", "F34", "F35"}
        );
        
        assertThat(generation.hasBodyCode("F31")).isTrue();
        assertThat(generation.hasBodyCode("E90")).isFalse();
    }
    
    private BmwGeneration createAndSaveGeneration(String code, Integer yearStart, Integer yearEnd) {
        BmwSeries series = createAndSaveSeries();
        
        BmwGeneration generation = BmwGeneration.builder()
            .series(series)
            .code(code + "-" + System.currentTimeMillis())
            .name(code)
            .yearStart(yearStart)
            .yearEnd(yearEnd)
            .isActive(true)
            .build();
        
        return generationRepository.saveAndFlush(generation);
    }
    
    // Target: 85%+ coverage with BMW business logic focus
}
```

## 🔗 **Integration Points**

### **Product Service Integration**
- Compatible products lookup delegation
- BMW cache synchronization (future M1 phase)
- Part compatibility validation support

### **User Service Integration**
- User vehicle validation support
- BMW compatibility for user's garage

### **Order Service Integration** (Future)
- Part compatibility validation during checkout
- BMW data lookup for order processing

## ✅ **Acceptance Criteria**

### **Functional Requirements**
- [ ] BMW series and generation lookup working
- [ ] Compatibility validation with business rules implemented
- [ ] Year boundary cases handled correctly (2019 F30/G20)
- [ ] Body code variant validation working
- [ ] Bulk operations for performance optimization

### **BMW-Specific Business Logic**
- [ ] F30 vs G20 2019 year boundary handled correctly
- [ ] Body code variants (F30/F31/F34/F35) properly differentiated
- [ ] Engine variant compatibility rules implemented
- [ ] Series-level compatibility rules enforced

### **Performance Requirements**
- [ ] BMW lookup operations complete within 100ms
- [ ] Compatibility validation completes within 50ms
- [ ] Bulk operations handle 100+ items efficiently
- [ ] Proper database indexing for BMW queries

### **Quality Requirements**
- [ ] `scripts/dev/run-tests.sh` passes with ≥70% coverage
- [ ] BMW-specific edge cases comprehensively tested
- [ ] OpenAPI documentation for external endpoints
- [ ] Consistent error handling for invalid BMW codes

## 🔧 **BMW Data Model Enhancement**

### **Business Methods Implementation**
```java
// BmwGeneration entity enhancements
public String getDisplayName() {
    return String.format("BMW %s %s (%s)", 
        series.getName(), name, getYearRange());
}

public String getYearRange() {
    if (yearEnd == null) {
        return yearStart + "-present";
    }
    return yearStart + "-" + yearEnd;
}

public boolean isCurrentGeneration() {
    return yearEnd == null;
}

public boolean includesYear(Integer year) {
    if (year < yearStart) return false;
    return yearEnd == null || year <= yearEnd;
}

public boolean hasBodyCode(String bodyCode) {
    return bodyCodes != null && Arrays.asList(bodyCodes).contains(bodyCode);
}
```

### **Compatibility Validation Logic**
```java
@Service
public class BmwCompatibilityDomainService {
    
    public CompatibilityResult validateCompatibility(
        String generationCode, String productSku, Integer year) {
        
        BmwGeneration generation = findByCode(generationCode);
        
        // Year boundary validation
        if (!generation.includesYear(year)) {
            return CompatibilityResult.incompatible(
                "Year " + year + " not supported for " + generationCode);
        }
        
        // Additional business rules...
        return CompatibilityResult.compatible();
    }
}
```

## 📚 **Reference Materials**
- **API Contract**: `docs/beamerparts_api_contract.md` - Vehicle Service M0 section
- **BMW Data**: Existing entity models in vehicle-service
- **Product Integration**: Product-service BMW compatibility patterns
- **Testing Standards**: `.cursorrules` - Testing and OpenAPI guidelines

## 🚀 **Getting Started**
1. **Review** existing BMW entities (BmwSeries, BmwGeneration)
2. **Start with TDD** for BmwCompatibilityDomainService (critical business logic)
3. **Implement** BMW-specific business methods with comprehensive edge case testing
4. **Create** controllers with proper OpenAPI documentation
5. **Test** BMW-specific scenarios (year boundaries, body codes, variants)
6. **Validate** performance for BMW lookup operations