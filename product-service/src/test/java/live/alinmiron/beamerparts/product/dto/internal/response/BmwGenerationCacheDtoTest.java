package live.alinmiron.beamerparts.product.dto.internal.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for BmwGenerationCacheDto business logic and behavior.
 * Tests business methods, factory patterns, and BMW generation-specific functionality.
 */
@DisplayName("BmwGenerationCacheDto Tests")
class BmwGenerationCacheDtoTest {

    // =================== Business Method Tests ===================

    @Test
    @DisplayName("isCurrent() should return true when yearEnd is null")
    void isCurrent_WithNullYearEnd_ShouldReturnTrue() {
        // Given
        BmwGenerationCacheDto dto = createTestDto("G20", "3", "G20 Generation", 2019, null, true);

        // When
        boolean isCurrent = dto.isCurrent();

        // Then
        assertThat(isCurrent).isTrue();
    }

    @Test
    @DisplayName("isCurrent() should return false when yearEnd is set")
    void isCurrent_WithYearEnd_ShouldReturnFalse() {
        // Given
        BmwGenerationCacheDto dto = createTestDto("F30", "3", "F30 Generation", 2012, 2019, true);

        // When
        boolean isCurrent = dto.isCurrent();

        // Then
        assertThat(isCurrent).isFalse();
    }

    @Test
    @DisplayName("isCurrent() should handle year end of zero correctly")
    void isCurrent_WithZeroYearEnd_ShouldReturnFalse() {
        // Given
        BmwGenerationCacheDto dto = createTestDto("ZERO", "3", "Zero Year End", 2020, 0, true);

        // When
        boolean isCurrent = dto.isCurrent();

        // Then
        assertThat(isCurrent).isFalse();
    }

    @Test
    @DisplayName("includesYear() should return true for year within range")
    void includesYear_WithYearInRange_ShouldReturnTrue() {
        // Given
        BmwGenerationCacheDto dto = createTestDto("F30", "3", "F30 Generation", 2012, 2019, true);

        // When & Then
        assertThat(dto.includesYear(2012)).isTrue(); // Start year
        assertThat(dto.includesYear(2015)).isTrue(); // Middle year  
        assertThat(dto.includesYear(2019)).isTrue(); // End year
    }

    @Test
    @DisplayName("includesYear() should return false for year before start")
    void includesYear_WithYearBeforeStart_ShouldReturnFalse() {
        // Given
        BmwGenerationCacheDto dto = createTestDto("F30", "3", "F30 Generation", 2012, 2019, true);

        // When & Then
        assertThat(dto.includesYear(2011)).isFalse();
        assertThat(dto.includesYear(2000)).isFalse();
        assertThat(dto.includesYear(1990)).isFalse();
    }

    @Test
    @DisplayName("includesYear() should return false for year after end")
    void includesYear_WithYearAfterEnd_ShouldReturnFalse() {
        // Given
        BmwGenerationCacheDto dto = createTestDto("F30", "3", "F30 Generation", 2012, 2019, true);

        // When & Then
        assertThat(dto.includesYear(2020)).isFalse();
        assertThat(dto.includesYear(2025)).isFalse();
        assertThat(dto.includesYear(2030)).isFalse();
    }

    @Test
    @DisplayName("includesYear() should handle current generation correctly (null yearEnd)")
    void includesYear_WithCurrentGeneration_ShouldHandleCorrectly() {
        // Given - Current generation starting 2019
        BmwGenerationCacheDto dto = createTestDto("G20", "3", "G20 Generation", 2019, null, true);

        // When & Then
        assertThat(dto.includesYear(2019)).isTrue(); // Start year
        assertThat(dto.includesYear(2023)).isTrue(); // Current year
        assertThat(dto.includesYear(2030)).isTrue(); // Future year (current generation)
        assertThat(dto.includesYear(2018)).isFalse(); // Before start
    }

