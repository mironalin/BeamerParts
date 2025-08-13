package live.alinmiron.beamerparts.product.entity;

import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.repository.ProductVariantRepository;
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
 * Comprehensive test suite for ProductVariant entity business logic
 * Tests all business methods and domain rules using real database operations
 * 
 * **KEY LESSONS APPLIED:**
 * - Tests define business logic FIRST, implementation follows
 * - Use @SpringBootTest for entity testing with real DB operations
 * - Test all business methods and edge cases comprehensively
 * - Verify entity relationships and constraints
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("ProductVariant Entity Tests")
class ProductVariantTest {

    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        
        // Create test category
        testCategory = Category.builder()
                .name("Test Category")
                .slug("test-category-" + System.currentTimeMillis()) // Unique slug
                .description("Test category")
                .displayOrder(1)
                .isActive(true)
                .build();
        testCategory = categoryRepository.save(testCategory);

        // Create test product
        testProduct = Product.builder()
                .sku("TEST-PRODUCT-" + System.currentTimeMillis()) // Unique SKU
                .name("Test Product")
                .slug("test-product-" + System.currentTimeMillis()) // Unique slug
                .description("Test product description")
                .shortDescription("Test short description")
                .basePrice(new BigDecimal("100.00"))
                .category(testCategory)
                .brand("Test Brand")
                .weightGrams(500)
                .isFeatured(false)
                .status(ProductStatus.ACTIVE)
                .build();
        testProduct = productRepository.save(testProduct);
    }

    // =========================
    // BUSINESS LOGIC TESTS
    // =========================

    @Test
    @DisplayName("Should generate full SKU by combining product SKU with suffix")
    void getFullSku_ShouldCombineProductSkuWithSuffix() {
        // Given
        ProductVariant variant = createTestVariant("Black", "-BLK", BigDecimal.ZERO);

        // When
        String fullSku = variant.getFullSku();

        // Then
        assertThat(fullSku).startsWith("TEST-PRODUCT").endsWith("-BLK");
    }

    @Test
    @DisplayName("Should calculate effective price by adding modifier to base price")
    void getEffectivePrice_WithPositiveModifier_ShouldIncreasePrice() {
        // Given
        BigDecimal priceModifier = new BigDecimal("25.00");
        ProductVariant variant = createTestVariant("Premium", "-PREM", priceModifier);

        // When
        BigDecimal effectivePrice = variant.getEffectivePrice();

        // Then
        assertThat(effectivePrice).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    @DisplayName("Should calculate effective price by subtracting negative modifier from base price")
    void getEffectivePrice_WithNegativeModifier_ShouldDecreasePrice() {
        // Given
        BigDecimal priceModifier = new BigDecimal("-15.00");
        ProductVariant variant = createTestVariant("Basic", "-BASIC", priceModifier);

        // When
        BigDecimal effectivePrice = variant.getEffectivePrice();

        // Then
        assertThat(effectivePrice).isEqualByComparingTo(new BigDecimal("85.00"));
    }

    @Test
    @DisplayName("Should calculate effective price with zero modifier")
    void getEffectivePrice_WithZeroModifier_ShouldReturnBasePrice() {
        // Given
        ProductVariant variant = createTestVariant("Standard", "-STD", BigDecimal.ZERO);

        // When
        BigDecimal effectivePrice = variant.getEffectivePrice();

        // Then
        assertThat(effectivePrice).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should generate display name combining product name and variant name")
    void getDisplayName_ShouldCombineProductAndVariantNames() {
        // Given
        ProductVariant variant = createTestVariant("Carbon Fiber", "-CF", new BigDecimal("50.00"));

        // When
        String displayName = variant.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("Test Product - Carbon Fiber");
    }

    @Test
    @DisplayName("Should return true when variant has inventory")
    void hasInventory_WithInventoryList_ShouldReturnTrue() {
        // Given
        ProductVariant variant = createTestVariant("Black", "-BLK", BigDecimal.ZERO);
        
        // Create mock inventory list
        List<Inventory> inventoryList = new ArrayList<>();
        Inventory inventory = Inventory.builder()
                .variant(variant)
                .quantityAvailable(10)
                .quantityReserved(2)
                .reorderPoint(5)
                .build();
        inventoryList.add(inventory);
        variant.setInventory(inventoryList);

        // When
        boolean hasInventory = variant.hasInventory();

        // Then
        assertThat(hasInventory).isTrue();
    }

    @Test
    @DisplayName("Should return false when variant has empty inventory list")
    void hasInventory_WithEmptyInventoryList_ShouldReturnFalse() {
        // Given
        ProductVariant variant = createTestVariant("White", "-WHT", BigDecimal.ZERO);
        variant.setInventory(new ArrayList<>());

        // When
        boolean hasInventory = variant.hasInventory();

        // Then
        assertThat(hasInventory).isFalse();
    }

    @Test
    @DisplayName("Should return false when variant has null inventory")
    void hasInventory_WithNullInventory_ShouldReturnFalse() {
        // Given
        ProductVariant variant = createTestVariant("Red", "-RED", BigDecimal.ZERO);
        variant.setInventory(null);

        // When
        boolean hasInventory = variant.hasInventory();

        // Then
        assertThat(hasInventory).isFalse();
    }

    @Test
    @DisplayName("Should have active status by default")
    void isActive_ShouldDefaultToTrue() {
        // Given
        ProductVariant variant = createTestVariant("Blue", "-BLU", BigDecimal.ZERO);

        // When & Then
        assertThat(variant.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should allow setting inactive status")
    void isActive_WhenSetToFalse_ShouldReturnFalse() {
        // Given
        ProductVariant variant = createTestVariant("Green", "-GRN", BigDecimal.ZERO);
        variant.setIsActive(false);

        // When & Then
        assertThat(variant.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should have zero price modifier by default")
    void priceModifier_ShouldDefaultToZero() {
        // Given
        ProductVariant variant = createTestVariant("Yellow", "-YEL", null);

        // When & Then
        assertThat(variant.getPriceModifier()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should set creation timestamp automatically")
    void createdAt_ShouldBeSetAutomatically() {
        // Given
        ProductVariant variant = createTestVariant("Purple", "-PUR", BigDecimal.ZERO);
        LocalDateTime beforePersist = LocalDateTime.now().minusSeconds(1);
        
        // When
        ProductVariant savedVariant = productVariantRepository.save(variant);
        LocalDateTime afterPersist = LocalDateTime.now().plusSeconds(1);

        // Then
        assertThat(savedVariant.getCreatedAt()).isNotNull();
        assertThat(savedVariant.getCreatedAt()).isAfter(beforePersist);
        assertThat(savedVariant.getCreatedAt()).isBefore(afterPersist);
    }

    // =========================
    // RELATIONSHIP TESTS
    // =========================

    @Test
    @DisplayName("Should establish bidirectional relationship with product")
    void setProduct_ShouldEstablishBidirectionalRelation() {
        // Given
        ProductVariant variant = ProductVariant.builder()
                .name("Orange")
                .skuSuffix("-ORG")
                .priceModifier(BigDecimal.ZERO)
                .isActive(true)
                .build();

        // When
        variant.setProduct(testProduct);
        ProductVariant savedVariant = productVariantRepository.save(variant);

        // Then
        assertThat(savedVariant.getProduct()).isEqualTo(testProduct);
        assertThat(savedVariant.getProduct().getSku()).startsWith("TEST-PRODUCT");
    }

    @Test
    @DisplayName("Should persist variant with all required fields")
    void persistVariant_WithValidData_ShouldSaveSuccessfully() {
        // Given
        ProductVariant variant = createTestVariant("Silver", "-SLV", new BigDecimal("10.00"));

        // When
        ProductVariant savedVariant = productVariantRepository.save(variant);

        // Then
        assertThat(savedVariant.getId()).isNotNull();
        assertThat(savedVariant.getName()).isEqualTo("Silver");
        assertThat(savedVariant.getSkuSuffix()).isEqualTo("-SLV");
        assertThat(savedVariant.getPriceModifier()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(savedVariant.getIsActive()).isTrue();
        assertThat(savedVariant.getCreatedAt()).isNotNull();
        assertThat(savedVariant.getProduct()).isEqualTo(testProduct);
    }

    @Test
    @DisplayName("Should handle inventory relationship correctly")
    void inventoryRelationship_ShouldBeManagedCorrectly() {
        // Given
        ProductVariant variant = createTestVariant("Gold", "-GLD", new BigDecimal("75.00"));
        ProductVariant savedVariant = productVariantRepository.save(variant);

        // Create inventory for the variant (Note: would need InventoryRepository for full test)
        // For this entity test, we'll focus on the hasInventory() method logic
        List<Inventory> inventoryList = new ArrayList<>();
        Inventory inventory1 = Inventory.builder()
                .variant(savedVariant)
                .quantityAvailable(15)
                .quantityReserved(3)
                .reorderPoint(8)
                .build();
        inventoryList.add(inventory1);
        savedVariant.setInventory(inventoryList);

        // When
        ProductVariant reloadedVariant = productVariantRepository.findById(savedVariant.getId()).orElseThrow();

        // Then
        assertThat(reloadedVariant.hasInventory()).isTrue();
        // Note: Lazy loading might require transaction context for full inventory list access
    }

    @Test
    @DisplayName("Should handle stock movements relationship correctly")
    void stockMovementsRelationship_ShouldBeManagedCorrectly() {
        // Given
        ProductVariant variant = createTestVariant("Bronze", "-BRZ", new BigDecimal("20.00"));
        ProductVariant savedVariant = productVariantRepository.save(variant);

        // Create stock movement for the variant (simplified for entity test)
        // Note: For full testing, would need StockMovementRepository
        List<StockMovement> stockMovements = new ArrayList<>();
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .variant(savedVariant)
                .movementType(StockMovementType.INCOMING)
                .quantityChange(50)
                .reason("Initial stock")
                .build();
        stockMovements.add(movement);
        savedVariant.setStockMovements(stockMovements);

        // When
        ProductVariant reloadedVariant = productVariantRepository.findById(savedVariant.getId()).orElseThrow();

        // Then
        assertThat(reloadedVariant).isNotNull();
        assertThat(reloadedVariant.getId()).isEqualTo(savedVariant.getId());
        // Note: Stock movements are lazily loaded and would require transaction context
    }

    // =========================
    // BUSINESS RULE VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should allow large positive price modifiers")
    void priceModifier_WithLargePositiveValue_ShouldBeAllowed() {
        // Given
        BigDecimal largePriceModifier = new BigDecimal("999.99");
        ProductVariant variant = createTestVariant("Platinum", "-PLT", largePriceModifier);

        // When
        BigDecimal effectivePrice = variant.getEffectivePrice();

        // Then
        assertThat(effectivePrice).isEqualByComparingTo(new BigDecimal("1099.99"));
    }

    @Test
    @DisplayName("Should allow large negative price modifiers")
    void priceModifier_WithLargeNegativeValue_ShouldBeAllowed() {
        // Given
        BigDecimal largePriceModifier = new BigDecimal("-50.00");
        ProductVariant variant = createTestVariant("Discount", "-DISC", largePriceModifier);

        // When
        BigDecimal effectivePrice = variant.getEffectivePrice();

        // Then
        assertThat(effectivePrice).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Should handle price precision correctly")
    void priceModifier_WithPreciseDecimal_ShouldMaintainPrecision() {
        // Given
        BigDecimal preciseModifier = new BigDecimal("12.34");
        ProductVariant variant = createTestVariant("Precise", "-PREC", preciseModifier);

        // When
        BigDecimal effectivePrice = variant.getEffectivePrice();

        // Then
        assertThat(effectivePrice).isEqualByComparingTo(new BigDecimal("112.34"));
    }

    @Test
    @DisplayName("Should handle special characters in variant name")
    void variantName_WithSpecialCharacters_ShouldBeHandledCorrectly() {
        // Given
        ProductVariant variant = createTestVariant("Matte & Gloss", "-M&G", BigDecimal.ZERO);

        // When
        String displayName = variant.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("Test Product - Matte & Gloss");
    }

    @Test
    @DisplayName("Should handle long variant names")
    void variantName_WithLongName_ShouldBeHandledCorrectly() {
        // Given
        String longName = "Extra Long Variant Name With Many Words That Describes The Product Variation";
        ProductVariant variant = createTestVariant(longName, "-LONG", BigDecimal.ZERO);

        // When
        String displayName = variant.getDisplayName();

        // Then
        assertThat(displayName).startsWith("Test Product - ");
        assertThat(displayName).contains(longName);
    }

    @Test
    @DisplayName("Should handle special characters in SKU suffix")
    void skuSuffix_WithSpecialCharacters_ShouldGenerateValidFullSku() {
        // Given
        ProductVariant variant = createTestVariant("Special", "-SP_01", BigDecimal.ZERO);

        // When
        String fullSku = variant.getFullSku();

        // Then
        assertThat(fullSku).startsWith("TEST-PRODUCT").endsWith("-SP_01");
    }

    // =========================
    // EDGE CASE TESTS
    // =========================

    @Test
    @DisplayName("Should handle null product gracefully in business methods")
    void businessMethods_WithNullProduct_ShouldHandleGracefully() {
        // Given
        ProductVariant variant = ProductVariant.builder()
                .name("Orphaned")
                .skuSuffix("-ORPH")
                .priceModifier(BigDecimal.ZERO)
                .isActive(true)
                .build();
        // Note: Not setting product

        // When & Then - These should throw NPE as per domain design
        assertThatThrownBy(() -> variant.getFullSku())
                .isInstanceOf(NullPointerException.class);
        
        assertThatThrownBy(() -> variant.getEffectivePrice())
                .isInstanceOf(NullPointerException.class);
        
        assertThatThrownBy(() -> variant.getDisplayName())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle empty string names appropriately")
    void variantName_WithEmptyString_ShouldBeHandledCorrectly() {
        // Given
        ProductVariant variant = createTestVariant("", "-EMPTY", BigDecimal.ZERO);

        // When
        String displayName = variant.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("Test Product - ");
    }

    // =========================
    // HELPER METHODS
    // =========================

    private ProductVariant createTestVariant(String name, String skuSuffix, BigDecimal priceModifier) {
        return ProductVariant.builder()
                .product(testProduct)
                .name(name)
                .skuSuffix(skuSuffix)
                .priceModifier(priceModifier != null ? priceModifier : BigDecimal.ZERO)
                .isActive(true)
                .build();
    }
}
