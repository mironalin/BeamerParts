package live.alinmiron.beamerparts.product.dto.internal.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for InventoryInternalDto business logic
 * Tests all business methods, stock calculations, and availability checks
 * 
 * **KEY LESSONS APPLIED:**
 * - Tests define business logic FIRST, implementation follows
 * - Test all business methods and edge cases comprehensively
 * - Focus on stock calculations, availability checks, and variant detection
 * - Test both successful and edge case scenarios
 */
@DisplayName("InventoryInternalDto Tests")
class InventoryInternalDtoTest {

    // =========================
    // AVAILABILITY CALCULATION TESTS
    // =========================

    @Test
    @DisplayName("Should be available when sufficient stock exists")
    void isAvailableForQuantity_WithSufficientStock_ShouldReturnTrue() {
        // Given
        InventoryInternalDto dto = createInventoryDto(10, 2, 3, 5);

        // When
        boolean available = dto.isAvailableForQuantity(5);

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should be available when requesting exact available quantity")
    void isAvailableForQuantity_WithExactQuantity_ShouldReturnTrue() {
        // Given
        InventoryInternalDto dto = createInventoryDto(7, 3, 2, 4);

        // When
        boolean available = dto.isAvailableForQuantity(7);

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should not be available when insufficient stock")
    void isAvailableForQuantity_WithInsufficientStock_ShouldReturnFalse() {
        // Given
        InventoryInternalDto dto = createInventoryDto(3, 2, 1, 2);

        // When
        boolean available = dto.isAvailableForQuantity(5);

        // Then
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("Should be available for zero quantity request")
    void isAvailableForQuantity_WithZeroQuantity_ShouldReturnTrue() {
        // Given
        InventoryInternalDto dto = createInventoryDto(5, 2, 1, 3);

        // When
        boolean available = dto.isAvailableForQuantity(0);

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should handle negative quantity request")
    void isAvailableForQuantity_WithNegativeQuantity_ShouldReturnTrue() {
        // Given
        InventoryInternalDto dto = createInventoryDto(5, 2, 1, 3);

        // When
        boolean available = dto.isAvailableForQuantity(-1);

        // Then
        assertThat(available).isTrue(); // Negative quantity always "available"
    }

    @Test
    @DisplayName("Should not be available when quantity available is null")
    void isAvailableForQuantity_WithNullQuantityAvailable_ShouldReturnFalse() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .quantityAvailable(null)
                .quantityReserved(2)
                .build();

        // When
        boolean available = dto.isAvailableForQuantity(1);

        // Then
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("Should not be available when zero stock")
    void isAvailableForQuantity_WithZeroStock_ShouldReturnFalse() {
        // Given
        InventoryInternalDto dto = createInventoryDto(0, 5, 1, 3);

        // When
        boolean available = dto.isAvailableForQuantity(1);

        // Then
        assertThat(available).isFalse();
    }

    // =========================
    // REMAINING STOCK CALCULATION TESTS
    // =========================

    @Test
    @DisplayName("Should calculate remaining stock after reservation")
    void getRemainingAfterReservation_WithValidReservation_ShouldCalculateCorrectly() {
        // Given
        InventoryInternalDto dto = createInventoryDto(10, 3, 2, 5);

        // When
        int remaining = dto.getRemainingAfterReservation(4);

        // Then
        assertThat(remaining).isEqualTo(6); // 10 - 4 = 6
    }

    @Test
    @DisplayName("Should return zero when reservation exceeds available stock")
    void getRemainingAfterReservation_WithExcessiveReservation_ShouldReturnZero() {
        // Given
        InventoryInternalDto dto = createInventoryDto(5, 2, 1, 3);

        // When
        int remaining = dto.getRemainingAfterReservation(8);

        // Then
        assertThat(remaining).isEqualTo(0); // Math.max(0, 5 - 8) = 0
    }

    @Test
    @DisplayName("Should return exact zero when reservation equals available stock")
    void getRemainingAfterReservation_WithExactReservation_ShouldReturnZero() {
        // Given
        InventoryInternalDto dto = createInventoryDto(7, 3, 1, 4);

        // When
        int remaining = dto.getRemainingAfterReservation(7);

        // Then
        assertThat(remaining).isEqualTo(0); // 7 - 7 = 0
    }

    @Test
    @DisplayName("Should handle zero reservation quantity")
    void getRemainingAfterReservation_WithZeroReservation_ShouldReturnOriginalStock() {
        // Given
        InventoryInternalDto dto = createInventoryDto(8, 2, 1, 4);

        // When
        int remaining = dto.getRemainingAfterReservation(0);

        // Then
        assertThat(remaining).isEqualTo(8); // 8 - 0 = 8
    }

    @Test
    @DisplayName("Should handle negative reservation quantity")
    void getRemainingAfterReservation_WithNegativeReservation_ShouldReturnIncreasedStock() {
        // Given
        InventoryInternalDto dto = createInventoryDto(5, 3, 1, 3);

        // When
        int remaining = dto.getRemainingAfterReservation(-3);

        // Then
        assertThat(remaining).isEqualTo(8); // 5 - (-3) = 8
    }

    @Test
    @DisplayName("Should return zero when quantity available is null")
    void getRemainingAfterReservation_WithNullQuantityAvailable_ShouldReturnZero() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .quantityAvailable(null)
                .quantityReserved(2)
                .build();

        // When
        int remaining = dto.getRemainingAfterReservation(3);

        // Then
        assertThat(remaining).isEqualTo(0);
    }

    // =========================
    // VARIANT DETECTION TESTS
    // =========================

    @Test
    @DisplayName("Should identify variant inventory when variant ID is present")
    void isVariantInventory_WithVariantId_ShouldReturnTrue() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .productId(100L)
                .variantId(200L)
                .productSku("PRODUCT-001")
                .variantSkuSuffix("COLOR-RED")
                .quantityAvailable(5)
                .build();

        // When
        boolean isVariant = dto.isVariantInventory();

        // Then
        assertThat(isVariant).isTrue();
    }

    @Test
    @DisplayName("Should identify base product inventory when variant ID is null")
    void isVariantInventory_WithNullVariantId_ShouldReturnFalse() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .productId(100L)
                .variantId(null)
                .productSku("BASE-PRODUCT-001")
                .variantSkuSuffix(null)
                .quantityAvailable(10)
                .build();

        // When
        boolean isVariant = dto.isVariantInventory();

        // Then
        assertThat(isVariant).isFalse();
    }

    @Test
    @DisplayName("Should handle zero variant ID as variant (0L != null)")
    void isVariantInventory_WithZeroVariantId_ShouldReturnTrue() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .productId(100L)
                .variantId(0L) // 0L is not null, so treated as variant
                .productSku("PRODUCT-001")
                .quantityAvailable(5)
                .build();

        // When
        boolean isVariant = dto.isVariantInventory();

        // Then
        assertThat(isVariant).isTrue(); // 0L != null, so considered variant inventory
    }

