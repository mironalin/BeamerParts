package live.alinmiron.beamerparts.product.domain;

import live.alinmiron.beamerparts.product.entity.BmwGenerationCache;
import live.alinmiron.beamerparts.product.entity.BmwSeriesCache;
import live.alinmiron.beamerparts.product.repository.BmwGenerationCacheRepository;
import live.alinmiron.beamerparts.product.repository.BmwSeriesCacheRepository;
import live.alinmiron.beamerparts.product.service.domain.BmwCompatibilityDomainService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * BMW Compatibility Domain Business Specifications
 * 
 * Defines the expected business behavior for BMW compatibility management:
 * - BMW hierarchy data synchronization from Vehicle Service
 * - Cache management for fast part compatibility lookups
 * - Business rules for BMW series and generation relationships
 * - Data validation and integrity enforcement
 * 
 * These tests define WHAT the business domain should do, not HOW it's implemented.
 * The domain service implementation will follow these specifications.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@Transactional
@Rollback
@DisplayName("BMW Compatibility Domain Business Specifications")
class BmwCompatibilityDomainSpecification {

    @Autowired
    private BmwCompatibilityDomainService bmwCompatibilityDomainService;

    @Autowired
    private BmwSeriesCacheRepository bmwSeriesCacheRepository;

    @Autowired
    private BmwGenerationCacheRepository bmwGenerationCacheRepository;

    private BmwSeriesCache testSeries;
    private String testSeriesCode = "SPEC-3";

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        bmwGenerationCacheRepository.findBySeriesCode(testSeriesCode)
                .forEach(bmwGenerationCacheRepository::delete);
        bmwSeriesCacheRepository.findByCode(testSeriesCode)
                .ifPresent(bmwSeriesCacheRepository::delete);

