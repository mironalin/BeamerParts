package live.alinmiron.beamerparts.product.dto.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Stock release request for releasing reserved inventory
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReleaseRequestDto {
    
    @NotBlank(message = "Reservation ID is required")
    private String reservationId;
    
    // Optional: specific items to release (for partial releases)
    private String productSku;
    private String variantSku;
    private Integer quantityToRelease; // If null, release all
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    private String reason; // "cancelled", "expired", "admin", etc.
}
