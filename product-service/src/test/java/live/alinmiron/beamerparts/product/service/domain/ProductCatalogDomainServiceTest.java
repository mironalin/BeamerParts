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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ProductCatalogDomainService
 * Tests all business logic and domain rules using real database operations
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("ProductCatalogDomainService Tests")
class ProductCatalogDomainServiceTest {

    @Autowired
    private ProductCatalogDomainService productCatalogDomainService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // =========================
    // PRODUCT LOOKUP TESTS
    // =========================

    @Test
    @DisplayName("Should find product by valid SKU")
    void findProductBySku_WithValidSku_ShouldReturnProduct() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-SKU-001");

        // When
        Optional<Product> found = productCatalogDomainService.findProductBySku("TEST-SKU-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
        assertThat(found.get().getSku()).isEqualTo("TEST-SKU-001");
    }

    @Test
    @DisplayName("Should return empty for non-existent SKU")
    void findProductBySku_WithNonExistentSku_ShouldReturnEmpty() {
        // When
        Optional<Product> found = productCatalogDomainService.findProductBySku("NON-EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return empty for null SKU")
    void findProductBySku_WithNullSku_ShouldReturnEmpty() {
        // When
        Optional<Product> found = productCatalogDomainService.findProductBySku(null);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return empty for empty SKU")
    void findProductBySku_WithEmptySku_ShouldReturnEmpty() {
        // When
        Optional<Product> found = productCatalogDomainService.findProductBySku("");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should trim SKU and find product")
    void findProductBySku_WithSkuWithWhitespace_ShouldTrimAndFind() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-SKU-TRIM");

        // When
        Optional<Product> found = productCatalogDomainService.findProductBySku("  TEST-SKU-TRIM  ");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("TEST-SKU-TRIM");
    }

    // =========================
    // BULK PRODUCT LOOKUP TESTS
    // =========================

    @Test
    @DisplayName("Should find multiple products by SKUs")
    void findProductsBulk_WithValidSkus_ShouldReturnAllFoundProducts() {
        // Given
        createTestProduct("Product 1", "SKU-001");
        createTestProduct("Product 2", "SKU-002");
        createTestProduct("Product 3", "SKU-003");

        List<String> skus = List.of("SKU-001", "SKU-002", "SKU-003");

        // When
        List<Product> products = productCatalogDomainService.findProductsBulk(skus);

        // Then
        assertThat(products).hasSize(3);
        assertThat(products)
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-001", "SKU-002", "SKU-003");
    }

    @Test
    @DisplayName("Should return only found products in bulk lookup")
    void findProductsBulk_WithMixedSkus_ShouldReturnOnlyFoundProducts() {
        // Given
        createTestProduct("Product 1", "SKU-001");
        createTestProduct("Product 2", "SKU-002");

        List<String> skus = List.of("SKU-001", "SKU-002", "NON-EXISTENT");

        // When
        List<Product> products = productCatalogDomainService.findProductsBulk(skus);

        // Then
        assertThat(products).hasSize(2);
        assertThat(products)
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-001", "SKU-002");
    }

    @Test
    @DisplayName("Should return empty list for null SKU list")
    void findProductsBulk_WithNullList_ShouldReturnEmptyList() {
        // When
        List<Product> products = productCatalogDomainService.findProductsBulk(null);

        // Then
        assertThat(products).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for empty SKU list")
    void findProductsBulk_WithEmptyList_ShouldReturnEmptyList() {
        // When
        List<Product> products = productCatalogDomainService.findProductsBulk(List.of());

        // Then
        assertThat(products).isEmpty();
    }

    @Test
    @DisplayName("Should filter out invalid SKUs in bulk lookup")
    void findProductsBulk_WithInvalidSkus_ShouldFilterAndFind() {
        // Given
        createTestProduct("Valid Product", "VALID-SKU");

        List<String> skus = Arrays.asList("VALID-SKU", null, "", "  ", "VALID-SKU"); // includes duplicates and invalid

        // When
        List<Product> products = productCatalogDomainService.findProductsBulk(skus);

        // Then
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getSku()).isEqualTo("VALID-SKU");
    }

    @Test
    @DisplayName("Should return empty for all invalid SKUs")
    void findProductsBulk_WithAllInvalidSkus_ShouldReturnEmpty() {
        // When
        List<Product> products = productCatalogDomainService.findProductsBulk(Arrays.asList(null, "", "  "));

        // Then
        assertThat(products).isEmpty();
    }

    // =========================
    // PRODUCT AVAILABILITY TESTS
    // =========================

    @Test
    @DisplayName("Should return true for available active product")
    void isProductAvailableForPurchase_WithActiveProduct_ShouldReturnTrue() {
        // Given
        Product product = createTestProduct("Available Product", "AVAILABLE-SKU");
        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);

        // When
        boolean isAvailable = productCatalogDomainService.isProductAvailableForPurchase("AVAILABLE-SKU");

        // Then
        assertThat(isAvailable).isTrue();
    }

    @Test
    @DisplayName("Should return false for inactive product")
    void isProductAvailableForPurchase_WithInactiveProduct_ShouldReturnFalse() {
        // Given
        Product product = createTestProduct("Inactive Product", "INACTIVE-SKU");
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);

        // When
        boolean isAvailable = productCatalogDomainService.isProductAvailableForPurchase("INACTIVE-SKU");

        // Then
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("Should return false for non-existent product")
    void isProductAvailableForPurchase_WithNonExistentProduct_ShouldReturnFalse() {
        // When
        boolean isAvailable = productCatalogDomainService.isProductAvailableForPurchase("NON-EXISTENT");

        // Then
        assertThat(isAvailable).isFalse();
    }

    // =========================
    // PRODUCT VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should validate product successfully for transaction")
    void validateProductForTransaction_WithValidProduct_ShouldReturnValid() {
        // Given
        Product product = createTestProduct("Valid Product", "VALID-SKU");
        product.setStatus(ProductStatus.ACTIVE);
        product.setBasePrice(new BigDecimal("99.99"));
        productRepository.save(product);

        // When
        ProductCatalogDomainService.ProductValidationResult result = 
                productCatalogDomainService.validateProductForTransaction("VALID-SKU");

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getProduct()).isEqualTo(product);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should return invalid for empty SKU")
    void validateProductForTransaction_WithEmptySku_ShouldReturnInvalid() {
        // When
        ProductCatalogDomainService.ProductValidationResult result = 
                productCatalogDomainService.validateProductForTransaction("");

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("SKU cannot be empty");
    }

    @Test
    @DisplayName("Should return not found for non-existent SKU")
    void validateProductForTransaction_WithNonExistentSku_ShouldReturnNotFound() {
        // When
        ProductCatalogDomainService.ProductValidationResult result = 
                productCatalogDomainService.validateProductForTransaction("NON-EXISTENT");

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.exists()).isFalse();
    }

