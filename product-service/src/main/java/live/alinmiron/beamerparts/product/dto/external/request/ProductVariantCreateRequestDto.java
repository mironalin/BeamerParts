package live.alinmiron.beamerparts.product.dto.external.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Product variant creation request DTO
 */
@Data
public class ProductVariantCreateRequestDto {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "SKU suffix is required")
    private String skuSuffix;
    
    @NotNull(message = "Price modifier is required")
    private BigDecimal priceModifier = BigDecimal.ZERO;
    
    private Boolean isActive = true;
}
