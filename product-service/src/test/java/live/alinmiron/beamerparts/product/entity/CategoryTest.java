package live.alinmiron.beamerparts.product.entity;

import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Category entity business logic, persistence, and relationships.
 * Tests business methods, hierarchical structure, validation, and category-specific behavior.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("Category Entity Tests")
class CategoryTest {

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ProductRepository productRepository;

    private long testIdCounter;

    @BeforeEach
    void setUp() {
        testIdCounter = System.currentTimeMillis();
    }

    // =================== Business Method Tests ===================

    @Test
    @DisplayName("isRootCategory() should return true when parent is null")
    void isRootCategory_WithNullParent_ShouldReturnTrue() {
        // Given
        Category rootCategory = createTestCategory("Electronics", "electronics", null);

        // When
        boolean isRoot = rootCategory.isRootCategory();

        // Then
        assertThat(isRoot).isTrue();
    }

    @Test
    @DisplayName("isRootCategory() should return false when parent is set")
    void isRootCategory_WithParent_ShouldReturnFalse() {
        // Given
        Category parent = createTestCategory("Electronics", "electronics", null);
        Category child = createTestCategory("Smartphones", "smartphones", null);
        child.setParent(parent);

        // When
        boolean isRoot = child.isRootCategory();

        // Then
        assertThat(isRoot).isFalse();
    }

