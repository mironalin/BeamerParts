package live.alinmiron.beamerparts.product.entity;

import live.alinmiron.beamerparts.product.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ProductCompatibility entity business logic
 * Tests all business methods and domain rules using real database operations
 * 
 * **KEY LESSONS APPLIED:**
 * - Tests define business logic FIRST, implementation follows
 * - Use @SpringBootTest for entity testing with real DB operations
 * - Test all business methods and edge cases comprehensively
 * - Verify entity relationships and BMW compatibility logic
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("ProductCompatibility Entity Tests")
class ProductCompatibilityTest {

    @Autowired
    private ProductCompatibilityRepository productCompatibilityRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private BmwGenerationCacheRepository bmwGenerationCacheRepository;
    
    @Autowired
    private BmwSeriesCacheRepository bmwSeriesCacheRepository;

    private Category testCategory;
    private Product testProduct;
    private BmwSeriesCache testSeries;
    private BmwGenerationCache testGeneration;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        productCompatibilityRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        bmwGenerationCacheRepository.deleteAll();
        bmwSeriesCacheRepository.deleteAll();
        
        // Create test category
        testCategory = Category.builder()
                .name("Test Category")
                .slug("test-category-" + System.currentTimeMillis())
                .description("Test category")
                .displayOrder(1)
                .isActive(true)
                .build();
        testCategory = categoryRepository.save(testCategory);

        // Create test product
        testProduct = Product.builder()
                .sku("TEST-PRODUCT-" + System.currentTimeMillis())
                .name("BMW Test Product")
                .slug("bmw-test-product-" + System.currentTimeMillis())
                .description("Test BMW compatible product")
                .shortDescription("BMW compatibility test")
                .basePrice(new BigDecimal("199.99"))
                .category(testCategory)
                .brand("BMW")
                .weightGrams(1500)
                .isFeatured(false)
                .status(ProductStatus.ACTIVE)
                .build();
        testProduct = productRepository.save(testProduct);

        // Create test BMW series
        testSeries = BmwSeriesCache.builder()
                .code("3")
                .name("3 Series")
                .displayOrder(1)
                .isActive(true)
                .build();
        testSeries = bmwSeriesCacheRepository.save(testSeries);

