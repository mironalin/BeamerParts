package live.alinmiron.beamerparts.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Inventory DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponseDto {
    
    private Long id;
    private Long productId;
    private Long variantId;
    private Integer quantityAvailable;
    private Integer quantityReserved;
    private Integer minimumStockLevel;
    private Integer reorderPoint;
    private LocalDateTime lastUpdated;
    
    // Computed fields
    private Integer totalQuantity;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private Boolean isBelowMinimum;
    private String displayName;
    
    // Optional nested data
    private ProductResponseDto product;
    private ProductVariantResponseDto variant;
}