    @Test
    @DisplayName("hasSubcategories() should return false when subcategories is null")
    void hasSubcategories_WithNullSubcategories_ShouldReturnFalse() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null);
        category.setSubcategories(null);

        // When
        boolean hasSubcategories = category.hasSubcategories();

        // Then
        assertThat(hasSubcategories).isFalse();
    }

    @Test
    @DisplayName("hasSubcategories() should return false when subcategories is empty")
    void hasSubcategories_WithEmptySubcategories_ShouldReturnFalse() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null);
        category.setSubcategories(new ArrayList<>());

        // When
        boolean hasSubcategories = category.hasSubcategories();

        // Then
        assertThat(hasSubcategories).isFalse();
    }

    @Test
    @DisplayName("hasSubcategories() should return true when subcategories exist")
    void hasSubcategories_WithSubcategories_ShouldReturnTrue() {
        // Given
        Category parent = createTestCategory("Electronics", "electronics", null);
        Category child1 = createTestCategory("Smartphones", "smartphones", null);
        Category child2 = createTestCategory("Laptops", "laptops", null);
        
        List<Category> subcategories = List.of(child1, child2);
        parent.setSubcategories(subcategories);

        // When
        boolean hasSubcategories = parent.hasSubcategories();

        // Then
        assertThat(hasSubcategories).isTrue();
    }

    @Test
    @DisplayName("hasProducts() should return false when products is null")
    void hasProducts_WithNullProducts_ShouldReturnFalse() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null);
        category.setProducts(null);

        // When
        boolean hasProducts = category.hasProducts();

        // Then
        assertThat(hasProducts).isFalse();
    }

    @Test
    @DisplayName("hasProducts() should return false when products is empty")
    void hasProducts_WithEmptyProducts_ShouldReturnFalse() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null);
        category.setProducts(new ArrayList<>());

        // When
        boolean hasProducts = category.hasProducts();

        // Then
        assertThat(hasProducts).isFalse();
    }

    @Test
    @DisplayName("hasProducts() should return true when products exist")
    void hasProducts_WithProducts_ShouldReturnTrue() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", null);
        Product product1 = createTestProduct("iPhone 15", "iphone-15", category);
        Product product2 = createTestProduct("Samsung Galaxy", "samsung-galaxy", category);
        
        List<Product> products = List.of(product1, product2);
        category.setProducts(products);

        // When
        boolean hasProducts = category.hasProducts();

        // Then
        assertThat(hasProducts).isTrue();
    }

    @Test
    @DisplayName("getFullPath() should return name for root category")
    void getFullPath_ForRootCategory_ShouldReturnName() {
        // Given
        Category rootCategory = createTestCategory("Electronics", "electronics", null);

        // When
        String fullPath = rootCategory.getFullPath();

        // Then
        assertThat(fullPath).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("getFullPath() should return hierarchical path for nested category")
    void getFullPath_ForNestedCategory_ShouldReturnHierarchicalPath() {
        // Given
        Category electronics = createTestCategory("Electronics", "electronics", null);
        Category mobile = createTestCategory("Mobile Devices", "mobile-devices", null);
        Category smartphones = createTestCategory("Smartphones", "smartphones", null);
        
        mobile.setParent(electronics);
        smartphones.setParent(mobile);

        // When
        String fullPath = smartphones.getFullPath();

        // Then
        assertThat(fullPath).isEqualTo("Electronics > Mobile Devices > Smartphones");
    }

    @Test
    @DisplayName("getFullPath() should handle deep nesting correctly")
    void getFullPath_WithDeepNesting_ShouldHandleCorrectly() {
        // Given - Create 4-level hierarchy
        Category level1 = createTestCategory("Automotive", "automotive", null);
        Category level2 = createTestCategory("BMW", "bmw", null);
        Category level3 = createTestCategory("3 Series", "3-series", null);
        Category level4 = createTestCategory("F30 Parts", "f30-parts", null);
        
        level2.setParent(level1);
        level3.setParent(level2);
        level4.setParent(level3);

        // When
        String fullPath = level4.getFullPath();

        // Then
        assertThat(fullPath).isEqualTo("Automotive > BMW > 3 Series > F30 Parts");
    }

    @Test
    @DisplayName("getFullPath() should handle special characters in names")
    void getFullPath_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Given
        Category parent = createTestCategory("Electronics & Gadgets", "electronics-gadgets", null);
        Category child = createTestCategory("Audio/Video Equipment", "audio-video", null);
        child.setParent(parent);

        // When
        String fullPath = child.getFullPath();

        // Then
        assertThat(fullPath).isEqualTo("Electronics & Gadgets > Audio/Video Equipment");
    }

    // =================== Persistence Tests ===================

    @Test
    @DisplayName("Should persist Category with all required fields")
    void persistence_WithValidData_ShouldSaveSuccessfully() {
        // Given
        Category category = createTestCategory("Electronics", "electronics", "Electronic devices and gadgets");

        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertThat(savedCategory.getId()).isNotNull();
        assertThat(savedCategory.getName()).isEqualTo("Electronics");
        assertThat(savedCategory.getSlug()).startsWith("electronics-");
        assertThat(savedCategory.getDescription()).isEqualTo("Electronic devices and gadgets");
        assertThat(savedCategory.getDisplayOrder()).isEqualTo(0);
        assertThat(savedCategory.getIsActive()).isTrue();
        assertThat(savedCategory.getCreatedAt()).isNotNull();
        assertThat(savedCategory.getParent()).isNull();
    }

    @Test
    @DisplayName("Should automatically set createdAt timestamp on save")
    void persistence_OnSave_ShouldSetCreatedAtTimestamp() {
        // Given
        Category category = createTestCategory("Test Category", "test-category", null);
        LocalDateTime beforeSave = LocalDateTime.now();

        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertThat(savedCategory.getCreatedAt()).isNotNull();
        assertThat(savedCategory.getCreatedAt()).isAfterOrEqualTo(beforeSave.minusSeconds(1));
    }

    @Test
    @DisplayName("Should handle parent-child relationships correctly")
    void persistence_WithParentChild_ShouldSaveCorrectly() {
        // Given
        Category parent = createAndSaveCategory("Electronics", "electronics", null);
        Category child = createTestCategory("Smartphones", "smartphones", null);
        child.setParent(parent);

        // When
        Category savedChild = categoryRepository.save(child);

        // Then
        assertThat(savedChild.getParent()).isNotNull();
        assertThat(savedChild.getParent().getId()).isEqualTo(parent.getId());
        assertThat(savedChild.getParent().getName()).isEqualTo("Electronics");
        assertThat(savedChild.isRootCategory()).isFalse();
    }

    @Test
    @DisplayName("Should handle maximum length name (100 characters)")
    void persistence_WithMaxLengthName_ShouldSaveSuccessfully() {
        // Given
        String longName = "A".repeat(100); // Max length according to schema
        Category category = createTestCategory(longName, "long-name", null);

        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertThat(savedCategory.getName()).hasSize(100);
        assertThat(savedCategory.getName()).isEqualTo(longName);
    }

    @Test
    @DisplayName("Should handle maximum length slug (100 characters)")
    void persistence_WithMaxLengthSlug_ShouldSaveSuccessfully() {
        // Given
        String baseSlug = "A".repeat(80); // Leave room for timestamp
        Category category = createTestCategory("Test Category", baseSlug, null);

        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertThat(savedCategory.getSlug()).startsWith(baseSlug);
    }

    // =================== Hierarchical Structure Tests ===================

    @Test
    @DisplayName("Should support multi-level category hierarchy")
    void hierarchy_MultiLevel_ShouldWorkCorrectly() {
        // Given
        Category automotive = createAndSaveCategory("Automotive", "automotive", null);
        Category bmw = createTestCategory("BMW", "bmw", null);
        bmw.setParent(automotive);
        Category savedBmw = categoryRepository.save(bmw);
        
        Category series3 = createTestCategory("3 Series", "3-series", null);
        series3.setParent(savedBmw);
        Category savedSeries3 = categoryRepository.save(series3);

        // When
        String fullPath = savedSeries3.getFullPath();

        // Then
        assertThat(fullPath).isEqualTo("Automotive > BMW > 3 Series");
        assertThat(savedSeries3.isRootCategory()).isFalse();
        assertThat(savedBmw.isRootCategory()).isFalse();
        assertThat(automotive.isRootCategory()).isTrue();
    }

    @Test
    @DisplayName("Should handle category with both subcategories and products")
    void hierarchy_WithSubcategoriesAndProducts_ShouldWorkCorrectly() {
        // Given
        Category electronics = createAndSaveCategory("Electronics", "electronics", null);
        
        // Add subcategory
        Category smartphones = createTestCategory("Smartphones", "smartphones", null);
        smartphones.setParent(electronics);
        categoryRepository.save(smartphones);
        
        // Add product directly to parent category
        Product laptop = createTestProduct("MacBook Pro", "macbook-pro", electronics);
        productRepository.save(laptop);

        // When & Then - Test the persisted relationships directly
        assertThat(smartphones.getParent().getId()).isEqualTo(electronics.getId());
        assertThat(laptop.getCategory().getId()).isEqualTo(electronics.getId());
        assertThat(electronics.isRootCategory()).isTrue();
        
        // Note: Due to lazy loading, we can't easily test hasSubcategories() and hasProducts() 
        // in this context without additional queries, which are tested separately
    }

    // =================== Edge Case Tests ===================

    @Test
    @DisplayName("Should handle empty string description gracefully")
    void edgeCases_WithEmptyDescription_ShouldHandleGracefully() {
        // Given
        Category category = createTestCategory("Test Category", "test-category", "");

        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertThat(savedCategory.getDescription()).isEqualTo("");
    }

    @Test
    @DisplayName("Should handle null description correctly")
    void edgeCases_WithNullDescription_ShouldHandleCorrectly() {
        // Given
        Category category = createTestCategory("Test Category", "test-category", null);

        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertThat(savedCategory.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should handle special characters in name and slug")
    void edgeCases_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Given
        Category category = createTestCategory("Audio/Video & Electronics", "audio-video-electronics", null);

        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertThat(savedCategory.getName()).isEqualTo("Audio/Video & Electronics");
        assertThat(savedCategory.getSlug()).startsWith("audio-video-electronics");
    }

    @Test
    @DisplayName("Should handle zero and negative display orders")
    void edgeCases_WithZeroAndNegativeDisplayOrder_ShouldSaveSuccessfully() {
        // Given
        Category category1 = createTestCategory("Zero Order", "zero-order", null);
        category1.setDisplayOrder(0);
        Category category2 = createTestCategory("Negative Order", "negative-order", null);
        category2.setDisplayOrder(-5);

        // When
        Category savedCategory1 = categoryRepository.save(category1);
        Category savedCategory2 = categoryRepository.save(category2);

        // Then
        assertThat(savedCategory1.getDisplayOrder()).isEqualTo(0);
        assertThat(savedCategory2.getDisplayOrder()).isEqualTo(-5);
    }

    @Test
    @DisplayName("Should properly handle isActive default value")
    void defaultValues_IsActive_ShouldDefaultToTrue() {
        // Given
        Category category = Category.builder()
                .name("Default Test")
                .slug("default-test-" + testIdCounter++)
                .displayOrder(1)
                // isActive not explicitly set - should default to true
                .build();

        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertThat(savedCategory.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should properly handle displayOrder default value")
    void defaultValues_DisplayOrder_ShouldDefaultToZero() {
        // Given
        Category category = Category.builder()
                .name("Default Order")
                .slug("default-order-" + testIdCounter++)
                .isActive(true)
                // displayOrder not explicitly set - should default to 0
                .build();

        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertThat(savedCategory.getDisplayOrder()).isEqualTo(0);
    }

    // =================== Business Logic Integration Tests ===================

    @Test
    @DisplayName("Should handle complex category tree traversal")
    void businessLogic_ComplexTreeTraversal_ShouldWorkCorrectly() {
        // Given - Create a realistic automotive category tree
        Category automotive = createAndSaveCategory("Automotive", "automotive", "Vehicle parts and accessories");
        
        Category bmw = createTestCategory("BMW", "bmw", "BMW vehicle parts");
        bmw.setParent(automotive);
        Category savedBmw = categoryRepository.save(bmw);
        
        Category series3 = createTestCategory("3 Series", "3-series", "BMW 3 Series parts");
        series3.setParent(savedBmw);
        Category savedSeries3 = categoryRepository.save(series3);
        
        Category brakes = createTestCategory("Brake System", "brake-system", "Brake components");
        brakes.setParent(savedSeries3);
        Category savedBrakes = categoryRepository.save(brakes);

        // When
        String brakesFullPath = savedBrakes.getFullPath();

        // Then
        assertThat(brakesFullPath).isEqualTo("Automotive > BMW > 3 Series > Brake System");
        assertThat(savedBrakes.isRootCategory()).isFalse();
        assertThat(savedSeries3.isRootCategory()).isFalse();
        assertThat(savedBmw.isRootCategory()).isFalse();
        assertThat(automotive.isRootCategory()).isTrue();
    }

    @Test
    @DisplayName("Should maintain referential integrity in parent-child relationships")
    void businessLogic_ReferentialIntegrity_ShouldBeMaintained() {
        // Given
        Category parent = createAndSaveCategory("Parent Category", "parent", null);
        Category child1 = createTestCategory("Child 1", "child-1", null);
        Category child2 = createTestCategory("Child 2", "child-2", null);
        
        child1.setParent(parent);
        child2.setParent(parent);
        
        categoryRepository.save(child1);
        categoryRepository.save(child2);

        // When & Then - Test referential integrity directly
        assertThat(child1.getParent().getId()).isEqualTo(parent.getId());
        assertThat(child2.getParent().getId()).isEqualTo(parent.getId());
        assertThat(parent.isRootCategory()).isTrue();
        assertThat(child1.isRootCategory()).isFalse();
        assertThat(child2.isRootCategory()).isFalse();
    }

    // =================== Helper Methods ===================

    private Category createTestCategory(String name, String slug, String description) {
        return Category.builder()
                .name(name)
                .slug(slug + "-" + testIdCounter++)
                .description(description)
                .displayOrder(0)
                .isActive(true)
                .subcategories(new ArrayList<>())
                .products(new ArrayList<>())
                .build();
    }

    private Category createAndSaveCategory(String name, String slug, String description) {
        Category category = createTestCategory(name, slug, description);
        return categoryRepository.save(category);
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