    @Test
    @DisplayName("includesYear() should handle edge case years correctly")
    void includesYear_WithEdgeCaseYears_ShouldHandleCorrectly() {
        // Given
        BmwGenerationCacheDto dto = createTestDto("E90", "3", "E90 Generation", 2005, 2011, false);

        // When & Then
        assertThat(dto.includesYear(2004)).isFalse(); // One year before
        assertThat(dto.includesYear(2005)).isTrue();  // Exact start
        assertThat(dto.includesYear(2006)).isTrue();  // One year after start
        assertThat(dto.includesYear(2010)).isTrue();  // One year before end
        assertThat(dto.includesYear(2011)).isTrue();  // Exact end
        assertThat(dto.includesYear(2012)).isFalse(); // One year after end
    }

    @Test
    @DisplayName("includesYear() should handle very old generations")
    void includesYear_WithOldGeneration_ShouldHandleCorrectly() {
        // Given - Very old BMW generation
        BmwGenerationCacheDto dto = createTestDto("E21", "3", "E21 Generation", 1975, 1983, false);

        // When & Then
        assertThat(dto.includesYear(1975)).isTrue();  // Start year
        assertThat(dto.includesYear(1979)).isTrue();  // Middle year
        assertThat(dto.includesYear(1983)).isTrue();  // End year
        assertThat(dto.includesYear(1974)).isFalse(); // Before start
        assertThat(dto.includesYear(1984)).isFalse(); // After end
    }

    @Test
    @DisplayName("includesYear() should handle future generations")
    void includesYear_WithFutureGeneration_ShouldHandleCorrectly() {
        // Given - Future BMW generation
        BmwGenerationCacheDto dto = createTestDto("FUTURE", "3", "Future Generation", 2030, 2040, true);

        // When & Then
        assertThat(dto.includesYear(2030)).isTrue();  // Start year
        assertThat(dto.includesYear(2035)).isTrue();  // Middle year
        assertThat(dto.includesYear(2040)).isTrue();  // End year
        assertThat(dto.includesYear(2029)).isFalse(); // Before start
        assertThat(dto.includesYear(2041)).isFalse(); // After end
        assertThat(dto.includesYear(2023)).isFalse(); // Current year (before future start)
    }

    // =================== Builder and Construction Tests ===================

    @Test
    @DisplayName("Should create DTO with all fields using builder")
    void builder_WithAllFields_ShouldCreateCorrectly() {
        // Given
        List<String> bodyCodes = Arrays.asList("F30", "F31", "F34", "F35");
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        BmwGenerationCacheDto dto = BmwGenerationCacheDto.builder()
                .code("F30")
                .seriesCode("3")
                .name("F30 Generation")
                .yearStart(2012)
                .yearEnd(2019)
                .bodyCodes(bodyCodes)
                .isActive(true)
                .lastUpdated(timestamp)
                .build();

        // Then
        assertThat(dto.getCode()).isEqualTo("F30");
        assertThat(dto.getSeriesCode()).isEqualTo("3");
        assertThat(dto.getName()).isEqualTo("F30 Generation");
        assertThat(dto.getYearStart()).isEqualTo(2012);
        assertThat(dto.getYearEnd()).isEqualTo(2019);
        assertThat(dto.getBodyCodes()).containsExactly("F30", "F31", "F34", "F35");
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getLastUpdated()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should create DTO with minimal required fields")
    void builder_WithMinimalFields_ShouldCreateCorrectly() {
        // When
        BmwGenerationCacheDto dto = BmwGenerationCacheDto.builder()
                .code("G20")
                .seriesCode("3")
                .name("G20 Generation")
                .yearStart(2019)
                .isActive(true)
                .build();

        // Then
        assertThat(dto.getCode()).isEqualTo("G20");
        assertThat(dto.getSeriesCode()).isEqualTo("3");
        assertThat(dto.getName()).isEqualTo("G20 Generation");
        assertThat(dto.getYearStart()).isEqualTo(2019);
        assertThat(dto.getYearEnd()).isNull(); // Current generation
        assertThat(dto.getBodyCodes()).isNull();
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getLastUpdated()).isNull();
    }

