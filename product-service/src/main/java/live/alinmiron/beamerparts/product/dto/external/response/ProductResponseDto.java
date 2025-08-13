package live.alinmiron.beamerparts.product.dto.external.response;

import live.alinmiron.beamerparts.product.entity.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    
    private Long id;
    private String name;
    private String slug;
    private String sku;
    private String description;
    private String shortDescription;
    private BigDecimal basePrice;
    private Long categoryId;
    private String brand;
    private Integer weightGrams;
    private String dimensionsJson;
    private Boolean isFeatured;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private String displayName;
    private Boolean hasVariants;
    private Boolean hasCompatibilityData;
    private Boolean isActive;
    private Integer totalStock;
    private Integer reservedStock;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    
    // Optional nested data
    private CategoryResponseDto category;
    private ProductImageResponseDto primaryImage;
    private List<ProductImageResponseDto> images;
    private List<ProductVariantResponseDto> variants;
    private List<InventoryResponseDto> inventory;
    private List<ProductCompatibilityResponseDto> compatibility;
}
