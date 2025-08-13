package live.alinmiron.beamerparts.product.dto.internal.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for StockReservationDto business logic
 * Tests all static factory methods, business rules, and state management
 * 
 * **KEY LESSONS APPLIED:**
 * - Tests define business logic FIRST, implementation follows
 * - Test all factory methods and business state transitions
 * - Focus on success/failure scenarios and data consistency
 * - Test timestamp handling and reservation state management
 */
@DisplayName("StockReservationDto Tests")
class StockReservationDtoTest {

    // =========================
    // SUCCESS FACTORY METHOD TESTS
    // =========================

    @Test
    @DisplayName("Should create successful reservation with all required fields")
    void success_WithValidParameters_ShouldCreateSuccessDto() {
        // Given
        String reservationId = "RES-12345";
        String productSku = "BRAKE-PAD-001";
        String variantSku = "VARIANT-001";
        Integer quantityReserved = 3;
        String userId = "USER-123";
        Integer remainingStock = 7;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // When
        StockReservationDto dto = StockReservationDto.success(
                reservationId, productSku, variantSku, quantityReserved, 
                userId, remainingStock, expiresAt
        );

        // Then
        assertThat(dto.getReservationId()).isEqualTo(reservationId);
        assertThat(dto.getProductSku()).isEqualTo(productSku);
        assertThat(dto.getVariantSku()).isEqualTo(variantSku);
        assertThat(dto.getQuantityReserved()).isEqualTo(quantityReserved);
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getRemainingStock()).isEqualTo(remainingStock);
        assertThat(dto.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(dto.isSuccess()).isTrue();
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getFailureReason()).isNull();
        assertThat(dto.getReservedAt()).isNotNull();
        assertThat(dto.getReservedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create successful reservation for base product without variant")
    void success_WithNullVariantSku_ShouldCreateSuccessDto() {
        // Given
        String reservationId = "RES-BASE-001";
        String productSku = "BASE-PRODUCT-001";
        String variantSku = null; // Base product has no variant
        Integer quantityReserved = 2;
        String userId = "USER-456";
        Integer remainingStock = 8;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        // When
        StockReservationDto dto = StockReservationDto.success(
                reservationId, productSku, variantSku, quantityReserved, 
                userId, remainingStock, expiresAt
        );

        // Then
        assertThat(dto.getReservationId()).isEqualTo(reservationId);
        assertThat(dto.getProductSku()).isEqualTo(productSku);
        assertThat(dto.getVariantSku()).isNull();
        assertThat(dto.getQuantityReserved()).isEqualTo(quantityReserved);
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getRemainingStock()).isEqualTo(remainingStock);
        assertThat(dto.isSuccess()).isTrue();
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should create successful reservation with zero remaining stock")
    void success_WithZeroRemainingStock_ShouldCreateSuccessDto() {
        // Given
        String reservationId = "RES-LAST-001";
        String productSku = "LAST-ITEM-001";
        String variantSku = "FINAL-VARIANT";
        Integer quantityReserved = 5;
        String userId = "USER-789";
        Integer remainingStock = 0; // Last items reserved
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        // When
        StockReservationDto dto = StockReservationDto.success(
                reservationId, productSku, variantSku, quantityReserved, 
                userId, remainingStock, expiresAt
        );

        // Then
        assertThat(dto.getRemainingStock()).isEqualTo(0);
        assertThat(dto.isSuccess()).isTrue();
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should set reserved timestamp to current time")
    void success_ShouldSetReservedAtToCurrentTime() {
        // Given
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // When
        StockReservationDto dto = StockReservationDto.success(
                "RES-TIME-001", "PRODUCT-001", null, 1, 
                "USER-001", 5, expiresAt
        );
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);

        // Then
        assertThat(dto.getReservedAt()).isAfter(beforeCreation);
        assertThat(dto.getReservedAt()).isBefore(afterCreation);
    }

    // =========================
    // FAILURE FACTORY METHOD TESTS
    // =========================

    @Test
    @DisplayName("Should create failed reservation with failure reason")
    void failure_WithValidParameters_ShouldCreateFailureDto() {
        // Given
        String productSku = "OUT-OF-STOCK-001";
        String variantSku = "VARIANT-OOS";
        String userId = "USER-999";
        String failureReason = "Insufficient stock available";

        // When
        StockReservationDto dto = StockReservationDto.failure(
                productSku, variantSku, userId, failureReason
        );

        // Then
        assertThat(dto.getProductSku()).isEqualTo(productSku);
        assertThat(dto.getVariantSku()).isEqualTo(variantSku);
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getFailureReason()).isEqualTo(failureReason);
        assertThat(dto.isSuccess()).isFalse();
        assertThat(dto.isActive()).isFalse();
        assertThat(dto.getReservationId()).isNull();
        assertThat(dto.getQuantityReserved()).isNull();
        assertThat(dto.getRemainingStock()).isNull();
        assertThat(dto.getReservedAt()).isNull();
        assertThat(dto.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("Should create failed reservation for base product")
    void failure_WithNullVariantSku_ShouldCreateFailureDto() {
        // Given
        String productSku = "FAILED-BASE-001";
        String variantSku = null;
        String userId = "USER-ERROR";
        String failureReason = "Product not found";

        // When
        StockReservationDto dto = StockReservationDto.failure(
                productSku, variantSku, userId, failureReason
        );

        // Then
        assertThat(dto.getProductSku()).isEqualTo(productSku);
        assertThat(dto.getVariantSku()).isNull();
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getFailureReason()).isEqualTo(failureReason);
        assertThat(dto.isSuccess()).isFalse();
        assertThat(dto.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should create failed reservation with null failure reason")
    void failure_WithNullFailureReason_ShouldCreateFailureDto() {
        // Given
        String productSku = "UNKNOWN-ERROR-001";
        String variantSku = "VARIANT-ERROR";
        String userId = "USER-UNKNOWN";
        String failureReason = null;

        // When
        StockReservationDto dto = StockReservationDto.failure(
                productSku, variantSku, userId, failureReason
        );

        // Then
        assertThat(dto.getFailureReason()).isNull();
        assertThat(dto.isSuccess()).isFalse();
        assertThat(dto.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should create failed reservation with empty failure reason")
    void failure_WithEmptyFailureReason_ShouldCreateFailureDto() {
        // Given
        String productSku = "EMPTY-ERROR-001";
        String variantSku = "VARIANT-EMPTY";
        String userId = "USER-EMPTY";
        String failureReason = "";

        // When
        StockReservationDto dto = StockReservationDto.failure(
                productSku, variantSku, userId, failureReason
        );

        // Then
        assertThat(dto.getFailureReason()).isEqualTo("");
        assertThat(dto.isSuccess()).isFalse();
    }

    // =========================
    // BUSINESS LOGIC TESTS
    // =========================

    @Test
    @DisplayName("Should represent successful cart reservation")
    void createCartReservation_ShouldHaveCorrectBusinessContext() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15); // Typical cart expiry

        // When
        StockReservationDto dto = StockReservationDto.success(
                "CART-RES-001", "CART-PRODUCT-001", "VARIANT-001", 
                2, "CART-USER-001", 3, expiresAt
        );

        // Additional cart-specific fields would be set by caller
        dto.setSource("cart");
        dto.setOrderId(null); // No order yet

        // Then
        assertThat(dto.isSuccess()).isTrue();
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getSource()).isEqualTo("cart");
        assertThat(dto.getOrderId()).isNull();
        assertThat(dto.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should represent successful order reservation")
    void createOrderReservation_ShouldHaveCorrectBusinessContext() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24); // Longer expiry for orders

        // When
        StockReservationDto dto = StockReservationDto.success(
                "ORDER-RES-001", "ORDER-PRODUCT-001", null, 
                1, "ORDER-USER-001", 9, expiresAt
        );

        // Additional order-specific fields would be set by caller
        dto.setSource("order");
        dto.setOrderId("ORDER-123456");

        // Then
        assertThat(dto.isSuccess()).isTrue();
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getSource()).isEqualTo("order");
        assertThat(dto.getOrderId()).isEqualTo("ORDER-123456");
        assertThat(dto.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
    }

    @Test
    @DisplayName("Should represent failed reservation due to insufficient stock")
    void insufficientStockFailure_ShouldHaveCorrectErrorContext() {
        // When
        StockReservationDto dto = StockReservationDto.failure(
                "INSUFFICIENT-001", "VARIANT-LOW", "USER-WANT-MORE", 
                "Requested quantity (5) exceeds available stock (2)"
        );

        // Then
        assertThat(dto.isSuccess()).isFalse();
        assertThat(dto.isActive()).isFalse();
        assertThat(dto.getFailureReason()).contains("Requested quantity");
        assertThat(dto.getFailureReason()).contains("exceeds available stock");
        assertThat(dto.getRemainingStock()).isNull(); // No stock info on failure
    }

    @Test
    @DisplayName("Should represent failed reservation due to product not found")
    void productNotFoundFailure_ShouldHaveCorrectErrorContext() {
        // When
        StockReservationDto dto = StockReservationDto.failure(
                "MISSING-001", "MISSING-VARIANT", "USER-SEARCH", 
                "Product with SKU 'MISSING-001' and variant 'MISSING-VARIANT' not found"
        );

        // Then
        assertThat(dto.isSuccess()).isFalse();
        assertThat(dto.getFailureReason()).contains("not found");
        assertThat(dto.getFailureReason()).contains("MISSING-001");
        assertThat(dto.getFailureReason()).contains("MISSING-VARIANT");
    }

    // =========================
    // EDGE CASE TESTS
    // =========================

    @Test
    @DisplayName("Should handle null parameters in success factory")
    void success_WithNullParameters_ShouldCreatePartialDto() {
        // When
        StockReservationDto dto = StockReservationDto.success(
                null, null, null, null, null, null, null
        );

        // Then
        assertThat(dto.getReservationId()).isNull();
        assertThat(dto.getProductSku()).isNull();
        assertThat(dto.getVariantSku()).isNull();
        assertThat(dto.getQuantityReserved()).isNull();
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getRemainingStock()).isNull();
        assertThat(dto.getExpiresAt()).isNull();
        assertThat(dto.isSuccess()).isTrue(); // Factory method sets this
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getReservedAt()).isNotNull(); // Set to current time
    }

