package live.alinmiron.beamerparts.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product variant creation request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductVariantRequestDto {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotBlank(message = "Variant name is required")
    @Size(max = 100, message = "Variant name must not exceed 100 characters")
    private String name;
    
    @NotBlank(message = "SKU suffix is required")
    @Size(max = 20, message = "SKU suffix must not exceed 20 characters")
    private String skuSuffix;
    
    @NotNull(message = "Price modifier is required")
    @DecimalMin(value = "-999999.99", message = "Price modifier must not be less than -999,999.99")
    @DecimalMax(value = "999999.99", message = "Price modifier must not exceed 999,999.99")
    @Builder.Default
    private BigDecimal priceModifier = BigDecimal.ZERO;
    
    @Builder.Default
    private Boolean isActive = true;
}
