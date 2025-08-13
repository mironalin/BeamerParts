package live.alinmiron.beamerparts.product.dto.external.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ProductVariant DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponseDto {
    
    private Long id;
    private Long productId;
    private String name;
    private String skuSuffix;
    private BigDecimal priceModifier;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    // Computed fields
    private String fullSku;
    private BigDecimal effectivePrice;
    private String displayName;
    private Boolean hasInventory;
    private Integer totalStock;
    private Boolean isLowStock;
    
    // Optional nested data
    private List<InventoryResponseDto> inventory;
}
