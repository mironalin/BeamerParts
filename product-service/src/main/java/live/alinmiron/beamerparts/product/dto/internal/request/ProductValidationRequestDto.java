package live.alinmiron.beamerparts.product.dto.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Single product validation request item for cart operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductValidationRequestDto {
    
    @NotBlank(message = "Product SKU is required")
    private String sku;
    
    private String variantSku; // Optional
    
    @PositiveOrZero(message = "Quantity must be positive or zero")
    private Integer requestedQuantity;
    
    // For context
    private String userId;
    private String source; // "cart", "checkout", "admin"
}
