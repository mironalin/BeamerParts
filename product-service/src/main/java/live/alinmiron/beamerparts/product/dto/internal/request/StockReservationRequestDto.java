package live.alinmiron.beamerparts.product.dto.internal.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Stock reservation request for inventory operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationRequestDto {
    
    @NotBlank(message = "Product SKU is required")
    private String productSku;
    
    private String variantSku; // Optional
    
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    private String orderId; // Optional reference
    private String source; // "cart", "order", "admin"
    private Integer expirationMinutes; // Default: 30 minutes
}