    @Test
    @DisplayName("Should handle null parameters in failure factory")
    void failure_WithNullParameters_ShouldCreatePartialDto() {
        // When
        StockReservationDto dto = StockReservationDto.failure(null, null, null, null);

        // Then
        assertThat(dto.getProductSku()).isNull();
        assertThat(dto.getVariantSku()).isNull();
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getFailureReason()).isNull();
        assertThat(dto.isSuccess()).isFalse(); // Factory method sets this
        assertThat(dto.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should handle negative quantity in success factory")
    void success_WithNegativeQuantity_ShouldCreateDto() {
        // Given
        Integer negativeQuantity = -3;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // When
        StockReservationDto dto = StockReservationDto.success(
                "NEGATIVE-RES-001", "PRODUCT-001", null, negativeQuantity, 
                "USER-001", 10, expiresAt
        );

        // Then
        assertThat(dto.getQuantityReserved()).isEqualTo(-3);
        assertThat(dto.isSuccess()).isTrue(); // Factory doesn't validate business rules
    }

    @Test
    @DisplayName("Should handle negative remaining stock")
    void success_WithNegativeRemainingStock_ShouldCreateDto() {
        // Given
        Integer negativeRemaining = -2;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // When
        StockReservationDto dto = StockReservationDto.success(
                "NEGATIVE-STOCK-001", "PRODUCT-001", null, 5, 
                "USER-001", negativeRemaining, expiresAt
        );

        // Then
        assertThat(dto.getRemainingStock()).isEqualTo(-2);
        assertThat(dto.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should handle past expiration time")
    void success_WithPastExpirationTime_ShouldCreateDto() {
        // Given
        LocalDateTime pastExpiry = LocalDateTime.now().minusMinutes(30);

        // When
        StockReservationDto dto = StockReservationDto.success(
                "EXPIRED-RES-001", "PRODUCT-001", null, 1, 
                "USER-001", 5, pastExpiry
        );

        // Then
        assertThat(dto.getExpiresAt()).isBefore(LocalDateTime.now());
        assertThat(dto.isSuccess()).isTrue();
        assertThat(dto.isActive()).isTrue(); // Factory doesn't check expiration
    }

    @Test
    @DisplayName("Should handle very long strings in failure reason")
    void failure_WithLongFailureReason_ShouldCreateDto() {
        // Given
        String longReason = "This is a very long failure reason that describes in great detail " +
                "what went wrong during the stock reservation process, including technical details, " +
                "error codes, system states, and troubleshooting information that might be useful " +
                "for debugging purposes in complex scenarios where multiple systems interact.";

        // When
        StockReservationDto dto = StockReservationDto.failure(
                "LONG-ERROR-001", "VARIANT-001", "USER-001", longReason
        );

        // Then
        assertThat(dto.getFailureReason()).isEqualTo(longReason);
        assertThat(dto.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Should handle special characters in SKUs and IDs")
    void success_WithSpecialCharacters_ShouldCreateDto() {
        // Given
        String specialReservationId = "RES-2024-001_β";
        String specialProductSku = "BMW-F30-320i_€199.99";
        String specialVariantSku = "COLOR-α&β";
        String specialUserId = "USER-123@domain.com";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // When
        StockReservationDto dto = StockReservationDto.success(
                specialReservationId, specialProductSku, specialVariantSku, 
                1, specialUserId, 5, expiresAt
        );

        // Then
        assertThat(dto.getReservationId()).isEqualTo(specialReservationId);
        assertThat(dto.getProductSku()).isEqualTo(specialProductSku);
        assertThat(dto.getVariantSku()).isEqualTo(specialVariantSku);
        assertThat(dto.getUserId()).isEqualTo(specialUserId);
        assertThat(dto.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should handle builder pattern correctly")
    void builder_ShouldCreateDtoWithAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusMinutes(30);

        // When
        StockReservationDto dto = StockReservationDto.builder()
                .reservationId("BUILDER-001")
                .productSku("BUILD-PRODUCT-001")
                .variantSku("BUILD-VARIANT-001")
                .quantityReserved(4)
                .userId("BUILD-USER-001")
                .orderId("BUILD-ORDER-001")
                .source("admin")
                .remainingStock(6)
                .totalReserved(10)
                .reservedAt(now)
                .expiresAt(expires)
                .isActive(true)
                .success(true)
                .failureReason(null)
                .build();

        // Then
        assertThat(dto.getReservationId()).isEqualTo("BUILDER-001");
        assertThat(dto.getProductSku()).isEqualTo("BUILD-PRODUCT-001");
        assertThat(dto.getVariantSku()).isEqualTo("BUILD-VARIANT-001");
        assertThat(dto.getQuantityReserved()).isEqualTo(4);
        assertThat(dto.getUserId()).isEqualTo("BUILD-USER-001");
        assertThat(dto.getOrderId()).isEqualTo("BUILD-ORDER-001");
        assertThat(dto.getSource()).isEqualTo("admin");
        assertThat(dto.getRemainingStock()).isEqualTo(6);
        assertThat(dto.getTotalReserved()).isEqualTo(10);
        assertThat(dto.getReservedAt()).isEqualTo(now);
        assertThat(dto.getExpiresAt()).isEqualTo(expires);
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.isSuccess()).isTrue();
        assertThat(dto.getFailureReason()).isNull();
    }
}