    @Test
    @DisplayName("Should handle default values correctly")
    void defaultValues_ShouldBeHandledCorrectly() {
        // When
        BmwGenerationCacheDto dto = new BmwGenerationCacheDto();

        // Then
        assertThat(dto.getCode()).isNull();
        assertThat(dto.getSeriesCode()).isNull();
        assertThat(dto.getName()).isNull();
        assertThat(dto.getYearStart()).isNull();
        assertThat(dto.getYearEnd()).isNull();
        assertThat(dto.getBodyCodes()).isNull();
        assertThat(dto.isActive()).isFalse(); // boolean default
        assertThat(dto.getLastUpdated()).isNull();
    }

    // =================== Edge Case and Validation Tests ===================

    @Test
    @DisplayName("Should handle null bodyCodes list gracefully")
    void bodyCodes_WithNull_ShouldHandleGracefully() {
        // Given
        BmwGenerationCacheDto dto = BmwGenerationCacheDto.builder()
                .code("TEST")
                .bodyCodes(null)
                .build();

        // When
        List<String> bodyCodes = dto.getBodyCodes();

        // Then
        assertThat(bodyCodes).isNull();
    }

    @Test
    @DisplayName("Should handle empty bodyCodes list correctly")
    void bodyCodes_WithEmptyList_ShouldHandleCorrectly() {
        // Given
        BmwGenerationCacheDto dto = BmwGenerationCacheDto.builder()
                .code("TEST")
                .bodyCodes(Arrays.asList())
                .build();

        // When
        List<String> bodyCodes = dto.getBodyCodes();

        // Then
        assertThat(bodyCodes).isEmpty();
    }

    @Test
    @DisplayName("Should handle single body code correctly")
    void bodyCodes_WithSingleCode_ShouldHandleCorrectly() {
        // Given
        BmwGenerationCacheDto dto = BmwGenerationCacheDto.builder()
                .code("TEST")
                .bodyCodes(Arrays.asList("F30"))
                .build();

        // When
        List<String> bodyCodes = dto.getBodyCodes();

        // Then
        assertThat(bodyCodes).containsExactly("F30");
    }

    @Test
    @DisplayName("Should handle multiple body codes correctly")
    void bodyCodes_WithMultipleCodes_ShouldHandleCorrectly() {
        // Given
        List<String> inputCodes = Arrays.asList("F30", "F31", "F34", "F35", "F80");
        BmwGenerationCacheDto dto = BmwGenerationCacheDto.builder()
                .code("F30")
                .bodyCodes(inputCodes)
                .build();

        // When
        List<String> bodyCodes = dto.getBodyCodes();

        // Then
        assertThat(bodyCodes).containsExactly("F30", "F31", "F34", "F35", "F80");
        assertThat(bodyCodes).hasSize(5);
    }

    @Test
    @DisplayName("Should handle extreme year values correctly")
    void yearValues_WithExtremeValues_ShouldHandleCorrectly() {
        // Given
        BmwGenerationCacheDto earlyDto = createTestDto("EARLY", "3", "Early Generation", 1900, 1950, false);
        BmwGenerationCacheDto futureDto = createTestDto("FUTURE", "3", "Future Generation", 2100, 2150, true);

        // When & Then
        assertThat(earlyDto.includesYear(1925)).isTrue();
        assertThat(earlyDto.includesYear(1899)).isFalse();
        assertThat(earlyDto.includesYear(1951)).isFalse();
        
        assertThat(futureDto.includesYear(2125)).isTrue();
        assertThat(futureDto.includesYear(2099)).isFalse();
        assertThat(futureDto.includesYear(2151)).isFalse();
    }

    @Test
    @DisplayName("Should handle year boundaries precisely")
    void yearBoundaries_ShouldBePrecise() {
        // Given
        BmwGenerationCacheDto dto = createTestDto("PRECISE", "3", "Precise Generation", 2010, 2020, true);

        // When & Then - Test exact boundaries
        assertThat(dto.includesYear(2009)).isFalse(); // One before start
        assertThat(dto.includesYear(2010)).isTrue();  // Exact start
        assertThat(dto.includesYear(2011)).isTrue();  // One after start
        
        assertThat(dto.includesYear(2019)).isTrue();  // One before end
        assertThat(dto.includesYear(2020)).isTrue();  // Exact end
        assertThat(dto.includesYear(2021)).isFalse(); // One after end
    }

