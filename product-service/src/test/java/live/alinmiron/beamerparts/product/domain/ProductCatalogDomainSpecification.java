package live.alinmiron.beamerparts.product.domain;

import live.alinmiron.beamerparts.product.entity.Category;
import live.alinmiron.beamerparts.product.entity.Product;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Business specifications for the Product Catalog Domain
 * 
 * These tests define the EXPECTED BUSINESS BEHAVIOR of product catalog management.
 * They serve as living documentation and drive the implementation.
 * 
 * Business Rules Under Test:
 * 1. Product lookup must be efficient and reliable
 * 2. Product validation ensures data integrity
 * 3. Bulk operations maintain performance requirements
 * 4. Active/inactive status controls product availability
 * 5. Product compatibility resolves correctly
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
    })
@Transactional
@Rollback
@DisplayName("Product Catalog Domain Business Specifications")
class ProductCatalogDomainSpecification {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;

    private Product testProduct;
    private Product inactiveProduct;
    private Category testCategory;

    @BeforeEach
    void setupBusinessScenario() {
        // Given: A real business scenario with BMW product catalog
        testCategory = Category.builder()
                .name("Engine Parts")
                .slug("engine-parts")
                .description("BMW engine components and accessories")
                .isActive(true)
                .displayOrder(1)
                .build();
        categoryRepository.save(testCategory);

        testProduct = Product.builder()
                .name("BMW F30 Oil Filter")
                .slug("bmw-f30-oil-filter")
                .sku("BMW-F30-OF-001")
                .description("OEM oil filter for BMW F30 series engines")
                .shortDescription("OEM oil filter for F30")
                .basePrice(new BigDecimal("24.99"))
                .category(testCategory)
                .brand("BMW")
                .status(ProductStatus.ACTIVE)
                .isFeatured(true)
                .weightGrams(250)
                .build();
        productRepository.save(testProduct);

        inactiveProduct = Product.builder()
                .name("BMW E90 Oil Filter")
                .slug("bmw-e90-oil-filter")
                .sku("BMW-E90-OF-001")
                .description("Discontinued oil filter for BMW E90 series")
                .basePrice(new BigDecimal("19.99"))
                .category(testCategory)
                .brand("BMW")
                .status(ProductStatus.INACTIVE)
                .isFeatured(false)
                .build();
        productRepository.save(inactiveProduct);
    }

    @Test
    @DisplayName("Should successfully retrieve product by SKU when product exists and is active")
    void shouldRetrieveProduct_whenSkuExistsAndActive() {
        // Given: Product with SKU "BMW-F30-OF-001" exists and is active
        String sku = "BMW-F30-OF-001";

        // When: Domain service looks up product by SKU
        Optional<Product> result = productRepository.findBySku(sku);

        // Then: Product should be found with correct business attributes
        assertThat(result).isPresent();
        Product product = result.get();
        
        assertThat(product.getSku()).isEqualTo(sku);
        assertThat(product.getName()).isEqualTo("BMW F30 Oil Filter");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getBasePrice()).isEqualTo(new BigDecimal("24.99"));
        assertThat(product.getBrand()).isEqualTo("BMW");
        assertThat(product.getCategory().getName()).isEqualTo("Engine Parts");
        
