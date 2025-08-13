# VS-Tests: Vehicle Service Comprehensive Testing & Coverage

**Phase**: Quality Assurance | **Service**: vehicle-service | **Priority**: High | **Estimated Effort**: 4-5 days

## 🎯 **Summary**
Implement comprehensive test suite for vehicle-service with focus on BMW-specific business logic and compatibility validation. Apply product-service proven patterns (92.38% coverage) with specialized BMW testing scenarios.

## 📊 **Coverage Targets & Standards**

### **Enterprise Coverage Goals**
- **Overall Service Coverage**: 85%+ (enterprise standard)
- **Critical Domain Services**: 90%+ (BMW compatibility, data synchronization)
- **Entity Business Logic**: 85%+ (BmwSeries, BmwGeneration with business methods)
- **Controllers**: 75%+ (API contracts and error handling)
- **DTOs/Mappers**: 80%+ (BMW data transformation logic)

### **Strategic TDD Application**
```yaml
TIER 1 (TDD Required - 90%+ Coverage):
  - BmwCompatibilityDomainService: Year ranges, body codes, generation matching
  - VehicleDataSyncService: Event-driven sync, conflict resolution
  - BmwGeneration Entity: Year boundary logic, compatibility methods
  - BmwSeries Entity: Active generation validation, hierarchy logic

TIER 2 (Implementation-First - 75%+ Coverage):
  - VehicleAdminService: BMW data management, bulk operations
  - Controllers: API contracts, validation, error handling
  - VehicleDataManagementService: Import/export, admin operations
```

## 🧪 **BMW-Specific Testing Strategy**

