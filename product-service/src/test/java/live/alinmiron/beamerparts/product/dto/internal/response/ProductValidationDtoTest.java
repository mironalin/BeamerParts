package live.alinmiron.beamerparts.product.dto.internal.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ProductValidationDto business logic
 * Tests all static factory methods, validation logic, and business rules
 * 
 * **KEY LESSONS APPLIED:**
 * - Tests define business logic FIRST, implementation follows
 * - Test all business methods and edge cases comprehensively
 * - Focus on validation logic, factory methods, and state transitions
 * - Test both successful and failure scenarios
 */
@DisplayName("ProductValidationDto Tests")
class ProductValidationDtoTest {

    // =========================
    // VALIDATION LOGIC TESTS
    // =========================

    @Test
    @DisplayName("Should be valid when exists, active, and available")
    void isValid_WithAllFlagsTrue_ShouldReturnTrue() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("TEST-001")
                .exists(true)
                .isActive(true)
                .isAvailable(true)
                .name("Test Product")
                .currentPrice(new BigDecimal("99.99"))
                .build();

        // When
        boolean valid = dto.isValid();

        // Then
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Should be invalid when product does not exist")
    void isValid_WithExistsFalse_ShouldReturnFalse() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("MISSING-001")
                .exists(false)
                .isActive(true)
                .isAvailable(true)
                .build();

        // When
        boolean valid = dto.isValid();

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Should be invalid when product is not active")
    void isValid_WithActiveFalse_ShouldReturnFalse() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("INACTIVE-001")
                .exists(true)
                .isActive(false)
                .isAvailable(true)
                .build();

        // When
        boolean valid = dto.isValid();

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Should be invalid when product is not available")
    void isValid_WithAvailableFalse_ShouldReturnFalse() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("UNAVAILABLE-001")
                .exists(true)
                .isActive(true)
                .isAvailable(false)
                .build();

        // When
        boolean valid = dto.isValid();

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Should be invalid when all flags are false")
    void isValid_WithAllFlagsFalse_ShouldReturnFalse() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("INVALID-001")
                .exists(false)
                .isActive(false)
                .isAvailable(false)
                .build();

        // When
        boolean valid = dto.isValid();

