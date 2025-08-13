package live.alinmiron.beamerparts.product.dto.external.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for CreateProductRequestDto validation
 * Tests all Jakarta validation annotations and business rules
 * 
 * **KEY LESSONS APPLIED:**
 * - Tests define business logic FIRST, implementation follows
 * - Test all validation annotations and edge cases comprehensively
 * - Use Jakarta Validator for realistic validation testing
 * - Test both valid and invalid scenarios with specific error messages
 */
@DisplayName("CreateProductRequestDto Validation Tests")
class CreateProductRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // =========================
    // VALID REQUEST TESTS
    // =========================

    @Test
    @DisplayName("Should pass validation with all required fields")
    void validate_WithValidRequest_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = CreateProductRequestDto.builder()
                .name("High-Performance Brake Pad")
                .slug("high-performance-brake-pad")
                .sku("BRP-001")
                .description("Premium ceramic brake pad for BMW 3-Series")
                .shortDescription("Premium brake pad for enhanced stopping power")
                .basePrice(new BigDecimal("199.99"))
                .categoryId(1L)
                .brand("BMW")
                .weightGrams(1500)
                .dimensionsJson("{\"length\":200,\"width\":100,\"height\":50}")
                .isFeatured(true)
                .status(ProductStatus.ACTIVE)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation with minimal required fields")
    void validate_WithMinimalRequiredFields_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = CreateProductRequestDto.builder()
                .name("Basic Product")
                .slug("basic-product")
                .sku("BASIC-001")
                .basePrice(new BigDecimal("50.00"))
                .categoryId(1L)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation with zero price")
    void validate_WithZeroPrice_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = CreateProductRequestDto.builder()
                .name("Free Product")
                .slug("free-product")
                .sku("FREE-001")
                .basePrice(new BigDecimal("0.00"))
                .categoryId(1L)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    // =========================
    // NAME VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should fail validation with null name")
    void validate_WithNullName_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .name(null)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Product name is required");
    }

    @Test
    @DisplayName("Should fail validation with empty name")
    void validate_WithEmptyName_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .name("")
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Product name is required");
    }

    @Test
    @DisplayName("Should fail validation with blank name")
    void validate_WithBlankName_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .name("   ")
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Product name is required");
    }

    @Test
    @DisplayName("Should fail validation with name exceeding 255 characters")
    void validate_WithNameTooLong_ShouldFailValidation() {
        // Given
        String longName = "A".repeat(256); // 256 characters
        CreateProductRequestDto dto = createValidRequestBuilder()
                .name(longName)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Product name must not exceed 255 characters");
    }

    @Test
    @DisplayName("Should pass validation with name at 255 character limit")
    void validate_WithNameAt255Characters_ShouldPassValidation() {
        // Given
        String maxName = "A".repeat(255); // Exactly 255 characters
        CreateProductRequestDto dto = createValidRequestBuilder()
                .name(maxName)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    // =========================
    // SLUG VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should fail validation with null slug")
    void validate_WithNullSlug_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .slug(null)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Product slug is required");
    }

    @Test
    @DisplayName("Should fail validation with slug exceeding 255 characters")
    void validate_WithSlugTooLong_ShouldFailValidation() {
        // Given
        String longSlug = "a".repeat(256);
        CreateProductRequestDto dto = createValidRequestBuilder()
                .slug(longSlug)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Product slug must not exceed 255 characters");
    }

    // =========================
    // SKU VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should fail validation with null SKU")
    void validate_WithNullSku_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .sku(null)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("SKU is required");
    }

    @Test
    @DisplayName("Should fail validation with SKU exceeding 50 characters")
    void validate_WithSkuTooLong_ShouldFailValidation() {
        // Given
        String longSku = "A".repeat(51);
        CreateProductRequestDto dto = createValidRequestBuilder()
                .sku(longSku)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("SKU must not exceed 50 characters");
    }

    @Test
    @DisplayName("Should pass validation with SKU at 50 character limit")
    void validate_WithSkuAt50Characters_ShouldPassValidation() {
        // Given
        String maxSku = "A".repeat(50);
        CreateProductRequestDto dto = createValidRequestBuilder()
                .sku(maxSku)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    // =========================
    // DESCRIPTION VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should pass validation with null description")
    void validate_WithNullDescription_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .description(null)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with description exceeding 5000 characters")
    void validate_WithDescriptionTooLong_ShouldFailValidation() {
        // Given
        String longDescription = "A".repeat(5001);
        CreateProductRequestDto dto = createValidRequestBuilder()
                .description(longDescription)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Description must not exceed 5000 characters");
    }

    @Test
    @DisplayName("Should pass validation with description at 5000 character limit")
    void validate_WithDescriptionAt5000Characters_ShouldPassValidation() {
        // Given
        String maxDescription = "A".repeat(5000);
        CreateProductRequestDto dto = createValidRequestBuilder()
                .description(maxDescription)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    // =========================
    // SHORT DESCRIPTION VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should fail validation with short description exceeding 500 characters")
    void validate_WithShortDescriptionTooLong_ShouldFailValidation() {
        // Given
        String longShortDescription = "A".repeat(501);
        CreateProductRequestDto dto = createValidRequestBuilder()
                .shortDescription(longShortDescription)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Short description must not exceed 500 characters");
    }

    // =========================
    // PRICE VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should fail validation with null base price")
    void validate_WithNullBasePrice_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .basePrice(null)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Base price is required");
    }

    @Test
    @DisplayName("Should fail validation with negative base price")
    void validate_WithNegativeBasePrice_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .basePrice(new BigDecimal("-10.00"))
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Base price must be zero or positive");
    }

    @Test
    @DisplayName("Should fail validation with base price exceeding maximum")
    void validate_WithBasePriceTooHigh_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .basePrice(new BigDecimal("1000000.00"))
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Base price must not exceed 999,999.99");
    }

    @Test
    @DisplayName("Should pass validation with base price at maximum limit")
    void validate_WithBasePriceAtMaximum_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .basePrice(new BigDecimal("999999.99"))
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    // =========================
    // CATEGORY ID VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should fail validation with null category ID")
    void validate_WithNullCategoryId_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .categoryId(null)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Category ID is required");
    }

    // =========================
    // BRAND VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should fail validation with brand exceeding 100 characters")
    void validate_WithBrandTooLong_ShouldFailValidation() {
        // Given
        String longBrand = "A".repeat(101);
        CreateProductRequestDto dto = createValidRequestBuilder()
                .brand(longBrand)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Brand must not exceed 100 characters");
    }

    @Test
    @DisplayName("Should pass validation with null brand")
    void validate_WithNullBrand_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .brand(null)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    // =========================
    // WEIGHT VALIDATION TESTS
    // =========================

    @Test
    @DisplayName("Should fail validation with zero weight")
    void validate_WithZeroWeight_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .weightGrams(0)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Weight must be positive if specified");
    }

    @Test
    @DisplayName("Should fail validation with negative weight")
    void validate_WithNegativeWeight_ShouldFailValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .weightGrams(-100)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Weight must be positive if specified");
    }

    @Test
    @DisplayName("Should pass validation with null weight")
    void validate_WithNullWeight_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .weightGrams(null)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation with positive weight")
    void validate_WithPositiveWeight_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .weightGrams(1000)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    // =========================
    // DEFAULT VALUES TESTS
    // =========================

    @Test
    @DisplayName("Should have correct default values")
    void builder_ShouldSetCorrectDefaults() {
        // Given & When
        CreateProductRequestDto dto = CreateProductRequestDto.builder()
                .name("Test Product")
                .slug("test-product")
                .sku("TEST-001")
                .basePrice(new BigDecimal("99.99"))
                .categoryId(1L)
                .build();

        // Then
        assertThat(dto.getIsFeatured()).isFalse(); // Default value
        assertThat(dto.getStatus()).isEqualTo(ProductStatus.ACTIVE); // Default value
    }

    @Test
    @DisplayName("Should allow overriding default values")
    void builder_ShouldAllowOverridingDefaults() {
        // Given & When
        CreateProductRequestDto dto = CreateProductRequestDto.builder()
                .name("Featured Product")
                .slug("featured-product")
                .sku("FEATURED-001")
                .basePrice(new BigDecimal("199.99"))
                .categoryId(1L)
                .isFeatured(true)
                .status(ProductStatus.INACTIVE)
                .build();

        // Then
        assertThat(dto.getIsFeatured()).isTrue();
        assertThat(dto.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    // =========================
    // MULTIPLE VIOLATIONS TESTS
    // =========================

    @Test
    @DisplayName("Should report multiple validation violations")
    void validate_WithMultipleViolations_ShouldReportAll() {
        // Given
        CreateProductRequestDto dto = CreateProductRequestDto.builder()
                .name("") // Invalid: blank
                .slug(null) // Invalid: null
                .sku("A".repeat(51)) // Invalid: too long
                .basePrice(new BigDecimal("-10.00")) // Invalid: negative
                .categoryId(null) // Invalid: null
                .weightGrams(-100) // Invalid: negative
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(6);
        assertThat(violations.stream().map(ConstraintViolation::getMessage))
                .containsExactlyInAnyOrder(
                        "Product name is required",
                        "Product slug is required",
                        "SKU must not exceed 50 characters",
                        "Base price must be zero or positive",
                        "Category ID is required",
                        "Weight must be positive if specified"
                );
    }

    // =========================
    // EDGE CASE TESTS
    // =========================

    @Test
    @DisplayName("Should handle special characters in text fields")
    void validate_WithSpecialCharacters_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = CreateProductRequestDto.builder()
                .name("BMW 3-Series Brake Pad (F30) - €199.99")
                .slug("bmw-3-series-brake-pad-f30")
                .sku("BMW-F30-BP-001")
                .description("High-performance brake pad for BMW F30 3-Series models. " +
                           "Temperature range: -20°C to +85°C. Performance: ±2% variance.")
                .shortDescription("Premium brake pad with ±2% performance variance")
                .basePrice(new BigDecimal("199.99"))
                .categoryId(1L)
                .brand("BMW Performance")
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle precise decimal values")
    void validate_WithPreciseDecimalPrice_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .basePrice(new BigDecimal("199.9999")) // High precision
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle very large weight values")
    void validate_WithLargeWeight_ShouldPassValidation() {
        // Given
        CreateProductRequestDto dto = createValidRequestBuilder()
                .weightGrams(Integer.MAX_VALUE)
                .build();

        // When
        Set<ConstraintViolation<CreateProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    // =========================
    // HELPER METHODS
    // =========================

    private CreateProductRequestDto.CreateProductRequestDtoBuilder createValidRequestBuilder() {
        return CreateProductRequestDto.builder()
                .name("Test Product")
                .slug("test-product")
                .sku("TEST-001")
                .description("Test product description")
                .shortDescription("Test short description")
                .basePrice(new BigDecimal("99.99"))
                .categoryId(1L)
                .brand("Test Brand")
                .weightGrams(1000)
                .dimensionsJson("{\"length\":100,\"width\":50,\"height\":25}")
                .isFeatured(false)
                .status(ProductStatus.ACTIVE);
    }
}