        // Create test BMW series
        testSeries = BmwSeriesCache.builder()
                .code(testSeriesCode)
                .name("3 Series")
                .displayOrder(3)
                .isActive(true)
                .lastUpdated(LocalDateTime.now())
                .build();
        testSeries = bmwSeriesCacheRepository.save(testSeries);
    }

    @Test
    @DisplayName("Should synchronize BMW series data from Vehicle Service with business validation")
    void shouldSynchronizeBmwSeriesWithBusinessValidation() {
        // Given: New BMW series data from Vehicle Service
        String newSeriesCode = "SPEC-X5";
        String seriesName = "X5 Series";
        Integer displayOrder = 15;
        Boolean isActive = true;

        // When: Synchronize series data through domain service
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
            bmwCompatibilityDomainService.synchronizeSeries(newSeriesCode, seriesName, displayOrder, isActive);

        // Then: Series should be created with proper business rules applied
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSeriesCode()).isEqualTo(newSeriesCode);
        
        // Verify series exists in database with correct data
        Optional<BmwSeriesCache> savedSeries = bmwSeriesCacheRepository.findByCode(newSeriesCode);
        assertThat(savedSeries).isPresent();
        assertThat(savedSeries.get().getName()).isEqualTo(seriesName);
        assertThat(savedSeries.get().getDisplayOrder()).isEqualTo(displayOrder);
        assertThat(savedSeries.get().getIsActive()).isEqualTo(isActive);
        assertThat(savedSeries.get().getLastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("Should update existing BMW series when synchronizing duplicate codes")
    void shouldUpdateExistingBmwSeriesOnDuplicateSync() {
        // Given: Existing BMW series
        String existingCode = testSeries.getCode();
        String updatedName = "3 Series Updated";
        Integer updatedDisplayOrder = 5;
        
        // When: Synchronize with updated data
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
            bmwCompatibilityDomainService.synchronizeSeries(existingCode, updatedName, updatedDisplayOrder, true);

        // Then: Series should be updated, not duplicated
        assertThat(result.isSuccess()).isTrue();
        
        List<BmwSeriesCache> allSeriesWithCode = bmwSeriesCacheRepository.findAll().stream()
                .filter(s -> s.getCode().equals(existingCode))
                .toList();
        assertThat(allSeriesWithCode).hasSize(1); // No duplicates
        
        BmwSeriesCache updatedSeries = allSeriesWithCode.get(0);
        assertThat(updatedSeries.getName()).isEqualTo(updatedName);
        assertThat(updatedSeries.getDisplayOrder()).isEqualTo(updatedDisplayOrder);
    }

    @Test
    @DisplayName("Should synchronize BMW generation data with parent series validation")
    void shouldSynchronizeBmwGenerationWithParentValidation() {
        // Given: BMW generation data for existing series
        String generationCode = "SPEC-F30";
        String generationName = "F30";
        Integer yearStart = 2012;
        Integer yearEnd = 2018;
        List<String> bodyCodes = List.of("SPEC-SEDAN", "SPEC-TOURING", "SPEC-GT");

        // When: Synchronize generation data
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
            bmwCompatibilityDomainService.synchronizeGeneration(
                generationCode, testSeriesCode, generationName, yearStart, yearEnd, bodyCodes, true);

        // Then: Generation should be created with proper parent series relationship
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGenerationCode()).isEqualTo(generationCode);
        assertThat(result.getSeriesCode()).isEqualTo(testSeriesCode);

        // Verify generation exists with correct data and relationships
        Optional<BmwGenerationCache> savedGeneration = bmwGenerationCacheRepository.findByCode(generationCode);
        assertThat(savedGeneration).isPresent();
        
        BmwGenerationCache generation = savedGeneration.get();
        assertThat(generation.getSeriesCache().getCode()).isEqualTo(testSeriesCode);
        assertThat(generation.getName()).isEqualTo(generationName);
        assertThat(generation.getYearStart()).isEqualTo(yearStart);
        assertThat(generation.getYearEnd()).isEqualTo(yearEnd);
        assertThat(generation.getBodyCodes()).containsExactlyInAnyOrder(
            "SPEC-SEDAN", "SPEC-TOURING", "SPEC-GT");
        assertThat(generation.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should reject generation synchronization when parent series does not exist")
    void shouldRejectGenerationSyncWhenParentSeriesNotExists() {
        // Given: Generation data with non-existent parent series
        String generationCode = "SPEC-F30";
        String nonExistentSeriesCode = "SPEC-NONEXISTENT";
        String generationName = "F30";

        // When: Attempt to synchronize generation without valid parent series
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
            bmwCompatibilityDomainService.synchronizeGeneration(
                generationCode, nonExistentSeriesCode, generationName, 2012, 2018, List.of(), true);

        // Then: Synchronization should fail with clear business error
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Parent series not found");
        assertThat(result.getErrorMessage()).contains(nonExistentSeriesCode);

        // Verify generation was not created
        Optional<BmwGenerationCache> generation = bmwGenerationCacheRepository.findByCode(generationCode);
        assertThat(generation).isEmpty();
    }

    @Test
    @DisplayName("Should efficiently retrieve all active BMW series ordered by display priority")
    void shouldRetrieveActiveBmwSeriesOrderedByDisplayPriority() {
        // Given: Multiple BMW series with different display orders and active status
        BmwSeriesCache inactiveSeries = bmwSeriesCacheRepository.save(BmwSeriesCache.builder()
                .code("SPEC-1")
                .name("1 Series")
                .displayOrder(1)
                .isActive(false)
                .build());
        
        BmwSeriesCache highPrioritySeries = bmwSeriesCacheRepository.save(BmwSeriesCache.builder()
                .code("SPEC-X3")
                .name("X3 Series")
                .displayOrder(2)
                .isActive(true)
                .build());

        // When: Retrieve all active series
        List<BmwCompatibilityDomainService.BmwSeriesInfo> activeSeries = 
            bmwCompatibilityDomainService.getAllActiveSeries();

        // Then: Should return only active series ordered by display priority
        assertThat(activeSeries).hasSize(2); // testSeries + highPrioritySeries
        
        // Verify ordering (lower display order = higher priority)
        assertThat(activeSeries.get(0).getDisplayOrder()).isEqualTo(2);
        assertThat(activeSeries.get(0).getCode()).isEqualTo("SPEC-X3");
        assertThat(activeSeries.get(1).getDisplayOrder()).isEqualTo(3);
        assertThat(activeSeries.get(1).getCode()).isEqualTo(testSeriesCode);
        
        // Verify inactive series is excluded
        assertThat(activeSeries).noneMatch(s -> s.getCode().equals("SPEC-1"));
    }

    @Test
    @DisplayName("Should efficiently retrieve BMW generations for specific series with hierarchical context")
    void shouldRetrieveGenerationsForSeriesWithHierarchicalContext() {
        // Given: Multiple generations for the test series
        BmwGenerationCache f30Generation = bmwGenerationCacheRepository.save(BmwGenerationCache.builder()
                .code("SPEC-F30")
                .seriesCache(testSeries)
                .name("F30")
                .yearStart(2012)
                .yearEnd(2018)
                .isActive(true)
                .build());
        
        BmwGenerationCache g20Generation = bmwGenerationCacheRepository.save(BmwGenerationCache.builder()
                .code("SPEC-G20")
                .seriesCache(testSeries)
                .name("G20")
                .yearStart(2019)
                .yearEnd(null) // Current generation
                .isActive(true)
                .build());

        // When: Retrieve generations for specific series
        List<BmwCompatibilityDomainService.BmwGenerationInfo> seriesGenerations = 
            bmwCompatibilityDomainService.getGenerationsForSeries(testSeriesCode);

        // Then: Should return generations with complete hierarchical context
        assertThat(seriesGenerations).hasSize(2);
        
        // Verify hierarchical information is complete
        BmwCompatibilityDomainService.BmwGenerationInfo currentGeneration = seriesGenerations.stream()
                .filter(g -> g.getCode().equals("SPEC-G20"))
                .findFirst().orElseThrow();
        
        assertThat(currentGeneration.getSeriesCode()).isEqualTo(testSeriesCode);
        assertThat(currentGeneration.getSeriesName()).isEqualTo("3 Series");
        assertThat(currentGeneration.isCurrentGeneration()).isTrue();
        assertThat(currentGeneration.getYearRange()).isEqualTo("2019-present");
        
        BmwCompatibilityDomainService.BmwGenerationInfo previousGeneration = seriesGenerations.stream()
                .filter(g -> g.getCode().equals("SPEC-F30"))
                .findFirst().orElseThrow();
        
        assertThat(previousGeneration.isCurrentGeneration()).isFalse();
        assertThat(previousGeneration.getYearRange()).isEqualTo("2012-2018");
    }

    @Test
    @DisplayName("Should validate BMW generation existence for product compatibility checks")
    void shouldValidateBmwGenerationExistenceForCompatibility() {
        // Given: Existing BMW generation
        BmwGenerationCache generation = bmwGenerationCacheRepository.save(BmwGenerationCache.builder()
                .code("SPEC-F30")
                .seriesCache(testSeries)
                .name("F30")
                .yearStart(2012)
                .yearEnd(2018)
                .isActive(true)
                .build());

        // When: Validate generation existence for compatibility
        boolean existsAndActive = bmwCompatibilityDomainService.isGenerationValidForCompatibility("SPEC-F30");
        boolean nonExistentGeneration = bmwCompatibilityDomainService.isGenerationValidForCompatibility("SPEC-NONEXISTENT");

        // Then: Should correctly validate generation existence and active status
        assertThat(existsAndActive).isTrue();
        assertThat(nonExistentGeneration).isFalse();
    }

    @Test
    @DisplayName("Should perform bulk synchronization operations with transactional integrity")
    void shouldPerformBulkSyncWithTransactionalIntegrity() {
        // Given: Bulk BMW data from Vehicle Service (using realistic BMW codes within DB constraints)
        List<BmwCompatibilityDomainService.BmwSeriesSyncData> seriesData = List.of(
            new BmwCompatibilityDomainService.BmwSeriesSyncData("SPEC-1", "1 Series", 1, true),
            new BmwCompatibilityDomainService.BmwSeriesSyncData("SPEC-5", "5 Series", 5, true),
            new BmwCompatibilityDomainService.BmwSeriesSyncData("SPEC-X1", "X1 Series", 11, true)
        );
        
        List<BmwCompatibilityDomainService.BmwGenerationSyncData> generationData = List.of(
            new BmwCompatibilityDomainService.BmwGenerationSyncData("SPEC-F40", "SPEC-1", "F40", 2019, null, List.of("SPEC-HATCH"), true),
            new BmwCompatibilityDomainService.BmwGenerationSyncData("SPEC-G30", "SPEC-5", "G30", 2017, null, List.of("SPEC-SEDAN"), true)
        );

        // When: Perform bulk synchronization
        BmwCompatibilityDomainService.BulkSyncResult result = 
            bmwCompatibilityDomainService.performBulkSync(seriesData, generationData);

        // Then: All data should be synchronized with transactional integrity
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSeriesSynced()).isEqualTo(3);
        assertThat(result.getGenerationsSynced()).isEqualTo(2);

        // Verify all series were created
        assertThat(bmwSeriesCacheRepository.findByCode("SPEC-1")).isPresent();
        assertThat(bmwSeriesCacheRepository.findByCode("SPEC-5")).isPresent();
        assertThat(bmwSeriesCacheRepository.findByCode("SPEC-X1")).isPresent();

        // Verify generations with proper parent relationships
        Optional<BmwGenerationCache> f40 = bmwGenerationCacheRepository.findByCode("SPEC-F40");
        assertThat(f40).isPresent();
        assertThat(f40.get().getSeriesCache().getCode()).isEqualTo("SPEC-1");
        
        Optional<BmwGenerationCache> g30 = bmwGenerationCacheRepository.findByCode("SPEC-G30");
        assertThat(g30).isPresent();
        assertThat(g30.get().getSeriesCache().getCode()).isEqualTo("SPEC-5");
    }

    @Test
    @DisplayName("Should handle invalid input data gracefully with clear business error messages")
    void shouldHandleInvalidInputDataGracefully() {
        // Test invalid series data
        BmwCompatibilityDomainService.BmwSeriesSyncResult invalidSeriesResult = 
            bmwCompatibilityDomainService.synchronizeSeries(null, "Test Series", 1, true);
        
        assertThat(invalidSeriesResult.isSuccess()).isFalse();
        assertThat(invalidSeriesResult.getErrorMessage()).contains("Series code cannot be empty");

        // Test invalid generation data
        BmwCompatibilityDomainService.BmwGenerationSyncResult invalidGenerationResult = 
            bmwCompatibilityDomainService.synchronizeGeneration("SPEC-TEST", testSeriesCode, null, 2020, null, List.of(), true);
        
        assertThat(invalidGenerationResult.isSuccess()).isFalse();
        assertThat(invalidGenerationResult.getErrorMessage()).contains("Generation name cannot be empty");

        // Test invalid year ranges
        BmwCompatibilityDomainService.BmwGenerationSyncResult invalidYearResult = 
            bmwCompatibilityDomainService.synchronizeGeneration("SPEC-TEST", testSeriesCode, "Test Gen", 2020, 2019, List.of(), true);
        
        assertThat(invalidYearResult.isSuccess()).isFalse();
        assertThat(invalidYearResult.getErrorMessage()).contains("Year end cannot be before year start");
    }

    @Test
    @DisplayName("Should maintain cache consistency when synchronizing updated BMW data")
    void shouldMaintainCacheConsistencyOnUpdates() {
        // Given: Existing BMW generation
        BmwGenerationCache existingGeneration = bmwGenerationCacheRepository.save(BmwGenerationCache.builder()
                .code("SPEC-F30")
                .seriesCache(testSeries)
                .name("F30")
                .yearStart(2012)
                .yearEnd(2018)
                .bodyCodes(new String[]{"SPEC-SEDAN"})
                .isActive(true)
                .lastUpdated(LocalDateTime.now().minusHours(1))
                .build());
        
        LocalDateTime originalUpdateTime = existingGeneration.getLastUpdated();

        // When: Synchronize with updated generation data
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
            bmwCompatibilityDomainService.synchronizeGeneration(
                "SPEC-F30", testSeriesCode, "F30 Updated", 2012, 2019, 
                List.of("SPEC-SEDAN", "SPEC-TOURING"), true);

        // Then: Generation should be updated maintaining cache consistency
        assertThat(result.isSuccess()).isTrue();
        
        Optional<BmwGenerationCache> updatedGeneration = bmwGenerationCacheRepository.findByCode("SPEC-F30");
        assertThat(updatedGeneration).isPresent();
        
        BmwGenerationCache generation = updatedGeneration.get();
        assertThat(generation.getName()).isEqualTo("F30 Updated");
        assertThat(generation.getYearEnd()).isEqualTo(2019);
        assertThat(generation.getBodyCodes()).containsExactlyInAnyOrder("SPEC-SEDAN", "SPEC-TOURING");
        assertThat(generation.getLastUpdated()).isAfter(originalUpdateTime);
        
        // Verify no duplicates were created
        List<BmwGenerationCache> allF30Generations = bmwGenerationCacheRepository.findAll().stream()
                .filter(g -> g.getCode().equals("SPEC-F30"))
                .toList();
        assertThat(allF30Generations).hasSize(1);
    }
}
