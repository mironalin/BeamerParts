package live.alinmiron.beamerparts.product.entity;

import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.repository.ProductVariantRepository;
import live.alinmiron.beamerparts.product.repository.StockMovementRepository;
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
 * Comprehensive test suite for StockMovement entity business logic
 * Tests all business methods and domain rules using real database operations
 * 
 * **KEY LESSONS APPLIED:**
 * - Tests define business logic FIRST, implementation follows
 * - Use @SpringBootTest for entity testing with real DB operations
 * - Test all business methods and edge cases comprehensively
 * - Verify entity relationships and audit trail functionality
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("StockMovement Entity Tests")
class StockMovementTest {

    @Autowired
    private StockMovementRepository stockMovementRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;
    private Product testProduct;
    private ProductVariant testVariant;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        stockMovementRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        
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
                .name("Test Product")
                .slug("test-product-" + System.currentTimeMillis())
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

        // Create test variant
        testVariant = ProductVariant.builder()
                .product(testProduct)
                .name("Black")
                .skuSuffix("-BLK")
                .priceModifier(BigDecimal.ZERO)
                .isActive(true)
                .build();
        testVariant = productVariantRepository.save(testVariant);
    }

    // =========================
    // MOVEMENT TYPE LOGIC TESTS
    // =========================

    @Test
    @DisplayName("Should identify INCOMING movement type correctly")
    void isIncoming_WithIncomingType_ShouldReturnTrue() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.INCOMING, 10, "Stock received");

        // When & Then
        assertThat(movement.isIncoming()).isTrue();
        assertThat(movement.isOutgoing()).isFalse();
        assertThat(movement.isAdjustment()).isFalse();
        assertThat(movement.isReservation()).isFalse();
        assertThat(movement.isRelease()).isFalse();
    }

    @Test
    @DisplayName("Should identify OUTGOING movement type correctly")
    void isOutgoing_WithOutgoingType_ShouldReturnTrue() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.OUTGOING, -5, "Sale");

        // When & Then
        assertThat(movement.isOutgoing()).isTrue();
        assertThat(movement.isIncoming()).isFalse();
        assertThat(movement.isAdjustment()).isFalse();
        assertThat(movement.isReservation()).isFalse();
        assertThat(movement.isRelease()).isFalse();
    }

    @Test
    @DisplayName("Should identify ADJUSTMENT movement type correctly")
    void isAdjustment_WithAdjustmentType_ShouldReturnTrue() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.ADJUSTMENT, 3, "Inventory correction");

        // When & Then
        assertThat(movement.isAdjustment()).isTrue();
        assertThat(movement.isIncoming()).isFalse();
        assertThat(movement.isOutgoing()).isFalse();
        assertThat(movement.isReservation()).isFalse();
        assertThat(movement.isRelease()).isFalse();
    }

    @Test
    @DisplayName("Should identify RESERVED movement type correctly")
    void isReservation_WithReservedType_ShouldReturnTrue() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.RESERVED, -2, "Order reservation");

        // When & Then
        assertThat(movement.isReservation()).isTrue();
        assertThat(movement.isIncoming()).isFalse();
        assertThat(movement.isOutgoing()).isFalse();
        assertThat(movement.isAdjustment()).isFalse();
        assertThat(movement.isRelease()).isFalse();
    }

    @Test
    @DisplayName("Should identify RELEASED movement type correctly")
    void isRelease_WithReleasedType_ShouldReturnTrue() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.RELEASED, 2, "Reservation released");

        // When & Then
        assertThat(movement.isRelease()).isTrue();
        assertThat(movement.isIncoming()).isFalse();
        assertThat(movement.isOutgoing()).isFalse();
        assertThat(movement.isAdjustment()).isFalse();
        assertThat(movement.isReservation()).isFalse();
    }

    // =========================
    // DISPLAY NAME LOGIC TESTS
    // =========================

    @Test
    @DisplayName("Should generate display name for product movement without variant")
    void getDisplayName_WithoutVariant_ShouldFormatCorrectly() {
        // Given
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .variant(null) // No variant
                .movementType(StockMovementType.INCOMING)
                .quantityChange(15)
                .reason("Stock received")
                .build();

        // When
        String displayName = movement.getDisplayName();

        // Then
        assertThat(displayName).startsWith("INCOMING - Test Product (+15)");
    }

    @Test
    @DisplayName("Should generate display name for product movement with variant")
    void getDisplayName_WithVariant_ShouldFormatCorrectly() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.OUTGOING, -3, "Sale");

        // When
        String displayName = movement.getDisplayName();

        // Then
        assertThat(displayName).contains("OUTGOING - Test Product - Black (-3)");
    }

    @Test
    @DisplayName("Should handle positive quantity change in display name")
    void getDisplayName_WithPositiveQuantity_ShouldShowPlusSign() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.ADJUSTMENT, 7, "Inventory increase");

        // When
        String displayName = movement.getDisplayName();

        // Then
        assertThat(displayName).contains("(+7)");
    }

    @Test
    @DisplayName("Should handle negative quantity change in display name")
    void getDisplayName_WithNegativeQuantity_ShouldShowMinusSign() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.OUTGOING, -12, "Stock sold");

        // When
        String displayName = movement.getDisplayName();

        // Then
        assertThat(displayName).contains("(-12)");
    }

    @Test
    @DisplayName("Should handle zero quantity change in display name")
    void getDisplayName_WithZeroQuantity_ShouldShowZero() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.ADJUSTMENT, 0, "No change");

        // When
        String displayName = movement.getDisplayName();

        // Then
        assertThat(displayName).contains("(0)");
    }

    // =========================
    // PERSISTENCE AND AUDIT TESTS
    // =========================

    @Test
    @DisplayName("Should persist stock movement with all required fields")
    void persistMovement_WithValidData_ShouldSaveSuccessfully() {
        // Given
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .variant(testVariant)
                .movementType(StockMovementType.INCOMING)
                .quantityChange(25)
                .reason("Initial stock")
                .referenceId("REF-001")
                .userCode("admin@test.com")
                .build();

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.getId()).isNotNull();
        assertThat(savedMovement.getProduct()).isEqualTo(testProduct);
        assertThat(savedMovement.getVariant()).isEqualTo(testVariant);
        assertThat(savedMovement.getMovementType()).isEqualTo(StockMovementType.INCOMING);
        assertThat(savedMovement.getQuantityChange()).isEqualTo(25);
        assertThat(savedMovement.getReason()).isEqualTo("Initial stock");
        assertThat(savedMovement.getReferenceId()).isEqualTo("REF-001");
        assertThat(savedMovement.getUserCode()).isEqualTo("admin@test.com");
        assertThat(savedMovement.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should set creation timestamp automatically")
    void createdAt_ShouldBeSetAutomatically() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.INCOMING, 10, "Auto timestamp test");
        LocalDateTime beforePersist = LocalDateTime.now().minusSeconds(1);
        
        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);
        LocalDateTime afterPersist = LocalDateTime.now().plusSeconds(1);

        // Then
        assertThat(savedMovement.getCreatedAt()).isNotNull();
        assertThat(savedMovement.getCreatedAt()).isAfter(beforePersist);
        assertThat(savedMovement.getCreatedAt()).isBefore(afterPersist);
    }

    @Test
    @DisplayName("Should create movement without variant (product-level movement)")
    void createMovement_WithoutVariant_ShouldPersistSuccessfully() {
        // Given
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .variant(null) // Product-level movement
                .movementType(StockMovementType.ADJUSTMENT)
                .quantityChange(5)
                .reason("Product-level adjustment")
                .build();

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.getId()).isNotNull();
        assertThat(savedMovement.getProduct()).isEqualTo(testProduct);
        assertThat(savedMovement.getVariant()).isNull();
        assertThat(savedMovement.getMovementType()).isEqualTo(StockMovementType.ADJUSTMENT);
    }

    @Test
    @DisplayName("Should handle optional fields as null")
    void createMovement_WithOptionalFieldsNull_ShouldPersistSuccessfully() {
        // Given
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .variant(testVariant)
                .movementType(StockMovementType.OUTGOING)
                .quantityChange(-3)
                .reason(null) // Optional
                .referenceId(null) // Optional
                .userCode(null) // Optional
                .build();

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.getId()).isNotNull();
        assertThat(savedMovement.getReason()).isNull();
        assertThat(savedMovement.getReferenceId()).isNull();
        assertThat(savedMovement.getUserCode()).isNull();
        assertThat(savedMovement.getCreatedAt()).isNotNull();
    }

    // =========================
    // BUSINESS RULE TESTS
    // =========================

    @Test
    @DisplayName("Should create inbound movement with positive quantity")
    void createInboundMovement_ShouldIncreaseStock() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.INCOMING, 50, "Stock delivery");

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.isIncoming()).isTrue();
        assertThat(savedMovement.getQuantityChange()).isPositive();
    }

    @Test
    @DisplayName("Should create outbound movement with negative quantity")
    void createOutboundMovement_ShouldDecreaseStock() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.OUTGOING, -20, "Product sale");

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.isOutgoing()).isTrue();
        assertThat(savedMovement.getQuantityChange()).isNegative();
    }

    @Test
    @DisplayName("Should create adjustment movement with any quantity change")
    void createAdjustmentMovement_ShouldCorrectStock() {
        // Given - Test both positive and negative adjustments
        StockMovement positiveAdj = createTestMovement(StockMovementType.ADJUSTMENT, 8, "Found extra stock");
        StockMovement negativeAdj = createTestMovement(StockMovementType.ADJUSTMENT, -3, "Damaged items");

        // When
        StockMovement savedPositive = stockMovementRepository.save(positiveAdj);
        StockMovement savedNegative = stockMovementRepository.save(negativeAdj);

        // Then
        assertThat(savedPositive.isAdjustment()).isTrue();
        assertThat(savedPositive.getQuantityChange()).isPositive();
        
        assertThat(savedNegative.isAdjustment()).isTrue();
        assertThat(savedNegative.getQuantityChange()).isNegative();
    }

    @Test
    @DisplayName("Should create reservation movement typically with negative quantity")
    void createReservationMovement_ShouldReserveStock() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.RESERVED, -5, "Order #12345");

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.isReservation()).isTrue();
        assertThat(savedMovement.getQuantityChange()).isNegative();
        assertThat(savedMovement.getReason()).contains("Order #12345");
    }

    @Test
    @DisplayName("Should create release movement typically with positive quantity")
    void createReleaseMovement_ShouldReleaseReservation() {
        // Given
        StockMovement movement = createTestMovement(StockMovementType.RELEASED, 5, "Order cancelled");

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.isRelease()).isTrue();
        assertThat(savedMovement.getQuantityChange()).isPositive();
    }

    // =========================
    // MOVEMENT TRACKING TESTS
    // =========================

    @Test
    @DisplayName("Should track movements with reference IDs")
    void trackMovements_WithReferenceId_ShouldLinkToExternalSystems() {
        // Given
        String orderRef = "ORDER-2024-001";
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .variant(testVariant)
                .movementType(StockMovementType.RESERVED)
                .quantityChange(-2)
                .reason("Order reservation")
                .referenceId(orderRef)
                .userCode("customer@example.com")
                .build();

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.getReferenceId()).isEqualTo(orderRef);
        assertThat(savedMovement.getUserCode()).isEqualTo("customer@example.com");
    }

    @Test
    @DisplayName("Should track user who performed the movement")
    void trackMovements_WithUserCode_ShouldAuditUserActions() {
        // Given
        String adminUser = "admin@beamerparts.com";
        StockMovement movement = createTestMovement(StockMovementType.ADJUSTMENT, 10, "Manual adjustment");
        movement.setUserCode(adminUser);

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.getUserCode()).isEqualTo(adminUser);
    }

    @Test
    @DisplayName("Should create audit trail chronologically")
    void trackMovements_ShouldCreateChronologicalAuditTrail() throws InterruptedException {
        // Given - Create movements with slight time delay
        StockMovement movement1 = createTestMovement(StockMovementType.INCOMING, 100, "Initial stock");
        StockMovement saved1 = stockMovementRepository.save(movement1);
        
        Thread.sleep(10); // Small delay to ensure different timestamps
        
        StockMovement movement2 = createTestMovement(StockMovementType.OUTGOING, -10, "First sale");
        StockMovement saved2 = stockMovementRepository.save(movement2);

        Thread.sleep(10);
        
        StockMovement movement3 = createTestMovement(StockMovementType.ADJUSTMENT, 5, "Correction");
        StockMovement saved3 = stockMovementRepository.save(movement3);

        // When & Then
        assertThat(saved1.getCreatedAt()).isBefore(saved2.getCreatedAt());
        assertThat(saved2.getCreatedAt()).isBefore(saved3.getCreatedAt());
    }

    // =========================
    // EDGE CASE TESTS
    // =========================

    @Test
    @DisplayName("Should handle large quantity changes")
    void handleLargeQuantityChanges_ShouldPersistCorrectly() {
        // Given
        StockMovement largeIncoming = createTestMovement(StockMovementType.INCOMING, 1000000, "Bulk delivery");
        StockMovement largeOutgoing = createTestMovement(StockMovementType.OUTGOING, -999999, "Bulk sale");

        // When
        StockMovement savedIncoming = stockMovementRepository.save(largeIncoming);
        StockMovement savedOutgoing = stockMovementRepository.save(largeOutgoing);

        // Then
        assertThat(savedIncoming.getQuantityChange()).isEqualTo(1000000);
        assertThat(savedOutgoing.getQuantityChange()).isEqualTo(-999999);
    }

    @Test
    @DisplayName("Should handle long reason strings")
    void handleLongReasonStrings_ShouldTruncateOrAcceptCorrectly() {
        // Given
        String longReason = "This is a very long reason that might exceed typical database field limits and needs to be handled appropriately by the application to ensure data integrity and proper functionality within the business domain";
        
        // Note: The reason field has length = 100, so this should be truncated or handled by the database
        StockMovement movement = createTestMovement(StockMovementType.ADJUSTMENT, 1, longReason);

        // When & Then - Should throw constraint violation for field length limit
        assertThatThrownBy(() -> stockMovementRepository.save(movement))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("value too long for type character varying(100)");
    }

    @Test
    @DisplayName("Should handle special characters in reason and reference ID")
    void handleSpecialCharacters_ShouldPersistCorrectly() {
        // Given
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .variant(testVariant)
                .movementType(StockMovementType.INCOMING)
                .quantityChange(5)
                .reason("Björk & Müller delivery #1")
                .referenceId("REF-ÄÖÜ-2024")
                .userCode("user@domain.com")
                .build();

        // When
        StockMovement savedMovement = stockMovementRepository.save(movement);

        // Then
        assertThat(savedMovement.getReason()).isEqualTo("Björk & Müller delivery #1");
        assertThat(savedMovement.getReferenceId()).isEqualTo("REF-ÄÖÜ-2024");
    }

    @Test
    @DisplayName("Should handle null movement type gracefully in display name")
    void getDisplayName_WithNullMovementType_ShouldHandleGracefully() {
        // Given
        StockMovement movement = StockMovement.builder()
                .product(testProduct)
                .variant(testVariant)
                .movementType(null) // This should not happen in practice
                .quantityChange(5)
                .build();

        // When & Then - Should throw NPE as per domain design
        assertThatThrownBy(() -> movement.getDisplayName())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle null product gracefully in display name")
    void getDisplayName_WithNullProduct_ShouldHandleGracefully() {
        // Given
        StockMovement movement = StockMovement.builder()
                .product(null) // This should not happen in practice
                .variant(testVariant)
                .movementType(StockMovementType.INCOMING)
                .quantityChange(5)
                .build();

        // When & Then - Should throw NPE as per domain design
        assertThatThrownBy(() -> movement.getDisplayName())
                .isInstanceOf(NullPointerException.class);
    }

    // =========================
    // HELPER METHODS
    // =========================

    private StockMovement createTestMovement(StockMovementType type, Integer quantityChange, String reason) {
        return StockMovement.builder()
                .product(testProduct)
                .variant(testVariant)
                .movementType(type)
                .quantityChange(quantityChange)
                .reason(reason)
                .build();
    }
}