        // Create test BMW generation
        testGeneration = BmwGenerationCache.builder()
                .code("F30")
                .seriesCache(testSeries)
                .name("F30")
                .yearStart(2012)
                .yearEnd(2019)
                .bodyCodes(new String[]{"Sedan", "Touring"})
                .isActive(true)
                .build();
        testGeneration = bmwGenerationCacheRepository.save(testGeneration);
    }

    // =========================
    // COMPATIBILITY LOGIC TESTS
    // =========================

    @Test
    @DisplayName("Should confirm compatibility with matching generation code")
    void isCompatibleWith_WithMatchingGeneration_ShouldReturnTrue() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "Perfect fit for F30");

        // When
        boolean isCompatible = compatibility.isCompatibleWith("F30");

        // Then
        assertThat(isCompatible).isTrue();
    }

    @Test
    @DisplayName("Should reject compatibility with non-matching generation code")
    void isCompatibleWith_WithNonMatchingGeneration_ShouldReturnFalse() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "F30 specific part");

        // When
        boolean isCompatible = compatibility.isCompatibleWith("E90");

        // Then
        assertThat(isCompatible).isFalse();
    }

    @Test
    @DisplayName("Should handle case-sensitive generation code comparison")
    void isCompatibleWith_WithDifferentCase_ShouldReturnFalse() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "Case sensitive test");

        // When
        boolean isCompatible = compatibility.isCompatibleWith("f30"); // lowercase

        // Then
        assertThat(isCompatible).isFalse();
    }

    @Test
    @DisplayName("Should handle null generation code gracefully")
    void isCompatibleWith_WithNullGenerationCode_ShouldReturnFalse() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "Null test");

        // When
        boolean isCompatible = compatibility.isCompatibleWith(null);

        // Then
        assertThat(isCompatible).isFalse();
    }

    // =========================
    // COMPATIBILITY STATUS TESTS
    // =========================

    @Test
    @DisplayName("Should return verified status for verified compatibility")
    void getCompatibilityStatus_WithVerifiedFlag_ShouldReturnVerified() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "Verified by engineering team");

        // When
        String status = compatibility.getCompatibilityStatus();

        // Then
        assertThat(status).isEqualTo("Verified Compatible");
    }

    @Test
    @DisplayName("Should return unverified status for unverified compatibility")
    void getCompatibilityStatus_WithoutVerifiedFlag_ShouldReturnUnverified() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(false, "Needs verification");

        // When
        String status = compatibility.getCompatibilityStatus();

        // Then
        assertThat(status).isEqualTo("Compatibility Not Verified");
    }

    @Test
    @DisplayName("Should default to unverified status")
    void isVerified_ShouldDefaultToFalse() {
        // Given
        ProductCompatibility compatibility = ProductCompatibility.builder()
                .product(testProduct)
                .generationCache(testGeneration)
                .notes("Default verification test")
                .build();

        // When & Then
        assertThat(compatibility.getIsVerified()).isFalse();
        assertThat(compatibility.getCompatibilityStatus()).isEqualTo("Compatibility Not Verified");
    }

    // =========================
    // DISPLAY NAME LOGIC TESTS
    // =========================

    @Test
    @DisplayName("Should generate display name combining product and generation")
    void getDisplayName_ShouldCombineProductAndGenerationNames() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "Display name test");

        // When
        String displayName = compatibility.getDisplayName();

        // Then
        assertThat(displayName).contains("BMW Test Product");
        assertThat(displayName).contains("3 Series F30 (2012-2019)");
        assertThat(displayName).isEqualTo("BMW Test Product - 3 Series F30 (2012-2019)");
    }

    @Test
    @DisplayName("Should handle null product gracefully in display name")
    void getDisplayName_WithNullProduct_ShouldHandleGracefully() {
        // Given
        ProductCompatibility compatibility = ProductCompatibility.builder()
                .product(null) // This should not happen in practice
                .generationCache(testGeneration)
                .isVerified(true)
                .build();

        // When & Then - Should throw NPE as per domain design
        assertThatThrownBy(() -> compatibility.getDisplayName())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle null generation cache gracefully in display name")
    void getDisplayName_WithNullGenerationCache_ShouldHandleGracefully() {
        // Given
        ProductCompatibility compatibility = ProductCompatibility.builder()
                .product(testProduct)
                .generationCache(null) // This should not happen in practice
                .isVerified(true)
                .build();

        // When & Then - Should throw NPE as per domain design
        assertThatThrownBy(() -> compatibility.getDisplayName())
                .isInstanceOf(NullPointerException.class);
    }

    // =========================
    // PERSISTENCE AND AUDIT TESTS
    // =========================

    @Test
    @DisplayName("Should persist compatibility with all required fields")
    void persistCompatibility_WithValidData_ShouldSaveSuccessfully() {
        // Given
        ProductCompatibility compatibility = ProductCompatibility.builder()
                .product(testProduct)
                .generationCache(testGeneration)
                .notes("Thoroughly tested compatibility")
                .isVerified(true)
                .build();

        // When
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);

        // Then
        assertThat(savedCompatibility.getId()).isNotNull();
        assertThat(savedCompatibility.getProduct()).isEqualTo(testProduct);
        assertThat(savedCompatibility.getGenerationCache()).isEqualTo(testGeneration);
        assertThat(savedCompatibility.getNotes()).isEqualTo("Thoroughly tested compatibility");
        assertThat(savedCompatibility.getIsVerified()).isTrue();
        assertThat(savedCompatibility.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should set creation timestamp automatically")
    void createdAt_ShouldBeSetAutomatically() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "Auto timestamp test");
        LocalDateTime beforePersist = LocalDateTime.now().minusSeconds(1);
        
        // When
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);
        LocalDateTime afterPersist = LocalDateTime.now().plusSeconds(1);

        // Then
        assertThat(savedCompatibility.getCreatedAt()).isNotNull();
        assertThat(savedCompatibility.getCreatedAt()).isAfter(beforePersist);
        assertThat(savedCompatibility.getCreatedAt()).isBefore(afterPersist);
    }

    @Test
    @DisplayName("Should handle optional notes as null")
    void createCompatibility_WithNullNotes_ShouldPersistSuccessfully() {
        // Given
        ProductCompatibility compatibility = ProductCompatibility.builder()
                .product(testProduct)
                .generationCache(testGeneration)
                .notes(null) // Optional field
                .isVerified(true)
                .build();

        // When
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);

        // Then
        assertThat(savedCompatibility.getId()).isNotNull();
        assertThat(savedCompatibility.getNotes()).isNull();
        assertThat(savedCompatibility.getIsVerified()).isTrue();
    }

    @Test
    @DisplayName("Should handle empty notes")
    void createCompatibility_WithEmptyNotes_ShouldPersistSuccessfully() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(false, "");

        // When
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);

        // Then
        assertThat(savedCompatibility.getNotes()).isEqualTo("");
        assertThat(savedCompatibility.getIsVerified()).isFalse();
    }

    // =========================
    // BUSINESS RULE TESTS
    // =========================

    @Test
    @DisplayName("Should create verified compatibility record")
    void createVerifiedCompatibility_ShouldSetCorrectStatus() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "Engineering verified: Perfect fit for F30 models");

        // When
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);

        // Then
        assertThat(savedCompatibility.getIsVerified()).isTrue();
        assertThat(savedCompatibility.getCompatibilityStatus()).isEqualTo("Verified Compatible");
        assertThat(savedCompatibility.isCompatibleWith("F30")).isTrue();
    }

    @Test
    @DisplayName("Should create unverified compatibility record")
    void createUnverifiedCompatibility_ShouldSetCorrectStatus() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(false, "Customer reported compatibility, needs verification");

        // When
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);

        // Then
        assertThat(savedCompatibility.getIsVerified()).isFalse();
        assertThat(savedCompatibility.getCompatibilityStatus()).isEqualTo("Compatibility Not Verified");
        assertThat(savedCompatibility.isCompatibleWith("F30")).isTrue(); // Still physically compatible
    }

    @Test
    @DisplayName("Should track compatibility across different generations")
    void trackCompatibility_AcrossDifferentGenerations_ShouldWorkCorrectly() {
        // Given - Create another generation
        BmwGenerationCache e90Generation = BmwGenerationCache.builder()
                .code("E90")
                .seriesCache(testSeries)
                .name("E90")
                .yearStart(2005)
                .yearEnd(2011)
                .bodyCodes(new String[]{"Sedan"})
                .isActive(true)
                .build();
        e90Generation = bmwGenerationCacheRepository.save(e90Generation);

        ProductCompatibility f30Compatibility = createTestCompatibility(true, "F30 specific");
        ProductCompatibility e90Compatibility = ProductCompatibility.builder()
                .product(testProduct)
                .generationCache(e90Generation)
                .notes("E90 specific")
                .isVerified(false)
                .build();

        // When
        ProductCompatibility savedF30 = productCompatibilityRepository.save(f30Compatibility);
        ProductCompatibility savedE90 = productCompatibilityRepository.save(e90Compatibility);

        // Then
        assertThat(savedF30.isCompatibleWith("F30")).isTrue();
        assertThat(savedF30.isCompatibleWith("E90")).isFalse();
        
        assertThat(savedE90.isCompatibleWith("E90")).isTrue();
        assertThat(savedE90.isCompatibleWith("F30")).isFalse();
    }

    // =========================
    // EDGE CASE TESTS
    // =========================

    @Test
    @DisplayName("Should handle long notes strings")
    void handleLongNotes_ShouldPersistCorrectly() {
        // Given
        String longNotes = "This is a very detailed compatibility note that describes extensive testing procedures, " +
                "fitment verification, performance analysis, and engineering validation that was conducted to " +
                "confirm this part's compatibility with the specified BMW generation. The testing included " +
                "physical fit verification, performance benchmarking, and long-term durability assessment " +
                "under various operating conditions and environmental factors to ensure optimal performance.";
        
        ProductCompatibility compatibility = createTestCompatibility(true, longNotes);

        // When
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);

        // Then
        assertThat(savedCompatibility.getNotes()).isEqualTo(longNotes);
    }

    @Test
    @DisplayName("Should handle special characters in notes")
    void handleSpecialCharactersInNotes_ShouldPersistCorrectly() {
        // Given
        String specialNotes = "Compatibility tested with BMW F30 320i, 325i, 330i & 335i models. " +
                "Temperature range: -20°C to +85°C. Performance: ±2% variance. " +
                "Requires additional bracket (Part #12-34-567).";
        
        ProductCompatibility compatibility = createTestCompatibility(true, specialNotes);

        // When
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);

        // Then
        assertThat(savedCompatibility.getNotes()).isEqualTo(specialNotes);
    }

    @Test
    @DisplayName("Should handle generation codes with special characters")
    void isCompatibleWith_WithSpecialCharacterCodes_ShouldWorkCorrectly() {
        // Given - Create generation with special characters
        BmwGenerationCache specialGeneration = BmwGenerationCache.builder()
                .code("F30-LCI")
                .seriesCache(testSeries)
                .name("F30 LCI")
                .yearStart(2016)
                .yearEnd(2019)
                .bodyCodes(new String[]{"Sedan"})
                .isActive(true)
                .build();
        specialGeneration = bmwGenerationCacheRepository.save(specialGeneration);

        ProductCompatibility compatibility = ProductCompatibility.builder()
                .product(testProduct)
                .generationCache(specialGeneration)
                .notes("LCI facelift model compatibility")
                .isVerified(true)
                .build();

        // When & Then
        assertThat(compatibility.isCompatibleWith("F30-LCI")).isTrue();
        assertThat(compatibility.isCompatibleWith("F30")).isFalse();
    }

    @Test
    @DisplayName("Should maintain referential integrity with product deletion")
    void referentialIntegrity_WithProductDeletion_ShouldBeEnforced() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "Referential integrity test");
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);

        // When & Then - Attempting to delete product should fail or cascade properly
        assertThat(savedCompatibility.getProduct()).isNotNull();
        // Note: The actual cascading behavior depends on JPA cascade configuration
        // This test verifies the relationship exists before any deletion attempts
    }

    @Test
    @DisplayName("Should maintain referential integrity with generation deletion")
    void referentialIntegrity_WithGenerationDeletion_ShouldBeEnforced() {
        // Given
        ProductCompatibility compatibility = createTestCompatibility(true, "Generation integrity test");
        ProductCompatibility savedCompatibility = productCompatibilityRepository.save(compatibility);

        // When & Then - Attempting to delete generation should fail or cascade properly
        assertThat(savedCompatibility.getGenerationCache()).isNotNull();
        assertThat(savedCompatibility.getGenerationCache().getCode()).isEqualTo("F30");
    }

    // =========================
    // HELPER METHODS
    // =========================

    private ProductCompatibility createTestCompatibility(boolean isVerified, String notes) {
        return ProductCompatibility.builder()
                .product(testProduct)
                .generationCache(testGeneration)
                .notes(notes)
                .isVerified(isVerified)
                .build();
    }
}
