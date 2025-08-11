package live.alinmiron.beamerparts.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import live.alinmiron.beamerparts.product.entity.StockMovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stock movement creation request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockMovementRequestDto {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    private Long variantId;
    
    @NotNull(message = "Movement type is required")
    private StockMovementType movementType;
    
    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;
    
    @Size(max = 100, message = "Reason must not exceed 100 characters")
    private String reason;
    
    @Size(max = 50, message = "Reference ID must not exceed 50 characters")
    private String referenceId;
    
    @NotBlank(message = "User code is required")
    @Size(max = 255, message = "User code must not exceed 255 characters")
    private String userCode;
}