### **BMW Compatibility Domain Service Testing (TIER 1 - TDD)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BmwCompatibilityDomainServiceTest {
    
    @Autowired
    private BmwCompatibilityDomainService compatibilityService;
    
    @Autowired
    private BmwGenerationRepository generationRepository;
    
    @Autowired
    private BmwSeriesRepository seriesRepository;
    
    // ===== Critical BMW Year Boundary Tests =====
    
    @Test
    void validateCompatibility_2019_F30_vs_G20_ShouldSelectCorrectGeneration() {
        // Create F30 (ends 2019) and G20 (starts 2019) generations
        BmwGeneration f30 = createAndSaveGeneration("F30", 2012, 2019);
        BmwGeneration g20 = createAndSaveGeneration("G20", 2019, null);
        
        // Test early 2019 (should be F30)
        CompatibilityResult earlyResult = compatibilityService
            .validateCompatibility("3", 2019, "early");
        assertThat(earlyResult.getSelectedGeneration()).isEqualTo("F30");
        
        // Test late 2019 (should be G20)
        CompatibilityResult lateResult = compatibilityService
            .validateCompatibility("3", 2019, "late");
        assertThat(lateResult.getSelectedGeneration()).isEqualTo("G20");
    }
    
    @Test
    void validateBodyCodeCompatibility_F30_Variants_ShouldDifferentiate() {
        // Create F30 generation with body code variants
        BmwGeneration f30 = createGenerationWithBodyCodes("F30", 
            new String[]{"F30", "F31", "F34", "F35"});
        
        // Test sedan (F30) compatibility
        CompatibilityResult sedanResult = compatibilityService
            .validateBodyCodeCompatibility("F30", "F30", "BMW-F30-AC-001");
        assertThat(sedanResult.isCompatible()).isTrue();
        
        // Test wagon (F31) compatibility - may have different parts
        CompatibilityResult wagonResult = compatibilityService
            .validateBodyCodeCompatibility("F30", "F31", "BMW-F30-AC-001");
        // Result depends on specific part compatibility rules
        
        // Test invalid body code
        CompatibilityResult invalidResult = compatibilityService
            .validateBodyCodeCompatibility("F30", "E90", "BMW-F30-AC-001");
        assertThat(invalidResult.isCompatible()).isFalse();
    }
    
    @Test
    void validateEngineVariantCompatibility_320i_vs_330d_ShouldApplyRules() {
        BmwGeneration f30 = createAndSaveGeneration("F30", 2012, 2019);
        
        // Test engine-specific part (turbo component)
        CompatibilityResult turboResult = compatibilityService
            .validateEngineCompatibility("F30", "330d", "BMW-F30-TURBO-001");
        assertThat(turboResult.isCompatible()).isTrue();
        
        // Same part should not be compatible with naturally aspirated engine
        CompatibilityResult naResult = compatibilityService
            .validateEngineCompatibility("F30", "320i", "BMW-F30-TURBO-001");
        assertThat(naResult.isCompatible()).isFalse();
        assertThat(naResult.getReason()).contains("Engine variant incompatible");
    }
    
    @Test
    void validateSeriesCompatibility_3Series_vs_XSeries_ShouldRejectCrossover() {
        BmwGeneration f30 = createAndSaveGeneration("F30", 2012, 2019); // 3 Series
        BmwGeneration f25 = createAndSaveGeneration("F25", 2010, 2017); // X3 Series
        
        // Sedan part should not fit X-Series
        CompatibilityResult crossoverResult = compatibilityService
            .validateSeriesCompatibility("F30", "F25", "BMW-F30-INTERIOR-001");
        
        assertThat(crossoverResult.isCompatible()).isFalse();
        assertThat(crossoverResult.getReason()).contains("Series incompatible");
    }
    
    // ===== Helper Methods =====
    
    private BmwGeneration createAndSaveGeneration(String code, Integer yearStart, Integer yearEnd) {
        BmwSeries series = createAndSaveSeries("3", "3 Series");
        
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
    
    private BmwSeries createAndSaveSeries(String code, String name) {
        BmwSeries series = BmwSeries.builder()
            .code(code + "-" + System.currentTimeMillis())
            .name(name)
            .displayOrder(1)
            .isActive(true)
            .build();
        
        return seriesRepository.saveAndFlush(series);
    }
    
    // Target: 90%+ coverage with BMW-specific scenarios
}
```

### **BMW Entity Business Logic Testing (TIER 1)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BmwGenerationTest {
    
    @Autowired
    private BmwGenerationRepository generationRepository;
    
    @Autowired
    private BmwSeriesRepository seriesRepository;
    
    // ===== BMW Business Logic Tests =====
    
    @Test
    void getDisplayName_ShouldFormatBmwCorrectly() {
        BmwGeneration generation = createAndSaveGeneration("F30", 2012, 2019);
        
        String displayName = generation.getDisplayName();
        
        assertThat(displayName).isEqualTo("BMW 3 Series F30 (2012-2019)");
    }
    
    @Test
    void getYearRange_ForCurrentGeneration_ShouldShowPresent() {
        BmwGeneration currentGen = createAndSaveGeneration("G20", 2019, null);
        
        String yearRange = currentGen.getYearRange();
        
        assertThat(yearRange).isEqualTo("2019-present");
    }
    
    @Test
    void getYearRange_ForEndedGeneration_ShouldShowRange() {
        BmwGeneration endedGen = createAndSaveGeneration("F30", 2012, 2019);
        
        String yearRange = endedGen.getYearRange();
        
        assertThat(yearRange).isEqualTo("2012-2019");
    }
    
    @Test
    void isCurrentGeneration_WithNullEndYear_ShouldReturnTrue() {
        BmwGeneration generation = createAndSaveGeneration("G20", 2019, null);
        
        boolean isCurrent = generation.isCurrentGeneration();
        
        assertThat(isCurrent).isTrue();
    }
    
    @Test
    void isCurrentGeneration_WithEndYear_ShouldReturnFalse() {
        BmwGeneration generation = createAndSaveGeneration("F30", 2012, 2019);
        
        boolean isCurrent = generation.isCurrentGeneration();
        
        assertThat(isCurrent).isFalse();
    }
    
    @Test
    void includesYear_WithinRange_ShouldReturnTrue() {
        BmwGeneration generation = createAndSaveGeneration("F30", 2012, 2019);
        
        assertThat(generation.includesYear(2015)).isTrue();
        assertThat(generation.includesYear(2012)).isTrue(); // Boundary
        assertThat(generation.includesYear(2019)).isTrue(); // Boundary
    }
    
    @Test
    void includesYear_OutsideRange_ShouldReturnFalse() {
        BmwGeneration generation = createAndSaveGeneration("F30", 2012, 2019);
        
        assertThat(generation.includesYear(2011)).isFalse();
        assertThat(generation.includesYear(2020)).isFalse();
    }
    
    @Test
    void includesYear_CurrentGeneration_ShouldAcceptFutureYears() {
        BmwGeneration currentGen = createAndSaveGeneration("G20", 2019, null);
        
        assertThat(currentGen.includesYear(2025)).isTrue(); // Future year
        assertThat(currentGen.includesYear(2019)).isTrue(); // Start year
    }
    
    @Test
    void hasBodyCode_WithMatchingCode_ShouldReturnTrue() {
        BmwGeneration generation = createGenerationWithBodyCodes("F30", 
            new String[]{"F30", "F31", "F34", "F35"});
        
        assertThat(generation.hasBodyCode("F30")).isTrue(); // Sedan
        assertThat(generation.hasBodyCode("F31")).isTrue(); // Wagon
        assertThat(generation.hasBodyCode("F34")).isTrue(); // Gran Turismo
        assertThat(generation.hasBodyCode("F35")).isTrue(); // Gran Turismo LCI
    }
    
    @Test
    void hasBodyCode_WithNonMatchingCode_ShouldReturnFalse() {
        BmwGeneration generation = createGenerationWithBodyCodes("F30", 
            new String[]{"F30", "F31", "F34", "F35"});
        
        assertThat(generation.hasBodyCode("E90")).isFalse(); // Previous gen
        assertThat(generation.hasBodyCode("G20")).isFalse(); // Next gen
        assertThat(generation.hasBodyCode("F32")).isFalse(); // Different series
    }
    
    @Test
    void hasBodyCode_WithNullBodyCodes_ShouldReturnFalse() {
        BmwGeneration generation = createAndSaveGeneration("F30", 2012, 2019);
        // No body codes set (null)
        
        assertThat(generation.hasBodyCode("F30")).isFalse();
    }
    
    // ===== Persistence Tests =====
    
    @Test
    void persistence_WithValidBmwData_ShouldSaveSuccessfully() {
        BmwGeneration generation = createAndSaveGeneration("F30", 2012, 2019);
        
        assertThat(generation.getId()).isNotNull();
        assertThat(generation.getCreatedAt()).isNotNull();
        assertThat(generation.getUpdatedAt()).isNotNull();
        assertThat(generation.getSeries()).isNotNull();
    }
    
    @Test
    void yearRangeConstraint_WithInvalidRange_ShouldThrowException() {
        BmwSeries series = createAndSaveSeries();
        
        // End year before start year should fail
        BmwGeneration invalidGeneration = BmwGeneration.builder()
            .series(series)
            .code("INVALID-" + System.currentTimeMillis())
            .name("Invalid")
            .yearStart(2019)
            .yearEnd(2015) // Invalid: end before start
            .isActive(true)
            .build();
        
        assertThatThrownBy(() -> generationRepository.saveAndFlush(invalidGeneration))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("year_range_check");
    }
    
    // ===== Helper Methods =====
    
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
    
    private BmwGeneration createGenerationWithBodyCodes(String code, String[] bodyCodes) {
        BmwSeries series = createAndSaveSeries();
        
        BmwGeneration generation = BmwGeneration.builder()
            .series(series)
            .code(code + "-" + System.currentTimeMillis())
            .name(code)
            .yearStart(2012)
            .yearEnd(2019)
            .bodyCodes(bodyCodes)
            .isActive(true)
            .build();
        
        return generationRepository.saveAndFlush(generation);
    }
    
    private BmwSeries createAndSaveSeries() {
        BmwSeries series = BmwSeries.builder()
            .code("3-" + System.currentTimeMillis())
            .name("3 Series")
            .displayOrder(1)
            .isActive(true)
            .build();
        
        return seriesRepository.saveAndFlush(series);
    }
    
    // Target: 85%+ coverage with BMW business logic focus
}
```