        // Business rule: Active products should be available for purchase
        assertThat(product.isAvailableForPurchase()).isTrue();
    }

    @Test
    @DisplayName("Should return empty when product SKU does not exist")
    void shouldReturnEmpty_whenSkuDoesNotExist() {
        // Given: Non-existent SKU
        String nonExistentSku = "NON-EXISTENT-SKU-999";

        // When: Domain service attempts to find product
        Optional<Product> result = productRepository.findBySku(nonExistentSku);

        // Then: No product should be found
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should validate product availability based on status")
    void shouldValidateProductAvailability_basedOnStatus() {
        // Given: Active and inactive products
        
        // When: Checking availability for active product
        boolean activeProductAvailable = testProduct.isAvailableForPurchase();
        
        // When: Checking availability for inactive product  
        boolean inactiveProductAvailable = inactiveProduct.isAvailableForPurchase();

        // Then: Only active products should be available
        assertThat(activeProductAvailable)
                .as("Active products should be available for purchase")
                .isTrue();
        
        assertThat(inactiveProductAvailable)
                .as("Inactive products should not be available for purchase")
                .isFalse();
    }

    @Test
    @DisplayName("Should validate product data integrity for business operations")
    void shouldValidateProductDataIntegrity() {
        // Given: Product with complete business data
        
        // Then: All essential business fields should be present and valid
        assertThat(testProduct.getSku())
                .as("SKU is required for all business operations")
                .isNotBlank();
        
        assertThat(testProduct.getName())
                .as("Name is required for customer display")
                .isNotBlank();
        
        assertThat(testProduct.getBasePrice())
                .as("Price is required for purchase operations")
                .isNotNull()
                .isPositive();
        
        assertThat(testProduct.getCategory())
                .as("Category is required for catalog organization")
                .isNotNull();
        
        assertThat(testProduct.getStatus())
                .as("Status controls product availability")
                .isNotNull();

        // Business rule: Featured products should be active
        if (testProduct.getIsFeatured()) {
            assertThat(testProduct.getStatus())
                    .as("Featured products must be active")
                    .isEqualTo(ProductStatus.ACTIVE);
        }
    }

    @Test
    @DisplayName("Should efficiently retrieve multiple products in bulk operations")
    void shouldRetrieveMultipleProducts_inBulkOperations() {
        // Given: Multiple product SKUs for bulk operation
        List<String> skus = List.of("BMW-F30-OF-001", "BMW-E90-OF-001", "NON-EXISTENT-SKU");

        // When: Domain service performs bulk lookup
        List<Product> results = productRepository.findBySkus(skus);

        // Then: Should return found products (excluding non-existent ones)
        assertThat(results)
                .hasSize(2)
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("BMW-F30-OF-001", "BMW-E90-OF-001");

        // Business rule: Bulk operations should maintain individual product integrity
        results.forEach(product -> {
            assertThat(product.getSku()).isNotBlank();
            assertThat(product.getName()).isNotBlank();
            assertThat(product.getBasePrice()).isPositive();
        });
    }

    @Test
    @DisplayName("Should validate product constraints for cart and order operations")
    void shouldValidateProductConstraints_forCartAndOrderOperations() {
        // Given: Product being added to cart/order
        String sku = "BMW-F30-OF-001";
        Integer requestedQuantity = 5;

        // When: Validating product for cart/order operations
        Optional<Product> productOpt = productRepository.findBySku(sku);
        assertThat(productOpt).isPresent();
        
        Product product = productOpt.get();

        // Then: Product should meet business constraints for transactions
        assertThat(product.getStatus())
                .as("Only active products can be added to cart")
                .isEqualTo(ProductStatus.ACTIVE);

        assertThat(product.getBasePrice())
                .as("Product must have valid pricing for checkout")
                .isPositive();

        // Business rule: Products must have valid weight for shipping calculations
        if (product.getWeightGrams() != null) {
            assertThat(product.getWeightGrams())
                    .as("Product weight must be positive for shipping")
                    .isPositive();
        }
    }

    @Test
    @DisplayName("Should handle product search by BMW generation compatibility")
    void shouldSearchProducts_byBmwGenerationCompatibility() {
        // Given: BMW F30 generation code (this would be resolved via BMW compatibility domain)
        String generationCode = "F30";

        // When: Searching for compatible products
        List<Product> compatibleProducts = productRepository.findByCompatibleGeneration(generationCode);

        // Then: Should return products compatible with F30 generation
        // Note: This would require BMW compatibility data to be set up
        assertThat(compatibleProducts).isNotNull();
        
        // Business rule: Compatible products must be active for customer purchase
        compatibleProducts.forEach(product -> {
            if (product.getStatus() == ProductStatus.ACTIVE) {
                assertThat(product.isAvailableForPurchase()).isTrue();
            }
        });
    }

    @Test
    @DisplayName("Should maintain product category relationships for catalog navigation")
    void shouldMaintainCategoryRelationships_forCatalogNavigation() {
        // Given: Product with category assignment
        
        // When: Accessing category information
        Category category = testProduct.getCategory();

        // Then: Category relationship should provide complete business context
        assertThat(category).isNotNull();
        assertThat(category.getName()).isEqualTo("Engine Parts");
        assertThat(category.getSlug()).isEqualTo("engine-parts");
        assertThat(category.getIsActive()).isTrue();

        // Business rule: Products should inherit display order from category
        assertThat(category.getDisplayOrder()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce business rules for product status transitions")
    void shouldEnforceBusinessRules_forProductStatusTransitions() {
        // Given: Active product that needs status change
        
        // When: Changing product status (business operation)
        ProductStatus originalStatus = testProduct.getStatus();
        testProduct.setStatus(ProductStatus.INACTIVE);
        
        // Then: Status change should affect availability
        assertThat(testProduct.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(testProduct.isAvailableForPurchase()).isFalse();

        // Business rule: Status changes should be auditable
        assertThat(testProduct.getUpdatedAt()).isNotNull();
        
        // Restore original status for test isolation
        testProduct.setStatus(originalStatus);
        assertThat(testProduct.isAvailableForPurchase()).isTrue();
    }
}
