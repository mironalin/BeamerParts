package live.alinmiron.beamerparts.product.service.domain;

import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.entity.ProductVariant;
import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ProductVariantDomainService
 * Tests all business logic and domain rules using real database operations
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("ProductVariantDomainService Tests")
class ProductVariantDomainServiceTest {

    @Autowired
    private ProductVariantDomainService productVariantDomainService;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // =========================
    // VARIANT CREATION TESTS
    // =========================

    @Test
    @DisplayName("Should create product variant with valid data successfully")
    void createProductVariant_WithValidData_ShouldCreateSuccessfully() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");

        // When
        ProductVariant variant = productVariantDomainService.createProductVariant(
                product.getId(), "Front Variant", "FRONT", 
                new BigDecimal("10.00"), true
        );

        // Then
        assertThat(variant).isNotNull();
        assertThat(variant.getId()).isNotNull();
        assertThat(variant.getProduct()).isEqualTo(product);
        assertThat(variant.getName()).isEqualTo("Front Variant");
        assertThat(variant.getSkuSuffix()).isEqualTo("FRONT");
        assertThat(variant.getPriceModifier()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(variant.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when creating variant for non-existent product")
    void createProductVariant_WithNonExistentProduct_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.createProductVariant(
                999L, "Test Variant", "TEST", BigDecimal.ZERO, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw exception when creating variant with duplicate SKU suffix")
    void createProductVariant_WithDuplicateSkuSuffix_ShouldThrowException() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        productVariantDomainService.createProductVariant(
                product.getId(), "Existing Variant", "FRONT", BigDecimal.ZERO, true
        );

        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.createProductVariant(
                product.getId(), "New Variant", "FRONT", BigDecimal.ZERO, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Variant with SKU suffix 'FRONT' already exists for this product");
    }

    @Test
    @DisplayName("Should create variant with negative price modifier (discount)")
    void createProductVariant_WithNegativePriceModifier_ShouldCreateSuccessfully() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");

        // When
        ProductVariant variant = productVariantDomainService.createProductVariant(
                product.getId(), "Discount Variant", "DISCOUNT", 
                new BigDecimal("-15.00"), true
        );

        // Then
        assertThat(variant.getPriceModifier()).isEqualByComparingTo(new BigDecimal("-15.00"));
    }

    @Test
    @DisplayName("Should create inactive variant")
    void createProductVariant_WithInactiveStatus_ShouldCreateSuccessfully() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");

        // When
        ProductVariant variant = productVariantDomainService.createProductVariant(
                product.getId(), "Inactive Variant", "INACTIVE", 
                BigDecimal.ZERO, false
        );

        // Then
        assertThat(variant.getIsActive()).isFalse();
    }

    // =========================
    // VARIANT UPDATE TESTS
    // =========================

    @Test
    @DisplayName("Should update product variant with valid changes")
    void updateProductVariant_WithValidChanges_ShouldUpdateSuccessfully() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant variant = createTestVariant(product, "Original Name", "ORIGINAL", BigDecimal.ZERO, true);

        // When
        ProductVariant updated = productVariantDomainService.updateProductVariant(
                variant.getId(), "Updated Name", "UPDATED", 
                new BigDecimal("25.00"), false
        );

        // Then
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getSkuSuffix()).isEqualTo("UPDATED");
        assertThat(updated.getPriceModifier()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when updating variant with duplicate SKU suffix")
    void updateProductVariant_WithDuplicateSkuSuffix_ShouldThrowException() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        createTestVariant(product, "Existing Variant", "EXISTING", BigDecimal.ZERO, true);
        ProductVariant variantToUpdate = createTestVariant(product, "Variant To Update", "UPDATE", BigDecimal.ZERO, true);

        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.updateProductVariant(
                variantToUpdate.getId(), "Updated Name", "EXISTING", BigDecimal.ZERO, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Variant with SKU suffix 'EXISTING' already exists for this product");
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent variant")
    void updateProductVariant_WithNonExistentId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.updateProductVariant(
                999L, "Name", "SKU", BigDecimal.ZERO, true
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Product variant not found with ID: 999");
    }

    @Test
    @DisplayName("Should allow updating same SKU suffix")
    void updateProductVariant_WithSameSkuSuffix_ShouldUpdateSuccessfully() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant variant = createTestVariant(product, "Test Variant", "SAME", BigDecimal.ZERO, true);

        // When
        ProductVariant updated = productVariantDomainService.updateProductVariant(
                variant.getId(), "Updated Name", "SAME", 
                new BigDecimal("10.00"), true
        );

        // Then
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getSkuSuffix()).isEqualTo("SAME");
        assertThat(updated.getPriceModifier()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    // =========================
    // VARIANT ACTIVATION TESTS
    // =========================

    @Test
    @DisplayName("Should activate inactive product variant")
    void activateProductVariant_WithInactiveVariant_ShouldActivateSuccessfully() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant inactiveVariant = createTestVariant(product, "Inactive Variant", "INACTIVE", BigDecimal.ZERO, false);

        // When
        ProductVariant activated = productVariantDomainService.activateProductVariant(inactiveVariant.getId());

        // Then
        assertThat(activated.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should activate already active variant without error")
    void activateProductVariant_WithActiveVariant_ShouldRemainActive() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant activeVariant = createTestVariant(product, "Active Variant", "ACTIVE", BigDecimal.ZERO, true);

        // When
        ProductVariant activated = productVariantDomainService.activateProductVariant(activeVariant.getId());

        // Then
        assertThat(activated.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when activating non-existent variant")
    void activateProductVariant_WithNonExistentId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.activateProductVariant(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product variant not found with ID: 999");
    }

    // =========================
    // VARIANT DEACTIVATION TESTS
    // =========================

    @Test
    @DisplayName("Should deactivate active product variant")
    void deactivateProductVariant_WithActiveVariant_ShouldDeactivateSuccessfully() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant activeVariant = createTestVariant(product, "Active Variant", "ACTIVE", BigDecimal.ZERO, true);

        // When
        ProductVariant deactivated = productVariantDomainService.deactivateProductVariant(activeVariant.getId());

        // Then
        assertThat(deactivated.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should deactivate already inactive variant without error")
    void deactivateProductVariant_WithInactiveVariant_ShouldRemainInactive() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant inactiveVariant = createTestVariant(product, "Inactive Variant", "INACTIVE", BigDecimal.ZERO, false);

        // When
        ProductVariant deactivated = productVariantDomainService.deactivateProductVariant(inactiveVariant.getId());

        // Then
        assertThat(deactivated.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when deactivating non-existent variant")
    void deactivateProductVariant_WithNonExistentId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.deactivateProductVariant(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product variant not found with ID: 999");
    }

    // =========================
    // VARIANT DELETION TESTS
    // =========================

    @Test
    @DisplayName("Should delete product variant successfully")
    void deleteProductVariant_WithValidId_ShouldDeleteSuccessfully() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant variant = createTestVariant(product, "Test Variant", "DELETE", BigDecimal.ZERO, true);
        Long variantId = variant.getId();

        // When
        productVariantDomainService.deleteProductVariant(variantId);

        // Then
        assertThat(productVariantRepository.findById(variantId)).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent variant")
    void deleteProductVariant_WithNonExistentId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.deleteProductVariant(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product variant not found with ID: 999");
    }

    // =========================
    // VARIANT LOOKUP TESTS
    // =========================

    @Test
    @DisplayName("Should find product variant by ID")
    void findProductVariantById_WithExistingId_ShouldReturnVariant() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant variant = createTestVariant(product, "Test Variant", "TEST", BigDecimal.ZERO, true);

        // When
        Optional<ProductVariant> found = productVariantDomainService.findProductVariantById(variant.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Variant");
    }

    @Test
    @DisplayName("Should return empty when finding non-existent variant by ID")
    void findProductVariantById_WithNonExistentId_ShouldReturnEmpty() {
        // When
        Optional<ProductVariant> found = productVariantDomainService.findProductVariantById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find product variant by product and SKU suffix")
    void findProductVariantBySkuSuffix_WithExistingSkuSuffix_ShouldReturnVariant() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant variant = createTestVariant(product, "Test Variant", "FIND", BigDecimal.ZERO, true);

        // When
        Optional<ProductVariant> found = productVariantDomainService.findProductVariantBySkuSuffix(
                product.getId(), "FIND"
        );

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Variant");
    }

    @Test
    @DisplayName("Should return empty when finding non-existent SKU suffix")
    void findProductVariantBySkuSuffix_WithNonExistentSkuSuffix_ShouldReturnEmpty() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");

        // When
        Optional<ProductVariant> found = productVariantDomainService.findProductVariantBySkuSuffix(
                product.getId(), "NON-EXISTENT"
        );

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all variants for a product")
    void findVariantsByProduct_WithExistingProduct_ShouldReturnAllVariants() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        createTestVariant(product, "Variant 1", "V1", BigDecimal.ZERO, true);
        createTestVariant(product, "Variant 2", "V2", BigDecimal.ZERO, false);
        createTestVariant(product, "Variant 3", "V3", BigDecimal.ZERO, true);

        // When
        List<ProductVariant> variants = productVariantDomainService.findVariantsByProduct(product.getId());

        // Then
        assertThat(variants).hasSize(3);
        assertThat(variants)
                .extracting(ProductVariant::getName)
                .containsExactlyInAnyOrder("Variant 1", "Variant 2", "Variant 3");
    }

    @Test
    @DisplayName("Should throw exception when finding variants for non-existent product")
    void findVariantsByProduct_WithNonExistentProduct_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.findVariantsByProduct(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should find only active variants for a product")
    void findActiveVariantsByProduct_WithExistingProduct_ShouldReturnActiveVariants() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        createTestVariant(product, "Active Variant 1", "A1", BigDecimal.ZERO, true);
        createTestVariant(product, "Inactive Variant", "I1", BigDecimal.ZERO, false);
        createTestVariant(product, "Active Variant 2", "A2", BigDecimal.ZERO, true);

        // When
        List<ProductVariant> activeVariants = productVariantDomainService.findActiveVariantsByProduct(product.getId());

        // Then
        assertThat(activeVariants).hasSize(2);
        assertThat(activeVariants)
                .extracting(ProductVariant::getName)
                .containsExactlyInAnyOrder("Active Variant 1", "Active Variant 2");
    }

    @Test
    @DisplayName("Should throw exception when finding active variants for non-existent product")
    void findActiveVariantsByProduct_WithNonExistentProduct_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.findActiveVariantsByProduct(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should search variants by name case-insensitively")
    void searchVariantsByName_ShouldReturnMatchingVariants() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        createTestVariant(product, "Front Brake Pads", "FRONT", BigDecimal.ZERO, true);
        createTestVariant(product, "Rear Brake Pads", "REAR", BigDecimal.ZERO, true);
        createTestVariant(product, "Engine Filter", "ENGINE", BigDecimal.ZERO, true);

        // When
        List<ProductVariant> results = productVariantDomainService.searchVariantsByName("brake");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(ProductVariant::getName)
                .containsExactlyInAnyOrder("Front Brake Pads", "Rear Brake Pads");
    }

    // =========================
    // PAGINATION TESTS
    // =========================

    @Test
    @DisplayName("Should find all product variants with pagination")
    void findAllProductVariants_WithPagination_ShouldReturnPagedResults() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        for (int i = 1; i <= 5; i++) {
            createTestVariant(product, "Variant " + i, "V" + i, BigDecimal.ZERO, true);
        }

        // When
        Page<ProductVariant> page = productVariantDomainService.findAllProductVariants(
                PageRequest.of(0, 3)
        );

        // Then
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    // =========================
    // BUSINESS VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should validate that variant exists")
    void existsById_WithExistingVariant_ShouldReturnTrue() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant variant = createTestVariant(product, "Test Variant", "TEST", BigDecimal.ZERO, true);

        // When
        boolean exists = productVariantDomainService.existsById(variant.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should validate that variant does not exist")
    void existsById_WithNonExistentVariant_ShouldReturnFalse() {
        // When
        boolean exists = productVariantDomainService.existsById(999L);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should validate SKU suffix uniqueness for product")
    void isSkuSuffixUniqueForProduct_WithUniqueSkuSuffix_ShouldReturnTrue() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        createTestVariant(product, "Existing Variant", "EXISTING", BigDecimal.ZERO, true);

        // When
        boolean isUnique = productVariantDomainService.isSkuSuffixUniqueForProduct(
                product.getId(), "NEW-SUFFIX"
        );

        // Then
        assertThat(isUnique).isTrue();
    }

    @Test
    @DisplayName("Should validate SKU suffix is not unique for product")
    void isSkuSuffixUniqueForProduct_WithExistingSkuSuffix_ShouldReturnFalse() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        createTestVariant(product, "Existing Variant", "EXISTING", BigDecimal.ZERO, true);

        // When
        boolean isUnique = productVariantDomainService.isSkuSuffixUniqueForProduct(
                product.getId(), "EXISTING"
        );

        // Then
        assertThat(isUnique).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when checking SKU uniqueness for non-existent product")
    void isSkuSuffixUniqueForProduct_WithNonExistentProduct_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.isSkuSuffixUniqueForProduct(999L, "TEST"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should validate active variant for inventory operations")
    void validateVariantForInventoryOperations_WithActiveVariant_ShouldPass() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant activeVariant = createTestVariant(product, "Active Variant", "ACTIVE", BigDecimal.ZERO, true);

        // When & Then
        assertThatCode(() -> productVariantDomainService.validateVariantForInventoryOperations(activeVariant.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when validating inactive variant for inventory operations")
    void validateVariantForInventoryOperations_WithInactiveVariant_ShouldThrowException() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        ProductVariant inactiveVariant = createTestVariant(product, "Inactive Variant", "INACTIVE", BigDecimal.ZERO, false);

        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.validateVariantForInventoryOperations(inactiveVariant.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot perform inventory operations on inactive variant");
    }

    @Test
    @DisplayName("Should allow null variant ID for base product inventory operations")
    void validateVariantForInventoryOperations_WithNullVariantId_ShouldPass() {
        // When & Then
        assertThatCode(() -> productVariantDomainService.validateVariantForInventoryOperations(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when validating non-existent variant for inventory operations")
    void validateVariantForInventoryOperations_WithNonExistentVariant_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.validateVariantForInventoryOperations(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product variant not found with ID: 999");
    }

    // =========================
    // STATISTICS TESTS
    // =========================

    @Test
    @DisplayName("Should calculate variant statistics for product correctly")
    void getVariantStatsForProduct_ShouldReturnCorrectStats() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");
        createTestVariant(product, "Active Variant 1", "A1", BigDecimal.ZERO, true);
        createTestVariant(product, "Active Variant 2", "A2", BigDecimal.ZERO, true);
        createTestVariant(product, "Inactive Variant", "I1", BigDecimal.ZERO, false);

        // When
        ProductVariantDomainService.VariantStats stats = productVariantDomainService.getVariantStatsForProduct(product.getId());

        // Then
        assertThat(stats.getTotalVariants()).isEqualTo(3);
        assertThat(stats.getActiveVariants()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw exception when getting stats for non-existent product")
    void getVariantStatsForProduct_WithNonExistentProduct_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productVariantDomainService.getVariantStatsForProduct(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should return zero stats for product with no variants")
    void getVariantStatsForProduct_WithNoVariants_ShouldReturnZeroStats() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-PRODUCT");

        // When
        ProductVariantDomainService.VariantStats stats = productVariantDomainService.getVariantStatsForProduct(product.getId());

        // Then
        assertThat(stats.getTotalVariants()).isEqualTo(0);
        assertThat(stats.getActiveVariants()).isEqualTo(0);
    }

    // =========================
    // HELPER METHODS
    // =========================

    private Product createTestProduct(String name, String sku) {
        Category category = createTestCategory();
        Product product = Product.builder()
                .sku(sku)
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

    private Category createTestCategory() {
        Category category = Category.builder()
                .name("Test Category")
                .slug("test-category")
                .description("Test category description")
                .displayOrder(1)
                .isActive(true)
                .build();
        return categoryRepository.save(category);
    }

    private ProductVariant createTestVariant(Product product, String name, String skuSuffix, 
                                           BigDecimal priceModifier, boolean isActive) {
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .name(name)
                .skuSuffix(skuSuffix)
                .priceModifier(priceModifier)
                .isActive(isActive)
                .build();
        return productVariantRepository.save(variant);
    }
}
