package live.alinmiron.beamerparts.product.service.domain;

import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for CategoryDomainService
 * Tests all business logic and domain rules using real database operations
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("CategoryDomainService Tests")
class CategoryDomainServiceTest {

    @Autowired
    private CategoryDomainService categoryDomainService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    // =========================
    // CATEGORY CREATION TESTS
    // =========================

    @Test
    @DisplayName("Should create category with valid data successfully")
    void createCategory_WithValidData_ShouldCreateSuccessfully() {
        // When
        Category category = categoryDomainService.createCategory(
                "Test Category", "test-category", "Test description", 
                null, 1, true
        );

        // Then
        assertThat(category).isNotNull();
        assertThat(category.getId()).isNotNull();
        assertThat(category.getName()).isEqualTo("Test Category");
        assertThat(category.getSlug()).isEqualTo("test-category");
        assertThat(category.getDescription()).isEqualTo("Test description");
        assertThat(category.getParent()).isNull();
        assertThat(category.getDisplayOrder()).isEqualTo(1);
        assertThat(category.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should create subcategory under active parent")
    void createCategory_WithActiveParent_ShouldCreateSuccessfully() {
        // Given
        Category parent = createTestCategory("Parent Category", "parent-category", true);

        // When
        Category subcategory = categoryDomainService.createCategory(
                "Child Category", "child-category", "Child description",
                parent.getId(), 1, true
        );

        // Then
        assertThat(subcategory.getParent()).isEqualTo(parent);
        assertThat(subcategory.getName()).isEqualTo("Child Category");
    }

    @Test
    @DisplayName("Should throw exception when creating category with duplicate slug")
    void createCategory_WithDuplicateSlug_ShouldThrowException() {
        // Given
        createTestCategory("Existing Category", "existing-slug", true);

        // When & Then
        assertThatThrownBy(() -> categoryDomainService.createCategory(
                "New Category", "existing-slug", "Description", null, 1, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category with slug 'existing-slug' already exists");
    }

    @Test
    @DisplayName("Should throw exception when creating category with non-existent parent")
    void createCategory_WithNonExistentParent_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> categoryDomainService.createCategory(
                "Test Category", "test-slug", "Description", 999L, 1, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Parent category not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw exception when creating category under inactive parent")
    void createCategory_WithInactiveParent_ShouldThrowException() {
        // Given
        Category inactiveParent = createTestCategory("Inactive Parent", "inactive-parent", false);

        // When & Then
        assertThatThrownBy(() -> categoryDomainService.createCategory(
                "Child Category", "child-category", "Description", 
                inactiveParent.getId(), 1, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot create category under inactive parent");
    }

    // =========================
    // CATEGORY UPDATE TESTS
    // =========================

    @Test
    @DisplayName("Should update category with valid changes")
    void updateCategory_WithValidChanges_ShouldUpdateSuccessfully() {
        // Given
        Category category = createTestCategory("Original Name", "original-slug", true);

        // When
        Category updated = categoryDomainService.updateCategory(
                category.getId(), "Updated Name", "updated-slug", "Updated description",
                null, 2, true
        );

        // Then
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getSlug()).isEqualTo("updated-slug");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw exception when updating with duplicate slug")
    void updateCategory_WithDuplicateSlug_ShouldThrowException() {
        // Given
        createTestCategory("Existing Category", "existing-slug", true);
        Category categoryToUpdate = createTestCategory("Category To Update", "update-slug", true);

        // When & Then
        assertThatThrownBy(() -> categoryDomainService.updateCategory(
                categoryToUpdate.getId(), "Updated Name", "existing-slug", "Description",
                null, 1, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category with slug 'existing-slug' already exists");
    }

    @Test
    @DisplayName("Should throw exception when setting category as its own parent")
    void updateCategory_WithSelfAsParent_ShouldThrowException() {
        // Given
        Category category = createTestCategory("Self Parent Test", "self-parent", true);

        // When & Then
        assertThatThrownBy(() -> categoryDomainService.updateCategory(
                category.getId(), "Updated Name", "updated-slug", "Description",
                category.getId(), 1, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category cannot be its own parent");
    }

    @Test
    @DisplayName("Should throw exception when moving to inactive parent")
    void updateCategory_WithInactiveParent_ShouldThrowException() {
        // Given
        Category category = createTestCategory("Test Category", "test-category", true);
        Category inactiveParent = createTestCategory("Inactive Parent", "inactive-parent", false);

        // When & Then
        assertThatThrownBy(() -> categoryDomainService.updateCategory(
                category.getId(), "Updated Name", "updated-slug", "Description",
                inactiveParent.getId(), 1, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot move category under inactive parent");
    }

    @Test
    @DisplayName("Should successfully move category to new active parent")
    void updateCategory_WithNewActiveParent_ShouldMoveSuccessfully() {
        // Given
        Category category = createTestCategory("Test Category", "test-category", true);
        Category newParent = createTestCategory("New Parent", "new-parent", true);

        // When
        Category updated = categoryDomainService.updateCategory(
                category.getId(), "Test Category", "test-category", "Description",
                newParent.getId(), 1, true
        );

        // Then
        assertThat(updated.getParent()).isEqualTo(newParent);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent category")
    void updateCategory_WithNonExistentId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> categoryDomainService.updateCategory(
                999L, "Name", "slug", "Description", null, 1, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category not found with ID: 999");
    }

    // =========================
    // CATEGORY DELETION TESTS
    // =========================

    @Test
    @DisplayName("Should delete empty category successfully")
    void deleteCategory_WithoutProductsOrSubcategories_ShouldDeleteSuccessfully() {
        // Given
        Category category = createTestCategory("Empty Category", "empty-category", true);

        // When
        categoryDomainService.deleteCategory(category.getId());

        // Then
        Category deletedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(deletedCategory.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when deleting category with products")
    void deleteCategory_WithProducts_ShouldThrowException() {
        // Given
        Category category = createTestCategory("Category With Products", "category-with-products", true);
        createTestProduct("Test Product", category);
        
        // Refresh category to load products relationship
        Category refreshedCategory = categoryRepository.findById(category.getId()).orElseThrow();

        // When & Then
        assertThatThrownBy(() -> categoryDomainService.deleteCategory(refreshedCategory.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete category with active products");
    }

    @Test
    @DisplayName("Should throw exception when deleting category with active subcategories")
    void deleteCategory_WithActiveSubcategories_ShouldThrowException() {
        // Given
        Category parent = createTestCategory("Parent Category", "parent-category", true);
        Category child = createTestCategory("Child Category", "child-category", true);
        child.setParent(parent);
        categoryRepository.save(child);

        // When & Then
        assertThatThrownBy(() -> categoryDomainService.deleteCategory(parent.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete category with active subcategories");
    }

    @Test
    @DisplayName("Should delete category with inactive subcategories")
    void deleteCategory_WithInactiveSubcategories_ShouldDeleteSuccessfully() {
        // Given
        Category parent = createTestCategory("Parent Category", "parent-category", true);
        Category inactiveChild = createTestCategory("Inactive Child", "inactive-child", false);
        inactiveChild.setParent(parent);
        categoryRepository.save(inactiveChild);

        // When
        categoryDomainService.deleteCategory(parent.getId());

        // Then
        Category deletedCategory = categoryRepository.findById(parent.getId()).orElseThrow();
        assertThat(deletedCategory.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent category")
    void deleteCategory_WithNonExistentId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> categoryDomainService.deleteCategory(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found with ID: 999");
    }

    // =========================
    // CATEGORY LOOKUP TESTS
    // =========================

    @Test
    @DisplayName("Should find category by ID")
    void findCategoryById_WithExistingId_ShouldReturnCategory() {
        // Given
        Category category = createTestCategory("Test Category", "test-category", true);

        // When
        Optional<Category> found = categoryDomainService.findCategoryById(category.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Category");
    }

    @Test
    @DisplayName("Should return empty when finding non-existent category by ID")
    void findCategoryById_WithNonExistentId_ShouldReturnEmpty() {
        // When
        Optional<Category> found = categoryDomainService.findCategoryById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find category by slug")
    void findCategoryBySlug_WithExistingSlug_ShouldReturnCategory() {
        // Given
        Category category = createTestCategory("Test Category", "test-category", true);

        // When
        Optional<Category> found = categoryDomainService.findCategoryBySlug("test-category");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Category");
    }

    @Test
    @DisplayName("Should return empty when finding non-existent category by slug")
    void findCategoryBySlug_WithNonExistentSlug_ShouldReturnEmpty() {
        // When
        Optional<Category> found = categoryDomainService.findCategoryBySlug("non-existent");

        // Then
        assertThat(found).isEmpty();
    }

    // =========================
    // CATEGORY HIERARCHY TESTS
    // =========================

    @Test
    @DisplayName("Should find root categories ordered by display order")
    void findRootCategories_ShouldReturnActiveRootCategoriesOrdered() {
        // Given
        Category root1 = createTestCategory("Root Category 1", "root-1", true);
        root1.setDisplayOrder(2);
        categoryRepository.save(root1);

        Category root2 = createTestCategory("Root Category 2", "root-2", true);
        root2.setDisplayOrder(1);
        categoryRepository.save(root2);

        Category inactiveRoot = createTestCategory("Inactive Root", "inactive-root", false);
        inactiveRoot.setDisplayOrder(0);
        categoryRepository.save(inactiveRoot);

        // When
        List<Category> rootCategories = categoryDomainService.findRootCategories();

        // Then
        assertThat(rootCategories).hasSize(2);
        assertThat(rootCategories.get(0).getName()).isEqualTo("Root Category 2"); // display order 1
        assertThat(rootCategories.get(1).getName()).isEqualTo("Root Category 1"); // display order 2
    }

    @Test
    @DisplayName("Should find subcategories of a parent category")
    void findSubcategories_WithExistingParent_ShouldReturnSubcategories() {
        // Given
        Category parent = createTestCategory("Parent Category", "parent-category", true);
        
        Category child1 = createTestCategory("Child 1", "child-1", true);
        child1.setParent(parent);
        child1.setDisplayOrder(2);
        categoryRepository.save(child1);

        Category child2 = createTestCategory("Child 2", "child-2", true);
        child2.setParent(parent);
        child2.setDisplayOrder(1);
        categoryRepository.save(child2);

        Category inactiveChild = createTestCategory("Inactive Child", "inactive-child", false);
        inactiveChild.setParent(parent);
        categoryRepository.save(inactiveChild);

        // When
        List<Category> subcategories = categoryDomainService.findSubcategories(parent.getId());

        // Then
        assertThat(subcategories).hasSize(2);
        assertThat(subcategories.get(0).getName()).isEqualTo("Child 2"); // display order 1
        assertThat(subcategories.get(1).getName()).isEqualTo("Child 1"); // display order 2
    }

    @Test
    @DisplayName("Should find categories with active products")
    void findCategoriesWithProducts_ShouldReturnCategoriesWithProducts() {
        // Given
        Category categoryWithProducts = createTestCategory("Category With Products", "with-products", true);
        createTestProduct("Test Product", categoryWithProducts);

        Category emptyCategory = createTestCategory("Empty Category", "empty-category", true);

        // When
        List<Category> categoriesWithProducts = categoryDomainService.findCategoriesWithProducts();

        // Then
        assertThat(categoriesWithProducts).isNotEmpty();
        assertThat(categoriesWithProducts)
                .extracting(Category::getName)
                .contains("Category With Products")
                .doesNotContain("Empty Category");
    }

    @Test
    @DisplayName("Should search categories by name case-insensitively")
    void searchCategoriesByName_ShouldReturnMatchingCategories() {
        // Given
        createTestCategory("Brake System", "brake-system", true);
        createTestCategory("Engine Parts", "engine-parts", true);
        createTestCategory("Brake Pads", "brake-pads", true);

        // When
        List<Category> results = categoryDomainService.searchCategoriesByName("brake");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Brake System", "Brake Pads");
    }

    // =========================
    // BUSINESS VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should validate active category exists")
    void isCategoryActiveAndExists_WithActiveCategory_ShouldReturnTrue() {
        // Given
        Category activeCategory = createTestCategory("Active Category", "active-category", true);

        // When
        boolean isActiveAndExists = categoryDomainService.isCategoryActiveAndExists(activeCategory.getId());

        // Then
        assertThat(isActiveAndExists).isTrue();
    }

    @Test
    @DisplayName("Should return false for inactive category")
    void isCategoryActiveAndExists_WithInactiveCategory_ShouldReturnFalse() {
        // Given
        Category inactiveCategory = createTestCategory("Inactive Category", "inactive-category", false);

        // When
        boolean isActiveAndExists = categoryDomainService.isCategoryActiveAndExists(inactiveCategory.getId());

        // Then
        assertThat(isActiveAndExists).isFalse();
    }

    @Test
    @DisplayName("Should return false for null category ID")
    void isCategoryActiveAndExists_WithNullId_ShouldReturnFalse() {
        // When
        boolean isActiveAndExists = categoryDomainService.isCategoryActiveAndExists(null);

        // Then
        assertThat(isActiveAndExists).isFalse();
    }

    @Test
    @DisplayName("Should return false for non-existent category")
    void isCategoryActiveAndExists_WithNonExistentId_ShouldReturnFalse() {
        // When
        boolean isActiveAndExists = categoryDomainService.isCategoryActiveAndExists(999L);

        // Then
        assertThat(isActiveAndExists).isFalse();
    }

    @Test
    @DisplayName("Should validate active category for product assignment")
    void validateCategoryForProductAssignment_WithActiveCategory_ShouldPass() {
        // Given
        Category activeCategory = createTestCategory("Active Category", "active-category", true);

        // When & Then
        assertThatCode(() -> categoryDomainService.validateCategoryForProductAssignment(activeCategory.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when validating inactive category for product assignment")
    void validateCategoryForProductAssignment_WithInactiveCategory_ShouldThrowException() {
        // Given
        Category inactiveCategory = createTestCategory("Inactive Category", "inactive-category", false);

        // When & Then
        assertThatThrownBy(() -> categoryDomainService.validateCategoryForProductAssignment(inactiveCategory.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot assign product to inactive category");
    }

    @Test
    @DisplayName("Should throw exception when validating null category ID for product assignment")
    void validateCategoryForProductAssignment_WithNullId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> categoryDomainService.validateCategoryForProductAssignment(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when validating non-existent category for product assignment")
    void validateCategoryForProductAssignment_WithNonExistentId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> categoryDomainService.validateCategoryForProductAssignment(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found with ID: 999");
    }

    // =========================
    // STATISTICS TESTS
    // =========================

    @Test
    @DisplayName("Should calculate category statistics correctly")
    void getCategoryStatistics_ShouldReturnCorrectStats() {
        // Given
        Category root1 = createTestCategory("Root 1", "root-1", true);
        Category root2 = createTestCategory("Root 2", "root-2", true);
        Category child = createTestCategory("Child", "child", true);
        child.setParent(root1);
        categoryRepository.save(child);
        
        Category inactiveCategory = createTestCategory("Inactive", "inactive", false);

        // When
        CategoryDomainService.CategoryStats stats = categoryDomainService.getCategoryStatistics();

        // Then
        assertThat(stats.getTotalActiveCategories()).isEqualTo(3); // root1, root2, child
        assertThat(stats.getRootCategories()).isEqualTo(2); // root1, root2
    }

    // =========================
    // HELPER METHODS
    // =========================

    private Category createTestCategory(String name, String slug, boolean isActive) {
        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description("Test description for " + name)
                .displayOrder(1)
                .isActive(isActive)
                .build();
        return categoryRepository.save(category);
    }

    private Product createTestProduct(String name, Category category) {
        Product product = Product.builder()
                .sku("TEST-SKU-" + System.currentTimeMillis())
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .description("Test product description")
                .shortDescription("Test short description")
                .basePrice(new BigDecimal("99.99"))
                .category(category)
                .brand("Test Brand")
                .weightGrams(500)
                .isFeatured(false)
                .status(ProductStatus.ACTIVE)
                .build();
        return productRepository.save(product);
    }
}