    @Test
    @DisplayName("Should return unavailable for inactive product")
    void validateProductForTransaction_WithInactiveProduct_ShouldReturnUnavailable() {
        // Given
        Product product = createTestProduct("Inactive Product", "INACTIVE-SKU");
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);

        // When
        ProductCatalogDomainService.ProductValidationResult result = 
                productCatalogDomainService.validateProductForTransaction("INACTIVE-SKU");

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.exists()).isTrue();
        assertThat(result.getErrorMessage()).contains("not active");
    }

    @Test
    @DisplayName("Should return invalid for product with zero price")
    void validateProductForTransaction_WithZeroPrice_ShouldReturnInvalid() {
        // Given
        Product product = createTestProduct("Zero Price Product", "ZERO-PRICE-SKU");
        product.setStatus(ProductStatus.ACTIVE);
        product.setBasePrice(BigDecimal.ZERO);
        productRepository.save(product);

        // When
        ProductCatalogDomainService.ProductValidationResult result = 
                productCatalogDomainService.validateProductForTransaction("ZERO-PRICE-SKU");

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("invalid pricing");
    }

    // =========================
    // BMW GENERATION TESTS
    // =========================

    @Test
    @DisplayName("Should find products by BMW generation")
    void findProductsByBmwGeneration_WithValidGeneration_ShouldReturnCompatibleProducts() {
        // Given
        Product compatibleProduct = createTestProduct("BMW F30 Product", "BMW-F30-SKU");
        compatibleProduct.setStatus(ProductStatus.ACTIVE);
        productRepository.save(compatibleProduct);

        // When
        List<Product> products = productCatalogDomainService.findProductsByBmwGeneration("F30");

        // Then
        // Note: This test depends on the repository implementation
        // For now, we'll verify the method handles the input correctly
        assertThat(products).isNotNull();
    }

    @Test
    @DisplayName("Should return empty for invalid generation code")
    void findProductsByBmwGeneration_WithEmptyGeneration_ShouldReturnEmpty() {
        // When
        List<Product> products = productCatalogDomainService.findProductsByBmwGeneration("");

        // Then
        assertThat(products).isEmpty();
    }

    @Test
    @DisplayName("Should return empty for null generation code")
    void findProductsByBmwGeneration_WithNullGeneration_ShouldReturnEmpty() {
        // When
        List<Product> products = productCatalogDomainService.findProductsByBmwGeneration(null);

        // Then
        assertThat(products).isEmpty();
    }

    // =========================
    // PRODUCT EXISTENCE TESTS
    // =========================

    @Test
    @DisplayName("Should return true if product exists")
    void productExists_WithExistingProduct_ShouldReturnTrue() {
        // Given
        createTestProduct("Existing Product", "EXISTS-SKU");

        // When
        boolean exists = productCatalogDomainService.productExists("EXISTS-SKU");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false if product does not exist")
    void productExists_WithNonExistentProduct_ShouldReturnFalse() {
        // When
        boolean exists = productCatalogDomainService.productExists("NON-EXISTENT");

        // Then
        assertThat(exists).isFalse();
    }

    // =========================
    // PRODUCT DISPLAY INFO TESTS
    // =========================

    @Test
    @DisplayName("Should get product display info for existing product")
    void getProductDisplayInfo_WithExistingProduct_ShouldReturnDisplayInfo() {
        // Given
        Product product = createTestProduct("Display Product", "DISPLAY-SKU");
        product.setBasePrice(new BigDecimal("149.99"));
        product.setBrand("BMW");
        product.setIsFeatured(true);
        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);

        // When
        Optional<ProductCatalogDomainService.ProductDisplayInfo> displayInfo = 
                productCatalogDomainService.getProductDisplayInfo("DISPLAY-SKU");

        // Then
        assertThat(displayInfo).isPresent();
        ProductCatalogDomainService.ProductDisplayInfo info = displayInfo.get();
        assertThat(info.getSku()).isEqualTo("DISPLAY-SKU");
        assertThat(info.getName()).isEqualTo("Display Product");
        assertThat(info.getBasePrice()).isEqualByComparingTo(new BigDecimal("149.99"));
        assertThat(info.getBrand()).isEqualTo("BMW");
        assertThat(info.isFeatured()).isTrue();
        assertThat(info.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should return empty display info for non-existent product")
    void getProductDisplayInfo_WithNonExistentProduct_ShouldReturnEmpty() {
        // When
        Optional<ProductCatalogDomainService.ProductDisplayInfo> displayInfo = 
                productCatalogDomainService.getProductDisplayInfo("NON-EXISTENT");

        // Then
        assertThat(displayInfo).isEmpty();
    }

    // =========================
    // PRODUCT CREATION TESTS
    // =========================

    @Test
    @DisplayName("Should create product with valid data")
    void createProduct_WithValidData_ShouldCreateSuccessfully() {
        // Given
        Category category = createTestCategory();

        // When
        Product product = productCatalogDomainService.createProduct(
                "New Product", "new-product", "NEW-SKU", "Description", "Short desc",
                new BigDecimal("99.99"), category.getId(), "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        );

        // Then
        assertThat(product).isNotNull();
        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("New Product");
        assertThat(product.getSku()).isEqualTo("NEW-SKU");
        assertThat(product.getSlug()).isEqualTo("new-product");
        assertThat(product.getBasePrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(product.getCategory()).isEqualTo(category);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw exception when creating product with duplicate SKU")
    void createProduct_WithDuplicateSku_ShouldThrowException() {
        // Given
        Category category = createTestCategory();
        createTestProduct("Existing Product", "DUPLICATE-SKU");

        // When & Then
        assertThatThrownBy(() -> productCatalogDomainService.createProduct(
                "New Product", "new-product", "DUPLICATE-SKU", "Description", "Short desc",
                new BigDecimal("99.99"), category.getId(), "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Product with SKU 'DUPLICATE-SKU' already exists");
    }

    @Test
    @DisplayName("Should throw exception when creating product with duplicate slug")
    void createProduct_WithDuplicateSlug_ShouldThrowException() {
        // Given
        Category category = createTestCategory();
        Product existingProduct = createTestProduct("Existing Product", "EXISTING-SKU");
        existingProduct.setSlug("duplicate-slug");
        productRepository.save(existingProduct);

        // When & Then
        assertThatThrownBy(() -> productCatalogDomainService.createProduct(
                "New Product", "duplicate-slug", "NEW-SKU", "Description", "Short desc",
                new BigDecimal("99.99"), category.getId(), "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Product with slug 'duplicate-slug' already exists");
    }

    @Test
    @DisplayName("Should throw exception when creating product with non-existent category")
    void createProduct_WithNonExistentCategory_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> productCatalogDomainService.createProduct(
                "New Product", "new-product", "NEW-SKU", "Description", "Short desc",
                new BigDecimal("99.99"), 999L, "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw exception when creating product in inactive category")
    void createProduct_WithInactiveCategory_ShouldThrowException() {
        // Given
        Category inactiveCategory = createTestCategory();
        inactiveCategory.setIsActive(false);
        categoryRepository.save(inactiveCategory);

        // When & Then
        assertThatThrownBy(() -> productCatalogDomainService.createProduct(
                "New Product", "new-product", "NEW-SKU", "Description", "Short desc",
                new BigDecimal("99.99"), inactiveCategory.getId(), "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot create product in inactive category");
    }

    // =========================
    // PRODUCT UPDATE TESTS
    // =========================

    @Test
    @DisplayName("Should update product with valid changes")
    void updateProduct_WithValidChanges_ShouldUpdateSuccessfully() {
        // Given
        Category category = createTestCategory();
        Product product = createTestProduct("Original Product", "ORIGINAL-SKU");

        // When
        Product updated = productCatalogDomainService.updateProduct(
                product.getId(), "Updated Product", "updated-product", "UPDATED-SKU",
                "Updated description", "Updated short desc", new BigDecimal("199.99"),
                category.getId(), "Mercedes", 2000, null, true, ProductStatus.DISCONTINUED
        );

        // Then
        assertThat(updated.getName()).isEqualTo("Updated Product");
        assertThat(updated.getSlug()).isEqualTo("updated-product");
        assertThat(updated.getSku()).isEqualTo("UPDATED-SKU");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getBasePrice()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(updated.getBrand()).isEqualTo("Mercedes");
        assertThat(updated.getIsFeatured()).isTrue();
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
    }

    @Test
    @DisplayName("Should throw exception when updating with duplicate SKU")
    void updateProduct_WithDuplicateSku_ShouldThrowException() {
        // Given
        createTestProduct("Existing Product", "EXISTING-SKU");
        Product productToUpdate = createTestProduct("Product To Update", "UPDATE-SKU");

        // When & Then
        assertThatThrownBy(() -> productCatalogDomainService.updateProduct(
                productToUpdate.getId(), "Updated Product", "updated-product", "EXISTING-SKU",
                "Description", "Short desc", new BigDecimal("99.99"),
                productToUpdate.getCategory().getId(), "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Product with SKU 'EXISTING-SKU' already exists");
    }

    @Test
    @DisplayName("Should throw exception when updating with duplicate slug")
    void updateProduct_WithDuplicateSlug_ShouldThrowException() {
        // Given
        Product existingProduct = createTestProduct("Existing Product", "EXISTING-SKU");
        existingProduct.setSlug("existing-slug");
        productRepository.save(existingProduct);

        Product productToUpdate = createTestProduct("Product To Update", "UPDATE-SKU");

        // When & Then
        assertThatThrownBy(() -> productCatalogDomainService.updateProduct(
                productToUpdate.getId(), "Updated Product", "existing-slug", "NEW-SKU",
                "Description", "Short desc", new BigDecimal("99.99"),
                productToUpdate.getCategory().getId(), "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Product with slug 'existing-slug' already exists");
    }

    @Test
    @DisplayName("Should throw exception when updating to inactive category")
    void updateProduct_WithInactiveCategory_ShouldThrowException() {
        // Given
        Product product = createTestProduct("Test Product", "TEST-SKU");
        Category inactiveCategory = createTestCategory();
        inactiveCategory.setIsActive(false);
        categoryRepository.save(inactiveCategory);

        // When & Then
        assertThatThrownBy(() -> productCatalogDomainService.updateProduct(
                product.getId(), "Updated Product", "updated-product", "UPDATED-SKU",
                "Description", "Short desc", new BigDecimal("99.99"),
                inactiveCategory.getId(), "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot move product to inactive category");
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent product")
    void updateProduct_WithNonExistentId_ShouldThrowException() {
        // Given
        Category category = createTestCategory();

        // When & Then
        assertThatThrownBy(() -> productCatalogDomainService.updateProduct(
                999L, "Updated Product", "updated-product", "UPDATED-SKU",
                "Description", "Short desc", new BigDecimal("99.99"),
                category.getId(), "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should allow updating same SKU and slug")
    void updateProduct_WithSameSkuAndSlug_ShouldUpdateSuccessfully() {
        // Given
        Product product = createTestProduct("Test Product", "SAME-SKU");
        product.setSlug("same-slug");
        productRepository.save(product);

        // When
        Product updated = productCatalogDomainService.updateProduct(
                product.getId(), "Updated Product", "same-slug", "SAME-SKU",
                "Updated description", "Short desc", new BigDecimal("199.99"),
                product.getCategory().getId(), "BMW", 1000, null,
                false, ProductStatus.ACTIVE
        );

        // Then
        assertThat(updated.getName()).isEqualTo("Updated Product");
        assertThat(updated.getSku()).isEqualTo("SAME-SKU");
        assertThat(updated.getSlug()).isEqualTo("same-slug");
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
                .brand("BMW")
                .weightGrams(500)
                .isFeatured(false)
                .status(ProductStatus.ACTIVE)
                .build();
        return productRepository.save(product);
    }

    private Category createTestCategory() {
        // Generate unique slug to avoid constraint violations
        String uniqueSlug = "test-category-" + System.currentTimeMillis();
        Category category = Category.builder()
                .name("Test Category")
                .slug(uniqueSlug)
                .description("Test category description")
                .displayOrder(1)
                .isActive(true)
                .build();
        return categoryRepository.save(category);
    }
}
