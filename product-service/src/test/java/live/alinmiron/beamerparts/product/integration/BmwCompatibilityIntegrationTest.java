package live.alinmiron.beamerparts.product.integration;

import live.alinmiron.beamerparts.product.dto.internal.request.BmwCacheBulkSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.BmwGenerationSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.BmwSeriesSyncRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.BmwGenerationCacheDto;
import live.alinmiron.beamerparts.product.dto.internal.response.BmwSeriesCacheDto;
import live.alinmiron.beamerparts.product.entity.BmwGenerationCache;
import live.alinmiron.beamerparts.product.entity.BmwSeriesCache;
import live.alinmiron.beamerparts.product.repository.BmwGenerationCacheRepository;
import live.alinmiron.beamerparts.product.repository.BmwSeriesCacheRepository;
import live.alinmiron.beamerparts.product.service.internal.BmwCacheInternalService;

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

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for complete BMW Compatibility workflows.
 * Tests the integration between BmwCacheInternalService, BmwCompatibilityDomainService, and the database.
 * 
 * PROFESSIONAL APPROACH: Uses Flyway migrations for production-like schema consistency.
 * This ensures our tests validate the exact same database schema used in production.
 * 
 * These tests verify:
 * - Complete service-to-service integration (internal → domain → repository)
 * - Real database operations with PostgreSQL using production schema
 * - BMW data synchronization workflows from Vehicle Service
 * - Cache management and hierarchical data integrity
 * - Business rule enforcement across all layers
 * - Migration compatibility and schema consistency
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@Transactional
@Rollback
@DisplayName("BMW Compatibility Integration Tests - Complete Business Workflows")
class BmwCompatibilityIntegrationTest {

    @Autowired
    private BmwCacheInternalService bmwCacheInternalService;

    @Autowired
    private BmwSeriesCacheRepository bmwSeriesCacheRepository;

    @Autowired
    private BmwGenerationCacheRepository bmwGenerationCacheRepository;

    private BmwSeriesCache testSeries;
    private String testSeriesCode = "INT-3";

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        bmwGenerationCacheRepository.findBySeriesCode(testSeriesCode)
                .forEach(bmwGenerationCacheRepository::delete);
        bmwSeriesCacheRepository.findByCode(testSeriesCode)
                .ifPresent(bmwSeriesCacheRepository::delete);

        // Create test BMW series through service layer
        BmwSeriesSyncRequestDto seriesRequest = BmwSeriesSyncRequestDto.builder()
                .code(testSeriesCode)
                .name("3 Series")
                .displayOrder(3)
                .isActive(true)
                .build();
        