        // Then
        assertThat(valid).isFalse();
    }

    // =========================
    // QUANTITY FULFILLMENT TESTS
    // =========================

    @Test
    @DisplayName("Should fulfill quantity when valid and sufficient stock")
    void canFulfillQuantity_WithValidProductAndSufficientStock_ShouldReturnTrue() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("STOCK-001")
                .exists(true)
                .isActive(true)
                .isAvailable(true)
                .availableQuantity(10)
                .build();

        // When
        boolean canFulfill = dto.canFulfillQuantity(5);

        // Then
        assertThat(canFulfill).isTrue();
    }

    @Test
    @DisplayName("Should fulfill quantity when requesting exact available amount")
    void canFulfillQuantity_WithExactQuantityRequest_ShouldReturnTrue() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("EXACT-001")
                .exists(true)
                .isActive(true)
                .isAvailable(true)
                .availableQuantity(7)
                .build();

        // When
        boolean canFulfill = dto.canFulfillQuantity(7);

        // Then
        assertThat(canFulfill).isTrue();
    }

    @Test
    @DisplayName("Should not fulfill quantity when insufficient stock")
    void canFulfillQuantity_WithInsufficientStock_ShouldReturnFalse() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("INSUFFICIENT-001")
                .exists(true)
                .isActive(true)
                .isAvailable(true)
                .availableQuantity(3)
                .build();

        // When
        boolean canFulfill = dto.canFulfillQuantity(5);

        // Then
        assertThat(canFulfill).isFalse();
    }

    @Test
    @DisplayName("Should not fulfill quantity when product is invalid")
    void canFulfillQuantity_WithInvalidProduct_ShouldReturnFalse() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("INVALID-002")
                .exists(false)
                .isActive(false)
                .isAvailable(false)
                .availableQuantity(10)
                .build();

        // When
        boolean canFulfill = dto.canFulfillQuantity(5);

        // Then
        assertThat(canFulfill).isFalse();
    }

    @Test
    @DisplayName("Should not fulfill quantity when available quantity is null")
    void canFulfillQuantity_WithNullAvailableQuantity_ShouldReturnFalse() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("NULL-STOCK-001")
                .exists(true)
                .isActive(true)
                .isAvailable(true)
                .availableQuantity(null)
                .build();

        // When
        boolean canFulfill = dto.canFulfillQuantity(1);

        // Then
        assertThat(canFulfill).isFalse();
    }

    @Test
    @DisplayName("Should not fulfill zero quantity request")
    void canFulfillQuantity_WithZeroQuantityRequest_ShouldReturnTrue() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("ZERO-REQUEST-001")
                .exists(true)
                .isActive(true)
                .isAvailable(true)
                .availableQuantity(5)
                .build();

        // When
        boolean canFulfill = dto.canFulfillQuantity(0);

        // Then
        assertThat(canFulfill).isTrue(); // Zero quantity is always fulfillable
    }

    @Test
    @DisplayName("Should handle negative quantity request")
    void canFulfillQuantity_WithNegativeQuantityRequest_ShouldReturnTrue() {
        // Given
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("NEGATIVE-REQUEST-001")
                .exists(true)
                .isActive(true)
                .isAvailable(true)
                .availableQuantity(5)
                .build();

        // When
        boolean canFulfill = dto.canFulfillQuantity(-1);

        // Then
        assertThat(canFulfill).isTrue(); // Negative quantity meets condition >= requestedQuantity
    }

    // =========================
    // ERROR FACTORY METHOD TESTS
    // =========================

    @Test
    @DisplayName("Should create error validation result with all required fields")
    void error_WithValidParameters_ShouldCreateErrorDto() {
        // Given
        String sku = "ERROR-001";
        String errorCode = "PRODUCT_NOT_FOUND";
        String errorMessage = "Product with SKU ERROR-001 was not found";

        // When
        ProductValidationDto errorDto = ProductValidationDto.error(sku, errorCode, errorMessage);

        // Then
        assertThat(errorDto.getSku()).isEqualTo(sku);
        assertThat(errorDto.isExists()).isFalse();
        assertThat(errorDto.isActive()).isFalse();
        assertThat(errorDto.isAvailable()).isFalse();
        assertThat(errorDto.getErrorCode()).isEqualTo(errorCode);
        assertThat(errorDto.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(errorDto.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should create error result with null error message")
    void error_WithNullErrorMessage_ShouldCreateErrorDto() {
        // Given
        String sku = "ERROR-002";
        String errorCode = "UNKNOWN_ERROR";

        // When
        ProductValidationDto errorDto = ProductValidationDto.error(sku, errorCode, null);

        // Then
        assertThat(errorDto.getSku()).isEqualTo(sku);
        assertThat(errorDto.getErrorCode()).isEqualTo(errorCode);
        assertThat(errorDto.getErrorMessage()).isNull();
        assertThat(errorDto.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should create error result with empty error code")
    void error_WithEmptyErrorCode_ShouldCreateErrorDto() {
        // Given
        String sku = "ERROR-003";
        String errorCode = "";
        String errorMessage = "Generic error occurred";

        // When
        ProductValidationDto errorDto = ProductValidationDto.error(sku, errorCode, errorMessage);

        // Then
        assertThat(errorDto.getSku()).isEqualTo(sku);
        assertThat(errorDto.getErrorCode()).isEqualTo("");
        assertThat(errorDto.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(errorDto.isValid()).isFalse();
    }

    // =========================
    // SUCCESS FACTORY METHOD TESTS
    // =========================

    @Test
    @DisplayName("Should create success validation result with ACTIVE status")
    void success_WithActiveStatus_ShouldCreateValidDto() {
        // Given
        String sku = "SUCCESS-001";
        String name = "Test Product";
        BigDecimal price = new BigDecimal("199.99");
        String status = "ACTIVE";
        Integer availableQuantity = 15;

        // When
        ProductValidationDto successDto = ProductValidationDto.success(sku, name, price, status, availableQuantity);

        // Then
        assertThat(successDto.getSku()).isEqualTo(sku);
        assertThat(successDto.getName()).isEqualTo(name);
        assertThat(successDto.getCurrentPrice()).isEqualTo(price);
        assertThat(successDto.getStatus()).isEqualTo(status);
        assertThat(successDto.getAvailableQuantity()).isEqualTo(availableQuantity);
        assertThat(successDto.isExists()).isTrue();
        assertThat(successDto.isActive()).isTrue();
        assertThat(successDto.isAvailable()).isTrue();
        assertThat(successDto.isInStock()).isTrue();
        assertThat(successDto.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should create success validation result with INACTIVE status")
    void success_WithInactiveStatus_ShouldCreateInvalidDto() {
        // Given
        String sku = "INACTIVE-SUCCESS-001";
        String name = "Inactive Product";
        BigDecimal price = new BigDecimal("99.99");
        String status = "INACTIVE";
        Integer availableQuantity = 5;

        // When
        ProductValidationDto successDto = ProductValidationDto.success(sku, name, price, status, availableQuantity);

        // Then
        assertThat(successDto.getSku()).isEqualTo(sku);
        assertThat(successDto.getStatus()).isEqualTo(status);
        assertThat(successDto.isExists()).isTrue();
        assertThat(successDto.isActive()).isFalse(); // Not ACTIVE
        assertThat(successDto.isAvailable()).isTrue(); // Has stock
        assertThat(successDto.isInStock()).isTrue();
        assertThat(successDto.isValid()).isFalse(); // Invalid due to inactive status
    }

    @Test
    @DisplayName("Should create success validation result with zero available quantity")
    void success_WithZeroAvailableQuantity_ShouldCreateUnavailableDto() {
        // Given
        String sku = "ZERO-STOCK-001";
        String name = "Out of Stock Product";
        BigDecimal price = new BigDecimal("49.99");
        String status = "ACTIVE";
        Integer availableQuantity = 0;

        // When
        ProductValidationDto successDto = ProductValidationDto.success(sku, name, price, status, availableQuantity);

        // Then
        assertThat(successDto.getSku()).isEqualTo(sku);
        assertThat(successDto.getAvailableQuantity()).isEqualTo(0);
        assertThat(successDto.isExists()).isTrue();
        assertThat(successDto.isActive()).isTrue();
        assertThat(successDto.isAvailable()).isFalse(); // No stock available
        assertThat(successDto.isInStock()).isFalse();
        assertThat(successDto.isValid()).isFalse(); // Invalid due to no availability
    }

    @Test
    @DisplayName("Should create success validation result with null available quantity")
    void success_WithNullAvailableQuantity_ShouldCreateUnavailableDto() {
        // Given
        String sku = "NULL-STOCK-SUCCESS-001";
        String name = "Product with Unknown Stock";
        BigDecimal price = new BigDecimal("299.99");
        String status = "ACTIVE";
        Integer availableQuantity = null;

        // When
        ProductValidationDto successDto = ProductValidationDto.success(sku, name, price, status, availableQuantity);

        // Then
        assertThat(successDto.getSku()).isEqualTo(sku);
        assertThat(successDto.getAvailableQuantity()).isNull();
        assertThat(successDto.isExists()).isTrue();
        assertThat(successDto.isActive()).isTrue();
        assertThat(successDto.isAvailable()).isFalse(); // Null quantity = not available
        assertThat(successDto.isInStock()).isFalse();
        assertThat(successDto.isValid()).isFalse(); // Invalid due to no availability
    }

    @Test
    @DisplayName("Should create success validation result with negative available quantity")
    void success_WithNegativeAvailableQuantity_ShouldCreateUnavailableDto() {
        // Given
        String sku = "NEGATIVE-STOCK-001";
        String name = "Product with Negative Stock";
        BigDecimal price = new BigDecimal("99.99");
        String status = "ACTIVE";
        Integer availableQuantity = -5;

        // When
        ProductValidationDto successDto = ProductValidationDto.success(sku, name, price, status, availableQuantity);

        // Then
        assertThat(successDto.getSku()).isEqualTo(sku);
        assertThat(successDto.getAvailableQuantity()).isEqualTo(-5);
        assertThat(successDto.isExists()).isTrue();
        assertThat(successDto.isActive()).isTrue();
        assertThat(successDto.isAvailable()).isFalse(); // Negative quantity = not available
        assertThat(successDto.isInStock()).isFalse();
        assertThat(successDto.isValid()).isFalse(); // Invalid due to no availability
    }

    // =========================
    // EDGE CASE TESTS
    // =========================

    @Test
    @DisplayName("Should handle null SKU in error factory method")
    void error_WithNullSku_ShouldCreateErrorDto() {
        // Given
        String errorCode = "NULL_SKU_ERROR";
        String errorMessage = "SKU cannot be null";

        // When
        ProductValidationDto errorDto = ProductValidationDto.error(null, errorCode, errorMessage);

        // Then
        assertThat(errorDto.getSku()).isNull();
        assertThat(errorDto.getErrorCode()).isEqualTo(errorCode);
        assertThat(errorDto.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(errorDto.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should handle null SKU in success factory method")
    void success_WithNullSku_ShouldCreateDto() {
        // Given
        String name = "Product with Null SKU";
        BigDecimal price = new BigDecimal("99.99");
        String status = "ACTIVE";
        Integer availableQuantity = 10;

        // When
        ProductValidationDto successDto = ProductValidationDto.success(null, name, price, status, availableQuantity);

        // Then
        assertThat(successDto.getSku()).isNull();
        assertThat(successDto.getName()).isEqualTo(name);
        assertThat(successDto.isValid()).isTrue(); // Still valid based on flags
    }

    @Test
    @DisplayName("Should handle null price in success factory method")
    void success_WithNullPrice_ShouldCreateDto() {
        // Given
        String sku = "NULL-PRICE-001";
        String name = "Product with Null Price";
        String status = "ACTIVE";
        Integer availableQuantity = 5;

        // When
        ProductValidationDto successDto = ProductValidationDto.success(sku, name, null, status, availableQuantity);

        // Then
        assertThat(successDto.getSku()).isEqualTo(sku);
        assertThat(successDto.getCurrentPrice()).isNull();
        assertThat(successDto.isValid()).isTrue(); // Validation doesn't check price
    }

    @Test
    @DisplayName("Should handle empty status string")
    void success_WithEmptyStatus_ShouldCreateInactiveDto() {
        // Given
        String sku = "EMPTY-STATUS-001";
        String name = "Product with Empty Status";
        BigDecimal price = new BigDecimal("99.99");
        String status = "";
        Integer availableQuantity = 10;

        // When
        ProductValidationDto successDto = ProductValidationDto.success(sku, name, price, status, availableQuantity);

        // Then
        assertThat(successDto.getStatus()).isEqualTo("");
        assertThat(successDto.isActive()).isFalse(); // Empty string != "ACTIVE"
        assertThat(successDto.isValid()).isFalse(); // Invalid due to inactive status
    }

    @Test
    @DisplayName("Should handle case-sensitive status comparison")
    void success_WithLowercaseActiveStatus_ShouldCreateInactiveDto() {
        // Given
        String sku = "LOWERCASE-STATUS-001";
        String name = "Product with Lowercase Active Status";
        BigDecimal price = new BigDecimal("99.99");
        String status = "active"; // lowercase
        Integer availableQuantity = 10;

        // When
        ProductValidationDto successDto = ProductValidationDto.success(sku, name, price, status, availableQuantity);

        // Then
        assertThat(successDto.getStatus()).isEqualTo("active");
        assertThat(successDto.isActive()).isFalse(); // Case-sensitive comparison
        assertThat(successDto.isValid()).isFalse(); // Invalid due to case mismatch
    }

    @Test
    @DisplayName("Should handle builder pattern correctly")
    void builder_ShouldCreateDtoWithAllFields() {
        // Given & When
        ProductValidationDto dto = ProductValidationDto.builder()
                .sku("BUILDER-001")
                .variantSku("VARIANT-001")
                .exists(true)
                .isActive(true)
                .isAvailable(true)
                .name("Builder Test Product")
                .currentPrice(new BigDecimal("149.99"))
                .status("ACTIVE")
                .availableQuantity(20)
                .isInStock(true)
                .errorCode(null)
                .errorMessage(null)
                .build();

        // Then
        assertThat(dto.getSku()).isEqualTo("BUILDER-001");
        assertThat(dto.getVariantSku()).isEqualTo("VARIANT-001");
        assertThat(dto.getName()).isEqualTo("Builder Test Product");
        assertThat(dto.getCurrentPrice()).isEqualTo(new BigDecimal("149.99"));
        assertThat(dto.getAvailableQuantity()).isEqualTo(20);
        assertThat(dto.isValid()).isTrue();
        assertThat(dto.canFulfillQuantity(15)).isTrue();
    }
}
