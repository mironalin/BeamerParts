package live.alinmiron.beamerparts.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inventory update request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventoryRequestDto {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    private Long variantId;
    
    @NotNull(message = "Quantity available is required")
    @PositiveOrZero(message = "Quantity available must be zero or positive")
    private Integer quantityAvailable;
    
    @NotNull(message = "Quantity reserved is required")
    @PositiveOrZero(message = "Quantity reserved must be zero or positive")
    private Integer quantityReserved;
    
    @NotNull(message = "Minimum stock level is required")
    @PositiveOrZero(message = "Minimum stock level must be zero or positive")
    private Integer minimumStockLevel;
    
    @NotNull(message = "Reorder point is required")
    @PositiveOrZero(message = "Reorder point must be zero or positive")
    private Integer reorderPoint;
}
