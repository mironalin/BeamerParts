package live.alinmiron.beamerparts.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cart item DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {
    
    private Long id;
    private Long userId;
    private String productSku;
    private String variantSku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private String fullSku;
    private BigDecimal totalPrice;
    
    // Optional extended info (when fetched with product service data)
    private String productName;
    private String productDescription;
    private String variantName;
    private String imageUrl;
    private Boolean inStock;
    private Integer availableQuantity;
}
