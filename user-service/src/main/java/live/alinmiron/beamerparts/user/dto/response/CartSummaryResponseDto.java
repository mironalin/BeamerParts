package live.alinmiron.beamerparts.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete cart summary DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartSummaryResponseDto {
    
    private Long userId;
    private List<CartItemResponseDto> items;
    private Integer totalItems;
    private Long totalQuantity;
    private BigDecimal totalValue;
    private Boolean hasOutOfStockItems;
    private Boolean hasInvalidItems;
    
    // Optional shipping/tax info (for future enhancement)
    private BigDecimal estimatedShipping;
    private BigDecimal estimatedTax;
    private BigDecimal estimatedTotal;
}
