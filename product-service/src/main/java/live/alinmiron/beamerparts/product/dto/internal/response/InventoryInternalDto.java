package live.alinmiron.beamerparts.product.dto.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Internal DTO for Inventory data used in service-to-service communication
 * Optimized for cart operations, stock checks, and reservations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryInternalDto {
    
    private Long id;
    private Long productId;
    private Long variantId;
    private String productSku;
    private String variantSkuSuffix;
    
    // Stock levels
    private Integer quantityAvailable;
    private Integer quantityReserved;
    private Integer minimumStockLevel;
    private Integer reorderPoint;
    
    // Derived fields for quick checks
    private Integer totalQuantity; // available + reserved
    private boolean isInStock;
    private boolean isLowStock; // available <= reorderPoint
    private boolean isBelowMinimum; // available < minimumStockLevel
    
    // Timestamps
    private LocalDateTime lastUpdated;
    
    /**
     * Calculate if product is available for requested quantity
     */
    public boolean isAvailableForQuantity(int requestedQuantity) {
        return quantityAvailable != null && quantityAvailable >= requestedQuantity;
    }
    
    /**
     * Calculate remaining stock after potential reservation
     */
    public int getRemainingAfterReservation(int reservationQuantity) {
        if (quantityAvailable == null) return 0;
        return Math.max(0, quantityAvailable - reservationQuantity);
    }
    
    /**
     * Check if this is a variant inventory or base product inventory
     */
    public boolean isVariantInventory() {
        return variantId != null;
    }
}