package live.alinmiron.beamerparts.product.entity;

import live.alinmiron.beamerparts.product.repository.CategoryRepository;
import live.alinmiron.beamerparts.product.repository.ProductRepository;
import live.alinmiron.beamerparts.product.repository.ProductVariantRepository;
import live.alinmiron.beamerparts.product.repository.StockReservationRepository;
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
 * Comprehensive tests for StockReservation entity business logic, persistence, and constraints.
 * Tests reservation lifecycle, audit fields, validation, and relationship integrity.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("StockReservation Entity Tests")
class StockReservationTest {

    @Autowired
    private StockReservationRepository stockReservationRepository;
    
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
        testVariant = createAndSaveVariant(testProduct, "L", "Large");
    }

    // =================== Persistence Tests ===================

    @Test
    @DisplayName("Should persist StockReservation with all required fields")
    void persistence_WithValidData_ShouldSaveSuccessfully() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, null, 5, "user123");

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getId()).isNotNull();
        assertThat(savedReservation.getReservationId()).startsWith("res-");
        assertThat(savedReservation.getProduct()).isEqualTo(testProduct);
        assertThat(savedReservation.getVariant()).isNull();
        assertThat(savedReservation.getQuantityReserved()).isEqualTo(5);
        assertThat(savedReservation.getUserId()).isEqualTo("user123");
        assertThat(savedReservation.getIsActive()).isTrue();
        assertThat(savedReservation.getCreatedAt()).isNotNull();
        assertThat(savedReservation.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Should persist StockReservation with variant correctly")
    void persistence_WithVariant_ShouldSaveSuccessfully() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, testVariant, 3, "user456");

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getVariant()).isEqualTo(testVariant);
        assertThat(savedReservation.getVariant().getSkuSuffix()).isEqualTo("L");
        assertThat(savedReservation.getQuantityReserved()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should automatically set createdAt timestamp on save")
    void persistence_OnSave_ShouldSetCreatedAtTimestamp() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, null, 5, "user123");
        LocalDateTime beforeSave = LocalDateTime.now();

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getCreatedAt()).isNotNull();
        assertThat(savedReservation.getCreatedAt()).isAfterOrEqualTo(beforeSave.minusSeconds(1));
        assertThat(savedReservation.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Should automatically set isActive to true if null")
    void persistence_WithNullIsActive_ShouldDefaultToTrue() {
        // Given
        StockReservation reservation = StockReservation.builder()
                .reservationId("res-test-" + testIdCounter++)
                .product(testProduct)
                .quantityReserved(5)
                .userId("user123")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .isActive(null) // Explicitly set to null
                .build();

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should preserve explicitly set isActive value")
    void persistence_WithExplicitIsActive_ShouldPreserveValue() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, null, 5, "user123");
        reservation.setIsActive(false);

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getIsActive()).isFalse();
    }

    // =================== Constraint Validation Tests ===================

    @Test
    @DisplayName("Should enforce unique constraint on reservationId")
    void constraints_DuplicateReservationId_ShouldThrowException() {
        // Given
        String duplicateId = "res-duplicate-" + testIdCounter;
        StockReservation reservation1 = createTestReservation(testProduct, null, 5, "user123");
        reservation1.setReservationId(duplicateId);
        stockReservationRepository.save(reservation1);

        StockReservation reservation2 = createTestReservation(testProduct, null, 3, "user456");
        reservation2.setReservationId(duplicateId);

        // When & Then
        assertThatThrownBy(() -> stockReservationRepository.saveAndFlush(reservation2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce not null constraint on product")
    void constraints_NullProduct_ShouldThrowException() {
        // Given
        StockReservation reservation = StockReservation.builder()
                .reservationId("res-test-" + testIdCounter++)
                .product(null) // Null product
                .quantityReserved(5)
                .userId("user123")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .isActive(true)
                .build();

        // When & Then
        assertThatThrownBy(() -> stockReservationRepository.saveAndFlush(reservation))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce not null constraint on quantityReserved")
    void constraints_NullQuantityReserved_ShouldThrowException() {
        // Given
        StockReservation reservation = StockReservation.builder()
                .reservationId("res-test-" + testIdCounter++)
                .product(testProduct)
                .quantityReserved(null) // Null quantity
                .userId("user123")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .isActive(true)
                .build();

        // When & Then
        assertThatThrownBy(() -> stockReservationRepository.saveAndFlush(reservation))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce not null constraint on userId")
    void constraints_NullUserId_ShouldThrowException() {
        // Given
        StockReservation reservation = StockReservation.builder()
                .reservationId("res-test-" + testIdCounter++)
                .product(testProduct)
                .quantityReserved(5)
                .userId(null) // Null userId
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .isActive(true)
                .build();

        // When & Then
        assertThatThrownBy(() -> stockReservationRepository.saveAndFlush(reservation))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce not null constraint on expiresAt")
    void constraints_NullExpiresAt_ShouldThrowException() {
        // Given
        StockReservation reservation = StockReservation.builder()
                .reservationId("res-test-" + testIdCounter++)
                .product(testProduct)
                .quantityReserved(5)
                .userId("user123")
                .expiresAt(null) // Null expiresAt
                .isActive(true)
                .build();

        // When & Then
        assertThatThrownBy(() -> stockReservationRepository.saveAndFlush(reservation))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should handle maximum length userId (255 characters)")
    void constraints_MaxLengthUserId_ShouldSaveSuccessfully() {
        // Given
        String longUserId = "A".repeat(255); // Max length according to schema
        StockReservation reservation = createTestReservation(testProduct, null, 5, longUserId);

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getUserId()).hasSize(255);
        assertThat(savedReservation.getUserId()).isEqualTo(longUserId);
    }

    @Test
    @DisplayName("Should handle maximum length reservationId (50 characters)")
    void constraints_MaxLengthReservationId_ShouldSaveSuccessfully() {
        // Given
        String longReservationId = "A".repeat(50); // Max length according to schema
        StockReservation reservation = createTestReservation(testProduct, null, 5, "user123");
        reservation.setReservationId(longReservationId);

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getReservationId()).hasSize(50);
        assertThat(savedReservation.getReservationId()).isEqualTo(longReservationId);
    }

    // =================== Relationship Tests ===================

    @Test
    @DisplayName("Should maintain correct relationship with Product")
    void relationships_WithProduct_ShouldMaintainCorrectly() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, null, 5, "user123");

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getProduct()).isNotNull();
        assertThat(savedReservation.getProduct().getId()).isEqualTo(testProduct.getId());
        assertThat(savedReservation.getProduct().getName()).isEqualTo(testProduct.getName());
    }

    @Test
    @DisplayName("Should maintain correct relationship with ProductVariant")
    void relationships_WithProductVariant_ShouldMaintainCorrectly() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, testVariant, 3, "user123");

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getVariant()).isNotNull();
        assertThat(savedReservation.getVariant().getId()).isEqualTo(testVariant.getId());
        assertThat(savedReservation.getVariant().getSkuSuffix()).isEqualTo("L");
        assertThat(savedReservation.getVariant().getName()).isEqualTo("Large");
    }

    @Test
    @DisplayName("Should allow null variant for base product reservations")
    void relationships_WithNullVariant_ShouldAllowNull() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, null, 5, "user123");

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getVariant()).isNull();
        assertThat(savedReservation.getProduct()).isNotNull();
    }

    // =================== Business Logic Tests ===================

    @Test
    @DisplayName("Should handle different reservation sources correctly")
    void businessLogic_WithDifferentSources_ShouldHandleCorrectly() {
        // Given
        StockReservation cartReservation = createTestReservation(testProduct, null, 5, "user123");
        cartReservation.setSource("cart");
        
        StockReservation orderReservation = createTestReservation(testProduct, testVariant, 3, "user456");
        orderReservation.setSource("order");
        orderReservation.setOrderId("ORD-123");

        // When
        StockReservation savedCartReservation = stockReservationRepository.save(cartReservation);
        StockReservation savedOrderReservation = stockReservationRepository.save(orderReservation);

        // Then
        assertThat(savedCartReservation.getSource()).isEqualTo("cart");
        assertThat(savedCartReservation.getOrderId()).isNull();
        
        assertThat(savedOrderReservation.getSource()).isEqualTo("order");
        assertThat(savedOrderReservation.getOrderId()).isEqualTo("ORD-123");
    }

    @Test
    @DisplayName("Should handle expiration times correctly")
    void businessLogic_WithExpirationTimes_ShouldHandleCorrectly() {
        // Given
        LocalDateTime futureExpiry = LocalDateTime.now().plusHours(2);
        LocalDateTime pastExpiry = LocalDateTime.now().minusHours(1);
        
        StockReservation activeReservation = createTestReservation(testProduct, null, 5, "user123");
        activeReservation.setExpiresAt(futureExpiry);
        
        StockReservation expiredReservation = createTestReservation(testProduct, testVariant, 3, "user456");
        expiredReservation.setExpiresAt(pastExpiry);

        // When
        StockReservation savedActiveReservation = stockReservationRepository.save(activeReservation);
        StockReservation savedExpiredReservation = stockReservationRepository.save(expiredReservation);

        // Then
        assertThat(savedActiveReservation.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(savedExpiredReservation.getExpiresAt()).isBefore(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should handle reservation lifecycle states")
    void businessLogic_ReservationLifecycle_ShouldHandleStatesCorrectly() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, null, 5, "user123");
        reservation.setIsActive(true);

        // When - Save as active
        StockReservation savedReservation = stockReservationRepository.save(reservation);
        assertThat(savedReservation.getIsActive()).isTrue();

        // When - Mark as inactive (expired/released)
        savedReservation.setIsActive(false);
        StockReservation updatedReservation = stockReservationRepository.save(savedReservation);

        // Then
        assertThat(updatedReservation.getIsActive()).isFalse();
    }

    // =================== Edge Cases and Data Integrity ===================

    @Test
    @DisplayName("Should handle special characters in userId")
    void edgeCases_WithSpecialCharactersInUserId_ShouldHandleCorrectly() {
        // Given
        String specialUserId = "user@domain.com-123_test!";
        StockReservation reservation = createTestReservation(testProduct, null, 5, specialUserId);

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getUserId()).isEqualTo(specialUserId);
    }

    @Test
    @DisplayName("Should handle special characters in reservationId")
    void edgeCases_WithSpecialCharactersInReservationId_ShouldHandleCorrectly() {
        // Given
        String specialReservationId = "res-123_ABC-def!@#";
        StockReservation reservation = createTestReservation(testProduct, null, 5, "user123");
        reservation.setReservationId(specialReservationId);

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getReservationId()).isEqualTo(specialReservationId);
    }

    @Test
    @DisplayName("Should reject zero quantity reservations due to database constraint")
    void edgeCases_WithZeroQuantity_ShouldThrowException() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, null, 0, "user123");

        // When & Then
        assertThatThrownBy(() -> stockReservationRepository.saveAndFlush(reservation))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("stock_reservations_quantity_reserved_check");
    }

    @Test
    @DisplayName("Should handle large quantity reservations")
    void edgeCases_WithLargeQuantity_ShouldHandleCorrectly() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, null, 999999, "user123");

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getQuantityReserved()).isEqualTo(999999);
    }

    @Test
    @DisplayName("Should handle empty optional fields correctly")
    void edgeCases_WithEmptyOptionalFields_ShouldHandleCorrectly() {
        // Given
        StockReservation reservation = createTestReservation(testProduct, null, 5, "user123");
        reservation.setOrderId(""); // Empty string
        reservation.setSource(""); // Empty string

        // When
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        // Then
        assertThat(savedReservation.getOrderId()).isEqualTo("");
        assertThat(savedReservation.getSource()).isEqualTo("");
    }

    @Test
    @DisplayName("Should handle concurrent reservations for same product")
    void businessLogic_ConcurrentReservations_ShouldHandleCorrectly() {
        // Given
        StockReservation reservation1 = createTestReservation(testProduct, null, 5, "user123");
        StockReservation reservation2 = createTestReservation(testProduct, null, 3, "user456");
        StockReservation reservation3 = createTestReservation(testProduct, testVariant, 2, "user789");

        // When
        StockReservation savedReservation1 = stockReservationRepository.save(reservation1);
        StockReservation savedReservation2 = stockReservationRepository.save(reservation2);
        StockReservation savedReservation3 = stockReservationRepository.save(reservation3);

        // Then
        assertThat(savedReservation1.getProduct().getId()).isEqualTo(testProduct.getId());
        assertThat(savedReservation2.getProduct().getId()).isEqualTo(testProduct.getId());
        assertThat(savedReservation3.getProduct().getId()).isEqualTo(testProduct.getId());
        
        assertThat(savedReservation1.getVariant()).isNull();
        assertThat(savedReservation2.getVariant()).isNull();
        assertThat(savedReservation3.getVariant()).isEqualTo(testVariant);
    }

    // =================== Helper Methods ===================

    private StockReservation createTestReservation(Product product, ProductVariant variant, Integer quantity, String userId) {
        return StockReservation.builder()
                .reservationId("res-test-" + testIdCounter++)
                .product(product)
                .variant(variant)
                .quantityReserved(quantity)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .isActive(true)
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
                .priceModifier(new BigDecimal("5.00"))
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
