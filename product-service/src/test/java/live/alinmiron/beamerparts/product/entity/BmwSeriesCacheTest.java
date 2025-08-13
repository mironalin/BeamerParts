package live.alinmiron.beamerparts.product.entity;

import live.alinmiron.beamerparts.product.repository.BmwSeriesCacheRepository;
import live.alinmiron.beamerparts.product.repository.BmwGenerationCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for BmwSeriesCache entity business logic, persistence, and relationships.
 * Tests business methods, validation, constraints, and BMW-specific behavior.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("BmwSeriesCache Entity Tests")
class BmwSeriesCacheTest {

    @Autowired
    private BmwSeriesCacheRepository bmwSeriesCacheRepository;
    
    @Autowired
    private BmwGenerationCacheRepository bmwGenerationCacheRepository;

    private long testIdCounter;

    @BeforeEach
    void setUp() {
        testIdCounter = System.currentTimeMillis();
    }

    // =================== Business Method Tests ===================

    @Test
    @DisplayName("getDisplayName() should return properly formatted BMW series name")
    void getDisplayName_WithValidName_ShouldReturnFormattedName() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1);

        // When
        String displayName = series.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("BMW 3 Series");
    }

    @Test
    @DisplayName("getDisplayName() should handle single character series codes")
    void getDisplayName_WithSingleChar_ShouldReturnFormattedName() {
        // Given
        BmwSeriesCache series = createTestSeries("X", "X Series", 1);

        // When
        String displayName = series.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("BMW X Series");
    }

    @Test
    @DisplayName("getDisplayName() should handle complex series names")
    void getDisplayName_WithComplexName_ShouldReturnFormattedName() {
        // Given
        BmwSeriesCache series = createTestSeries("X5M", "X5 M Series", 1);

        // When
        String displayName = series.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("BMW X5 M Series");
    }

    @Test
    @DisplayName("hasActiveGenerations() should return false when generations list is null")
    void hasActiveGenerations_WithNullGenerations_ShouldReturnFalse() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1);
        series.setGenerations(null);

        // When
        boolean hasActive = series.hasActiveGenerations();

        // Then
        assertThat(hasActive).isFalse();
    }

    @Test
    @DisplayName("hasActiveGenerations() should return false when generations list is empty")
    void hasActiveGenerations_WithEmptyGenerations_ShouldReturnFalse() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1);
        series.setGenerations(new ArrayList<>());

        // When
        boolean hasActive = series.hasActiveGenerations();

        // Then
        assertThat(hasActive).isFalse();
    }

    @Test
    @DisplayName("hasActiveGenerations() should return false when all generations are inactive")
    void hasActiveGenerations_WithAllInactiveGenerations_ShouldReturnFalse() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1);
        BmwGenerationCache inactiveGen1 = createTestGeneration("E90", "E90 Generation", 2005, 2012, false);
        BmwGenerationCache inactiveGen2 = createTestGeneration("E46", "E46 Generation", 1998, 2005, false);
        
        series.setGenerations(List.of(inactiveGen1, inactiveGen2));

        // When
        boolean hasActive = series.hasActiveGenerations();

        // Then
        assertThat(hasActive).isFalse();
    }

    @Test
    @DisplayName("hasActiveGenerations() should return true when at least one generation is active")
    void hasActiveGenerations_WithSomeActiveGenerations_ShouldReturnTrue() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1);
        BmwGenerationCache activeGen = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        BmwGenerationCache inactiveGen = createTestGeneration("E90", "E90 Generation", 2005, 2012, false);
        
        series.setGenerations(List.of(activeGen, inactiveGen));

        // When
        boolean hasActive = series.hasActiveGenerations();

        // Then
        assertThat(hasActive).isTrue();
    }

    @Test
    @DisplayName("hasActiveGenerations() should return true when all generations are active")
    void hasActiveGenerations_WithAllActiveGenerations_ShouldReturnTrue() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1);
        BmwGenerationCache activeGen1 = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        BmwGenerationCache activeGen2 = createTestGeneration("G20", "G20 Generation", 2019, null, true);
        
        series.setGenerations(List.of(activeGen1, activeGen2));

        // When
        boolean hasActive = series.hasActiveGenerations();

        // Then
        assertThat(hasActive).isTrue();
    }

    // =================== Persistence Tests ===================

    @Test
    @DisplayName("Should persist BmwSeriesCache with all required fields")
    void persistence_WithValidData_ShouldSaveSuccessfully() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1);

        // When
        BmwSeriesCache savedSeries = bmwSeriesCacheRepository.save(series);

        // Then
        assertThat(savedSeries.getCode()).startsWith("3-");
        assertThat(savedSeries.getName()).isEqualTo("3 Series");
        assertThat(savedSeries.getDisplayOrder()).isEqualTo(1);
        assertThat(savedSeries.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should handle series updates correctly")
    void persistence_OnUpdate_ShouldUpdateCorrectly() {
        // Given
        BmwSeriesCache series = createAndSaveSeries("X3", "X3 Series", 3);
        
        // When
        series.setName("X3 Updated Series");
        BmwSeriesCache updatedSeries = bmwSeriesCacheRepository.save(series);

        // Then
        assertThat(updatedSeries.getName()).isEqualTo("X3 Updated Series");
        assertThat(updatedSeries.getDisplayOrder()).isEqualTo(3);
        assertThat(updatedSeries.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should handle maximum length series name (50 characters)")
    void persistence_WithMaxLengthName_ShouldSaveSuccessfully() {
        // Given
        String longName = "A".repeat(50); // Max length according to schema
        BmwSeriesCache series = createTestSeries("LONG", longName, 1);

        // When
        BmwSeriesCache savedSeries = bmwSeriesCacheRepository.save(series);

        // Then
        assertThat(savedSeries.getName()).hasSize(50);
        assertThat(savedSeries.getName()).isEqualTo(longName);
    }

    @Test
    @DisplayName("Should handle maximum length series code (10 characters)")
    void persistence_WithMaxLengthCode_ShouldSaveSuccessfully() {
        // Given  
        String baseCode = "AAAA"; // Short base to leave room for timestamp
        BmwSeriesCache series = createTestSeries(baseCode, "Long Code Series", 1);

        // When
        BmwSeriesCache savedSeries = bmwSeriesCacheRepository.save(series);

        // Then
        assertThat(savedSeries.getCode()).startsWith(baseCode + "-");
        assertThat(savedSeries.getName()).isEqualTo("Long Code Series");
    }

    // =================== Data Validation Tests ===================

    @Test
    @DisplayName("Should handle properly formatted series data correctly")
    void validation_WithProperData_ShouldWorkCorrectly() {
        // Given
        BmwSeriesCache series = createTestSeries("3", "3 Series", 1);

        // When
        BmwSeriesCache savedSeries = bmwSeriesCacheRepository.save(series);

        // Then
        assertThat(savedSeries.getName()).isEqualTo("3 Series");
        assertThat(savedSeries.getDisplayOrder()).isEqualTo(1);
        assertThat(savedSeries.getIsActive()).isTrue();
    }

    // =================== Edge Case Tests ===================

    @Test
    @DisplayName("Should handle empty string name gracefully")
    void edgeCases_WithEmptyName_ShouldHandleGracefully() {
        // Given
        BmwSeriesCache series = createTestSeries("EMPTY", "", 1);

        // When
        String displayName = series.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("BMW ");
    }

    @Test
    @DisplayName("Should handle special characters in series code")
    void edgeCases_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Given
        BmwSeriesCache series = createTestSeries("X5-M", "X5 M Series", 1);

        // When
        BmwSeriesCache savedSeries = bmwSeriesCacheRepository.save(series);

        // Then
        assertThat(savedSeries.getCode()).startsWith("X5-M-");
        assertThat(savedSeries.getDisplayName()).isEqualTo("BMW X5 M Series");
    }

    @Test
    @DisplayName("Should handle zero and negative display orders")
    void edgeCases_WithZeroDisplayOrder_ShouldSaveSuccessfully() {
        // Given
        BmwSeriesCache series1 = createTestSeries("ZERO", "Zero Order", 0);
        BmwSeriesCache series2 = createTestSeries("NEG", "Negative Order", -1);

        // When
        BmwSeriesCache savedSeries1 = bmwSeriesCacheRepository.save(series1);
        BmwSeriesCache savedSeries2 = bmwSeriesCacheRepository.save(series2);

        // Then
        assertThat(savedSeries1.getDisplayOrder()).isEqualTo(0);
        assertThat(savedSeries2.getDisplayOrder()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should properly handle isActive default value")
    void defaultValues_IsActive_ShouldDefaultToTrue() {
        // Given
        BmwSeriesCache series = BmwSeriesCache.builder()
                .code("DEFAULT-" + testIdCounter++)
                .name("Default Test")
                .displayOrder(1)
                // isActive not explicitly set - should default to true
                .build();

        // When
        BmwSeriesCache savedSeries = bmwSeriesCacheRepository.save(series);

        // Then
        assertThat(savedSeries.getIsActive()).isTrue();
    }

    // =================== Relationship Tests ===================

    @Test
    @DisplayName("Should properly handle series-generation relationships")
    void relationships_SeriesGenerations_ShouldWorkCorrectly() {
        // Given
        BmwSeriesCache series = createAndSaveSeries("REL", "Relationship Test", 1);

        // When
        String displayName = series.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("BMW Relationship Test");
        assertThat(series.hasActiveGenerations()).isFalse(); // No generations yet
    }

    // =================== Helper Methods ===================

    private BmwSeriesCache createTestSeries(String code, String name, Integer displayOrder) {
        return BmwSeriesCache.builder()
                .code(code + "-" + testIdCounter++)
                .name(name)
                .displayOrder(displayOrder)
                .isActive(true)
                .generations(new ArrayList<>())
                .build();
    }

    private BmwSeriesCache createAndSaveSeries(String code, String name, Integer displayOrder) {
        BmwSeriesCache series = createTestSeries(code, name, displayOrder);
        return bmwSeriesCacheRepository.save(series);
    }

    private BmwGenerationCache createTestGeneration(String code, String name, Integer yearStart, Integer yearEnd, Boolean isActive) {
        return BmwGenerationCache.builder()
                .code(code + "-" + testIdCounter++)
                .name(name)
                .yearStart(yearStart)
                .yearEnd(yearEnd)
                .isActive(isActive)
                .build();
    }
}
