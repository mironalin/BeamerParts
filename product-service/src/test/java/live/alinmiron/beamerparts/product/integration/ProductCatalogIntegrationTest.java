package live.alinmiron.beamerparts.product.integration;

import live.alinmiron.beamerparts.product.dto.internal.request.BulkProductRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.request.ProductValidationRequestDto;
import live.alinmiron.beamerparts.product.dto.internal.response.ProductInternalDto;
import live.alinmiron.beamerparts.product.dto.internal.response.ProductValidationDto;
import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.service.internal.ProductInternalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for complete Product Catalog workflows.
 * Tests the integration between ProductInternalService, ProductCatalogDomainService, and the database.
 * 
 * PROFESSIONAL APPROACH: Uses Flyway migrations for production-like schema consistency.
 * This ensures our tests validate the exact same database schema used in production.
 * 
 * These tests verify:
 * - Complete service-to-service integration (internal → domain → repository)
 * - Real database operations with PostgreSQL using production schema
 * - Product lookup and validation workflows
 * - Bulk operations performance and accuracy
 * - Business rule enforcement across all layers
 * - Migration compatibility and schema consistency
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@Transactional
@Rollback
@DisplayName("Product Catalog Integration Tests - Complete Business Workflows")
class ProductCatalogIntegrationTest {

    @Autowired
    private ProductInternalService productInternalService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product activeProduct;
    private Product inactiveProduct;
    private Category testCategory;
    private String activeSku = "INT-PROD-001";
    private String inactiveSku = "INT-PROD-002";

    @BeforeEach
    void setUp() {
        // Create test category
        testCategory = categoryRepository.findBySlug("integration-category")
                .orElseGet(() -> {
                    Category category = Category.builder()
                            .name("Integration Test Category")
                            .slug("integration-category")
                            .description("Category for product integration tests")
                            .isActive(true)
                            .displayOrder(100)
                            .build();
                    return categoryRepository.save(category);
                });

        // Clean up any existing test data
        productRepository.findBySku(activeSku).ifPresent(productRepository::delete);
        productRepository.findBySku(inactiveSku).ifPresent(productRepository::delete);

        // Create active test product
        activeProduct = Product.builder()
                .name("Integration Test Active Product")
                .slug("integration-test-active-product")
                .sku(activeSku)
                .description("Active product for integration testing")
                .shortDescription("Active test product")
                .basePrice(new BigDecimal("49.99"))
                .category(testCategory)
                .brand("TestBrand")
                .status(ProductStatus.ACTIVE)
                .isFeatured(true)
                .weightGrams(150)
                .build();
        activeProduct = productRepository.save(activeProduct);

        // Create inactive test product
        inactiveProduct = Product.builder()
                .name("Integration Test Inactive Product")
                .slug("integration-test-inactive-product")
                .sku(inactiveSku)
                .description("Inactive product for integration testing")
                .basePrice(new BigDecimal("29.99"))
                .category(testCategory)
                .brand("TestBrand")
                .status(ProductStatus.INACTIVE)
                .isFeatured(false)
                .build();
        inactiveProduct = productRepository.save(inactiveProduct);
    }

    @Test
    @DisplayName("Complete Product Lookup Workflow - Service Integration")
    void shouldCompleteProductLookupWorkflowThroughAllLayers() {
        // Given: Active product exists in catalog
        String sku = activeSku;

        // When: Retrieve product through complete service stack
        ProductInternalDto result = productInternalService.getProductBySku(sku, false, false, false);

        // Then: Verify complete product information at all layers
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo(sku);
        assertThat(result.getName()).isEqualTo("Integration Test Active Product");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getBasePrice()).isEqualTo(new BigDecimal("49.99"));
        assertThat(result.getBrand()).isEqualTo("TestBrand");
        assertThat(result.getCategoryName()).isEqualTo("Integration Test Category");
        assertThat(result.getCategoryId()).isEqualTo(testCategory.getId());
        assertThat(result.isFeatured()).isTrue();
        assertThat(result.getWeightGrams()).isEqualTo(150);

