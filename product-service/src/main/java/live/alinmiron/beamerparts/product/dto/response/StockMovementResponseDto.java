package live.alinmiron.beamerparts.product.dto.response;

import live.alinmiron.beamerparts.product.entity.StockMovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * StockMovement DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponseDto {
    
    private Long id;
    private Long productId;
    private Long variantId;
    private StockMovementType movementType;
    private Integer quantityChange;
    private String reason;
    private String referenceId;
    private String userCode;
    private LocalDateTime createdAt;
    
    // Computed fields
    private Boolean isIncoming;
    private Boolean isOutgoing;
    private Boolean isAdjustment;
    private Boolean isReservation;
    private Boolean isRelease;
    private String displayName;
    
    // Optional nested data
    private ProductResponseDto product;
    private ProductVariantResponseDto variant;
}
