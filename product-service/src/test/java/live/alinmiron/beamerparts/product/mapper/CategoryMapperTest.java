package live.alinmiron.beamerparts.product.mapper;

import live.alinmiron.beamerparts.product.dto.external.response.CategoryResponseDto;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for CategoryMapper real-time data integration and mapping logic.
 * Tests business method integration, edge cases, and mapper behavior with various category states.
 */
@DisplayName("CategoryMapper Tests")
class CategoryMapperTest {

    private CategoryMapper categoryMapper;
    private long testIdCounter;

    @BeforeEach
    void setUp() {
        categoryMapper = new CategoryMapper();
        testIdCounter = System.currentTimeMillis();
    }

    // =================== External DTO Mapping Tests ===================

    @Test
    @DisplayName("mapToExternalDto() should handle null category gracefully")
    void mapToExternalDto_WithNullCategory_ShouldReturnNull() {
        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("mapToExternalDto() should map all basic fields correctly")
    void mapToExternalDto_WithBasicCategory_ShouldMapAllFields() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();
        Category category = createTestCategory("Electronics", "electronics", "Electronic devices", 1, true, createdAt);
        category.setId(100L);

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getName()).isEqualTo("Electronics");
        assertThat(result.getSlug()).isEqualTo("electronics");
        assertThat(result.getDescription()).isEqualTo("Electronic devices");
        assertThat(result.getParentId()).isNull();
        assertThat(result.getDisplayOrder()).isEqualTo(1);
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("mapToExternalDto() should map parent relationship correctly")
    void mapToExternalDto_WithParentCategory_ShouldMapParentId() {
        // Given
        Category parent = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        parent.setId(50L);
        
        Category child = createTestCategory("Smartphones", "smartphones", null, 1, true, LocalDateTime.now());
        child.setId(100L);
        child.setParent(parent);

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(child);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isEqualTo(50L);
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getName()).isEqualTo("Smartphones");
    }

    @Test
    @DisplayName("mapToExternalDto() should handle null parent correctly")
    void mapToExternalDto_WithNullParent_ShouldMapNullParentId() {
        // Given
        Category rootCategory = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        rootCategory.setId(100L);
        rootCategory.setParent(null);

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(rootCategory);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isNull();
    }

    @Test
    @DisplayName("mapToExternalDto() should use real-time hasSubcategories() method")
    void mapToExternalDto_WithSubcategories_ShouldUseRealTimeMethod() {
        // Given
        Category parent = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        Category child1 = createTestCategory("Smartphones", "smartphones", null, 1, true, LocalDateTime.now());
        Category child2 = createTestCategory("Laptops", "laptops", null, 2, true, LocalDateTime.now());
        
        // Set up subcategories relationship
        List<Category> subcategories = List.of(child1, child2);
        parent.setSubcategories(subcategories);

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(parent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHasSubcategories()).isTrue(); // Should use category.hasSubcategories()
    }

    @Test
    @DisplayName("mapToExternalDto() should correctly identify categories without subcategories")
    void mapToExternalDto_WithoutSubcategories_ShouldReturnFalse() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        category.setSubcategories(new ArrayList<>()); // Empty list

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHasSubcategories()).isFalse();
    }

    @Test
    @DisplayName("mapToExternalDto() should handle null subcategories list")
    void mapToExternalDto_WithNullSubcategories_ShouldReturnFalse() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        category.setSubcategories(null);

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHasSubcategories()).isFalse();
    }

    @Test
    @DisplayName("mapToExternalDto() should use real-time hasProducts() method")
    void mapToExternalDto_WithProducts_ShouldUseRealTimeMethod() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        Product product1 = createTestProduct("iPhone 15", "iphone-15", category);
        Product product2 = createTestProduct("Samsung Galaxy", "samsung-galaxy", category);
        
        // Set up products relationship
        List<Product> products = List.of(product1, product2);
        category.setProducts(products);

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHasProducts()).isTrue(); // Should use category.hasProducts()
    }

    @Test
    @DisplayName("mapToExternalDto() should correctly identify categories without products")
    void mapToExternalDto_WithoutProducts_ShouldReturnFalse() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        category.setProducts(new ArrayList<>()); // Empty list

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHasProducts()).isFalse();
    }

    @Test
    @DisplayName("mapToExternalDto() should handle null products list")
    void mapToExternalDto_WithNullProducts_ShouldReturnFalse() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        category.setProducts(null);

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHasProducts()).isFalse();
    }

    // =================== Admin DTO Mapping Tests ===================

    @Test
    @DisplayName("mapToAdminDto() should handle null category gracefully")
    void mapToAdminDto_WithNullCategory_ShouldReturnNull() {
        // When
        CategoryResponseDto result = categoryMapper.mapToAdminDto(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("mapToAdminDto() should map all basic fields correctly")
    void mapToAdminDto_WithBasicCategory_ShouldMapAllFields() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();
        Category category = createTestCategory("Electronics", "electronics", "Electronic devices", 1, true, createdAt);
        category.setId(100L);

        // When
        CategoryResponseDto result = categoryMapper.mapToAdminDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getName()).isEqualTo("Electronics");
        assertThat(result.getSlug()).isEqualTo("electronics");
        assertThat(result.getDescription()).isEqualTo("Electronic devices");
        assertThat(result.getParentId()).isNull();
        assertThat(result.getDisplayOrder()).isEqualTo(1);
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("mapToAdminDto() should produce identical results to mapToExternalDto()")
    void mapToAdminDto_ComparedToExternal_ShouldBeIdentical() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();
        Category category = createTestCategory("Electronics", "electronics", "Electronic devices", 1, true, createdAt);
        category.setId(100L);
        
        Product product = createTestProduct("Test Product", "test-product", category);
        category.setProducts(List.of(product));
        
        Category subcategory = createTestCategory("Smartphones", "smartphones", null, 1, true, LocalDateTime.now());
        category.setSubcategories(List.of(subcategory));

        // When
        CategoryResponseDto externalResult = categoryMapper.mapToExternalDto(category);
        CategoryResponseDto adminResult = categoryMapper.mapToAdminDto(category);

        // Then
        assertThat(adminResult).isNotNull();
        assertThat(externalResult).isNotNull();
        
        // All fields should be identical
        assertThat(adminResult.getId()).isEqualTo(externalResult.getId());
        assertThat(adminResult.getName()).isEqualTo(externalResult.getName());
        assertThat(adminResult.getSlug()).isEqualTo(externalResult.getSlug());
        assertThat(adminResult.getDescription()).isEqualTo(externalResult.getDescription());
        assertThat(adminResult.getParentId()).isEqualTo(externalResult.getParentId());
        assertThat(adminResult.getDisplayOrder()).isEqualTo(externalResult.getDisplayOrder());
        assertThat(adminResult.getIsActive()).isEqualTo(externalResult.getIsActive());
        assertThat(adminResult.getCreatedAt()).isEqualTo(externalResult.getCreatedAt());
        assertThat(adminResult.getHasSubcategories()).isEqualTo(externalResult.getHasSubcategories());
        assertThat(adminResult.getHasProducts()).isEqualTo(externalResult.getHasProducts());
    }

    // =================== Edge Case and Business Logic Tests ===================

    @Test
    @DisplayName("Should handle category with both subcategories and products")
    void mapping_WithSubcategoriesAndProducts_ShouldHandleBoth() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        
        // Add subcategories
        Category smartphone = createTestCategory("Smartphones", "smartphones", null, 1, true, LocalDateTime.now());
        category.setSubcategories(List.of(smartphone));
        
        // Add products
        Product laptop = createTestProduct("MacBook Pro", "macbook-pro", category);
        category.setProducts(List.of(laptop));

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHasSubcategories()).isTrue();
        assertThat(result.getHasProducts()).isTrue();
    }

    @Test
    @DisplayName("Should handle inactive categories correctly")
    void mapping_WithInactiveCategory_ShouldMapCorrectly() {
        // Given
        Category inactiveCategory = createTestCategory("Discontinued", "discontinued", "Old category", 99, false, LocalDateTime.now());
        inactiveCategory.setId(200L);

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(inactiveCategory);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
        assertThat(result.getName()).isEqualTo("Discontinued");
        assertThat(result.getDisplayOrder()).isEqualTo(99);
    }

    @Test
    @DisplayName("Should handle categories with zero display order")
    void mapping_WithZeroDisplayOrder_ShouldMapCorrectly() {
        // Given
        Category category = createTestCategory("Zero Order", "zero-order", null, 0, true, LocalDateTime.now());

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDisplayOrder()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle categories with negative display order")
    void mapping_WithNegativeDisplayOrder_ShouldMapCorrectly() {
        // Given
        Category category = createTestCategory("Negative Order", "negative-order", null, -5, true, LocalDateTime.now());

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDisplayOrder()).isEqualTo(-5);
    }

    @Test
    @DisplayName("Should handle empty description correctly")
    void mapping_WithEmptyDescription_ShouldMapCorrectly() {
        // Given
        Category category = createTestCategory("Test Category", "test-category", "", 1, true, LocalDateTime.now());

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("");
    }

    @Test
    @DisplayName("Should handle null description correctly")
    void mapping_WithNullDescription_ShouldMapCorrectly() {
        // Given
        Category category = createTestCategory("Test Category", "test-category", null, 1, true, LocalDateTime.now());

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should handle special characters in names and descriptions")
    void mapping_WithSpecialCharacters_ShouldMapCorrectly() {
        // Given
        Category category = createTestCategory(
            "Audio/Video & Electronics", 
            "audio-video-electronics", 
            "High-quality A/V equipment & electronic devices (premium grade)", 
            1, 
            true, 
            LocalDateTime.now()
        );

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Audio/Video & Electronics");
        assertThat(result.getSlug()).isEqualTo("audio-video-electronics");
        assertThat(result.getDescription()).isEqualTo("High-quality A/V equipment & electronic devices (premium grade)");
    }

    @Test
    @DisplayName("Should handle very long names and descriptions")
    void mapping_WithLongContent_ShouldMapCorrectly() {
        // Given
        String longName = "A".repeat(100); // Max length
        String longDescription = "B".repeat(1000); // Long description
        Category category = createTestCategory(longName, "long-content", longDescription, 1, true, LocalDateTime.now());

        // When
        CategoryResponseDto result = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(longName);
        assertThat(result.getDescription()).isEqualTo(longDescription);
    }

    // =================== Real-Time Business Logic Integration Tests ===================

    @Test
    @DisplayName("Should accurately reflect real-time subcategory status changes")
    void businessLogic_RealTimeSubcategoryChanges_ShouldReflectAccurately() {
        // Given
        Category parent = createTestCategory("Parent", "parent", null, 0, true, LocalDateTime.now());
        
        // Initially no subcategories
        parent.setSubcategories(new ArrayList<>());
        CategoryResponseDto resultBefore = categoryMapper.mapToExternalDto(parent);
        
        // Add subcategories
        Category child = createTestCategory("Child", "child", null, 1, true, LocalDateTime.now());
        parent.getSubcategories().add(child);
        CategoryResponseDto resultAfter = categoryMapper.mapToExternalDto(parent);

        // Then
        assertThat(resultBefore.getHasSubcategories()).isFalse();
        assertThat(resultAfter.getHasSubcategories()).isTrue();
    }

    @Test
    @DisplayName("Should accurately reflect real-time product status changes")
    void businessLogic_RealTimeProductChanges_ShouldReflectAccurately() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null, 0, true, LocalDateTime.now());
        
        // Initially no products
        category.setProducts(new ArrayList<>());
        CategoryResponseDto resultBefore = categoryMapper.mapToExternalDto(category);
        
        // Add products
        Product product = createTestProduct("iPhone", "iphone", category);
        category.getProducts().add(product);
        CategoryResponseDto resultAfter = categoryMapper.mapToExternalDto(category);

        // Then
        assertThat(resultBefore.getHasProducts()).isFalse();
        assertThat(resultAfter.getHasProducts()).isTrue();
    }

    // =================== Helper Methods ===================

    private Category createTestCategory(String name, String slug, String description, Integer displayOrder, Boolean isActive, LocalDateTime createdAt) {
        return Category.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .displayOrder(displayOrder)
                .isActive(isActive)
                .createdAt(createdAt)
                .subcategories(new ArrayList<>())
                .products(new ArrayList<>())
                .build();
    }

    private Product createTestProduct(String name, String slug, Category category) {
        return Product.builder()
                .name(name)
                .slug(slug + "-" + testIdCounter++)
                .sku("SKU-" + testIdCounter++)
                .description("Test product description")
                .shortDescription("Short description")
                .basePrice(new BigDecimal("99.99"))
                .category(category)
                .brand("Test Brand")
                .weightGrams(500)
                .status(ProductStatus.ACTIVE)
                .build();
    }
}
