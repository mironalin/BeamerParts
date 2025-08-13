package live.alinmiron.beamerparts.product.entity;

import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.InventoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Inventory entity business logic, rich domain methods, and constraints.
 * Tests stock management, business rules, persistence, and relationship integrity.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("Inventory Entity Tests")
class InventoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;

    private Product testProduct;
    private ProductVariant testVariant;
    private Category testCategory;
    private long testIdCounter;

    @BeforeEach
    void setUp() {
        testIdCounter = System.currentTimeMillis();
        testCategory = createAndSaveCategory("Test Category " + testIdCounter, "test-category-" + testIdCounter);
        testProduct = createAndSaveProduct("Test Product " + testIdCounter, "test-product-" + testIdCounter);
        testVariant = createAndSaveVariant(testProduct, "XL", "Extra Large");
    }

    // =================== Business Logic Tests - Core Methods ===================

    @Test
    @DisplayName("getTotalQuantity() should return sum of available and reserved quantities")
    void businessLogic_GetTotalQuantity_ShouldReturnCorrectSum() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 25, 10, 5);

        // When
        Integer totalQuantity = inventory.getTotalQuantity();

        // Then
        assertThat(totalQuantity).isEqualTo(125); // 100 + 25
    }

    @Test
    @DisplayName("getTotalQuantity() should handle zero quantities correctly")
    void businessLogic_GetTotalQuantityWithZeros_ShouldReturnZero() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 0, 0, 10, 5);

        // When
        Integer totalQuantity = inventory.getTotalQuantity();

        // Then
        assertThat(totalQuantity).isEqualTo(0);
    }

    @Test
    @DisplayName("isLowStock() should return true when available quantity is at or below reorder point")
    void businessLogic_IsLowStock_ShouldReturnTrueWhenAtOrBelowReorderPoint() {
        // Given
        Inventory atReorderPoint = createTestInventory(testProduct, null, 10, 5, 10, 5);
        Inventory belowReorderPoint = createTestInventory(testProduct, null, 5, 5, 10, 5);

        // When & Then
        assertThat(atReorderPoint.isLowStock()).isTrue();
        assertThat(belowReorderPoint.isLowStock()).isTrue();
    }

    @Test
    @DisplayName("isLowStock() should return false when available quantity is above reorder point")
    void businessLogic_IsLowStock_ShouldReturnFalseWhenAboveReorderPoint() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 15, 5, 10, 5);

        // When & Then
        assertThat(inventory.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("isOutOfStock() should return true when available quantity is zero")
    void businessLogic_IsOutOfStock_ShouldReturnTrueWhenZeroAvailable() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 0, 10, 10, 5);

        // When & Then
        assertThat(inventory.isOutOfStock()).isTrue();
    }

    @Test
    @DisplayName("isOutOfStock() should return false when available quantity is greater than zero")
    void businessLogic_IsOutOfStock_ShouldReturnFalseWhenAvailable() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 1, 5, 10, 5);

        // When & Then
        assertThat(inventory.isOutOfStock()).isFalse();
    }

    @Test
    @DisplayName("isBelowMinimum() should return true when available quantity is below minimum stock level")
    void businessLogic_IsBelowMinimum_ShouldReturnTrueWhenBelowMinimum() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 3, 5, 10, 5);

        // When & Then
        assertThat(inventory.isBelowMinimum()).isTrue();
    }

    @Test
    @DisplayName("isBelowMinimum() should return false when available quantity is at or above minimum stock level")
    void businessLogic_IsBelowMinimum_ShouldReturnFalseWhenAtOrAboveMinimum() {
        // Given
        Inventory atMinimum = createTestInventory(testProduct, null, 5, 5, 10, 5);
        Inventory aboveMinimum = createTestInventory(testProduct, null, 10, 5, 10, 5);

        // When & Then
        assertThat(atMinimum.isBelowMinimum()).isFalse();
        assertThat(aboveMinimum.isBelowMinimum()).isFalse();
    }

    // =================== Business Logic Tests - Reservation Methods ===================

    @Test
    @DisplayName("canReserve() should return true when sufficient quantity available")
    void businessLogic_CanReserve_ShouldReturnTrueWhenSufficientQuantity() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 10, 20, 5);

        // When & Then
        assertThat(inventory.canReserve(50)).isTrue();
        assertThat(inventory.canReserve(100)).isTrue();
        assertThat(inventory.canReserve(1)).isTrue();
    }

    @Test
    @DisplayName("canReserve() should return false when insufficient quantity available")
    void businessLogic_CanReserve_ShouldReturnFalseWhenInsufficientQuantity() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 50, 10, 20, 5);

        // When & Then
        assertThat(inventory.canReserve(51)).isFalse();
        assertThat(inventory.canReserve(100)).isFalse();
    }

    @Test
    @DisplayName("canReserve() should return false for null or zero/negative quantities")
    void businessLogic_CanReserve_ShouldReturnFalseForInvalidQuantities() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 10, 20, 5);

        // When & Then
        assertThat(inventory.canReserve(null)).isFalse();
        assertThat(inventory.canReserve(0)).isFalse();
        assertThat(inventory.canReserve(-5)).isFalse();
    }

    @Test
    @DisplayName("makeReservation() should successfully reserve stock when sufficient quantity available")
    void businessLogic_MakeReservation_ShouldSucceedWhenSufficientQuantity() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 10, 20, 5);
        
        // When
        inventory.makeReservation(30);

        // Then
        assertThat(inventory.getQuantityAvailable()).isEqualTo(70); // 100 - 30
        assertThat(inventory.getQuantityReserved()).isEqualTo(40); // 10 + 30
        assertThat(inventory.getTotalQuantity()).isEqualTo(110); // Total unchanged
    }

    @Test
    @DisplayName("makeReservation() should throw exception when insufficient quantity available")
    void businessLogic_MakeReservation_ShouldThrowExceptionWhenInsufficientQuantity() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 50, 10, 20, 5);

        // When & Then
        assertThatThrownBy(() -> inventory.makeReservation(51))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot reserve 51 items. Available: 50");
    }

    @Test
    @DisplayName("releaseReservation() should successfully release reserved stock")
    void businessLogic_ReleaseReservation_ShouldSucceedWhenSufficientReserved() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 50, 30, 20, 5);
        
        // When
        inventory.releaseReservation(15);

        // Then
        assertThat(inventory.getQuantityAvailable()).isEqualTo(65); // 50 + 15
        assertThat(inventory.getQuantityReserved()).isEqualTo(15); // 30 - 15
        assertThat(inventory.getTotalQuantity()).isEqualTo(80); // Total unchanged
    }

    @Test
    @DisplayName("releaseReservation() should throw exception when insufficient reserved quantity")
    void businessLogic_ReleaseReservation_ShouldThrowExceptionWhenInsufficientReserved() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 50, 20, 30, 5);

        // When & Then
        assertThatThrownBy(() -> inventory.releaseReservation(25))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot release 25 items. Reserved: 20");
    }

    // =================== Business Logic Tests - Domain Service Methods ===================

    @Test
    @DisplayName("reserveQuantity() should successfully reserve stock and update timestamp")
    void businessLogic_ReserveQuantity_ShouldSucceedAndUpdateTimestamp() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 10, 20, 5);
        LocalDateTime beforeReservation = LocalDateTime.now();
        
        // When
        inventory.reserveQuantity(25);

        // Then
        assertThat(inventory.getQuantityAvailable()).isEqualTo(75); // 100 - 25
        assertThat(inventory.getQuantityReserved()).isEqualTo(35); // 10 + 25
        assertThat(inventory.getLastUpdated()).isAfterOrEqualTo(beforeReservation);
    }

    @Test
    @DisplayName("reserveQuantity() should throw exception when cannot reserve")
    void businessLogic_ReserveQuantity_ShouldThrowExceptionWhenCannotReserve() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 40, 10, 20, 5);

        // When & Then
        assertThatThrownBy(() -> inventory.reserveQuantity(50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot reserve 50 items. Available: 40");
    }

    @Test
    @DisplayName("releaseQuantity() should successfully release stock and update timestamp")
    void businessLogic_ReleaseQuantity_ShouldSucceedAndUpdateTimestamp() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 60, 40, 20, 5);
        LocalDateTime beforeRelease = LocalDateTime.now();
        
        // When
        inventory.releaseQuantity(20);

        // Then
        assertThat(inventory.getQuantityAvailable()).isEqualTo(80); // 60 + 20
        assertThat(inventory.getQuantityReserved()).isEqualTo(20); // 40 - 20
        assertThat(inventory.getLastUpdated()).isAfterOrEqualTo(beforeRelease);
    }

    @Test
    @DisplayName("releaseQuantity() should throw exception for invalid quantities")
    void businessLogic_ReleaseQuantity_ShouldThrowExceptionForInvalidQuantities() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 60, 30, 20, 5);

        // When & Then
        assertThatThrownBy(() -> inventory.releaseQuantity(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot release null items");

        assertThatThrownBy(() -> inventory.releaseQuantity(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot release 0 items");

        assertThatThrownBy(() -> inventory.releaseQuantity(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot release -5 items");

        assertThatThrownBy(() -> inventory.releaseQuantity(35))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot release 35 items. Reserved: 30");
    }

    @Test
    @DisplayName("adjustQuantity() should successfully adjust stock and update timestamp")
    void businessLogic_AdjustQuantity_ShouldSucceedAndUpdateTimestamp() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 80, 20, 30, 5);
        LocalDateTime beforeAdjustment = LocalDateTime.now();
        
        // When
        inventory.adjustQuantity(150); // New total quantity

        // Then
        assertThat(inventory.getQuantityAvailable()).isEqualTo(130); // 150 - 20 (reserved)
        assertThat(inventory.getQuantityReserved()).isEqualTo(20); // Unchanged
        assertThat(inventory.getTotalQuantity()).isEqualTo(150);
        assertThat(inventory.getLastUpdated()).isAfterOrEqualTo(beforeAdjustment);
    }

    @Test
    @DisplayName("adjustQuantity() should throw exception for negative quantities")
    void businessLogic_AdjustQuantity_ShouldThrowExceptionForNegativeQuantities() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 80, 20, 30, 5);

        // When & Then
        assertThatThrownBy(() -> inventory.adjustQuantity(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity cannot be negative: -10");

        assertThatThrownBy(() -> inventory.adjustQuantity(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity cannot be negative: null");
    }

    @Test
    @DisplayName("adjustQuantity() should throw exception when new quantity conflicts with reservations")
    void businessLogic_AdjustQuantity_ShouldThrowExceptionWhenConflictsWithReservations() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 80, 30, 40, 5);

        // When & Then - Cannot set total quantity below current reservations
        assertThatThrownBy(() -> inventory.adjustQuantity(25))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot set quantity to 25 - would conflict with 30 reserved items");
    }

    @Test
    @DisplayName("adjustQuantity() should allow setting quantity equal to reserved amount")
    void businessLogic_AdjustQuantity_ShouldAllowQuantityEqualToReserved() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 80, 30, 40, 5);

        // When
        inventory.adjustQuantity(30); // Exactly equal to reserved amount

        // Then
        assertThat(inventory.getQuantityAvailable()).isEqualTo(0); // 30 - 30
        assertThat(inventory.getQuantityReserved()).isEqualTo(30); // Unchanged
        assertThat(inventory.getTotalQuantity()).isEqualTo(30);
    }

    // =================== Business Logic Tests - Display Methods ===================

    @Test
    @DisplayName("getDisplayName() should return product name for base product inventory")
    void businessLogic_GetDisplayName_ShouldReturnProductNameForBaseProduct() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 10, 20, 5);

        // When
        String displayName = inventory.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo(testProduct.getName());
    }

    @Test
    @DisplayName("getDisplayName() should return product name and variant name for variant inventory")
    void businessLogic_GetDisplayName_ShouldReturnProductAndVariantNameForVariant() {
        // Given
        Inventory inventory = createTestInventory(testProduct, testVariant, 50, 5, 15, 3);

        // When
        String displayName = inventory.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo(testProduct.getName() + " - " + testVariant.getName());
    }

    // =================== Persistence Tests ===================

    @Test
    @DisplayName("Should persist Inventory with all required fields")
    void persistence_WithValidData_ShouldSaveSuccessfully() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 15, 25, 10);

        // When
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Then
        assertThat(savedInventory.getId()).isNotNull();
        assertThat(savedInventory.getProduct()).isEqualTo(testProduct);
        assertThat(savedInventory.getVariant()).isNull();
        assertThat(savedInventory.getQuantityAvailable()).isEqualTo(100);
        assertThat(savedInventory.getQuantityReserved()).isEqualTo(15);
        assertThat(savedInventory.getMinimumStockLevel()).isEqualTo(10);
        assertThat(savedInventory.getReorderPoint()).isEqualTo(25);
        assertThat(savedInventory.getLastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("Should persist Inventory with variant correctly")
    void persistence_WithVariant_ShouldSaveSuccessfully() {
        // Given
        Inventory inventory = createTestInventory(testProduct, testVariant, 75, 10, 20, 8);

        // When
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Then
        assertThat(savedInventory.getVariant()).isEqualTo(testVariant);
        assertThat(savedInventory.getVariant().getSkuSuffix()).isEqualTo("XL");
        assertThat(savedInventory.getVariant().getName()).isEqualTo("Extra Large");
    }

    @Test
    @DisplayName("Should automatically set lastUpdated timestamp on save")
    void persistence_OnSave_ShouldSetLastUpdatedTimestamp() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 15, 25, 10);
        LocalDateTime beforeSave = LocalDateTime.now();

        // When
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Then
        assertThat(savedInventory.getLastUpdated()).isNotNull();
        assertThat(savedInventory.getLastUpdated()).isAfterOrEqualTo(beforeSave.minusSeconds(1));
    }

    @Test
    @DisplayName("Should use default values correctly")
    void persistence_WithDefaults_ShouldUseCorrectDefaultValues() {
        // Given
        Inventory inventory = Inventory.builder()
                .product(testProduct)
                // Using defaults for quantities and thresholds
                .build();

        // When
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Then
        assertThat(savedInventory.getQuantityAvailable()).isEqualTo(0);
        assertThat(savedInventory.getQuantityReserved()).isEqualTo(0);
        assertThat(savedInventory.getMinimumStockLevel()).isEqualTo(5);
        assertThat(savedInventory.getReorderPoint()).isEqualTo(10);
    }

    // =================== Constraint Tests ===================

    @Test
    @DisplayName("Should enforce not null constraint on product")
    void constraints_NullProduct_ShouldThrowException() {
        // Given
        Inventory inventory = Inventory.builder()
                .product(null) // Null product
                .quantityAvailable(100)
                .quantityReserved(10)
                .minimumStockLevel(5)
                .reorderPoint(10)
                .build();

        // When & Then
        assertThatThrownBy(() -> inventoryRepository.saveAndFlush(inventory))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce not null constraint on quantityAvailable")
    void constraints_NullQuantityAvailable_ShouldThrowException() {
        // Given
        Inventory inventory = Inventory.builder()
                .product(testProduct)
                .quantityAvailable(null) // Null quantity
                .quantityReserved(10)
                .minimumStockLevel(5)
                .reorderPoint(10)
                .build();

        // When & Then
        assertThatThrownBy(() -> inventoryRepository.saveAndFlush(inventory))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // =================== Relationship Tests ===================

    @Test
    @DisplayName("Should maintain correct relationship with Product")
    void relationships_WithProduct_ShouldMaintainCorrectly() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 15, 25, 10);

        // When
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Then
        assertThat(savedInventory.getProduct()).isNotNull();
        assertThat(savedInventory.getProduct().getId()).isEqualTo(testProduct.getId());
        assertThat(savedInventory.getProduct().getName()).isEqualTo(testProduct.getName());
    }

    @Test
    @DisplayName("Should maintain correct relationship with ProductVariant")
    void relationships_WithProductVariant_ShouldMaintainCorrectly() {
        // Given
        Inventory inventory = createTestInventory(testProduct, testVariant, 75, 10, 20, 8);

        // When
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Then
        assertThat(savedInventory.getVariant()).isNotNull();
        assertThat(savedInventory.getVariant().getId()).isEqualTo(testVariant.getId());
        assertThat(savedInventory.getVariant().getSkuSuffix()).isEqualTo("XL");
        assertThat(savedInventory.getVariant().getName()).isEqualTo("Extra Large");
    }

    // =================== Edge Cases and Complex Scenarios ===================

    @Test
    @DisplayName("Should handle complete stock depletion and replenishment cycle")
    void edgeCases_StockDepletionAndReplenishment_ShouldHandleCorrectly() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 100, 0, 20, 5);

        // When - Reserve all available stock
        inventory.reserveQuantity(100);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(0);
        assertThat(inventory.getQuantityReserved()).isEqualTo(100);
        assertThat(inventory.isOutOfStock()).isTrue();

        // When - Release some reservations
        inventory.releaseQuantity(30);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(30);
        assertThat(inventory.getQuantityReserved()).isEqualTo(70);
        assertThat(inventory.isOutOfStock()).isFalse();

        // When - Replenish stock
        inventory.adjustQuantity(200); // Total quantity
        assertThat(inventory.getQuantityAvailable()).isEqualTo(130); // 200 - 70
        assertThat(inventory.getQuantityReserved()).isEqualTo(70);
        assertThat(inventory.getTotalQuantity()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should handle multiple reservation and release operations")
    void edgeCases_MultipleReservationOperations_ShouldHandleCorrectly() {
        // Given
        Inventory inventory = createTestInventory(testProduct, null, 200, 0, 30, 10);

        // When - Multiple reservations
        inventory.reserveQuantity(50);
        inventory.reserveQuantity(30);
        inventory.reserveQuantity(20);

        // Then
        assertThat(inventory.getQuantityAvailable()).isEqualTo(100); // 200 - 100
        assertThat(inventory.getQuantityReserved()).isEqualTo(100);

        // When - Partial releases
        inventory.releaseQuantity(25);
        inventory.releaseQuantity(15);

        // Then
        assertThat(inventory.getQuantityAvailable()).isEqualTo(140); // 100 + 40
        assertThat(inventory.getQuantityReserved()).isEqualTo(60); // 100 - 40
    }

    @Test
    @DisplayName("Should handle boundary conditions for stock levels")
    void edgeCases_BoundaryConditions_ShouldHandleCorrectly() {
        // Given - Inventory exactly at thresholds
        Inventory inventory = createTestInventory(testProduct, null, 10, 5, 10, 10);

        // Then - At reorder point
        assertThat(inventory.isLowStock()).isTrue();
        assertThat(inventory.isBelowMinimum()).isFalse();

        // When - Reduce by 1
        inventory.adjustQuantity(14); // Total = 14, Available = 9 (14-5)
        
        // Then - Below reorder point but at minimum
        assertThat(inventory.isLowStock()).isTrue();
        assertThat(inventory.isBelowMinimum()).isTrue();
    }

    // =================== Helper Methods ===================

    private Inventory createTestInventory(Product product, ProductVariant variant, 
                                        Integer available, Integer reserved, 
                                        Integer reorderPoint, Integer minimumLevel) {
        return Inventory.builder()
                .product(product)
                .variant(variant)
                .quantityAvailable(available)
                .quantityReserved(reserved)
                .reorderPoint(reorderPoint)
                .minimumStockLevel(minimumLevel)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    private Product createAndSaveProduct(String name, String slug) {
        Product product = Product.builder()
                .name(name)
                .slug(slug)
                .sku("SKU-" + testIdCounter++)
                .description("Test product description")
                .shortDescription("Short description")
                .basePrice(new BigDecimal("99.99"))
                .category(testCategory)
                .brand("Test Brand")
                .weightGrams(500)
                .status(ProductStatus.ACTIVE)
                .build();
        return productRepository.save(product);
    }

    private ProductVariant createAndSaveVariant(Product product, String skuSuffix, String name) {
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .skuSuffix(skuSuffix)
                .name(name)
                .priceModifier(new BigDecimal("10.00"))
                .isActive(true)
                .build();
        return productVariantRepository.save(variant);
    }

    private Category createAndSaveCategory(String name, String slug) {
        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description("Test category description")
                .displayOrder(1)
                .isActive(true)
                .build();
        return categoryRepository.save(category);
    }
}
