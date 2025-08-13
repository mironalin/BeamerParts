package live.alinmiron.beamerparts.product.dto.external.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating inventory entries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryRequestDto {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    private Long productVariantId;
    
    @NotNull(message = "Quantity available is required")
    @Min(value = 0, message = "Quantity available cannot be negative")
    private Integer quantityAvailable;
    
    @Min(value = 0, message = "Quantity reserved cannot be negative")
    @Builder.Default
    private Integer quantityReserved = 0;
    
    @Min(value = 0, message = "Minimum stock level cannot be negative")
    @Builder.Default
    private Integer minimumStockLevel = 5;
    
    @Min(value = 0, message = "Reorder point cannot be negative")
    @Builder.Default
    private Integer reorderPoint = 10;
}