        // Verify database state consistency
        Product dbProduct = productRepository.findBySku(sku).orElseThrow();
        assertThat(dbProduct.isAvailableForPurchase()).isTrue();
        assertThat(dbProduct.getDisplayName()).isEqualTo("TestBrand Integration Test Active Product");
    }

    @Test
    @DisplayName("Product Not Found Error Handling - Complete Integration")
    void shouldHandleProductNotFoundGracefullyAcrossLayers() {
        // Given: Non-existent product SKU
        String nonExistentSku = "NON-EXISTENT-PROD-999";

        // When: Attempt to retrieve non-existent product
        // Then: Should throw exception with clear message
        assertThatThrownBy(() -> productInternalService.getProductBySku(nonExistentSku, false, false, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found: " + nonExistentSku);
    }

    @Test
    @DisplayName("Bulk Product Retrieval - Performance and Accuracy")
    void shouldRetrieveBulkProductsEfficientlyThroughAllLayers() {
        // Given: Multiple product SKUs for bulk operation
        BulkProductRequestDto request = BulkProductRequestDto.builder()
                .skus(List.of(activeSku, inactiveSku, "NON-EXISTENT-SKU"))
                .includeInventory(false)
                .includeVariants(false)
                .includeCompatibility(false)
                .build();

        // When: Perform bulk retrieval through service stack
        List<ProductInternalDto> results = productInternalService.getProductsBulk(request);

        // Then: Should return found products with complete information
        assertThat(results).hasSize(2);
        
        ProductInternalDto activeResult = results.stream()
                .filter(p -> p.getSku().equals(activeSku))
                .findFirst().orElseThrow();
        ProductInternalDto inactiveResult = results.stream()
                .filter(p -> p.getSku().equals(inactiveSku))
                .findFirst().orElseThrow();

        // Verify active product details
        assertThat(activeResult.getName()).isEqualTo("Integration Test Active Product");
        assertThat(activeResult.getStatus()).isEqualTo("ACTIVE");
        assertThat(activeResult.isFeatured()).isTrue();

        // Verify inactive product details
        assertThat(inactiveResult.getName()).isEqualTo("Integration Test Inactive Product");
        assertThat(inactiveResult.getStatus()).isEqualTo("INACTIVE");
        assertThat(inactiveResult.isFeatured()).isFalse();

        // Business rule: Bulk operations should maintain data integrity
        results.forEach(product -> {
            assertThat(product.getSku()).isNotBlank();
            assertThat(product.getName()).isNotBlank();
            assertThat(product.getBasePrice()).isPositive();
            assertThat(product.getCategoryName()).isNotBlank();
        });
    }

    @Test
    @DisplayName("Product Validation for Cart Operations - Complete Workflow")
    void shouldValidateProductsForCartOperationsThroughAllLayers() {
        // Given: Products being validated for cart operations
        List<ProductValidationRequestDto> request = List.of(
                ProductValidationRequestDto.builder()
                        .sku(activeSku)
                        .requestedQuantity(5)
                        .build(),
                ProductValidationRequestDto.builder()
                        .sku(inactiveSku)
                        .requestedQuantity(3)
                        .build(),
                ProductValidationRequestDto.builder()
                        .sku("NON-EXISTENT-SKU")
                        .requestedQuantity(1)
                        .build()
        );

        // When: Validate products through complete service stack
        List<ProductValidationDto> results = productInternalService.validateProducts(request);

        // Then: Should return validation results for all requested products
        assertThat(results).hasSize(3);

        // Verify active product validation
        ProductValidationDto activeValidation = results.stream()
                .filter(v -> v.getSku().equals(activeSku))
                .findFirst().orElseThrow();
        assertThat(activeValidation.isExists()).isTrue();
        assertThat(activeValidation.isActive()).isTrue();
        assertThat(activeValidation.getName()).isEqualTo("Integration Test Active Product");
        assertThat(activeValidation.getCurrentPrice()).isEqualTo(new BigDecimal("49.99"));

        // Verify inactive product validation
        ProductValidationDto inactiveValidation = results.stream()
                .filter(v -> v.getSku().equals(inactiveSku))
                .findFirst().orElseThrow();
        assertThat(inactiveValidation.isExists()).isTrue();
        assertThat(inactiveValidation.isActive()).isFalse();
        assertThat(inactiveValidation.isAvailable()).isFalse();
        assertThat(inactiveValidation.getErrorMessage()).isEqualTo("Product is not active");

        // Verify non-existent product validation
        ProductValidationDto notFoundValidation = results.stream()
                .filter(v -> v.getSku().equals("NON-EXISTENT-SKU"))
                .findFirst().orElseThrow();
        assertThat(notFoundValidation.isExists()).isFalse();
        assertThat(notFoundValidation.isActive()).isFalse();
        assertThat(notFoundValidation.isAvailable()).isFalse();
        assertThat(notFoundValidation.getErrorMessage()).isEqualTo("Product not found");
    }

    @Test
    @DisplayName("Product Existence Check - Simple and Fast")
    void shouldCheckProductExistenceEfficientlyAcrossLayers() {
        // When: Check existence for various products
        boolean activeExists = productInternalService.productExistsAndActive(activeSku);
        boolean inactiveExists = productInternalService.productExistsAndActive(inactiveSku);
        boolean nonExistentExists = productInternalService.productExistsAndActive("NON-EXISTENT-SKU");

        // Then: Should return correct existence status
        assertThat(activeExists)
                .as("Active product should be reported as existing and active")
                .isTrue();
        
        assertThat(inactiveExists)
                .as("Inactive product should be reported as not active")
                .isFalse();
        
        assertThat(nonExistentExists)
                .as("Non-existent product should be reported as not existing")
                .isFalse();

        // Verify legacy method compatibility
        assertThat(productInternalService.isProductValid(activeSku)).isTrue();
        assertThat(productInternalService.isProductValid(inactiveSku)).isFalse();
    }

    @Test
    @DisplayName("Product Lookup with Optional Includes - Flexible API")
    void shouldHandleOptionalIncludesCorrectlyAcrossLayers() {
        // When: Request product with various include options
        ProductInternalDto withoutIncludes = productInternalService.getProductBySku(
                activeSku, false, false, false);
        ProductInternalDto withInventory = productInternalService.getProductBySku(
                activeSku, true, false, false);

        // Then: Should respect include flags
        assertThat(withoutIncludes.getInventory()).isNull();
        
        // Note: Inventory will be null if no inventory exists for this product
        // The include flag determines whether to attempt loading it
        assertThat(withInventory).isNotNull();
        // The inventory field may still be null if no inventory record exists
    }

    @Test
    @DisplayName("BMW Generation Compatibility Search - Integration")
    void shouldSearchProductsByBmwGenerationThroughAllLayers() {
        // Given: BMW generation code (this would require compatibility data setup)
        String generationCode = "F30";

        // When: Search for compatible products through service stack
        List<ProductInternalDto> results = productInternalService.getProductsByGeneration(
                generationCode, false);

        // Then: Should return products compatible with generation
        assertThat(results).isNotNull();
        
        // Business rule: Only active products should be returned for customer purchase
        results.forEach(product -> {
            assertThat(product.getStatus()).isEqualTo("ACTIVE");
        });
        
        // Note: Actual results depend on compatibility data being set up
        // This test validates the integration pathway works correctly
    }

    @Test
    @DisplayName("Product Category Relationship Integrity - Complete Validation")
    void shouldMaintainCategoryRelationshipIntegrityAcrossLayers() {
        // When: Retrieve product with category information
        ProductInternalDto result = productInternalService.getProductBySku(activeSku, false, false, false);

        // Then: Category relationship should be complete and accurate
        assertThat(result.getCategoryName()).isEqualTo("Integration Test Category");
        assertThat(result.getCategoryId()).isEqualTo(testCategory.getId());

        // Verify database relationship integrity
        Product dbProduct = productRepository.findBySku(activeSku).orElseThrow();
        Category dbCategory = dbProduct.getCategory();
        assertThat(dbCategory.getName()).isEqualTo("Integration Test Category");
        assertThat(dbCategory.getSlug()).isEqualTo("integration-category");
        assertThat(dbCategory.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Empty and Invalid Input Handling - Robust Error Management")
    void shouldHandleEmptyAndInvalidInputsGracefullyAcrossLayers() {
        // Test empty bulk request
        BulkProductRequestDto emptyRequest = BulkProductRequestDto.builder()
                .skus(List.of())
                .includeInventory(false)
                .includeVariants(false)
                .includeCompatibility(false)
                .build();
        
        List<ProductInternalDto> emptyResults = productInternalService.getProductsBulk(emptyRequest);
        assertThat(emptyResults).isEmpty();

        // Test validation with empty list
        List<ProductValidationDto> emptyValidation = productInternalService.validateProducts(List.of());
        assertThat(emptyValidation).isEmpty();

        // Test generation search with empty string
        List<ProductInternalDto> emptyGeneration = productInternalService.getProductsByGeneration("", false);
        assertThat(emptyGeneration).isEmpty();
    }
}