    // =========================
    // BUSINESS STATE TESTS
    // =========================

    @Test
    @DisplayName("Should represent in-stock inventory correctly")
    void inventory_WithStockAvailable_ShouldBeInStock() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .quantityAvailable(10)
                .quantityReserved(3)
                .minimumStockLevel(2)
                .reorderPoint(5)
                .totalQuantity(13) // available + reserved
                .isInStock(true)
                .isLowStock(false) // 10 > 5 (reorderPoint)
                .isBelowMinimum(false) // 10 > 2 (minimumStockLevel)
                .build();

        // When & Then
        assertThat(dto.isInStock()).isTrue();
        assertThat(dto.isLowStock()).isFalse();
        assertThat(dto.isBelowMinimum()).isFalse();
        assertThat(dto.getTotalQuantity()).isEqualTo(13);
        assertThat(dto.isAvailableForQuantity(8)).isTrue();
        assertThat(dto.getRemainingAfterReservation(6)).isEqualTo(4);
    }

    @Test
    @DisplayName("Should represent low-stock inventory correctly")
    void inventory_WithLowStock_ShouldBeLowStock() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .quantityAvailable(3) // At reorder point
                .quantityReserved(2)
                .minimumStockLevel(1)
                .reorderPoint(3)
                .totalQuantity(5)
                .isInStock(true)
                .isLowStock(true) // 3 <= 3 (reorderPoint)
                .isBelowMinimum(false) // 3 > 1 (minimumStockLevel)
                .build();

        // When & Then
        assertThat(dto.isInStock()).isTrue();
        assertThat(dto.isLowStock()).isTrue();
        assertThat(dto.isBelowMinimum()).isFalse();
        assertThat(dto.isAvailableForQuantity(3)).isTrue();
        assertThat(dto.isAvailableForQuantity(4)).isFalse();
    }

    @Test
    @DisplayName("Should represent below-minimum inventory correctly")
    void inventory_BelowMinimum_ShouldBeBelowMinimum() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .quantityAvailable(1) // Below minimum
                .quantityReserved(0)
                .minimumStockLevel(3)
                .reorderPoint(5)
                .totalQuantity(1)
                .isInStock(true) // Still in stock, but critical
                .isLowStock(true) // 1 <= 5 (reorderPoint)
                .isBelowMinimum(true) // 1 < 3 (minimumStockLevel)
                .build();

        // When & Then
        assertThat(dto.isInStock()).isTrue();
        assertThat(dto.isLowStock()).isTrue();
        assertThat(dto.isBelowMinimum()).isTrue();
        assertThat(dto.isAvailableForQuantity(1)).isTrue();
        assertThat(dto.isAvailableForQuantity(2)).isFalse();
    }

    @Test
    @DisplayName("Should represent out-of-stock inventory correctly")
    void inventory_OutOfStock_ShouldBeOutOfStock() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .quantityAvailable(0)
                .quantityReserved(5) // All stock reserved
                .minimumStockLevel(2)
                .reorderPoint(4)
                .totalQuantity(5)
                .isInStock(false)
                .isLowStock(true)
                .isBelowMinimum(true)
                .build();

        // When & Then
        assertThat(dto.isInStock()).isFalse();
        assertThat(dto.isLowStock()).isTrue();
        assertThat(dto.isBelowMinimum()).isTrue();
        assertThat(dto.isAvailableForQuantity(1)).isFalse();
        assertThat(dto.getRemainingAfterReservation(1)).isEqualTo(0);
    }

    // =========================
    // EDGE CASE TESTS
    // =========================

    @Test
    @DisplayName("Should handle inventory with all null stock values")
    void inventory_WithAllNullValues_ShouldHandleGracefully() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .productId(100L)
                .productSku("NULL-STOCK-001")
                .quantityAvailable(null)
                .quantityReserved(null)
                .minimumStockLevel(null)
                .reorderPoint(null)
                .totalQuantity(null)
                .build();

        // When & Then
        assertThat(dto.isAvailableForQuantity(1)).isFalse();
        assertThat(dto.getRemainingAfterReservation(1)).isEqualTo(0);
        assertThat(dto.isVariantInventory()).isFalse();
    }

    @Test
    @DisplayName("Should handle inventory with negative stock values")
    void inventory_WithNegativeValues_ShouldHandleCorrectly() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .quantityAvailable(-5) // Invalid state but handled
                .quantityReserved(3)
                .minimumStockLevel(2)
                .reorderPoint(4)
                .build();

        // When & Then
        assertThat(dto.isAvailableForQuantity(1)).isFalse(); // -5 < 1
        assertThat(dto.getRemainingAfterReservation(2)).isEqualTo(0); // Math.max(0, -5 - 2)
    }

    @Test
    @DisplayName("Should handle very large quantity requests")
    void inventory_WithLargeQuantityRequest_ShouldHandleCorrectly() {
        // Given
        InventoryInternalDto dto = createInventoryDto(1000, 200, 50, 100);

        // When & Then
        assertThat(dto.isAvailableForQuantity(Integer.MAX_VALUE)).isFalse();
        assertThat(dto.isAvailableForQuantity(1000)).isTrue();
        assertThat(dto.getRemainingAfterReservation(Integer.MAX_VALUE)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle inventory with timestamp information")
    void inventory_WithTimestamp_ShouldPreserveTimestamp() {
        // Given
        LocalDateTime lastUpdated = LocalDateTime.now().minusHours(2);
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .id(123L)
                .productId(456L)
                .variantId(789L)
                .productSku("TIMESTAMPED-001")
                .variantSkuSuffix("SIZE-L")
                .quantityAvailable(15)
                .quantityReserved(5)
                .lastUpdated(lastUpdated)
                .build();

        // When & Then
        assertThat(dto.getId()).isEqualTo(123L);
        assertThat(dto.getProductId()).isEqualTo(456L);
        assertThat(dto.getVariantId()).isEqualTo(789L);
        assertThat(dto.getProductSku()).isEqualTo("TIMESTAMPED-001");
        assertThat(dto.getVariantSkuSuffix()).isEqualTo("SIZE-L");
        assertThat(dto.getLastUpdated()).isEqualTo(lastUpdated);
        assertThat(dto.isVariantInventory()).isTrue();
        assertThat(dto.isAvailableForQuantity(10)).isTrue();
    }

    @Test
    @DisplayName("Should handle inventory SKU edge cases")
    void inventory_WithSkuEdgeCases_ShouldHandleCorrectly() {
        // Given
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .productSku("") // Empty SKU
                .variantSkuSuffix(null) // Null variant suffix
                .quantityAvailable(5)
                .variantId(null) // Base product
                .build();

        // When & Then
        assertThat(dto.getProductSku()).isEqualTo("");
        assertThat(dto.getVariantSkuSuffix()).isNull();
        assertThat(dto.isVariantInventory()).isFalse();
        assertThat(dto.isAvailableForQuantity(3)).isTrue();
    }

    @Test
    @DisplayName("Should maintain data consistency in complex scenarios")
    void inventory_ComplexScenario_ShouldMaintainConsistency() {
        // Given - Complex inventory state
        InventoryInternalDto dto = InventoryInternalDto.builder()
                .id(999L)
                .productId(1000L)
                .variantId(2000L)
                .productSku("COMPLEX-SCENARIO-001")
                .variantSkuSuffix("CONFIG-A1B2C3")
                .quantityAvailable(25)
                .quantityReserved(15)
                .minimumStockLevel(10)
                .reorderPoint(20)
                .totalQuantity(40) // 25 + 15
                .isInStock(true)
                .isLowStock(true) // 25 > 20, but close to reorder point
                .isBelowMinimum(false) // 25 > 10
                .lastUpdated(LocalDateTime.now().minusMinutes(30))
                .build();

        // When & Then - Complex business checks
        assertThat(dto.isVariantInventory()).isTrue();
        assertThat(dto.isInStock()).isTrue();
        assertThat(dto.isLowStock()).isTrue();
        assertThat(dto.isBelowMinimum()).isFalse();
        
        // Availability checks
        assertThat(dto.isAvailableForQuantity(25)).isTrue(); // Exact available
        assertThat(dto.isAvailableForQuantity(26)).isFalse(); // Exceeds available
        assertThat(dto.isAvailableForQuantity(15)).isTrue(); // Within available
        
        // Remaining stock calculations
        assertThat(dto.getRemainingAfterReservation(10)).isEqualTo(15); // 25 - 10
        assertThat(dto.getRemainingAfterReservation(25)).isEqualTo(0);  // 25 - 25
        assertThat(dto.getRemainingAfterReservation(30)).isEqualTo(0);  // Math.max(0, 25 - 30)
        
        // Data integrity
        assertThat(dto.getQuantityAvailable() + dto.getQuantityReserved()).isEqualTo(dto.getTotalQuantity());
    }

    // =========================
    // HELPER METHODS
    // =========================

    private InventoryInternalDto createInventoryDto(Integer available, Integer reserved, Integer minimum, Integer reorderPoint) {
        return InventoryInternalDto.builder()
                .quantityAvailable(available)
                .quantityReserved(reserved)
                .minimumStockLevel(minimum)
                .reorderPoint(reorderPoint)
                .totalQuantity(available != null && reserved != null ? available + reserved : null)
                .isInStock(available != null && available > 0)
                .isLowStock(available != null && reorderPoint != null && available <= reorderPoint)
                .isBelowMinimum(available != null && minimum != null && available < minimum)
                .build();
    }
}
