package live.alinmiron.beamerparts.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Add to cart request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequestDto {
    
    @NotBlank(message = "Product SKU is required")
    @Size(max = 50, message = "Product SKU must be less than 50 characters")
    private String productSku;
    
    @Size(max = 20, message = "Variant SKU must be less than 20 characters")
    private String variantSku;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 99, message = "Quantity must be at most 99")
    private Integer quantity;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Unit price must have at most 8 integer digits and 2 decimal places")
    private BigDecimal unitPrice;
}
