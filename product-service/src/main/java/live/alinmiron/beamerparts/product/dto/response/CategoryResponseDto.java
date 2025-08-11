package live.alinmiron.beamerparts.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Category DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {
    
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long parentId;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    // Computed fields
    private String fullPath;
    private Boolean hasSubcategories;
    private Boolean hasProducts;
    private Boolean isRootCategory;
    private Integer subcategoryCount;
    private Integer productCount;
    
    // Optional nested data
    private CategoryResponseDto parent;
    private List<CategoryResponseDto> subcategories;
}