    @Test
    @DisplayName("Should handle same start and end year")
    void yearRange_WithSameStartAndEnd_ShouldHandleCorrectly() {
        // Given - Generation that lasted only one year
        BmwGenerationCacheDto dto = createTestDto("ONE_YEAR", "3", "One Year Generation", 2015, 2015, true);

        // When & Then
        assertThat(dto.includesYear(2014)).isFalse();
        assertThat(dto.includesYear(2015)).isTrue();  // Only valid year
        assertThat(dto.includesYear(2016)).isFalse();
        assertThat(dto.isCurrent()).isFalse(); // Has end year, so not current
    }

    // =================== Real BMW Generation Scenarios ===================

    @Test
    @DisplayName("Should handle real BMW F30 generation correctly")
    void realScenario_F30Generation_ShouldWorkCorrectly() {
        // Given - Real BMW F30 3-Series generation
        BmwGenerationCacheDto f30 = BmwGenerationCacheDto.builder()
                .code("F30")
                .seriesCode("3")
                .name("F30/F31/F34/F35")
                .yearStart(2012)
                .yearEnd(2019)
                .bodyCodes(Arrays.asList("F30", "F31", "F34", "F35"))
                .isActive(false) // No longer current
                .build();

        // When & Then
        assertThat(f30.isCurrent()).isFalse();
        assertThat(f30.includesYear(2015)).isTrue(); // Peak production year
        assertThat(f30.includesYear(2011)).isFalse(); // Before F30
        assertThat(f30.includesYear(2020)).isFalse(); // After F30 (G20 era)
        assertThat(f30.getBodyCodes()).containsExactly("F30", "F31", "F34", "F35");
    }

    @Test
    @DisplayName("Should handle real BMW G20 generation correctly")
    void realScenario_G20Generation_ShouldWorkCorrectly() {
        // Given - Real BMW G20 3-Series generation (current)
        BmwGenerationCacheDto g20 = BmwGenerationCacheDto.builder()
                .code("G20")
                .seriesCode("3")
                .name("G20/G21")
                .yearStart(2019)
                .yearEnd(null) // Current generation
                .bodyCodes(Arrays.asList("G20", "G21"))
                .isActive(true)
                .build();

        // When & Then
        assertThat(g20.isCurrent()).isTrue();
        assertThat(g20.includesYear(2019)).isTrue(); // Launch year
        assertThat(g20.includesYear(2023)).isTrue(); // Current year
        assertThat(g20.includesYear(2030)).isTrue(); // Future (still current)
        assertThat(g20.includesYear(2018)).isFalse(); // Before G20
        assertThat(g20.getBodyCodes()).containsExactly("G20", "G21");
    }

    @Test
    @DisplayName("Should handle real BMW E90 generation correctly")
    void realScenario_E90Generation_ShouldWorkCorrectly() {
        // Given - Real BMW E90 3-Series generation (historical)
        BmwGenerationCacheDto e90 = BmwGenerationCacheDto.builder()
                .code("E90")
                .seriesCode("3")
                .name("E90/E91/E92/E93")
                .yearStart(2005)
                .yearEnd(2011)
                .bodyCodes(Arrays.asList("E90", "E91", "E92", "E93"))
                .isActive(false)
                .build();

        // When & Then
        assertThat(e90.isCurrent()).isFalse();
        assertThat(e90.includesYear(2008)).isTrue(); // Middle of production
        assertThat(e90.includesYear(2004)).isFalse(); // Before E90
        assertThat(e90.includesYear(2012)).isFalse(); // After E90 (F30 era)
        assertThat(e90.getBodyCodes()).containsExactly("E90", "E91", "E92", "E93");
    }

    // =================== Helper Methods ===================

    private BmwGenerationCacheDto createTestDto(String code, String seriesCode, String name, 
                                               Integer yearStart, Integer yearEnd, boolean isActive) {
        return BmwGenerationCacheDto.builder()
                .code(code)
                .seriesCode(seriesCode)
                .name(name)
                .yearStart(yearStart)
                .yearEnd(yearEnd)
                .isActive(isActive)
                .build();
    }
}
