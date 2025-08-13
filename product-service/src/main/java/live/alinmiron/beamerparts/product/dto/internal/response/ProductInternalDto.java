package live.alinmiron.beamerparts.product.dto.internal.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Internal DTO for Product data used in service-to-service communication
 * Optimized for performance and specific internal use cases
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInternalDto {
    
    private Long id;
    private String sku;
    private String name;
    private String slug;
    private BigDecimal basePrice;
    private String status;
    private boolean isFeatured;
    private String brand;
    private Integer weightGrams;
    private String shortDescription;
    
    // Category info
    private Long categoryId;
    private String categoryName;
    
    // Variants (only if requested)
    private List<ProductVariantInternalDto> variants;
    
    // Inventory info (only if requested)
    private InventoryInternalDto inventory;
    
    // BMW Compatibility (only if requested)
    private List<String> compatibleGenerations;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Nested DTO for product variants in internal communication
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVariantInternalDto {
        private Long id;
        private String name;
        private String skuSuffix;
        private BigDecimal priceModifier;
        private boolean isActive;
        
        // Variant-specific inventory
        private InventoryInternalDto inventory;
    }
}