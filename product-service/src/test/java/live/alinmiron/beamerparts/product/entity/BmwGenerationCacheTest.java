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

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for BmwGenerationCache entity business logic, persistence, and relationships.
 * Tests business methods, validation, constraints, and BMW generation-specific behavior.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("BmwGenerationCache Entity Tests")
class BmwGenerationCacheTest {

    @Autowired
    private BmwGenerationCacheRepository bmwGenerationCacheRepository;
    
    @Autowired
    private BmwSeriesCacheRepository bmwSeriesCacheRepository;

    private long testIdCounter;
    private BmwSeriesCache testSeries;

    @BeforeEach
    void setUp() {
        testIdCounter = System.currentTimeMillis();
        testSeries = createAndSaveSeries("3", "3 Series");
    }

    // =================== Business Method Tests ===================

    @Test
    @DisplayName("getDisplayName() should return properly formatted generation name with year range")
    void getDisplayName_WithValidData_ShouldReturnFormattedName() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);

        // When
        String displayName = generation.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("3 Series F30 Generation (2012-2019)");
    }

    @Test
    @DisplayName("getDisplayName() should show 'present' for current generation")
    void getDisplayName_WithNullYearEnd_ShouldShowPresent() {
        // Given
        BmwGenerationCache generation = createTestGeneration("G20", "G20 Generation", 2019, null, true);

        // When
        String displayName = generation.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("3 Series G20 Generation (2019-present)");
    }

    @Test
    @DisplayName("getYearRange() should return proper range for completed generation")
    void getYearRange_WithYearEnd_ShouldReturnRange() {
        // Given
        BmwGenerationCache generation = createTestGeneration("E90", "E90 Generation", 2005, 2012, true);

        // When
        String yearRange = generation.getYearRange();

        // Then
        assertThat(yearRange).isEqualTo("2005-2012");
    }

    @Test
    @DisplayName("getYearRange() should return 'present' for current generation")
    void getYearRange_WithNullYearEnd_ShouldReturnPresent() {
        // Given
        BmwGenerationCache generation = createTestGeneration("G20", "G20 Generation", 2019, null, true);

        // When
        String yearRange = generation.getYearRange();

        // Then
        assertThat(yearRange).isEqualTo("2019-present");
    }

    @Test
    @DisplayName("isCurrentGeneration() should return true when yearEnd is null")
    void isCurrentGeneration_WithNullYearEnd_ShouldReturnTrue() {
        // Given
        BmwGenerationCache generation = createTestGeneration("G20", "G20 Generation", 2019, null, true);

        // When
        boolean isCurrent = generation.isCurrentGeneration();

        // Then
        assertThat(isCurrent).isTrue();
    }

    @Test
    @DisplayName("isCurrentGeneration() should return false when yearEnd is set")
    void isCurrentGeneration_WithYearEnd_ShouldReturnFalse() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);

        // When
        boolean isCurrent = generation.isCurrentGeneration();

        // Then
        assertThat(isCurrent).isFalse();
    }

    @Test
    @DisplayName("hasBodyCode() should return false when bodyCodes is null")
    void hasBodyCode_WithNullBodyCodes_ShouldReturnFalse() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        generation.setBodyCodes(null);

        // When
        boolean hasCode = generation.hasBodyCode("335i");

        // Then
        assertThat(hasCode).isFalse();
    }

    @Test
    @DisplayName("hasBodyCode() should return false when searching for null bodyCode")
    void hasBodyCode_WithNullSearchCode_ShouldReturnFalse() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        generation.setBodyCodes(new String[]{"320i", "328i", "335i"});

        // When
        boolean hasCode = generation.hasBodyCode(null);

        // Then
        assertThat(hasCode).isFalse();
    }

    @Test
    @DisplayName("hasBodyCode() should return true for exact match")
    void hasBodyCode_WithExactMatch_ShouldReturnTrue() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        generation.setBodyCodes(new String[]{"320i", "328i", "335i"});

        // When
        boolean hasCode = generation.hasBodyCode("335i");

        // Then
        assertThat(hasCode).isTrue();
    }

    @Test
    @DisplayName("hasBodyCode() should return true for case-insensitive match")
    void hasBodyCode_WithCaseInsensitiveMatch_ShouldReturnTrue() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        generation.setBodyCodes(new String[]{"320i", "328i", "335i"});

        // When
        boolean hasCode1 = generation.hasBodyCode("335I"); // uppercase
        boolean hasCode2 = generation.hasBodyCode("320I"); // uppercase
        boolean hasCode3 = generation.hasBodyCode("328I"); // uppercase

        // Then
        assertThat(hasCode1).isTrue();
        assertThat(hasCode2).isTrue();
        assertThat(hasCode3).isTrue();
    }

    @Test
    @DisplayName("hasBodyCode() should return false for non-existent code")
    void hasBodyCode_WithNonExistentCode_ShouldReturnFalse() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        generation.setBodyCodes(new String[]{"320i", "328i", "335i"});

        // When
        boolean hasCode = generation.hasBodyCode("340i");

        // Then
        assertThat(hasCode).isFalse();
    }

    @Test
    @DisplayName("hasBodyCode() should handle empty bodyCodes array")
    void hasBodyCode_WithEmptyBodyCodes_ShouldReturnFalse() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        generation.setBodyCodes(new String[]{});

        // When
        boolean hasCode = generation.hasBodyCode("335i");

        // Then
        assertThat(hasCode).isFalse();
    }

    @Test
    @DisplayName("hasBodyCode() should handle single element array")
    void hasBodyCode_WithSingleElement_ShouldMatchCorrectly() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        generation.setBodyCodes(new String[]{"335i"});

        // When
        boolean hasMatchingCode = generation.hasBodyCode("335i");
        boolean hasNonMatchingCode = generation.hasBodyCode("320i");

        // Then
        assertThat(hasMatchingCode).isTrue();
        assertThat(hasNonMatchingCode).isFalse();
    }

    // =================== Persistence Tests ===================

    @Test
    @DisplayName("Should persist BmwGenerationCache with all required fields")
    void persistence_WithValidData_ShouldSaveSuccessfully() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getCode()).startsWith("F30");
        assertThat(savedGeneration.getName()).isEqualTo("F30 Generation");
        assertThat(savedGeneration.getYearStart()).isEqualTo(2012);
        assertThat(savedGeneration.getYearEnd()).isEqualTo(2019);
        assertThat(savedGeneration.getIsActive()).isTrue();
        assertThat(savedGeneration.getSeriesCache()).isEqualTo(testSeries);
    }

    @Test
    @DisplayName("Should handle generation updates correctly")
    void persistence_OnUpdate_ShouldUpdateCorrectly() {
        // Given
        BmwGenerationCache generation = createAndSaveGeneration("E90", "E90 Generation", 2005, 2012, true);

        // When
        generation.setName("E90 Updated Generation");
        BmwGenerationCache updatedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(updatedGeneration.getName()).isEqualTo("E90 Updated Generation");
        assertThat(updatedGeneration.getYearStart()).isEqualTo(2005);
        assertThat(updatedGeneration.getYearEnd()).isEqualTo(2012);
        assertThat(updatedGeneration.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should handle maximum length generation name (100 characters)")
    void persistence_WithMaxLengthName_ShouldSaveSuccessfully() {
        // Given
        String longName = "A".repeat(100); // Max length according to schema
        BmwGenerationCache generation = createTestGeneration("LONG", longName, 2020, null, true);

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getName()).hasSize(100);
        assertThat(savedGeneration.getName()).isEqualTo(longName);
    }

    @Test
    @DisplayName("Should handle maximum length generation code (20 characters)")
    void persistence_WithMaxLengthCode_ShouldSaveSuccessfully() {
        // Given
        String longCode = "A".repeat(15); // Leave room for timestamp suffix
        BmwGenerationCache generation = createTestGeneration(longCode, "Long Code Generation", 2020, null, true);

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getCode()).startsWith(longCode);
    }

    @Test
    @DisplayName("Should handle bodyCodes array persistence")
    void persistence_WithBodyCodes_ShouldSaveSuccessfully() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);
        generation.setBodyCodes(new String[]{"320i", "328i", "330i", "335i", "340i"});

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getBodyCodes()).containsExactly("320i", "328i", "330i", "335i", "340i");
    }

    // =================== Data Validation Tests ===================

    @Test
    @DisplayName("Should handle properly formatted generation data correctly")
    void validation_WithProperData_ShouldWorkCorrectly() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30", "F30 Generation", 2012, 2019, true);

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getName()).isEqualTo("F30 Generation");
        assertThat(savedGeneration.getYearStart()).isEqualTo(2012);
        assertThat(savedGeneration.getYearEnd()).isEqualTo(2019);
        assertThat(savedGeneration.getIsActive()).isTrue();
        assertThat(savedGeneration.getSeriesCache()).isEqualTo(testSeries);
    }

    // =================== Edge Case Tests ===================

    @Test
    @DisplayName("Should handle years far in the past")
    void edgeCases_WithOldYears_ShouldHandleCorrectly() {
        // Given
        BmwGenerationCache generation = createTestGeneration("E21", "E21 Generation", 1975, 1983, false);

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getYearStart()).isEqualTo(1975);
        assertThat(savedGeneration.getYearEnd()).isEqualTo(1983);
        assertThat(savedGeneration.getYearRange()).isEqualTo("1975-1983");
        assertThat(savedGeneration.isCurrentGeneration()).isFalse();
    }

    @Test
    @DisplayName("Should handle years in the future")
    void edgeCases_WithFutureYears_ShouldHandleCorrectly() {
        // Given
        BmwGenerationCache generation = createTestGeneration("FUTURE", "Future Generation", 2030, 2040, true);

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getYearStart()).isEqualTo(2030);
        assertThat(savedGeneration.getYearEnd()).isEqualTo(2040);
        assertThat(savedGeneration.getYearRange()).isEqualTo("2030-2040");
        assertThat(savedGeneration.isCurrentGeneration()).isFalse();
    }

    @Test
    @DisplayName("Should handle empty string name gracefully")
    void edgeCases_WithEmptyName_ShouldHandleGracefully() {
        // Given
        BmwGenerationCache generation = createTestGeneration("EMPTY", "", 2020, null, true);

        // When
        String displayName = generation.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("3 Series  (2020-present)");
    }

    @Test
    @DisplayName("Should handle special characters in generation code")
    void edgeCases_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Given
        BmwGenerationCache generation = createTestGeneration("F30-LCI", "F30 LCI Generation", 2015, 2019, true);

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getCode()).startsWith("F30-LCI");
        assertThat(savedGeneration.getDisplayName()).contains("F30 LCI Generation");
    }

    @Test
    @DisplayName("Should properly handle isActive default value")
    void defaultValues_IsActive_ShouldDefaultToTrue() {
        // Given
        BmwGenerationCache generation = BmwGenerationCache.builder()
                .code("DEFAULT-" + testIdCounter++)
                .seriesCache(testSeries)
                .name("Default Test")
                .yearStart(2020)
                // isActive not explicitly set - should default to true
                .build();

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should handle null yearEnd correctly (current generation)")
    void nullValues_YearEnd_ShouldAllowNull() {
        // Given
        BmwGenerationCache generation = createTestGeneration("CURRENT", "Current Generation", 2023, null, true);

        // When
        BmwGenerationCache savedGeneration = bmwGenerationCacheRepository.save(generation);

        // Then
        assertThat(savedGeneration.getYearEnd()).isNull();
        assertThat(savedGeneration.isCurrentGeneration()).isTrue();
        assertThat(savedGeneration.getYearRange()).isEqualTo("2023-present");
    }

    // =================== Helper Methods ===================

    private BmwSeriesCache createAndSaveSeries(String code, String name) {
        BmwSeriesCache series = BmwSeriesCache.builder()
                .code(code + "-" + testIdCounter++)
                .name(name)
                .displayOrder(1)
                .isActive(true)
                .build();
        return bmwSeriesCacheRepository.save(series);
    }

    private BmwGenerationCache createTestGeneration(String code, String name, Integer yearStart, Integer yearEnd, Boolean isActive) {
        return BmwGenerationCache.builder()
                .code(code + "-" + testIdCounter++)
                .seriesCache(testSeries)
                .name(name)
                .yearStart(yearStart)
                .yearEnd(yearEnd)
                .isActive(isActive)
                .build();
    }

    private BmwGenerationCache createAndSaveGeneration(String code, String name, Integer yearStart, Integer yearEnd, Boolean isActive) {
        BmwGenerationCache generation = createTestGeneration(code, name, yearStart, yearEnd, isActive);
        return bmwGenerationCacheRepository.save(generation);
    }
}
