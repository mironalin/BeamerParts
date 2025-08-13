package live.alinmiron.beamerparts.product.dto.external.request;

import jakarta.validation.constraints.*;
import live.alinmiron.beamerparts.product.entity.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product creation request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequestDto {
    
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Product slug is required")
    @Size(max = 255, message = "Product slug must not exceed 255 characters")
    private String slug;
    
    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
    
    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;
    
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Base price must be zero or positive")
    @DecimalMax(value = "999999.99", message = "Base price must not exceed 999,999.99")
    private BigDecimal basePrice;
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;
    
    @Positive(message = "Weight must be positive if specified")
    private Integer weightGrams;
    
    private String dimensionsJson;
    
    @Builder.Default
    private Boolean isFeatured = false;
    
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;
}