### **Vehicle Data Sync Service Testing (TIER 1)**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class VehicleDataSyncServiceTest {
    
    @Autowired
    private VehicleDataSyncService syncService;
    
    @MockBean
    private ProductServiceClient productServiceClient;
    
    @MockBean
    private VehicleEventPublisher eventPublisher;
    
    @Test
    void synchronizeWithProductService_WithConflicts_ShouldResolveCorrectly() {
        // Setup conflicting data scenario
        BmwGeneration localGeneration = createLocalGeneration("F30", 2012, 2019);
        BmwGenerationDto remoteGeneration = createRemoteGeneration("F30", 2012, 2020); // Conflict
        
        when(productServiceClient.getBmwGenerations())
            .thenReturn(List.of(remoteGeneration));
        
        SyncResult result = syncService.synchronizeWithProductService();
        
        // Vehicle-service should win conflicts
        assertThat(result.getConflictsResolved()).isEqualTo(1);
        assertThat(result.getResolutionStrategy()).isEqualTo("VEHICLE_SERVICE_WINS");
        
        // Verify event published for sync
        verify(eventPublisher).publishBmwGenerationUpdated(any());
    }
    
    @Test
    void validateDataConsistency_WithOrphanedRecords_ShouldIdentifyIssues() {
        // Create orphaned compatibility record
        createOrphanedCompatibilityRecord("ORPHAN-F30", "BMW-F30-AC-001");
        
        ValidationResult result = syncService.validateDataConsistency();
        
        assertThat(result.hasIssues()).isTrue();
        assertThat(result.getOrphanedRecords()).hasSize(1);
        assertThat(result.getIssues()).contains("Orphaned compatibility record found");
    }
    
    // Target: 85%+ coverage with sync scenarios
}
```

## ✅ **Acceptance Criteria**

### **Coverage Achievement**
- [ ] Overall service coverage ≥ 85%
- [ ] Critical domain services ≥ 90% (BMW compatibility, sync)
- [ ] Entity business logic ≥ 85% (BmwSeries, BmwGeneration)
- [ ] Controllers ≥ 75%
- [ ] All tests pass with `scripts/dev/run-tests.sh`

### **BMW-Specific Test Quality**
- [ ] Year boundary edge cases comprehensively tested (F30/G20 2019)
- [ ] Body code variant compatibility validated
- [ ] Engine variant rules properly tested
- [ ] Series-level compatibility rules verified
- [ ] Real database operations for BMW business logic

### **Testing Patterns Applied**
- [ ] Strategic TDD for TIER 1 BMW compatibility logic
- [ ] Implementation-first for TIER 2 admin/management features
- [ ] Product-service proven patterns consistently applied
- [ ] BMW-specific test data generation with proper constraints

## 📚 **Reference Materials**
- **Proven Patterns**: `product-service/src/test/` (92.38% coverage achieved)
- **BMW Business Logic**: `docs/tickets/vehicle-service/BUSINESS-LOGIC.md`
- **Testing Standards**: `.cursorrules` - Comprehensive testing guidelines
- **BMW Compatibility**: Existing BMW entity business methods

## 🚀 **Implementation Approach**
1. **Start with TIER 1 TDD**: BmwCompatibilityDomainService tests first
2. **Apply BMW-specific scenarios**: Year boundaries, body codes, engine variants
3. **Focus on business logic**: BMW generation transitions, compatibility rules
4. **Test entity business methods**: BmwGeneration and BmwSeries logic
5. **Add sync service tests**: Data consistency and conflict resolution
6. **Create controller tests**: API contracts with real BMW data
7. **Validate BMW edge cases**: Comprehensive compatibility scenario coverage
