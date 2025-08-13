package live.alinmiron.beamerparts.product.service.domain;

import live.alinmiron.beamerparts.product.entity.BmwGenerationCache;
import live.alinmiron.beamerparts.product.entity.BmwSeriesCache;
import live.alinmiron.beamerparts.product.repository.BmwGenerationCacheRepository;
import live.alinmiron.beamerparts.product.repository.BmwSeriesCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for BmwCompatibilityDomainService
 * Tests all business logic and domain rules using real database operations
 * 
 * **KEY LESSONS APPLIED:**
 * - Tests define business logic FIRST, implementation follows
 * - Use real database operations with @Transactional rollback
 * - Unique test data generation to avoid constraint violations
 * - Comprehensive coverage of all business rules and edge cases
 * - Professional error handling and validation testing
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("BmwCompatibilityDomainService Tests")
class BmwCompatibilityDomainServiceTest {

    @Autowired
    private BmwCompatibilityDomainService bmwCompatibilityDomainService;

    @Autowired
    private BmwSeriesCacheRepository bmwSeriesCacheRepository;

    @Autowired
    private BmwGenerationCacheRepository bmwGenerationCacheRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test to ensure isolated state
        bmwGenerationCacheRepository.deleteAll();
        bmwSeriesCacheRepository.deleteAll();
    }

    // =========================
    // BMW SERIES SYNCHRONIZATION TESTS
    // =========================

    @Test
    @DisplayName("Should synchronize BMW series successfully with valid data")
    void synchronizeSeries_WithValidData_ShouldReturnSuccess() {
        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries("3", "3 Series", 1, true);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSeriesCode()).isEqualTo("3");
        assertThat(result.getErrorMessage()).isNull();

        // Verify database state
        Optional<BmwSeriesCache> saved = bmwSeriesCacheRepository.findByCode("3");
        assertThat(saved).isPresent();
        assertThat(saved.get().getName()).isEqualTo("3 Series");
        assertThat(saved.get().getDisplayOrder()).isEqualTo(1);
        assertThat(saved.get().getIsActive()).isTrue();
        assertThat(saved.get().getLastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("Should update existing BMW series successfully")
    void synchronizeSeries_WithExistingSeries_ShouldUpdateSuccessfully() {
        // Given
        BmwSeriesCache existingSeries = createTestSeries("X5", "Original Name", 5, false);

        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries("X5", "Updated X5 Series", 2, true);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSeriesCode()).isEqualTo("X5");

        // Verify update
        BmwSeriesCache updated = bmwSeriesCacheRepository.findByCode("X5").orElseThrow();
        assertThat(updated.getCode()).isEqualTo(existingSeries.getCode()); // Same entity
        assertThat(updated.getName()).isEqualTo("Updated X5 Series");
        assertThat(updated.getDisplayOrder()).isEqualTo(2);
        assertThat(updated.getIsActive()).isTrue();
        assertThat(updated.getLastUpdated()).isAfterOrEqualTo(existingSeries.getLastUpdated());
    }

    @Test
    @DisplayName("Should default isActive to true when null provided")
    void synchronizeSeries_WithNullIsActive_ShouldDefaultToTrue() {
        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries("7", "7 Series", 3, null);

        // Then
        assertThat(result.isSuccess()).isTrue();
        
        BmwSeriesCache saved = bmwSeriesCacheRepository.findByCode("7").orElseThrow();
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should trim whitespace from series code and name")
    void synchronizeSeries_WithWhitespace_ShouldTrimValues() {
        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries("  M  ", "  M Series  ", 4, true);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSeriesCode()).isEqualTo("M");
        
        BmwSeriesCache saved = bmwSeriesCacheRepository.findByCode("M").orElseThrow();
        assertThat(saved.getCode()).isEqualTo("M");
        assertThat(saved.getName()).isEqualTo("M Series");
    }

    @Test
    @DisplayName("Should fail synchronization with null series code")
    void synchronizeSeries_WithNullCode_ShouldReturnFailure() {
        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries(null, "Test Series", 1, true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Series code cannot be empty");
        assertThat(result.getSeriesCode()).isNull();
    }

    @Test
    @DisplayName("Should fail synchronization with empty series code")
    void synchronizeSeries_WithEmptyCode_ShouldReturnFailure() {
        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries("   ", "Test Series", 1, true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Series code cannot be empty");
    }

    @Test
    @DisplayName("Should fail synchronization with null series name")
    void synchronizeSeries_WithNullName_ShouldReturnFailure() {
        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries("TEST", null, 1, true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Series name cannot be empty");
    }

    @Test
    @DisplayName("Should fail synchronization with empty series name")
    void synchronizeSeries_WithEmptyName_ShouldReturnFailure() {
        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries("TEST", "   ", 1, true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Series name cannot be empty");
    }

    @Test
    @DisplayName("Should fail synchronization with null display order")
    void synchronizeSeries_WithNullDisplayOrder_ShouldReturnFailure() {
        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries("TEST", "Test Series", null, true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Display order must be a positive number");
    }

    @Test
    @DisplayName("Should fail synchronization with negative display order")
    void synchronizeSeries_WithNegativeDisplayOrder_ShouldReturnFailure() {
        // When
        BmwCompatibilityDomainService.BmwSeriesSyncResult result = 
                bmwCompatibilityDomainService.synchronizeSeries("TEST", "Test Series", -1, true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Display order must be a positive number");
    }

    // =========================
    // BMW GENERATION SYNCHRONIZATION TESTS
    // =========================

    @Test
    @DisplayName("Should synchronize BMW generation successfully with valid data")
    void synchronizeGeneration_WithValidData_ShouldReturnSuccess() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "F30", "3", "F30 3 Series", 2012, 2019, 
                    Arrays.asList("F30", "F31", "F34"), true);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGenerationCode()).isEqualTo("F30");
        assertThat(result.getSeriesCode()).isEqualTo("3");
        assertThat(result.getErrorMessage()).isNull();

        // Verify database state
        Optional<BmwGenerationCache> saved = bmwGenerationCacheRepository.findByCode("F30");
        assertThat(saved).isPresent();
        assertThat(saved.get().getName()).isEqualTo("F30 3 Series");
        assertThat(saved.get().getYearStart()).isEqualTo(2012);
        assertThat(saved.get().getYearEnd()).isEqualTo(2019);
        assertThat(saved.get().getBodyCodes()).containsExactly("F30", "F31", "F34");
        assertThat(saved.get().getIsActive()).isTrue();
        assertThat(saved.get().getSeriesCache()).isEqualTo(series);
    }

    @Test
    @DisplayName("Should synchronize generation with null year end (current generation)")
    void synchronizeGeneration_WithNullYearEnd_ShouldReturnSuccess() {
        // Given
        createTestSeries("X5", "X5 Series", 2, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "G05", "X5", "G05 X5", 2018, null, 
                    Arrays.asList("G05"), true);

        // Then
        assertThat(result.isSuccess()).isTrue();
        
        BmwGenerationCache saved = bmwGenerationCacheRepository.findByCode("G05").orElseThrow();
        assertThat(saved.getYearEnd()).isNull();
    }

    @Test
    @DisplayName("Should synchronize generation with empty body codes")
    void synchronizeGeneration_WithEmptyBodyCodes_ShouldReturnSuccess() {
        // Given
        createTestSeries("1", "1 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "F20", "1", "F20 1 Series", 2011, 2019, 
                    Arrays.asList(), true);

        // Then
        assertThat(result.isSuccess()).isTrue();
        
        BmwGenerationCache saved = bmwGenerationCacheRepository.findByCode("F20").orElseThrow();
        assertThat(saved.getBodyCodes()).isNull();
    }

    @Test
    @DisplayName("Should synchronize generation with null body codes")
    void synchronizeGeneration_WithNullBodyCodes_ShouldReturnSuccess() {
        // Given
        createTestSeries("1", "1 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "F21", "1", "F21 1 Series", 2015, 2021, 
                    null, true);

        // Then
        assertThat(result.isSuccess()).isTrue();
        
        BmwGenerationCache saved = bmwGenerationCacheRepository.findByCode("F21").orElseThrow();
        assertThat(saved.getBodyCodes()).isNull();
    }

    @Test
    @DisplayName("Should update existing BMW generation successfully")
    void synchronizeGeneration_WithExistingGeneration_ShouldUpdateSuccessfully() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1, true);
        BmwGenerationCache existingGen = createTestGeneration("G20", series, "Original Name", 2019, null);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "G20", "3", "Updated G20 3 Series", 2019, 2025, 
                    Arrays.asList("G20", "G21"), true);

        // Then
        assertThat(result.isSuccess()).isTrue();
        
        BmwGenerationCache updated = bmwGenerationCacheRepository.findByCode("G20").orElseThrow();
        assertThat(updated.getCode()).isEqualTo(existingGen.getCode()); // Same entity
        assertThat(updated.getName()).isEqualTo("Updated G20 3 Series");
        assertThat(updated.getYearEnd()).isEqualTo(2025);
        assertThat(updated.getBodyCodes()).containsExactly("G20", "G21");
    }

    @Test
    @DisplayName("Should default isActive to true when null provided for generation")
    void synchronizeGeneration_WithNullIsActive_ShouldDefaultToTrue() {
        // Given
        createTestSeries("X3", "X3 Series", 3, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "G01", "X3", "G01 X3", 2017, null, 
                    Arrays.asList("G01"), null);

        // Then
        assertThat(result.isSuccess()).isTrue();
        
        BmwGenerationCache saved = bmwGenerationCacheRepository.findByCode("G01").orElseThrow();
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should trim whitespace from generation code and name")
    void synchronizeGeneration_WithWhitespace_ShouldTrimValues() {
        // Given
        createTestSeries("5", "5 Series", 2, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "  G30  ", "5", "  G30 5 Series  ", 2016, null, 
                    Arrays.asList("G30"), true);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGenerationCode()).isEqualTo("G30");
        
        BmwGenerationCache saved = bmwGenerationCacheRepository.findByCode("G30").orElseThrow();
        assertThat(saved.getCode()).isEqualTo("G30");
        assertThat(saved.getName()).isEqualTo("G30 5 Series");
    }

    @Test
    @DisplayName("Should fail generation synchronization with null generation code")
    void synchronizeGeneration_WithNullCode_ShouldReturnFailure() {
        // Given
        createTestSeries("3", "3 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    null, "3", "Test Gen", 2020, null, 
                    Arrays.asList("TEST"), true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Generation code cannot be empty");
    }

    @Test
    @DisplayName("Should fail generation synchronization with empty generation code")
    void synchronizeGeneration_WithEmptyCode_ShouldReturnFailure() {
        // Given
        createTestSeries("3", "3 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "   ", "3", "Test Gen", 2020, null, 
                    Arrays.asList("TEST"), true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Generation code cannot be empty");
    }

    @Test
    @DisplayName("Should fail generation synchronization with null name")
    void synchronizeGeneration_WithNullName_ShouldReturnFailure() {
        // Given
        createTestSeries("3", "3 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "TEST", "3", null, 2020, null, 
                    Arrays.asList("TEST"), true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Generation name cannot be empty");
    }

    @Test
    @DisplayName("Should fail generation synchronization with empty name")
    void synchronizeGeneration_WithEmptyName_ShouldReturnFailure() {
        // Given
        createTestSeries("3", "3 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "TEST", "3", "   ", 2020, null, 
                    Arrays.asList("TEST"), true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Generation name cannot be empty");
    }

    @Test
    @DisplayName("Should fail generation synchronization with null year start")
    void synchronizeGeneration_WithNullYearStart_ShouldReturnFailure() {
        // Given
        createTestSeries("3", "3 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "TEST", "3", "Test Gen", null, null, 
                    Arrays.asList("TEST"), true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Year start must be a valid year");
    }

    @Test
    @DisplayName("Should fail generation synchronization with invalid year start (too early)")
    void synchronizeGeneration_WithTooEarlyYearStart_ShouldReturnFailure() {
        // Given
        createTestSeries("3", "3 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "TEST", "3", "Test Gen", 1899, null, 
                    Arrays.asList("TEST"), true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Year start must be a valid year");
    }

    @Test
    @DisplayName("Should fail generation synchronization with invalid year start (too late)")
    void synchronizeGeneration_WithTooLateYearStart_ShouldReturnFailure() {
        // Given
        createTestSeries("3", "3 Series", 1, true);
        int futureYear = LocalDateTime.now().getYear() + 10;

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "TEST", "3", "Test Gen", futureYear, null, 
                    Arrays.asList("TEST"), true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Year start must be a valid year");
    }

    @Test
    @DisplayName("Should fail generation synchronization with year end before year start")
    void synchronizeGeneration_WithYearEndBeforeYearStart_ShouldReturnFailure() {
        // Given
        createTestSeries("3", "3 Series", 1, true);

        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "TEST", "3", "Test Gen", 2020, 2019, 
                    Arrays.asList("TEST"), true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Year end cannot be before year start");
    }

    @Test
    @DisplayName("Should fail generation synchronization with non-existent parent series")
    void synchronizeGeneration_WithNonExistentSeries_ShouldReturnFailure() {
        // When
        BmwCompatibilityDomainService.BmwGenerationSyncResult result = 
                bmwCompatibilityDomainService.synchronizeGeneration(
                    "TEST", "NONEXISTENT", "Test Gen", 2020, null, 
                    Arrays.asList("TEST"), true);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Parent series not found: NONEXISTENT");
    }

    // =========================
    // BMW SERIES QUERY TESTS
    // =========================

    @Test
    @DisplayName("Should get all active BMW series ordered by display order")
    void getAllActiveSeries_WithActiveSeriesData_ShouldReturnOrderedList() {
        // Given
        createTestSeries("X5", "X5 Series", 3, true);
        createTestSeries("3", "3 Series", 1, true);
        createTestSeries("5", "5 Series", 2, true);
        createTestSeries("Z4", "Z4 Series", 4, false); // Inactive

        // When
        List<BmwCompatibilityDomainService.BmwSeriesInfo> result = 
                bmwCompatibilityDomainService.getAllActiveSeries();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(BmwCompatibilityDomainService.BmwSeriesInfo::getCode)
                .containsExactly("3", "5", "X5"); // Ordered by displayOrder
        assertThat(result)
                .extracting(BmwCompatibilityDomainService.BmwSeriesInfo::getIsActive)
                .allMatch(isActive -> isActive);
    }

    @Test
    @DisplayName("Should return empty list when no active series exist")
    void getAllActiveSeries_WithNoActiveSeriesData_ShouldReturnEmptyList() {
        // Given
        createTestSeries("Z4", "Z4 Series", 1, false); // Inactive only

        // When
        List<BmwCompatibilityDomainService.BmwSeriesInfo> result = 
                bmwCompatibilityDomainService.getAllActiveSeries();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get generations for specific series")
    void getGenerationsForSeries_WithValidSeriesCode_ShouldReturnGenerations() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1, true);
        createTestGeneration("E90", series, "E90 3 Series", 2005, 2012);
        createTestGeneration("F30", series, "F30 3 Series", 2012, 2019);
        createTestGeneration("G20", series, "G20 3 Series", 2019, null);
        
        // Different series generation (should not be included)
        BmwSeriesCache otherSeries = createTestSeries("5", "5 Series", 2, true);
        createTestGeneration("G30", otherSeries, "G30 5 Series", 2016, null);

        // When
        List<BmwCompatibilityDomainService.BmwGenerationInfo> result = 
                bmwCompatibilityDomainService.getGenerationsForSeries("3");

        // Then
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(BmwCompatibilityDomainService.BmwGenerationInfo::getCode)
                .containsExactlyInAnyOrder("E90", "F30", "G20");
        assertThat(result)
                .extracting(BmwCompatibilityDomainService.BmwGenerationInfo::getSeriesCode)
                .allMatch(code -> code.equals("3"));
    }

    @Test
    @DisplayName("Should return empty list for non-existent series")
    void getGenerationsForSeries_WithNonExistentSeries_ShouldReturnEmptyList() {
        // When
        List<BmwCompatibilityDomainService.BmwGenerationInfo> result = 
                bmwCompatibilityDomainService.getGenerationsForSeries("NONEXISTENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for null series code")
    void getGenerationsForSeries_WithNullSeriesCode_ShouldReturnEmptyList() {
        // When
        List<BmwCompatibilityDomainService.BmwGenerationInfo> result = 
                bmwCompatibilityDomainService.getGenerationsForSeries(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for empty series code")
    void getGenerationsForSeries_WithEmptySeriesCode_ShouldReturnEmptyList() {
        // When
        List<BmwCompatibilityDomainService.BmwGenerationInfo> result = 
                bmwCompatibilityDomainService.getGenerationsForSeries("   ");

        // Then
        assertThat(result).isEmpty();
    }

    // =========================
    // GENERATION VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should validate active generation as compatible")
    void isGenerationValidForCompatibility_WithActiveGeneration_ShouldReturnTrue() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1, true);
        createTestGeneration("F30", series, "F30 3 Series", 2012, 2019, true);

        // When
        boolean result = bmwCompatibilityDomainService.isGenerationValidForCompatibility("F30");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate inactive generation as incompatible")
    void isGenerationValidForCompatibility_WithInactiveGeneration_ShouldReturnFalse() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1, true);
        createTestGeneration("E90", series, "E90 3 Series", 2005, 2012, false);

        // When
        boolean result = bmwCompatibilityDomainService.isGenerationValidForCompatibility("E90");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for non-existent generation")
    void isGenerationValidForCompatibility_WithNonExistentGeneration_ShouldReturnFalse() {
        // When
        boolean result = bmwCompatibilityDomainService.isGenerationValidForCompatibility("NONEXISTENT");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for null generation code")
    void isGenerationValidForCompatibility_WithNullCode_ShouldReturnFalse() {
        // When
        boolean result = bmwCompatibilityDomainService.isGenerationValidForCompatibility(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for empty generation code")
    void isGenerationValidForCompatibility_WithEmptyCode_ShouldReturnFalse() {
        // When
        boolean result = bmwCompatibilityDomainService.isGenerationValidForCompatibility("   ");

        // Then
        assertThat(result).isFalse();
    }

    // =========================
    // BULK SYNCHRONIZATION TESTS
    // =========================

    @Test
    @DisplayName("Should perform bulk synchronization successfully")
    void performBulkSync_WithValidData_ShouldReturnSuccess() {
        // Given
        List<BmwCompatibilityDomainService.BmwSeriesSyncData> seriesData = Arrays.asList(
            new BmwCompatibilityDomainService.BmwSeriesSyncData("3", "3 Series", 1, true),
            new BmwCompatibilityDomainService.BmwSeriesSyncData("5", "5 Series", 2, true),
            new BmwCompatibilityDomainService.BmwSeriesSyncData("X5", "X5 Series", 3, true)
        );

        List<BmwCompatibilityDomainService.BmwGenerationSyncData> generationData = Arrays.asList(
            new BmwCompatibilityDomainService.BmwGenerationSyncData("F30", "3", "F30 3 Series", 2012, 2019, Arrays.asList("F30"), true),
            new BmwCompatibilityDomainService.BmwGenerationSyncData("G30", "5", "G30 5 Series", 2016, null, Arrays.asList("G30"), true)
        );

        // When
        BmwCompatibilityDomainService.BulkSyncResult result = 
                bmwCompatibilityDomainService.performBulkSync(seriesData, generationData);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSeriesSynced()).isEqualTo(3);
        assertThat(result.getGenerationsSynced()).isEqualTo(2);
        assertThat(result.getErrorMessage()).isNull();

        // Verify database state
        assertThat(bmwSeriesCacheRepository.count()).isEqualTo(3);
        assertThat(bmwGenerationCacheRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle partial failures in bulk synchronization gracefully")
    void performBulkSync_WithPartialFailures_ShouldContinueAndReturnPartialSuccess() {
        // Given
        List<BmwCompatibilityDomainService.BmwSeriesSyncData> seriesData = Arrays.asList(
            new BmwCompatibilityDomainService.BmwSeriesSyncData("3", "3 Series", 1, true),
            new BmwCompatibilityDomainService.BmwSeriesSyncData(null, "Invalid Series", 2, true), // Invalid
            new BmwCompatibilityDomainService.BmwSeriesSyncData("X5", "X5 Series", 3, true)
        );

        List<BmwCompatibilityDomainService.BmwGenerationSyncData> generationData = Arrays.asList(
            new BmwCompatibilityDomainService.BmwGenerationSyncData("F30", "3", "F30 3 Series", 2012, 2019, Arrays.asList("F30"), true),
            new BmwCompatibilityDomainService.BmwGenerationSyncData("INVALID", "NONEXISTENT", "Invalid Gen", 2020, null, Arrays.asList("INVALID"), true) // Invalid series
        );

        // When
        BmwCompatibilityDomainService.BulkSyncResult result = 
                bmwCompatibilityDomainService.performBulkSync(seriesData, generationData);

        // Then
        assertThat(result.isSuccess()).isTrue(); // Overall operation succeeds
        assertThat(result.getSeriesSynced()).isEqualTo(2); // Only valid series synced
        assertThat(result.getGenerationsSynced()).isEqualTo(1); // Only valid generation synced

        // Verify database state
        assertThat(bmwSeriesCacheRepository.count()).isEqualTo(2);
        assertThat(bmwGenerationCacheRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should sync series before generations in bulk operation")
    void performBulkSync_ShouldSyncSeriesBeforeGenerations() {
        // Given - Generation references series that will be created in the same bulk operation
        List<BmwCompatibilityDomainService.BmwSeriesSyncData> seriesData = Arrays.asList(
            new BmwCompatibilityDomainService.BmwSeriesSyncData("NEW", "New Series", 1, true)
        );

        List<BmwCompatibilityDomainService.BmwGenerationSyncData> generationData = Arrays.asList(
            new BmwCompatibilityDomainService.BmwGenerationSyncData("NEW01", "NEW", "New Gen", 2020, null, Arrays.asList("NEW01"), true)
        );

        // When
        BmwCompatibilityDomainService.BulkSyncResult result = 
                bmwCompatibilityDomainService.performBulkSync(seriesData, generationData);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSeriesSynced()).isEqualTo(1);
        assertThat(result.getGenerationsSynced()).isEqualTo(1);

        // Verify the generation is correctly linked to the series
        BmwGenerationCache generation = bmwGenerationCacheRepository.findByCode("NEW01").orElseThrow();
        assertThat(generation.getSeriesCache().getCode()).isEqualTo("NEW");
    }

    // =========================
    // REPOSITORY QUERY TESTS (Lower-level operations)
    // =========================

    @Test
    @DisplayName("Should find all BMW series")
    void findAllBmwSeries_WithExistingData_ShouldReturnAllSeries() {
        // Given
        createTestSeries("3", "3 Series", 1, true);
        createTestSeries("5", "5 Series", 2, false);

        // When
        List<BmwSeriesCache> result = bmwCompatibilityDomainService.findAllBmwSeries();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(BmwSeriesCache::getCode)
                .containsExactlyInAnyOrder("3", "5");
    }

    @Test
    @DisplayName("Should find only active BMW series")
    void findActiveBmwSeries_WithMixedData_ShouldReturnOnlyActive() {
        // Given
        createTestSeries("3", "3 Series", 1, true);
        createTestSeries("5", "5 Series", 2, false);
        createTestSeries("X5", "X5 Series", 3, true);

        // When
        List<BmwSeriesCache> result = bmwCompatibilityDomainService.findActiveBmwSeries();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(BmwSeriesCache::getCode)
                .containsExactlyInAnyOrder("3", "X5");
        assertThat(result)
                .extracting(BmwSeriesCache::getIsActive)
                .allMatch(isActive -> isActive);
    }

    @Test
    @DisplayName("Should find BMW series by code")
    void findBmwSeriesByCode_WithExistingCode_ShouldReturnSeries() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1, true);

        // When
        Optional<BmwSeriesCache> result = bmwCompatibilityDomainService.findBmwSeriesByCode("3");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(series);
    }

    @Test
    @DisplayName("Should return empty for non-existent series code")
    void findBmwSeriesByCode_WithNonExistentCode_ShouldReturnEmpty() {
        // When
        Optional<BmwSeriesCache> result = bmwCompatibilityDomainService.findBmwSeriesByCode("NONEXISTENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should search BMW series by name case-insensitively")
    void searchBmwSeriesByName_WithPartialName_ShouldReturnMatches() {
        // Given
        createTestSeries("3", "3 Series Sport", 1, true);
        createTestSeries("5", "5 Series Luxury", 2, true);
        createTestSeries("X5", "X5 SUV", 3, true);

        // When
        List<BmwSeriesCache> result = bmwCompatibilityDomainService.searchBmwSeriesByName("series");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(BmwSeriesCache::getCode)
                .containsExactlyInAnyOrder("3", "5");
    }

    @Test
    @DisplayName("Should find all BMW generations")
    void findAllBmwGenerations_WithExistingData_ShouldReturnAllGenerations() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1, true);
        createTestGeneration("F30", series, "F30 3 Series", 2012, 2019);
        createTestGeneration("G20", series, "G20 3 Series", 2019, null);

        // When
        List<BmwGenerationCache> result = bmwCompatibilityDomainService.findAllBmwGenerations();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(BmwGenerationCache::getCode)
                .containsExactlyInAnyOrder("F30", "G20");
    }

    @Test
    @DisplayName("Should find only active BMW generations")
    void findActiveBmwGenerations_WithMixedData_ShouldReturnOnlyActive() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1, true);
        createTestGeneration("F30", series, "F30 3 Series", 2012, 2019, true);
        createTestGeneration("E90", series, "E90 3 Series", 2005, 2012, false);
        createTestGeneration("G20", series, "G20 3 Series", 2019, null, true);

        // When
        List<BmwGenerationCache> result = bmwCompatibilityDomainService.findActiveBmwGenerations();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(BmwGenerationCache::getCode)
                .containsExactlyInAnyOrder("F30", "G20");
        assertThat(result)
                .extracting(BmwGenerationCache::getIsActive)
                .allMatch(isActive -> isActive);
    }

    @Test
    @DisplayName("Should find BMW generation by code")
    void findBmwGenerationByCode_WithExistingCode_ShouldReturnGeneration() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1, true);
        BmwGenerationCache generation = createTestGeneration("F30", series, "F30 3 Series", 2012, 2019);

        // When
        Optional<BmwGenerationCache> result = bmwCompatibilityDomainService.findBmwGenerationByCode("F30");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(generation);
    }

    @Test
    @DisplayName("Should return empty for non-existent generation code")
    void findBmwGenerationByCode_WithNonExistentCode_ShouldReturnEmpty() {
        // When
        Optional<BmwGenerationCache> result = bmwCompatibilityDomainService.findBmwGenerationByCode("NONEXISTENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find BMW generations by series code")
    void findBmwGenerationsBySeriesCode_WithExistingSeriesCode_ShouldReturnGenerations() {
        // Given
        BmwSeriesCache series3 = createTestSeries("3", "3 Series", 1, true);
        BmwSeriesCache series5 = createTestSeries("5", "5 Series", 2, true);
        
        createTestGeneration("F30", series3, "F30 3 Series", 2012, 2019);
        createTestGeneration("G20", series3, "G20 3 Series", 2019, null);
        createTestGeneration("G30", series5, "G30 5 Series", 2016, null);

        // When
        List<BmwGenerationCache> result = bmwCompatibilityDomainService.findBmwGenerationsBySeriesCode("3");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(BmwGenerationCache::getCode)
                .containsExactlyInAnyOrder("F30", "G20");
        assertThat(result)
                .extracting(generation -> generation.getSeriesCache().getCode())
                .allMatch(code -> code.equals("3"));
    }

    // =========================
    // HELPER METHODS
    // =========================

    private BmwSeriesCache createTestSeries(String code, String name, Integer displayOrder, Boolean isActive) {
        BmwSeriesCache series = BmwSeriesCache.builder()
                .code(code)
                .name(name)
                .displayOrder(displayOrder)
                .isActive(isActive)
                .lastUpdated(LocalDateTime.now())
                .build();
        return bmwSeriesCacheRepository.save(series);
    }

    private BmwGenerationCache createTestGeneration(String code, BmwSeriesCache series, String name, 
                                                  Integer yearStart, Integer yearEnd) {
        return createTestGeneration(code, series, name, yearStart, yearEnd, true);
    }

    private BmwGenerationCache createTestGeneration(String code, BmwSeriesCache series, String name, 
                                                  Integer yearStart, Integer yearEnd, Boolean isActive) {
        BmwGenerationCache generation = BmwGenerationCache.builder()
                .code(code)
                .seriesCache(series)
                .name(name)
                .yearStart(yearStart)
                .yearEnd(yearEnd)
                .isActive(isActive)
                .lastUpdated(LocalDateTime.now())
                .build();
        return bmwGenerationCacheRepository.save(generation);
    }
}