        bmwCacheInternalService.syncSeries(seriesRequest);
        testSeries = bmwSeriesCacheRepository.findByCode(testSeriesCode).orElseThrow();
    }

    @Test
    @DisplayName("Complete BMW Series Synchronization Workflow - Service Integration")
    void shouldCompleteBmwSeriesSyncWorkflowThroughAllLayers() {
        // Given: New BMW series data from Vehicle Service
        BmwSeriesSyncRequestDto syncRequest = BmwSeriesSyncRequestDto.builder()
                .code("INT-X5")
                .name("X5 Series")
                .displayOrder(15)
                .isActive(true)
                .build();

        // When: Synchronize through complete service stack
        BmwSeriesCacheDto result = bmwCacheInternalService.syncSeries(syncRequest);

        // Then: Verify complete series information at all layers
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("INT-X5");
        assertThat(result.getName()).isEqualTo("X5 Series");
        assertThat(result.getDisplayOrder()).isEqualTo(15);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getLastUpdated()).isNotNull();

        // Verify database state consistency
        BmwSeriesCache dbSeries = bmwSeriesCacheRepository.findByCode("INT-X5").orElseThrow();
        assertThat(dbSeries.getDisplayName()).isEqualTo("BMW X5 Series");
        assertThat(dbSeries.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("BMW Generation Synchronization with Parent Series Validation - Complete Integration")
    void shouldSynchronizeBmwGenerationWithParentValidationThroughAllLayers() {
        // Given: BMW generation data for existing series
        BmwGenerationSyncRequestDto syncRequest = BmwGenerationSyncRequestDto.builder()
                .code("INT-F30")
                .seriesCode(testSeriesCode)
                .name("F30")
                .yearStart(2012)
                .yearEnd(2018)
                .bodyCodes(List.of("INT-SEDAN", "INT-TOURING"))
                .isActive(true)
                .build();

        // When: Synchronize generation through complete service stack
        BmwGenerationCacheDto result = bmwCacheInternalService.syncGeneration(syncRequest);

        // Then: Verify complete generation information with parent relationships
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("INT-F30");
        assertThat(result.getSeriesCode()).isEqualTo(testSeriesCode);
        assertThat(result.getName()).isEqualTo("F30");
        assertThat(result.getYearStart()).isEqualTo(2012);
        assertThat(result.getYearEnd()).isEqualTo(2018);
        assertThat(result.getBodyCodes()).containsExactlyInAnyOrder("INT-SEDAN", "INT-TOURING");
        assertThat(result.isActive()).isTrue();

        // Verify database state and relationships
        BmwGenerationCache dbGeneration = bmwGenerationCacheRepository.findByCode("INT-F30").orElseThrow();
        assertThat(dbGeneration.getSeriesCache().getCode()).isEqualTo(testSeriesCode);
        assertThat(dbGeneration.getDisplayName()).contains("3 Series F30");
        assertThat(dbGeneration.getYearRange()).isEqualTo("2012-2018");
        assertThat(dbGeneration.hasBodyCode("INT-SEDAN")).isTrue();
        assertThat(dbGeneration.hasBodyCode("INT-TOURING")).isTrue();
        assertThat(dbGeneration.isCurrentGeneration()).isFalse();
    }

    @Test
    @DisplayName("Generation Sync Error Handling for Non-existent Parent Series - Complete Integration")
    void shouldHandleGenerationSyncErrorsGracefullyAcrossLayers() {
        // Given: Generation data with non-existent parent series
        BmwGenerationSyncRequestDto invalidRequest = BmwGenerationSyncRequestDto.builder()
                .code("INT-F30")
                .seriesCode("INT-NONEXISTENT")
                .name("F30")
                .yearStart(2012)
                .yearEnd(2018)
                .bodyCodes(List.of("INT-SEDAN"))
                .isActive(true)
                .build();

        // When: Attempt to synchronize with invalid parent series
        // Then: Should throw exception with clear business error
        assertThatThrownBy(() -> bmwCacheInternalService.syncGeneration(invalidRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Parent series not found: INT-NONEXISTENT");

        // Verify generation was not created
        assertThat(bmwGenerationCacheRepository.findByCode("INT-F30")).isEmpty();
    }

    @Test
    @DisplayName("Retrieve All BMW Series Ordered by Display Priority - Performance and Accuracy")
    void shouldRetrieveAllSeriesEfficientlyThroughAllLayers() {
        // Given: Multiple BMW series with different display orders
        bmwCacheInternalService.syncSeries(BmwSeriesSyncRequestDto.builder()
                .code("INT-1")
                .name("1 Series")
                .displayOrder(1)
                .isActive(true)
                .build());
        
        bmwCacheInternalService.syncSeries(BmwSeriesSyncRequestDto.builder()
                .code("INT-X3")
                .name("X3 Series")
                .displayOrder(13)
                .isActive(true)
                .build());
        
        bmwCacheInternalService.syncSeries(BmwSeriesSyncRequestDto.builder()
                .code("INT-7")
                .name("7 Series")
                .displayOrder(7)
                .isActive(false) // Inactive series
                .build());

        // When: Retrieve all series through service stack
        List<BmwSeriesCacheDto> allSeries = bmwCacheInternalService.getAllSeries();

        // Then: Should return only active series ordered by display priority
        assertThat(allSeries).hasSize(3); // testSeries + INT-1 + INT-X3 (excluding inactive INT-7)
        
        // Verify ordering (lower display order = higher priority)
        assertThat(allSeries.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(allSeries.get(0).getCode()).isEqualTo("INT-1");
        assertThat(allSeries.get(1).getDisplayOrder()).isEqualTo(3);
        assertThat(allSeries.get(1).getCode()).isEqualTo(testSeriesCode);
        assertThat(allSeries.get(2).getDisplayOrder()).isEqualTo(13);
        assertThat(allSeries.get(2).getCode()).isEqualTo("INT-X3");
        
        // Verify business rule: Only active series returned
        assertThat(allSeries).noneMatch(s -> s.getCode().equals("INT-7"));
        assertThat(allSeries).allMatch(BmwSeriesCacheDto::isActive);
    }

    @Test
    @DisplayName("BMW Series Lookup by Code - Fast and Accurate")
    void shouldLookupBmwSeriesByCodeEfficientlyAcrossLayers() {
        // When: Lookup existing series by code
        BmwSeriesCacheDto existingSeries = bmwCacheInternalService.getSeriesByCode(testSeriesCode);

        // Then: Should return complete series information
        assertThat(existingSeries).isNotNull();
        assertThat(existingSeries.getCode()).isEqualTo(testSeriesCode);
        assertThat(existingSeries.getName()).isEqualTo("3 Series");
        assertThat(existingSeries.getDisplayOrder()).isEqualTo(3);
        assertThat(existingSeries.isActive()).isTrue();

        // When: Attempt to lookup non-existent series
        // Then: Should throw clear business exception
        assertThatThrownBy(() -> bmwCacheInternalService.getSeriesByCode("INT-NONEXISTENT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("BMW series not found: INT-NONEXISTENT");
    }

    @Test
    @DisplayName("Retrieve All BMW Generations Ordered by Year - Complete Hierarchical Data")
    void shouldRetrieveAllGenerationsWithHierarchicalContextThroughAllLayers() {
        // Given: Multiple BMW generations across different series and years
        bmwCacheInternalService.syncGeneration(BmwGenerationSyncRequestDto.builder()
                .code("INT-F30")
                .seriesCode(testSeriesCode)
                .name("F30")
                .yearStart(2012)
                .yearEnd(2018)
                .bodyCodes(List.of("INT-SEDAN"))
                .isActive(true)
                .build());
        
        bmwCacheInternalService.syncGeneration(BmwGenerationSyncRequestDto.builder()
                .code("INT-G20")
                .seriesCode(testSeriesCode)
                .name("G20")
                .yearStart(2019)
                .yearEnd(null) // Current generation
                .bodyCodes(List.of("INT-SEDAN", "INT-TOURING"))
                .isActive(true)
                .build());

        // When: Retrieve all generations through service stack
        List<BmwGenerationCacheDto> allGenerations = bmwCacheInternalService.getAllGenerations();

        // Then: Should return generations ordered by year (descending - newest first)
        assertThat(allGenerations).hasSize(2);
        
        // Verify ordering (newer generations first)
        BmwGenerationCacheDto currentGeneration = allGenerations.get(0);
        assertThat(currentGeneration.getCode()).isEqualTo("INT-G20");
        assertThat(currentGeneration.getYearStart()).isEqualTo(2019);
        assertThat(currentGeneration.getYearEnd()).isNull();
        
        BmwGenerationCacheDto previousGeneration = allGenerations.get(1);
        assertThat(previousGeneration.getCode()).isEqualTo("INT-F30");
        assertThat(previousGeneration.getYearStart()).isEqualTo(2012);
        assertThat(previousGeneration.getYearEnd()).isEqualTo(2018);
        
        // Verify hierarchical information is complete
        allGenerations.forEach(generation -> {
            assertThat(generation.getSeriesCode()).isEqualTo(testSeriesCode);
            assertThat(generation.isActive()).isTrue();
            assertThat(generation.getLastUpdated()).isNotNull();
        });
    }

    @Test
    @DisplayName("Retrieve Generations for Specific Series - Hierarchical Query Performance")
    void shouldRetrieveGenerationsForSpecificSeriesThroughAllLayers() {
        // Given: Generations for multiple series
        bmwCacheInternalService.syncSeries(BmwSeriesSyncRequestDto.builder()
                .code("INT-5")
                .name("5 Series")
                .displayOrder(5)
                .isActive(true)
                .build());
        
        // Add generations to test series (3 Series)
        bmwCacheInternalService.syncGeneration(BmwGenerationSyncRequestDto.builder()
                .code("INT-F30")
                .seriesCode(testSeriesCode)
                .name("F30")
                .yearStart(2012)
                .yearEnd(2018)
                .isActive(true)
                .build());
        
        // Add generation to different series (5 Series)
        bmwCacheInternalService.syncGeneration(BmwGenerationSyncRequestDto.builder()
                .code("INT-G30")
                .seriesCode("INT-5")
                .name("G30")
                .yearStart(2017)
                .yearEnd(null)
                .isActive(true)
                .build());

        // When: Retrieve generations for specific series
        List<BmwGenerationCacheDto> seriesGenerations = bmwCacheInternalService.getGenerationsBySeriesCode(testSeriesCode);

        // Then: Should return only generations for the specified series
        assertThat(seriesGenerations).hasSize(1);
        
        BmwGenerationCacheDto generation = seriesGenerations.get(0);
        assertThat(generation.getCode()).isEqualTo("INT-F30");
        assertThat(generation.getSeriesCode()).isEqualTo(testSeriesCode);
        
        // Verify isolation: Should not include generations from other series
        assertThat(seriesGenerations).noneMatch(g -> g.getCode().equals("INT-G30"));
    }

    @Test
    @DisplayName("BMW Generation Lookup by Code - Fast and Accurate")
    void shouldLookupBmwGenerationByCodeEfficientlyAcrossLayers() {
        // Given: Existing BMW generation
        bmwCacheInternalService.syncGeneration(BmwGenerationSyncRequestDto.builder()
                .code("INT-F30")
                .seriesCode(testSeriesCode)
                .name("F30")
                .yearStart(2012)
                .yearEnd(2018)
                .bodyCodes(List.of("INT-SEDAN", "INT-TOURING"))
                .isActive(true)
                .build());

        // When: Lookup existing generation by code
        BmwGenerationCacheDto existingGeneration = bmwCacheInternalService.getGenerationByCode("INT-F30");

        // Then: Should return complete generation information
        assertThat(existingGeneration).isNotNull();
        assertThat(existingGeneration.getCode()).isEqualTo("INT-F30");
        assertThat(existingGeneration.getSeriesCode()).isEqualTo(testSeriesCode);
        assertThat(existingGeneration.getName()).isEqualTo("F30");
        assertThat(existingGeneration.getYearStart()).isEqualTo(2012);
        assertThat(existingGeneration.getYearEnd()).isEqualTo(2018);
        assertThat(existingGeneration.getBodyCodes()).containsExactlyInAnyOrder("INT-SEDAN", "INT-TOURING");

        // When: Attempt to lookup non-existent generation
        // Then: Should throw clear business exception
        assertThatThrownBy(() -> bmwCacheInternalService.getGenerationByCode("INT-NONEXISTENT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("BMW generation not found: INT-NONEXISTENT");
    }

    @Test
    @DisplayName("Bulk BMW Data Synchronization - Transactional Integrity and Performance")
    void shouldPerformBulkSyncWithTransactionalIntegrityThroughAllLayers() {
        // Given: Bulk BMW data from Vehicle Service
        List<BmwSeriesSyncRequestDto> seriesData = List.of(
            BmwSeriesSyncRequestDto.builder()
                    .code("INT-1")
                    .name("1 Series")
                    .displayOrder(1)
                    .isActive(true)
                    .build(),
            BmwSeriesSyncRequestDto.builder()
                    .code("INT-5")
                    .name("5 Series")
                    .displayOrder(5)
                    .isActive(true)
                    .build()
        );
        
        List<BmwGenerationSyncRequestDto> generationData = List.of(
            BmwGenerationSyncRequestDto.builder()
                    .code("INT-F40")
                    .seriesCode("INT-1")
                    .name("F40")
                    .yearStart(2019)
                    .yearEnd(null)
                    .bodyCodes(List.of("INT-HATCH"))
                    .isActive(true)
                    .build(),
            BmwGenerationSyncRequestDto.builder()
                    .code("INT-G30")
                    .seriesCode("INT-5")
                    .name("G30")
                    .yearStart(2017)
                    .yearEnd(null)
                    .bodyCodes(List.of("INT-SEDAN"))
                    .isActive(true)
                    .build()
        );
        
        BmwCacheBulkSyncRequestDto bulkRequest = BmwCacheBulkSyncRequestDto.builder()
                .seriesData(seriesData)
                .generationData(generationData)
                .build();

        // When: Perform bulk synchronization through complete service stack
        bmwCacheInternalService.bulkSync(bulkRequest.getSeriesData(), bulkRequest.getGenerationData());

        // Then: All data should be synchronized with transactional integrity
        
        // Verify all series were created
        assertThat(bmwSeriesCacheRepository.findByCode("INT-1")).isPresent();
        assertThat(bmwSeriesCacheRepository.findByCode("INT-5")).isPresent();

        // Verify generations with proper parent relationships
        BmwGenerationCache f40 = bmwGenerationCacheRepository.findByCode("INT-F40").orElseThrow();
        assertThat(f40.getSeriesCache().getCode()).isEqualTo("INT-1");
        assertThat(f40.isCurrentGeneration()).isTrue();
        assertThat(f40.hasBodyCode("INT-HATCH")).isTrue();
        
        BmwGenerationCache g30 = bmwGenerationCacheRepository.findByCode("INT-G30").orElseThrow();
        assertThat(g30.getSeriesCache().getCode()).isEqualTo("INT-5");
        assertThat(g30.isCurrentGeneration()).isTrue();
        assertThat(g30.hasBodyCode("INT-SEDAN")).isTrue();
        
        // Verify data integrity across complete hierarchy
        List<BmwSeriesCacheDto> allSeries = bmwCacheInternalService.getAllSeries();
        assertThat(allSeries).hasSize(3); // testSeries + INT-1 + INT-5
        
        List<BmwGenerationCacheDto> allGenerations = bmwCacheInternalService.getAllGenerations();
        assertThat(allGenerations).hasSize(2); // INT-F40 + INT-G30
    }

    @Test
    @DisplayName("BMW Data Update Synchronization - Cache Consistency Maintenance")
    void shouldMaintainCacheConsistencyOnUpdatesAcrossLayers() {
        // Given: Existing BMW generation
        bmwCacheInternalService.syncGeneration(BmwGenerationSyncRequestDto.builder()
                .code("INT-F30")
                .seriesCode(testSeriesCode)
                .name("F30")
                .yearStart(2012)
                .yearEnd(2018)
                .bodyCodes(List.of("INT-SEDAN"))
                .isActive(true)
                .build());
        
        BmwGenerationCache originalGeneration = bmwGenerationCacheRepository.findByCode("INT-F30").orElseThrow();
        LocalDateTime originalUpdateTime = originalGeneration.getLastUpdated();

        // When: Synchronize with updated generation data
        BmwGenerationCacheDto updatedResult = bmwCacheInternalService.syncGeneration(
            BmwGenerationSyncRequestDto.builder()
                    .code("INT-F30")
                    .seriesCode(testSeriesCode)
                    .name("F30 Updated")
                    .yearStart(2012)
                    .yearEnd(2019) // Extended production
                    .bodyCodes(List.of("INT-SEDAN", "INT-TOURING", "INT-GT"))
                    .isActive(true)
                    .build());

        // Then: Generation should be updated maintaining cache consistency
        assertThat(updatedResult.getName()).isEqualTo("F30 Updated");
        assertThat(updatedResult.getYearEnd()).isEqualTo(2019);
        assertThat(updatedResult.getBodyCodes()).containsExactlyInAnyOrder("INT-SEDAN", "INT-TOURING", "INT-GT");
        
        // Verify database consistency
        BmwGenerationCache updatedGeneration = bmwGenerationCacheRepository.findByCode("INT-F30").orElseThrow();
        assertThat(updatedGeneration.getName()).isEqualTo("F30 Updated");
        assertThat(updatedGeneration.getYearEnd()).isEqualTo(2019);
        assertThat(updatedGeneration.getLastUpdated()).isAfter(originalUpdateTime);
        
        // Verify no duplicates were created
        List<BmwGenerationCache> allF30Generations = bmwGenerationCacheRepository.findAll().stream()
                .filter(g -> g.getCode().equals("INT-F30"))
                .toList();
        assertThat(allF30Generations).hasSize(1);
        
        // Verify cache query consistency
        BmwGenerationCacheDto retrievedGeneration = bmwCacheInternalService.getGenerationByCode("INT-F30");
        assertThat(retrievedGeneration.getName()).isEqualTo("F30 Updated");
        assertThat(retrievedGeneration.getYearEnd()).isEqualTo(2019);
    }
}
